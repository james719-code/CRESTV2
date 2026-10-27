import { test } from 'node:test'
import assert from 'node:assert/strict'
import { GroupDeletionTracker, withGroupDeletions } from '../src/data/group-deletions.ts'
import { createDemoWorkspace } from '../src/data/demo.ts'

function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => {
    resolve = yes
    reject = no
  })
  return { promise, resolve, reject }
}

test('deletion survives leaving details and only marks its own group as busy', async () => {
  const tracker = new GroupDeletionTracker()
  const workspace = createDemoWorkspace()
  const group = workspace.groups[0]
  const gate = deferred()
  let calls = 0
  const unsubscribe = tracker.subscribe(() => {})
  const job = tracker.delete(group, () => {
    calls++
    return gate.promise
  })
  assert.equal(tracker.getSnapshot().operations[group.id].deleting, true)
  assert.equal(Object.keys(tracker.getSnapshot().operations).length, 1)
  unsubscribe() // Details unmount while the operation continues in the workspace.
  assert.equal(
    tracker.delete(group, () => Promise.resolve()),
    job,
  )
  const remoteWithoutGroup = {
    ...workspace,
    groups: workspace.groups.filter((item) => item.id !== group.id),
  }
  assert.ok(
    withGroupDeletions(remoteWithoutGroup, tracker.getSnapshot()).groups.some(
      (item) => item.id === group.id,
    ),
  )
  gate.resolve()
  await job
  assert.equal(calls, 1)
  assert.equal(tracker.getSnapshot().operations[group.id], undefined)
  assert.ok(
    !withGroupDeletions(workspace, tracker.getSnapshot()).groups.some(
      (item) => item.id === group.id,
    ),
  )
})

test('a failed group stays visible with an error and supports retry', async () => {
  const tracker = new GroupDeletionTracker()
  const workspace = createDemoWorkspace()
  const group = workspace.groups[0]
  await assert.rejects(
    tracker.delete(group, async () => {
      throw new Error('Connection lost')
    }),
    /Connection lost/,
  )
  const operation = tracker.getSnapshot().operations[group.id]
  assert.equal(operation.deleting, false)
  assert.equal(operation.error, 'Connection lost')
  assert.equal(
    withGroupDeletions(workspace, tracker.getSnapshot()).groups.length,
    workspace.groups.length,
  )
  await tracker.delete(group, async () => {})
  assert.equal(tracker.getSnapshot().operations[group.id], undefined)
  assert.equal(
    withGroupDeletions(workspace, tracker.getSnapshot()).groups.length,
    workspace.groups.length - 1,
  )
})

test('concurrent group deletions finish independently', async () => {
  const tracker = new GroupDeletionTracker()
  const [first, second] = createDemoWorkspace().groups
  const a = deferred(),
    b = deferred()
  const firstJob = tracker.delete(first, () => a.promise)
  const secondJob = tracker.delete(second, () => b.promise)
  a.resolve()
  await firstJob
  assert.equal(tracker.getSnapshot().operations[first.id], undefined)
  assert.equal(tracker.getSnapshot().operations[second.id].deleting, true)
  b.resolve()
  await secondJob
  assert.deepEqual(new Set(tracker.getSnapshot().deletedIds), new Set([first.id, second.id]))
})

test('changing accounts prevents an old deletion from repopulating another workspace', async () => {
  const tracker = new GroupDeletionTracker()
  const group = createDemoWorkspace().groups[0]
  const gate = deferred()
  const job = tracker.delete(group, () => gate.promise)
  tracker.reset()
  gate.reject(new Error('Old session finished'))
  await assert.rejects(job)
  assert.deepEqual(tracker.getSnapshot(), { operations: {}, deletedIds: [] })
})
