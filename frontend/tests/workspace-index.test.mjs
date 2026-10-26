import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createDemoWorkspace } from '../src/data/demo.ts'
import { studentGroup } from '../src/domain.ts'
import { indexWorkspace, pageWindow } from '../src/workspace-index.ts'

test('indexed joins preserve legacy first-match group membership and stale IDs', () => {
  const data = createDemoWorkspace()
  data.students.push(
    { ...data.students[0], id: 'stale', groupId: 'missing' },
    { ...data.students[0], id: 'direct-only', groupId: data.groups[2].id },
  )
  data.students[0].groupId = data.groups[3].id
  data.groups[1].memberIds.push(data.students[0].id, 'stale')
  const index = indexWorkspace(data)
  for (const student of data.students) {
    assert.equal(index.studentGroups.get(student.id), studentGroup(student, data.groups))
  }
  assert.equal(index.studentsById.get(data.students[0].id), data.students[0])
})

test('group search indexes titles and known members without requiring every member record', () => {
  const data = createDemoWorkspace()
  data.groups[0].memberIds.push('missing-student')
  const index = indexWorkspace(data)
  const search = index.groupSearch.get(data.groups[0].id)
  assert.ok(search.includes(data.groups[0].title.toLowerCase()))
  assert.ok(search.includes(data.students[0].name.toLowerCase()))
  assert.ok(!search.includes('undefined'))
})

test('pagination bounds rendering, preserves all records, and clamps after filters or removal', () => {
  const data = Array.from({ length: 37 }, (_, id) => id)
  const windows = [1, 2, 3, 4].map((page) => pageWindow(data, page))
  assert.ok(windows.every(({ rows }) => rows.length <= 12))
  assert.deepEqual(
    windows.flatMap(({ rows }) => rows),
    data,
  )
  assert.deepEqual(pageWindow(data.slice(0, 3), 4), {
    page: 1,
    pages: 1,
    start: 0,
    rows: [0, 1, 2],
  })
  assert.deepEqual(pageWindow([], 4), { page: 1, pages: 1, start: 0, rows: [] })
  assert.equal(pageWindow(data, -1).page, 1)
})
