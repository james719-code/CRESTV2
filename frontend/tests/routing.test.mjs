import { test } from 'node:test'
import assert from 'node:assert/strict'
import { parseRoute, isWorkspaceRoute, groupRoute, routeGroupId } from '../src/routing.ts'

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
    'settings',
    'about',
  ]) {
    assert.equal(parseRoute(`#${page}`), page)
    assert.equal(isWorkspaceRoute(page), true)
  }
})

test('group details have persistent, encoded page links and reject malformed IDs', () => {
  const route = groupRoute('STEM group #1')
  assert.equal(parseRoute(`#${route}`), route)
  assert.equal(routeGroupId(route), 'STEM group #1')
  assert.equal(isWorkspaceRoute(route), true)
  assert.equal(routeGroupId('groups'), null)
  for (const hash of ['#groups/', '#groups/%', '#groups/a%2Fb'])
    assert.equal(parseRoute(hash), 'home')
})
