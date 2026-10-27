import {
  collection,
  doc,
  runTransaction,
  serverTimestamp,
  type Firestore,
} from 'firebase/firestore'
import type { Milestone, MilestoneInput } from '../domain.ts'
import { checkMilestoneVersion, validateMilestone } from './milestones.ts'
import type { identifyNotifications } from './inbox.ts'

const teacherRef = (db: Firestore, uid: string) => doc(db, 'users/user_details/teachers', uid)

export async function saveMilestoneRecord(
  db: Firestore,
  uid: string,
  input: MilestoneInput,
  existing?: Milestone,
) {
  const value = validateMilestone(input)
  const ref = existing
    ? doc(db, 'group_milestones', existing.id)
    : doc(collection(db, 'group_milestones'))
  await runTransaction(db, async (tx) => {
    const role = await tx.get(teacherRef(db, uid))
    const group = await tx.get(doc(db, 'groups', value.groupId))
    const record = await tx.get(ref)
    if (role.data()?.access !== true) throw new Error('An approved teacher account is required.')
    if (!group.exists()) throw new Error('This group no longer exists.')
    checkMilestoneVersion(
      record.exists() ? ({ ...record.data(), id: ref.id } as Milestone) : undefined,
      existing?.version ?? null,
    )
    if (existing && record.data()?.groupId !== value.groupId)
      throw new Error('A milestone cannot be moved to another group.')
    if (existing)
      tx.update(ref, {
        ...value,
        version: existing.version + 1,
        updatedAt: serverTimestamp(),
        updatedBy: uid,
      })
    else
      tx.set(ref, {
        ...value,
        completedAt: 0,
        version: 1,
        createdAt: serverTimestamp(),
        createdBy: uid,
        updatedAt: serverTimestamp(),
        updatedBy: uid,
      })
  })
}

export async function changeMilestoneRecord(
  db: Firestore,
  uid: string,
  milestone: Milestone,
  action: 'complete' | 'reopen' | 'delete',
) {
  await runTransaction(db, async (tx) => {
    const role = await tx.get(teacherRef(db, uid))
    const ref = doc(db, 'group_milestones', milestone.id)
    const record = await tx.get(ref)
    if (role.data()?.access !== true) throw new Error('An approved teacher account is required.')
    checkMilestoneVersion(
      record.exists() ? ({ ...record.data(), id: ref.id } as Milestone) : undefined,
      milestone.version,
    )
    if (action === 'delete') tx.delete(ref)
    else
      tx.update(ref, {
        completedAt: action === 'complete' ? Date.now() : 0,
        version: milestone.version + 1,
        updatedAt: serverTimestamp(),
        updatedBy: uid,
      })
  })
}

export async function captureInboxRecords(
  db: Firestore,
  uid: string,
  events: Awaited<ReturnType<typeof identifyNotifications>>,
) {
  for (let offset = 0; offset < events.length; offset += 100) {
    const portion = events.slice(offset, offset + 100)
    await runTransaction(db, async (tx) => {
      const role = await tx.get(teacherRef(db, uid))
      if (role.data()?.access !== true) throw new Error('An approved teacher account is required.')
      const refs = portion.map((event) => doc(db, 'teacher_inboxes', uid, 'items', event.id))
      const records = await Promise.all(refs.map((ref) => tx.get(ref)))
      portion.forEach((event, index) => {
        if (!records[index].exists())
          tx.set(refs[index], {
            kind: event.kind,
            groupId: event.groupId,
            title: event.title,
            message: event.message,
            observedAt: serverTimestamp(),
            read: false,
          })
      })
    })
  }
}

export async function setInboxRead(db: Firestore, uid: string, ids: string[], read: boolean) {
  for (let offset = 0; offset < ids.length; offset += 100) {
    await runTransaction(db, async (tx) => {
      const role = await tx.get(teacherRef(db, uid))
      if (role.data()?.access !== true) throw new Error('An approved teacher account is required.')
      ids
        .slice(offset, offset + 100)
        .forEach((id) => tx.update(doc(db, 'teacher_inboxes', uid, 'items', id), { read }))
    })
  }
}
