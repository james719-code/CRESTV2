import { useEffect, useMemo, useState } from 'react'
import { toast } from 'sonner'
import { ArrowRight, CheckCircle, Clock, PencilSimpleLine, Plus } from '@/icons'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { EmptyState, LabeledFilter } from './workspace'
import { ListPagination } from './list-pagination'
import { pageWindow } from '../workspace-index'
import { useReturnFocus } from './use-return-focus'
import type { Milestone, MilestoneInput, MilestoneStage, Workspace } from '../domain'
import { manilaDate, milestoneDate, milestoneStages, milestoneStatus } from '../data/milestones'
import './planning.css'

export interface MilestoneActions {
  onSave: (input: MilestoneInput, existing?: Milestone) => Promise<void>
  onChange: (milestone: Milestone, action: 'complete' | 'reopen' | 'delete') => Promise<void>
}

function useToday() {
  const [today, setToday] = useState(manilaDate)
  useEffect(() => {
    const update = () => setToday(manilaDate())
    const timer = window.setInterval(update, 30_000)
    window.addEventListener('focus', update)
    return () => {
      clearInterval(timer)
      window.removeEventListener('focus', update)
    }
  }, [])
  return today
}

const statusLabels = {
  overdue: 'Overdue',
  today: 'Due today',
  upcoming: 'Upcoming',
  completed: 'Completed',
}

