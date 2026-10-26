import { useDeferredValue, useId, useMemo, useState } from 'react'
import {
  ArrowDown,
  ArrowRight,
  CheckCircle,
  FileMagnifyingGlass,
  MagnifyingGlass,
  UsersThree,
  X,
} from '@/icons'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent } from '@/components/ui/card'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  groupStatus,
  initials,
  statusLabels,
  studentAccountLabels,
  studentAccountStatus,
  type GroupStatus,
  type Student,
  type Workspace,
} from '../domain'
import { indexWorkspace, pageWindow } from '../workspace-index'
import { useCardLayout } from './use-card-layout'
import { ListPagination } from './list-pagination'
import { ResearchArt, ResearchSteps } from './research-art'

export function StatusBadge({ status }: { status: GroupStatus }) {
  return (
    <Badge variant="outline" className={`status-badge status-${status}`}>
      <span className="status-dot" />
      {statusLabels[status]}
    </Badge>
  )
}

export function StrandBadge({ strand }: { strand: string }) {
  return (
    <Badge variant="secondary" className={`strand-badge strand-${strand.toLowerCase()}`}>
      {strand || 'Unspecified'}
    </Badge>
  )
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string
  description: string
  action?: React.ReactNode
}) {
  return (
    <div className="empty-state">
      <ResearchArt variant="empty" />
      <h3>{title}</h3>
      <p>{description}</p>
      {action}
    </div>
  )
}

export function SearchInput({
  value,
  onChange,
  placeholder,
}: {
  value: string
  onChange: (value: string) => void
  placeholder: string
}) {
  const id = useId()
  return (
    <div className="search-field">
      <Label htmlFor={id}>Search</Label>
      <div className="search-input">
        <MagnifyingGlass size={18} />
        <Input
          id={id}
          aria-label={placeholder}
          placeholder={placeholder}
          value={value}
          onChange={(event) => onChange(event.target.value)}
        />
        {value && (
          <button aria-label="Clear search" onClick={() => onChange('')}>
            <X size={15} />
          </button>
        )}
      </div>
    </div>
  )
}

export function LabeledFilter({
  label,
  ariaLabel,
  value,
  onChange,
  children,
  className = '',
}: {
  label: string
  ariaLabel: string
  value: string
  onChange: (value: string) => void
  children: React.ReactNode
  className?: string
}) {
  const id = useId()
  return (
    <div className={`filter-field ${className}`}>
      <Label htmlFor={id}>{label}</Label>
      <Select value={value} onValueChange={onChange}>
        <SelectTrigger id={id} aria-label={ariaLabel}>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>{children}</SelectContent>
      </Select>
    </div>
  )
}

const groupFilters: { value: 'all' | GroupStatus; label: string }[] = [
  { value: 'all', label: 'All groups' },
  { value: 'pending', label: statusLabels.pending },
  { value: 'changes', label: statusLabels.changes },
  { value: 'approved', label: statusLabels.approved },
  { value: 'preparing', label: 'Preparing' },
]

