// Copy an already verified Appwrite export without contacting or changing Appwrite.
import { readFile, writeFile, mkdir } from 'node:fs/promises'
import { resolve, relative, isAbsolute } from 'node:path'
import { createHash } from 'node:crypto'
const args = process.argv.slice(2)
const option = (name, fallback) => { const i = args.indexOf(name); if (i < 0) return fallback; if (!args[i + 1] || args[i + 1].startsWith('--')) throw new Error(`Missing ${name}`); return args[i + 1] }
const sha = bytes => createHash('sha256').update(bytes).digest('hex')
const base = resolve(option('--source', 'appwrite-downloads'))
const env = Object.fromEntries((await readFile(option('--env-file', '.env.backend-migration.local'), 'utf8')).split(/\r?\n/).flatMap(line => {
  const m = line.match(/^\s*([A-Z][A-Z0-9_]*)\s*=\s*(.*?)\s*$/); return m ? [[m[1], m[2].replace(/^(["'])(.*)\1$/, '$2')]] : []
}))
const destination = new URL(env.SUPABASE_URL)
if (destination.protocol !== 'https:' || destination.username || destination.password || destination.search || destination.hash) throw new Error('Use a secure Supabase project URL.')
const bucket = env.SUPABASE_STORAGE_BUCKET || 'crest-files'
const source = JSON.parse(await readFile(resolve(base, 'manifest.json'), 'utf8'))
if (source.version !== 1 || !source.files) throw new Error('Invalid local export manifest.')
let groups = []
if (args.includes('--firestore-backup')) groups = JSON.parse(await readFile(option('--firestore-backup'), 'utf8')).groups || []
const files = []
for (const file of Object.values(source.files)) {
  const path = resolve(base, file.relative_path.replaceAll('\\', '/'))
  const rel = relative(base, path)
  if (rel.startsWith('..') || isAbsolute(rel)) throw new Error('Export path escapes the source directory.')
  if (file.status !== 'verified' || file.mime_type !== 'application/pdf' || !/^[A-Za-z0-9_-]{1,128}$/.test(file.file_id)) throw new Error('Only verified PDF exports with valid IDs are supported.')
  const bytes = await readFile(path)
  if (bytes.length !== file.expected_bytes || bytes.length > 20971520 || bytes.subarray(0, 5).toString() !== '%PDF-' || sha(bytes) !== file.sha256) throw new Error(`Local verification failed for ${file.file_id}.`)
  const hex = sha(`${source.source.project}/${file.bucket_id}/${file.file_id}`).slice(0, 32)
  const id = `sb_${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
  const groupRefs = groups.filter(g => g.fields?.file_link?.stringValue === file.file_id)
  if (groupRefs.length > 1) throw new Error('A private file is referenced by multiple groups; review ownership first.')
  files.push({ path, mapping: { id, legacy_file_id: file.file_id, bucket, object_path: `migration/appwrite/${id}.pdf`, kind: 'migration', group_id: groupRefs[0]?.name.split('/').at(-1) || null, size_bytes: bytes.length, sha256: file.sha256 } })
}
if (new Set(files.map(f => f.mapping.legacy_file_id)).size !== files.length) throw new Error('Duplicate legacy IDs across buckets require explicit resolution.')
console.log(`Verified ${files.length} local PDFs (${files.reduce((n, f) => n + f.mapping.size_bytes, 0)} bytes). Destination: ${destination.origin}/${bucket}.`)
if (!args.includes('--copy')) { console.log('Plan only. Pass --copy to upload and verify every object.'); process.exit(0) }
const key = env.SUPABASE_SECRET_KEY
if (!key) throw new Error('Configure SUPABASE_SECRET_KEY locally.')
const headers = { apikey: key, ...(key.startsWith('sb_secret_') ? {} : { Authorization: `Bearer ${key}` }) }
async function request(path, options = {}) {
  const response = await fetch(`${destination.origin}${path}`, { ...options, redirect: 'error', headers: { ...headers, ...options.headers }, signal: AbortSignal.timeout(120000) })
  if (!response.ok) throw new Error(`Destination HTTP ${response.status} for ${path.split('?')[0]}.`)
  return response
}
const info = await (await request(`/storage/v1/bucket/${encodeURIComponent(bucket)}`)).json()
if (info.public !== false || info.file_size_limit !== 20971520 || !info.allowed_mime_types?.includes('application/pdf')) throw new Error('Destination bucket must be private with the PDF/20 MB policy.')
await mkdir('state/backend-migration', { recursive: true })
const receipt = { project: destination.hostname.split('.')[0], bucket, files: [] }
for (const { path, mapping } of files) {
  const query = `/rest/v1/crest_storage_files?or=(id.eq.${mapping.id},legacy_file_id.eq.${mapping.legacy_file_id})`
  const rows = await (await request(query)).json()
  if (rows.length > 1 || rows.some(row => Object.entries(mapping).some(([k, v]) => row[k] !== v))) throw new Error(`Conflicting mapping for ${mapping.legacy_file_id}.`)
  const objectPath = `${encodeURIComponent(bucket)}/${mapping.object_path}`
  const bytes = await readFile(path)
  // Upload without overwrite, then verify destination contents even on reruns.
  const upload = await fetch(`${destination.origin}/storage/v1/object/${objectPath}`, { method: 'POST', headers: { ...headers, 'Content-Type': 'application/pdf', 'x-upsert': 'false' }, body: bytes, signal: AbortSignal.timeout(120000) })
  if (!upload.ok && ![400, 409].includes(upload.status)) throw new Error(`Upload HTTP ${upload.status}.`)
  const stored = Buffer.from(await (await request(`/storage/v1/object/authenticated/${objectPath}`)).arrayBuffer())
  if (stored.length !== bytes.length || sha(stored) !== mapping.sha256) throw new Error('Destination checksum mismatch. No mapping was created.')
  if (!rows.length) await request('/rest/v1/crest_storage_files', { method: 'POST', headers: { 'Content-Type': 'application/json', Prefer: 'return=minimal' }, body: JSON.stringify(mapping) })
  receipt.files.push({ ...mapping, verified_at: new Date().toISOString() })
  await writeFile('state/backend-migration/local-upload-receipt.json', JSON.stringify(receipt, null, 2), { mode: 0o600 })
  console.log(`Verified ${receipt.files.length}/${files.length}: ${mapping.legacy_file_id}`)
}
console.log('All destination checksums match. Existing Firestore file references remain valid through the legacy ID mapping.')
