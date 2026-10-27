import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  parseRoute,
  isWorkspaceRoute,
  groupRoute,
  routeGroupId,
  readerRoute,
  routeReader,
} from '../src/routing.ts'

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
    'help',
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

test('reader links survive reload and identify collection plus record, never a signed file URL', () => {
  for (const source of ['qualitative', 'quantitative', 'documents', 'groups']) {
    const target = { source, id: 'Paper #1 with spaces' }
    const route = readerRoute(target)
    assert.equal(parseRoute(`#${route}`), route)
    assert.deepEqual(routeReader(route), target)
    assert.equal(isWorkspaceRoute(route), true)
    assert.equal(routeGroupId(route), null)
  }
  for (const hash of [
    '#read/groups/',
    '#read/other/id',
    '#read/groups/%',
    '#read/groups/a%2Fb',
    '#read/qualitative/a/b',
  ])
    assert.equal(parseRoute(hash), 'home')
  assert.equal(routeReader('research'), null)
})
