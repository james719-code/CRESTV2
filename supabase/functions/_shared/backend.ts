import { createClient } from '@supabase/supabase-js'
import { createRemoteJWKSet, importPKCS8, jwtVerify, SignJWT } from 'jose'

export class HttpError extends Error {
  constructor(public status: number, message: string) { super(message) }
}

export function required(name: string): string {
  const value = Deno.env.get(name)?.trim()
  if (!value) throw new HttpError(503, 'The school connection has not been configured yet.')
  return value
}

export function admin() {
  const keys = JSON.parse(Deno.env.get('SUPABASE_SECRET_KEYS') || '{}') as Record<string, string>
  const key = keys[Deno.env.get('CREST_SUPABASE_KEY_NAME') || 'default'] || Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')
  assert(key, 503, 'The school storage server is not configured.')
  return createClient(required('SUPABASE_URL'), key, {
    auth: { persistSession: false, autoRefreshToken: false },
    global: { fetch: (input, init) => {
      // Opaque secret keys belong on apikey, not in a JWT bearer header.
      const headers = new Headers(init?.headers)
      if (key.startsWith('sb_secret_') && headers.get('Authorization') === `Bearer ${key}`) headers.delete('Authorization')
      return fetch(input, { ...init, headers })
    } },
  })
}

export function assert(condition: unknown, status: number, message: string): asserts condition {
  if (!condition) throw new HttpError(status, message)
}

export async function serve(
  request: Request,
  feature: string,
  action: (body: Record<string, unknown>, user: User) => Promise<unknown>,
) {
  const origin = request.headers.get('Origin')
  const allowed = (Deno.env.get('CREST_ALLOWED_ORIGINS') || '').split(',').map(v => v.trim())
  const headers = new Headers({ 'Content-Type': 'application/json', 'Cache-Control': 'no-store', Vary: 'Origin' })
  if (origin && allowed.includes(origin)) headers.set('Access-Control-Allow-Origin', origin)
  headers.set('Access-Control-Allow-Headers', 'authorization, content-type, apikey, x-client-info')
  headers.set('Access-Control-Allow-Methods', 'POST, OPTIONS')
  try {
    assert(!origin || allowed.includes(origin), 403, 'This website is not authorized.')
    if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers })
    assert(request.method === 'POST', 405, 'Use POST.')
    assert(Deno.env.get(feature) === 'true', 503, 'This school service is not enabled yet.')
    const text = await request.text()
    assert(text.length <= 16384, 413, 'Request is too large.')
    let body: Record<string, unknown>
    try { body = JSON.parse(text) } catch { throw new HttpError(400, 'Invalid JSON.') }
    assert(body && typeof body === 'object' && !Array.isArray(body), 400, 'Invalid request.')
    const user = await authenticate(request)
    return new Response(JSON.stringify(await action(body, user)), { headers })
  } catch (error) {
    const known = error instanceof HttpError
    // Never log credentials, device tokens, signed URLs, or student records.
    if (!known) console.error('CREST backend operation failed.')
    return new Response(JSON.stringify({ error: known ? error.message : 'The school service could not complete this request.' }), {
      status: known ? error.status : 500, headers,
    })
  }
}

const googleKeys = createRemoteJWKSet(new URL('https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com'))
let oauth: { value: string; expires: number } | undefined

export async function googleAccessToken(): Promise<string> {
  if (oauth && oauth.expires > Date.now() + 60000) return oauth.value
  const account = JSON.parse(required('CREST_FIREBASE_SERVICE_ACCOUNT_JSON'))
  assert(account.project_id === required('CREST_FIREBASE_PROJECT_ID'), 503, 'Firebase project configuration does not match.')
  const key = await importPKCS8(account.private_key, 'RS256')
  const assertion = await new SignJWT({ scope: 'https://www.googleapis.com/auth/cloud-platform https://www.googleapis.com/auth/firebase.messaging https://www.googleapis.com/auth/identitytoolkit' })
    .setProtectedHeader({ alg: 'RS256' }).setIssuer(account.client_email)
    .setAudience('https://oauth2.googleapis.com/token').setIssuedAt().setExpirationTime('1h').sign(key)
  const response = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST', body: new URLSearchParams({ grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer', assertion }),
  })
  assert(response.ok, 503, 'The school server could not authenticate.')
  const result = await response.json()
  oauth = { value: result.access_token, expires: Date.now() + result.expires_in * 1000 }
  return oauth.value
}

export type User = { uid: string; teacher: boolean; student: boolean; registered: boolean }

