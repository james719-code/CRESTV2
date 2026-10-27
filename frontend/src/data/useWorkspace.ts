import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from 'react'
import { GroupDeletionTracker, withGroupDeletions } from './group-deletions'
import { createDemoWorkspace } from './demo'
import { readDemoFile, saveDemoFile } from './demoFiles'
import type { SessionState } from './firebase'
import { isDemo, validConfig } from './config'
import {
  deleteStudentWorkspace,
  deleteGroupWorkspace,
  reviewStudentWorkspace,
  reviewWorkspace,
  type StudentAccountStatus,
  type StudentDecision,
  type Teacher,
  type UploadInput,
  type Workspace,
} from '../domain'

const storageKey = 'crest-teacher-demo-v1'
const emptyWorkspace: Workspace = {
  students: [],
  teachers: [],
  groups: [],
  research: [],
  documents: [],
}

function loadDemo(): Workspace {
  try {
    const saved = localStorage.getItem(storageKey)
    if (saved) {
      const data: Workspace = JSON.parse(saved)
      if (
        ['students', 'teachers', 'groups', 'research', 'documents'].every((key) =>
          Array.isArray(data[key as keyof Workspace]),
        )
      )
        return data
    }
  } catch {
    /* Start a clean demo if storage is unavailable or corrupt. */
  }
  return createDemoWorkspace()
}

export function useWorkspace() {
  const [workspace, setWorkspace] = useState<Workspace>(() =>
    isDemo ? loadDemo() : emptyWorkspace,
  )
  const [teacher, setTeacher] = useState<Teacher | null>(() =>
    isDemo ? createDemoWorkspace().teachers[0] : null,
  )
  const [session, setSession] = useState<SessionState>(isDemo ? 'ready' : 'loading')
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const [groupDeletionTracker] = useState(() => new GroupDeletionTracker())
  const groupDeletions = useSyncExternalStore(
    groupDeletionTracker.subscribe,
    groupDeletionTracker.getSnapshot,
  )
  const accountId = useRef<string | null>(null)

  useEffect(() => {
    if (isDemo || !validConfig) return
    let disposed = false
    let stop: (() => void) | undefined
    import('./firebase')
      .then((api) => {
        if (disposed) return
        stop = api.observeWorkspace({
          session: (state, currentTeacher) => {
            // Reconnecting after the public page is not an account change.
            const nextAccount = currentTeacher?.id ??
              (state === 'signed-out' || state === 'denied' ? null : accountId.current)
            if (accountId.current !== nextAccount) {
              accountId.current = nextAccount
              groupDeletionTracker.reset()
            }
            setSession(state)
            setTeacher(currentTeacher)
            if (state === 'denied' || state === 'signed-out') setWorkspace(emptyWorkspace)
          },
          data: (data) => {
            setWorkspace(data)
            setError('')
          },
          error: setError,
        })
      })
      .catch(() => {
        if (!disposed) {
          setError('The school connection could not be started. Check the website configuration.')
          setSession('error')
        }
      })
    return () => {
      disposed = true
      stop?.()
      // Activity pauses this observer on the public page. Require a fresh role
      // check before showing retained state when the workspace becomes visible.
      setSession('loading')
      setWorkspace(emptyWorkspace)
    }
  }, [retry, groupDeletionTracker])

  function commitDemo(next: Workspace) {
    try {
      localStorage.setItem(storageKey, JSON.stringify(next))
    } catch {
      throw new Error(
        'Your browser could not save this change. Allow site storage or free some space and try again.',
      )
    }
    setWorkspace(next)
  }

  async function review(id: string, decision: 'approve' | 'changes', feedback = '') {
    if (!isDemo) return (await import('./firebase')).reviewGroup(id, decision, feedback)
    commitDemo(reviewWorkspace(workspace, id, decision, feedback))
  }

  async function approveAccount(id: string, role: 'student' | 'teacher') {
    if (role === 'student') return reviewStudent(id, 'approve', 'pending')
    if (!isDemo) return (await import('./firebase')).approveAccount(id, role)
    const next = structuredClone(workspace)
    const account = next.teachers.find((item) => item.id === id)
    if (!account) throw new Error('This account no longer exists.')
    if (account.access) throw new Error('This account has already been approved.')
    account.access = true
    commitDemo(next)
  }

  async function reviewStudent(
    id: string,
    decision: StudentDecision,
    expected: StudentAccountStatus,
  ) {
    if (!isDemo) return (await import('./firebase')).reviewStudent(id, decision, expected)
    commitDemo(reviewStudentWorkspace(workspace, id, decision, expected))
  }

  async function upload(input: UploadInput) {
    if (!isDemo) return (await import('./firebase')).upload(input)
    const fileId = `local-${crypto.randomUUID()}`
    await saveDemoFile(fileId, input.file)
    const next = structuredClone(workspace)
    if (input.kind === 'document')
      next.documents.unshift({
        id: crypto.randomUUID(),
        name: input.title,
        description: input.description,
        createdAt: Date.now(),
        fileId,
      })
    else
      next.research.unshift({
        id: crypto.randomUUID(),
        title: input.title,
        strand: input.strand,
        members: input.authors,
        type: input.type,
        fileId,
        createdAt: Date.now(),
        downloads: 0,
        unfinished: false,
      })
    commitDemo(next)
  }

  async function deleteStudent(id: string) {
    if (!isDemo) return (await import('./firebase')).deleteStudent(id)
    commitDemo(deleteStudentWorkspace(workspace, id))
  }

  function deleteGroup(id: string): Promise<void> {
    const group =
      workspace.groups.find((item) => item.id === id) || groupDeletions.operations[id]?.group
    if (!group) return Promise.reject(new Error('This group no longer exists.'))
    return groupDeletionTracker.delete(group, async () => {
      if (!isDemo) return (await import('./firebase')).deleteGroup(id)
      commitDemo(deleteGroupWorkspace(workspace, id))
    })
  }

  const readFile = useCallback(async (fileId: string) => {
    if (!isDemo) return (await import('./firebase')).readFile(fileId)
    if (fileId.startsWith('sample-')) return undefined
    const file = await readDemoFile(fileId)
    if (!file)
      throw new Error('This demo file is no longer in your browser. Upload it again to view it.')
    return file
  }, [])

  return {
    workspace: withGroupDeletions(workspace, groupDeletions),
    groupDeletions: groupDeletions.operations,
    teacher,
    session,
    isDemo,
    error,
    validConfig: isDemo || validConfig,
    refresh: () => {
      setSession('loading')
      setError('')
      setRetry((value) => value + 1)
    },
    login: async () => (await import('./firebase')).login(),
    logout: async () => (await import('./firebase')).logout(),
    review,
    approveAccount,
    reviewStudent,
    deleteStudent,
    deleteGroup,
    upload,
    readFile,
  }
}
