import 'fake-indexeddb/auto'
import { beforeEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { cachePdf, cachedPdf, removeCachedPdf, pdfCacheKey, PDF_CACHE_TTL, PDF_CACHE_LIMIT } from '../src/data/pdf-cache.ts'
import { createPdfReader } from '../src/data/pdf-reader.ts'

const pdf = () => new Blob(['%PDF-1.4\nCREST cache test\n%%EOF'], { type: 'application/pdf' })
beforeEach(() => removeCachedPdf())

test('PDFs survive a new reader and expire after 30 days without sliding expiry', async () => {
  const key = pdfCacheKey('teacher-a', 'paper')
  await cachePdf(key, pdf(), 100)
  assert.equal(await (await cachedPdf(key, 101)).text(), await pdf().text())
  assert.ok(await cachedPdf(key, 100 + PDF_CACHE_TTL - 1))
  assert.equal(await cachedPdf(key, 100 + PDF_CACHE_TTL), undefined)
})

test('cache keys isolate users, projects, and file revisions', async () => {
  const key = pdfCacheKey('project-a/teacher-a', 'paper-v1')
  await cachePdf(key, pdf())
  for (const other of [pdfCacheKey('project-a/teacher-b', 'paper-v1'), pdfCacheKey('project-b/teacher-a', 'paper-v1'), pdfCacheKey('project-a/teacher-a', 'paper-v2')])
    assert.equal(await cachedPdf(other), undefined)
  assert.ok(await cachedPdf(key))
})

test('cache evicts the least recently viewed PDF at its 100 MiB cap', async () => {
  const file = new Blob([new Uint8Array(PDF_CACHE_LIMIT / 5)], { type: 'application/pdf' })
  for (let i = 0; i < 5; i++) await cachePdf(`paper-${i}`, file, 100 + i)
  assert.ok(await cachedPdf('paper-0', 110))
  await cachePdf('paper-5', file, 111)
  assert.equal(await cachedPdf('paper-1', 112), undefined)
  assert.ok(await cachedPdf('paper-0', 112))
  assert.ok(await cachedPdf('paper-5', 112))
})

test('expired entries are removed before evicting useful PDFs', async () => {
  const file = new Blob([new Uint8Array(PDF_CACHE_LIMIT / 2)], { type: 'application/pdf' })
  await cachePdf('old', file, 0)
  await cachePdf('fresh', file, PDF_CACHE_TTL - 10)
  await cachedPdf('old', PDF_CACHE_TTL - 1)
  await cachePdf('new', file, PDF_CACHE_TTL)
  assert.equal(await cachedPdf('old', PDF_CACHE_TTL), undefined)
  assert.ok(await cachedPdf('fresh', PDF_CACHE_TTL))
})

function reader(overrides = {}) {
  let approvals = 0, downloads = 0
  const options = {
    scope: () => 'teacher-a',
    authorize: async () => { approvals++; return 'https://private.test/short-lived-url' },
    download: async () => { downloads++; return pdf() },
    ...overrides,
  }
  return { ...createPdfReader(options), counts: () => ({ approvals, downloads }) }
}

test('reopening downloads once while every open rechecks current access', async () => {
  const api = reader()
  await api.read('paper')
  await api.read('paper')
  assert.deepEqual(api.counts(), { approvals: 2, downloads: 1 })
  const restored = reader()
  await restored.read('paper')
  assert.deepEqual(restored.counts(), { approvals: 1, downloads: 0 })
})

test('concurrent opens share a single authorization and download', async () => {
  const api = reader()
  await Promise.all([api.read('paper'), api.read('paper'), api.read('paper')])
  assert.deepEqual(api.counts(), { approvals: 1, downloads: 1 })
})

test('denied or offline authorization cannot open a cached private PDF and removes it', async () => {
  const key = pdfCacheKey('teacher-a', 'paper')
  for (const message of ['Access revoked', 'Network unavailable']) {
    await cachePdf(key, pdf())
    const api = reader({ authorize: async () => { throw new Error(message) } })
    await assert.rejects(api.read('paper'), new RegExp(message))
    assert.equal(await cachedPdf(key), undefined)
    assert.equal(api.counts().downloads, 0)
  }
})

test('failed, empty, or invalid PDF downloads are not cached and can be retried', async () => {
  for (const download of [async () => { throw new Error('Download failed') }, async () => new Blob([], { type: 'application/pdf' }), async () => new Blob(['<html>Denied</html>'], { type: 'application/pdf' })]) {
    const api = reader({ download })
    await assert.rejects(api.read('paper'))
    assert.equal(await cachedPdf(pdfCacheKey('teacher-a', 'paper')), undefined)
    await reader().read('paper')
    await removeCachedPdf()
  }
})

test('a corrupt stored PDF is replaced with a validated download', async () => {
  await cachePdf(pdfCacheKey('teacher-a', 'paper'), new Blob(['not a PDF'], { type: 'application/pdf' }))
  const api = reader()
  assert.equal(await (await api.read('paper')).text(), await pdf().text())
  assert.equal(api.counts().downloads, 1)
})

test('blocked browser storage still allows authorized PDFs to open', async () => {
  const original = globalThis.indexedDB
  globalThis.indexedDB = { open() { throw new Error('Storage unavailable') } }
  try {
    const api = reader()
    assert.equal(await (await api.read('paper')).text(), await pdf().text())
  } finally { globalThis.indexedDB = original }
})

test('sign-out clears cached PDFs and prevents an in-flight download restoring them', async () => {
  let finish
  const downloading = new Promise(resolve => { finish = resolve })
  const api = reader({ download: () => downloading })
  const pending = api.read('paper')
  const rejected = assert.rejects(pending, /sign-in has changed/)
  await api.clear()
  finish(pdf())
  await rejected
  assert.equal(await cachedPdf(pdfCacheKey('teacher-a', 'paper')), undefined)
  const fresh = reader()
  await fresh.read('paper')
  await fresh.clear()
  assert.equal(await cachedPdf(pdfCacheKey('teacher-a', 'paper')), undefined)
})

test('switching account during a download never returns or caches the previous user PDF', async () => {
  let scope = 'teacher-a'
  const api = reader({ scope: () => scope, download: async () => { scope = 'teacher-b'; return pdf() } })
  await assert.rejects(api.read('paper'), /sign-in has changed/)
  assert.equal(await cachedPdf(pdfCacheKey('teacher-a', 'paper')), undefined)
})
