import {
  lazy,
  Suspense,
  useEffect,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
} from 'react'
import {
  ArrowRight,
  Bell,
  BookOpenText,
  CaretDown,
  CaretRight,
  ClipboardText,
  DownloadSimple,
  Files,
  GraduationCap,
  Info,
  MagnifyingGlass,
  Plus,
  Question,
  SignOut,
  SquaresFour,
  Users,
  UsersThree,
  X,
  type Icon,
} from '@/icons'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Toaster } from '@/components/ui/sonner'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { EmptyState, GroupTable, SearchInput, StudentTable } from './components/workspace'
import type { FileRequest } from './components/workspace-dialogs'
import { useWorkspace } from './data/useWorkspace'
import { AuthPage } from './components/auth-page'
import { CrestLogo } from './components/crest-logo'
import { DisplaySettings } from './components/display-settings'
import { PageGuidance } from './components/page-guidance'
import { isWorkspaceRoute, parseRoute, type Route } from './routing'
import {
  groupStatus,
  initials,
  statusLabels,
  studentGroup,
  toCsv,
  type Page,
  type Student,
} from './domain'
import './App.css'

const Overview = lazy(() => import('./components/overview').then((m) => ({ default: m.Overview })))
const ResearchLibrary = lazy(() =>
  import('./components/library').then((m) => ({ default: m.ResearchLibrary })),
)
const DocumentLibrary = lazy(() =>
  import('./components/library').then((m) => ({ default: m.DocumentLibrary })),
)
const Accounts = lazy(() => import('./components/library').then((m) => ({ default: m.Accounts })))
const LandingPage = lazy(() =>
  import('./components/landing-page').then((m) => ({ default: m.LandingPage })),
)
const GroupDetails = lazy(() =>
  import('./components/workspace-dialogs').then((m) => ({ default: m.GroupDetails })),
)
const StudentDetails = lazy(() =>
  import('./components/workspace-dialogs').then((m) => ({ default: m.StudentDetails })),
)
const UploadDialog = lazy(() =>
  import('./components/workspace-dialogs').then((m) => ({ default: m.UploadDialog })),
)
const FileViewer = lazy(() =>
  import('./components/workspace-dialogs').then((m) => ({ default: m.FileViewer })),
)

function LoadingContent() {
  return (
    <div className="content-loading" role="status">
      Loading your workspace…
    </div>
  )
}

const navigation: { page: Page; label: string; icon: Icon; section: 'classroom' | 'library' }[] = [
  { page: 'overview', label: 'Overview', icon: SquaresFour, section: 'classroom' },
  { page: 'groups', label: 'Research groups', icon: UsersThree, section: 'classroom' },
  { page: 'students', label: 'Students', icon: GraduationCap, section: 'classroom' },
  { page: 'reviews', label: 'Submission reviews', icon: ClipboardText, section: 'classroom' },
  { page: 'research', label: 'Research library', icon: BookOpenText, section: 'library' },
  { page: 'documents', label: 'Documents', icon: Files, section: 'library' },
  { page: 'accounts', label: 'Account approvals', icon: Users, section: 'library' },
]

const descriptions: Record<Page, { title: string; description: string; eyebrow: string }> = {
  overview: {
    title: 'Your classroom overview',
    description: 'Keep research moving, one group at a time.',
    eyebrow: 'A LITTLE CLARITY FOR YOUR DAY',
  },
  groups: {
    title: 'Research groups',
    description: 'Follow research submissions and keep your groups moving forward.',
    eyebrow: 'RESEARCH GROUPS',
  },
  students: {
    title: 'Student progress',
    description: 'See each student, their research group, and their submission progress.',
    eyebrow: 'STUDENT PROGRESS',
  },
  reviews: {
    title: 'Submission reviews',
    description: 'Read manuscripts, guide revisions, and welcome new work to the library.',
    eyebrow: 'SUBMISSION REVIEWS',
  },
  research: {
    title: 'Research library',
    description: 'Find, read, and preserve the research made by your school community.',
    eyebrow: 'THE SCHOOL RESEARCH LIBRARY',
  },
  documents: {
    title: 'School documents',
    description: 'Share the templates, rubrics, and guides your students need.',
    eyebrow: 'SCHOOL DOCUMENTS',
  },
  accounts: {
    title: 'Account approvals',
    description: 'Review student and teacher registrations for your school.',
    eyebrow: 'ACCOUNT APPROVALS',
  },
}

