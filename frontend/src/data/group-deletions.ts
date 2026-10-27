import type { ResearchGroup, Workspace } from '../domain.ts'

export interface GroupDeletionOperation {
  group: ResearchGroup
  deleting: boolean
  error: string | null
}

export interface GroupDeletionSnapshot {
  operations: Record<string, GroupDeletionOperation>
  deletedIds: string[]
}

/** Lives above routed pages so leaving group details cannot lose progress or errors. */
export class GroupDeletionTracker {
  private snapshot: GroupDeletionSnapshot = { operations: {}, deletedIds: [] }
  private listeners = new Set<() => void>()
  private active = new Map<string, Promise<void>>()
  private generation = 0

  getSnapshot = () => this.snapshot
  subscribe = (listener: () => void) => {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }
  private publish(snapshot: GroupDeletionSnapshot) {
    this.snapshot = snapshot
    for (const listener of this.listeners) listener()
  }
  reset() {
    this.generation++
    this.active.clear()
    this.publish({ operations: {}, deletedIds: [] })
  }
  delete(group: ResearchGroup, action: () => Promise<void>): Promise<void> {
    const existing = this.active.get(group.id)
    if (existing) return existing
    if (this.snapshot.deletedIds.includes(group.id)) return Promise.resolve()
    const generation = this.generation
    this.publish({
      ...this.snapshot,
      operations: {
        ...this.snapshot.operations,
        [group.id]: { group, deleting: true, error: null },
      },
    })
    const job = Promise.resolve()
      .then(action)
      .then(() => {
        if (generation !== this.generation) return
        const operations = { ...this.snapshot.operations }
        delete operations[group.id]
        this.publish({ operations, deletedIds: [...this.snapshot.deletedIds, group.id] })
      })
      .catch((cause: unknown) => {
        if (generation === this.generation)
          this.publish({
            ...this.snapshot,
            operations: {
              ...this.snapshot.operations,
              [group.id]: {
                group,
                deleting: false,
                error:
                  cause instanceof Error
                    ? cause.message
                    : 'The group could not be deleted. Try again.',
              },
            },
          })
        throw cause
      })
      .finally(() => {
        if (generation === this.generation) this.active.delete(group.id)
      })
    this.active.set(group.id, job)
    return job
  }
}

export function withGroupDeletions(
  workspace: Workspace,
  snapshot: GroupDeletionSnapshot,
): Workspace {
  if (!Object.keys(snapshot.operations).length && !snapshot.deletedIds.length) return workspace
  const groups = new Map(workspace.groups.map((group) => [group.id, group]))
  for (const operation of Object.values(snapshot.operations)) {
    if (!groups.has(operation.group.id)) groups.set(operation.group.id, operation.group)
  }
  for (const id of snapshot.deletedIds) groups.delete(id)
  return { ...workspace, groups: [...groups.values()] }
}
