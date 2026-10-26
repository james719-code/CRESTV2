import { cachePdf, cachedPdf, pdfCacheKey, removeCachedPdf } from './pdf-cache.ts'

export async function validatePdf(blob: Blob): Promise<Blob> {
  if (
    !blob.size ||
    blob.size > 20 * 1024 * 1024 ||
    !blob.type.includes('pdf') ||
    (await blob.slice(0, 5).text()) !== '%PDF-'
  )
    throw new Error('This file is not a valid PDF of up to 20 MB. Ask the uploader to check it.')
  return blob
}

export function createPdfReader(options: {
  scope: () => string
  authorize: (id: string) => Promise<string>
  download: (url: string) => Promise<Blob>
}) {
  const pending = new Map<string, Promise<Blob>>()
  let generation = 0
  // Serialize saves with sign-out cleanup, including a save already in progress.
  let writes = Promise.resolve()
  const enqueue = (work: () => Promise<void>) => {
    writes = writes.then(work).catch(() => undefined)
    return writes
  }
  function read(fileId: string): Promise<Blob> {
    const scope = options.scope()
    const key = pdfCacheKey(scope, fileId)
    const existing = pending.get(key)
    if (existing) return existing
    const started = generation
    function checkSession() {
      if (generation !== started || options.scope() !== scope)
        throw new Error('Your sign-in has changed. Open the PDF again.')
    }
    const request = (async () => {
      let url: string
      try {
        // Signed URLs last 60 seconds. Never persist them or use local copies
        // to bypass current Firebase approval and file-membership checks.
        url = await options.authorize(fileId)
      } catch (error) {
        await enqueue(() => removeCachedPdf(key))
        throw error
      }
      checkSession()
      const cached = await cachedPdf(key)
      if (cached) {
        checkSession()
        try {
          await validatePdf(cached)
        } catch {
          await enqueue(() => removeCachedPdf(key))
          return download()
        }
        checkSession()
        return cached
      }
      return download()
      async function download() {
        checkSession()
        const blob = await validatePdf(await options.download(url))
        checkSession()
        await enqueue(async () => {
          if (generation === started && options.scope() === scope) await cachePdf(key, blob)
        })
        checkSession()
        return blob
      }
    })()
    pending.set(key, request)
    void request
      .finally(() => {
        if (pending.get(key) === request) pending.delete(key)
      })
      .catch(() => undefined)
    return request
  }
  return {
    read,
    clear: () => {
      generation++
      pending.clear()
      return enqueue(() => removeCachedPdf())
    },
  }
}
