import type { ReactNode } from 'react'
import {
  ArrowRight,
  Bell,
  BookOpenText,
  CheckCircle,
  ClipboardText,
  DeviceMobile,
  Eye,
  Files,
  GraduationCap,
  Info,
  MagnifyingGlass,
  Question,
  SlidersHorizontal,
  SquaresFour,
  Users,
  UsersThree,
  type Icon,
} from '@/icons'
import { Button } from '@/components/ui/button'
import { helpVideoUrl } from '@/data/config'
import type { Page } from '../domain'
import './help-page.css'

interface Section {
  id: string
  title: string
  icon: Icon
  summary: string
  steps: ReactNode[]
  tip?: ReactNode
  open?: { page: Page; label: string }
}

const sections: Section[] = [
  {
    id: 'research-planning',
    title: 'Deadlines and milestones',
    icon: ClipboardText,
    summary: 'Plan proposal, chapter, defense, and final manuscript deadlines for each group.',
    steps: [
      <>
        Choose <strong>Deadlines & milestones</strong>, then <strong>Add milestone</strong>. Select
        a group, enter a title, choose the stage, and set a deadline.
      </>,
      <>
        Deadlines run through the end of the selected day in Manila time. Unfinished work becomes{' '}
        <strong>Overdue</strong> the next day.
      </>,
      <>
        Filter by group, stage, or status. Choose <strong>Mark complete</strong> after checking the
        work; use <strong>Reopen</strong> if more work is needed.
      </>,
      <>
        Each group’s details also show its milestones. The overview highlights the nearest
        deadlines.
      </>,
    ],
    open: { page: 'milestones', label: 'Open deadlines & milestones' },
  },
  {
    id: 'notification-inbox',
    title: 'Your notification inbox',
    icon: Bell,
    summary:
      'Keep submissions, research approvals, revision feedback, and pending registrations in one place.',
    steps: [
      <>
        Select the <strong>bell</strong> or <strong>Notification inbox</strong> in the sidebar. Use
        the read-status and update-type filters to find an update.
      </>,
      <>
        Choose <strong>View group</strong> or <strong>View accounts</strong> to open the related
        record. Opening an update marks it read.
      </>,
      <>
        Use <strong>Mark unread</strong> to return later, or <strong>Mark all as read</strong> to
        clear your unread count. Other teachers keep their own read status.
      </>,
      <>
        The inbox captures current school updates when the workspace connects. The time shown is
        when an update was added to the inbox. Browser push permission is not required.
      </>,
    ],
    open: { page: 'inbox', label: 'Open notification inbox' },
  },
  {
    id: 'getting-around',
    title: 'Finding your way around',
    icon: SquaresFour,
    summary: 'Everything you need is one click away. Here is what each part of the screen does.',
    steps: [
      <>
        The <strong>sidebar</strong> on the left lists every page. A number beside Submission
        reviews or Account approvals means something is waiting for you.
      </>,
      <>
        <strong>Search your workspace</strong> at the top finds groups, students, research and
        documents. Press <kbd>Ctrl</kbd> + <kbd>K</kbd> (or <kbd>⌘</kbd> + <kbd>K</kbd> on a Mac)
        from any page.
      </>,
      <>
        The <strong>bell</strong> opens your notification inbox. Its count shows unread submissions,
        research approvals, revision feedback, and pending registrations. Read status belongs to
        your account.
      </>,
      <>
        Your <strong>profile menu</strong> (top right) opens About CREST and lets you sign out.
      </>,
      <>
        <strong>Export progress</strong> downloads a spreadsheet file with each student’s account
        status, group and research status, ready for a check-in or a report.
      </>,
    ],
    tip: 'Using a phone or tablet? Tap the menu button at the top left to open the sidebar.',
  },
  {
    id: 'overview',
    title: 'Overview: your classroom at a glance',
    icon: SquaresFour,
    summary: 'Start every day here for a quick read on what needs you.',
    steps: [
      <>
        The four <strong>number cards</strong> show students, research groups, papers pending review
        and approved research. Select a card to jump to that list.
      </>,
      <>
        <strong>Research progress</strong> shows how many groups are approved, waiting for review,
        making changes or still preparing.
      </>,
      <>
        <strong>Needs your attention</strong> lists the next best actions: papers to review, groups
        making changes and accounts waiting for approval.
      </>,
      <>
        <strong>Research group progress</strong> is a short table of groups. Choose{' '}
        <strong>Review paper</strong> or <strong>View group</strong> to open one.
      </>,
    ],
    open: { page: 'overview', label: 'Open Overview' },
  },
  {
    id: 'groups',
    title: 'Research groups and their status',
    icon: UsersThree,
    summary: 'See who is preparing, waiting for you, revising or finished.',
    steps: [
      <>
        Open <strong>Research groups</strong> in the sidebar. Use the tabs to filter by status and
        the search box to find a group, student or research title. The strand filter narrows the
        list further.
      </>,
      <>
        Choose a group to see its <strong>research title, members, progress steps</strong> and any
        feedback you have already given.
      </>,
      <>
        Use the <strong>Group & research</strong> heading to change the sort order. Groups that need
        you are labelled <strong>Review paper</strong>.
      </>,
    ],
    tip: (
      <>
        <span className="help-legend">
          <span className="status-badge status-preparing">
            <span className="status-dot" />
            Preparing research
          </span>
          <span className="status-badge status-pending">
            <span className="status-dot" />
            Pending review
          </span>
          <span className="status-badge status-changes">
            <span className="status-dot" />
            Changes requested
          </span>
          <span className="status-badge status-approved">
            <span className="status-dot" />
            Approved
          </span>
        </span>
        These four statuses follow a group’s journey from first draft to the school library.
      </>
    ),
    open: { page: 'groups', label: 'Open Research groups' },
  },
  {
    id: 'reviews',
    title: 'Reviewing a submitted paper',
    icon: ClipboardText,
    summary: 'Read the manuscript, then approve it or send clear feedback.',
    steps: [
      <>
        Open <strong>Submission reviews</strong>. Only papers waiting for you appear here.
      </>,
      <>
        Choose a group, then open the <strong>research PDF</strong> and read it. A review is only
        available once a manuscript has been submitted.
      </>,
      <>
        Type your comments in <strong>Feedback for the group</strong>. Be specific about what works
        and what needs attention. Feedback is required when you ask for changes.
      </>,
      <>
        Choose <strong>Request changes</strong> to send the paper back, or{' '}
        <strong>Approve research</strong> to add it to the shared library. Approval asks you to
        confirm first.
      </>,
    ],
    tip: 'After you request changes, the group revises and resubmits. The paper returns to your review list when it is ready again.',
    open: { page: 'reviews', label: 'Open Submission reviews' },
  },
  {
    id: 'students',
    title: 'Managing students',
    icon: GraduationCap,
    summary: 'Check one student at a time and control who can use the app.',
    steps: [
      <>
        Open <strong>Students</strong> and search for a name. Each row shows the student’s strand,
        group and research status.
      </>,
      <>
        Select a student to open their record. From there you can{' '}
        <strong>approve an account</strong>, <strong>decline a registration</strong>,{' '}
        <strong>revoke access</strong>, or <strong>restore access</strong> that was declined.
      </>,
      <>
        A declined registration can also be <strong>deleted</strong>. Group memberships are cleared
        when you do this.
      </>,
    ],
    open: { page: 'students', label: 'Open Students' },
  },
  {
    id: 'accounts',
    title: 'Approving accounts',
    icon: Users,
    summary: 'Welcome new students and fellow teachers.',
    steps: [
      <>
        Open <strong>Account approvals</strong> to see student and teacher registrations that are
        waiting.
      </>,
      <>
        Choose <strong>Approve account</strong> beside a name you recognise. Approved students can
        create or join a research group in the CREST app.
      </>,
      <>New teachers also appear here. Only approve people you know from your school.</>,
    ],
    open: { page: 'accounts', label: 'Open Account approvals' },
  },
  {
    id: 'library',
    title: 'The research library and documents',
    icon: BookOpenText,
    summary: 'Keep finished work and helpful resources where everyone can find them.',
    steps: [
      <>
        <strong>Research library</strong> holds approved student papers and research you add. Select
        a paper to read it.
      </>,
      <>
        Choose <strong>Add research</strong> to upload a PDF with its title, strand and type, so it
        is available to the school.
      </>,
      <>
        <strong>Documents</strong> is for templates, rubrics and guides. Choose{' '}
        <strong>Share document</strong> to upload one for students and teachers.
      </>,
    ],
    tip: 'Deleting a group never removes research that has already been approved. Published papers stay in the library.',
    open: { page: 'research', label: 'Open Research library' },
  },
  {
    id: 'comfort',
    title: 'Making CREST comfortable to use',
    icon: SlidersHorizontal,
    summary: 'Change how the site looks and reads. Every feature stays available.',
    steps: [
      <>
        Open <strong>Settings</strong>, or choose <strong>Display</strong> anywhere it appears.
      </>,
      <>
        Pick <strong>Standard</strong> for a compact layout or <strong>Easy reading</strong> for
        larger text, bigger buttons and a short guide on every page.
      </>,
      <>
        Choose a <strong>Light</strong> or <strong>Dark</strong> theme, or let it follow your
        device.
      </>,
    ],
    open: { page: 'settings', label: 'Open Settings' },
  },
]