function App() {
  const service = useWorkspace()
  const { workspace, teacher, isDemo, session } = service
  const [routeName, setRouteName] = useState<Route>(() => parseRoute(location.hash))
  const page: Page = isWorkspaceRoute(routeName) ? routeName : 'overview'
  const [mobileNav, setMobileNav] = useState(false)
  const navigationRef = useRef<HTMLElement>(null)
  const wasNavigationOpen = useRef(false)
  const [selectedGroup, setSelectedGroup] = useState<string | null>(null)
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null)
  const [uploadKind, setUploadKind] = useState<'research' | 'document' | null>(null)
  const [file, setFile] = useState<FileRequest | null>(null)
  const [searchOpen, setSearchOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [helpOpen, setHelpOpen] = useState(false)
  const [signingIn, setSigningIn] = useState(false)
  const [authError, setAuthError] = useState('')
  const pending = workspace.groups.filter((group) => groupStatus(group) === 'pending')
  const pendingAccounts =
    workspace.students.filter((student) => !student.accepted).length +
    workspace.teachers.filter((item) => !item.access).length
  const currentNav = navigation.find((item) => item.page === page)!

  useEffect(() => {
    const restoreFocus = wasNavigationOpen.current
    wasNavigationOpen.current = mobileNav
    const frame = requestAnimationFrame(() => {
      if (mobileNav)
        navigationRef.current?.querySelector<HTMLButtonElement>('.mobile-nav-close')?.focus()
      else if (restoreFocus) document.querySelector<HTMLButtonElement>('.mobile-menu')?.focus()
    })
    return () => cancelAnimationFrame(frame)
  }, [mobileNav])

  useEffect(() => {
    const desktop = matchMedia('(min-width: 768px)')
    const closeOnDesktop = () => {
      if (desktop.matches) setMobileNav(false)
    }
    desktop.addEventListener('change', closeOnDesktop)
    return () => desktop.removeEventListener('change', closeOnDesktop)
  }, [])

  function trapNavigationFocus(event: ReactKeyboardEvent<HTMLElement>) {
    if (!mobileNav || event.key !== 'Tab') return
    const targets = Array.from(
      navigationRef.current?.querySelectorAll<HTMLElement>('a[href], button:not(:disabled)') || [],
    ).filter((element) => element.getClientRects().length > 0)
    const first = targets[0],
      last = targets[targets.length - 1]
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last?.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first?.focus()
    }
  }

  useEffect(() => {
    const handleHash = () => {
      setRouteName(parseRoute(location.hash))
      setMobileNav(false)
      setSearchOpen(false)
      setSelectedGroup(null)
      setSelectedStudent(null)
      setUploadKind(null)
      setFile(null)
      setHelpOpen(false)
      window.scrollTo(0, 0)
    }
    const handleKey = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        if (!isWorkspaceRoute(parseRoute(location.hash))) return
        event.preventDefault()
        setSearchOpen((open) => !open)
      }
      if (event.key === 'Escape') setMobileNav(false)
    }
    window.addEventListener('hashchange', handleHash)
    window.addEventListener('keydown', handleKey)
    return () => {
      window.removeEventListener('hashchange', handleHash)
      window.removeEventListener('keydown', handleKey)
    }
  }, [])

  useEffect(() => {
    document.title =
      routeName === 'home'
        ? 'CREST · School research, shared.'
        : routeName === 'signin'
          ? 'Teacher sign in · CREST'
          : `${currentNav.label} · CREST Teacher workspace`
  }, [currentNav.label, routeName])

  useEffect(() => {
    if (routeName === 'signin' && !isDemo && session === 'ready') location.hash = 'overview'
  }, [routeName, isDemo, session])

  function navigate(next: Route) {
    location.hash = next
    setRouteName(next)
    setMobileNav(false)
    window.scrollTo(0, 0)
  }

  async function login() {
    setSigningIn(true)
    setAuthError('')
    try {
      await service.login()
    } catch {
      setAuthError('Google sign-in could not be completed. Allow the sign-in window and try again.')
    } finally {
      setSigningIn(false)
    }
  }

  function exportProgress() {
    if (!workspace.students.length) {
      toast.info('There are no student records to export yet.')
      return
    }
    const rows: (string | number)[][] = [
      [
        'Student',
        'Strand',
        'Account status',
        'Research group',
        'Research title',
        'Research status',
      ],
    ]
    workspace.students.forEach((student) => {
      const group = studentGroup(student, workspace.groups)
      rows.push([
        student.name,
        student.strand,
        student.accepted ? 'Approved' : 'Pending approval',
        group?.name || 'No group',
        group?.title || '',
        group ? statusLabels[groupStatus(group)] : 'Not started',
      ])
    })
    const url = URL.createObjectURL(new Blob([toCsv(rows)], { type: 'text/csv;charset=utf-8;' }))
    const link = document.createElement('a')
    link.href = url
    link.download = `CREST-${isDemo ? 'demo-' : ''}student-progress.csv`
    link.click()
    window.setTimeout(() => URL.revokeObjectURL(url), 1000)
    toast.success('Student progress report downloaded.')
  }

  if (routeName === 'home') {
    return (
      <Suspense fallback={<LoadingContent />}>
        <LandingPage
          isDemo={isDemo}
          signedIn={!isDemo && session === 'ready'}
          onSignIn={() => navigate('signin')}
          onWorkspace={() => navigate('overview')}
        />
      </Suspense>
    )
  }

  if (routeName === 'signin' || !service.validConfig || (!isDemo && session !== 'ready')) {
    return (
      <TooltipProvider>
        <AuthPage
          isDemo={isDemo}
          validConfig={service.validConfig}
          session={session}
          error={service.error}
          signingIn={signingIn}
          authError={authError}
          onLogin={login}
          onRetry={service.refresh}
          onLogout={() =>
            service.logout().catch(() => toast.error('Sign-out could not be completed. Try again.'))
          }
          onHome={() => navigate('home')}
          onDemo={() => navigate('overview')}
        />
        <Toaster />
      </TooltipProvider>
    )
  }

  const lowerQuery = query.trim().toLowerCase()
  const searchGroups = (searchOpen ? workspace.groups : [])
    .filter((group) => `${group.name} ${group.title}`.toLowerCase().includes(lowerQuery))
    .slice(0, 4)
  const searchStudents = (searchOpen ? workspace.students : [])
    .filter((student) => student.name.toLowerCase().includes(lowerQuery))
    .slice(0, 4)
  const searchResearch = (searchOpen ? workspace.research : [])
    .filter((paper) =>
      `${paper.title} ${paper.members.join(' ')}`.toLowerCase().includes(lowerQuery),
    )
    .slice(0, 3)
  const searchDocuments = (searchOpen ? workspace.documents : [])
    .filter((document) =>
      `${document.name} ${document.description}`.toLowerCase().includes(lowerQuery),
    )
    .slice(0, 3)
  const matches =
    searchGroups.length + searchStudents.length + searchResearch.length + searchDocuments.length

  return (
    <TooltipProvider>
      <a
        className="skip-link"
        href="#main-content"
        onClick={(event) => {
          event.preventDefault()
          document.getElementById('main-content')?.focus()
        }}
      >
        Skip to content
      </a>
      <div className="app-shell">
        {mobileNav && (
          <button
            className="mobile-nav-scrim"
            aria-label="Close navigation"
            onClick={() => setMobileNav(false)}
          />
        )}
        <aside
          ref={navigationRef}
          role={mobileNav ? 'dialog' : undefined}
          aria-modal={mobileNav ? true : undefined}
          onKeyDown={trapNavigationFocus}
          className={`sidebar ${mobileNav ? 'sidebar-open' : ''}`}
          aria-label="Main navigation"
        >
          <a
            className="brand-lockup"
            href="#home"
            onClick={() => navigate('home')}
            aria-label="CREST home"
          >
            <CrestLogo size={44} decorative />
            <span>
              <span className="wordmark">CREST</span>
              <span className="brand-descriptor">School research library</span>
            </span>
          </a>
          <button
            className="mobile-nav-close"
            aria-label="Close navigation"
            onClick={() => setMobileNav(false)}
          >
            <X size={22} />
          </button>
          <div className="workspace-switch">
            <span className="workspace-icon">
              <GraduationCap size={21} />
            </span>
            <div>
              <strong>Teacher workspace</strong>
              <span>Bonifacio D. Borebor SHS</span>
            </div>
            <span
              className="workspace-status"
              aria-label={isDemo ? 'Demo workspace' : 'Connected workspace'}
            />
          </div>
          <nav>
            {(['classroom', 'library'] as const).map((section) => (
              <div className="nav-section" key={section}>
                <p>{section === 'classroom' ? 'YOUR CLASSROOM' : 'SCHOOL LIBRARY'}</p>
                {navigation
                  .filter((item) => item.section === section)
                  .map((item) => (
                    <a
                      key={item.page}
                      href={`#${item.page}`}
                      className={`nav-item ${page === item.page ? 'nav-item-active' : ''}`}
                      aria-current={page === item.page ? 'page' : undefined}
                      onClick={() => navigate(item.page)}
                    >
                      <item.icon size={21} weight={page === item.page ? 'duotone' : 'regular'} />
                      <span>{item.label}</span>
                      {item.page === 'reviews' && pending.length > 0 && (
                        <span className="nav-count">{pending.length}</span>
                      )}
                      {item.page === 'accounts' && pendingAccounts > 0 && (
                        <span className="nav-count neutral-count">{pendingAccounts}</span>
                      )}
                    </a>
                  ))}
              </div>
            ))}
          </nav>
          <div className="sidebar-bottom">
            <div className="sidebar-note">
              <span>
                <BookOpenText size={22} />
              </span>
              <h3>Small steps. Shared discoveries.</h3>
              <p>A little guidance today can inspire tomorrow’s research.</p>
              <button onClick={() => setHelpOpen(true)}>
                Your teacher’s guide
                <ArrowRight size={14} />
              </button>
            </div>
            <button className="help-link" onClick={() => setHelpOpen(true)}>
              <Question size={20} />
              Help & getting started
              <ArrowRight size={15} />
            </button>
            <div className="sidebar-school">
              <span className="tiny-dot" />
              <span>Made for your school community.</span>
            </div>
          </div>
        </aside>
        <div className="workspace-main" inert={mobileNav}>
          <header className="topbar">
            <div className="breadcrumb">
              <Button
                variant="ghost"
                size="icon"
                className="mobile-menu"
                aria-label="Open navigation"
                aria-expanded={mobileNav}
                onClick={() => setMobileNav(true)}
              >
                <SquaresFour size={22} />
              </Button>
              <span>Workspace</span>
              <CaretRight size={13} />
              <strong>{currentNav.label}</strong>
            </div>
            <div className="topbar-actions">
              <DisplaySettings />
              <button
                className="global-search"
                aria-label="Search your workspace"
                onClick={() => setSearchOpen(true)}
              >
                <MagnifyingGlass size={18} />
                <span>Search your workspace</span>
                <kbd>Ctrl K</kbd>
              </button>
              <Tooltip>
                <TooltipTrigger asChild>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="notification-button"
                        aria-label={`Notifications, ${pending.length + pendingAccounts} items need attention`}
                      >
                        <Bell size={21} />
                        {!!(pending.length + pendingAccounts) && <span />}
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="notification-menu">
                      <DropdownMenuLabel>Needs your attention</DropdownMenuLabel>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem onSelect={() => navigate('reviews')}>
                        <ClipboardText size={18} />
                        <span>{pending.length} submissions to review</span>
                      </DropdownMenuItem>
                      <DropdownMenuItem onSelect={() => navigate('accounts')}>
                        <Users size={18} />
                        <span>{pendingAccounts} accounts to approve</span>
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TooltipTrigger>
                <TooltipContent>Needs your attention</TooltipContent>
              </Tooltip>
              <div className="topbar-divider" />
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <button className="profile-button" aria-label="Teacher account menu">
                    <span className="profile-avatar">{initials(teacher?.name || 'Teacher')}</span>
                    <span>
                      <strong>{teacher?.name || 'Teacher'}</strong>
                      <small>Research teacher</small>
                    </span>
                    <CaretDown size={14} />
                  </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuLabel>
                    {isDemo ? 'Demo teacher account' : teacher?.name}
                  </DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onSelect={() => setHelpOpen(true)}>
                    <Info size={17} />
                    About your workspace
                  </DropdownMenuItem>
                  {!isDemo && (
                    <DropdownMenuItem
                      onSelect={() =>
                        service
                          .logout()
                          .catch(() => toast.error('Sign-out could not be completed. Try again.'))
                      }
                    >
                      <SignOut size={17} />
                      Sign out
                    </DropdownMenuItem>
                  )}
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </header>
          <main id="main-content" tabIndex={-1}>
            <div className="page-content">
              <div className="page-heading">
                <div>
                  <div className="eyebrow greeting">
                    {page === 'overview'
                      ? `HELLO, ${teacher?.name.split(' ')[0].toUpperCase() || 'TEACHER'} · YOUR TEACHER WORKSPACE`
                      : descriptions[page].eyebrow}
                  </div>
                  <h1>{descriptions[page].title}</h1>
                  <p>{descriptions[page].description}</p>
                </div>
                <div className="page-heading-actions">
                  {page !== 'research' && page !== 'documents' && page !== 'accounts' && (
                    <Button variant="outline" className="export-button" onClick={exportProgress}>
                      <DownloadSimple size={18} />
                      Export progress
                    </Button>
                  )}
                  {page === 'research' || page === 'documents' ? (
                    <Button
                      onClick={() => setUploadKind(page === 'documents' ? 'document' : 'research')}
                    >
                      <Plus size={18} />
                      {page === 'documents' ? 'Share document' : 'Add research'}
                    </Button>
                  ) : page === 'overview' ? (
                    <Button onClick={() => navigate('reviews')}>
                      Review submissions
                      <ArrowRight size={17} />
                    </Button>
                  ) : null}
                </div>
              </div>
              <PageGuidance page={page} navigate={navigate} />
              {isDemo && (
                <div className="demo-banner">
                  <span>
                    <Info size={17} />
                    <strong>Demo workspace</strong>
                    <span>Explore with sample records. Changes are saved in this browser.</span>
                  </span>
                  <button onClick={() => setHelpOpen(true)}>
                    About this demo
                    <ArrowRight size={15} />
                  </button>
                </div>
              )}
              <Suspense fallback={<LoadingContent />}>
                {page === 'overview' && (
                  <Overview
                    workspace={workspace}
                    onNavigate={navigate}
                    onGroup={setSelectedGroup}
                    onFile={setFile}
                  />
                )}
                {page === 'groups' && (
                  <GroupTable workspace={workspace} onOpen={setSelectedGroup} />
                )}
                {page === 'reviews' && (
                  <>
                    <div className="review-intro">
                      <div className="review-intro-icon">
                        <ClipboardText size={27} weight="duotone" />
                      </div>
                      <div>
                        <strong>
                          {pending.length
                            ? `${pending.length} ${pending.length === 1 ? 'manuscript is' : 'manuscripts are'} ready for your review.`
                            : 'Your review queue is clear.'}
                        </strong>
                        <p>
                          Approving research adds it to the shared library. Requesting changes sends
                          feedback to the group.
                        </p>
                      </div>
                    </div>
                    <GroupTable workspace={workspace} onOpen={setSelectedGroup} reviewsOnly />
                  </>
                )}
                {page === 'students' && (
                  <StudentTable workspace={workspace} onOpen={setSelectedStudent} />
                )}
                {page === 'research' && <ResearchLibrary workspace={workspace} onFile={setFile} />}
                {page === 'documents' && <DocumentLibrary workspace={workspace} onFile={setFile} />}
                {page === 'accounts' && (
                  <Accounts workspace={workspace} onApprove={service.approveAccount} />
                )}
              </Suspense>
              <footer className="page-footer">
                <span>
                  <span className="wordmark">CREST</span>
                  <span>School research, shared.</span>
                </span>
                <span>Bonifacio D. Borebor Sr. High School</span>
              </footer>
            </div>
          </main>
        </div>
        <Suspense
          fallback={
            <div className="dialog-loading" role="status">
              Opening details…
            </div>
          }
        >
          {selectedGroup && (
            <GroupDetails
              key={selectedGroup}
              group={workspace.groups.find((group) => group.id === selectedGroup)}
              workspace={workspace}
              onClose={() => setSelectedGroup(null)}
              onReview={service.review}
              onFile={setFile}
            />
          )}
          {selectedStudent && (
            <StudentDetails
              student={selectedStudent}
              workspace={workspace}
              onClose={() => setSelectedStudent(null)}
              onGroup={setSelectedGroup}
            />
          )}
          {uploadKind && (
            <UploadDialog
              key={uploadKind || 'closed'}
              kind={uploadKind}
              onClose={() => setUploadKind(null)}
              onUpload={service.upload}
            />
          )}
          {file && (
            <FileViewer
              key={file?.id || 'no-file'}
              file={file}
              onClose={() => setFile(null)}
              readFile={service.readFile}
              isDemo={isDemo}
            />
          )}
        </Suspense>
        <Dialog open={searchOpen} onOpenChange={setSearchOpen}>
          <DialogContent className="search-dialog">
            <DialogHeader>
              <DialogTitle>Find it in your workspace</DialogTitle>
              <DialogDescription>
                Search groups, students, research papers, and documents.
              </DialogDescription>
            </DialogHeader>
            <SearchInput
              value={query}
              onChange={setQuery}
              placeholder="Try a student name or research title…"
            />
            <div className="workspace-search-results">
              {searchGroups.length > 0 && (
                <section>
                  <h3>Research groups</h3>
                  {searchGroups.map((group) => (
                    <button
                      key={group.id}
                      onClick={() => {
                        setSearchOpen(false)
                        setSelectedGroup(group.id)
                      }}
                    >
                      <UsersThree size={20} />
                      <span>
                        <strong>{group.name}</strong>
                        <small>{group.title || 'Preparing research'}</small>
                      </span>
                      <ArrowRight size={16} />
                    </button>
                  ))}
                </section>
              )}
              {searchStudents.length > 0 && (
                <section>
                  <h3>Students</h3>
                  {searchStudents.map((student) => (
                    <button
                      key={student.id}
                      onClick={() => {
                        setSearchOpen(false)
                        setSelectedStudent(student)
                      }}
                    >
                      <span className="student-avatar">{initials(student.name)}</span>
                      <span>
                        <strong>{student.name}</strong>
                        <small>{student.strand}</small>
                      </span>
                      <ArrowRight size={16} />
                    </button>
                  ))}
                </section>
              )}
              {searchResearch.length > 0 && (
                <section>
                  <h3>Research library</h3>
                  {searchResearch.map((paper) => (
                    <button
                      key={`${paper.type}-${paper.id}`}
                      disabled={!paper.fileId}
                      onClick={() => {
                        setSearchOpen(false)
                        setFile({ id: paper.fileId, title: paper.title })
                      }}
                    >
                      <BookOpenText size={20} />
                      <span>
                        <strong>{paper.title}</strong>
                        <small>{paper.strand}</small>
                      </span>
                      <ArrowRight size={16} />
                    </button>
                  ))}
                </section>
              )}
              {searchDocuments.length > 0 && (
                <section>
                  <h3>Documents</h3>
                  {searchDocuments.map((document) => (
                    <button
                      key={document.id}
                      disabled={!document.fileId}
                      onClick={() => {
                        setSearchOpen(false)
                        setFile({ id: document.fileId, title: document.name })
                      }}
                    >
                      <Files size={20} />
                      <span>
                        <strong>{document.name}</strong>
                        <small>School resource</small>
                      </span>
                      <ArrowRight size={16} />
                    </button>
                  ))}
                </section>
              )}
              {!matches && (
                <EmptyState
                  title="Nothing found just yet"
                  description="Try another student name, group, or research title."
                />
              )}
            </div>
          </DialogContent>
        </Dialog>
        <Dialog open={helpOpen} onOpenChange={setHelpOpen}>
          <DialogContent className="help-dialog">
            <DialogHeader>
              <span className="dialog-feature-icon">
                <BookOpenText size={28} />
              </span>
              <DialogTitle>A little guide to your teacher workspace.</DialogTitle>
              <DialogDescription>
                CREST helps students and teachers find, submit, review, and preserve school
                research.
              </DialogDescription>
            </DialogHeader>
            <div className="help-steps">
              <div>
                <span>01</span>
                <section>
                  <h3>See the whole classroom</h3>
                  <p>
                    Use Overview for a quick pulse. Research groups and Students show who is
                    preparing, awaiting review, revising, or approved.
                  </p>
                </section>
              </div>
              <div>
                <span>02</span>
                <section>
                  <h3>Give every group a next step</h3>
                  <p>
                    Open a submission to read the PDF. Approve it for the library or request changes
                    with clear feedback.
                  </p>
                </section>
              </div>
              <div>
                <span>03</span>
                <section>
                  <h3>Keep knowledge in reach</h3>
                  <p>
                    Add research to the library, share documents, and approve new accounts from the
                    sidebar.
                  </p>
                </section>
              </div>
            </div>
            {isDemo && (
              <div className="notice-box">
                <strong>You’re exploring a demo.</strong>
                <p>
                  These sample names, papers, and groups are fictional. Changes stay in this
                  browser. Sample manuscripts are not attached; upload a PDF to try the viewer.
                  Connecting the school’s Firebase and Appwrite settings enables the shared app
                  records.
                </p>
              </div>
            )}
            <p className="about-school">
              Compiled Research as an Educational Application for Students and Teachers
              <br />
              <strong>Bonifacio D. Borebor Sr. High School</strong>
            </p>
          </DialogContent>
        </Dialog>
      </div>
      <Toaster position="bottom-right" richColors closeButton />
    </TooltipProvider>
  )
}

export default App
