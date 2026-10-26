/* Data-only FCM delivery; no credentials or Firebase configuration in this worker. */
function activeAccount() {
  return new Promise((resolve) => {
    const request = indexedDB.open('crest-push-session', 1)
    request.onupgradeneeded = () => request.result.createObjectStore('session')
    request.onerror = () => resolve(null)
    request.onsuccess = () => {
      const db = request.result
      const lookup = db.transaction('session').objectStore('session').get('uid')
      lookup.onsuccess = () => { db.close(); resolve(lookup.result || null) }
      lookup.onerror = () => { db.close(); resolve(null) }
    }
  })
}

self.addEventListener('push', (event) => {
  let payload
  try { payload = event.data?.json() } catch { return }
  const data = payload?.data
  if (!data?.eventId || typeof data.title !== 'string' || typeof data.body !== 'string') return
  event.waitUntil((async () => {
    if (await activeAccount() !== data.recipientUid) return
    await self.registration.showNotification(data.title, {
      body: data.body, tag: data.eventId, data: { url: '/#overview' },
    })
  })())
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  event.waitUntil((async () => {
    const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })
    const existing = windows.find(client => new URL(client.url).origin === self.location.origin)
    if (existing) { await existing.focus(); await existing.navigate('/#overview') }
    else await self.clients.openWindow('/#overview')
  })())
})
