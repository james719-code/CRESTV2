import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import postcss from 'postcss'
import { scopePdfCss } from '../scripts/scope-pdf-css.ts'

test('PDF viewer styles cannot target the workspace, while nested selectors and keyframes remain valid', () => {
  const output = postcss.parse(
    scopePdfCss(`
    :root { --viewer-color: red; --viewer-container-height: 0; }
    .sidebar, .spread:is(.horizontal, .wrapped) { position: relative; &.open { display: block; } }
    @media (forced-colors: active) { :root { --viewer-color: blue; } .sidebar { color: CanvasText; } }
    @keyframes loading { from { opacity: 0; } to { opacity: 1; } }
  `),
  )
  const rules = []
  output.walkRules((rule) => rules.push(rule))
  assert.equal(rules[0].selector, '.paper-reader')
  assert.equal(
    rules[0].nodes.find((node) => node.prop === '--viewer-container-height').value,
    'inherit',
  )
  assert.deepEqual(rules[1].selectors, [
    '.paper-reader .sidebar',
    '.paper-reader .spread:is(.horizontal, .wrapped)',
  ])
  assert.equal(rules[2].selector, '&.open')
  assert.equal(rules[3].selector, '.paper-reader')
  assert.equal(rules[4].selector, '.paper-reader .sidebar')
  assert.equal(rules[5].selector, 'from')
  assert.equal(rules[6].selector, 'to')
})

test('all top-level rules in the installed PDF.js stylesheet are restricted to the reader', async () => {
  const raw = await readFile(
    new URL('../node_modules/pdfjs-dist/web/pdf_viewer.css', import.meta.url),
    'utf8',
  )
  const css = postcss.parse(scopePdfCss(raw))
  let checked = 0
  css.walkRules((rule) => {
    let parent = rule.parent
    while (parent && parent.type !== 'root') {
      if (parent.type === 'rule' || (parent.type === 'atrule' && /keyframes$/i.test(parent.name)))
        return
      parent = parent.parent
    }
    checked++
    for (const selector of rule.selectors)
      assert.match(selector, /^\.paper-reader(?:\s|$|[.:#\[])/, `Unscoped selector: ${selector}`)
  })
  assert.ok(checked > 100, 'The PDF.js stylesheet should be checked in full.')
  const viewerHeight = []
  css.walkDecls('--viewer-container-height', (declaration) => viewerHeight.push(declaration.value))
  assert.deepEqual(viewerHeight, ['inherit'])
})
