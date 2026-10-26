// Read-only checks of the deployed indexes and CREST's actual query shapes.
// Reuses the Firebase CLI login; never prints tokens or school records.
import { createRequire } from 'node:module'
import { readFile, writeFile, mkdir } from 'node:fs/promises'
import { dirname, join, resolve } from 'node:path'

const args = process.argv.slice(2)
const option = (name, fallback) => {
  const index = args.indexOf(name)
  if (index < 0) return fallback
  if (!args[index + 1] || args[index + 1].startsWith('--')) throw new Error(`Missing ${name}`)
  return args[index + 1]
}
const project = option('--project', JSON.parse(await readFile('.firebaserc', 'utf8')).projects.default)
if (!/^[a-z][a-z0-9-]+$/.test(project)) throw new Error('Invalid Firebase project ID.')
const require = createRequire(import.meta.url)
const cli = option('--firebase-tools-dir', process.env.APPDATA ? join(process.env.APPDATA, 'npm/node_modules/firebase-tools') : '')
const auth = cli ? require(join(resolve(cli), 'lib/auth.js')) : require('firebase-tools/lib/auth.js')
const account = auth.getProjectDefaultAccount(process.cwd())
if (!account) throw new Error('Sign in using the Firebase CLI first.')
const tokens = account.tokens.expires_at > Date.now() + 60000 ? account.tokens :
  await auth.getAccessToken(account.tokens.refresh_token, ['https://www.googleapis.com/auth/cloud-platform'])
