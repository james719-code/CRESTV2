import { performance } from 'node:perf_hooks'
import assert from 'node:assert/strict'
import { indexWorkspace } from '../src/workspace-index.ts'
import { studentGroup } from '../src/domain.ts'

// Synthetic school data only; this measures joins/search, not browser frame rate.
const groups = Array.from({ length: 500 }, (_, id) => ({
  id: `g${id}`,
  name: `Group ${id}`,
  title: `Research project ${id}`,
  memberIds: [],
  strand: 'STEM',
}))
const students = Array.from({ length: 2000 }, (_, id) => {
  const group = groups[Math.floor(id / 4)]
  const student = { id: `s${id}`, name: `Student ${id}`, groupId: group.id, accepted: true }
  group.memberIds.push(student.id)
  return student
})
const workspace = { groups, students, teachers: [], documents: [], research: [] }
function measure(fn) {
  const samples = Array.from({ length: 9 }, () => {
    const start = performance.now()
    fn()
    return performance.now() - start
  }).sort((a, b) => a - b)
  return +samples[4].toFixed(2)
}
const queries = ['s', 'st', 'student', 'student 19', 'student 199']
const legacy = measure(() => {
  for (const query of queries) {
    groups.filter((group) =>
      `${group.name} ${group.title} ${students
        .filter((student) => group.memberIds.includes(student.id))
        .map((student) => student.name)
        .join(' ')}`
        .toLowerCase()
        .includes(query),
    )
    students.filter((student) =>
      `${student.name} ${studentGroup(student, groups)?.name || ''}`.toLowerCase().includes(query),
    )
  }
})
const indexed = measure(() => {
  const index = indexWorkspace(workspace)
  assert.equal(index.studentGroups.size, students.length)
  for (const query of queries) {
    groups.filter((group) => index.groupSearch.get(group.id).includes(query))
    students.filter((student) =>
      `${student.name} ${index.studentGroups.get(student.id)?.name || ''}`
        .toLowerCase()
        .includes(query),
    )
  }
})
console.log(
  JSON.stringify(
    {
      students: students.length,
      groups: groups.length,
      queries: queries.length,
      medianOf: 9,
      legacyMs: legacy,
      indexedMs: indexed,
    },
    null,
    2,
  ),
)