const quickStart = [
  'Sign in with your approved school Google account.',
  'Look at Overview to see what needs you today.',
  'Approve your students in Account approvals so they can join the app.',
  'Open a submitted paper in Submission reviews and give feedback.',
  'Add a paper or document to the library for the next researchers.',
]

const faq: { question: string; answer: ReactNode }[] = [
  {
    question: 'I signed in but it says teacher access is required.',
    answer:
      'Your Google account does not have an approved teacher profile yet. Ask an approved teacher to review your registration under Account approvals, then sign in again.',
  },
  {
    question: 'A student says they cannot create or join a group.',
    answer:
      'Check Account approvals or Students. A student can use the app fully only after you approve their account. If the account was declined, open the record and restore access.',
  },
  {
    question: 'Where did a submitted paper go after I requested changes?',
    answer:
      'It stays with the group under Changes requested. When the group resubmits, the paper returns to Submission reviews.',
  },
  {
    question: 'The PDF is taking a while to open.',
    answer:
      'Large manuscripts can take a moment on a slow connection. Papers you have opened before load faster the next time.',
  },
  {
    question: 'Can I undo deleting a group?',
    answer:
      'No. Deleting a group removes its submission record and feedback, and the page asks you to confirm first. Approved research already in the library is not removed.',
  },
]

