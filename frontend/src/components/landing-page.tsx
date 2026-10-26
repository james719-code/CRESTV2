import {
  ArrowRight,
  ArrowUpRight,
  BookOpenText,
  Check,
  CheckCircle,
  ClipboardText,
  Clock,
  DeviceMobile,
  FileText,
  GraduationCap,
  Heart,
  ShieldCheck,
  UsersThree,
} from '@/icons'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { CrestLogo } from './crest-logo'
import { DisplaySettings } from './display-settings'
import './landing-page.css'

interface LandingPageProps {
  isDemo: boolean
  signedIn: boolean
  onSignIn: () => void
  onWorkspace: () => void
}

function scrollToSection(id: string) {
  const behavior =
    window.matchMedia('(prefers-reduced-motion: reduce)').matches ||
    document.documentElement.dataset.reduceMotion === 'true'
      ? 'auto'
      : 'smooth'
  document.getElementById(id)?.scrollIntoView({ behavior, block: 'start' })
}

export function LandingPage({ isDemo, signedIn, onSignIn, onWorkspace }: LandingPageProps) {
  const enter = signedIn ? onWorkspace : onSignIn
  const enterLabel = signedIn ? 'Open workspace' : 'Teacher sign in'
  return (
    <div className="landing-page">
      <a
        className="skip-link"
        href="#landing-content"
        onClick={(event) => {
          event.preventDefault()
          document.getElementById('landing-content')?.focus()
        }}
      >
        Skip to content
      </a>
      <header className="landing-header">
        <div className="landing-container landing-nav">
          <a href="#home" className="landing-brand" aria-label="CREST home">
            <CrestLogo size={48} decorative />
            <span>
              <span className="wordmark">CREST</span>
              <span>School research, shared.</span>
            </span>
          </a>
          <nav aria-label="Website navigation">
            <button onClick={() => scrollToSection('for-teachers')}>For teachers</button>
            <button onClick={() => scrollToSection('how-it-works')}>How it works</button>
            <button onClick={() => scrollToSection('the-project')}>The project</button>
          </nav>
          <div className="landing-header-actions">
            <DisplaySettings />
            <Button onClick={enter}>
              {enterLabel}
              <ArrowUpRight size={17} />
            </Button>
          </div>
        </div>
      </header>
      <main id="landing-content" tabIndex={-1}>
        <section className="landing-hero landing-container">
          <div className="landing-hero-copy">
            <Badge variant="outline" className="landing-kicker">
              <span />
              MADE FOR YOUR SCHOOL COMMUNITY
            </Badge>
            <h1>
              Good research grows
              <br />
              with <span>good guidance.</span>
            </h1>
            <p>
              Bring your students’ research into focus. Follow each group’s progress, give
              thoughtful feedback, and build a library your school can learn from.
            </p>
            <div className="landing-hero-actions">
              <Button size="lg" onClick={enter}>
                {enterLabel}
                <ArrowRight size={18} />
              </Button>
              {isDemo && (
                <Button variant="outline" size="lg" onClick={onWorkspace}>
                  Explore the teacher demo
                  <ArrowUpRight size={18} />
                </Button>
              )}
            </div>
            <div className="landing-school">
              <GraduationCap size={22} />
              <span>
                Made for students and teachers at
                <br />
                <strong>Bonifacio D. Borebor Sr. High School</strong>
              </span>
            </div>
            <div className="landing-hero-credit">
              <Heart size={16} />
              <span>
                Built by <strong>James Ryan S. Gallego</strong> · Maintained for{' '}
                <strong>3+ years</strong>
              </span>
            </div>
          </div>
          <figure
            className="landing-preview"
            aria-label="Illustrative teacher workspace preview with fictional sample records"
          >
            <div className="preview-window-bar">
              <div>
                <i />
                <i />
                <i />
              </div>
              <span>
                <ShieldCheck size={13} />
                CREST · Teacher workspace
              </span>
              <span className="preview-window-avatar">AR</span>
            </div>
            <div className="preview-workspace">
              <div className="preview-heading">
                <div>
                  <p>YOUR RESEARCH CLASSROOM</p>
                  <h2>A clear view. A useful next step.</h2>
                </div>
                <CrestLogo size={42} decorative />
              </div>
              <div className="preview-metrics">
                <div>
                  <span>
                    <UsersThree size={17} />
                    Research groups
                  </span>
                  <strong>09</strong>
                </div>
                <div>
                  <span>
                    <ClipboardText size={17} />
                    Pending review
                  </span>
                  <strong>03</strong>
                </div>
                <div>
                  <span>
                    <CheckCircle size={17} />
                    Approved
                  </span>
                  <strong>03</strong>
                </div>
              </div>
              <div className="preview-progress">
                <div>
                  <strong>Research progress</strong>
                  <span>3 of 9 groups approved</span>
                </div>
                <div className="preview-progress-bar">
                  <span />
                  <span />
                  <span />
                  <span />
                </div>
                <div className="preview-legend">
                  <span>
                    <i />
                    Preparing
                  </span>
                  <span>
                    <i />
                    In review
                  </span>
                  <span>
                    <i />
                    Revising
                  </span>
                  <span>
                    <i />
                    Approved
                  </span>
                </div>
              </div>
              <div className="preview-table-heading">
                <strong>Your research groups</strong>
                <span>At a glance</span>
              </div>
              <div className="preview-group">
                <span className="preview-group-icon">
                  <UsersThree size={19} />
                </span>
                <div>
                  <strong>The Green Collective</strong>
                  <span>STEM · Sustainable water filtration</span>
                </div>
                <span className="preview-status review">Pending review</span>
              </div>
              <div className="preview-group">
                <span className="preview-group-icon teal">
                  <UsersThree size={19} />
                </span>
                <div>
                  <strong>Tomorrow Lab</strong>
                  <span>STEM · School rainwater collection</span>
                </div>
                <span className="preview-status approved">
                  <Check size={12} />
                  Approved
                </span>
              </div>
              <div className="preview-feedback">
                <ChatBubble />
                <div>
                  <strong>A little feedback goes a long way.</strong>
                  <p>Give every group a clear next step.</p>
                </div>
                <CheckCircle size={22} />
              </div>
            </div>
            <figcaption>
              <span />
              Workspace preview · Fictional sample records
            </figcaption>
          </figure>
        </section>
        <div className="landing-connection-strip">
          <div className="landing-container">
            <span>
              <DeviceMobile size={20} />
              Students research on the app.
            </span>
            <ArrowRight size={18} />
            <span>
              <ClipboardText size={20} />
              Teachers guide on the web.
            </span>
            <ArrowRight size={18} />
            <span>
              <BookOpenText size={20} />
              Knowledge stays with the school.
            </span>
          </div>
        </div>
        <section id="for-teachers" className="landing-section landing-container">
          <div className="landing-section-heading">
            <span className="landing-eyebrow">A LITTLE CLARITY FOR YOUR CLASSROOM</span>
            <h2>
              Less searching.
              <br />
              More time for your students.
            </h2>
            <p>Your teacher workspace brings the people, papers, and next steps together.</p>
          </div>
          <div className="landing-feature-grid">
            <article className="landing-feature feature-progress">
              <span className="landing-feature-icon">
                <UsersThree size={26} />
              </span>
              <span className="landing-feature-number">01 / FOLLOW THE JOURNEY</span>
              <h3>See where every group stands.</h3>
              <p>
                Find your students, see their research groups, and know who is preparing, waiting
                for review, making changes, or ready for the library.
              </p>
              <div className="feature-statuses">
                <span>Preparing</span>
                <ArrowRight size={13} />
                <span>In review</span>
                <ArrowRight size={13} />
                <span>
                  <Check size={13} />
                  Approved
                </span>
              </div>
            </article>
            <article className="landing-feature">
              <span className="landing-feature-icon teal">
                <ClipboardText size={26} />
              </span>
              <span className="landing-feature-number">02 / GUIDE THE NEXT STEP</span>
              <h3>Make your feedback count.</h3>
              <p>
                Read a submitted manuscript, ask for specific changes, or approve the finished
                research. Keep the group’s feedback close to the work.
              </p>
              <div className="feature-footer">
                <CheckCircle size={17} />A clear next step for every submission
              </div>
            </article>
            <article className="landing-feature">
              <span className="landing-feature-icon teal">
                <BookOpenText size={26} />
              </span>
              <span className="landing-feature-number">03 / KEEP KNOWLEDGE IN REACH</span>
              <h3>Build a library that lasts.</h3>
              <p>
                Preserve approved papers and share the templates, rubrics, and guides that give the
                next researchers a good place to start.
              </p>
              <div className="feature-footer">
                <FileText size={17} />
                Research papers, templates, and school resources
              </div>
            </article>
            <article className="landing-feature feature-community">
              <span className="landing-feature-icon">
                <GraduationCap size={26} />
              </span>
              <span className="landing-feature-number">04 / BRING THE CLASSROOM TOGETHER</span>
              <h3>A shared space for school research.</h3>
              <p>
                Welcome student accounts, search across your workspace, and export a view of student
                progress when it’s time to check in.
              </p>
              <div className="feature-footer">
                <ShieldCheck size={17} />A workspace for approved teachers
              </div>
            </article>
          </div>
        </section>
        <section id="how-it-works" className="landing-how-section">
          <div className="landing-container">
            <div className="landing-section-heading">
              <span className="landing-eyebrow">ONE CONNECTED RESEARCH JOURNEY</span>
              <h2>
                From a first idea
                <br />
                to something worth sharing.
              </h2>
              <p>The Android app and teacher website each have a part to play.</p>
            </div>
            <div className="landing-steps">
              <article>
                <span>01</span>
                <div className="step-platform">
                  <DeviceMobile size={16} />
                  IN THE STUDENT APP
                </div>
                <h3>
                  Work together.
                  <br />
                  Submit when ready.
                </h3>
                <p>
                  Students create or join a research group and submit their manuscript through
                  CREST.
                </p>
              </article>
              <article>
                <span>02</span>
                <div className="step-platform">
                  <ClipboardText size={16} />
                  IN YOUR TEACHER WORKSPACE
                </div>
                <h3>
                  Read thoughtfully.
                  <br />
                  Guide the next revision.
                </h3>
                <p>
                  Teachers review the PDF and share clear feedback, or approve a paper for the
                  school library.
                </p>
              </article>
              <article>
                <span>03</span>
                <div className="step-platform">
                  <BookOpenText size={16} />
                  IN THE SCHOOL LIBRARY
                </div>
                <h3>
                  Keep the work.
                  <br />
                  Pass the knowledge on.
                </h3>
                <p>
                  Approved research becomes a reference for the next students finding their own
                  questions.
                </p>
              </article>
            </div>
          </div>
        </section>
        <section id="the-project" className="landing-project landing-container">
          <div className="project-identity">
            <CrestLogo size={100} />
            <span className="landing-eyebrow">THE PEOPLE BEHIND THE PROJECT</span>
            <h2>
              A school project.
              <br />A long-term commitment.
            </h2>
            <p>
              CREST was built by <strong>James Ryan S. Gallego</strong> and has been maintained for{' '}
              <strong>3+ years</strong>.
            </p>
            <p>
              It brings school research into one shared space—so students can find a starting point,
              teachers can guide the work, and good ideas stay within reach.
            </p>
            <div className="project-full-name">
              Compiled Research as an Educational Application for Students and Teachers
            </div>
          </div>
          <div className="project-credit">
            <span className="project-credit-label">
              <Clock size={18} />
              BUILT WITH CARE. MAINTAINED OVER TIME.
            </span>
            <div className="project-years">
              3<span>+</span>
              <small>years of continued maintenance</small>
            </div>
            <div className="project-maintainer">
              <span className="maintainer-monogram">JG</span>
              <div>
                <strong>James Ryan S. Gallego</strong>
                <span>Creator & maintainer</span>
              </div>
              <Heart size={24} />
            </div>
            <p>
              Made for a real school community,
              <br />
              with a commitment that continues.
            </p>
          </div>
        </section>
        <section className="landing-final">
          <div className="landing-container">
            <div>
              <span className="landing-eyebrow">YOUR NEXT RESEARCH DAY STARTS HERE</span>
              <h2>
                A clearer view for you.
                <br />A better next step for your students.
              </h2>
            </div>
            <div>
              <Button size="lg" onClick={enter}>
                {enterLabel}
                <ArrowRight size={18} />
              </Button>
              {isDemo && (
                <button className="landing-demo-link" onClick={onWorkspace}>
                  Take a look at the demo
                  <ArrowUpRight size={16} />
                </button>
              )}
            </div>
          </div>
        </section>
      </main>
      <footer className="landing-footer landing-container">
        <a href="#home" className="landing-brand" aria-label="CREST home">
          <CrestLogo size={42} decorative />
          <span>
            <span className="wordmark">CREST</span>
            <span>School research, shared.</span>
          </span>
        </a>
        <p>
          Built by <strong>James Ryan S. Gallego</strong>
          <br />
          Maintained for <strong>3+ years</strong>
        </p>
        <span>Bonifacio D. Borebor Sr. High School</span>
      </footer>
    </div>
  )
}

function ChatBubble() {
  return (
    <span className="preview-feedback-icon">
      <ClipboardText size={21} />
    </span>
  )
}
