import { useState } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { notificationsEnabled } from '../data/edge'

export function NotificationSettings() {
  const [busy, setBusy] = useState(false)
  const [enabled, setEnabled] = useState(() => {
    try { return !!localStorage.getItem('crest-push-token') }
    catch { return false }
  })
  if (!notificationsEnabled) return null
  return <Button variant="outline" disabled={busy} onClick={async () => {
    setBusy(true)
    try {
      const api = await import('../data/notifications')
      if (enabled) await api.disableNotifications()
      else await api.enableNotifications()
      setEnabled(!enabled)
      toast.success(enabled ? 'School notifications turned off.' : 'School notifications turned on.')
    } catch (error) { toast.error(error instanceof Error ? error.message : 'Notifications could not be updated.') }
    finally { setBusy(false) }
  }}>{enabled ? 'Turn off notifications' : 'Enable notifications'}</Button>
}
