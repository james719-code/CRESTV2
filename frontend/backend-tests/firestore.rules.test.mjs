import { readFile } from 'node:fs/promises'
import { before, after, test } from 'node:test'
import assert from 'node:assert/strict'
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing'
import { collection, doc, getDoc, getDocs, setDoc, updateDoc, deleteDoc, writeBatch, query, where, documentId, increment, serverTimestamp } from 'firebase/firestore'
import { reviewStudentAccount } from '../src/data/student-accounts.ts'
import { deleteDeclinedStudent, deleteResearchGroup } from '../src/data/workspace-deletion.ts'
import { captureInboxRecords, changeMilestoneRecord, saveMilestoneRecord, setInboxRead } from '../src/data/teacher-planning.ts'

let env
const students = 'users/user_details/students'
const teachers = 'users/user_details/teachers'
const studentData = (uid, extra = {}) => ({ uid, name: 'Student', gender: 'Other', strand: 'STEM', lrn: 123456789012, accepted: true, research_accepted: false, groupId: '', group_id: '', ...extra })
const groupData = (members, extra = {}) => ({ group_name: 'Test group', strand: 'STEM', group_member: members, file_link: '', uploaded: false, accepted_research: false, research_type: '', comments: [], ...extra })
const db = uid => env.authenticatedContext(uid, { email: `${uid}@example.test` }).firestore()
before(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-crest', firestore: { host: '127.0.0.1', port: 8085, rules: await readFile(new URL('../../firebase/firestore.rules', import.meta.url), 'utf8') } })
  await env.clearFirestore()
  await env.withSecurityRulesDisabled(async context => {
    const admin = context.firestore()
    await Promise.all([
      setDoc(doc(admin, teachers, 'teacher'), { uid: 'teacher', name: 'Teacher', email: 'teacher@example.test', access: true, upload_count: 0 }),
      setDoc(doc(admin, teachers, 'planner'), { uid: 'planner', name: 'Planner', access: true }),
      setDoc(doc(admin, teachers, 'planner-peer'), { uid: 'planner-peer', name: 'Peer', access: true }),
      setDoc(doc(admin, teachers, 'pending-teacher'), { uid: 'pending-teacher', name: 'Pending', email: 'pending-teacher@example.test', access: false, upload_count: 0 }),
      ...['student', 'peer', 'outsider', 'joiner', 'creator'].map(uid => setDoc(doc(admin, students, uid), studentData(uid, ['student', 'peer'].includes(uid) ? { groupId: 'team', group_id: 'team' } : {}))),
      setDoc(doc(admin, students, 'pending'), studentData('pending', { accepted: false })),
      setDoc(doc(admin, 'groups', 'team'), groupData(['student', 'peer'])),
      setDoc(doc(admin, 'groups', 'other'), groupData(['outsider'])),
      setDoc(doc(admin, 'researches/research_details/qualitative', 'paper'), { title: 'Paper', downloads: 0, createdAt: 1, file_link: 'legacy' }),
      setDoc(doc(admin, 'documents', 'document'), { name: 'Document' }),
      setDoc(doc(admin, 'users', 'legacy-user'), { name: 'Legacy fixture', password: 'test-only-placeholder' }),
      setDoc(doc(admin, 'researches', 'legacy-paper'), { name: 'Legacy fixture' }),
    ])
  })
})
after(async () => { await env?.cleanup() })

test('students read only milestones for their own group using a constrained query', async () => {
  await env.withSecurityRulesDisabled(async context => {
    const admin = context.firestore()
    await setDoc(doc(admin, 'group_milestones/own-deadline'), { groupId: 'team', title: 'Proposal', stage: 'proposal', dueDate: '2026-11-01' })
    await setDoc(doc(admin, 'group_milestones/other-deadline'), { groupId: 'other', title: 'Other proposal', stage: 'proposal', dueDate: '2026-11-02' })
  })
  await assertSucceeds(getDoc(doc(db('student'), 'group_milestones/own-deadline')))
  await assertSucceeds(getDocs(query(collection(db('student'), 'group_milestones'), where('groupId', '==', 'team'))))
  await assertFails(getDocs(collection(db('student'), 'group_milestones')))
  await assertFails(getDoc(doc(db('student'), 'group_milestones/other-deadline')))
  await assertFails(getDocs(query(collection(db('student'), 'group_milestones'), where('groupId', '==', 'other'))))
  await assertFails(updateDoc(doc(db('student'), 'group_milestones/own-deadline'), { completedAt: 1 }))
})

