import { getApp } from 'firebase/app'
import { getAuth } from 'firebase/auth'
import { deleteToken, getMessaging, getToken, isSupported } from 'firebase/messaging'
import { edgeRequest, notificationsEnabled } from './edge'

const tokenKey = 'crest-push-token'

// Shared with the worker so queued messages are suppressed after sign-out,
// including when the network cannot remove the old server registration.
async function setNotificationAccount(uid: string | null) {
  await new Promise<void>((resolve, reject) => {
    const request = indexedDB.open('crest-push-session', 1)
    request.onupgradeneeded = () => request.result.createObjectStore('session')
    request.onerror = () => reject(new Error('Browser notification storage is unavailable.'))
    request.onsuccess = () => {
      const db = request.result
      const transaction = db.transaction('session', 'readwrite')
      transaction.objectStore('session').put(uid, 'uid')
      transaction.oncomplete = () => { db.close(); resolve() }
      transaction.onerror = () => { db.close(); reject(new Error('Browser notification settings could not be saved.')) }
    }
  })
}

// Called only from a user click, so the browser can show its permission prompt.
export async function enableNotifications() {
  if (!notificationsEnabled) throw new Error('Notifications are not enabled yet.')
  if (!await isSupported()) throw new Error('This browser does not support school notifications.')
  const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY
  if (!vapidKey || !import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID)
    throw new Error('School notifications have not been configured.')
  if (await Notification.requestPermission() !== 'granted')
    throw new Error('Allow notifications in your browser settings to receive school updates.')
  const worker = await navigator.serviceWorker.register('/firebase-messaging-sw.js')
  const ready = await navigator.serviceWorker.ready
  const token = await getToken(getMessaging(getApp()), { vapidKey, serviceWorkerRegistration: ready })
  if (!token) throw new Error('This browser could not register for notifications.')
  try {
    await edgeRequest('crest-notifications', { action: 'register-device', token, platform: 'web' })
    await setNotificationAccount(getAuth().currentUser?.uid || null)
    localStorage.setItem(tokenKey, token)
  } catch (error) {
    await deleteToken(getMessaging(getApp())).catch(() => undefined)
    await worker.unregister().catch(() => undefined)
    throw error
  }
}

export async function refreshNotifications() {
  if (!notificationsEnabled || !localStorage.getItem(tokenKey) || !await isSupported() || Notification.permission !== 'granted') return
  const registration = await navigator.serviceWorker.ready
  const token = await getToken(getMessaging(getApp()), { vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY, serviceWorkerRegistration: registration })
  const old = localStorage.getItem(tokenKey)
  await edgeRequest('crest-notifications', { action: 'register-device', token, platform: 'web' })
  await setNotificationAccount(getAuth().currentUser?.uid || null)
  localStorage.setItem(tokenKey, token)
  if (old && old !== token) await edgeRequest('crest-notifications', { action: 'remove-device', token: old })
}

export async function disableNotifications() {
  const token = localStorage.getItem(tokenKey)
  if (!token) return
  await setNotificationAccount(null)
  await edgeRequest('crest-notifications', { action: 'remove-device', token }).catch(() => undefined)
  if (await isSupported()) await deleteToken(getMessaging(getApp())).catch(() => undefined)
  localStorage.removeItem(tokenKey)
}
