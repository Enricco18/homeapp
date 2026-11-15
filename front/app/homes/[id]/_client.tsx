'use client'

import { useAuth } from '@clerk/nextjs'
import { useCallback, useEffect, useRef, useState } from 'react'
import Link from 'next/link'
import { createApi } from '@/lib/api'
import { STORE_LABELS, type GroceryItemResponse, type HomeResponse, type ItemResponse, type QuickMessageResponse, type Store } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { ArrowLeft, Plus, Trash2, ShoppingCart, CheckCircle2, Circle, Settings, Package, Users, Send } from 'lucide-react'
import type { UserResponse } from '@/lib/types'

function getInitials(author: UserResponse): string {
  const name = author.fullName || author.username || author.email || ''
  const parts = name.trim().split(/\s+/)
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
  return name.slice(0, 2).toUpperCase() || '?'
}
import ScrambledText from '@/components/ScrambledText'
import TextType from '@/components/TextType'

const STORE_COLORS: Record<Store, string> = {
  MERCADO: 'bg-green-100 text-green-700',
  ACOUGUE: 'bg-red-100 text-red-700',
  PADARIA: 'bg-yellow-100 text-yellow-700',
  HORTIFRUTI: 'bg-lime-100 text-lime-700',
  PEIXARIA: 'bg-blue-100 text-blue-700',
  FARMACIA: 'bg-purple-100 text-purple-700',
  OUTRO: 'bg-zinc-100 text-zinc-600',
}

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

type Filter = 'all' | 'pending' | 'bought'

