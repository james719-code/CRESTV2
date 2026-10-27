import { CrestLogo } from './crest-logo'

export function AboutPage() {
  return (
    <article className="about-page">
      <section className="about-introduction settings-panel">
        <CrestLogo size={80} decorative />
        <h2>School research, shared.</h2>
        <p>
          CREST stands for{' '}
          <strong>Compiled Research as an Educational Application for Students and Teachers</strong>
          . It brings our school’s research into one accessible library, connecting the work
          students create with the people who can learn from it.
        </p>
      </section>
      <section className="settings-panel school-profile">
        <img
          src="/bdbshs-school-logo.jpg"
          alt="Bonifacio D. Borebor Sr. High School seal"
          width="120"
          height="120"
        />
        <div>
          <h2>Bonifacio D. Borebor Sr. High School</h2>
          <p>
            Built for our school community, CREST gives students and teachers a shared space to
            develop research, exchange feedback, and preserve completed work for future learners.
          </p>
        </div>
      </section>
      <section className="settings-panel">
        <h2>Why we built CREST</h2>
        <p>
          Research should keep helping people after the final submission. When papers are kept in
          separate files or printed copies, useful ideas can be difficult to find and valuable
          student work can be overlooked. We built CREST to make school research easier to discover,
          organize, and preserve.
        </p>
        <p>
          Students can form groups and submit manuscripts. Teachers can review submissions, give
          clear feedback, and approve completed research for the library. Having these steps
          together helps every group understand what comes next.
        </p>
      </section>
      <section className="settings-panel">
        <h2>A resource for the next researchers</h2>
        <p>
          The library helps learners explore previous studies, understand how research is presented,
          and build on ideas with proper acknowledgment of the original authors. Shared templates,
          rubrics, and school documents keep useful guidance close at hand.
        </p>
        <p>
          Through the Android app and teacher website, CREST supports the same goal: make research a
          lasting contribution to our school community.
        </p>
      </section>
      <section className="settings-panel">
        <h2>The people behind CREST</h2>
        <p>
          <strong>James Ryan S. Gallego</strong>
          <br />
          Lead developer and proponent
        </p>
        <h3>Co-researchers</h3>
        <p>
          Ralph Windel Azana
          <br />
          Grant Gabriel Versoza
          <br />
          Herald Ian Lejarde
          <br />
          Uriel Sapian
        </p>
      </section>
    </article>
  )
}
