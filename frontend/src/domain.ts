export type ResearchType = 'qualitative' | 'quantitative'
export type GroupStatus = 'preparing' | 'pending' | 'changes' | 'approved'
export type Page =
  'overview' | 'groups' | 'reviews' | 'students' | 'research' | 'documents' | 'accounts'

export interface Student {
  id: string
  name: string
  strand: string
  groupId: string
  accepted: boolean
  declined?: boolean
}

export type StudentAccountStatus = 'pending' | 'approved' | 'declined'
export type StudentDecision = 'approve' | 'decline'

export const studentAccountStatus = (
  student: Pick<Student, 'accepted' | 'declined'>,
): StudentAccountStatus =>
  student.accepted ? 'approved' : student.declined ? 'declined' : 'pending'

export const studentAccountLabels: Record<StudentAccountStatus, string> = {
  pending: 'Pending approval',
  approved: 'Approved',
  declined: 'Declined',
}

export function studentDecisionUpdate(
  student: Pick<Student, 'accepted' | 'declined'>,
  decision: StudentDecision,
  expected: StudentAccountStatus,
) {
  if (studentAccountStatus(student) !== expected)
    throw new Error('This account has changed. Check its current status and try again.')
  if (
    (decision === 'approve' && expected === 'approved') ||
    (decision === 'decline' && expected === 'declined')
  )
    throw new Error('This account already has that status.')
  return { accepted: decision === 'approve', declined: decision === 'decline' }
}

export function reviewStudentWorkspace(
  workspace: Workspace,
  id: string,
  decision: StudentDecision,
  expected: StudentAccountStatus,
): Workspace {
  const next = structuredClone(workspace)
  const student = next.students.find((item) => item.id === id)
  if (!student) throw new Error('This student account no longer exists.')
  Object.assign(student, studentDecisionUpdate(student, decision, expected))
  return next
}

export interface Teacher {
  id: string
  name: string
  email: string
  access: boolean
}

export interface ResearchGroup {
  id: string
  name: string
  strand: string
  memberIds: string[]
  title: string
  type: ResearchType | ''
  uploaded: boolean
  accepted: boolean
  fileId: string
  comments: string[]
}

export interface Research {
  id: string
  title: string
  members: string[]
  strand: string
  type: ResearchType
  fileId: string
  createdAt: number
  downloads: number
  unfinished: boolean
}

export interface SchoolDocument {
  id: string
  name: string
  description: string
  fileId: string
  createdAt: number
}

export interface Workspace {
  students: Student[]
  teachers: Teacher[]
  groups: ResearchGroup[]
  research: Research[]
  documents: SchoolDocument[]
}

export interface UploadInput {
  kind: 'research' | 'document'
  title: string
  description: string
  authors: string[]
  strand: string
  type: ResearchType
  file: File
}

export const statusLabels: Record<GroupStatus, string> = {
  preparing: 'Preparing research',
  pending: 'Pending review',
  changes: 'Changes requested',
  approved: 'Approved',
}

export const statusOrder: GroupStatus[] = ['preparing', 'changes', 'pending', 'approved']

export function groupStatus(group: ResearchGroup): GroupStatus {
  if (group.accepted) return 'approved'
  if (group.uploaded) return 'pending'
  if (group.comments.length > 0) return 'changes'
  return 'preparing'
}

export function studentGroup(student: Student, groups: ResearchGroup[]) {
  return groups.find(
    (group) => group.id === student.groupId || group.memberIds.includes(student.id),
  )
}

export function initials(name: string) {
  return (
    name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toUpperCase() || '—'
  )
}

export function displayDate(value: number) {
  if (!value) return 'Date not recorded'
  return new Intl.DateTimeFormat('en-PH', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    timeZone: 'Asia/Manila',
  }).format(value)
}

export function researchType(value: unknown): ResearchType | '' {
  const normalized = String(value ?? '').toLowerCase()
  return normalized === 'qualitative' || normalized === 'quantitative' ? normalized : ''
}

export function validateReview(
  group: ResearchGroup,
  decision: 'approve' | 'changes',
  feedback: string,
) {
  if (!group.uploaded || group.accepted)
    throw new Error('This submission has already changed. Review its current status first.')
  if (decision === 'changes' && !feedback.trim())
    throw new Error('Add feedback so the group knows what to change.')
  if (
    decision === 'approve' &&
    (!group.title.trim() || !group.type || !group.fileId || !group.memberIds.length)
  ) {
    throw new Error('The submission is missing its title, PDF, research type, or members.')
  }
}

export function reviewWorkspace(
  workspace: Workspace,
  id: string,
  decision: 'approve' | 'changes',
  feedback = '',
): Workspace {
  const group = workspace.groups.find((item) => item.id === id)
  if (!group) throw new Error('This group no longer exists. Refresh your workspace.')
  validateReview(group, decision, feedback)
  const members = group.memberIds.map((uid) =>
    workspace.students.find((student) => student.id === uid),
  )
  if (decision === 'approve' && members.some((member) => !member?.name.trim()))
    throw new Error('Some group members could not be verified. Check the student records first.')
  const next = structuredClone(workspace)
  const updated = next.groups.find((item) => item.id === id)!
  updated.uploaded = false
  updated.accepted = decision === 'approve'
  if (decision === 'changes') updated.comments.push(feedback.trim())
  else
    next.research.unshift({
      id: crypto.randomUUID(),
      title: group.title,
      strand: group.strand,
      type: group.type as ResearchType,
      members: members.map((member) => member!.name),
      fileId: group.fileId,
      createdAt: Date.now(),
      downloads: 0,
      unfinished: false,
    })
  return next
}

// Quote every cell and neutralize spreadsheet formulas, including after whitespace.
export function toCsv(rows: (string | number)[][]) {
  return (
    '\uFEFF' +
    rows
      .map((row) =>
        row
          .map((value) => {
            const text = String(value)
            const safe = /^\s*[=+@-]/.test(text) ? `'${text}` : text
            return `"${safe.replaceAll('"', '""')}"`
          })
          .join(','),
      )
      .join('\r\n')
  )
}
