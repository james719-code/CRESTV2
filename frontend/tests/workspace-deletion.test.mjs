import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  deleteStudentWorkspace,
  deleteGroupWorkspace,
  reviewStudentWorkspace,
} from '../src/domain.ts'
import { createDemoWorkspace } from '../src/data/demo.ts'

test('only a declined student can be deleted, without affecting other students or published work', () => {
  const original = createDemoWorkspace()
  assert.throws(() => deleteStudentWorkspace(original, 's1'), /Only declined/)
  assert.throws(() => deleteStudentWorkspace(original, 's37'), /Only declined/)
  assert.throws(() => deleteStudentWorkspace(original, 'missing'), /no longer exists/)
  const declined = reviewStudentWorkspace(original, 's1', 'decline', 'approved')
  const next = deleteStudentWorkspace(declined, 's1')
  assert.equal(next.students.length, original.students.length - 1)
  assert.ok(next.groups.every((group) => !group.memberIds.includes('s1')))
  assert.deepEqual(next.research, original.research)
  assert.ok(original.groups.some((group) => group.memberIds.includes('s1')))
  const restored = reviewStudentWorkspace(declined, 's1', 'approve', 'declined')
  assert.throws(() => deleteStudentWorkspace(restored, 's1'), /Only declined/)
})

test('deleting a group clears its student links, preserves other memberships and library records', () => {
  const original = createDemoWorkspace()
  const group = original.groups[0]
  const next = deleteGroupWorkspace(original, group.id)
  assert.equal(next.groups.length, original.groups.length - 1)
  assert.ok(
    next.students
      .filter((student) => group.memberIds.includes(student.id))
      .every((student) => student.groupId === ''),
  )
  assert.deepEqual(
    next.students.filter((student) => !group.memberIds.includes(student.id)),
    original.students.filter((student) => !group.memberIds.includes(student.id)),
  )
  assert.deepEqual(next.research, original.research)
  assert.deepEqual(next.documents, original.documents)
  assert.equal(original.groups.length, next.groups.length + 1)
  assert.throws(() => deleteGroupWorkspace(next, group.id), /no longer exists/)
})
