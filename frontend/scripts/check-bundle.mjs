// Count the complete static entry graph, including any newly extracted vendors.
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { gzipSync } from 'node:zlib'

const output = new URL('../dist/', import.meta.url)
const manifest = JSON.parse(await readFile(new URL('.vite/manifest.json', output), 'utf8'))
const initial = new Set()
function visit(key) {
  if (initial.has(key)) return
  assert(manifest[key], `Missing build entry: ${key}`)
  initial.add(key)
  for (const dependency of manifest[key].imports || []) visit(dependency)
}
const entries = Object.keys(manifest).filter((key) => manifest[key].isEntry)
assert.equal(entries.length, 1, 'Expected one public web entry.')
visit(entries[0])
let bytes = 0,
  gzip = 0
for (const key of initial) {
  assert(
    !/src\/(TeacherApp\.tsx|data\/firebase\.ts)$/.test(key),
    'Teacher/Firebase code must load on demand.',
  )
  const file = manifest[key].file
  if (!file.endsWith('.js')) continue
  const content = await readFile(new URL(file, output))
  bytes += content.length
  gzip += gzipSync(content).length
}
assert(bytes <= 420_000, `Startup JavaScript exceeds 420 kB: ${bytes} bytes.`)
assert(gzip <= 125_000, `Compressed startup JavaScript exceeds 125 kB: ${gzip} bytes.`)
console.log(
  `Public startup JavaScript: ${(bytes / 1000).toFixed(2)} kB / ${(gzip / 1000).toFixed(2)} kB gzip; budgets passed.`,
)
