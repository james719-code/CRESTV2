import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { credits } from '../src/data/credits.ts'

const notices = readFileSync(
  new URL('../public/licenses/THIRD-PARTY-NOTICES.txt', import.meta.url),
  'utf8',
)
const manifest = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8'))

test('every runtime dependency is credited and has its license text', () => {
  const credited = new Set(credits.flatMap((credit) => credit.packages))
  for (const name of Object.keys(manifest.dependencies)) {
    assert(credited.has(name), `${name} needs an entry in src/data/credits.ts`)
    assert(notices.includes(`\n${name} `), `${name} is missing from THIRD-PARTY-NOTICES.txt`)
  }
})

test('the typeface and component sources are included in the notices', () => {
  assert(notices.includes('SIL OPEN FONT LICENSE Version 1.1'))
  assert(notices.includes('Copyright (c) 2023 shadcn'))
})