async function authenticate(request: Request): Promise<User> {
  const token = request.headers.get('Authorization')?.match(/^Bearer (\S+)$/)?.[1]
  assert(token, 401, 'Sign in to continue.')
  const project = required('CREST_FIREBASE_PROJECT_ID')
  let payload
  try {
    const verified = await jwtVerify(token, googleKeys, {
      algorithms: ['RS256'], issuer: `https://securetoken.google.com/${project}`, audience: project,
    })
    payload = verified.payload
    const now = Math.floor(Date.now() / 1000)
    assert(payload.sub && payload.sub.length <= 128 && typeof payload.auth_time === 'number' && payload.auth_time <= now &&
      typeof payload.iat === 'number' && payload.iat <= now && typeof payload.exp === 'number', 401, 'Invalid sign-in session.')
  } catch { throw new HttpError(401, 'Sign in again to continue.') }
  // Check disabled accounts and token revocation as well as the JWT signature.
  const response = await fetch(`https://identitytoolkit.googleapis.com/v1/projects/${project}/accounts:lookup`, {
    method: 'POST', headers: { Authorization: `Bearer ${await googleAccessToken()}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ localId: [payload.sub] }),
  })
  assert(response.ok, 503, 'Account status could not be verified.')
  const account = (await response.json()).users?.[0]
  assert(account && !account.disabled && Number(account.validSince || 0) <= Number(payload.auth_time), 401, 'Sign in again to continue.')
  const uid = payload.sub!
  const [teacher, student] = await Promise.all([
    readDocument(`${paths.teachers}/${uid}`), readDocument(`${paths.students}/${uid}`),
  ])
  return { uid, teacher: teacher?.data.access === true, student: student?.data.accepted === true, registered: !!(teacher || student) }
}

export const paths = {
  teachers: 'users/user_details/teachers', students: 'users/user_details/students', groups: 'groups',
  qualitative: 'researches/research_details/qualitative', quantitative: 'researches/research_details/quantitative', documents: 'documents',
}

type Field = { stringValue?: string; booleanValue?: boolean; integerValue?: string; doubleValue?: number; arrayValue?: { values?: Field[] }; nullValue?: null }
function decode(value: Field): unknown {
  if ('stringValue' in value) return value.stringValue
  if ('booleanValue' in value) return value.booleanValue
  if ('integerValue' in value) return Number(value.integerValue)
  if ('doubleValue' in value) return value.doubleValue
  if ('arrayValue' in value) return (value.arrayValue?.values || []).map(decode)
  return null
}
export type RecordData = { id: string; data: Record<string, unknown>; version: string }
function record(value: { name: string; fields?: Record<string, Field>; updateTime: string }): RecordData {
  return { id: value.name.split('/').at(-1)!, data: Object.fromEntries(Object.entries(value.fields || {}).map(([k, v]) => [k, decode(v)])), version: value.updateTime }
}
function firestoreBase() {
  return `https://firestore.googleapis.com/v1/projects/${required('CREST_FIREBASE_PROJECT_ID')}/databases/(default)/documents`
}
export async function readDocument(path: string): Promise<RecordData | null> {
  const response = await fetch(`${firestoreBase()}/${path.split('/').map(encodeURIComponent).join('/')}`, {
    headers: { Authorization: `Bearer ${await googleAccessToken()}` },
  })
  if (response.status === 404) return null
  assert(response.ok, 503, 'School permissions could not be checked.')
  return record(await response.json())
}
export async function findDocuments(path: string, field: string, value: string | boolean): Promise<RecordData[]> {
  const parts = path.split('/')
  const collectionId = parts.pop()!
  const parent = parts.length ? `/${parts.join('/')}` : ''
  const response = await fetch(`${firestoreBase()}${parent}:runQuery`, {
    method: 'POST', headers: { Authorization: `Bearer ${await googleAccessToken()}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ structuredQuery: {
      from: [{ collectionId }], where: { fieldFilter: { field: { fieldPath: field }, op: 'EQUAL', value: typeof value === 'boolean' ? { booleanValue: value } : { stringValue: value } } },
    } }),
  })
  assert(response.ok, 503, 'School permissions could not be checked.')
  const rows = await response.json()
  return rows.filter((row: { document?: unknown }) => row.document).map((row: { document: Parameters<typeof record>[0] }) => record(row.document))
}
export function member(group: Record<string, unknown>, uid: string): boolean {
  return Array.isArray(group.group_member) && group.group_member.includes(uid)
}
export function identifier(value: unknown): string {
  assert(typeof value === 'string' && /^[A-Za-z0-9_-]{1,128}$/.test(value), 400, 'Invalid record reference.')
  return value
}
export async function sha256(value: string): Promise<string> {
  const bytes = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value))
  return Array.from(new Uint8Array(bytes), n => n.toString(16).padStart(2, '0')).join('')
}
