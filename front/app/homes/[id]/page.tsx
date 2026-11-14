import HomeDetailClient from './_client'

export const instant = false

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params
  return <HomeDetailClient homeId={Number(id)} />
}
