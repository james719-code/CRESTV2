import { test } from 'node:test'
import assert from 'node:assert/strict'
import { parseRoute, isWorkspaceRoute } from '../src/routing.ts'

test('the public entry and unknown routes open the landing page', () => {
  for (const hash of ['', '#', '#home', '#unrecognized', '#features']) {
    assert.equal(parseRoute(hash), 'home')
  }
})

test('sign-in is a separate route from the public page and teacher workspace', () => {
  assert.equal(parseRoute('#signin'), 'signin')
  assert.equal(isWorkspaceRoute('home'), false)
  assert.equal(isWorkspaceRoute('signin'), false)
})

test('existing teacher deep links remain available behind the session gate', () => {
  for (const page of [
    'overview',
    'groups',
    'reviews',
    'students',
    'research',
    'documents',
    'accounts',
  ]) {
    assert.equal(parseRoute(`#${page}`), page)
    assert.equal(isWorkspaceRoute(page), true)
  }
})
