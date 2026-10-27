import { researchType, type ResearchGroup, type Workspace } from '../domain.ts'

export type WorkspaceSource =
  'students' | 'teachers' | 'groups' | 'qualitative' | 'quantitative' | 'documents'
export interface WorkspaceRecord {
  id: string
  data: Record<string, unknown>
}

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

export function mapGroup(id: string, data: Record<string, unknown>): ResearchGroup {
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

// Each listener replaces only its own array. Other views keep their cached joins
// and search results when an unrelated collection changes.
export class WorkspaceSnapshot {
  private received = new Set<WorkspaceSource>()
  private research: Record<'qualitative' | 'quantitative', Workspace['research']> = {
    qualitative: [],
    quantitative: [],
  }
  private workspace: Workspace = {
    students: [],
    teachers: [],
    groups: [],
    research: [],
    documents: [],
  }

  update(source: WorkspaceSource, rows: WorkspaceRecord[]): Workspace | null {
    switch (source) {
      case 'students':
        this.workspace = {
          ...this.workspace,
          students: rows.map(({ id, data }) => ({
            id,
            name: string(data.name),
            strand: string(data.strand),
            groupId: string(data.groupId) || string(data.group_id),
            accepted: data.accepted === true,
            declined: data.declined === true,
          })),
        }
        break
      case 'teachers':
        this.workspace = {
          ...this.workspace,
          teachers: rows.map(({ id, data }) => ({
            id,
            name: string(data.name),
            email: string(data.email),
            access: data.access === true,
          })),
        }
        break
      case 'groups':
        this.workspace = {
          ...this.workspace,
          groups: rows.map(({ id, data }) => mapGroup(id, data)),
        }
        break
      case 'qualitative':
      case 'quantitative':
        this.research[source] = rows.map(({ id, data }) => ({
          id,
          type: source,
          title: string(data.title),
          strand: string(data.strand),
          members: strings(data.members),
          fileId: string(data.file_link),
          createdAt: timestamp(data.createdAt),
          downloads: typeof data.downloads === 'number' ? Math.max(0, data.downloads) : 0,
          unfinished: data.unfinished === true,
        }))
        this.workspace = {
          ...this.workspace,
          research: [...this.research.qualitative, ...this.research.quantitative].sort(
            (a, b) => b.createdAt - a.createdAt,
          ),
        }
        break
      case 'documents':
        this.workspace = {
          ...this.workspace,
          documents: rows
            .map(({ id, data }) => ({
              id,
              name: string(data.name),
              description: string(data.description),
              fileId: string(data.file_link),
              createdAt: timestamp(data.createdAt),
            }))
            .sort((a, b) => b.createdAt - a.createdAt),
        }
        break
    }
    this.received.add(source)
    return this.received.size === 6 ? this.workspace : null
  }
}
