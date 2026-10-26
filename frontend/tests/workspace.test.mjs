import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createDemoWorkspace } from '../src/data/demo.ts'
import { groupStatus, studentGroup, researchType, reviewWorkspace, toCsv } from '../src/domain.ts'

test('submission state takes precedence over old revision feedback', () => {
  const group = createDemoWorkspace().groups[0]
  assert.equal(groupStatus({ ...group, uploaded: false, accepted: false }), 'preparing')
  assert.equal(groupStatus({ ...group, comments: ['Revise'], uploaded: false }), 'changes')
  assert.equal(groupStatus({ ...group, comments: ['Revise'], uploaded: true }), 'pending')
  assert.equal(groupStatus({ ...group, comments: ['Revise'], accepted: true }), 'approved')
})

test('legacy group IDs and membership both resolve student progress', () => {
  const data = createDemoWorkspace()
  const student = data.students[0]
  assert.equal(studentGroup(student, data.groups).id, 'g1')
  assert.equal(studentGroup({ ...student, groupId: '' }, data.groups).id, 'g1')
  assert.equal(studentGroup({ ...student, id: 'unassigned', groupId: '' }, data.groups), undefined)
})

test('approving research updates the group and library together without changing the input', () => {
  const data = createDemoWorkspace()
  const next = reviewWorkspace(data, 'g1', 'approve')
  assert.equal(next.research.length, data.research.length + 1)
  assert.equal(groupStatus(next.groups[0]), 'approved')
  assert.equal(next.groups[0].uploaded, false)
  assert.equal(next.research[0].fileId, data.groups[0].fileId)
  assert.deepEqual(
    next.research[0].members,
    data.students.slice(0, 4).map((student) => student.name),
  )
  assert.equal(groupStatus(data.groups[0]), 'pending')
  assert.deepEqual(next.groups.slice(1), data.groups.slice(1))
})

test('an already reviewed submission cannot create a duplicate paper', () => {
  const reviewed = reviewWorkspace(createDemoWorkspace(), 'g1', 'approve')
  assert.throws(() => reviewWorkspace(reviewed, 'g1', 'approve'), /already changed/)
})

test('requesting changes requires feedback and keeps the original PDF and title', () => {
  const data = createDemoWorkspace()
  assert.throws(() => reviewWorkspace(data, 'g1', 'changes', '   '), /Add feedback/)
  const next = reviewWorkspace(data, 'g1', 'changes', '  Clarify the sampling method.  ')
  assert.equal(groupStatus(next.groups[0]), 'changes')
  assert.equal(next.groups[0].title, data.groups[0].title)
  assert.equal(next.groups[0].fileId, data.groups[0].fileId)
  assert.deepEqual(next.groups[0].comments, ['Clarify the sampling method.'])
  assert.equal(next.research.length, data.research.length)
})

test('approval refuses incomplete submissions and missing member records', () => {
  for (const patch of [{ title: '' }, { fileId: '' }, { type: '' }, { memberIds: [] }]) {
    const data = createDemoWorkspace()
    Object.assign(data.groups[0], patch)
    assert.throws(() => reviewWorkspace(data, 'g1', 'approve'), /missing/)
  }
  const data = createDemoWorkspace()
  data.students = data.students.slice(1)
  assert.throws(() => reviewWorkspace(data, 'g1', 'approve'), /members could not be verified/)
  assert.throws(() => reviewWorkspace(data, 'unknown', 'approve'), /no longer exists/)
})

test('research types preserve the Android values and reject invalid types', () => {
  assert.equal(researchType('QUANTITATIVE'), 'quantitative')
  assert.equal(researchType('Qualitative'), 'qualitative')
  assert.equal(researchType('other'), '')
  assert.equal(researchType(undefined), '')
})

test('CSV quotes commas, newlines and double quotes, and neutralizes formula injection', () => {
  const csv = toCsv([
    ['Title', 'Name'],
    ['One, two\nthree', 'A "quoted" name'],
    ['=HYPERLINK("https://example.com")', '  +1+2'],
    ['@SUM(1)', '-1'],
  ])
  assert.ok(csv.startsWith('\uFEFF'))
  assert.ok(csv.includes('"One, two\nthree"'))
  assert.ok(csv.includes('"A ""quoted"" name"'))
  assert.ok(csv.includes('"\'=HYPERLINK('))
  assert.ok(csv.includes('"\'  +1+2"'))
  assert.ok(csv.includes('"\'@SUM(1)"'))
})

test('the demo has consistent student membership and real count-based progress', () => {
  const data = createDemoWorkspace()
  assert.equal(data.students.filter((student) => student.accepted).length, 36)
  assert.equal(new Set(data.students.map((student) => student.id)).size, data.students.length)
  assert.equal(data.groups.length, 9)
  assert.equal(data.groups.filter((group) => groupStatus(group) === 'pending').length, 3)
  for (const group of data.groups) {
    assert.equal(group.memberIds.length, 4)
    for (const id of group.memberIds)
      assert.ok(data.students.some((student) => student.id === id && student.groupId === group.id))
  }
})
