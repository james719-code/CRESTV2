import type { Milestone, MilestoneInput, MilestoneStage } from '../domain.ts'

export const milestoneStages: Record<MilestoneStage, string> = {
  proposal: 'Proposal',
  chapter: 'Chapter',
  defense: 'Defense',
  final: 'Final manuscript',
}

export function manilaDate(now = Date.now()): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Manila',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now)
  const part = (type: string) => parts.find((value) => value.type === type)!.value
  return `${part('year')}-${part('month')}-${part('day')}`
}

export function validDueDate(date: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || date < '2000-01-01' || date > '2100-12-31') return false
  const time = Date.parse(`${date}T00:00:00Z`)
  return Number.isFinite(time) && new Date(time).toISOString().slice(0, 10) === date
}

export function validateMilestone(input: MilestoneInput): MilestoneInput {
  const title = input.title.trim()
  if (!input.groupId) throw new Error('Choose a research group.')
  if (!title || title.length > 200)
    throw new Error('Enter a milestone title of up to 200 characters.')
  if (!Object.hasOwn(milestoneStages, input.stage)) throw new Error('Choose a milestone stage.')
  if (!validDueDate(input.dueDate))
    throw new Error('Choose a valid deadline between 2000 and 2100.')
  return { ...input, title }
}

export function milestoneStatus(milestone: Milestone, today = manilaDate()) {
  if (milestone.completedAt > 0) return 'completed'
  if (milestone.dueDate < today) return 'overdue'
  if (milestone.dueDate === today) return 'today'
  return 'upcoming'
}

export function milestoneDate(date: string) {
  return new Intl.DateTimeFormat('en-PH', {
    timeZone: 'Asia/Manila',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(Date.parse(`${date}T00:00:00+08:00`))
}

export function checkMilestoneVersion(current: Milestone | undefined, expected: number | null) {
  if (expected === null ? !!current : !current || current.version !== expected)
    throw new Error('This milestone has changed. Check the latest details and try again.')
}
