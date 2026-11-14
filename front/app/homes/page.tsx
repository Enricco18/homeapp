'use client'

import { useAuth } from '@clerk/nextjs'
import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { createApi } from '@/lib/api'
import type { HomeResponse } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { ShoppingCart, Package, Users, Plus, Pencil, Trash2, ChevronRight } from 'lucide-react'

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

export default function HomesPage() {
  const { getToken } = useAuth()
  const [homes, setHomes] = useState<HomeResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [showEdit, setShowEdit] = useState<HomeResponse | null>(null)
  const [showDelete, setShowDelete] = useState<HomeResponse | null>(null)
  const [formName, setFormName] = useState('')
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
      setHomes(await api.homes.list())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao carregar casas')
    } finally {
      setLoading(false)
    }
  }, [getApi])

  useEffect(() => { load() }, [load])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formName.trim()) return
    setSubmitting(true)
    try {
      const api = await getApi()
      const home = await api.homes.create(formName.trim())
      setHomes(prev => [...prev, home])
      setShowCreate(false)
      setFormName('')
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao criar casa')
    } finally {
      setSubmitting(false)
    }
  }

  const handleEdit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!showEdit || !formName.trim()) return
    setSubmitting(true)
    try {
      const api = await getApi()
      const updated = await api.homes.update(showEdit.id, formName.trim())
      setHomes(prev => prev.map(h => h.id === updated.id ? updated : h))
      setShowEdit(null)
      setFormName('')
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao editar casa')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async () => {
    if (!showDelete) return
    setSubmitting(true)
    try {
      const api = await getApi()
      await api.homes.delete(showDelete.id)
      setHomes(prev => prev.filter(h => h.id !== showDelete.id))
      setShowDelete(null)
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao excluir casa')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <p className="text-zinc-400">Carregando...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-4">
        <p className="text-red-500">{error}</p>
        <Button onClick={load}>Tentar novamente</Button>
      </div>
    )
  }

  return (
    <div className="max-w-3xl mx-auto w-full px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Minhas Casas</h1>
        <Button onClick={() => { setFormName(''); setShowCreate(true) }}>
          <Plus className="size-4" />
          Nova Casa
        </Button>
      </div>

      {homes.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <p className="text-zinc-400 mb-4">Você ainda não tem nenhuma casa.</p>
          <Button onClick={() => { setFormName(''); setShowCreate(true) }}>
            <Plus className="size-4" />
            Criar minha primeira casa
          </Button>
        </div>
      ) : (
        <div className="grid gap-4">
          {homes.map(home => (
            <div key={home.id} className="bg-white border rounded-xl p-5 shadow-sm">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h2 className="text-lg font-semibold">{home.name}</h2>
                  <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                    home.role === 'OWNER'
                      ? 'bg-amber-100 text-amber-700'
                      : 'bg-blue-100 text-blue-700'
                  }`}>
                    {home.role === 'OWNER' ? 'Proprietário' : 'Membro'}
                  </span>
                </div>
                {home.role === 'OWNER' && (
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => { setShowEdit(home); setFormName(home.name) }}
                      className="p-1.5 rounded-lg text-zinc-400 hover:text-zinc-700 hover:bg-zinc-100 transition-colors"
                    >
                      <Pencil className="size-4" />
                    </button>
                    <button
                      onClick={() => setShowDelete(home)}
                      className="p-1.5 rounded-lg text-zinc-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                    >
                      <Trash2 className="size-4" />
                    </button>
                  </div>
                )}
              </div>
              <div className="flex items-center gap-2 flex-wrap">
                <Link
                  href={`/homes/${home.id}/grocery-list`}
                  className="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-lg border hover:bg-zinc-50 transition-colors"
                >
                  <ShoppingCart className="size-4 text-zinc-500" />
                  Lista de compras
                  <ChevronRight className="size-3 text-zinc-400" />
                </Link>
                <Link
                  href={`/homes/${home.id}/items`}
                  className="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-lg border hover:bg-zinc-50 transition-colors"
                >
                  <Package className="size-4 text-zinc-500" />
                  Itens
                  <ChevronRight className="size-3 text-zinc-400" />
                </Link>
                <Link
                  href={`/homes/${home.id}`}
                  className="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-lg border hover:bg-zinc-50 transition-colors"
                >
                  <Users className="size-4 text-zinc-500" />
                  Membros
                  <ChevronRight className="size-3 text-zinc-400" />
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title="Nova Casa">
        <form onSubmit={handleCreate} className="flex flex-col gap-4">
          <input
            autoFocus
            type="text"
            placeholder="Nome da casa"
            value={formName}
            onChange={e => setFormName(e.target.value)}
            maxLength={100}
            className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-900"
          />
          <div className="flex gap-2 justify-end">
            <Button variant="outline" type="button" onClick={() => setShowCreate(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={submitting || !formName.trim()}>
              {submitting ? 'Criando...' : 'Criar'}
            </Button>
          </div>
        </form>
      </Modal>

      <Modal open={!!showEdit} onClose={() => setShowEdit(null)} title="Editar Casa">
        <form onSubmit={handleEdit} className="flex flex-col gap-4">
          <input
            autoFocus
            type="text"
            placeholder="Nome da casa"
            value={formName}
            onChange={e => setFormName(e.target.value)}
            maxLength={100}
            className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-900"
          />
          <div className="flex gap-2 justify-end">
            <Button variant="outline" type="button" onClick={() => setShowEdit(null)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={submitting || !formName.trim()}>
              {submitting ? 'Salvando...' : 'Salvar'}
            </Button>
          </div>
        </form>
      </Modal>

      <Modal open={!!showDelete} onClose={() => setShowDelete(null)} title="Excluir Casa">
        <p className="text-sm text-zinc-600 mb-4">
          Tem certeza que deseja excluir <strong>{showDelete?.name}</strong>? Esta ação não pode ser desfeita.
        </p>
        <div className="flex gap-2 justify-end">
          <Button variant="outline" onClick={() => setShowDelete(null)}>
            Cancelar
          </Button>
          <Button variant="destructive" onClick={handleDelete} disabled={submitting}>
            {submitting ? 'Excluindo...' : 'Excluir'}
          </Button>
        </div>
      </Modal>
    </div>
  )
}
