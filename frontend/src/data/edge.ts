export const storageProvider = String(import.meta.env.VITE_STORAGE_PROVIDER || 'disabled')
export const notificationsEnabled = import.meta.env.VITE_FCM_ENABLED === 'true'

export async function edgeRequest<T>(name: 'crest-storage' | 'crest-notifications', body: object): Promise<T> {
  const base = String(import.meta.env.VITE_SUPABASE_URL || '').replace(/\/$/, '')
  if (!base || new URL(base).protocol !== 'https:') throw new Error('The school connection is not configured.')
  const { getAuth } = await import('firebase/auth')
  const user = getAuth().currentUser
  if (!user) throw new Error('Sign in again to continue.')
  const response = await fetch(`${base}/functions/v1/${name}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${await user.getIdToken()}`, 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  const result = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(result.error || 'The school service could not complete this request.')
  return result as T
}

export async function notifyEvent(event: 'group-submitted' | 'group-reviewed' | 'account-approved', recordId: string, role?: 'student' | 'teacher') {
  if (!notificationsEnabled) return
  // Notifications are supplementary: a delivery failure must not undo saved work.
  try { await edgeRequest('crest-notifications', { action: 'notify-event', event, recordId, role }) }
  catch { console.warn('The change was saved, but its notification could not be delivered.') }
}