export function GroupTable({
  workspace,
  onOpen,
  reviewsOnly = false,
  compact = false,
}: {
  workspace: Workspace
  onOpen: (id: string) => void
  reviewsOnly?: boolean
  compact?: boolean
}) {
  const [query, setQuery] = useState('')
  const [strand, setStrand] = useState('all')
  const [tab, setTab] = useState('all')
  const [ascending, setAscending] = useState(true)
  const [page, setPage] = useState(1)
  const { ref, cards } = useCardLayout()
  const lookup = useMemo(() => indexWorkspace(workspace), [workspace])
  const search = useDeferredValue(query.trim().toLowerCase())
  const base = useMemo(
    () => workspace.groups.filter((group) => !reviewsOnly || groupStatus(group) === 'pending'),
    [workspace.groups, reviewsOnly],
  )
  const strands = useMemo(
    () => [...new Set(workspace.groups.map((group) => group.strand).filter(Boolean))].sort(),
    [workspace.groups],
  )
  const statusCounts = useMemo(
    () =>
      base.reduce(
        (counts, group) => {
          counts[groupStatus(group)] += 1
          return counts
        },
        { pending: 0, changes: 0, approved: 0, preparing: 0 },
      ),
    [base],
  )
  const matches = useMemo(
    () =>
      base
        .filter((group) => {
          return (
            lookup.groupSearch.get(group.id)!.includes(search) &&
            (strand === 'all' || group.strand === strand) &&
            (tab === 'all' || groupStatus(group) === tab)
          )
        })
        .sort((a, b) => (ascending ? a.name.localeCompare(b.name) : b.name.localeCompare(a.name))),
    [base, lookup, search, strand, tab, ascending],
  )
  const pagination = pageWindow(matches, page, compact ? 5 : 12)
  const rows = pagination.rows
  function filter<T>(setter: (value: T) => void, value: T) {
    setter(value)
    setPage(1)
  }
  return (
    <Card className="table-card" ref={ref}>
      <div className="table-card-heading">
        <div>
          <h2>{reviewsOnly ? 'Ready for your feedback' : 'Research group progress'}</h2>
          <p>
            {reviewsOnly
              ? 'Read a submission, then approve it or guide the next revision.'
              : 'A shared view of where each group stands.'}
          </p>
        </div>
        <span className="count-label">
          {base.length} {base.length === 1 ? 'group' : 'groups'}
        </span>
      </div>
      <Tabs value={tab} onValueChange={(value) => filter(setTab, value)}>
        {!reviewsOnly && !cards && (
          <TabsList className="filter-tabs" aria-label="Filter by research status">
            {groupFilters.map(({ value, label }) => (
              <TabsTrigger key={value} value={value}>
                {label} <span>{value === 'all' ? base.length : statusCounts[value]}</span>
              </TabsTrigger>
            ))}
          </TabsList>
        )}
        <div className="table-toolbar">
          <SearchInput
            value={query}
            onChange={(value) => filter(setQuery, value)}
            placeholder="Search groups or research…"
          />
          <LabeledFilter
            label="Strand"
            ariaLabel="Filter groups by strand"
            value={strand}
            onChange={(value) => filter(setStrand, value)}
          >
            <SelectItem value="all">All strands</SelectItem>
            {strands.map((item) => (
              <SelectItem key={item} value={item}>
                {item}
              </SelectItem>
            ))}
          </LabeledFilter>
          {!reviewsOnly && cards && (
            <LabeledFilter
              label="Research status"
              ariaLabel="Filter groups by research status"
              value={tab}
              onChange={(value) => filter(setTab, value)}
              className="status-filter"
            >
              {groupFilters.map(({ value, label }) => (
                <SelectItem key={value} value={value}>
                  {label} ({value === 'all' ? base.length : statusCounts[value]})
                </SelectItem>
              ))}
            </LabeledFilter>
          )}
        </div>
        <TabsContent
          value={tab}
          className="table-panel"
          {...(reviewsOnly || cards
            ? {
                role: 'region' as const,
                'aria-label': reviewsOnly
                  ? 'Submissions ready for review'
                  : 'Filtered research groups',
                'aria-labelledby': undefined,
              }
            : {})}
        >
          {rows.length ? (
            <>
              {!cards ? (
                <div className="group-table-desktop">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead
                          className="group-column"
                          aria-sort={ascending ? 'ascending' : 'descending'}
                        >
                          <button
                            className="sort-button"
                            onClick={() => setAscending(!ascending)}
                            aria-label={`Sort groups by name, ${ascending ? 'currently A to Z' : 'currently Z to A'}`}
                          >
                            Group & research{' '}
                            <ArrowDown size={13} className={ascending ? '' : 'rotate-180'} />
                          </button>
                        </TableHead>
                        <TableHead>Strand</TableHead>
                        <TableHead>Members</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead>
                          <span className="sr-only">Action</span>
                        </TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {rows.map((group, index) => (
                        <TableRow key={group.id}>
                          <TableCell>
                            <div className="group-cell">
                              <span className={`group-avatar avatar-${index % 4}`}>
                                <UsersThree size={21} />
                              </span>
                              <div>
                                <button className="name-button" onClick={() => onOpen(group.id)}>
                                  {group.name || 'Unnamed group'}
                                </button>
                                <p className="research-snippet" title={group.title}>
                                  {group.title || 'Research has not been submitted yet'}
                                </p>
                              </div>
                            </div>
                          </TableCell>
                          <TableCell>
                            <StrandBadge strand={group.strand} />
                          </TableCell>
                          <TableCell>
                            <div
                              className="member-stack"
                              aria-label={`${group.memberIds.length} members`}
                            >
                              {group.memberIds.slice(0, 3).map((id, memberIndex) => (
                                <span
                                  key={id}
                                  className={`member-avatar member-${memberIndex}`}
                                  title={lookup.studentsById.get(id)?.name || 'Student'}
                                >
                                  {initials(lookup.studentsById.get(id)?.name || 'Student')}
                                </span>
                              ))}
                              {group.memberIds.length > 3 && (
                                <span className="member-more">+{group.memberIds.length - 3}</span>
                              )}
                              {!group.memberIds.length && <span className="muted">No members</span>}
                            </div>
                          </TableCell>
                          <TableCell>
                            <StatusBadge status={groupStatus(group)} />
                          </TableCell>
                          <TableCell>
                            <Button
                              size="sm"
                              variant="ghost"
                              className="row-action"
                              onClick={() => onOpen(group.id)}
                              aria-label={`${groupStatus(group) === 'pending' ? 'Review' : 'View'} ${group.name}`}
                            >
                              {groupStatus(group) === 'pending' ? 'Review paper' : 'View group'}
                              <ArrowRight size={15} />
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              ) : (
                <ul className="group-card-list" aria-label="Research groups">
                  {rows.map((group, index) => (
                    <li className="group-reading-card" key={group.id}>
                      <header>
                        <span className={`group-avatar avatar-${index % 4}`}>
                          <UsersThree size={22} />
                        </span>
                        <h3>{group.name || 'Unnamed group'}</h3>
                      </header>
                      <p>
                        {group.title || 'This group is preparing its first research submission.'}
                      </p>
                      <div className="group-card-meta">
                        <StrandBadge strand={group.strand} />
                        <span>
                          <UsersThree size={18} />
                          {group.memberIds.length}{' '}
                          {group.memberIds.length === 1 ? 'member' : 'members'}
                        </span>
                      </div>
                      <ResearchSteps status={groupStatus(group)} />
                      <div className="group-card-footer">
                        <StatusBadge status={groupStatus(group)} />
                        <Button
                          variant={groupStatus(group) === 'pending' ? 'default' : 'outline'}
                          onClick={() => onOpen(group.id)}
                          aria-label={`${groupStatus(group) === 'pending' ? 'Review paper for' : 'View group'} ${group.name}`}
                        >
                          {groupStatus(group) === 'pending' ? 'Review paper' : 'View group'}
                          <ArrowRight size={18} />
                        </Button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </>
          ) : (
            <EmptyState
              title={
                base.length
                  ? 'No groups match these filters'
                  : reviewsOnly
                    ? 'You’re all caught up'
                    : 'Your research groups will appear here'
              }
              description={
                base.length
                  ? 'Try another name or clear your filters.'
                  : reviewsOnly
                    ? 'New student submissions will appear here when they are ready for review.'
                    : 'Groups created by students in the CREST app will appear in your workspace.'
              }
              action={
                base.length ? (
                  <Button
                    variant="outline"
                    onClick={() => {
                      setQuery('')
                      setStrand('all')
                      setTab('all')
                    }}
                  >
                    Clear filters
                  </Button>
                ) : undefined
              }
            />
          )}
        </TabsContent>
      </Tabs>
      {compact ? (
        <div className="table-footer">
          Showing {rows.length} of {matches.length} groups
        </div>
      ) : (
        <ListPagination
          {...pagination}
          count={rows.length}
          total={matches.length}
          noun="groups"
          onPage={setPage}
        />
      )}
    </Card>
  )
}

export function StudentTable({
  workspace,
  onOpen,
}: {
  workspace: Workspace
  onOpen: (student: Student) => void
}) {
  const [query, setQuery] = useState('')
  const [strand, setStrand] = useState('all')
  const [status, setStatus] = useState('all')
  const [accountStatus, setAccountStatus] = useState('all')
  const [page, setPage] = useState(1)
  const { ref, cards } = useCardLayout()
  const lookup = useMemo(() => indexWorkspace(workspace), [workspace])
  const search = useDeferredValue(query.trim().toLowerCase())
  const students = workspace.students
  const strands = useMemo(
    () => [...new Set(students.map((student) => student.strand).filter(Boolean))].sort(),
    [students],
  )
  const matches = useMemo(
    () =>
      students
        .filter((student) => {
          const group = lookup.studentGroups.get(student.id)
          return (
            `${student.name} ${group?.name || ''}`.toLowerCase().includes(search) &&
            (accountStatus === 'all' || studentAccountStatus(student) === accountStatus) &&
            (strand === 'all' || strand === student.strand) &&
            (status === 'all' ||
              (status === 'ungrouped' ? !group : group && groupStatus(group) === status))
          )
        })
        .sort((a, b) => a.name.localeCompare(b.name)),
    [students, lookup, search, strand, status, accountStatus],
  )
  const pagination = pageWindow(matches, page)
  const rows = pagination.rows
  function filter<T>(setter: (value: T) => void, value: T) {
    setter(value)
    setPage(1)
  }
  return (
    <Card className="table-card" ref={ref}>
      <div className="table-card-heading">
        <div>
          <h2>Student management</h2>
          <p>Manage student access and follow their group’s research submission.</p>
        </div>
        <span className="count-label">{students.length} students</span>
      </div>
      <div className="table-toolbar wrap-toolbar">
        <SearchInput
          value={query}
          onChange={(value) => filter(setQuery, value)}
          placeholder="Search students or groups…"
        />
        <LabeledFilter
          label="Account status"
          ariaLabel="Filter students by account status"
          value={accountStatus}
          onChange={(value) => filter(setAccountStatus, value)}
        >
          <SelectItem value="all">All accounts</SelectItem>
          {Object.entries(studentAccountLabels).map(([key, label]) => (
            <SelectItem value={key} key={key}>
              {label}
            </SelectItem>
          ))}
        </LabeledFilter>
        <LabeledFilter
          label="Strand"
          ariaLabel="Filter students by strand"
          value={strand}
          onChange={(value) => filter(setStrand, value)}
        >
          <SelectItem value="all">All strands</SelectItem>
          {strands.map((item) => (
            <SelectItem value={item} key={item}>
              {item}
            </SelectItem>
          ))}
        </LabeledFilter>
        <LabeledFilter
          label="Research status"
          ariaLabel="Filter students by progress"
          className="status-filter"
          value={status}
          onChange={(value) => filter(setStatus, value)}
        >
          <SelectItem value="all">All progress</SelectItem>
          {Object.entries(statusLabels).map(([key, label]) => (
            <SelectItem value={key} key={key}>
              {label}
            </SelectItem>
          ))}
          <SelectItem value="ungrouped">No group yet</SelectItem>
        </LabeledFilter>
      </div>
      {rows.length ? (
        <>
          {!cards ? (
            <div className="student-table-desktop">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Student</TableHead>
                    <TableHead>Account status</TableHead>
                    <TableHead>Strand</TableHead>
                    <TableHead>Research group</TableHead>
                    <TableHead>Research status</TableHead>
                    <TableHead>
                      <span className="sr-only">Action</span>
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rows.map((student, index) => {
                    const group = lookup.studentGroups.get(student.id)
                    return (
                      <TableRow key={student.id}>
                        <TableCell>
                          <div className="group-cell">
                            <span className={`student-avatar avatar-${index % 4}`}>
                              {initials(student.name)}
                            </span>
                            <button className="name-button" onClick={() => onOpen(student)}>
                              {student.name || 'Unnamed student'}
                            </button>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline">
                            {studentAccountLabels[studentAccountStatus(student)]}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <StrandBadge strand={student.strand} />
                        </TableCell>
                        <TableCell>
                          {group?.name || <span className="muted">No group yet</span>}
                        </TableCell>
                        <TableCell>
                          {group ? (
                            <StatusBadge status={groupStatus(group)} />
                          ) : (
                            <Badge variant="outline">Not started</Badge>
                          )}
                        </TableCell>
                        <TableCell>
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => onOpen(student)}
                            aria-label={`View ${student.name}`}
                          >
                            Manage student
                            <ArrowRight size={15} />
                          </Button>
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </div>
          ) : (
            <ul className="student-card-list" aria-label="Students">
              {rows.map((student) => {
                const group = lookup.studentGroups.get(student.id)
                return (
                  <li className="group-reading-card student-reading-card" key={student.id}>
                    <header className={`student-card-heading tone-${student.strand.toLowerCase()}`}>
                      <span className="student-avatar">{initials(student.name)}</span>
                      <div>
                        <span className="card-eyebrow">
                          {studentAccountLabels[studentAccountStatus(student)]}
                        </span>
                        <h3>{student.name || 'Unnamed student'}</h3>
                      </div>
                      <StrandBadge strand={student.strand} />
                    </header>
                    <div className="student-project">
                      <span className="card-eyebrow">
                        <UsersThree size={16} />
                        {group?.name || 'Waiting for a research group'}
                      </span>
                      <h4>{group?.title || 'A new research journey starts here'}</h4>
                      <p>
                        {group
                          ? 'Group submission progress'
                          : 'Their progress will appear after joining a group in the app.'}
                      </p>
                      {group ? (
                        <StatusBadge status={groupStatus(group)} />
                      ) : (
                        <Badge variant="outline">No group yet</Badge>
                      )}
                    </div>
                    <ResearchSteps status={group ? groupStatus(group) : undefined} />
                    <Button
                      variant="outline"
                      onClick={() => onOpen(student)}
                      aria-label={`View student ${student.name}`}
                    >
                      Manage student
                      <ArrowRight size={18} />
                    </Button>
                  </li>
                )
              })}
            </ul>
          )}
        </>
      ) : (
        <EmptyState
          title="No students to show"
          description={
            students.length
              ? 'Try another name, strand, account status, or progress filter.'
              : 'Student registrations will appear here.'
          }
          action={
            students.length ? (
              <Button
                variant="outline"
                onClick={() => {
                  setQuery('')
                  setStrand('all')
                  setStatus('all')
                  setAccountStatus('all')
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
        count={rows.length}
        total={matches.length}
        noun="students"
        onPage={setPage}
      />
    </Card>
  )
}

export function SummaryStrip({
  workspace,
  onSelect,
}: {
  workspace: Workspace
  onSelect: (page: 'students' | 'groups' | 'reviews' | 'research') => void
}) {
  const approved = workspace.groups.filter((group) => groupStatus(group) === 'approved').length
  const pending = workspace.groups.filter((group) => groupStatus(group) === 'pending').length
  const stats = [
    {
      title: 'Students',
      value: workspace.students.filter((student) => student.accepted).length,
      description: 'Approved student accounts',
      icon: UsersThree,
      page: 'students' as const,
      color: 'blue',
    },
    {
      title: 'Research groups',
      value: workspace.groups.length,
      description: 'Working toward their next step',
      icon: FileMagnifyingGlass,
      page: 'groups' as const,
      color: 'teal',
    },
    {
      title: 'Pending review',
      value: pending,
      description: pending ? 'Ready for your feedback' : 'You’re all caught up',
      icon: FileMagnifyingGlass,
      page: 'reviews' as const,
      color: 'amber',
    },
    {
      title: 'Approved research',
      value: approved,
      description: 'Group papers added to the library',
      icon: CheckCircle,
      page: 'research' as const,
      color: 'green',
    },
  ]
  return (
    <div className="stats-grid">
      {stats.map((stat) => (
        <Card key={stat.title} className={`stat-card stat-${stat.color}`}>
          <CardContent>
            <div className="stat-top">
              <span>{stat.title}</span>
              <span className="stat-icon">
                <stat.icon size={20} />
              </span>
            </div>
            <div className="stat-bottom">
              <strong>{stat.value.toString().padStart(2, '0')}</strong>
              <button
                onClick={() => onSelect(stat.page)}
                aria-label={`View ${stat.title.toLowerCase()}`}
              >
                <ArrowRight size={19} />
              </button>
            </div>
            <p>{stat.description}</p>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
