import { ArrowRight, Info, X } from '@/icons'
import { Button } from '@/components/ui/button'
import { useDisplayPreferences } from '../display-context'
import type { Page } from '../domain'

const guidance: Record<
  Exclude<Page, 'help'>,
  { title: string; steps: string[]; action: string; next: Page }
> = {
  overview: {
    title: 'Start with what needs your attention',
    steps: [
      'See your student and group totals.',
      'Open pending reviews to read submitted papers.',
      'Use the sidebar to find students or resources.',
    ],
    action: 'Open submission reviews',
    next: 'reviews',
  },
  groups: {
    title: 'Find a group, then open its research',
    steps: [
      'Search by a group name, student, or research title.',
      'Use a status or strand filter to narrow the list.',
      'Choose View group or Review paper for the full details.',
    ],
    action: 'See papers awaiting review',
    next: 'reviews',
  },
  students: {
    title: 'Check one student at a time',
    steps: [
      'Search for the student’s name.',
      'Check their group and research status.',
      'Open their record to approve, decline, restore access, or delete a declined registration.',
    ],
    action: 'Open research groups',
    next: 'groups',
  },
  reviews: {
    title: 'Review a paper in three clear steps',
    steps: [
      'Open a group and read its submitted PDF.',
      'Write specific feedback if changes are needed.',
      'Request changes, or approve the finished paper.',
    ],
    action: 'View the research library',
    next: 'research',
  },
  research: {
    title: 'Find and read a research paper',
    steps: [
      'Search by title, author, or strand.',
      'Choose Read paper to open the PDF.',
      'Use Add research to share a paper with the school.',
    ],
    action: 'Explore school documents',
    next: 'documents',
  },
  documents: {
    title: 'Find a useful resource for your students',
    steps: [
      'Search for a template, guide, or rubric.',
      'Choose Open document to read it.',
      'Use Share document to add your own PDF.',
    ],
    action: 'Go to the research library',
    next: 'research',
  },
  accounts: {
    title: 'Welcome a new student or teacher',
    steps: [
      'Check the name and role on each registration.',
      'Confirm it belongs to your school community.',
      'Approve correct accounts, or decline a student registration.',
    ],
    action: 'Manage students',
    next: 'students',
  },
  settings: {
    title: 'Make the workspace easier to read',
    steps: [
      'Choose a reading layout.',
      'Adjust the theme, text size, and contrast.',
      'Your choices are saved in this browser automatically.',
    ],
    action: 'Return to overview',
    next: 'overview',
  },
  about: {
    title: 'Get to know CREST',
    steps: [
      'Learn why we built the school research library.',
      'See how students and teachers work together.',
      'Meet the team behind the project.',
    ],
    action: 'Explore the research library',
    next: 'research',
  },
}

export function PageGuidance({ page, navigate }: { page: Page; navigate: (page: Page) => void }) {
  const { preferences, update, preset } = useDisplayPreferences()
  if (!preferences.guidance)
    return preferences.hintDismissed ? null : (
      <section className="reading-shortcut" aria-label="Reading comfort">
        <div>
          <strong>Prefer a little more room to read?</strong>
          <p>
            Easy reading gives you bigger text, larger controls, and a short guide on each page.
          </p>
        </div>
        <div>
          <Button variant="outline" onClick={() => preset('easy-reading')}>
            Try easy reading
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => update({ hintDismissed: true })}
            aria-label="Dismiss reading suggestion"
          >
            <X size={18} />
          </Button>
        </div>
      </section>
    )
  if (page === 'help') return null
  const guide = guidance[page]
  return (
    <section className="page-guidance" aria-label="Page guide">
      <div className="guide-title">
        <span>
          <Info size={21} />
          <h2>{guide.title}</h2>
        </span>
        <button onClick={() => update({ guidance: false })} aria-label="Hide page guidance">
          <span>Hide guide</span>
          <X size={18} />
        </button>
      </div>
      <ol>
        {guide.steps.map((step, index) => (
          <li key={step}>
            <span>{index + 1}</span>
            {step}
          </li>
        ))}
      </ol>
      <Button variant="link" onClick={() => navigate(guide.next)}>
        {guide.action}
        <ArrowRight size={17} />
      </Button>
    </section>
  )
}