export default function HomeDetailClient({ homeId }: { homeId: number }) {
  const { getToken } = useAuth()

  const [home, setHome] = useState<HomeResponse | null>(null)
  const [entries, setEntries] = useState<GroceryItemResponse[]>([])
  const [catalog, setCatalog] = useState<ItemResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<Filter>('all')
  const [showAdd, setShowAdd] = useState(false)
  const [selectedItemId, setSelectedItemId] = useState<number | ''>('')
  const [quantity, setQuantity] = useState(1)
  const [editEntry, setEditEntry] = useState<GroceryItemResponse | null>(null)
  const [editQty, setEditQty] = useState(1)
  const [submitting, setSubmitting] = useState(false)

  const [quickMessage, setQuickMessage] = useState<QuickMessageResponse | null>(null)
  const [msgInput, setMsgInput] = useState('')
  const [sendingMsg, setSendingMsg] = useState(false)
  const [animEffect, setAnimEffect] = useState<'scramble' | 'texttype'>('scramble')
  const [replayKey, setReplayKey] = useState(0)
  const prevMsgRef = useRef<string | null>(null)

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
      const boughtFilter = filter === 'pending' ? false : filter === 'bought' ? true : undefined
      const [homeData, entriesData, catalogData, msgData] = await Promise.all([
        api.homes.get(homeId),
        api.groceryList.list(homeId, boughtFilter),
        api.items.list(homeId),
        api.quickMessages.get(homeId),
      ])
      setHome(homeData)
      setEntries(entriesData)
      setCatalog(catalogData)
      setQuickMessage(msgData)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao carregar')
    } finally {
      setLoading(false)
    }
  }, [getApi, homeId, filter])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    if (quickMessage?.message !== prevMsgRef.current) {
      prevMsgRef.current = quickMessage?.message ?? null
      setAnimEffect(Math.random() < 0.5 ? 'scramble' : 'texttype')
    }
  }, [quickMessage?.message])

  useEffect(() => {
    const interval = setInterval(async () => {
      try {
        const api = await getApi()
        setQuickMessage(await api.quickMessages.get(homeId))
      } catch { /* silencioso */ }
    }, 30_000)
    return () => clearInterval(interval)
  }, [getApi, homeId])

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedItemId) return
    setSubmitting(true)
    try {
      const api = await getApi()
      const entry = await api.groceryList.add(homeId, Number(selectedItemId), quantity)
      setEntries(prev => [entry, ...prev])
      setShowAdd(false)
      setSelectedItemId('')
      setQuantity(1)
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao adicionar item')
    } finally {
      setSubmitting(false)
    }
  }

  const handleUpdateQty = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!editEntry) return
    setSubmitting(true)
    try {
      const api = await getApi()
      const updated = await api.groceryList.update(homeId, editEntry.id, editQty)
      setEntries(prev => prev.map(e => e.id === updated.id ? updated : e))
      setEditEntry(null)
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao atualizar quantidade')
    } finally {
      setSubmitting(false)
    }
  }

  const handleBuy = async (entry: GroceryItemResponse) => {
    try {
      const api = await getApi()
      const updated = await api.groceryList.buy(homeId, entry.id)
      setEntries(prev => prev.map(e => e.id === updated.id ? updated : e))
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao marcar como comprado')
    }
  }

  const handleDelete = async (entry: GroceryItemResponse) => {
    if (!confirm(`Remover "${entry.itemName}" da lista?`)) return
    try {
      const api = await getApi()
      await api.groceryList.delete(homeId, entry.id)
      setEntries(prev => prev.filter(e => e.id !== entry.id))
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao remover item')
    }
  }

  const handleSendMsg = async (e: React.FormEvent) => {
    e.preventDefault()
    const text = msgInput.trim()
    if (!text) return
    setSendingMsg(true)
    try {
      const api = await getApi()
      const created = await api.quickMessages.send(homeId, text)
      setQuickMessage(created)
      setMsgInput('')
    } catch (e) {
      alert(e instanceof Error ? e.message : 'Erro ao enviar mensagem')
    } finally {
      setSendingMsg(false)
    }
  }

  const pendingItems = catalog.filter(item =>
    !entries.some(e => e.itemId === item.id && !e.bought)
  )

  const pending = entries.filter(e => !e.bought)
  const bought = entries.filter(e => e.bought)

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

  return (
    <div className="max-w-3xl mx-auto w-full px-4 py-8">
      <Link href="/homes" className="inline-flex items-center gap-1 text-sm text-zinc-500 hover:text-zinc-800 mb-6">
        <ArrowLeft className="size-4" />
        Voltar
      </Link>

      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold">{home.name}</h1>
          <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
            isOwner ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700'
          }`}>
            {isOwner ? 'Proprietário' : 'Membro'}
          </span>
        </div>

        <div className="relative group">
          <button className="size-10 rounded-xl bg-zinc-100 hover:bg-zinc-200 flex items-center justify-center transition-colors">
            <Settings className="size-5 text-zinc-600 group-hover:rotate-45 transition-transform duration-300" />
          </button>
          <div className="absolute right-0 top-full mt-2 w-48 bg-white border rounded-xl shadow-lg overflow-hidden opacity-0 invisible group-hover:opacity-100 group-hover:visible -translate-y-1 group-hover:translate-y-0 transition-all duration-150 z-10">
            <Link href={`/homes/${homeId}/items`} className="flex items-center gap-3 px-4 py-3 text-sm hover:bg-zinc-50 transition-colors">
              <Package className="size-4 text-zinc-500 shrink-0" />
              Catálogo de Itens
            </Link>
            <Link href={`/homes/${homeId}/members`} className="flex items-center gap-3 px-4 py-3 text-sm hover:bg-zinc-50 transition-colors border-t">
              <Users className="size-4 text-zinc-500 shrink-0" />
              Membros
            </Link>
          </div>
        </div>
      </div>

      <div className="bg-white border rounded-xl px-5 py-4 shadow-sm mb-6 flex items-center gap-4">
        {quickMessage ? (
          <div
            className="size-8 rounded-full bg-zinc-900 text-white flex items-center justify-center text-xs font-bold shrink-0"
            title={quickMessage.author.fullName || quickMessage.author.username || quickMessage.author.email || ''}
          >
            {getInitials(quickMessage.author)}
          </div>
        ) : (
          <div className="size-8 rounded-full bg-zinc-100 shrink-0" />
        )}
        <div className="flex-1 min-w-0">
          {quickMessage ? (
            animEffect === 'scramble' ? (
              <ScrambledText key={quickMessage.message} effect="scramble" className="text-2xl font-bold leading-tight">
                {quickMessage.message}
              </ScrambledText>
            ) : (
              <div onMouseEnter={() => setReplayKey(k => k + 1)}>
                <TextType
                  key={`${quickMessage.message}-${replayKey}`}
                  text={quickMessage.message}
                  loop={false}
                  showCursor={false}
                  typeEffect="rise"
                  className="text-2xl font-bold"
                />
              </div>
            )
          ) : (
            <p className="text-sm text-zinc-400 italic">Nenhuma mensagem rápida</p>
          )}
        </div>
        <form onSubmit={handleSendMsg} className="flex items-center gap-2 shrink-0">
          <input
            type="text"
            value={msgInput}
            onChange={e => setMsgInput(e.target.value.slice(0, 20))}
            placeholder="Mensagem..."
            maxLength={20}
            className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:ring-2 focus:ring-zinc-900 w-36"
          />
          <button
            type="submit"
            disabled={sendingMsg || !msgInput.trim()}
            className="p-1.5 rounded-lg bg-zinc-900 text-white hover:bg-zinc-700 disabled:opacity-40 transition-colors"
          >
            <Send className="size-4" />
          </button>
        </form>
      </div>

      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <ShoppingCart className="size-5 text-zinc-600" />
          <h2 className="text-lg font-bold">Lista de Compras</h2>
        </div>
        <Button size="sm" onClick={() => { setSelectedItemId(''); setQuantity(1); setShowAdd(true) }}>
          <Plus className="size-4" />
          Adicionar
        </Button>
      </div>

      <div className="flex gap-2 mb-4">
        {(['all', 'pending', 'bought'] as Filter[]).map(f => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
              filter === f
                ? 'bg-zinc-900 text-white'
                : 'bg-zinc-100 text-zinc-600 hover:bg-zinc-200'
            }`}
          >
            {f === 'all' ? `Todos (${entries.length})` : f === 'pending' ? `Pendentes (${pending.length})` : `Comprados (${bought.length})`}
          </button>
        ))}
      </div>

      {entries.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <ShoppingCart className="size-12 text-zinc-200 mb-3" />
          <p className="text-zinc-400 mb-4">A lista está vazia.</p>
          <Button onClick={() => { setSelectedItemId(''); setQuantity(1); setShowAdd(true) }}>
            <Plus className="size-4" />
            Adicionar item
          </Button>
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          {(filter === 'all' ? [...pending, ...bought] : entries).map(entry => (
            <div
              key={entry.id}
              className={`bg-white border rounded-xl px-4 py-3 flex items-center gap-3 shadow-sm ${
                entry.bought ? 'opacity-60' : ''
              }`}
            >
              <button
                onClick={() => !entry.bought && handleBuy(entry)}
                disabled={entry.bought}
                className="shrink-0 text-zinc-300 hover:text-green-500 transition-colors disabled:cursor-default"
              >
                {entry.bought
                  ? <CheckCircle2 className="size-5 text-green-500" />
                  : <Circle className="size-5" />
                }
              </button>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className={`text-sm font-medium ${entry.bought ? 'line-through text-zinc-400' : ''}`}>
                    {entry.itemName}
                  </span>
                  <span className={`text-xs px-2 py-0.5 rounded-full ${STORE_COLORS[entry.store]}`}>
                    {STORE_LABELS[entry.store]}
                  </span>
                </div>
                <div className="flex items-center gap-2 mt-0.5">
                  <span className="text-xs text-zinc-400">Qtd: {entry.quantity}</span>
                  {!entry.bought && (
                    <button
                      onClick={() => { setEditEntry(entry); setEditQty(entry.quantity) }}
                      className="text-xs text-zinc-400 underline hover:text-zinc-700"
                    >
                      editar
                    </button>
                  )}
                  {entry.bought && entry.boughtAt && (
                    <span className="text-xs text-zinc-400">
                      · {new Date(entry.boughtAt).toLocaleDateString('pt-BR')}
                    </span>
                  )}
                </div>
              </div>
              <button
                onClick={() => handleDelete(entry)}
                className="p-1.5 rounded-lg text-zinc-300 hover:text-red-500 hover:bg-red-50 transition-colors shrink-0"
              >
                <Trash2 className="size-4" />
              </button>
            </div>
          ))}
        </div>
      )}

      <Modal open={showAdd} onClose={() => setShowAdd(false)} title="Adicionar à Lista">
        <form onSubmit={handleAdd} className="flex flex-col gap-4">
          <div>
            <label className="text-xs font-medium text-zinc-500 block mb-1">Item</label>
            {pendingItems.length === 0 ? (
              <p className="text-sm text-zinc-400">
                Todos os itens do catálogo já estão na lista.{' '}
                <Link href={`/homes/${homeId}/items`} className="underline">
                  Adicionar mais itens
                </Link>
              </p>
            ) : (
              <select
                autoFocus
                value={selectedItemId}
                onChange={e => setSelectedItemId(Number(e.target.value))}
                className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-900 bg-white"
              >
                <option value="">Selecione um item</option>
                {pendingItems.map(item => (
                  <option key={item.id} value={item.id}>
                    {item.name} — {STORE_LABELS[item.store]}
                  </option>
                ))}
              </select>
            )}
          </div>
          <div>
            <label className="text-xs font-medium text-zinc-500 block mb-1">Quantidade</label>
            <input
              type="number"
              min={1}
              value={quantity}
              onChange={e => setQuantity(Math.max(1, Number(e.target.value)))}
              className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-900"
            />
          </div>
          <div className="flex gap-2 justify-end">
            <Button variant="outline" type="button" onClick={() => setShowAdd(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={submitting || !selectedItemId || pendingItems.length === 0}>
              {submitting ? 'Adicionando...' : 'Adicionar'}
            </Button>
          </div>
        </form>
      </Modal>

      <Modal open={!!editEntry} onClose={() => setEditEntry(null)} title="Editar Quantidade">
        <form onSubmit={handleUpdateQty} className="flex flex-col gap-4">
          <div>
            <label className="text-xs font-medium text-zinc-500 block mb-1">
              {editEntry?.itemName}
            </label>
            <input
              autoFocus
              type="number"
              min={1}
              value={editQty}
              onChange={e => setEditQty(Math.max(1, Number(e.target.value)))}
              className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-900"
            />
          </div>
          <div className="flex gap-2 justify-end">
            <Button variant="outline" type="button" onClick={() => setEditEntry(null)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Salvando...' : 'Salvar'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
