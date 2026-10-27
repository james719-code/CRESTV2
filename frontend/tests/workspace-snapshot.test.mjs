import { test } from 'node:test'
import assert from 'node:assert/strict'
import { WorkspaceSnapshot } from '../src/data/workspace-snapshot.ts'

const sources = ['students', 'teachers', 'groups', 'qualitative', 'quantitative', 'documents']
function initialized() {
  const snapshot = new WorkspaceSnapshot()
  let workspace
  for (const source of sources) workspace = snapshot.update(source, [])
  return { snapshot, workspace }
}

test('the session receives no partial workspace before all six sources load', () => {
  const snapshot = new WorkspaceSnapshot()
  for (const source of sources.slice(0, -1)) assert.equal(snapshot.update(source, []), null)
  assert.equal(snapshot.update('students', []), null)
  assert.deepEqual(snapshot.update('documents', []), {
    students: [],
    teachers: [],
    groups: [],
    research: [],
    documents: [],
  })
})

test('a changed collection preserves unrelated arrays and previous snapshots', () => {
  const { snapshot, workspace: before } = initialized()
  const after = snapshot.update('students', [
    { id: 's', data: { name: 'Student', group_id: 'legacy', accepted: true } },
  ])
  assert.notEqual(after.students, before.students)
  assert.equal(before.students.length, 0)
  assert.equal(after.students[0].groupId, 'legacy')
  for (const key of ['teachers', 'groups', 'research', 'documents'])
    assert.equal(after[key], before[key])
  const documentUpdate = snapshot.update('documents', [{ id: 'doc', data: { createdAt: 5 } }])
  assert.equal(documentUpdate.students, after.students)
  assert.equal(documentUpdate.groups, after.groups)
  assert.equal(documentUpdate.research, after.research)
})

test('research stays globally sorted across both sources and removes deleted records', () => {
  const { snapshot } = initialized()
  snapshot.update('qualitative', [{ id: 'q', data: { createdAt: 2, downloads: -1 } }])
  const workspace = snapshot.update('quantitative', [
    { id: 'new', data: { createdAt: { toMillis: () => 4 } } },
    { id: 'old', data: { createdAt: NaN } },
  ])
  assert.deepEqual(
    workspace.research.map((row) => row.id),
    ['new', 'q', 'old'],
  )
  assert.equal(workspace.research[1].downloads, 0)
  assert.equal(workspace.research[1].type, 'qualitative')
  const after = snapshot.update('qualitative', [])
  assert.deepEqual(
    after.research.map((row) => row.id),
    ['new', 'old'],
  )
  assert.equal(workspace.research.length, 3)
  assert.equal(after.students, workspace.students)
})

test('each observer starts fresh without another account’s records', () => {
  const { snapshot } = initialized()
  snapshot.update('teachers', [{ id: 'previous-account', data: { access: true } }])
  assert.equal(new WorkspaceSnapshot().update('students', []), null)
  assert.deepEqual(initialized().workspace.teachers, [])
})
