import { test } from 'node:test'
import assert from 'node:assert/strict'
import { studentAccountStatus, reviewStudentWorkspace } from '../src/domain.ts'
import { createDemoWorkspace } from '../src/data/demo.ts'

test('existing Android profiles retain approved/pending states without a declined field', () => {
  assert.equal(studentAccountStatus({ accepted: true }), 'approved')
  assert.equal(studentAccountStatus({ accepted: false }), 'pending')
  assert.equal(studentAccountStatus({ accepted: false, declined: true }), 'declined')
  assert.equal(studentAccountStatus({ accepted: true, declined: true }), 'approved')
})

test('decline retains student records, group membership, and research; restore clears declined status', () => {
  const original = createDemoWorkspace()
  const next = reviewStudentWorkspace(original, 's1', 'decline', 'approved')
  assert.equal(studentAccountStatus(next.students[0]), 'declined')
  assert.equal(next.students.length, original.students.length)
  assert.deepEqual(next.groups, original.groups)
  assert.deepEqual(next.research, original.research)
  assert.equal(studentAccountStatus(original.students[0]), 'approved')
  const restored = reviewStudentWorkspace(next, 's1', 'approve', 'declined')
  assert.equal(restored.students[0].accepted, true)
  assert.equal(restored.students[0].declined, false)
})

test('declining a pending student removes them from pending counts without deleting their profile', () => {
  const next = reviewStudentWorkspace(createDemoWorkspace(), 's37', 'decline', 'pending')
  assert.equal(next.students.filter(s => studentAccountStatus(s) === 'pending').length, 2)
  assert.ok(next.students.find(s => s.id === 's37' && s.declined && !s.accepted))
})

test('stale decisions cannot overwrite a different teacher decision or a missing student', () => {
  const original = createDemoWorkspace()
  const approved = reviewStudentWorkspace(original, 's37', 'approve', 'pending')
  assert.throws(() => reviewStudentWorkspace(approved, 's37', 'decline', 'pending'), /has changed/)
  assert.throws(() => reviewStudentWorkspace(original, 'missing', 'decline', 'pending'), /no longer exists/)
  assert.throws(() => reviewStudentWorkspace(approved, 's37', 'approve', 'approved'), /already has/)
})
