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
  SlidersHorizontal,
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
import { useWorkspace } from './data/useWorkspace'
import { AuthPage } from './components/auth-page'
import { CrestLogo } from './components/crest-logo'
import { DisplaySettings } from './components/display-settings'
import { NotificationSettings } from './components/notification-settings'
import { notificationsEnabled } from './data/edge'
import { PageGuidance } from './components/page-guidance'
import {
  groupRoute,
  routeGroupId,
  readerRoute,
  routeReader,
  isWorkspaceRoute,
  parseRoute,
  type ReaderTarget,
  type Route,
} from './routing'
import { readerBackRoute, resolveReaderPaper } from './data/reader-paper'
import { AboutPage } from './components/about-page'
import {
  groupStatus,
  initials,
  statusLabels,
  studentGroup,
  studentAccountLabels,
  studentAccountStatus,
  toCsv,
  type Page,
  type Student,
} from './domain'
import './card-designs.css'
import './workspace-polish.css'
import './workspace-modern.css'

const Overview = lazy(() => import('./components/overview').then((m) => ({ default: m.Overview })))
const ResearchLibrary = lazy(() =>
  import('./components/library').then((m) => ({ default: m.ResearchLibrary })),
)
const DocumentLibrary = lazy(() =>
  import('./components/library').then((m) => ({ default: m.DocumentLibrary })),
)
const Accounts = lazy(() => import('./components/library').then((m) => ({ default: m.Accounts })))
const GroupDetails = lazy(() =>
  import('./components/workspace-dialogs').then((m) => ({ default: m.GroupDetails })),
)
const StudentDetails = lazy(() =>
  import('./components/workspace-dialogs').then((m) => ({ default: m.StudentDetails })),
)
const UploadDialog = lazy(() =>
  import('./components/workspace-dialogs').then((m) => ({ default: m.UploadDialog })),
)
const HelpPage = lazy(() => import('./components/help-page').then((m) => ({ default: m.HelpPage })))
const PaperReader = lazy(() => import('./components/paper-reader'))
const Milestones = lazy(() =>
  import('./components/milestones').then((m) => ({ default: m.Milestones })),
)
const MilestoneSummary = lazy(() =>
  import('./components/milestones').then((m) => ({ default: m.MilestoneSummary })),
)
const NotificationInbox = lazy(() =>
  import('./components/notification-inbox').then((m) => ({ default: m.NotificationInbox })),
)

function LoadingContent() {
  return (
    <div className="content-loading" role="status">
      Loading your workspace…
    </div>
  )
}

const navigation: {
  page: Page
  label: string
  icon: Icon
  section: 'classroom' | 'library' | 'personal'
}[] = [
  { page: 'overview', label: 'Overview', icon: SquaresFour, section: 'classroom' },
  { page: 'groups', label: 'Research groups', icon: UsersThree, section: 'classroom' },
  { page: 'students', label: 'Students', icon: GraduationCap, section: 'classroom' },
  { page: 'reviews', label: 'Submission reviews', icon: ClipboardText, section: 'classroom' },
  {
    page: 'milestones',
    label: 'Deadlines & milestones',
    icon: ClipboardText,
    section: 'classroom',
  },
  { page: 'research', label: 'Research library', icon: BookOpenText, section: 'library' },
  { page: 'documents', label: 'Documents', icon: Files, section: 'library' },
  { page: 'accounts', label: 'Account approvals', icon: Users, section: 'library' },
  { page: 'help', label: 'Help & tutorial', icon: Question, section: 'personal' },
  { page: 'inbox', label: 'Notification inbox', icon: Bell, section: 'personal' },
  { page: 'settings', label: 'Settings', icon: SlidersHorizontal, section: 'personal' },
  { page: 'about', label: 'About CREST', icon: Info, section: 'personal' },
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
    title: 'Student management',
    description: 'Approve or decline students, manage access, and follow research progress.',
    eyebrow: 'STUDENT MANAGEMENT',
  },
  reviews: {
    title: 'Submission reviews',
    description: 'Read manuscripts, guide revisions, and welcome new work to the library.',
    eyebrow: 'SUBMISSION REVIEWS',
  },
  milestones: {
    title: 'Deadlines & milestones',
    description: 'Plan proposals, chapters, defenses, and final manuscripts with clear due dates.',
    eyebrow: 'RESEARCH PLANNING',
  },
  inbox: {
    title: 'Notification inbox',
    description: 'Keep up with submissions, research approvals, and revision feedback.',
    eyebrow: 'YOUR SCHOOL UPDATES',
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
  settings: {
    title: 'Settings',
    description: 'Choose a display and reading style that feels comfortable on your device.',
    eyebrow: 'YOUR PREFERENCES',
  },
  help: {
    title: 'Help & tutorial',
    description: 'A step-by-step guide to everything in your teacher workspace.',
    eyebrow: 'LEARN THE WORKSPACE',
  },
  about: {
    title: 'About CREST',
    description: 'Built for the researchers of Bonifacio D. Borebor Sr. High School.',
    eyebrow: 'OUR PURPOSE & OUR SCHOOL',
  },
}

