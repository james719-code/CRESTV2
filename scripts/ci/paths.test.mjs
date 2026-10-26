import { test, after } from 'node:test'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync, renameSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, dirname, basename, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { classifyPaths } from './paths.mjs'
import { changedPaths } from './git-changes.mjs'
import { shouldBuildWeb } from './netlify-ignore.mjs'

const none = { android: false, web: false, backend: false }
for (const [label, files, expected] of [
  ['Android source only', ['android/app/src/main/Example.kt'], { ...none, android: true }],
  ['web source only', ['frontend/src/App.tsx'], { ...none, web: true }],
  ['mixed app changes', ['android/settings.gradle.kts', 'frontend/src/App.tsx'], { ...none, android: true, web: true }],
  ['documentation anywhere', ['README.md', 'docs/image.png', 'android/README.md', 'frontend/README.md', 'branding/BRAND_GUIDE.md'], none],
  ['documentation and Android', ['docs/backend.md', 'android/app/build.gradle.kts'], { ...none, android: true }],
  ['Netlify settings only', ['netlify.toml'], { ...none, web: true }],
  ['shared design tokens', ['branding/tokens/brand.tokens.json'], { ...none, android: true, web: true }],
  ['generated web styles only', ['branding/generated/web/crest-tokens.css'], { ...none, web: true }],
  ['generated Android styles only', ['branding/generated/android/CrestBrandTokens.kt'], { ...none, android: true }],
  ['shared font source', ['branding/assets/fonts/Inter-Regular.ttf'], { ...none, android: true, web: true }],
  ['Firebase rules only', ['firebase/firestore.rules'], { ...none, backend: true }],
  ['Supabase function only', ['supabase/functions/crest-storage/index.ts'], { ...none, backend: true }],
  ['backend tests stored under frontend', ['frontend/backend-tests/firestore.rules.test.mjs'], { ...none, backend: true }],
  ['dependency changes affect web and rules tests', ['frontend/package-lock.json'], { ...none, web: true, backend: true }],
  ['CI configuration verifies every component', ['.github/workflows/ci.yml'], { android: true, web: true, backend: true }],
  ['Android release configuration only', ['.github/workflows/release.yml'], { ...none, android: true }],
  ['unrelated repository settings', ['.editorconfig', '.gitignore'], none],
]) test(label, () => assert.deepEqual(classifyPaths(files), expected))

