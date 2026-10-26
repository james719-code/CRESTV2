import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  defaultPreferences,
  parsePreferences,
  resolveTheme,
  withReadingPreset,
} from '../src/preferences.ts'

test('display preferences start with the device theme and retain every feature', () => {
  assert.equal(defaultPreferences.theme, 'system')
  assert.equal(defaultPreferences.layout, 'standard')
  assert.deepEqual(parsePreferences(null), defaultPreferences)
  assert.deepEqual(parsePreferences([]), defaultPreferences)
})

test('corrupt or loosely typed saved values cannot select an invalid display mode', () => {
  assert.deepEqual(
    parsePreferences({
      theme: ['dark'],
      textSize: { size: 'large' },
      layout: 'senior',
      strongContrast: 'yes',
      guidance: 1,
    }),
    defaultPreferences,
  )
  assert.equal(parsePreferences({ theme: 'sepia' }).theme, 'system')
  assert.equal(parsePreferences({ textSize: '300%' }).textSize, 'standard')
})

test('explicit theme choices override the device while system mode follows it', () => {
  assert.equal(resolveTheme('dark', false), 'dark')
  assert.equal(resolveTheme('light', true), 'light')
  assert.equal(resolveTheme('system', true), 'dark')
  assert.equal(resolveTheme('system', false), 'light')
})

test('Easy reading changes readability and guidance while keeping the chosen theme', () => {
  const initial = { ...defaultPreferences, theme: 'dark' }
  const easy = withReadingPreset(initial, 'easy-reading')
  assert.equal(easy.theme, 'dark')
  assert.equal(easy.layout, 'easy-reading')
  assert.equal(easy.textSize, 'large')
  assert.equal(easy.strongContrast, true)
  assert.equal(easy.reduceMotion, true)
  assert.equal(easy.guidance, true)
  assert.equal(easy.hintDismissed, true)
  assert.equal(initial.layout, 'standard')
})

test('switching back to Standard restores its sizing without changing the theme', () => {
  const easy = withReadingPreset({ ...defaultPreferences, theme: 'light' }, 'easy-reading')
  const standard = withReadingPreset(easy, 'standard')
  assert.equal(standard.theme, 'light')
  assert.equal(standard.textSize, 'standard')
  assert.equal(standard.layout, 'standard')
  assert.equal(standard.strongContrast, false)
  assert.equal(standard.guidance, false)
})

test('all display choices round-trip through browser JSON storage', () => {
  const preferences = {
    theme: 'dark',
    textSize: 'extra-large',
    layout: 'easy-reading',
    strongContrast: true,
    reduceMotion: true,
    guidance: true,
    hintDismissed: true,
  }
  assert.deepEqual(parsePreferences(JSON.parse(JSON.stringify(preferences))), preferences)
})

function contrast(a, b) {
  const luminance = (hex) => {
    const rgb = hex
      .replace('#', '')
      .match(/../g)
      .map((channel) => parseInt(channel, 16) / 255)
      .map((channel) => (channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4))
    return rgb[0] * 0.2126 + rgb[1] * 0.7152 + rgb[2] * 0.0722
  }
  const values = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (values[0] + 0.05) / (values[1] + 0.05)
}

test('stronger-contrast text and controls remain readable in both themes', async () => {
  const { readFile } = await import('node:fs/promises')
  const css = await readFile(new URL('../src/appearance.css', import.meta.url), 'utf8')
  for (const [theme, surface, background] of [
    ['light', '#ffffff', '#f5f7fb'],
    ['dark', '#152236', '#0e1726'],
  ]) {
    const block = css.match(
      new RegExp(
        `:root\\[data-theme=['"]${theme}['"]\\]\\[data-contrast=['"]strong['"]\\]\\s*\\{([^}]+)`,
      ),
    )[1]
    const value = (name) =>
      block.match(new RegExp(`--crest-color-${name}:\\s*(#[a-f0-9]+)`, 'i'))[1]
    for (const surfaceColor of [surface, background]) {
      assert.ok(contrast(value('on-surface'), surfaceColor) >= 7, `${theme} main text`)
      assert.ok(
        contrast(value('on-surface-variant'), surfaceColor) >= 7,
        `${theme} supporting text`,
      )
      assert.ok(contrast(value('outline'), surfaceColor) >= 3, `${theme} control border`)
    }
  }
})
