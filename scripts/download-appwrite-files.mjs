/** Read-only Appwrite export. No Supabase/Firebase calls or source mutations. */
import { createReadStream, createWriteStream } from 'node:fs'
import { mkdir, readFile, writeFile, rename, stat } from 'node:fs/promises'
import { resolve, relative, isAbsolute, join, extname } from 'node:path'
import { createHash } from 'node:crypto'
import { Readable, Transform } from 'node:stream'
import { pipeline } from 'node:stream/promises'

const args = process.argv.slice(2)
function option(name, fallback) {
  const index = args.indexOf(name)
  if (index < 0) return fallback
  if (!args[index + 1] || args[index + 1].startsWith('--')) throw new Error(`Missing ${name} value.`)
  return args[index + 1]
}
const pause = ms => new Promise(done => setTimeout(done, ms))
const id = value => {
  if (typeof value !== 'string' || !/^[A-Za-z0-9][A-Za-z0-9._-]{0,35}$/.test(value)) throw new Error('Invalid Appwrite object identifier.')
  return value
}
function filename(name) {
  const safe = String(name || 'file').normalize('NFC').replace(/[<>:"/\\|?*\u0000-\u001f]/g, '_').replace(/[. ]+$/, '') || 'file'
  const extension = extname(safe).slice(0, 16)
  return safe.length <= 76 ? safe : `${safe.slice(0, 60)}${extension}`
}
async function fileHash(path) {
  const hash = createHash('sha256')
  for await (const chunk of createReadStream(path)) hash.update(chunk)
  return hash.digest('hex')
}

async function main() {
  const env = { ...process.env }
  const envPath = resolve(option('--env-file', '.env.backend-migration.local'))
  for (const line of (await readFile(envPath, 'utf8')).split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Z][A-Z0-9_]*)\s*=\s*(.*?)\s*$/)
    if (match && !env[match[1]]) env[match[1]] = match[2].replace(/^(["'])(.*)\1$/, '$2')
  }
  function need(name) {
    if (!env[name]?.trim()) throw new Error(`Configure ${name} in the ignored local settings file.`)
    return env[name].trim()
  }
  const endpoint = new URL(need('APPWRITE_ENDPOINT'))
  if (endpoint.protocol !== 'https:' || endpoint.username || endpoint.password || endpoint.search || endpoint.hash)
    throw new Error('Use an HTTPS Appwrite endpoint without credentials or query strings.')
  const source = { endpoint: endpoint.toString().replace(/\/$/, ''), project: id(need('APPWRITE_PROJECT_ID')) }
  const configuredBucket = id(need('APPWRITE_BUCKET_ID'))
  const headers = { 'X-Appwrite-Project': source.project, 'X-Appwrite-Key': need('APPWRITE_API_KEY') }
  const workspace = resolve('.')
  const output = resolve(option('--output', 'appwrite-downloads'))
  function within(root, path) {
    const rel = relative(root, path)
    if (!rel || rel === '..' || rel.startsWith(`..${process.platform === 'win32' ? '\\' : '/'}`) || isAbsolute(rel))
      throw new Error('The export target must stay inside the intended folder.')
    return path
  }
  within(workspace, output)
  const local = (...parts) => within(output, resolve(output, ...parts))

  async function get(path, timeout = 60000) {
    const url = new URL(`${source.endpoint}/${path}`)
    for (let attempt = 0; attempt < 4; attempt++) {
      let response
      try {
        response = await fetch(url, { headers, redirect: 'error', signal: AbortSignal.timeout(timeout) })
      } catch {
        if (attempt === 3) throw new Error('Appwrite could not be reached after four attempts.')
        await pause(1000 * (attempt + 1)); continue
      }
      if (response.ok) return response
      await response.body?.cancel()
      if ([429, 500, 502, 503, 504].includes(response.status) && attempt < 3) {
        await pause(1000 * (attempt + 1)); continue
      }
      if (response.status === 401 || response.status === 403)
        throw new Error(`Appwrite denied read access (HTTP ${response.status}). Check the API key and its files.read/buckets.read scopes.`)
      throw new Error(`Appwrite read failed (HTTP ${response.status}).`)
    }
  }
  async function list(path, field) {
    const values = new Map()
    let cursor
    let total = 0
    while (true) {
      const query = new URLSearchParams()
      query.append('queries[]', JSON.stringify({ method: 'limit', values: [100] }))
      query.append('queries[]', JSON.stringify({ method: 'orderAsc', attribute: '$id' }))
      if (cursor) query.append('queries[]', JSON.stringify({ method: 'cursorAfter', values: [cursor] }))
      const page = await (await get(`${path}?${query}`)).json()
      if (!Array.isArray(page[field]) || !Number.isInteger(page.total)) throw new Error('Unexpected Appwrite listing response.')
      total = page.total
      for (const item of page[field]) values.set(id(item.$id), item)
      if (page[field].length < 100) break
      const next = page[field].at(-1).$id
      if (next === cursor) throw new Error('The Appwrite listing did not advance.')
      cursor = next
    }
    if (values.size !== total) throw new Error('The source inventory changed during listing. Retry the export.')
    return [...values.values()]
  }

  const bucketPath = `storage/buckets/${encodeURIComponent(configuredBucket)}`
  const buckets = args.includes('--all-buckets')
    ? await list('storage/buckets', 'buckets')
    : [await (await get(bucketPath)).json()]
  if (!buckets.some(bucket => bucket.$id === configuredBucket)) throw new Error('The configured source bucket was not found in this project.')
  const inventory = []
  for (const bucket of buckets) {
    const files = await list(`storage/buckets/${encodeURIComponent(id(bucket.$id))}/files`, 'files')
    for (const file of files) {
      if (!Number.isSafeInteger(file.sizeOriginal) || file.sizeOriginal < 0) throw new Error('A source file has an invalid byte count.')
      inventory.push({ bucket, file })
    }
  }
  const expectedBytes = inventory.reduce((sum, item) => sum + item.file.sizeOriginal, 0)
  console.log(`Inventory: ${buckets.length} bucket(s), ${inventory.length} file(s), ${expectedBytes} bytes.`)
  await mkdir(output, { recursive: true })
  const manifestPath = local('manifest.json')
  let manifest = { version: 1, source, started_at: new Date().toISOString(), scope: args.includes('--all-buckets') ? 'all-project-buckets' : 'configured-bucket', buckets, files: {} }
  try {
    const previous = JSON.parse(await readFile(manifestPath, 'utf8'))
    if (previous.version !== 1 || JSON.stringify(previous.source) !== JSON.stringify(source)) throw new Error('This folder contains an export from another source. Select a new folder.')
    manifest = { ...previous, buckets, scope: manifest.scope }
  } catch (error) { if (error.code !== 'ENOENT') throw error }
  async function save() {
    const temporary = local('manifest.json.part')
    await writeFile(temporary, JSON.stringify(manifest, null, 2), { mode: 0o600 })
    await rename(temporary, manifestPath)
  }
  await save()
  let completed = 0
  let failed = 0
  let verifiedBytes = 0
  for (const { bucket, file } of inventory) {
    const key = `${bucket.$id}/${file.$id}`
    const previous = manifest.files[key]
    const version = createHash('sha256').update(String(file.$updatedAt)).digest('hex').slice(0, 8)
    const relativePath = join(`bucket-${id(bucket.$id)}`, `${id(file.$id)}_${version}__${filename(file.name)}`)
    const target = local(relativePath)
    const partial = local(`${relativePath}.part`)
    const record = {
      bucket_id: bucket.$id, file_id: file.$id, original_name: file.name, mime_type: file.mimeType,
      expected_bytes: file.sizeOriginal, source_updated_at: file.$updatedAt,
      source_md5: file.signature, permissions: file.$permissions,
      relative_path: relativePath, status: 'pending',
    }
    try {
      if (previous?.status === 'verified' && previous.source_updated_at === file.$updatedAt && previous.relative_path === relativePath) {
        const existing = await stat(target)
        if (existing.size === file.sizeOriginal && await fileHash(target) === previous.sha256) {
          completed++; verifiedBytes += existing.size
          console.log(`Verified ${completed}/${inventory.length} (existing local copy).`)
          continue
        }
        throw new Error('The existing local copy differs from its recorded checksum. It was retained without replacement.')
      }
      try { await stat(target); throw new Error('An unverified local file already exists. It was retained without replacement.') }
      catch (error) { if (error.code !== 'ENOENT') throw error }
      await mkdir(local(`bucket-${id(bucket.$id)}`), { recursive: true })
      let checksum
      let byteCount
      for (let attempt = 0; attempt < 3; attempt++) {
        try {
          const response = await get(`storage/buckets/${encodeURIComponent(bucket.$id)}/files/${encodeURIComponent(file.$id)}/download`, 300000)
          if (!response.body) throw new Error('The download response had no file body.')
          const sha256 = createHash('sha256')
          const md5 = createHash('md5')
          byteCount = 0
          const counter = new Transform({ transform(chunk, _encoding, callback) {
            byteCount += chunk.length; sha256.update(chunk); md5.update(chunk); callback(null, chunk)
          } })
          await pipeline(Readable.fromWeb(response.body), counter, createWriteStream(partial, { flags: 'w', mode: 0o600 }))
          checksum = sha256.digest('hex')
          const sourceChecksum = md5.digest('hex')
          if (byteCount !== file.sizeOriginal) throw new Error('Downloaded byte count does not match the source.')
          if (/^[a-f0-9]{32}$/i.test(file.signature || '') && sourceChecksum !== file.signature.toLowerCase())
            throw new Error('Downloaded MD5 does not match the source signature.')
          const current = await (await get(`storage/buckets/${encodeURIComponent(bucket.$id)}/files/${encodeURIComponent(file.$id)}`)).json()
          if (current.$updatedAt !== file.$updatedAt || current.sizeOriginal !== file.sizeOriginal) throw new Error('The source file changed during download.')
          break
        } catch (error) {
          if (attempt === 2 || /denied read access/.test(error.message)) throw error
          await pause(1000 * (attempt + 1))
        }
      }
      await rename(partial, target)
      if (await fileHash(target) !== checksum) throw new Error('Local disk checksum verification failed.')
      manifest.files[key] = { ...record, status: 'verified', sha256: checksum, downloaded_bytes: byteCount, verified_at: new Date().toISOString() }
      completed++; verifiedBytes += byteCount
      console.log(`Downloaded and verified ${completed}/${inventory.length}.`)
    } catch (error) {
      failed++
      manifest.files[key] = { ...record, status: 'failed', error: error.message }
      console.log(`Download ${completed + failed}/${inventory.length} failed: ${error.message}`)
    }
    await save()
  }
  let inventoryUnchanged = true
  for (const bucket of buckets) {
    const current = await list(`storage/buckets/${encodeURIComponent(bucket.$id)}/files`, 'files')
    const expected = new Map(inventory.filter(item => item.bucket.$id === bucket.$id).map(item => [item.file.$id, item.file.$updatedAt]))
    if (current.length !== expected.size || current.some(file => expected.get(file.$id) !== file.$updatedAt)) inventoryUnchanged = false
  }
  manifest.finished_at = new Date().toISOString()
  manifest.summary = { total_files: inventory.length, verified_files: completed, failed_files: failed, total_bytes: expectedBytes, verified_bytes: verifiedBytes, inventory_unchanged: inventoryUnchanged }
  await save()
  console.log(JSON.stringify({ output, ...manifest.summary }))
  if (failed || !inventoryUnchanged) process.exitCode = 1
}

main().catch(error => { console.error(error.message); process.exitCode = 1 })
