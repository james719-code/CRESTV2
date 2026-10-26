import { readFile } from 'node:fs/promises'
import { before, after, test } from 'node:test'
import assert from 'node:assert/strict'
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing'
import { collection, doc, getDoc, getDocs, setDoc, updateDoc, deleteDoc, writeBatch, query, where, documentId, increment } from 'firebase/firestore'
import { reviewStudentAccount } from '../src/data/student-accounts.ts'

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