test('student inboxes are private and cannot capture another group’s updates', async () => {
  const id = 'd'.repeat(64)
  const event = { kind: 'revision', groupId: 'team', title: 'Feedback', message: 'Clarify sources.', observedAt: serverTimestamp(), read: false }
  await assertSucceeds(setDoc(doc(db('student'), 'student_inboxes/student/items', id), event))
  await assertSucceeds(updateDoc(doc(db('student'), 'student_inboxes/student/items', id), { read: true }))
  await assertFails(updateDoc(doc(db('student'), 'student_inboxes/student/items', id), { message: 'Replaced' }))
  await assertFails(getDoc(doc(db('peer'), 'student_inboxes/student/items', id)))
  await assertFails(getDoc(doc(db('teacher'), 'student_inboxes/student/items', id)))
  await assertFails(setDoc(doc(db('student'), 'student_inboxes/student/items', 'e'.repeat(64)), { ...event, groupId: 'other' }))
  await assertFails(setDoc(doc(db('peer'), 'student_inboxes/student/items', 'f'.repeat(64)), event))
})

test('milestone transactions persist plans, complete and reopen them, and reject stale changes', async () => {
  const input = { groupId: 'team', title: '  Submit proposal  ', stage: 'proposal', dueDate: '2026-11-01' }
  await saveMilestoneRecord(db('planner'), 'planner', input)
  const records = await getDocs(collection(db('planner'), 'group_milestones'))
  const record = records.docs.find((item) => item.data().title === 'Submit proposal')
  assert.ok(record)
  const milestone = { ...record.data(), id: record.id }
  assert.equal(milestone.version, 1)
  await changeMilestoneRecord(db('planner-peer'), 'planner-peer', milestone, 'complete')
  const completed = { ...(await getDoc(record.ref)).data(), id: record.id }
  assert.ok(completed.completedAt > 0)
  assert.equal(completed.version, 2)
  await assert.rejects(saveMilestoneRecord(db('planner'), 'planner', { ...input, title: 'Stale title' }, milestone), /has changed/)
  await assert.rejects(changeMilestoneRecord(db('planner'), 'planner', milestone, 'delete'), /has changed/)
  await changeMilestoneRecord(db('planner'), 'planner', completed, 'reopen')
  const reopened = { ...(await getDoc(record.ref)).data(), id: record.id }
  assert.equal(reopened.completedAt, 0)
  await saveMilestoneRecord(db('planner'), 'planner', { ...input, title: 'Updated title' }, reopened)
  const edited = { ...(await getDoc(record.ref)).data(), id: record.id }
  assert.equal(edited.title, 'Updated title')
  await changeMilestoneRecord(db('planner'), 'planner', edited, 'delete')
  assert.equal((await getDoc(record.ref)).exists(), false)
})

test('students, pending teachers and anonymous clients cannot read or write milestone plans', async () => {
  const input = { groupId: 'team', title: 'Restricted plan', stage: 'chapter', dueDate: '2026-11-02' }
  for (const client of [db('student'), db('pending'), db('pending-teacher'), env.unauthenticatedContext().firestore()]) {
    await assertFails(getDocs(collection(client, 'group_milestones')))
    await assertFails(setDoc(doc(client, 'group_milestones', 'forged'), input))
  }
  await assertFails(saveMilestoneRecord(db('student'), 'planner', input))
  await assert.rejects(saveMilestoneRecord(db('planner'), 'planner', { ...input, groupId: 'missing-group' }), /no longer exists/)
  await assert.rejects(saveMilestoneRecord(db('planner'), 'planner', { ...input, dueDate: '2026-02-30' }), /valid deadline/)
})

