import { test } from 'node:test'
import assert from 'node:assert/strict'
import { helpVideoEmbedUrl } from '../src/data/help-video.ts'

test('no video is shown unless a link is configured', () => {
  for (const value of [undefined, '', '   ', 'not a url']) {
    assert.equal(helpVideoEmbedUrl(value), null)
  }
})

test('common share links become embeddable player links', () => {
  assert.equal(
    helpVideoEmbedUrl('https://www.youtube.com/watch?v=dQw4w9WgXcQ'),
    'https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ',
  )
  assert.equal(
    helpVideoEmbedUrl('https://youtu.be/dQw4w9WgXcQ?t=3'),
    'https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ',
  )
  assert.equal(
    helpVideoEmbedUrl('https://vimeo.com/123456789'),
    'https://player.vimeo.com/video/123456789',
  )
  assert.equal(
    helpVideoEmbedUrl('https://drive.google.com/file/d/abc_123-XYZ/view?usp=sharing'),
    'https://drive.google.com/file/d/abc_123-XYZ/preview',
  )
  assert.equal(
    helpVideoEmbedUrl('https://www.loom.com/share/0123abcd4567'),
    'https://www.loom.com/embed/0123abcd4567',
  )
})

test('unknown hosts and insecure links are never framed', () => {
  for (const value of [
    'http://www.youtube.com/watch?v=dQw4w9WgXcQ',
    'https://example.com/video.mp4',
    'https://evil.example/embed/dQw4w9WgXcQ',
    'https://www.youtube.com/watch?v="><script>',
    'javascript:alert(1)',
  ]) {
    assert.equal(helpVideoEmbedUrl(value), null)
  }
})
