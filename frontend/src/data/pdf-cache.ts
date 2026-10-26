// Local copies contain private school files. Keys include the backend and user;
// callers must reauthorize every open before consulting this cache.
export const PDF_CACHE_TTL = 30 * 24 * 60 * 60 * 1000
export const PDF_CACHE_LIMIT = 100 * 1024 * 1024
const databaseName = 'crest-private-pdfs-v1'

interface Metadata {
  key: string
  size: number
  expiresAt: number
  accessedAt: number
}

function database(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(databaseName, 1)
    request.onupgradeneeded = () => {
      request.result.createObjectStore('files')
      request.result.createObjectStore('metadata', { keyPath: 'key' })
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
    request.onblocked = () => reject(new Error('PDF cache is unavailable.'))
  })
}

function completed(tx: IDBTransaction): Promise<void> {
  return new Promise((resolve, reject) => {
    tx.oncomplete = () => resolve()
    tx.onerror = tx.onabort = () => reject(tx.error)
  })
}

export const pdfCacheKey = (scope: string, fileId: string) => JSON.stringify([scope, fileId])

export async function cachedPdf(key: string, now = Date.now()): Promise<Blob | undefined> {
  let db: IDBDatabase | undefined
  try {
    db = await database()
    const tx = db.transaction(['files', 'metadata'], 'readwrite')
    const done = completed(tx)
    const files = tx.objectStore('files')
    const metadata = tx.objectStore('metadata')
    let blob: Blob | undefined
    const request = metadata.getAll()
    request.onsuccess = () => {
      for (const entry of request.result as Metadata[]) {
        if (!Number.isFinite(entry.expiresAt) || entry.expiresAt <= now) {
          metadata.delete(entry.key)
          files.delete(entry.key)
        } else if (entry.key === key) {
          const file = files.get(key)
          file.onsuccess = () => {
            if (file.result instanceof Blob && file.result.size === entry.size) {
              blob = file.result
              // Reading does not extend the original 30-day expiry.
              metadata.put({ ...entry, accessedAt: now })
            } else {
              files.delete(key)
              metadata.delete(key)
            }
          }
        }
      }
    }
    await done
    return blob
  } catch {
    // Private mode, quota limits, and browser eviction must not block reading.
    return undefined
  } finally {
    db?.close()
  }
}

export async function cachePdf(key: string, blob: Blob, now = Date.now()): Promise<void> {
  if (!blob.size || blob.size > PDF_CACHE_LIMIT) return
  let db: IDBDatabase | undefined
  try {
    db = await database()
    const tx = db.transaction(['files', 'metadata'], 'readwrite')
    const done = completed(tx)
    const files = tx.objectStore('files')
    const metadata = tx.objectStore('metadata')
    const request = metadata.getAll()
    request.onsuccess = () => {
      const entries = (request.result as Metadata[]).sort((a, b) => a.accessedAt - b.accessedAt)
      const retained: Metadata[] = []
      for (const entry of entries) {
        if (entry.key === key || entry.expiresAt <= now) {
          files.delete(entry.key)
          metadata.delete(entry.key)
        } else retained.push(entry)
      }
      let total = retained.reduce((sum, entry) => sum + entry.size, 0)
      for (const entry of retained) {
        if (total + blob.size <= PDF_CACHE_LIMIT) break
        files.delete(entry.key)
        metadata.delete(entry.key)
        total -= entry.size
      }
      files.put(blob, key)
      metadata.put({ key, size: blob.size, expiresAt: now + PDF_CACHE_TTL, accessedAt: now })
    }
    await done
  } catch {
    // Saving is best effort; the downloaded PDF still opens.
  } finally {
    db?.close()
  }
}

export async function removeCachedPdf(key?: string): Promise<void> {
  let db: IDBDatabase | undefined
  try {
    db = await database()
    const tx = db.transaction(['files', 'metadata'], 'readwrite')
    const done = completed(tx)
    for (const name of ['files', 'metadata']) {
      const store = tx.objectStore(name)
      if (key === undefined) store.clear()
      else store.delete(key)
    }
    await done
  } catch {
    // Access still requires a fresh server check even if local cleanup fails.
  } finally {
    db?.close()
  }
}