function TeacherApp({
  routeName,
  onNavigate,
  onSessionChange,
}: {
  routeName: Route
  onNavigate: (next: Route) => void
  onSessionChange: (signedIn: boolean) => void
}) {
  const service = useWorkspace()
  const { workspace, teacher, isDemo, session } = service
  const reading = routeReader(routeName)
  const paper = reading ? resolveReaderPaper(workspace, reading) : null
  const selectedGroup = routeGroupId(routeName)
  const page: Page = reading
    ? reading.source === 'groups'
      ? 'groups'
      : reading.source === 'documents'
        ? 'documents'
        : 'research'
    : selectedGroup
      ? 'groups'
      : isWorkspaceRoute(routeName)
        ? (routeName as Page)
        : 'overview'
  const [mobileNav, setMobileNav] = useState(false)
  const navigationRef = useRef<HTMLElement>(null)
  const wasNavigationOpen = useRef(false)
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null)
  const [uploadKind, setUploadKind] = useState<'research' | 'document' | null>(null)
  const [searchOpen, setSearchOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [signingIn, setSigningIn] = useState(false)
  const [authError, setAuthError] = useState('')
  const pending = workspace.groups.filter((group) => groupStatus(group) === 'pending')
  const unreadNotifications = (workspace.notifications || []).filter((item) => !item.read).length
  const pendingAccounts =
    workspace.students.filter((student) => studentAccountStatus(student) === 'pending').length +
    workspace.teachers.filter((item) => !item.access).length
  const currentNav = navigation.find((item) => item.page === page)!

  useEffect(() => {
    onSessionChange(!isDemo && session === 'ready')
  }, [isDemo, session, onSessionChange])

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
      setMobileNav(false)
      setSearchOpen(false)
      setSelectedStudent(null)
      setUploadKind(null)
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
          : reading
            ? `${paper?.title || 'Paper reader'} · CREST`
            : `${currentNav.label} · CREST Teacher workspace`
  }, [currentNav.label, routeName, reading, paper?.title])

  useEffect(() => {
    if (routeName === 'signin' && !isDemo && session === 'ready') location.hash = 'overview'
  }, [routeName, isDemo, session])

  function navigate(next: Route) {
    onNavigate(next)
    setMobileNav(false)
    window.scrollTo(0, 0)
  }

  function openGroup(id: string) {
    setSelectedStudent(null)
    setSearchOpen(false)
    navigate(groupRoute(id))
  }

  function openFile(target: ReaderTarget) {
    setSelectedStudent(null)
    setSearchOpen(false)
    navigate(readerRoute(target))
  }

  function deleteGroup(id: string) {
    const deletion = service.deleteGroup(id)
    navigate('groups')
    void deletion
      .then(() => toast.success('Group deleted. Its members can create or join another group.'))
      .catch(() => {
        // The group card keeps the failure and its retry action visible.
      })
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
        studentAccountLabels[studentAccountStatus(student)],
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

  if (reading) {
    const scope = JSON.stringify([
      isDemo ? 'demo' : import.meta.env.VITE_FIREBASE_PROJECT_ID,
      import.meta.env.VITE_SUPABASE_URL || import.meta.env.VITE_APPWRITE_PROJECT_ID || '',
      teacher?.id || 'demo',
    ])
    return (
      <TooltipProvider>
        <Suspense fallback={<LoadingContent />}>
          <PaperReader
            key={JSON.stringify([scope, paper?.fileId, routeName])}
            paper={paper}
            scope={scope}
            readFile={service.readFile}
            isDemo={isDemo}
            backLabel={
              reading.source === 'groups'
                ? 'Back to group'
                : reading.source === 'documents'
                  ? 'Back to documents'
                  : 'Back to library'
            }
            onBack={() => navigate(readerBackRoute(reading))}
          />
        </Suspense>
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
            {(['classroom', 'library', 'personal'] as const).map((section) => (
              <div className="nav-section" key={section}>
                <p>
                  {section === 'classroom'
                    ? 'YOUR CLASSROOM'
                    : section === 'library'
                      ? 'SCHOOL LIBRARY'
                      : 'PREFERENCES & ABOUT'}
                </p>
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
                      {item.page === 'inbox' && unreadNotifications > 0 && (
                        <span className="nav-count">{unreadNotifications}</span>
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
              <button onClick={() => navigate('help')}>
                Your teacher’s guide
                <ArrowRight size={14} />
              </button>
            </div>
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
                  <Button
                    variant="ghost"
                    size="icon"
                    className="notification-button"
                    aria-label={`Notification inbox, ${unreadNotifications} unread`}
                    onClick={() => navigate('inbox')}
                  >
                    <Bell size={21} />
                    {unreadNotifications > 0 && (
                      <strong className="notification-unread-count">
                        {unreadNotifications > 99 ? '99+' : unreadNotifications}
                      </strong>
                    )}
                  </Button>
                </TooltipTrigger>
                <TooltipContent>Notification inbox</TooltipContent>
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
                  <DropdownMenuItem onSelect={() => navigate('about')}>
                    <Info size={17} />
                    About CREST
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
                  <h1>
                    {selectedGroup
                      ? workspace.groups.find((group) => group.id === selectedGroup)?.name ||
                        'Group details'
                      : descriptions[page].title}
                  </h1>
                  <p>
                    {selectedGroup
                      ? 'Thesis, members, submission progress, and teacher feedback.'
                      : descriptions[page].description}
                  </p>
                </div>
                <div className="page-heading-actions">
                  {!selectedGroup &&
                    ['overview', 'groups', 'reviews', 'students'].includes(page) && (
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
              {!selectedGroup && <PageGuidance page={page} navigate={navigate} />}
              {isDemo && (
                <div className="demo-banner">
                  <span>
                    <Info size={17} />
                    <strong>Demo workspace</strong>
                    <span>Explore with sample records. Changes are saved in this browser.</span>
                  </span>
                  <button onClick={() => navigate('help')}>
                    About this demo
                    <ArrowRight size={15} />
                  </button>
                </div>
              )}
              <Suspense fallback={<LoadingContent />}>
                {page === 'overview' && (
                  <MilestoneSummary
                    workspace={workspace}
                    onOpen={() => navigate('milestones')}
                    onGroup={openGroup}
                  />
                )}
                {page === 'overview' && (
                  <Overview
                    workspace={workspace}
                    onNavigate={navigate}
                    onGroup={openGroup}
                    onFile={openFile}
                  />
                )}
                {page === 'groups' && !selectedGroup && (
                  <GroupTable
                    workspace={workspace}
                    onOpen={openGroup}
                    deletions={service.groupDeletions}
                    onRetryDelete={deleteGroup}
                  />
                )}
                {selectedGroup && (
                  <GroupDetails
                    key={selectedGroup}
                    group={workspace.groups.find((group) => group.id === selectedGroup)}
                    workspace={workspace}
                    onClose={() => navigate('groups')}
                    onReview={service.review}
                    onFile={openFile}
                    onDelete={deleteGroup}
                    onSaveMilestone={service.saveMilestone}
                    onChangeMilestone={service.changeMilestone}
                  />
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
                    <GroupTable
                      workspace={workspace}
                      onOpen={openGroup}
                      reviewsOnly
                      deletions={service.groupDeletions}
                      onRetryDelete={deleteGroup}
                    />
                  </>
                )}
                {page === 'students' && (
                  <StudentTable workspace={workspace} onOpen={setSelectedStudent} />
                )}
                {page === 'research' && <ResearchLibrary workspace={workspace} onFile={openFile} />}
                {page === 'milestones' && (
                  <Milestones
                    workspace={workspace}
                    onSave={service.saveMilestone}
                    onChange={service.changeMilestone}
                  />
                )}
                {page === 'inbox' && (
                  <NotificationInbox
                    workspace={workspace}
                    onRead={service.markNotifications}
                    navigate={navigate}
                    error={service.inboxError}
                    onRetry={service.retryInbox}
                  />
                )}
                {page === 'documents' && (
                  <DocumentLibrary workspace={workspace} onFile={openFile} />
                )}
                {page === 'accounts' && (
                  <Accounts
                    workspace={workspace}
                    onApprove={service.approveAccount}
                    onReviewStudent={service.reviewStudent}
                  />
                )}
                {page === 'settings' && (
                  <div className="settings-page">
                    <DisplaySettings inline />
                    {!isDemo && notificationsEnabled && (
                      <section className="settings-panel">
                        <h2>School notifications</h2>
                        <NotificationSettings />
                      </section>
                    )}
                  </div>
                )}
                {page === 'about' && <AboutPage />}
                {page === 'help' && <HelpPage isDemo={isDemo} onNavigate={navigate} />}
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
          {selectedStudent && (
            <StudentDetails
              student={
                workspace.students.find((student) => student.id === selectedStudent.id) || null
              }
              workspace={workspace}
              onClose={() => setSelectedStudent(null)}
              onGroup={openGroup}
              onReviewStudent={service.reviewStudent}
              onDeleteStudent={service.deleteStudent}
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
                        openGroup(group.id)
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
                        openFile({ source: paper.type, id: paper.id })
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
                        openFile({ source: 'documents', id: document.id })
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
      </div>
      <Toaster position="bottom-right" richColors closeButton />
    </TooltipProvider>
  )
}

export default TeacherApp
