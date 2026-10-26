import { edgeRequest, storageProvider } from './edge'
import { getAuth } from 'firebase/auth'
import { createPdfReader, validatePdf } from './pdf-reader'

const legacy = {
  endpoint: String(import.meta.env.VITE_APPWRITE_ENDPOINT || '').replace(/\/$/, ''),
  project: String(import.meta.env.VITE_APPWRITE_PROJECT_ID || ''),
  bucket: String(import.meta.env.VITE_APPWRITE_BUCKET_ID || ''),
}
const isSupabaseReference = (value: string) => /^sb_[0-9a-f-]{36}$/.test(value)

const pdfReader = createPdfReader({
  scope: () => {
    const auth = getAuth()
    if (!auth.currentUser) throw new Error('Sign in again to continue.')
    return JSON.stringify([auth.app.options.projectId, import.meta.env.VITE_SUPABASE_URL, auth.currentUser.uid])
  },
  authorize: async (fileId) => (await edgeRequest<{ url: string }>('crest-storage', { action: 'read-file', fileId })).url,
  download: async (url) => {
    const response = await fetch(url, { cache: 'no-store' })
    if (!response.ok) throw new Error('The PDF could not be opened. Check your connection and school file permissions.')
    return response.blob()
  },
})

export const clearSchoolFileCache = () => pdfReader.clear()

function fileEndpoint(fileId?: string) {
  if (!legacy.endpoint || !legacy.project || !legacy.bucket)
    throw new Error('Appwrite storage has not been configured.')
  if (new URL(legacy.endpoint).protocol !== 'https:') throw new Error('The storage address must use HTTPS.')
  return `${legacy.endpoint}/storage/buckets/${encodeURIComponent(legacy.bucket)}/files${fileId ? `/${encodeURIComponent(fileId)}` : ''}`
}

export async function readFile(fileId: string): Promise<Blob> {
  if (!['appwrite', 'supabase'].includes(storageProvider)) throw new Error('School file storage is not enabled yet.')
  let response: Response
  if (storageProvider === 'supabase' || isSupabaseReference(fileId)) {
    return pdfReader.read(fileId)
  } else if (storageProvider === 'appwrite') {
    response = await fetch(`${fileEndpoint(fileId)}/view`, { headers: { 'X-Appwrite-Project': legacy.project }, credentials: 'include' })
  } else throw new Error('School file storage is not enabled yet.')
  if (!response.ok) throw new Error('The PDF could not be opened. Check your connection and school file permissions.')
  return validatePdf(await response.blob())
}

export async function uploadFile(file: File, kind: 'document' | 'research'): Promise<string> {
  if (!file.size || file.size > 20 * 1024 * 1024 || (await file.slice(0, 5).text()) !== '%PDF-')
    throw new Error('Choose a PDF of up to 20 MB.')
  if (storageProvider === 'supabase') {
    const signed = await edgeRequest<{ fileId: string; url: string }>('crest-storage', { action: 'create-upload', kind, size: file.size })
    const response = await fetch(signed.url, { method: 'PUT', headers: { 'Content-Type': 'application/pdf' }, body: file })
    if (!response.ok) throw new Error('Your PDF could not be uploaded. Try again.')
    return signed.fileId
  }
  if (storageProvider !== 'appwrite') throw new Error('School file storage is not enabled yet.')
  const body = new FormData()
  body.append('fileId', 'unique()')
  body.append('file', file)
  const response = await fetch(fileEndpoint(), { method: 'POST', headers: { 'X-Appwrite-Project': legacy.project }, credentials: 'include', body })
  if (!response.ok) throw new Error('Your PDF could not be uploaded. Try again.')
  const result = await response.json()
  if (!result.$id) throw new Error('The file service did not return a reference.')
  return result.$id
}

export async function deleteFile(fileId: string): Promise<void> {
  if (!['appwrite', 'supabase'].includes(storageProvider)) return
  if (storageProvider === 'supabase' || isSupabaseReference(fileId)) {
    await edgeRequest('crest-storage', { action: 'delete-file', fileId })
  }
  // Keep Appwrite originals and detached uploads until a separate retention
  // policy is approved. This integration never deletes source objects.
}
