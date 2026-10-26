import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const config = JSON.parse(await readFile(new URL('../../firebase/firestore.indexes.json', import.meta.url), 'utf8'))
function indexed(collection, field, order) {
  const override = config.fieldOverrides.find(item => item.collectionGroup === collection && item.fieldPath === field)
    || config.fieldOverrides.find(item => item.collectionGroup === collection && item.fieldPath === '*')
  return !override || override.indexes.some(index => index.order === order && index.queryScope === 'COLLECTION')
}

test('private PDF access and storage management retain file-reference lookups', () => {
  for (const collection of ['groups', 'qualitative', 'quantitative', 'documents']) {
    assert.ok(indexed(collection, 'file_link', 'ASCENDING'), `${collection}.file_link is needed by the file gateway`)
  }
})

test('library pagination, document ordering, and strand totals remain indexed', () => {
  for (const collection of ['qualitative', 'quantitative', 'documents']) {
    assert.ok(indexed(collection, 'createdAt', 'DESCENDING'), `${collection} needs newest-first pagination`)
  }
  for (const collection of ['qualitative', 'quantitative']) {
    assert.ok(indexed(collection, 'strand', 'ASCENDING'), `${collection} needs strand counts`)
  }
  assert.ok(indexed('groups', 'group_name', 'ASCENDING'))
})

test('account approvals, notifications, and pending submissions retain their query indexes', () => {
  assert.ok(indexed('students', 'accepted', 'ASCENDING'))
  assert.ok(indexed('teachers', 'access', 'ASCENDING'))
  assert.ok(config.indexes.some(index => index.collectionGroup === 'groups' && index.queryScope === 'COLLECTION'
    && ['uploaded', 'accepted_research'].every(field => index.fields.some(item => item.fieldPath === field && item.order === 'ASCENDING'))))
})

test('field configuration has no duplicate or contradictory overrides', () => {
  const keys = config.fieldOverrides.map(item => `${item.collectionGroup}/${item.fieldPath}`)
  assert.equal(new Set(keys).size, keys.length)
  for (const override of config.fieldOverrides) {
    for (const index of override.indexes) {
      assert.notEqual(Boolean(index.order), Boolean(index.arrayConfig))
      assert.equal(index.queryScope, 'COLLECTION')
    }
  }
})
