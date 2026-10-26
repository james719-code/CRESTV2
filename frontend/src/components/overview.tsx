import {
  ArrowRight,
  BookOpenText,
  CheckCircle,
  Clock,
  FileText,
  PencilSimpleLine,
  UserPlus,
} from '@/icons'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { groupStatus, statusLabels, type GroupStatus, type Page, type Workspace } from '../domain'
import { GroupTable, SummaryStrip } from './workspace'
import type { FileRequest } from './workspace-dialogs'

export function Overview({
  workspace,
  onNavigate,
  onGroup,
  onFile,
}: {
  workspace: Workspace
  onNavigate: (page: Page) => void
  onGroup: (id: string) => void
  onFile: (file: FileRequest) => void
}) {
  const counts = Object.fromEntries(
    (['preparing', 'pending', 'changes', 'approved'] as GroupStatus[]).map((status) => [
      status,
      workspace.groups.filter((group) => groupStatus(group) === status).length,
    ]),
  ) as Record<GroupStatus, number>
  const total = workspace.groups.length
  const pendingAccounts =
    workspace.students.filter((student) => !student.accepted).length +
    workspace.teachers.filter((teacher) => !teacher.access).length
  const approvedAngle = total ? (counts.approved / total) * 360 : 0
  const pendingAngle = approvedAngle + (total ? (counts.pending / total) * 360 : 0)
  const changesAngle = pendingAngle + (total ? (counts.changes / total) * 360 : 0)
  const ring = total
    ? `conic-gradient(var(--crest-color-secondary) 0deg ${approvedAngle}deg, var(--primary) ${approvedAngle}deg ${pendingAngle}deg, #d29a35 ${pendingAngle}deg ${changesAngle}deg, var(--crest-color-outline-variant) ${changesAngle}deg 360deg)`
    : 'var(--border)'
  return (
    <>
      <SummaryStrip workspace={workspace} onSelect={onNavigate} />
      <div className="overview-middle">
        <Card className="progress-card">
          <CardContent>
            <div className="section-heading">
              <div>
                <span className="eyebrow">THE BIG PICTURE</span>
                <h2>Research progress</h2>
                <p>Here’s where your groups are in their research journey.</p>
              </div>
              <span className="decorative-book" aria-hidden="true">
                <BookOpenText size={30} weight="duotone" />
              </span>
            </div>
            <div className="progress-card-body">
              <div
                className="research-ring"
                style={{ background: ring }}
                role="img"
                aria-label={`${counts.approved} of ${total} research groups approved`}
              >
                <div>
                  <strong>
                    {counts.approved}
                    <span> / {total}</span>
                  </strong>
                  <small>groups approved</small>
                </div>
              </div>
              <div className="progress-legend">
                {(['pending', 'changes', 'preparing', 'approved'] as GroupStatus[]).map(
                  (status) => (
                    <button
                      key={status}
                      onClick={() => onNavigate(status === 'pending' ? 'reviews' : 'groups')}
                    >
                      <span className={`legend-dot legend-${status}`} />
                      <span>{statusLabels[status]}</span>
                      <strong>{counts[status].toString().padStart(2, '0')}</strong>
                      <ArrowRight size={14} />
                    </button>
                  ),
                )}
              </div>
            </div>
            <div className="progress-card-footer">
              <CheckCircle size={16} />
              {total
                ? `${Math.round((counts.approved / total) * 100)}% of groups have an approved manuscript.`
                : 'Group progress will appear when students start their research.'}
            </div>
          </CardContent>
        </Card>
        <Card className="attention-card">
          <CardContent>
            <div className="section-heading">
              <div>
                <span className="eyebrow">YOUR NEXT STEPS</span>
                <h2>Needs your attention</h2>
              </div>
            </div>
            <div className="attention-list">
              <button onClick={() => onNavigate('reviews')}>
                <span className="attention-icon blue">
                  <FileText size={22} />
                </span>
                <span>
                  <strong>
                    {counts.pending}{' '}
                    {counts.pending === 1 ? 'submission needs' : 'submissions need'} a review
                  </strong>
                  <small>Help your groups move forward</small>
                </span>
                <ArrowRight size={18} />
              </button>
              <button onClick={() => onNavigate('groups')}>
                <span className="attention-icon amber">
                  <PencilSimpleLine size={22} />
                </span>
                <span>
                  <strong>
                    {counts.changes} {counts.changes === 1 ? 'group is' : 'groups are'} making
                    changes
                  </strong>
                  <small>Follow up on your feedback</small>
                </span>
                <ArrowRight size={18} />
              </button>
              <button onClick={() => onNavigate('accounts')}>
                <span className="attention-icon teal">
                  <UserPlus size={22} />
                </span>
                <span>
                  <strong>
                    {pendingAccounts} {pendingAccounts === 1 ? 'account awaits' : 'accounts await'}{' '}
                    approval
                  </strong>
                  <small>Welcome the next researchers</small>
                </span>
                <ArrowRight size={18} />
              </button>
            </div>
            <p className="attention-note">
              <Clock size={15} />A clear next step makes a difference.
            </p>
          </CardContent>
        </Card>
      </div>
      <GroupTable workspace={workspace} onOpen={onGroup} compact />
      <div className="overview-bottom">
        <Card className="recent-card">
          <CardContent>
            <div className="section-heading">
              <div>
                <h2>Recently added research</h2>
                <p>Student work, preserved and shared.</p>
              </div>
              <Button variant="ghost" size="sm" onClick={() => onNavigate('research')}>
                View library
                <ArrowRight size={15} />
              </Button>
            </div>
            {workspace.research.slice(0, 2).map((paper) => (
              <button
                key={`${paper.type}-${paper.id}`}
                className="recent-paper"
                onClick={() => onFile({ id: paper.fileId, title: paper.title })}
              >
                <span className="paper-icon">
                  <FileText size={23} />
                </span>
                <span>
                  <strong>{paper.title}</strong>
                  <small>
                    {paper.strand} <span>·</span> <span className="capitalize">{paper.type}</span>
                  </small>
                </span>
                <ArrowRight size={17} />
              </button>
            ))}
            {!workspace.research.length && (
              <p className="muted py-6">Approved papers will appear in the research library.</p>
            )}
          </CardContent>
        </Card>
        <div className="library-note">
          <span className="library-note-icon">
            <BookOpenText size={34} weight="duotone" />
          </span>
          <div>
            <span className="eyebrow">SCHOOL RESEARCH, SHARED.</span>
            <h2>
              Today’s work.
              <br />
              Tomorrow’s starting point.
            </h2>
            <p>Every paper you review helps the next group discover something new.</p>
            <button onClick={() => onNavigate('documents')}>
              Explore school resources <ArrowRight size={16} />
            </button>
          </div>
        </div>
      </div>
    </>
  )
}