test('each teacher inbox is private; reconnect cannot reset read status or rewrite saved content', async () => {
  const event = { id: 'a'.repeat(64), kind: 'submission', groupId: 'team', title: 'Submission', message: 'Please review.' }
  await captureInboxRecords(db('planner'), 'planner', [event])
  const ref = doc(db('planner'), 'teacher_inboxes/planner/items', event.id)
  assert.equal((await getDoc(ref)).data().read, false)
  await setInboxRead(db('planner'), 'planner', [event.id], true)
  await captureInboxRecords(db('planner'), 'planner', [{ ...event, message: 'Replacement' }])
  assert.equal((await getDoc(ref)).data().read, true)
  assert.equal((await getDoc(ref)).data().message, event.message)
  await assertFails(updateDoc(ref, { message: 'Forged content' }))
  for (const client of [db('planner-peer'), db('student'), env.unauthenticatedContext().firestore()]) {
    await assertFails(getDoc(doc(client, 'teacher_inboxes/planner/items', event.id)))
    await assertFails(getDocs(collection(client, 'teacher_inboxes/planner/items')))
    await assertFails(updateDoc(doc(client, 'teacher_inboxes/planner/items', event.id), { read: false }))
  }
  await assertFails(captureInboxRecords(db('planner-peer'), 'planner', [{ ...event, id: 'b'.repeat(64) }]))
  await captureInboxRecords(db('planner-peer'), 'planner-peer', [event])
  assert.equal((await getDoc(doc(db('planner-peer'), 'teacher_inboxes/planner-peer/items', event.id))).data().read, false)
})

test('losing teacher approval blocks both milestone changes and private inbox access', async () => {
  await env.withSecurityRulesDisabled(context => setDoc(doc(context.firestore(), teachers, 'revoked-planner'), { uid: 'revoked-planner', access: false }))
  await assert.rejects(captureInboxRecords(db('revoked-planner'), 'revoked-planner', [{ id: 'c'.repeat(64), kind: 'approval', groupId: 'team', title: 'Approved', message: '' }]), /approved teacher/)
  await assertFails(getDocs(collection(db('revoked-planner'), 'teacher_inboxes/revoked-planner/items')))
  await assertFails(getDocs(collection(db('revoked-planner'), 'group_milestones')))
})

test('deleting declined accounts clears all memberships and refuses restored or pending students', async () => {
  const uid = 'delete-declined'
  await env.withSecurityRulesDisabled(async context => {
    const admin = context.firestore()
    await Promise.all([
      setDoc(doc(admin, students, uid), studentData(uid, { accepted: false, declined: true, groupId: 'delete-membership', group_id: 'delete-membership' })),
      setDoc(doc(admin, 'groups/delete-membership'), groupData([uid, 'peer'])),
      setDoc(doc(admin, 'groups/delete-stale-membership'), groupData([uid])),
      setDoc(doc(admin, students, 'delete-restored'), studentData('delete-restored', { accepted: true, declined: true })),
    ])
  })
  await assert.rejects(deleteDeclinedStudent(db('teacher'), 'teacher', 'pending'), /Only declined/)
  await assert.rejects(deleteDeclinedStudent(db('teacher'), 'teacher', 'delete-restored'), /Only declined/)
  await assertSucceeds(deleteDeclinedStudent(db('teacher'), 'teacher', uid))
  assert.equal((await getDoc(doc(db('teacher'), students, uid))).exists(), false)
  assert.deepEqual((await getDoc(doc(db('teacher'), 'groups/delete-membership'))).data().group_member, ['peer'])
  assert.deepEqual((await getDoc(doc(db('teacher'), 'groups/delete-stale-membership'))).data().group_member, [])
  assert.equal((await getDoc(doc(db('teacher'), 'researches/research_details/qualitative/paper'))).exists(), true)
})

test('group deletion cleans both profile field spellings, including stale links, and preserves other groups', async () => {
  const id = 'delete-group'
  await env.withSecurityRulesDisabled(async context => {
    const admin = context.firestore()
    await Promise.all([
      setDoc(doc(admin, 'groups', id), groupData(['delete-member', 'missing-member', 'moved-member'], { accepted_research: true, file_link: 'legacy' })),
      setDoc(doc(admin, students, 'delete-member'), studentData('delete-member', { groupId: id, group_id: id })),
      setDoc(doc(admin, students, 'stale-member'), studentData('stale-member', { group_id: id })),
      setDoc(doc(admin, students, 'legacy-only-member'), { uid: 'legacy-only-member', name: 'Legacy student', accepted: true, group_id: id }),
      setDoc(doc(admin, students, 'moved-member'), studentData('moved-member', { groupId: 'other', group_id: 'other' })),
    ])
  })
  await assertSucceeds(deleteResearchGroup(db('teacher'), 'teacher', id))
  assert.equal((await getDoc(doc(db('teacher'), 'groups', id))).exists(), false)
  for (const uid of ['delete-member', 'stale-member']) {
    const data = (await getDoc(doc(db('teacher'), students, uid))).data()
    assert.equal(data.groupId, '')
    assert.equal(data.group_id, '')
  }
  assert.equal((await getDoc(doc(db('teacher'), students, 'moved-member'))).data().groupId, 'other')
  assert.equal((await getDoc(doc(db('teacher'), students, 'legacy-only-member'))).data().group_id, '')
  assert.equal((await getDoc(doc(db('teacher'), 'researches/research_details/qualitative/paper'))).exists(), true)
})

