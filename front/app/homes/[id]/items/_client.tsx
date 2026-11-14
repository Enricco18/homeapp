'use client'

import { useAuth } from '@clerk/nextjs'
import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { createApi } from '@/lib/api'
import { STORE_LABELS, STORES, type ItemResponse, type Store } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { ArrowLeft, Plus, Pencil, Trash2 } from 'lucide-react'

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

const STORE_COLORS: Record<Store, string> = {
  MERCADO: 'bg-green-100 text-green-700',
  ACOUGUE: 'bg-red-100 text-red-700',
  PADARIA: 'bg-yellow-100 text-yellow-700',
  HORTIFRUTI: 'bg-lime-100 text-lime-700',
  PEIXARIA: 'bg-blue-100 text-blue-700',
  FARMACIA: 'bg-purple-100 text-purple-700',
  OUTRO: 'bg-zinc-100 text-zinc-600',
}

export default function ItemsClient({ homeId }: { homeId: number }) {
  const { getToken } = useAuth()

  const [items, setItems] = useState<ItemResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editItem, setEditItem] = useState<ItemResponse | null>(null)
  const [showDelete, setShowDelete] = useState<ItemResponse | null>(null)
  const [formName, setFormName] = useState('')
  const [formStore, setFormStore] = useState<Store>('MERCADO')
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
      setItems(await api.items.list(homeId))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao carregar itens')
    } finally {
      setLoading(false)
    }
  }, [getApi, homeId])

  useEffect(() => { load() }, [load])

  const openCreate = () => {
    setEditItem(null)
    setFormName('')
    setFormStore('MERCADO')
    setShowForm(true)
  }

  const openEdit = (item: ItemResponse) => {
    setEditItem(item)
    setFormName(item.name)
    setFormStore(item.store)
    setShowForm(true)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formName.trim()) return
    setSubmitting(true)
    try {
      const api = await getApi()
      if (editItem) {
        const updated = await api.items.update(homeId, editItem.id, { name: formName.trim(), store: formStore })
        setItems(prev => prev.map(i => i.id === updated.id ? updated : i))
      } else {
        const created = await api.items.create(homeId, { name: formName.trim(), store: formStore })
        setItems(prev => [...prev, created])
      }
      setShowForm(false)
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao salvar item')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async () => {
    if (!showDelete) return
    setSubmitting(true)
    try {
      const api = await getApi()
      await api.items.delete(homeId, showDelete.id)
      setItems(prev => prev.filter(i => i.id !== showDelete.id))
      setShowDelete(null)
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao excluir item')
    } finally {
      setSubmitting(false)
    }
  }

  const grouped = items.reduce<Record<string, ItemResponse[]>>((acc, item) => {
    const store = STORE_LABELS[item.store]
    acc[store] = [...(acc[store] ?? []), item]
    return acc
  }, {})

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
      <Link href={`/homes/${homeId}`} className="inline-flex items-center gap-1 text-sm text-zinc-500 hover:text-zinc-800 mb-6">
        <ArrowLeft className="size-4" />
        Voltar
      </Link>

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Catálogo de Itens</h1>
        <Button onClick={openCreate}>
          <Plus className="size-4" />
          Novo Item
        </Button>
      </div>

      {items.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <p className="text-zinc-400 mb-4">Nenhum item cadastrado ainda.</p>
          <Button onClick={openCreate}>
            <Plus className="size-4" />
            Adicionar primeiro item
          </Button>
        </div>
      ) : (
        <div className="flex flex-col gap-6">
          {Object.entries(grouped).map(([storeName, storeItems]) => (
            <div key={storeName} className="bg-white border rounded-xl shadow-sm overflow-hidden">
              <div className="px-5 py-3 border-b bg-zinc-50">
                <h2 className="text-sm font-semibold text-zinc-600">{storeName}</h2>
              </div>
              <ul className="divide-y">
                {storeItems.map(item => (
                  <li key={item.id} className="flex items-center justify-between px-5 py-3">
                    <div className="flex items-center gap-3">
                      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${STORE_COLORS[item.store]}`}>
                        {STORE_LABELS[item.store]}
                      </span>
                      <span className="text-sm font-medium">{item.name}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => openEdit(item)}
                        className="p-1.5 rounded-lg text-zinc-400 hover:text-zinc-700 hover:bg-zinc-100 transition-colors"
                      >
                        <Pencil className="size-4" />
                      </button>
                      <button
                        onClick={() => setShowDelete(item)}
                        className="p-1.5 rounded-lg text-zinc-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                      >
                        <Trash2 className="size-4" />
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      )}

      <Modal open={showForm} onClose={() => setShowForm(false)} title={editItem ? 'Editar Item' : 'Novo Item'}>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="text-xs font-medium text-zinc-500 block mb-1">Nome</label>
            <input
              autoFocus
              type="text"
              placeholder="Nome do item"
              value={formName}
              onChange={e => setFormName(e.target.value)}
              maxLength={100}
              className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-900"
            />
          </div>
          <div>
            <label className="text-xs font-medium text-zinc-500 block mb-1">Loja</label>
            <select
              value={formStore}
              onChange={e => setFormStore(e.target.value as Store)}
              className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-900 bg-white"
            >
              {STORES.map(s => (
                <option key={s} value={s}>{STORE_LABELS[s]}</option>
              ))}
            </select>
          </div>
          <div className="flex gap-2 justify-end">
            <Button variant="outline" type="button" onClick={() => setShowForm(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={submitting || !formName.trim()}>
              {submitting ? 'Salvando...' : editItem ? 'Salvar' : 'Criar'}
            </Button>
          </div>
        </form>
      </Modal>

      <Modal open={!!showDelete} onClose={() => setShowDelete(null)} title="Excluir Item">
        <p className="text-sm text-zinc-600 mb-4">
          Tem certeza que deseja excluir <strong>{showDelete?.name}</strong>?
        </p>
        <div className="flex gap-2 justify-end">
          <Button variant="outline" onClick={() => setShowDelete(null)}>Cancelar</Button>
          <Button variant="destructive" onClick={handleDelete} disabled={submitting}>
            {submitting ? 'Excluindo...' : 'Excluir'}
          </Button>
        </div>
      </Modal>
    </div>
  )
}