const root = `https://firestore.googleapis.com/v1/projects/${project}/databases/(default)`
async function request(path, body) {
  const response = await fetch(`${root}/${path}`, {
    method: body ? 'POST' : 'GET',
    headers: { Authorization: `Bearer ${tokens.access_token}`, 'Content-Type': 'application/json' },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  if (!response.ok) {
    const status = (await response.json().catch(() => ({}))).error?.status || 'UNKNOWN'
    throw new Error(`Firestore audit ${path.split('?')[0]} failed (HTTP ${response.status}, ${status}).`)
  }
  return response.json()
}
const report = { project, checkedAt: new Date().toISOString(), indexes: [], fields: [], queries: [] }
// Configuration responses contain field names and index states, never record values.
for (const [resource, target] of [['indexes', report.indexes], ['fields', report.fields]]) {
  let pageToken
  do {
    const query = new URLSearchParams(resource === 'fields' ? { filter: 'indexConfig.usesAncestorConfig=false' } : {})
    if (pageToken) query.set('pageToken', pageToken)
    const page = await request(`collectionGroups/-/${resource}?${query}`)
    target.push(...(page[resource] || []))
    pageToken = page.nextPageToken
  } while (pageToken)
}
const output = resolve(option('--output', 'state/backend-migration/firestore-optimization/audit.json'))
await mkdir(dirname(output), { recursive: true })
// Preserve configuration diagnostics even if a query or readiness check fails.
await writeFile(output, JSON.stringify(report, null, 2) + '\n')
if (args.includes('--verify-config')) {
  const expected = JSON.parse(await readFile('firebase/firestore.indexes.json', 'utf8'))
  const modes = indexes => (indexes || []).map(index => {
    const field = index.fields?.find(field => field.fieldPath !== '__name__') || index
    return `${index.queryScope}:${field.order || field.arrayConfig}`
  }).sort().join(',')
  const failures = []
  for (const field of expected.fieldOverrides) {
    const suffix = `/collectionGroups/${field.collectionGroup}/fields/${field.fieldPath}`
    const deployed = report.fields.find(item => item.name.endsWith(suffix))
    if (!deployed || modes(deployed.indexConfig?.indexes) !== modes(field.indexes)
      || (deployed.indexConfig?.indexes || []).some(index => index.state !== 'READY')) {
      failures.push(`${field.collectionGroup}.${field.fieldPath}`)
    }
  }
  const compositeKey = index => JSON.stringify({
    collection: index.collectionGroup || index.name.split('/collectionGroups/')[1].split('/')[0],
    scope: index.queryScope,
    fields: index.fields.filter(field => field.fieldPath !== '__name__'),
  })
  const actual = new Map(report.indexes.map(index => [compositeKey(index), index.state]))
  if (actual.size !== expected.indexes.length || expected.indexes.some(index => actual.get(compositeKey(index)) !== 'READY')) {
    failures.push('composite indexes')
  }
  if (failures.length) throw new Error(`Index configuration is different or still building: ${failures.join(', ')}.`)
  report.configurationVerified = true
}
const paths = ['users/user_details/students', 'users/user_details/teachers', 'groups',
  'researches/research_details/qualitative', 'researches/research_details/quantitative', 'documents']
const filter = (fieldPath, value) => ({ fieldFilter: { field: { fieldPath }, op: 'EQUAL', value } })
const checks = []
for (const path of paths) {
  checks.push({ name: `${path}:count`, path, count: true })
  if (path.includes('/qualitative') || path.includes('/quantitative') || path === 'documents') {
    checks.push({ name: `${path}:recent-page`, path, orderBy: [{ field: { fieldPath: 'createdAt' }, direction: 'DESCENDING' }], limit: 15 })
  }
  if (path.includes('/qualitative') || path.includes('/quantitative')) {
    checks.push({ name: `${path}:strand-count`, path, count: true, where: filter('strand', { stringValue: 'STEM' }) })
  }
  if (path === 'groups' || path === 'documents' || path.startsWith('researches/')) {
    checks.push({ name: `${path}:file-access`, path, where: filter('file_link', { stringValue: '__crest_audit_missing__' }) })
  }
}
checks.push(
  { name: 'students:pending-count', path: paths[0], count: true, where: filter('accepted', { booleanValue: false }) },
  { name: 'teachers:pending-count', path: paths[1], count: true, where: filter('access', { booleanValue: false }) },
  { name: 'teachers:notification-recipients', path: paths[1], where: filter('access', { booleanValue: true }) },
  { name: 'groups:pending-count', path: 'groups', count: true, where: { compositeFilter: { op: 'AND', filters: [
    filter('uploaded', { booleanValue: true }), filter('accepted_research', { booleanValue: false }),
  ] } } },
  { name: 'groups:name-order', path: 'groups', orderBy: [{ field: { fieldPath: 'group_name' }, direction: 'ASCENDING' }] },
)
for (const { name, path, count, ...options } of (args.includes('--config-only') ? [] : checks)) {
  const parts = path.split('/')
  const collectionId = parts.pop()
  const parent = parts.length ? `/${parts.join('/')}` : ''
  const structuredQuery = { from: [{ collectionId }], ...options }
  // Execute aggregation queries for exact counts; plan document queries without
  // fetching their results. Query Explain requires Google IAM, not a client token.
  const response = await request(`documents${parent}:${count ? 'runAggregationQuery' : 'runQuery'}`, {
    ...(count ? { structuredAggregationQuery: { structuredQuery, aggregations: [{ alias: 'total', count: {} }] } } : { structuredQuery }),
    explainOptions: { analyze: !!count },
  })
  const metrics = response.find(row => row.explainMetrics)?.explainMetrics
  const total = count ? response.find(row => row.result)?.result?.aggregateFields?.total?.integerValue : undefined
  report.queries.push({ name, ...(total !== undefined ? { count: Number(total) } : {}),
    indexesUsed: metrics?.planSummary?.indexesUsed || [], ...(metrics?.executionStats ? { executionStats: metrics.executionStats } : {}) })
}
await writeFile(output, JSON.stringify(report, null, 2) + '\n')
console.log(JSON.stringify({ project, queriesPassed: report.queries.length, indexes: report.indexes.length,
  fieldConfigurations: report.fields.length, configurationVerified: report.configurationVerified || false,
  counts: report.queries.filter(q => q.count !== undefined).map(q => ({ query: q.name, count: q.count })), output }, null, 2))
