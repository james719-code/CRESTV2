import { initializeApp } from 'firebase/app'
import {
  getAuth,
  GoogleAuthProvider,
  onAuthStateChanged,
  signInWithPopup,
  signOut,
} from 'firebase/auth'

import {
  arrayUnion,
  collection,
  doc,
  getFirestore,
  onSnapshot,
  runTransaction,
  type DocumentData,
  type Unsubscribe,
} from 'firebase/firestore'
import type { ResearchGroup, StudentAccountStatus, StudentDecision, Teacher, UploadInput, Workspace } from '../domain'
import { researchType, validateReview } from '../domain'
import { reviewStudentAccount } from './student-accounts'
import { clearSchoolFileCache, deleteFile, uploadFile } from './storage'
import { notifyEvent } from './edge'
export { readFile } from './storage'

const app = initializeApp({
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
})
const auth = getAuth(app)
const db = getFirestore(app)
const paths = {
  students: 'users/user_details/students',
  teachers: 'users/user_details/teachers',
  groups: 'groups',
  qualitative: 'researches/research_details/qualitative',
  quantitative: 'researches/research_details/quantitative',
  documents: 'documents',
} as const

const string = (value: unknown) => (typeof value === 'string' ? value : '')
const strings = (value: unknown): string[] =>
  Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : []
const timestamp = (value: unknown): number => {
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0
  if (
    value &&
    typeof value === 'object' &&
    'toMillis' in value &&
    typeof value.toMillis === 'function'
  )
    return value.toMillis()
  return 0
}

function mapGroup(id: string, data: DocumentData): ResearchGroup {
  return {
    id,
    name: string(data.group_name),
    strand: string(data.strand),
    memberIds: strings(data.group_member),
    title: string(data.research_title),
    type: researchType(data.research_type),
    uploaded: data.uploaded === true,
    accepted: data.accepted_research === true,
    fileId: string(data.file_link),
    comments: strings(data.comments),
  }
}

export type SessionState = 'loading' | 'signed-out' | 'ready' | 'denied' | 'error'

export function observeWorkspace(callbacks: {
  session: (status: SessionState, teacher: Teacher | null) => void
  data: (workspace: Workspace) => void
  error: (message: string) => void
}) {
  let stopRole: Unsubscribe | undefined
  let stops: Unsubscribe[] = []
  let generation = 0
  const clearData = () => {
    generation++
    stops.forEach((stop) => stop())
    stops = []
  }
  const stopAuth = onAuthStateChanged(
    auth,
    (user) => {
      stopRole?.()
      clearData()
      if (!user) {
        void clearSchoolFileCache()
        callbacks.session('signed-out', null)
        return
      }
      callbacks.session('loading', null)
      let watching = false
      stopRole = onSnapshot(
        doc(db, paths.teachers, user.uid),
        (role) => {
          if (!role.exists() || role.data().access !== true) {
            void clearSchoolFileCache()
            clearData()
            watching = false
            callbacks.session('denied', null)
            return
          }
          const data = role.data()
          const teacher: Teacher = {
            id: user.uid,
            name: string(data.name) || 'Teacher',
            email: string(data.email),
            access: true,
          }
          if (watching) {
            callbacks.session('ready', teacher)
            return
          }
          watching = true
          const currentGeneration = generation
          const values = new Map<string, { id: string; data: DocumentData }[]>()
          const sources = Object.entries(paths)
          stops = sources.map(([key, path]) =>
            onSnapshot(
              collection(db, path),
              (snapshot) => {
                if (generation !== currentGeneration) return
                values.set(
                  key,
                  snapshot.docs.map((item) => ({ id: item.id, data: item.data() })),
                )
                if (values.size !== sources.length) return
                const rows = (key: string) => values.get(key) || []
                callbacks.data({
                  students: rows('students').map(({ id, data }) => ({
                    id,
                    name: string(data.name),
                    strand: string(data.strand),
                    groupId: string(data.groupId) || string(data.group_id),
                    accepted: data.accepted === true,
                    declined: data.declined === true,
                  })),
                  teachers: rows('teachers').map(({ id, data }) => ({
                    id,
                    name: string(data.name),
                    email: string(data.email),
                    access: data.access === true,
                  })),
                  groups: rows('groups').map(({ id, data }) => mapGroup(id, data)),
                  research: (['qualitative', 'quantitative'] as const)
                    .flatMap((type) =>
                      rows(type).map(({ id, data }) => ({
                        id,
                        type,
                        title: string(data.title),
                        strand: string(data.strand),
                        members: strings(data.members),
                        fileId: string(data.file_link),
                        createdAt: timestamp(data.createdAt),
                        downloads:
                          typeof data.downloads === 'number' ? Math.max(0, data.downloads) : 0,
                        unfinished: data.unfinished === true,
                      })),
                    )
                    .sort((a, b) => b.createdAt - a.createdAt),
                  documents: rows('documents')
                    .map(({ id, data }) => ({
                      id,
                      name: string(data.name),
                      description: string(data.description),
                      fileId: string(data.file_link),
                      createdAt: timestamp(data.createdAt),
                    }))
                    .sort((a, b) => b.createdAt - a.createdAt),
                })
                callbacks.session('ready', teacher)
              },
              () => {
                if (generation !== currentGeneration) return
                clearData()
                callbacks.error(
                  'Your school records could not be loaded. Check your connection and teacher permissions, then try again.',
                )
                callbacks.session('error', teacher)
              },
            ),
          )
        },
        () => {
          clearData()
          callbacks.error(
            'Your teacher access could not be verified. Check your connection and try again.',
          )
          callbacks.session('error', null)
        },
      )
    },
    () => {
      callbacks.error('Sign-in could not be restored. Please try again.')
      callbacks.session('error', null)
    },
  )
  return () => {
    stopAuth()
    stopRole?.()
    clearData()
  }
}