function jump(id: string) {
  const reduce =
    window.matchMedia('(prefers-reduced-motion: reduce)').matches ||
    document.documentElement.dataset.reduceMotion === 'true'
  document
    .getElementById(id)
    ?.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'start' })
}

export function HelpPage({
  isDemo,
  onNavigate,
}: {
  isDemo: boolean
  onNavigate: (page: Page) => void
}) {
  const contents = [
    { id: 'quick-start', title: 'Quick start' },
    ...(helpVideoUrl ? [{ id: 'video', title: 'Watch the walkthrough' }] : []),
    ...sections.map(({ id, title }) => ({ id, title })),
    { id: 'faq', title: 'Common questions' },
  ]
  return (
    <div className="help-layout">
      <nav className="help-contents" aria-label="Tutorial contents">
        <p>On this page</p>
        <ul>
          {contents.map((item) => (
            <li key={item.id}>
              <button onClick={() => jump(item.id)}>{item.title}</button>
            </li>
          ))}
        </ul>
      </nav>
      <div className="help-content">
        <section id="quick-start" className="help-card help-intro">
          <div className="help-card-heading">
            <span className="help-icon">
              <CheckCircle size={24} />
            </span>
            <div>
              <h2>Your first five minutes</h2>
              <p>
                CREST helps students and teachers find, submit, review and preserve school research.
                Follow these steps once and you are set.
              </p>
            </div>
          </div>
          <ol className="help-steps">
            {quickStart.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
          {isDemo && (
            <div className="help-tip">
              <Info size={18} />
              <p>
                <strong>You are exploring a demo.</strong> The names, papers and groups are
                fictional and changes stay in this browser. Sample manuscripts are not attached, so
                upload a PDF to try the reader.
              </p>
            </div>
          )}
          <div className="help-student-note">
            <DeviceMobile size={20} />
            <p>
              <strong>Students use the Android app.</strong> They create or join a group and submit
              their manuscript there. You guide them from this website.
            </p>
          </div>
        </section>

        {helpVideoUrl && (
          <section id="video" className="help-card">
            <div className="help-card-heading">
              <span className="help-icon">
                <Eye size={24} />
              </span>
              <div>
                <h2>Watch the walkthrough</h2>
                <p>A short video tour of the teacher workspace.</p>
              </div>
            </div>
            <div className="help-video">
              <iframe
                src={helpVideoUrl}
                title="CREST teacher workspace walkthrough"
                loading="lazy"
                allow="fullscreen; picture-in-picture"
                allowFullScreen
                referrerPolicy="strict-origin-when-cross-origin"
                sandbox="allow-scripts allow-same-origin allow-presentation allow-popups"
              />
            </div>
          </section>
        )}

        {sections.map((section) => (
          <section key={section.id} id={section.id} className="help-card">
            <div className="help-card-heading">
              <span className="help-icon">
                <section.icon size={24} />
              </span>
              <div>
                <h2>{section.title}</h2>
                <p>{section.summary}</p>
              </div>
            </div>
            <ol className="help-steps">
              {section.steps.map((step, index) => (
                <li key={index}>{step}</li>
              ))}
            </ol>
            {section.tip && (
              <div className="help-tip">
                <Info size={18} />
                <p>{section.tip}</p>
              </div>
            )}
            {section.open && (
              <Button variant="outline" onClick={() => onNavigate(section.open!.page)}>
                {section.open.label}
                <ArrowRight size={17} />
              </Button>
            )}
          </section>
        ))}

        <section id="faq" className="help-card">
          <div className="help-card-heading">
            <span className="help-icon">
              <Question size={24} />
            </span>
            <div>
              <h2>Common questions</h2>
              <p>Quick answers to things teachers often ask.</p>
            </div>
          </div>
          <div className="help-faq">
            {faq.map((item) => (
              <details key={item.question}>
                <summary>{item.question}</summary>
                <p>{item.answer}</p>
              </details>
            ))}
          </div>
        </section>

        <section className="help-card help-shortcuts">
          <div className="help-card-heading">
            <span className="help-icon">
              <MagnifyingGlass size={24} />
            </span>
            <div>
              <h2>Handy shortcuts</h2>
            </div>
          </div>
          <ul>
            <li>
              <span>
                <kbd>Ctrl</kbd> + <kbd>K</kbd>
              </span>
              Search your workspace
            </li>
            <li>
              <span>
                <Bell size={16} />
              </span>
              See what needs your attention
            </li>
            <li>
              <span>
                <Files size={16} />
              </span>
              Share templates and rubrics in Documents
            </li>
          </ul>
        </section>
      </div>
    </div>
  )
}
