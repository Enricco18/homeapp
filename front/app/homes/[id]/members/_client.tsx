'use client'

import { useAuth } from '@clerk/nextjs'
import { useCallback, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { createApi } from '@/lib/api'
import type { HomeResponse, MemberResponse } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { ArrowLeft, UserMinus, UserPlus, Crown, User, Search } from 'lucide-react'
import type { ClerkUser } from '@/app/api/users/route'

function Modal({ open, onClose, title, children }: {
  open: boolean
  onClose: () => void
  title: string
  children: React.ReactNode
}) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="fixed inset-0 bg-black/40" onClick={onClose} />
      <div className="relative bg-white rounded-xl shadow-xl p-6 w-full max-w-md mx-4">
        <h2 className="text-lg font-semibold mb-4">{title}</h2>
        {children}
      </div>
    </div>
  )
}

function memberDisplayName(member: MemberResponse): string {
  return member.user.fullName || member.user.username || member.user.email || member.user.id
}

export default function MembersClient({ homeId }: { homeId: number }) {
  const { getToken, userId } = useAuth()
  const router = useRouter()

  const [home, setHome] = useState<HomeResponse | null>(null)
  const [members, setMembers] = useState<MemberResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [removingId, setRemovingId] = useState<string | null>(null)

  const [showInvite, setShowInvite] = useState(false)
  const [clerkUsers, setClerkUsers] = useState<ClerkUser[]>([])
  const [usersLoading, setUsersLoading] = useState(false)
  const [search, setSearch] = useState('')
  const [selectedUser, setSelectedUser] = useState<ClerkUser | null>(null)
  const [submitting, setSubmitting] = useState(false)

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
      const [homeData, membersData] = await Promise.all([
        api.homes.get(homeId),
        api.homes.members(homeId),
      ])
      setHome(homeData)
      setMembers(membersData)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao carregar')
    } finally {
      setLoading(false)
    }
  }, [getApi, homeId])

  useEffect(() => { load() }, [load])

  const openInvite = async () => {
    setSelectedUser(null)
    setSearch('')
    setShowInvite(true)
    setUsersLoading(true)
    try {
      const res = await fetch('/api/users')
      if (!res.ok) throw new Error('Erro ao buscar usuários')
      setClerkUsers(await res.json())
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao carregar usuários')
      setShowInvite(false)
    } finally {
      setUsersLoading(false)
    }
  }

  const handleInvite = async () => {
    if (!selectedUser) return
    setSubmitting(true)
    try {
      const api = await getApi()
      await api.invites.create(homeId, selectedUser.id)
      setShowInvite(false)
      alert(`Convite enviado para ${selectedUser.name}!`)
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao enviar convite')
    } finally {
      setSubmitting(false)
    }
  }

  const handleRemoveMember = async (memberId: string) => {
    const isSelf = memberId === userId
    const isOwner = home?.role === 'OWNER'
    const confirmMsg = isSelf
      ? 'Tem certeza que deseja sair desta casa?'
      : 'Tem certeza que deseja remover este membro?'
    if (!confirm(confirmMsg)) return

    setRemovingId(memberId)
    try {
      const api = await getApi()
      await api.homes.removeMember(homeId, memberId)
      if (isSelf && !isOwner) {
        router.push('/homes')
      } else {
        setMembers(prev => prev.filter(m => m.user.id !== memberId))
      }
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao remover membro')
    } finally {
      setRemovingId(null)
    }
  }

  if (loading) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-zinc-400">Carregando...</p>
      </div>
    )
  }

  if (error || !home) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-4">
        <p className="text-red-500">{error ?? 'Casa não encontrada'}</p>
        <Button onClick={load}>Tentar novamente</Button>
      </div>
    )
  }

  const isOwner = home.role === 'OWNER'
  const memberIds = new Set(members.map(m => m.user.id))

  const filteredUsers = clerkUsers.filter(u => {
    if (memberIds.has(u.id)) return false
    const q = search.toLowerCase()
    return u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)
  })

  return (
    <div className="max-w-3xl mx-auto w-full px-4 py-8">
      <Link href={`/homes/${homeId}`} className="inline-flex items-center gap-1 text-sm text-zinc-500 hover:text-zinc-800 mb-6">
        <ArrowLeft className="size-4" />
        Voltar
      </Link>

      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold">Membros</h1>
          <p className="text-sm text-zinc-500">{home.name}</p>
        </div>
        {isOwner && (
          <Button onClick={openInvite}>
            <UserPlus className="size-4" />
            Convidar
          </Button>
        )}
      </div>

      <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
        <div className="px-5 py-3 border-b bg-zinc-50">
          <p className="text-sm font-semibold text-zinc-600">{members.length} {members.length === 1 ? 'membro' : 'membros'}</p>
        </div>
        <ul className="divide-y">
          {members.map(member => {
            const isSelf = member.user.id === userId
            const canRemove = isOwner ? member.role !== 'OWNER' : isSelf
            const displayName = memberDisplayName(member)
            return (
              <li key={member.user.id} className="flex items-center justify-between px-5 py-3">
                <div className="flex items-center gap-3">
                  {member.user.imageUrl ? (
                    <img src={member.user.imageUrl} alt="" className="size-9 rounded-full object-cover shrink-0" />
                  ) : (
                    <div className="size-9 rounded-full bg-zinc-100 flex items-center justify-center shrink-0">
                      {member.role === 'OWNER'
                        ? <Crown className="size-4 text-amber-500" />
                        : <User className="size-4 text-zinc-400" />
                      }
                    </div>
                  )}
                  <div>
                    <p className="text-sm font-medium">
                      {displayName}
                      {isSelf && <span className="text-xs text-zinc-400 ml-1">(você)</span>}
                    </p>
                    <p className="text-xs text-zinc-400">
                      {member.role === 'OWNER' ? 'Proprietário' : 'Membro'}
                      {member.user.email && <span> · {member.user.email}</span>}
                    </p>
                  </div>
                </div>
                {canRemove && (
                  <button
                    onClick={() => handleRemoveMember(member.user.id)}
                    disabled={removingId === member.user.id}
                    className="p-1.5 rounded-lg text-zinc-400 hover:text-red-600 hover:bg-red-50 transition-colors disabled:opacity-50"
                    title={isSelf ? 'Sair da casa' : 'Remover membro'}
                  >
                    <UserMinus className="size-4" />
                  </button>
                )}
              </li>
            )
          })}
        </ul>
      </div>

      <Modal open={showInvite} onClose={() => setShowInvite(false)} title="Convidar Membro">
        {usersLoading ? (
          <p className="text-sm text-zinc-400 text-center py-6">Buscando usuários...</p>
        ) : (
          <div className="flex flex-col gap-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-zinc-400" />
              <input
                autoFocus
                type="text"
                placeholder="Buscar por nome ou e-mail"
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="w-full border rounded-lg pl-9 pr-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-900"
              />
            </div>

            <div className="max-h-64 overflow-y-auto flex flex-col gap-1 -mx-1 px-1">
              {filteredUsers.length === 0 ? (
                <p className="text-sm text-zinc-400 text-center py-4">
                  {search ? 'Nenhum usuário encontrado.' : 'Todos os usuários já são membros.'}
                </p>
              ) : (
                filteredUsers.map(user => (
                  <button
                    key={user.id}
                    type="button"
                    onClick={() => setSelectedUser(prev => prev?.id === user.id ? null : user)}
                    className={`flex items-center gap-3 w-full px-3 py-2.5 rounded-lg text-left transition-colors ${
                      selectedUser?.id === user.id
                        ? 'bg-zinc-900 text-white'
                        : 'hover:bg-zinc-100'
                    }`}
                  >
                    {user.imageUrl ? (
                      <img src={user.imageUrl} alt="" className="size-8 rounded-full object-cover shrink-0" />
                    ) : (
                      <div className="size-8 rounded-full bg-zinc-200 flex items-center justify-center shrink-0">
                        <User className="size-4 text-zinc-500" />
                      </div>
                    )}
                    <div className="min-w-0">
                      <p className="text-sm font-medium truncate">{user.name}</p>
                      <p className={`text-xs truncate ${selectedUser?.id === user.id ? 'text-zinc-300' : 'text-zinc-400'}`}>
                        {user.email}
                      </p>
                    </div>
                  </button>
                ))
              )}
            </div>

            <div className="flex gap-2 justify-end pt-2 border-t">
              <Button variant="outline" type="button" onClick={() => setShowInvite(false)}>
                Cancelar
              </Button>
              <Button onClick={handleInvite} disabled={submitting || !selectedUser}>
                {submitting ? 'Enviando...' : 'Enviar Convite'}
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
