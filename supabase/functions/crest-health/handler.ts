import { admin } from '../_shared/backend.ts'

// Public GET/HEAD endpoint. Only service status is returned; file metadata stays private.
export async function health(request: Request): Promise<Response> {
  const headers = new Headers({ 'Content-Type': 'application/json', 'Cache-Control': 'no-store' })
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    headers.set('Allow', 'GET, HEAD')
    return new Response(JSON.stringify({ error: 'Use GET or HEAD.' }), { status: 405, headers })
  }

  let healthy = false
  try {
    // A real, bounded database read creates activity even when the library is empty.
    // Do not request a count or fetch any PDF objects for a health check.
    const { error } = await admin().from('crest_storage_files').select('id').limit(1)
      .abortSignal(AbortSignal.timeout(10_000))
    healthy = !error
  } catch {
    // Configuration, connection and timeout details must not reach a public caller.
  }

  const body = JSON.stringify({
    status: healthy ? 'ok' : 'unavailable',
    database: healthy ? 'ok' : 'unavailable',
    checkedAt: new Date().toISOString(),
  })
  return new Response(request.method === 'HEAD' ? null : body, { status: healthy ? 200 : 503, headers })
}
