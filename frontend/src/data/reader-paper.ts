import { groupStatus, statusLabels, type Workspace } from '../domain.ts'
import { groupRoute, type ReaderTarget, type Route } from '../routing.ts'

export interface ReaderPaper {
  fileId: string
  title: string
  kind: 'research' | 'document' | 'submission'
  authors: string[]
  strand: string
  researchType: string
  description: string
  createdAt: number
  status: string
  groupName?: string
}

export function readerBackRoute(target: ReaderTarget): Route {
  return target.source === 'groups'
    ? groupRoute(target.id)
    : target.source === 'documents'
      ? 'documents'
      : 'research'
}

export function resolveReaderPaper(workspace: Workspace, target: ReaderTarget): ReaderPaper | null {
  if (target.source === 'documents') {
    const document = workspace.documents.find((record) => record.id === target.id)
    return document
      ? {
          fileId: document.fileId,
          title: document.name,
          kind: 'document',
          authors: [],
          strand: '',
          researchType: '',
          description: document.description,
          createdAt: document.createdAt,
          status: 'School resource',
        }
      : null
  }
  if (target.source === 'groups') {
    const group = workspace.groups.find((record) => record.id === target.id)
    if (!group) return null
    const students = new Map(workspace.students.map((student) => [student.id, student.name]))
    return {
      fileId: group.fileId,
      title: group.title || group.name,
      kind: 'submission',
      authors: group.memberIds.map((id) => students.get(id) || 'Member details unavailable'),
      strand: group.strand,
      researchType: group.type,
      description: '',
      createdAt: 0,
      status: statusLabels[groupStatus(group)],
      groupName: group.name,
    }
  }
  const paper = workspace.research.find(
    (record) => record.id === target.id && record.type === target.source,
  )
  return paper
    ? {
        fileId: paper.fileId,
        title: paper.title,
        kind: 'research',
        authors: paper.members,
        strand: paper.strand,
        researchType: paper.type,
        description: '',
        createdAt: paper.createdAt,
        status: 'Published in the school library',
      }
    : null
}
