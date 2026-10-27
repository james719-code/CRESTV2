import {
  ArrowUpRight,
  BookOpenText,
  ClipboardText,
  Clock,
  DeviceMobile,
  FileText,
  GraduationCap,
  MagnifyingGlass,
  Monitor,
  ShieldCheck,
} from '@/icons'
import { Button } from '@/components/ui/button'
import { credits, interLicensePath, noticesPath, type CreditGroup } from '@/data/credits'
import { CrestLogo } from './crest-logo'
import './about-page.css'

const values = [
  {
    icon: MagnifyingGlass,
    title: 'Easy to discover',
    text: 'Past studies live in one searchable library instead of scattered files and printed copies.',
  },
  {
    icon: ClipboardText,
    title: 'Clear next steps',
    text: 'Groups submit a manuscript and teachers answer with focused feedback or an approval.',
  },
  {
    icon: BookOpenText,
    title: 'Kept for others',
    text: 'Approved research stays available so the next researchers have a good place to start.',
  },
]

const journey = [
  {
    icon: DeviceMobile,
    place: 'In the Android app',
    title: 'Students research',
    text: 'Create or join a group, read the library and submit a manuscript.',
  },
  {
    icon: Monitor,
    place: 'On this website',
    title: 'Teachers guide',
    text: 'Read each paper, share feedback and approve finished work.',
  },
  {
    icon: GraduationCap,
    place: 'In the school library',
    title: 'Knowledge stays',
    text: 'Approved papers and resources are preserved for future learners.',
  },
]

const coResearchers = [
  'Ralph Windel Azana',
  'Grant Gabriel Versoza',
  'Herald Ian Lejarde',
  'Uriel Sapian',
]
const groups: CreditGroup[] = ['Typeface', 'Interface', 'Documents and sign-in']

function monogram(name: string) {
  const words = name.split(' ').filter(Boolean)
  return `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase()
}

export function AboutPage() {
  return (
    <article className="about-modern">
      <section className="about-hero">
        <div className="about-hero-copy">
          <span className="about-eyebrow">WHAT CREST IS</span>
          <h2>School research, shared.</h2>
          <p>
            CREST stands for{' '}
            <strong>
              Compiled Research as an Educational Application for Students and Teachers
            </strong>
            . It brings our school’s research into one accessible library and connects the work
            students create with the people who can learn from it.
          </p>
          <ul className="about-facts">
            <li>
              <ShieldCheck size={20} />
              <span>
                <strong>Android app + website</strong>
                One shared library
              </span>
            </li>
            <li>
              <Clock size={20} />
              <span>
                <strong>3+ years</strong>
                Of continued maintenance
              </span>
            </li>
            <li>
              <GraduationCap size={20} />
              <span>
                <strong>Made for our school</strong>
                Students and teachers
              </span>
            </li>
          </ul>
        </div>
        <div className="about-hero-mark">
          <CrestLogo size={120} decorative />
          <div className="about-school">
            <img
              src="/bdbshs-school-logo.jpg"
              alt="Bonifacio D. Borebor Sr. High School seal"
              width="56"
              height="56"
            />
            <span>
              Bonifacio D. Borebor
              <br />
              Sr. High School
            </span>
          </div>
        </div>
      </section>

      <section className="about-section">
        <div className="about-heading">
          <span className="about-eyebrow">WHY WE BUILT IT</span>
          <h2>Research should keep helping people after the final submission.</h2>
          <p>
            When papers sit in separate files or printed copies, useful ideas are hard to find and
            valuable student work can be overlooked. CREST makes school research easier to discover,
            organize and preserve, with proper acknowledgment of the original authors.
          </p>
        </div>
        <div className="about-values">
          {values.map((item) => (
            <div key={item.title}>
              <span className="about-icon">
                <item.icon size={24} />
              </span>
              <h3>{item.title}</h3>
              <p>{item.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="about-section">
        <div className="about-heading">
          <span className="about-eyebrow">HOW IT FITS TOGETHER</span>
          <h2>One journey, two places to work.</h2>
        </div>
        <ol className="about-journey">
          {journey.map((step, index) => (
            <li key={step.title}>
              <span className="about-step">{index + 1}</span>
              <span className="about-place">
                <step.icon size={16} />
                {step.place}
              </span>
              <h3>{step.title}</h3>
              <p>{step.text}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="about-section">
        <div className="about-heading">
          <span className="about-eyebrow">THE PEOPLE BEHIND CREST</span>
          <h2>Built by students, for students.</h2>
        </div>
        <div className="about-people">
          <div className="about-lead">
            <span className="about-avatar large">{monogram('James Ryan S. Gallego')}</span>
            <div>
              <strong>James Ryan S. Gallego</strong>
              <span>Lead developer and proponent</span>
            </div>
          </div>
          <div className="about-team">
            <h3>Co-researchers</h3>
            <ul>
              {coResearchers.map((name) => (
                <li key={name}>
                  <span className="about-avatar">{monogram(name)}</span>
                  {name}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      <section className="about-section" id="licenses">
        <div className="about-heading">
          <span className="about-eyebrow">LICENSES & CREDITS</span>
          <h2>Built on open-source work.</h2>
          <p>
            CREST relies on generous free software and a free typeface. Each project below is used
            under its own license, and we are grateful to the people who maintain them.
          </p>
        </div>
        <div className="about-credits">
          {groups.map((group) => (
            <div key={group}>
              <h3>{group}</h3>
              <ul>
                {credits
                  .filter((credit) => credit.group === group)
                  .map((credit) => (
                    <li key={credit.name}>
                      <span>
                        <strong>{credit.name}</strong>
                        <small>{credit.role}</small>
                      </span>
                      <span className="about-license">{credit.license}</span>
                      <a
                        href={credit.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        aria-label={`${credit.name} website (opens in a new tab)`}
                      >
                        Website
                        <ArrowUpRight size={15} />
                      </a>
                    </li>
                  ))}
              </ul>
            </div>
          ))}
        </div>
        <div className="about-license-actions">
          <Button asChild>
            <a href={noticesPath} target="_blank" rel="noopener noreferrer">
              <FileText size={18} />
              Read full license notices
            </a>
          </Button>
          <Button asChild variant="outline">
            <a href={interLicensePath} target="_blank" rel="noopener noreferrer">
              Inter font license
              <ArrowUpRight size={17} />
            </a>
          </Button>
        </div>
        <p className="about-fineprint">
          The CREST name and logo, and the school seal, are not covered by these open-source
          licenses. The seal belongs to Bonifacio D. Borebor Sr. High School.
        </p>
      </section>
    </article>
  )
}
