import { deepEqual, equal, ok } from 'node:assert/strict'
import { health } from './handler.ts'

const endpoint = 'https://health-test.supabase.co/functions/v1/crest-health'
const testEnv: Record<string, string> = {
  SUPABASE_URL: 'https://health-test.supabase.co',
  SUPABASE_SECRET_KEYS: JSON.stringify({ default: 'sb_secret_health_test' }),
  CREST_SUPABASE_KEY_NAME: 'default',
  SUPABASE_SERVICE_ROLE_KEY: '',
}

async function withDatabase(fetcher: typeof fetch, test: () => Promise<void>) {
  const originalFetch = globalThis.fetch
  const originalEnv = Object.fromEntries(Object.keys(testEnv).map(name => [name, Deno.env.get(name)]))
  try {
    for (const [name, value] of Object.entries(testEnv)) Deno.env.set(name, value)
    globalThis.fetch = fetcher
    await test()
  } finally {
    globalThis.fetch = originalFetch
    for (const [name, value] of Object.entries(originalEnv)) {
      if (value === undefined) Deno.env.delete(name)
      else Deno.env.set(name, value)
    }
  }
}

Deno.test('GET reads at most one ID using server credentials and exposes only health', async () => {
  let reads = 0
  await withDatabase((input, init) => {
    reads++
    const url = new URL(String(input))
    equal(url.pathname, '/rest/v1/crest_storage_files')
    equal(url.searchParams.get('select'), 'id')
    equal(url.searchParams.get('limit'), '1')
    equal(init?.method, 'GET')
    equal(new Headers(init?.headers).get('apikey'), 'sb_secret_health_test')
    equal(new Headers(init?.headers).get('Authorization'), null)
    ok(init?.signal)
    return Promise.resolve(Response.json([{ id: 'private-file-id' }]))
  }, async () => {
    const response = await health(new Request(endpoint))
    equal(response.status, 200)
    equal(response.headers.get('Cache-Control'), 'no-store')
    const body = await response.json()
    deepEqual(Object.keys(body).sort(), ['checkedAt', 'database', 'status'])
    equal(body.status, 'ok')
    equal(body.database, 'ok')
    ok(Number.isFinite(Date.parse(body.checkedAt)))
    equal(reads, 1)
  })
})

Deno.test('an empty library is healthy and HEAD still reads the database', async () => {
  let reads = 0
  await withDatabase(() => {
    reads++
    return Promise.resolve(Response.json([]))
  }, async () => {
    const response = await health(new Request(endpoint, { method: 'HEAD' }))
    equal(response.status, 200)
    equal(await response.text(), '')
    equal(reads, 1)
  })
})

Deno.test('unsupported methods do not touch the database', async () => {
  await withDatabase(() => { throw new Error('Database must not be called') }, async () => {
    for (const method of ['POST', 'PUT', 'DELETE', 'OPTIONS']) {
      const response = await health(new Request(endpoint, { method }))
      equal(response.status, 405)
      equal(response.headers.get('Allow'), 'GET, HEAD')
    }
  })
})

Deno.test('database errors produce 503 without exposing server error details', async () => {
  await withDatabase(() => Promise.resolve(Response.json({ message: 'private server detail' }, { status: 500 })), async () => {
    const response = await health(new Request(endpoint))
    equal(response.status, 503)
    const body = await response.json()
    equal(body.status, 'unavailable')
    equal(body.database, 'unavailable')
    ok(!JSON.stringify(body).includes('private server detail'))
  })
})

Deno.test('connection failures produce 503, including bodyless HEAD responses', async () => {
  await withDatabase(() => { throw new TypeError('private connection detail') }, async () => {
    const response = await health(new Request(endpoint, { method: 'HEAD' }))
    equal(response.status, 503)
    equal(await response.text(), '')
  })
})

Deno.test('missing server configuration fails closed before any database request', async () => {
  await withDatabase(() => { throw new Error('Database must not be called') }, async () => {
    Deno.env.set('SUPABASE_SECRET_KEYS', '{}')
    const response = await health(new Request(endpoint))
    equal(response.status, 503)
    equal((await response.json()).status, 'unavailable')
  })
})
