import type { ResearchGroup, Workspace } from './domain.ts'

// Build joins once when records change, rather than searching every group per row/keystroke.
export function indexWorkspace(workspace: Workspace) {
  const studentsById = new Map(workspace.students.map((student) => [student.id, student]))
  const groupsById = new Map<string, { group: ResearchGroup; order: number }>()
  const memberships = new Map<string, { group: ResearchGroup; order: number }>()
  const groupSearch = new Map<string, string>()
  workspace.groups.forEach((group, order) => {
    if (!groupsById.has(group.id)) groupsById.set(group.id, { group, order })
    for (const id of group.memberIds) {
      if (!memberships.has(id)) memberships.set(id, { group, order })
    }
    groupSearch.set(
      group.id,
      `${group.name} ${group.title} ${group.memberIds.map((id) => studentsById.get(id)?.name || '').join(' ')}`.toLowerCase(),
    )
  })
  const studentGroups = new Map<string, ResearchGroup>()
  for (const student of workspace.students) {
    const direct = groupsById.get(student.groupId)
    const member = memberships.get(student.id)
    // Preserve the app's first-match rule when legacy IDs and membership disagree.
    const match =
      direct && member ? (direct.order < member.order ? direct : member) : direct || member
    if (match) studentGroups.set(student.id, match.group)
  }
  return { studentsById, studentGroups, groupSearch }
}

export function pageWindow<T>(items: T[], requested: number, size = 12) {
  const pages = Math.max(1, Math.ceil(items.length / size))
  const page = Math.max(1, Math.min(requested, pages))
  const start = (page - 1) * size
  return { page, pages, start, rows: items.slice(start, start + size) }
}
