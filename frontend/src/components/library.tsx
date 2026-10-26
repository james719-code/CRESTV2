import { useDeferredValue, useMemo, useState } from 'react'
import { ArrowRight, Check, FilePdf, UserPlus } from '@/icons'
import { toast } from 'sonner'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { SelectItem } from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { displayDate, initials, studentAccountStatus, type Workspace } from '../domain'
import { StudentAccountActions, type ReviewStudent } from './student-account-actions'
import { EmptyState, LabeledFilter, SearchInput, StrandBadge } from './workspace'
import type { FileRequest } from './workspace-dialogs'
import { ResearchArt } from './research-art'
import { ListPagination } from './list-pagination'
import { pageWindow } from '../workspace-index'
import { useCardLayout } from './use-card-layout'

export function ResearchLibrary({
  workspace,
  onFile,
}: {
  workspace: Workspace
  onFile: (file: FileRequest) => void
}) {
  const [query, setQuery] = useState('')
  const [type, setType] = useState('all')
  const [page, setPage] = useState(1)
  const search = useDeferredValue(query.trim().toLowerCase())
  const papers = useMemo(
    () =>
      workspace.research.filter(
        (paper) =>
          `${paper.title} ${paper.members.join(' ')} ${paper.strand}`
            .toLowerCase()
            .includes(search) &&
          (type === 'all' || paper.type === type),
      ),
    [workspace.research, search, type],
  )
  const pagination = pageWindow(papers, page)
  return (
    <Card className="table-card">
      <div className="table-card-heading">
        <div>
          <h2>Research papers</h2>
          <p>Approved student work and research shared by teachers.</p>
        </div>
        <span className="count-label">{workspace.research.length} papers</span>
      </div>
      <div className="table-toolbar">
        <SearchInput
          value={query}
          onChange={(value) => {
            setQuery(value)
            setPage(1)
          }}
          placeholder="Search papers, authors, or strands…"
        />
        <LabeledFilter
          label="Research type"
          ariaLabel="Filter by research type"
          value={type}
          onChange={(value) => {
            setType(value)
            setPage(1)
          }}
        >
          <SelectItem value="all">All research types</SelectItem>
          <SelectItem value="qualitative">Qualitative</SelectItem>
          <SelectItem value="quantitative">Quantitative</SelectItem>
        </LabeledFilter>
      </div>
      {papers.length ? (
        <div className="paper-grid">
          {pagination.rows.map((paper) => (
            <article className="research-card" key={`${paper.type}-${paper.id}`}>
              <div className={`research-cover tone-${paper.strand.toLowerCase()}`}>
                <div>
                  <span className="card-eyebrow">The CREST collection</span>
                  <strong>
                    Ideas worth
                    <br />
                    sharing.
                  </strong>
                  <span className="cover-format">Research paper · PDF</span>
                </div>
                <ResearchArt />
                <StrandBadge strand={paper.strand} />
              </div>
              <Badge variant="outline" className="research-type">
                {paper.type}
              </Badge>
              <h3>{paper.title}</h3>
              <p>{paper.members.join(', ') || 'Authors not recorded'}</p>
              <div className="research-card-bottom">
                <span>{displayDate(paper.createdAt)}</span>
                <Button
                  size="sm"
                  variant="ghost"
                  disabled={!paper.fileId}
                  onClick={() => onFile({ id: paper.fileId, title: paper.title })}
                >
                  Read paper
                  <ArrowRight size={15} />
                </Button>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <EmptyState
          title={
            workspace.research.length
              ? 'No papers match your search'
              : 'A library starts with one paper'
          }
          description={
            workspace.research.length
              ? 'Try another title or clear the research type filter.'
              : 'Approve a group submission or add a research paper to start your school collection.'
          }
          action={
            workspace.research.length ? (
              <Button
                variant="outline"
                onClick={() => {
                  setQuery('')
                  setType('all')
                }}
              >
                Clear filters
              </Button>
            ) : undefined
          }
        />
      )}
      <ListPagination
        {...pagination}
        count={pagination.rows.length}
        total={papers.length}
        noun="papers"
        onPage={setPage}
      />
    </Card>
  )
}

export function DocumentLibrary({
  workspace,
  onFile,
}: {
  workspace: Workspace
  onFile: (file: FileRequest) => void
}) {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(1)
  const search = useDeferredValue(query.trim().toLowerCase())
  const documents = useMemo(
    () =>
      workspace.documents.filter((item) =>
        `${item.name} ${item.description}`.toLowerCase().includes(search),
      ),
    [workspace.documents, search],
  )
  const pagination = pageWindow(documents, page)
  return (
    <Card className="table-card">
      <div className="table-card-heading">
        <div>
          <h2>Resources for better research</h2>
          <p>Templates, rubrics, and guides for your school community.</p>
        </div>
        <span className="count-label">{workspace.documents.length} documents</span>
      </div>
      <div className="table-toolbar">
        <SearchInput
          value={query}
          onChange={(value) => {
            setQuery(value)
            setPage(1)
          }}
          placeholder="Search school documents…"
        />
      </div>
      {documents.length ? (
        <div className="document-list">
          {pagination.rows.map((document) => (
            <article key={document.id}>
              <span className="document-file-icon document-cover" aria-hidden="true">
                <FilePdf size={29} />
                <span />
                <span />
                <span />
              </span>
              <div>
                <h3>{document.name}</h3>
                <p>{document.description || 'School research resource'}</p>
                <span>
                  PDF document <span>·</span> {displayDate(document.createdAt)}
                </span>
              </div>
              <Button
                variant="outline"
                disabled={!document.fileId}
                onClick={() => onFile({ id: document.fileId, title: document.name })}
              >
                Open document
                <ArrowRight size={16} />
              </Button>
            </article>
          ))}
        </div>
      ) : (
        <EmptyState
          title={
            workspace.documents.length ? 'No matching documents' : 'Give your students a head start'
          }
          description={
            workspace.documents.length
              ? 'Try a different title or description.'
              : 'Share a manuscript template, evaluation rubric, or research guide.'
          }
        />
      )}
      <ListPagination
        {...pagination}
        count={pagination.rows.length}
        total={documents.length}
        noun="documents"
        onPage={setPage}
      />
    </Card>
  )
}

export function Accounts({
  workspace,
  onApprove,
  onReviewStudent,
}: {
  workspace: Workspace
  onApprove: (id: string, role: 'student' | 'teacher') => Promise<void>
  onReviewStudent: ReviewStudent
}) {
  const [busy, setBusy] = useState<string | null>(null)
  const [page, setPage] = useState(1)
  const { ref, cards } = useCardLayout()
  const pending = useMemo(
    () => [
      ...workspace.students
        .filter((student) => studentAccountStatus(student) === 'pending')
        .map((student) => ({ ...student, role: 'student' as const, detail: student.strand })),
      ...workspace.teachers
        .filter((teacher) => !teacher.access)
        .map((teacher) => ({ ...teacher, role: 'teacher' as const, detail: teacher.email })),
    ],
    [workspace.students, workspace.teachers],
  )
  const pagination = pageWindow(pending, page)
  async function approve(id: string, role: 'student' | 'teacher') {
    setBusy(`${role}-${id}`)
    try {
      await onApprove(id, role)
      toast.success('Account approved. They can now access CREST.')
    } catch (cause) {
      toast.error(
        cause instanceof Error ? cause.message : 'The account could not be approved. Try again.',
      )
    } finally {
      setBusy(null)
    }
  }
  return (
    <>
      <Card className="account-intro">
        <CardContent>
          <span className="attention-icon blue">
            <UserPlus size={26} />
          </span>
          <div>
            <h2>A welcome starts here.</h2>
            <p>
              Review new registrations before giving them access to your school’s research library.
            </p>
          </div>
          <Badge variant="outline">{pending.length} pending</Badge>
        </CardContent>
      </Card>
      <Card className="table-card" ref={ref}>
        <div className="table-card-heading">
          <div>
            <h2>Pending registrations</h2>
            <p>Only approved teacher accounts can manage this workspace.</p>
          </div>
        </div>
        {pending.length ? (
          cards ? (
            <ul className="account-card-list" aria-label="Pending registrations">
              {pagination.rows.map((account) => (
                <li key={`${account.role}-${account.id}`}>
                  <header>
                    <span className="student-avatar">{initials(account.name)}</span>
                    <div>
                      <h3>{account.name || 'Unnamed account'}</h3>
                      <Badge variant="outline" className="capitalize">
                        {account.role}
                      </Badge>
                    </div>
                  </header>
                  <dl>
                    <dt>{account.role === 'teacher' ? 'Email address' : 'Strand'}</dt>
                    <dd>{account.detail || 'Not recorded'}</dd>
                  </dl>
                  {account.role === 'student' ? (
                    <StudentAccountActions student={account} onReview={onReviewStudent} />
                  ) : (
                    <Button
                      variant="outline"
                      disabled={!!busy}
                      onClick={() => approve(account.id, account.role)}
                      aria-label={`Approve ${account.role} account for ${account.name}`}
                    >
                      <Check size={16} />
                      {busy === `${account.role}-${account.id}` ? 'Approving…' : 'Approve account'}
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Role</TableHead>
                  <TableHead>Strand / email</TableHead>
                  <TableHead>Action</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {pagination.rows.map((account) => (
                  <TableRow key={`${account.role}-${account.id}`}>
                    <TableCell>
                      <div className="group-cell">
                        <span className="student-avatar">{initials(account.name)}</span>
                        <strong>{account.name || 'Unnamed account'}</strong>
                      </div>
                    </TableCell>
                    <TableCell className="capitalize">{account.role}</TableCell>
                    <TableCell>{account.detail || 'Not recorded'}</TableCell>
                    <TableCell>
                      {account.role === 'student' ? (
                        <StudentAccountActions student={account} onReview={onReviewStudent} />
                      ) : (
                        <Button
                          variant="outline"
                          disabled={!!busy}
                          onClick={() => approve(account.id, account.role)}
                          aria-label={`Approve ${account.role} account for ${account.name}`}
                        >
                          <Check size={16} />
                          {busy === `${account.role}-${account.id}`
                            ? 'Approving…'
                            : 'Approve account'}
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )
        ) : (
          <EmptyState
            title="No registrations awaiting review"
            description="New student and teacher registrations will appear here for approval."
          />
        )}
        <ListPagination
          {...pagination}
          count={pagination.rows.length}
          total={pending.length}
          noun="accounts"
          onPage={setPage}
        />
      </Card>
    </>
  )
}
