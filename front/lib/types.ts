export type HomeRole = 'OWNER' | 'MEMBER'
export type InviteStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED'
export type Store = 'MERCADO' | 'ACOUGUE' | 'PADARIA' | 'HORTIFRUTI' | 'PEIXARIA' | 'FARMACIA' | 'OUTRO'

export const STORE_LABELS: Record<Store, string> = {
  MERCADO: 'Mercado',
  ACOUGUE: 'Açougue',
  PADARIA: 'Padaria',
  HORTIFRUTI: 'Hortifruti',
  PEIXARIA: 'Peixaria',
  FARMACIA: 'Farmácia',
  OUTRO: 'Outro',
}

export const STORES: Store[] = ['MERCADO', 'ACOUGUE', 'PADARIA', 'HORTIFRUTI', 'PEIXARIA', 'FARMACIA', 'OUTRO']

export interface HomeResponse {
  id: number
  name: string
  role: HomeRole
}

export interface UserResponse {
  id: string
  username: string | null
  fullName: string | null
  email: string | null
  imageUrl: string | null
}

export interface MemberResponse {
  user: UserResponse
  role: HomeRole
}

export interface QuickMessageResponse {
  id: number
  message: string
  author: UserResponse
  createdAt: string
  expiresAt: string
}

export interface ItemResponse {
  id: number
  name: string
  store: Store
}

export interface GroceryItemResponse {
  id: number
  itemId: number
  itemName: string
  store: Store
  quantity: number
  bought: boolean
  boughtBy: string | null
  boughtAt: string | null
  createdAt: string
  updatedAt: string
}

export interface InviteResponse {
  id: number
  homeId: number
  homeName: string
  inviterId: string
  inviteeId: string
  status: InviteStatus
  createdAt: string
  expiresAt: string
  respondedAt: string | null
}
