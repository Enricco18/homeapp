import { auth, clerkClient } from '@clerk/nextjs/server'

export interface ClerkUser {
  id: string
  name: string
  email: string
  imageUrl: string
}

export async function GET() {
  const { userId } = await auth()
  if (!userId) return Response.json({ error: 'Unauthorized' }, { status: 401 })

  const client = await clerkClient()
  const { data: users } = await client.users.getUserList({ limit: 100 })

  const result: ClerkUser[] = users.map(u => ({
    id: u.id,
    name: [u.firstName, u.lastName].filter(Boolean).join(' ') || (u.emailAddresses[0]?.emailAddress ?? u.id),
    email: u.emailAddresses[0]?.emailAddress ?? '',
    imageUrl: u.imageUrl,
  }))

  return Response.json(result)
}
