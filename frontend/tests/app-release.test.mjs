import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  androidDownloadUrl,
  fetchAppRelease,
  latestReleaseApiUrl,
  parseAppRelease,
} from '../src/data/app-release.ts'

function published(tag = 'v1.0.0') {
  return {
    tag_name: tag,
    draft: false,
    prerelease: false,
    assets: [
      {
        name: 'app-release.apk',
        state: 'uploaded',
        size: 10485760,
        browser_download_url: `https://github.com/james719-code/CRESTV2/releases/download/${tag}/app-release.apk`,
      },
    ],
  }
}

test('download version follows the published APK, independently of the next development version', () => {
  const release = parseAppRelease(published())
  assert.equal(release.version, '1.0.0')
  assert.equal(release.sizeBytes, 10485760)
  assert.match(release.downloadUrl, /download\/v1\.0\.0\/app-release\.apk$/)
  assert.match(androidDownloadUrl, /releases\/latest\/download\/app-release\.apk$/)
  assert.equal(parseAppRelease(published('android-v2.0.0')).version, '2.0.0')
})

test('drafts, prereleases, web releases, and releases without an uploaded APK are not advertised', () => {
  for (const change of [
    { draft: true },
    { prerelease: true },
    { tag_name: 'web-v2.0.0' },
    { assets: [] },
  ]) {
    assert.equal(parseAppRelease({ ...published(), ...change }), null)
  }
  const incomplete = published()
  incomplete.assets[0].state = 'new'
  assert.equal(parseAppRelease(incomplete), null)
})

test('release data cannot redirect downloads away from the expected repository and tag', () => {
  for (const url of [
    'https://example.com/app-release.apk',
    'javascript:alert(1)',
    'https://github.com/james719-code/CRESTV2/releases/download/v0.9.0/app-release.apk',
  ]) {
    const release = published()
    release.assets[0].browser_download_url = url
    assert.equal(parseAppRelease(release), null)
  }
})

test('malformed release responses fall back without displaying an invented version or size', () => {
  for (const value of [
    null,
    [],
    {},
    { ...published(), tag_name: null },
    { ...published(), assets: [null] },
  ]) {
    assert.equal(parseAppRelease(value), null)
  }
  const release = published()
  release.assets[0].size = -1
  assert.equal(parseAppRelease(release).sizeBytes, null)
})

test('fetches public release metadata without credentials and uses the published version', async () => {
  const signal = new AbortController().signal
  const release = await fetchAppRelease(signal, async (url, options) => {
    assert.equal(url, latestReleaseApiUrl)
    assert.equal(options.signal, signal)
    assert.equal(options.headers.Authorization, undefined)
    return new Response(JSON.stringify(published('android-v2.0.0')))
  })
  assert.equal(release.version, '2.0.0')
})

test('rate limits, missing releases, invalid JSON, offline and aborted requests preserve the fallback', async () => {
  const signal = new AbortController().signal
  for (const response of [
    new Response('', { status: 403 }),
    new Response('', { status: 404 }),
    new Response('', { status: 429 }),
    new Response('invalid JSON'),
  ]) {
    assert.equal(await fetchAppRelease(signal, async () => response), null)
  }
  assert.equal(
    await fetchAppRelease(signal, async () => {
      throw new TypeError('Failed to fetch')
    }),
    null,
  )
  const controller = new AbortController()
  controller.abort()
  assert.equal(
    await fetchAppRelease(controller.signal, async (_url, options) => {
      options.signal.throwIfAborted()
    }),
    null,
  )
  assert.match(androidDownloadUrl, /releases\/latest\/download\/app-release\.apk$/)
})
