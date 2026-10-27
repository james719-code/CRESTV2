import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  manilaDate,
  milestoneStatus,
  validDueDate,
  validateMilestone,
  checkMilestoneVersion,
} from '../src/data/milestones.ts'
import { identifyNotifications, inboxCandidates, mergeNotifications } from '../src/data/inbox.ts'
import { createDemoWorkspace } from '../src/data/demo.ts'
import { WorkspaceSnapshot } from '../src/data/workspace-snapshot.ts'

test('deadlines stay due through the entire selected Manila day, independent of host timezone', () => {
  const item = { dueDate: '2026-10-27', completedAt: 0 }
  assert.equal(manilaDate(Date.parse('2026-10-27T15:59:59Z')), '2026-10-27')
  assert.equal(milestoneStatus(item, manilaDate(Date.parse('2026-10-27T15:59:59Z'))), 'today')
  assert.equal(milestoneStatus(item, manilaDate(Date.parse('2026-10-27T16:00:00Z'))), 'overdue')
  assert.equal(milestoneStatus({ ...item, completedAt: 1 }, '2026-10-28'), 'completed')
  assert.equal(milestoneStatus(item, '2026-10-26'), 'upcoming')
})

test('deadline validation rejects impossible dates and incomplete milestones', () => {
  for (const date of [
    '2026-02-29',
    '2026-04-31',
    '2026-13-01',
    '2026-1-01',
    '1999-12-31',
    '2101-01-01',
    '',
  ])
    assert.equal(validDueDate(date), false, date)
  assert.equal(validDueDate('2028-02-29'), true)
  const input = { groupId: 'g1', title: '  Chapter 1  ', stage: 'chapter', dueDate: '2026-10-27' }
  assert.equal(validateMilestone(input).title, 'Chapter 1')
  for (const patch of [
    { groupId: '' },
    { title: ' ' },
    { title: 'x'.repeat(201) },
    { stage: 'unknown' },
    { dueDate: 'invalid' },
  ])
    assert.throws(() => validateMilestone({ ...input, ...patch }))
})

test('stale milestone edits and deletions cannot overwrite another teacher’s change', () => {
  checkMilestoneVersion(undefined, null)
  checkMilestoneVersion({ version: 3 }, 3)
  assert.throws(() => checkMilestoneVersion({ version: 4 }, 3), /has changed/)
  assert.throws(() => checkMilestoneVersion(undefined, 3), /has changed/)
  assert.throws(() => checkMilestoneVersion({ version: 1 }, null), /has changed/)
})

test('inbox captures Android-compatible submission flags, approvals and all feedback', async () => {
  const workspace = createDemoWorkspace()
  const events = await identifyNotifications(inboxCandidates(workspace))
  assert.equal(events.filter((item) => item.kind === 'submission').length, 3)
  assert.equal(
    events.filter((item) => item.kind === 'approval').length,
    workspace.groups.filter((group) => group.accepted).length,
  )
  assert.equal(
    events.filter((item) => item.kind === 'revision').length,
    workspace.groups.reduce((sum, group) => sum + group.comments.length, 0),
  )
  assert.ok(events.every((item) => /^[a-f0-9]{64}$/.test(item.id)))
  assert.equal(new Set(events.map((item) => item.id)).size, events.length)
})

test('reconnect preserves read state and history; a replacement PDF becomes a new unread update', async () => {
  const workspace = createDemoWorkspace()
  const events = await identifyNotifications(inboxCandidates(workspace))
  const initial = mergeNotifications([], events, 100)
  initial[0].read = true
  const reconnected = mergeNotifications(initial, events, 200)
  assert.deepEqual(reconnected, initial)
  workspace.groups[0].fileId = 'a-new-immutable-file'
  const updated = mergeNotifications(
    reconnected,
    await identifyNotifications(inboxCandidates(workspace)),
    300,
  )
  assert.equal(updated.length, initial.length + 1)
  assert.equal(updated[0].read, false)
  assert.equal(updated[0].observedAt, 300)
  assert.equal(updated.find((item) => item.id === initial[0].id).read, true)
  workspace.groups = []
  assert.equal(
    mergeNotifications(updated, await identifyNotifications(inboxCandidates(workspace)), 400)
      .length,
    updated.length,
  )
})

test('the live session waits for milestones and the private inbox and preserves unrelated arrays', () => {
  const snapshot = new WorkspaceSnapshot(['milestones', 'notifications'])
  for (const source of [
    'students',
    'teachers',
    'groups',
    'qualitative',
    'quantitative',
    'documents',
    'milestones',
  ])
    assert.equal(snapshot.update(source, []), null)
  const ready = snapshot.update('notifications', [
    { id: 'event', data: { kind: 'submission', observedAt: { toMillis: () => 50 }, read: true } },
  ])
  assert.equal(ready.notifications[0].observedAt, 50)
  assert.equal(ready.notifications[0].read, true)
  const changed = snapshot.update('milestones', [
    {
      id: 'm',
      data: {
        title: 'Proposal',
        groupId: 'g1',
        stage: 'proposal',
        dueDate: '2026-10-27',
        version: 2,
      },
    },
  ])
  assert.equal(changed.milestones[0].version, 2)
  assert.equal(changed.notifications, ready.notifications)
  assert.equal(changed.groups, ready.groups)
})
