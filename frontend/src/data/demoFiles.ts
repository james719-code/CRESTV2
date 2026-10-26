function database(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('crest-demo-files', 1)
    request.onupgradeneeded = () => request.result.createObjectStore('files')
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(new Error('Your browser could not save this demo file.'))
  })
}

export async function saveDemoFile(id: string, file: File) {
  const db = await database()
  try {
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction('files', 'readwrite')
      tx.objectStore('files').put(file, id)
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(new Error('There is not enough browser storage for this file.'))
      tx.onabort = () => reject(new Error('The file could not be saved. Try a smaller PDF.'))
    })
  } finally {
    db.close()
  }
}

export async function readDemoFile(id: string): Promise<Blob | undefined> {
  const db = await database()
  try {
    return await new Promise((resolve, reject) => {
      const request = db.transaction('files', 'readonly').objectStore('files').get(id)
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(new Error('The demo file could not be opened.'))
    })
  } finally {
    db.close()
  }
}