test('students and unapproved teachers cannot use teacher deletion operations', async () => {
  await assertFails(deleteResearchGroup(db('student'), 'teacher', 'team'))
  await assertFails(deleteDeclinedStudent(db('student'), 'teacher', 'pending'))
  await env.withSecurityRulesDisabled(context => setDoc(doc(context.firestore(), teachers, 'deletion-no-access'), { uid: 'deletion-no-access', access: false }))
  await assertFails(deleteResearchGroup(db('deletion-no-access'), 'deletion-no-access', 'team'))
})

test('legacy parent records stay inaccessible to every client role', async () => {
  for (const client of [env.unauthenticatedContext().firestore(), db('pending'), db('student'), db('teacher')]) {
    for (const [collectionPath, id] of [['users', 'legacy-user'], ['researches', 'legacy-paper']]) {
      await assertFails(getDoc(doc(client, collectionPath, id)))
      await assertFails(getDocs(collection(client, collectionPath)))
    }
  }
})

test('anonymous users cannot read or alter school records', async () => {
  const anon = env.unauthenticatedContext().firestore()
  for (const path of [`${students}/student`, `${teachers}/teacher`, 'groups/team', 'documents/document', 'researches/research_details/qualitative/paper']) {
    await assertFails(getDoc(doc(anon, path)))
    await assertFails(setDoc(doc(anon, path), { access: true }))
  }
})
test('pending accounts read their own status but cannot browse records or self-approve', async () => {
  await assertSucceeds(getDoc(doc(db('pending'), students, 'pending')))
  await assertFails(getDocs(collection(db('pending'), students)))
  await assertFails(getDoc(doc(db('pending'), 'documents/document')))
  await assertFails(updateDoc(doc(db('pending'), students, 'pending'), { accepted: true }))
  await assertFails(updateDoc(doc(db('pending-teacher'), teachers, 'pending-teacher'), { access: true }))
})
test('new profiles must belong to the caller and start unapproved', async () => {
  await assertFails(setDoc(doc(db('new'), students, 'new'), studentData('new')))
  await assertFails(setDoc(doc(db('new'), students, 'victim'), studentData('victim', { accepted: false })))
  await assertSucceeds(setDoc(doc(db('new'), students, 'new'), studentData('new', { accepted: false })))
  await assertSucceeds(setDoc(doc(db('new-teacher'), teachers, 'new-teacher'), { uid: 'new-teacher', name: 'New teacher', email: 'new-teacher@example.test', access: false, upload_count: 0 }))
})
test('students cannot rewrite approval, role, strand, or another profile', async () => {
  await assertFails(updateDoc(doc(db('student'), students, 'student'), { accepted: true, strand: 'ABM' }))
  await assertFails(updateDoc(doc(db('student'), students, 'peer'), { name: 'Changed' }))
  await assertSucceeds(updateDoc(doc(db('student'), students, 'student'), { name: 'My new name' }))
})
test('students can read group peers by ID without browsing all private student records', async () => {
  await assertSucceeds(getDocs(query(collection(db('student'), students), where(documentId(), 'in', ['student', 'peer']))))
  await assertFails(getDoc(doc(db('student'), students, 'outsider')))
  await assertFails(getDocs(collection(db('student'), students)))
  await assertFails(getDocs(collection(db('student'), teachers)))
})
test('approved teachers can manage accounts and load the workspace', async () => {
  for (const path of [students, teachers, 'groups', 'documents', 'researches/research_details/qualitative', 'researches/research_details/quantitative']) await assertSucceeds(getDocs(collection(db('teacher'), path)))
  await assertSucceeds(updateDoc(doc(db('teacher'), students, 'pending'), { accepted: true }))
  await assertSucceeds(updateDoc(doc(db('teacher'), teachers, 'pending-teacher'), { access: true }))
})
test('only teachers can publish records; students can increment downloads by exactly one', async () => {
  const student = doc(db('student'), 'researches/research_details/qualitative/paper')
  await assertSucceeds(updateDoc(student, { downloads: increment(1) }))
  await assertFails(updateDoc(student, { downloads: 999 }))
  await assertFails(updateDoc(student, { title: 'Replaced' }))
  await assertFails(setDoc(doc(db('student'), 'documents/forged'), { name: 'Fake' }))
  await assertSucceeds(setDoc(doc(db('teacher'), 'documents/teacher-upload'), { name: 'Real' }))
})
test('students cannot approve submissions, change members, or edit other groups', async () => {
  await assertFails(updateDoc(doc(db('student'), 'groups/team'), { accepted_research: true }))
  await assertFails(updateDoc(doc(db('student'), 'groups/team'), { group_member: ['student', 'outsider'] }))
  await assertFails(updateDoc(doc(db('student'), 'groups/other'), { uploaded: true }))
  await assertSucceeds(updateDoc(doc(db('student'), 'groups/team'), { uploaded: true, file_link: 'sb_00000000-0000-0000-0000-000000000001', research_type: 'qualitative', research_title: 'Our submission' }))
})
test('creating a group requires an atomic matching profile update', async () => {
  const creator = db('creator')
  await assertFails(setDoc(doc(creator, 'groups/new-team'), groupData(['creator'])))
  const batch = writeBatch(creator)
  batch.set(doc(creator, 'groups/new-team'), groupData(['creator']))
  batch.update(doc(creator, students, 'creator'), { groupId: 'new-team', group_id: 'new-team' })
  await assertSucceeds(batch.commit())
})
test('joining and leaving only changes the caller membership, atomically', async () => {
  const joiner = db('joiner')
  await assertFails(updateDoc(doc(joiner, students, 'joiner'), { groupId: 'other', group_id: 'other' }))
  const join = writeBatch(joiner)
  join.update(doc(joiner, 'groups/other'), { group_member: ['outsider', 'joiner'] })
  join.update(doc(joiner, students, 'joiner'), { groupId: 'other', group_id: 'other' })
  await assertSucceeds(join.commit())
  const leave = writeBatch(joiner)
  leave.update(doc(joiner, 'groups/other'), { group_member: ['outsider'] })
  leave.update(doc(joiner, students, 'joiner'), { groupId: '', group_id: '' })
  await assertSucceeds(leave.commit())
})
test('favorites are private to their owner', async () => {
  const data = { researchId: 'paper', type: 'QUALITATIVE', updatedAt: 1 }
  await assertSucceeds(setDoc(doc(db('student'), 'user_favorites/student/items/paper'), data))
  await assertFails(getDoc(doc(db('peer'), 'user_favorites/student/items/paper')))
  await assertFails(setDoc(doc(db('peer'), 'user_favorites/student/items/paper'), data))
  await assertSucceeds(deleteDoc(doc(db('student'), 'user_favorites/student/items/paper')))
})

