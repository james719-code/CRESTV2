import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from 'react'
import { GroupDeletionTracker, withGroupDeletions } from './group-deletions'
import { createDemoWorkspace } from './demo'
import { readDemoFile, saveDemoFile } from './demoFiles'
import type { SessionState } from './firebase'
import { isDemo, validConfig } from './config'
import { checkMilestoneVersion, validateMilestone } from './milestones'
import { identifyNotifications, inboxCandidates, mergeNotifications } from './inbox'
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
  type Milestone,
  type MilestoneInput,
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
        return {
          ...data,
          milestones: Array.isArray(data.milestones) ? data.milestones : [],
          notifications: Array.isArray(data.notifications) ? data.notifications : [],
        }
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
  const [inboxError, setInboxError] = useState('')
  const [inboxRetry, setInboxRetry] = useState(0)
  const [retry, setRetry] = useState(0)
  const [groupDeletionTracker] = useState(() => new GroupDeletionTracker())
  const groupDeletions = useSyncExternalStore(
    groupDeletionTracker.subscribe,
    groupDeletionTracker.getSnapshot,
  )
  const accountId = useRef<string | null>(null)

  useEffect(() => {
    if (session !== 'ready' || !teacher) return
    let active = true
    void identifyNotifications(inboxCandidates(workspace))
      .then(async (events) => {
        if (!active) return
        const known = new Set((workspace.notifications || []).map((item) => item.id))
        const missing = events.filter((item) => !known.has(item.id))
        if (!missing.length) {
          setInboxError('')
          return
        }
        if (isDemo) {
          const next = {
            ...workspace,
            notifications: mergeNotifications(workspace.notifications || [], missing, Date.now()),
          }
          localStorage.setItem(storageKey, JSON.stringify(next))
          setWorkspace(next)
        } else await (await import('./firebase')).captureInbox(teacher.id, missing)
        if (active) setInboxError('')
      })
      .catch(() => {
        if (active)
          setInboxError(
            'New updates could not be saved to your inbox. Check your connection and retry.',
          )
      })
    return () => {
      active = false
    }
  }, [workspace, teacher, session, inboxRetry])

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
            const nextAccount =
              currentTeacher?.id ??
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

  async function saveMilestone(input: MilestoneInput, existing?: Milestone) {
    if (!isDemo) return (await import('./firebase')).saveMilestone(input, existing)
    const value = validateMilestone(input)
    if (!workspace.groups.some((group) => group.id === value.groupId))
      throw new Error('This group no longer exists.')
    const current = workspace.milestones?.find((item) => item.id === existing?.id)
    checkMilestoneVersion(current, existing?.version ?? null)
    if (existing && existing.groupId !== value.groupId)
      throw new Error('A milestone cannot be moved to another group.')
    const milestone: Milestone = {
      ...value,
      id: existing?.id || crypto.randomUUID(),
      version: (existing?.version || 0) + 1,
      completedAt: current?.completedAt || 0,
    }
    commitDemo({
      ...workspace,
      milestones: [
        ...(workspace.milestones || []).filter((item) => item.id !== milestone.id),
        milestone,
      ],
    })
  }

  async function changeMilestone(milestone: Milestone, action: 'complete' | 'reopen' | 'delete') {
    if (!isDemo) return (await import('./firebase')).changeMilestone(milestone, action)
    checkMilestoneVersion(
      workspace.milestones?.find((item) => item.id === milestone.id),
      milestone.version,
    )
    commitDemo({
      ...workspace,
      milestones:
        action === 'delete'
          ? (workspace.milestones || []).filter((item) => item.id !== milestone.id)
          : (workspace.milestones || []).map((item) =>
              item.id === milestone.id
                ? {
                    ...item,
                    completedAt: action === 'complete' ? Date.now() : 0,
                    version: item.version + 1,
                  }
                : item,
            ),
    })
  }

  async function markNotifications(ids: string[], read: boolean) {
    if (!isDemo) return (await import('./firebase')).markNotifications(ids, read)
    const selected = new Set(ids)
    commitDemo({
      ...workspace,
      notifications: (workspace.notifications || []).map((item) =>
        selected.has(item.id) ? { ...item, read } : item,
      ),
    })
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
    inboxError,
    retryInbox: () => setInboxRetry((value) => value + 1),
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
    saveMilestone,
    changeMilestone,
    markNotifications,
  }
}
