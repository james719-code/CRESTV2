import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import postcss from 'postcss'

function declaration(file, selector, property) {
  const css = postcss.parse(readFileSync(new URL(`../src/${file}`, import.meta.url), 'utf8'))
  let value
  css.walkRules((rule) => {
    if (rule.selectors?.includes(selector)) {
      rule.walkDecls(property, (decl) => { value = decl.value })
    }
  })
  return value
}

test('research categories and submission states use paired shared semantic colors', () => {
  for (const strand of ['stem', 'humss', 'abm', 'tvl', 'gas']) {
    assert.equal(declaration('App.css', `.strand-${strand}`, 'background'), `var(--crest-color-strand-${strand})`)
    assert.equal(declaration('App.css', `.strand-${strand}`, 'color'), `var(--crest-color-on-strand-${strand})`)
  }
  for (const [status, role] of Object.entries({ pending: 'info', changes: 'warning', approved: 'success' })) {
    assert.equal(declaration('App.css', `.status-${status}`, 'background'), `var(--crest-color-${role})`)
    assert.equal(declaration('App.css', `.status-${status}`, 'color'), `var(--crest-color-on-${role})`)
  }
})

test('shared controls, research cards, and dialogs consume the branded geometry', () => {
  assert.equal(declaration('appearance.css', ':root', '--control-height'), 'var(--crest-size-touch-minimum)')
  assert.equal(declaration('modern.css', ":root [data-slot='button']", 'min-height'), 'var(--control-height)')
  assert.equal(declaration('modern.css', ":root [data-slot='button']", 'border-radius'), 'var(--crest-radius-control)')
  assert.equal(declaration('App.css', '.research-card', 'border-radius'), 'var(--crest-radius-card)')
  assert.equal(declaration('modern.css', ":root [data-slot='dialog-content']", 'border-radius'), 'var(--crest-radius-sheet)')
  assert.equal(declaration('components/paper-reader.css', ".reader-toolbar [data-slot='button']", 'min-height'), 'var(--control-height)')
})
