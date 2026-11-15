'use client'

import { useAuth } from '@clerk/nextjs'
import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { createApi } from '@/lib/api'
import type { InviteResponse, InviteStatus } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { Check, X, Trash2, Mail, Send } from 'lucide-react'
import type { ClerkUser } from '@/app/api/users/route'

const STATUS_STYLES: Record<InviteStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  ACCEPTED: 'bg-green-100 text-green-700',
  DECLINED: 'bg-red-100 text-red-700',
}

const STATUS_LABELS: Record<InviteStatus, string> = {
  PENDING: 'Pendente',
  ACCEPTED: 'Aceito',
  DECLINED: 'Recusado',
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

export default function InvitesPage() {
  const { getToken } = useAuth()
  const [tab, setTab] = useState<'received' | 'sent'>('received')
  const [invites, setInvites] = useState<InviteResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actioning, setActioning] = useState<number | null>(null)
  const [userMap, setUserMap] = useState<Record<string, string>>({})

  useEffect(() => {
    fetch('/api/users')
      .then(r => r.json())
      .then((users: ClerkUser[]) => {
        const map: Record<string, string> = {}
        users.forEach(u => { map[u.id] = u.name || u.email })
        setUserMap(map)
      })
      .catch(() => {})
  }, [])

  const getApi = useCallback(async () => {
    const token = await getToken()
    if (!token) throw new Error('Não autenticado')
    return createApi(token)
  }, [getToken])

  const load = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const api = await getApi()
      setInvites(await api.invites.list(tab))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao carregar convites')
    } finally {
      setLoading(false)
    }
  }, [getApi, tab])

  useEffect(() => { load() }, [load])

  const handleAccept = async (invite: InviteResponse) => {
    setActioning(invite.id)
    try {
      const api = await getApi()
      const updated = await api.invites.accept(invite.id)
      setInvites(prev => prev.map(i => i.id === updated.id ? updated : i))
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao aceitar convite')
    } finally {
      setActioning(null)
    }
  }

  const handleDecline = async (invite: InviteResponse) => {
    setActioning(invite.id)
    try {
      const api = await getApi()
      const updated = await api.invites.decline(invite.id)
      setInvites(prev => prev.map(i => i.id === updated.id ? updated : i))
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao recusar convite')
    } finally {
      setActioning(null)
    }
  }

  const handleDelete = async (invite: InviteResponse) => {
    if (!confirm('Remover este convite?')) return
    setActioning(invite.id)
    try {
      const api = await getApi()
      await api.invites.delete(invite.id)
      setInvites(prev => prev.filter(i => i.id !== invite.id))
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao remover convite')
    } finally {
      setActioning(null)
    }
  }

  return (
    <div className="max-w-3xl mx-auto w-full px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">Convites</h1>

      <div className="flex gap-1 mb-6 bg-zinc-100 rounded-lg p-1 w-fit">
        <button
          onClick={() => setTab('received')}
          className={`flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
            tab === 'received' ? 'bg-white shadow-sm text-zinc-900' : 'text-zinc-500 hover:text-zinc-700'
          }`}
        >
          <Mail className="size-4" />
          Recebidos
        </button>
        <button
          onClick={() => setTab('sent')}
          className={`flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
            tab === 'sent' ? 'bg-white shadow-sm text-zinc-900' : 'text-zinc-500 hover:text-zinc-700'
          }`}
        >
          <Send className="size-4" />
          Enviados
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <p className="text-zinc-400">Carregando...</p>
        </div>
      ) : error ? (
        <div className="flex flex-col items-center justify-center py-20 gap-4">
          <p className="text-red-500">{error}</p>
          <Button onClick={load}>Tentar novamente</Button>
        </div>
      ) : invites.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          {tab === 'received'
            ? <Mail className="size-12 text-zinc-200 mb-3" />
            : <Send className="size-12 text-zinc-200 mb-3" />
          }
          <p className="text-zinc-400">
            {tab === 'received' ? 'Nenhum convite recebido.' : 'Nenhum convite enviado.'}
          </p>
          {tab === 'sent' && (
            <p className="text-sm text-zinc-400 mt-1">
              Para convidar alguém, acesse{' '}
              <Link href="/homes" className="underline hover:text-zinc-700">uma casa</Link>
              {' '}e clique em "Convidar".
            </p>
          )}
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          {invites.map(invite => (
            <div key={invite.id} className="bg-white border rounded-xl px-5 py-4 shadow-sm">
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <Link
                      href={`/homes/${invite.homeId}`}
                      className="font-semibold hover:underline truncate"
                    >
                      {invite.homeName}
                    </Link>
                    <span className={`text-xs font-medium px-2 py-0.5 rounded-full shrink-0 ${STATUS_STYLES[invite.status]}`}>
                      {STATUS_LABELS[invite.status]}
                    </span>
                  </div>
                  <div className="text-xs text-zinc-400 space-y-0.5">
                    {tab === 'received' ? (
                      <p>De: <span className="font-medium text-zinc-600">{userMap[invite.inviterId] ?? invite.inviterId}</span></p>
                    ) : (
                      <p>Para: <span className="font-medium text-zinc-600">{userMap[invite.inviteeId] ?? invite.inviteeId}</span></p>
                    )}
                    <p>Enviado: {formatDate(invite.createdAt)} · Expira: {formatDate(invite.expiresAt)}</p>
                    {invite.respondedAt && (
                      <p>Respondido: {formatDate(invite.respondedAt)}</p>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  {tab === 'received' && invite.status === 'PENDING' && (
                    <>
                      <button
                        onClick={() => handleAccept(invite)}
                        disabled={actioning === invite.id}
                        className="p-1.5 rounded-lg text-zinc-400 hover:text-green-600 hover:bg-green-50 transition-colors disabled:opacity-50"
                        title="Aceitar"
                      >
                        <Check className="size-5" />
                      </button>
                      <button
                        onClick={() => handleDecline(invite)}
                        disabled={actioning === invite.id}
                        className="p-1.5 rounded-lg text-zinc-400 hover:text-red-600 hover:bg-red-50 transition-colors disabled:opacity-50"
                        title="Recusar"
                      >
                        <X className="size-5" />
                      </button>
                    </>
                  )}
                  <button
                    onClick={() => handleDelete(invite)}
                    disabled={actioning === invite.id}
                    className="p-1.5 rounded-lg text-zinc-400 hover:text-red-600 hover:bg-red-50 transition-colors disabled:opacity-50"
                    title="Remover"
                  >
                    <Trash2 className="size-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
