/** Prepared migration tool. No automatic execution, source deletion, or metadata backfill.
 * Node 22+. Explicit --plan or --copy required; see docs/BACKEND_SETUP.md.
 */
import { readFile, writeFile, mkdir } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { createHash, randomUUID } from 'node:crypto'

const args = process.argv.slice(2)
const plan = args.includes('--plan')
const copy = args.includes('--copy')
const option = (name, fallback) => {
  const index = args.indexOf(name)
  if (index < 0) return fallback
  if (!args[index + 1] || args[index + 1].startsWith('--')) throw new Error(`Missing ${name} value.`)
  return args[index + 1]
}
const hash = bytes => createHash('sha256').update(bytes).digest('hex')
const segment = value => encodeURIComponent(value)

async function main() {
  if (!plan && !copy) {
    console.log('Prepared only. Later: node scripts/migrate-appwrite-storage.mjs --plan|--copy --manifest state/backend-migration/manifest.json')
    return
  }
  if (plan && copy) throw new Error('Choose --plan or --copy separately.')
  const envFile = resolve(option('--env-file', '.env.backend-migration.local'))
  const config = { ...process.env }
  const content = await readFile(envFile, 'utf8')
  for (const line of content.split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Z][A-Z0-9_]*)\s*=\s*(.*?)\s*$/)
    if (match && !config[match[1]]) config[match[1]] = match[2].replace(/^(["'])(.*)\1$/, '$2')
  }
  const need = name => { if (!config[name]?.trim()) throw new Error(`Configure ${name} locally.`); return config[name].trim() }
  const secure = value => {
    const url = new URL(value)
    if (url.protocol !== 'https:' || url.username || url.password || url.search || url.hash) throw new Error('Use HTTPS project URLs without credentials or query strings.')
    return url.toString().replace(/\/$/, '')
  }
  const source = { endpoint: secure(need('APPWRITE_ENDPOINT')), project: need('APPWRITE_PROJECT_ID'), bucket: need('APPWRITE_BUCKET_ID') }
  const destination = { url: secure(need('SUPABASE_URL')), bucket: need('SUPABASE_STORAGE_BUCKET') }
  const sourceHeaders = { 'X-Appwrite-Project': source.project, 'X-Appwrite-Key': need('APPWRITE_API_KEY') }
  const filesUrl = `${source.endpoint}/storage/buckets/${segment(source.bucket)}/files`
  const manifestPath = resolve(option('--manifest', 'state/backend-migration/manifest.json'))
  async function checked(url, options = {}, allowMissing = false) {
    const response = await fetch(url, { ...options, redirect: 'error' })
    if (allowMissing && response.status === 404) return response
    if (!response.ok) throw new Error(`Storage request failed (HTTP ${response.status}); no source files were changed.`)
    return response
  }

  if (plan) {
    const files = []
    let cursor
    while (true) {
      const query = new URL(filesUrl)
      query.searchParams.append('queries[]', JSON.stringify({ method: 'limit', values: [100] }))
      if (cursor) query.searchParams.append('queries[]', JSON.stringify({ method: 'cursorAfter', values: [cursor] }))
      const response = await checked(query, { headers: sourceHeaders })
      const page = await response.json()
      if (!Array.isArray(page.files)) throw new Error('Unexpected Appwrite listing response.')
      for (const file of page.files) {
        if (file.mimeType !== 'application/pdf' || file.sizeOriginal < 1 || file.sizeOriginal > 20 * 1024 * 1024)
          throw new Error('A source object is outside the prepared PDF/20 MB policy. Review the source bucket before copying.')
        const id = `sb_${randomUUID()}`
        files.push({ id, legacy_file_id: file.$id, object_path: `migration/appwrite/${id}.pdf`, size_bytes: file.sizeOriginal, source_updated_at: file.$updatedAt, source_permissions: file.$permissions })
      }
      if (page.files.length < 100) break
      const next = page.files.at(-1).$id
      if (next === cursor) throw new Error('Appwrite listing did not advance.')
      cursor = next
    }
    await mkdir(dirname(manifestPath), { recursive: true })
    // Exclusive creation prevents replacing a reviewed plan by accident.
    await writeFile(manifestPath, JSON.stringify({ version: 1, source, destination, created_at: new Date().toISOString(), files }, null, 2), { flag: 'wx', mode: 0o600 })
    console.log(`Plan saved: ${files.length} PDFs. Review the manifest before --copy. No destination writes were made.`)
    return
  }

  const manifest = JSON.parse(await readFile(manifestPath, 'utf8'))
  if (manifest.version !== 1 || JSON.stringify(manifest.source) !== JSON.stringify(source) || JSON.stringify(manifest.destination) !== JSON.stringify(destination))
    throw new Error('The reviewed manifest does not match the configured source and destination.')
  if (!Array.isArray(manifest.files)) throw new Error('Invalid manifest.')
  const key = need('SUPABASE_SECRET_KEY')
  const destinationHeaders = key.startsWith('sb_secret_')
    ? { apikey: key }
    : { apikey: key, Authorization: `Bearer ${key}` }
  const bucket = await (await checked(`${destination.url}/storage/v1/bucket/${segment(destination.bucket)}`, { headers: destinationHeaders })).json()
  if (bucket.public !== false) throw new Error('The destination bucket must be private. No files were copied.')
  const receiptPath = `${manifestPath}.verified.json`
  let receipt = { version: 1, files: {} }
  try { receipt = JSON.parse(await readFile(receiptPath, 'utf8')) }
  catch (error) { if (error.code !== 'ENOENT') throw error }
  let verified = 0
  let failed = 0
  for (const file of manifest.files) {
    try {
      if (!/^sb_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(file.id)
        || !/^[A-Za-z0-9_-]{1,128}$/.test(file.legacy_file_id)
        || file.object_path !== `migration/appwrite/${file.id}.pdf`
        || !Number.isInteger(file.size_bytes) || file.size_bytes < 1 || file.size_bytes > 20 * 1024 * 1024)
        throw new Error('Invalid planned object.')
      const metadata = await (await checked(`${filesUrl}/${segment(file.legacy_file_id)}`, { headers: sourceHeaders })).json()
      if (metadata.$updatedAt !== file.source_updated_at || metadata.sizeOriginal !== file.size_bytes)
        throw new Error('Source changed after planning; create and review a new manifest.')
      const bytes = Buffer.from(await (await checked(`${filesUrl}/${segment(file.legacy_file_id)}/download`, { headers: sourceHeaders })).arrayBuffer())
      if (bytes.length !== file.size_bytes || bytes.subarray(0, 5).toString('ascii') !== '%PDF-') throw new Error('Source content verification failed.')
      const digest = hash(bytes)
      const registryUrl = new URL(`${destination.url}/rest/v1/crest_storage_files`)
      registryUrl.searchParams.set('or', `(id.eq.${file.id},legacy_file_id.eq.${file.legacy_file_id})`)
      const rows = await (await checked(registryUrl, { headers: destinationHeaders })).json()
      if (rows.length > 1 || rows.some(row => row.id !== file.id || row.bucket !== destination.bucket || row.object_path !== file.object_path || row.sha256 !== digest))
        throw new Error('A conflicting destination mapping already exists. Review it manually.')
      const objectBase = `${destination.url}/storage/v1/object`
      const objectSuffix = `${segment(destination.bucket)}/${file.object_path.split('/').map(segment).join('/')}`
      let stored = await checked(`${objectBase}/authenticated/${objectSuffix}`, { headers: destinationHeaders }, true)
      if (stored.status === 404) {
        await checked(`${objectBase}/${objectSuffix}`, {
          method: 'POST', headers: { ...destinationHeaders, 'Content-Type': 'application/pdf', 'x-upsert': 'false' }, body: bytes,
        })
        stored = await checked(`${objectBase}/authenticated/${objectSuffix}`, { headers: destinationHeaders })
      }
      const copiedBytes = Buffer.from(await stored.arrayBuffer())
      if (copiedBytes.length !== bytes.length || hash(copiedBytes) !== digest) throw new Error('Destination hash verification failed. No mapping was written.')
      if (!rows.length) await checked(`${destination.url}/rest/v1/crest_storage_files`, {
        method: 'POST', headers: { ...destinationHeaders, 'Content-Type': 'application/json', Prefer: 'return=minimal' },
        body: JSON.stringify({ id: file.id, legacy_file_id: file.legacy_file_id, bucket: destination.bucket, object_path: file.object_path, kind: 'migration', size_bytes: bytes.length, sha256: digest }),
      })
      receipt.files[file.id] = { sha256: digest, size_bytes: bytes.length, verified_at: new Date().toISOString() }
      await writeFile(receiptPath, JSON.stringify(receipt, null, 2), { mode: 0o600 })
      verified++
    } catch (error) {
      failed++
      // Public identifiers and controlled errors only; never dump service responses.
      console.error(`Copy failed for ${file.id}: ${error.message}`)
    }
  }
  console.log(`Verified: ${verified}; failed: ${failed}. Appwrite originals and Firestore references were retained.`)
  if (failed) process.exitCode = 1
}

main().catch(error => { console.error(error.message); process.exitCode = 1 })
