import { useState } from 'react'
import { toast } from 'sonner'
import { ArrowRight, Bell, Check } from '@/icons'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { SelectItem } from '@/components/ui/select'
import { EmptyState, LabeledFilter } from './workspace'
import { ListPagination } from './list-pagination'
import { pageWindow } from '../workspace-index'
import type { InboxNotification, Workspace } from '../domain'
import type { Route } from '../routing'
import { groupRoute } from '../routing'
import './planning.css'

const kinds = {
  submission: 'Submissions',
  approval: 'Approvals',
  revision: 'Revision updates',
  account: 'Account registrations',
}

export function NotificationInbox({
  workspace,
  onRead,
  navigate,
  error,
  onRetry,
}: {
  workspace: Workspace
  onRead: (ids: string[], read: boolean) => Promise<void>
  navigate: (route: Route) => void
  error: string
  onRetry: () => void
}) {
  const [filter, setFilter] = useState('all')
  const [kind, setKind] = useState('all')
  const [page, setPage] = useState(1)
  const [busy, setBusy] = useState(false)
  const items = workspace.notifications || []
  const unread = items.filter((item) => !item.read)
  const filtered = items
    .filter((item) => (filter === 'all' || !item.read) && (kind === 'all' || item.kind === kind))
    .slice()
    .sort((a, b) => b.observedAt - a.observedAt || a.id.localeCompare(b.id))
  const pagination = pageWindow(filtered, page)
  async function mark(ids: string[], read: boolean) {
    setBusy(true)
    try {
      await onRead(ids, read)
      return true
    } catch (cause) {
      toast.error(cause instanceof Error ? cause.message : 'The inbox could not be updated.')
      return false
    } finally {
      setBusy(false)
    }
  }
  async function open(item: InboxNotification) {
    if (!item.read && !(await mark([item.id], true))) return
    navigate(item.groupId ? groupRoute(item.groupId) : 'accounts')
  }
  return (
    <Card className="table-card inbox-panel">
      <div className="planning-heading">
        <div>
          <h2>Your notification inbox</h2>
          <p>
            {unread.length} unread · Updates are saved for your teacher account when the workspace
            connects.
          </p>
        </div>
        <Button
          variant="outline"
          disabled={busy || !unread.length}
          onClick={() =>
            void mark(
              unread.map((item) => item.id),
              true,
            )
          }
        >
          <Check size={17} />
          Mark all as read
        </Button>
      </div>
      {error && (
        <div className="notice-box" role="alert">
          <p>{error}</p>
          <Button variant="outline" onClick={onRetry}>
            Retry inbox sync
          </Button>
        </div>
      )}
      <div className="table-toolbar">
        <LabeledFilter
          label="Read status"
          ariaLabel="Filter notifications by read status"
          value={filter}
          onChange={(value) => {
            setFilter(value)
            setPage(1)
          }}
        >
          <SelectItem value="all">All notifications</SelectItem>
          <SelectItem value="unread">Unread only</SelectItem>
        </LabeledFilter>
        <LabeledFilter
          label="Update type"
          ariaLabel="Filter notifications by update type"
          value={kind}
          onChange={(value) => {
            setKind(value)
            setPage(1)
          }}
        >
          <SelectItem value="all">All updates</SelectItem>
          {Object.entries(kinds).map(([value, label]) => (
            <SelectItem key={value} value={value}>
              {label}
            </SelectItem>
          ))}
        </LabeledFilter>
      </div>
      {pagination.rows.length ? (
        <ul className="inbox-list">
          {pagination.rows.map((item) => {
            const available =
              !item.groupId || workspace.groups.some((group) => group.id === item.groupId)
            return (
              <li key={item.id} className={item.read ? '' : 'is-unread'}>
                <span className="inbox-icon" aria-hidden="true">
                  <Bell size={20} />
                </span>
                <div className="inbox-content">
                  <span className="inbox-kind">
                    {kinds[item.kind]}
                    {!item.read && <strong>Unread</strong>}
                  </span>
                  <h3>{item.title}</h3>
                  <p>{item.message}</p>
                  <small>
                    Added to inbox{' '}
                    {item.observedAt
                      ? new Intl.DateTimeFormat('en-PH', {
                          timeZone: 'Asia/Manila',
                          dateStyle: 'medium',
                          timeStyle: 'short',
                        }).format(item.observedAt)
                      : 'just now'}{' '}
                    · Manila time
                  </small>
                  {!available && <p className="muted">This group is no longer available.</p>}
                </div>
                <div className="inbox-actions">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={busy || !available}
                    onClick={() => void open(item)}
                  >
                    View {item.groupId ? 'group' : 'accounts'}
                    <ArrowRight size={15} />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={busy}
                    aria-label={`Mark ${item.title} as ${item.read ? 'unread' : 'read'}`}
                    onClick={() => void mark([item.id], !item.read)}
                  >
                    Mark {item.read ? 'unread' : 'read'}
                  </Button>
                </div>
              </li>
            )
          })}
        </ul>
      ) : (
        <EmptyState
          title={filter === 'unread' ? 'You’re all caught up' : 'No updates to show'}
          description="Submissions, research approvals, revision feedback, and pending registrations appear here as school records sync."
        />
      )}
      <ListPagination
        {...pagination}
        count={pagination.rows.length}
        total={filtered.length}
        noun="notifications"
        onPage={setPage}
      />
      <p className="inbox-note">
        Read status is private to your account. Inbox updates work without browser push
        notifications. Older records show when they were added here, since their original event time
        was not recorded.
      </p>
    </Card>
  )
}