export function MilestoneSummary({
  workspace,
  onOpen,
  onGroup,
}: {
  workspace: Workspace
  onOpen: () => void
  onGroup: (id: string) => void
}) {
  const today = useToday()
  const items = (workspace.milestones || [])
    .filter(
      (item) => !item.completedAt && workspace.groups.some((group) => group.id === item.groupId),
    )
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
  const overdue = items.filter((item) => milestoneStatus(item, today) === 'overdue').length
  return (
    <Card className="planning-summary">
      <div className="planning-heading">
        <div>
          <h2>Research deadlines</h2>
          <p>
            {overdue ? `${overdue} overdue · ` : ''}
            {items.length} open milestones
          </p>
        </div>
        <Button variant="outline" onClick={onOpen}>
          View milestones <ArrowRight size={16} />
        </Button>
      </div>
      {items.length ? (
        <ul className="deadline-preview">
          {items.slice(0, 3).map((item) => (
            <li key={item.id}>
              <button onClick={() => onGroup(item.groupId)}>
                <span>
                  <strong>{item.title}</strong>
                  <small>{workspace.groups.find((group) => group.id === item.groupId)?.name}</small>
                </span>
                <span className={`deadline-label ${milestoneStatus(item, today)}`}>
                  {milestoneStatus(item, today) === 'overdue' ? 'Overdue · ' : ''}
                  {milestoneDate(item.dueDate)}
                </span>
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <p className="muted">
          Add proposal, chapter, defense, or final manuscript deadlines to plan the next steps.
        </p>
      )}
    </Card>
  )
}

export function Milestones({
  workspace,
  groupId,
  onSave,
  onChange,
}: MilestoneActions & { workspace: Workspace; groupId?: string }) {
  const today = useToday()
  const [group, setGroup] = useState('all')
  const [status, setStatus] = useState('all')
  const [stage, setStage] = useState('all')
  const [page, setPage] = useState(1)
  const [editor, setEditor] = useState<{ existing?: Milestone } | null>(null)
  const [deleting, setDeleting] = useState<Milestone | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const groups = useMemo(
    () => new Map(workspace.groups.map((item) => [item.id, item])),
    [workspace.groups],
  )
  const all = (workspace.milestones || []).filter(
    (item) => groups.has(item.groupId) && (!groupId || item.groupId === groupId),
  )
  const filtered = all
    .filter(
      (item) =>
        (groupId || group === 'all' || item.groupId === group) &&
        (status === 'all' || milestoneStatus(item, today) === status) &&
        (stage === 'all' || item.stage === stage),
    )
    .sort(
      (a, b) =>
        Number(!!a.completedAt) - Number(!!b.completedAt) ||
        a.dueDate.localeCompare(b.dueDate) ||
        a.id.localeCompare(b.id),
    )
  const pagination = pageWindow(filtered, page)
  async function change(item: Milestone, action: 'complete' | 'reopen' | 'delete') {
    setBusy(true)
    setError('')
    try {
      await onChange(item, action)
      setDeleting(null)
      toast.success(
        action === 'delete'
          ? 'Milestone deleted.'
          : action === 'complete'
            ? 'Milestone completed.'
            : 'Milestone reopened.',
      )
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'The milestone could not be updated.')
    } finally {
      setBusy(false)
    }
  }
  return (
    <Card className="table-card milestone-panel">
      <div className="planning-heading">
        <div>
          <h2>Deadlines & milestones</h2>
          <p>
            Deadlines run through the end of the selected day in Manila time. Completion is recorded
            by a teacher.
          </p>
        </div>
        <Button disabled={!workspace.groups.length} onClick={() => setEditor({})}>
          <Plus size={17} /> Add milestone
        </Button>
      </div>
      <div className="milestone-totals" aria-label="Milestone counts">
        {Object.entries(statusLabels).map(([key, label]) => (
          <button
            key={key}
            aria-pressed={status === key}
            className={`deadline-label ${key}`}
            onClick={() => {
              setStatus(status === key ? 'all' : key)
              setPage(1)
            }}
          >
            <strong>{all.filter((item) => milestoneStatus(item, today) === key).length}</strong>{' '}
            {label}
          </button>
        ))}
      </div>
      <div className="table-toolbar">
        {!groupId && (
          <LabeledFilter
            label="Research group"
            ariaLabel="Filter milestones by group"
            value={group}
            onChange={(value) => {
              setGroup(value)
              setPage(1)
            }}
          >
            <SelectItem value="all">All groups</SelectItem>
            {workspace.groups.map((item) => (
              <SelectItem key={item.id} value={item.id}>
                {item.name}
              </SelectItem>
            ))}
          </LabeledFilter>
        )}
        <LabeledFilter
          label="Stage"
          ariaLabel="Filter milestones by stage"
          value={stage}
          onChange={(value) => {
            setStage(value)
            setPage(1)
          }}
        >
          <SelectItem value="all">All stages</SelectItem>
          {Object.entries(milestoneStages).map(([value, label]) => (
            <SelectItem key={value} value={value}>
              {label}
            </SelectItem>
          ))}
        </LabeledFilter>
        <LabeledFilter
          label="Status"
          ariaLabel="Filter milestones by status"
          value={status}
          onChange={(value) => {
            setStatus(value)
            setPage(1)
          }}
        >
          <SelectItem value="all">All statuses</SelectItem>
          {Object.entries(statusLabels).map(([value, label]) => (
            <SelectItem key={value} value={value}>
              {label}
            </SelectItem>
          ))}
        </LabeledFilter>
      </div>
      {error && !deleting && (
        <p className="notice-box" role="alert">
          {error}
        </p>
      )}
      {pagination.rows.length ? (
        <ul className="milestone-list">
          {pagination.rows.map((item) => (
            <li key={item.id}>
              <div className="milestone-main">
                <Badge variant="outline">{milestoneStages[item.stage]}</Badge>
                <h3>{item.title}</h3>
                {!groupId && <p>{groups.get(item.groupId)?.name}</p>}
                <span className={`deadline-label ${milestoneStatus(item, today)}`}>
                  <Clock size={15} />
                  {statusLabels[milestoneStatus(item, today)]} · {milestoneDate(item.dueDate)}
                </span>
              </div>
              <div className="milestone-actions">
                <Button
                  size="sm"
                  variant="outline"
                  disabled={busy}
                  aria-label={`${item.completedAt ? 'Reopen' : 'Complete'} ${item.title}`}
                  onClick={() => void change(item, item.completedAt ? 'reopen' : 'complete')}
                >
                  <CheckCircle size={16} />
                  {item.completedAt ? 'Reopen' : 'Mark complete'}
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  disabled={busy}
                  aria-label={`Edit ${item.title}`}
                  onClick={() => setEditor({ existing: item })}
                >
                  <PencilSimpleLine size={16} />
                  Edit
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  disabled={busy}
                  aria-label={`Delete ${item.title}`}
                  onClick={() => {
                    setError('')
                    setDeleting(item)
                  }}
                >
                  Delete
                </Button>
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <EmptyState
          title={all.length ? 'No matching milestones' : 'Plan the next research milestone'}
          description={
            all.length
              ? 'Choose another group, stage, or status.'
              : 'Add a deadline for a proposal, chapter, defense, or final manuscript.'
          }
        />
      )}
      <ListPagination
        {...pagination}
        count={pagination.rows.length}
        total={filtered.length}
        noun="milestones"
        onPage={setPage}
      />
      {editor && (
        <MilestoneEditor
          workspace={workspace}
          groupId={groupId}
          existing={editor.existing}
          onSave={onSave}
          onClose={() => setEditor(null)}
        />
      )}
      <Dialog
        open={!!deleting}
        onOpenChange={(open) => {
          if (!open && !busy) setDeleting(null)
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete this milestone?</DialogTitle>
            <DialogDescription>
              {deleting?.title} will be removed from the group’s plan.
            </DialogDescription>
          </DialogHeader>
          {error && <p role="alert">{error}</p>}
          <div className="planning-dialog-actions">
            <Button variant="outline" disabled={busy} onClick={() => setDeleting(null)}>
              Cancel
            </Button>
            <Button disabled={busy} onClick={() => deleting && void change(deleting, 'delete')}>
              {busy ? 'Deleting…' : 'Delete milestone'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </Card>
  )
}

function MilestoneEditor({
  workspace,
  groupId,
  existing,
  onSave,
  onClose,
}: Pick<MilestoneActions, 'onSave'> & {
  workspace: Workspace
  groupId?: string
  existing?: Milestone
  onClose: () => void
}) {
  const returnFocus = useReturnFocus()
  const [input, setInput] = useState<MilestoneInput>({
    groupId: existing?.groupId || groupId || '',
    title: existing?.title || '',
    stage: existing?.stage || 'proposal',
    dueDate: existing?.dueDate || manilaDate(),
  })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open && !busy) onClose()
      }}
    >
      <DialogContent className="milestone-editor" onCloseAutoFocus={returnFocus}>
        <DialogHeader>
          <DialogTitle>{existing ? 'Edit milestone' : 'Add a research milestone'}</DialogTitle>
          <DialogDescription>
            Choose a group, a research stage, and the day it is due. Dates use Manila time.
          </DialogDescription>
        </DialogHeader>
        <form
          className="milestone-form"
          onSubmit={async (event) => {
            event.preventDefault()
            setBusy(true)
            setError('')
            try {
              await onSave(input, existing)
              toast.success('Milestone saved.')
              onClose()
            } catch (cause) {
              setError(cause instanceof Error ? cause.message : 'The milestone could not be saved.')
            } finally {
              setBusy(false)
            }
          }}
        >
          <div>
            <Label htmlFor="milestone-group">Research group</Label>
            <Select
              value={input.groupId}
              disabled={busy || !!existing || !!groupId}
              onValueChange={(value) => setInput({ ...input, groupId: value })}
            >
              <SelectTrigger id="milestone-group">
                <SelectValue placeholder="Choose a group" />
              </SelectTrigger>
              <SelectContent>
                {workspace.groups.map((item) => (
                  <SelectItem key={item.id} value={item.id}>
                    {item.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label htmlFor="milestone-title">Milestone title</Label>
            <Input
              id="milestone-title"
              required
              maxLength={200}
              disabled={busy}
              value={input.title}
              placeholder="e.g. Complete Chapter 1"
              onChange={(event) => setInput({ ...input, title: event.target.value })}
            />
          </div>
          <div>
            <Label htmlFor="milestone-stage">Research stage</Label>
            <Select
              value={input.stage}
              disabled={busy}
              onValueChange={(value) => setInput({ ...input, stage: value as MilestoneStage })}
            >
              <SelectTrigger id="milestone-stage">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {Object.entries(milestoneStages).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label htmlFor="milestone-date">Deadline (Manila time)</Label>
            <Input
              id="milestone-date"
              type="date"
              min="2000-01-01"
              max="2100-12-31"
              required
              disabled={busy}
              value={input.dueDate}
              onChange={(event) => setInput({ ...input, dueDate: event.target.value })}
            />
          </div>
          {error && (
            <p className="notice-box" role="alert">
              {error}
            </p>
          )}
          <div className="planning-dialog-actions">
            <Button type="button" variant="outline" disabled={busy} onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={busy}>
              {busy ? 'Saving…' : 'Save milestone'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