export async function login() {
  await signInWithPopup(auth, new GoogleAuthProvider())
}

export async function logout() {
  await signOut(auth)
  await clearSchoolFileCache()
}

function currentTeacherRef() {
  if (!auth.currentUser) throw new Error('Sign in again before making changes.')
  return doc(db, paths.teachers, auth.currentUser.uid)
}

export async function reviewGroup(id: string, decision: 'approve' | 'changes', feedback: string) {
  const roleRef = currentTeacherRef()
  const groupRef = doc(db, paths.groups, id)
  await runTransaction(db, async (tx) => {
    const role = await tx.get(roleRef)
    if (role.data()?.access !== true) throw new Error('An approved teacher account is required.')
    const record = await tx.get(groupRef)
    if (!record.exists()) throw new Error('This group no longer exists. Refresh your workspace.')
    const group = mapGroup(id, record.data())
    validateReview(group, decision, feedback)
    if (decision === 'changes') {
      // Preserve the previous file and title so feedback remains understandable.
      // Android uses uploaded/accepted_research and comments for the same workflow.
      tx.update(groupRef, {
        uploaded: false,
        accepted_research: false,
        comments: arrayUnion(feedback.trim()),
      })
      return
    }
    const members = await Promise.all(
      group.memberIds.map((uid) => tx.get(doc(db, paths.students, uid))),
    )
    if (members.some((member) => !member.exists() || !string(member.data()?.name)))
      throw new Error('Some group members could not be verified. Check the student records first.')
    const researchRef = doc(
      collection(db, group.type === 'qualitative' ? paths.qualitative : paths.quantitative),
    )
    tx.set(researchRef, {
      title: group.title,
      members: members.map((member) => string(member.data()?.name)),
      strand: group.strand,
      downloads: 0,
      unfinished: false,
      file_link: group.fileId,
      createdAt: Date.now(),
    })
    tx.update(groupRef, { accepted_research: true, uploaded: false })
  })
  await notifyEvent('group-reviewed', id)
}

export async function approveAccount(id: string, role: 'student' | 'teacher') {
  if (role === 'student') return reviewStudent(id, 'approve', 'pending')
  const roleRef = currentTeacherRef()
  const accountRef = doc(db, paths.teachers, id)
  await runTransaction(db, async (tx) => {
    const teacher = await tx.get(roleRef)
    const account = await tx.get(accountRef)
    if (teacher.data()?.access !== true) throw new Error('An approved teacher account is required.')
    if (!account.exists()) throw new Error('This account no longer exists.')
    if (account.data()?.access === true) throw new Error('This account has already been approved.')
    tx.update(accountRef, { access: true })
  })
  await notifyEvent('account-approved', id, role)
}

export async function reviewStudent(id: string, decision: StudentDecision, expected: StudentAccountStatus) {
  await reviewStudentAccount(db, currentTeacherRef().id, id, decision, expected)
  if (decision === 'approve') await notifyEvent('account-approved', id, 'student')
}

export async function upload(input: UploadInput) {
  // Check access before touching storage; the metadata transaction checks it again.
  const roleRef = currentTeacherRef()
  await runTransaction(db, async (tx) => {
    const role = await tx.get(roleRef)
    if (role.data()?.access !== true) throw new Error('An approved teacher account is required.')
  })
  const fileId = await uploadFile(input.file, input.kind === 'document' ? 'document' : 'research')
  try {
    await runTransaction(db, async (tx) => {
      const role = await tx.get(roleRef)
      if (role.data()?.access !== true)
        throw new Error('Your teacher access has changed. Sign in again.')
      if (input.kind === 'document') {
        tx.set(doc(collection(db, paths.documents)), {
          name: input.title,
          description: input.description,
          file_link: fileId,
          createdAt: Date.now(),
        })
      } else {
        tx.set(doc(collection(db, paths[input.type])), {
          title: input.title,
          members: input.authors,
          strand: input.strand,
          type: input.type.toUpperCase(),
          file_link: fileId,
          createdAt: Date.now(),
          downloads: 0,
          unfinished: false,
        })
      }
    })
  } catch (error) {
    await deleteFile(fileId).catch(() => undefined)
    throw error
  }
}
