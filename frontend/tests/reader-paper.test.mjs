import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createDemoWorkspace } from '../src/data/demo.ts'
import { resolveReaderPaper, readerBackRoute } from '../src/data/reader-paper.ts'

test('paper links resolve current metadata by collection and record, including duplicate IDs', () => {
  const workspace = createDemoWorkspace()
  workspace.research = [
    {
      id: 'same-id',
      type: 'qualitative',
      title: 'First paper',
      fileId: 'revision-1',
      members: ['Author A'],
      strand: 'STEM',
      createdAt: 8,
    },
    {
      id: 'same-id',
      type: 'quantitative',
      title: 'Other paper',
      fileId: 'other-file',
      members: ['Author B'],
      strand: 'ABM',
      createdAt: 9,
    },
  ]
  const target = { source: 'qualitative', id: 'same-id' }
  assert.equal(resolveReaderPaper(workspace, target).fileId, 'revision-1')
  assert.deepEqual(resolveReaderPaper(workspace, target).authors, ['Author A'])
  workspace.research[0].fileId = 'revision-2'
  assert.equal(resolveReaderPaper(workspace, target).fileId, 'revision-2')
  assert.equal(
    resolveReaderPaper(workspace, { source: 'quantitative', id: 'same-id' }).title,
    'Other paper',
  )
  assert.equal(resolveReaderPaper(workspace, { source: 'qualitative', id: 'missing' }), null)
  assert.equal(readerBackRoute(target), 'research')
})

test('group submissions use actual members/status and documents use their saved description', () => {
  const workspace = createDemoWorkspace()
  const group = workspace.groups[0]
  const target = { source: 'groups', id: group.id }
  const paper = resolveReaderPaper(workspace, target)
  assert.equal(paper.groupName, group.name)
  assert.deepEqual(
    paper.authors,
    group.memberIds.map((id) => workspace.students.find((student) => student.id === id).name),
  )
  assert.equal(paper.createdAt, 0)
  assert.equal(readerBackRoute(target), `groups/${encodeURIComponent(group.id)}`)
  const document = workspace.documents[0]
  assert.equal(
    resolveReaderPaper(workspace, { source: 'documents', id: document.id }).description,
    document.description,
  )
  assert.equal(readerBackRoute({ source: 'documents', id: document.id }), 'documents')
})