test('teacher student-management transactions decline, restore, and revoke access without changing membership', async () => {
  const uid = 'managed-student'
  await env.withSecurityRulesDisabled(context => setDoc(doc(context.firestore(), students, uid), studentData(uid, { accepted: false, groupId: 'team', group_id: 'team' })))
  await reviewStudentAccount(db('teacher'), 'teacher', uid, 'decline', 'pending')
  let record = (await getDoc(doc(db('teacher'), students, uid))).data()
  if (record.accepted !== false || record.declined !== true || record.groupId !== 'team') throw new Error('Decline changed the wrong fields')
  await assertFails(getDoc(doc(db(uid), 'documents/document')))
  await assertFails(updateDoc(doc(db(uid), students, uid), { accepted: true, declined: false }))
  await reviewStudentAccount(db('teacher'), 'teacher', uid, 'approve', 'declined')
  await assertSucceeds(getDoc(doc(db(uid), 'documents/document')))
  await assert.rejects(reviewStudentAccount(db('teacher'), 'teacher', uid, 'decline', 'pending'), /has changed/)
  await reviewStudentAccount(db('teacher'), 'teacher', uid, 'decline', 'approved')
  await assertFails(getDoc(doc(db(uid), 'documents/document')))
  record = (await getDoc(doc(db('teacher'), students, uid))).data()
  if (record.groupId !== 'team' || record.name !== 'Student') throw new Error('Student records were changed')
})

test('students cannot impersonate teacher transactions or change decline status', async () => {
  await assertFails(reviewStudentAccount(db('student'), 'teacher', 'pending', 'decline', 'approved'))
  await assertFails(updateDoc(doc(db('pending'), students, 'pending'), { declined: true }))
  await assertFails(updateDoc(doc(db('student'), students, 'student'), { declined: true }))
})
