import type { HomeResponse, MemberResponse, ItemResponse, GroceryItemResponse, InviteResponse, QuickMessageResponse, Store } from './types'

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

async function request<T>(token: string, path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...options?.headers,
    },
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`${res.status}: ${text}`)
  }
  if (res.status === 204) return undefined as T
  return res.json()
}

export function createApi(token: string) {
  return {
    homes: {
      list: () => request<HomeResponse[]>(token, '/homes'),
      get: (id: number) => request<HomeResponse>(token, `/homes/${id}`),
      create: (name: string) =>
        request<HomeResponse>(token, '/homes', { method: 'POST', body: JSON.stringify({ name }) }),
      update: (id: number, name: string) =>
        request<HomeResponse>(token, `/homes/${id}`, { method: 'PUT', body: JSON.stringify({ name }) }),
      delete: (id: number) => request<void>(token, `/homes/${id}`, { method: 'DELETE' }),
      members: (id: number) => request<MemberResponse[]>(token, `/homes/${id}/members`),
      removeMember: (homeId: number, userId: string) =>
        request<void>(token, `/homes/${homeId}/members/${userId}`, { method: 'DELETE' }),
    },
    items: {
      list: (homeId: number) => request<ItemResponse[]>(token, `/homes/${homeId}/items`),
      create: (homeId: number, data: { name: string; store: Store }) =>
        request<ItemResponse>(token, `/homes/${homeId}/items`, { method: 'POST', body: JSON.stringify(data) }),
      update: (homeId: number, itemId: number, data: { name: string; store: Store }) =>
        request<ItemResponse>(token, `/homes/${homeId}/items/${itemId}`, {
          method: 'PUT',
          body: JSON.stringify(data),
        }),
      delete: (homeId: number, itemId: number) =>
        request<void>(token, `/homes/${homeId}/items/${itemId}`, { method: 'DELETE' }),
    },
    groceryList: {
      list: (homeId: number, bought?: boolean) =>
        request<GroceryItemResponse[]>(
          token,
          `/homes/${homeId}/grocery-list${bought !== undefined ? `?bought=${bought}` : ''}`
        ),
      add: (homeId: number, itemId: number, quantity: number) =>
        request<GroceryItemResponse>(token, `/homes/${homeId}/grocery-list`, {
          method: 'POST',
          body: JSON.stringify({ itemId, quantity }),
        }),
      update: (homeId: number, entryId: number, quantity: number) =>
        request<GroceryItemResponse>(token, `/homes/${homeId}/grocery-list/${entryId}`, {
          method: 'PATCH',
          body: JSON.stringify({ quantity }),
        }),
      buy: (homeId: number, entryId: number) =>
        request<GroceryItemResponse>(token, `/homes/${homeId}/grocery-list/${entryId}/buy`, { method: 'POST' }),
      delete: (homeId: number, entryId: number) =>
        request<void>(token, `/homes/${homeId}/grocery-list/${entryId}`, { method: 'DELETE' }),
    },
    quickMessages: {
      get: (homeId: number) => request<QuickMessageResponse | null>(token, `/homes/${homeId}/quick-message`),
      send: (homeId: number, message: string) =>
        request<QuickMessageResponse>(token, `/homes/${homeId}/quick-message`, {
          method: 'POST',
          body: JSON.stringify({ message }),
        }),
    },
    invites: {
      list: (type: 'received' | 'sent') => request<InviteResponse[]>(token, `/invites?type=${type}`),
      create: (homeId: number, inviteeId: string) =>
        request<InviteResponse>(token, '/invites', {
          method: 'POST',
          body: JSON.stringify({ homeId, inviteeId }),
        }),
      accept: (id: number) => request<InviteResponse>(token, `/invites/${id}/accept`, { method: 'POST' }),
      decline: (id: number) => request<InviteResponse>(token, `/invites/${id}/decline`, { method: 'POST' }),
      delete: (id: number) => request<void>(token, `/invites/${id}`, { method: 'DELETE' }),
    },
  }
}
