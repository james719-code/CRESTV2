import { useEffect, useState } from 'react'
import { createDemoWorkspace } from './demo'
import { readDemoFile, saveDemoFile } from './demoFiles'
import type { SessionState } from './firebase'
import { reviewWorkspace, type Teacher, type UploadInput, type Workspace } from '../domain'

const storageKey = 'crest-teacher-demo-v1'
const settings = [
  import.meta.env.VITE_FIREBASE_API_KEY,
  import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  import.meta.env.VITE_FIREBASE_PROJECT_ID,
  import.meta.env.VITE_FIREBASE_APP_ID,
]
const isDemo = settings.every((value) => !value)
const validConfig = settings.every(Boolean)
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

  useEffect(() => {
    if (isDemo || !validConfig) return
    let disposed = false
    let stop: (() => void) | undefined
    import('./firebase')
      .then((api) => {
        if (disposed) return
        stop = api.observeWorkspace({
          session: (state, currentTeacher) => {
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
    }
  }, [retry])

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
    if (!isDemo) return (await import('./firebase')).approveAccount(id, role)
    const next = structuredClone(workspace)
    if (role === 'student') {
      const account = next.students.find((item) => item.id === id)
      if (account) account.accepted = true
    } else {
      const account = next.teachers.find((item) => item.id === id)
      if (account) account.access = true
    }
    commitDemo(next)
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

  async function readFile(fileId: string) {
    if (!isDemo) return (await import('./firebase')).readFile(fileId)
    if (fileId.startsWith('sample-')) return undefined
    const file = await readDemoFile(fileId)
    if (!file)
      throw new Error('This demo file is no longer in your browser. Upload it again to view it.')
    return file
  }

  return {
    workspace,
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
    upload,
    readFile,
  }
}
