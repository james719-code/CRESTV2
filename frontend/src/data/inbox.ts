import { studentAccountStatus, type InboxNotification, type Workspace } from '../domain.ts'

export interface InboxCandidate extends Omit<InboxNotification, 'id' | 'read' | 'observedAt'> {
  key: string
}

// Capture current school updates on connection, including Android submissions.
// Stable identities prevent reconnects or another tab from resetting read state.
export function inboxCandidates(workspace: Workspace): InboxCandidate[] {
  const events: InboxCandidate[] = []
  for (const group of workspace.groups) {
    const base = { groupId: group.id, message: group.title || group.name }
    if (group.uploaded && !group.accepted)
      events.push({
        ...base,
        key: JSON.stringify(['submission', group.id, group.fileId]),
        kind: 'submission',
        title: `${group.name} submitted research`,
      })
    if (group.accepted)
      events.push({
        ...base,
        key: JSON.stringify(['approval', group.id, group.fileId]),
        kind: 'approval',
        title: `${group.name}: research approved`,
      })
    group.comments.forEach((comment, index) =>
      events.push({
        groupId: group.id,
        key: JSON.stringify(['revision', group.id, index, comment]),
        kind: 'revision',
        title: `${group.name}: revision feedback`,
        message: comment,
      }),
    )
  }
  for (const student of workspace.students.filter(
    (item) => studentAccountStatus(item) === 'pending',
  ))
    events.push({
      key: JSON.stringify(['student', student.id]),
      kind: 'account',
      groupId: '',
      title: 'Student registration awaiting approval',
      message: student.name,
    })
  for (const teacher of workspace.teachers.filter((item) => !item.access))
    events.push({
      key: JSON.stringify(['teacher', teacher.id]),
      kind: 'account',
      groupId: '',
      title: 'Teacher registration awaiting approval',
      message: teacher.name,
    })
  return events
}

export async function identifyNotifications(candidates: InboxCandidate[]) {
  return Promise.all(
    candidates.map(async ({ key, ...event }) => {
      const hash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(key))
      return {
        ...event,
        title: event.title.slice(0, 240),
        message: event.message.slice(0, 4000),
        id: Array.from(new Uint8Array(hash), (byte) => byte.toString(16).padStart(2, '0')).join(''),
      }
    }),
  )
}

export function mergeNotifications(
  current: InboxNotification[],
  events: Awaited<ReturnType<typeof identifyNotifications>>,
  now: number,
) {
  const known = new Set(current.map((item) => item.id))
  return [
    ...events
      .filter((item) => !known.has(item.id))
      .map((item) => ({ ...item, read: false, observedAt: now })),
    ...current,
  ]
}