const fixture = mkdtempSync(join(tmpdir(), 'crest-ci-'))
const git = args => execFileSync('git', args, { cwd: fixture, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim()
const put = (path, contents) => { mkdirSync(dirname(join(fixture, path)), { recursive: true }); writeFileSync(join(fixture, path), contents) }
const commit = message => {
  git(['add', '-A'])
  git(['-c', 'user.name=CREST CI test', '-c', 'user.email=ci@example.invalid', '-c', 'commit.gpgsign=false', '-c', `core.hooksPath=${join(fixture, 'empty-hooks')}`, 'commit', '-qm', message])
  return git(['rev-parse', 'HEAD'])
}
after(() => {
  // Only remove the exact temporary fixture created by this test process.
  assert.equal(resolve(dirname(fixture)), resolve(tmpdir()))
  assert.ok(basename(fixture).startsWith('crest-ci-'))
  rmSync(fixture, { recursive: true, force: true })
})
git(['init', '--quiet', '--initial-branch=main'])
put('android/app.kt', 'android\n')
put('frontend/src/app.ts', 'web\n')
put('README.md', 'docs\n')
const initial = commit('initial fixture')
put('README.md', 'updated docs\n')
const docsOnly = commit('documentation only')
put('android/app.kt', 'updated Android\n')
const androidOnly = commit('Android only')
put('frontend/src/app.ts', 'updated website\n')
const webOnly = commit('website only')

test('real Git diffs select the right app and include all commits in a push', () => {
  assert.deepEqual(classifyPaths(changedPaths(initial, docsOnly, { cwd: fixture })), none)
  assert.deepEqual(classifyPaths(changedPaths(docsOnly, androidOnly, { cwd: fixture })), { ...none, android: true })
  assert.deepEqual(classifyPaths(changedPaths(androidOnly, webOnly, { cwd: fixture })), { ...none, web: true })
  assert.deepEqual(classifyPaths(changedPaths(initial, webOnly, { cwd: fixture })), { ...none, android: true, web: true })
})
test('Netlify skips documentation/Android changes and builds changed web inputs', () => {
  const options = { cwd: join(fixture, 'frontend') }
  assert.equal(shouldBuildWeb(initial, docsOnly, options), false)
  assert.equal(shouldBuildWeb(docsOnly, androidOnly, options), false)
  assert.equal(shouldBuildWeb(androidOnly, webOnly, options), true)
})
test('first builds, explicit cache clears, and unavailable history build conservatively', () => {
  assert.equal(shouldBuildWeb('', webOnly, { cwd: fixture }), true)
  assert.equal(shouldBuildWeb(webOnly, webOnly, { cwd: fixture }), true)
  assert.equal(shouldBuildWeb('missing-commit', webOnly, { cwd: fixture }), true)
  assert.deepEqual(classifyPaths(changedPaths('0'.repeat(40), webOnly, { cwd: join(fixture, 'frontend') })), { ...none, android: true, web: true })
})
test('PR comparison uses the merge base and excludes base-branch-only changes', () => {
  git(['checkout', '-qb', 'feature', initial])
  put('README.md', 'feature documentation\n')
  const feature = commit('feature documentation')
  assert.deepEqual(classifyPaths(changedPaths(webOnly, feature, { cwd: fixture, mergeBase: true })), none)
})
test('moving a file between components checks both the removed and added paths', () => {
  git(['checkout', '-q', 'main'])
  renameSync(join(fixture, 'frontend/src/app.ts'), join(fixture, 'android/moved.ts'))
  const moved = commit('move between apps')
  assert.deepEqual(classifyPaths(changedPaths(webOnly, moved, { cwd: fixture })), { ...none, android: true, web: true })
})
test('deletions remain relevant build inputs', () => {
  const before = git(['rev-parse', 'HEAD'])
  rmSync(join(fixture, 'android/app.kt'))
  const deleted = commit('remove Android file')
  assert.deepEqual(classifyPaths(changedPaths(before, deleted, { cwd: fixture })), { ...none, android: true })
})

function detect(eventName, event, sha = webOnly) {
  const state = join(fixture, 'event-state')
  mkdirSync(state, { recursive: true })
  const eventPath = join(state, 'event.json')
  const outputPath = join(state, 'output.txt')
  writeFileSync(eventPath, JSON.stringify(event))
  writeFileSync(outputPath, '')
  execFileSync(process.execPath, [fileURLToPath(new URL('./detect-changes.mjs', import.meta.url))], {
    cwd: fixture,
    env: { ...process.env, GITHUB_EVENT_NAME: eventName, GITHUB_EVENT_PATH: eventPath, GITHUB_SHA: sha,
      GITHUB_OUTPUT: outputPath, GITHUB_STEP_SUMMARY: join(state, 'summary.md') },
    stdio: ['ignore', 'pipe', 'pipe'],
  })
  return Object.fromEntries(readFileSync(outputPath, 'utf8').trim().split('\n').map(line => {
    const [key, value] = line.split('='); return [key, value === 'true']
  }))
}
test('GitHub push events write the expected component outputs', () => {
  assert.deepEqual(detect('push', { before: initial }, docsOnly), none)
  assert.deepEqual(detect('push', { before: docsOnly }, androidOnly), { ...none, android: true })
  assert.deepEqual(detect('push', { before: androidOnly }, webOnly), { ...none, web: true })
})
test('GitHub merge-queue events compare the synthetic merge to its base', () => {
  assert.deepEqual(detect('merge_group', { merge_group: { base_sha: androidOnly, head_sha: webOnly } }), { ...none, web: true })
})
test('manual runs can deliberately select one component', () => {
  assert.deepEqual(detect('workflow_dispatch', { inputs: { component: 'android' } }), { ...none, android: true })
  assert.deepEqual(detect('workflow_dispatch', { inputs: { component: 'web' } }), { ...none, web: true })
  assert.deepEqual(detect('workflow_dispatch', { inputs: { component: 'backend' } }), { ...none, backend: true })
})
test('missing GitHub history requests all checks instead of incorrectly skipping them', () => {
  assert.deepEqual(detect('push', { before: 'missing-commit' }), { android: true, web: true, backend: true })
})
