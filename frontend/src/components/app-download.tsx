import { useEffect, useState } from 'react'
import {
  ArrowUpRight,
  BookOpenText,
  CheckCircle,
  DeviceMobile,
  DownloadSimple,
  UsersThree,
} from '@/icons'
import { Button } from '@/components/ui/button'
import { CrestLogo } from './crest-logo'
import {
  androidDownloadUrl,
  androidReleasesUrl,
  fetchAppRelease,
  type PublishedAppRelease,
} from '@/data/app-release'

export function AppDownload() {
  const [release, setRelease] = useState<PublishedAppRelease | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    const timeout = window.setTimeout(() => controller.abort(), 8000)
    async function loadRelease() {
      const published = await fetchAppRelease(controller.signal)
      window.clearTimeout(timeout)
      if (active) {
        setRelease(published)
        setLoading(false)
      }
    }
    void loadRelease()
    return () => {
      active = false
      window.clearTimeout(timeout)
      controller.abort()
    }
  }, [])

  return (
    <section
      id="download-app"
      className="landing-download landing-container"
      aria-labelledby="download-title"
    >
      <div className="landing-download-copy">
        <span className="landing-eyebrow">FOR THE RESEARCH DAY AHEAD</span>
        <h2 id="download-title">
          Take your school library
          <br />
          with you.
        </h2>
        <p>
          Download the CREST app here. Find a starting point for your research, keep useful papers
          close, and bring your group’s work together on your Android phone.
        </p>
        <ul className="landing-app-features">
          <li>
            <BookOpenText size={20} />
            <span>Browse and read your school’s research papers.</span>
          </li>
          <li>
            <CheckCircle size={20} />
            <span>Save useful research to your favorites.</span>
          </li>
          <li>
            <UsersThree size={20} />
            <span>Join a research group and submit your manuscript.</span>
          </li>
        </ul>
        <p className="landing-platform-note">
          On a computer? Teachers can sign in to the workspace above.
        </p>
      </div>
      <div className="landing-download-panel">
        <div className="landing-app-identity">
          <CrestLogo size={76} decorative />
          <div>
            <h3>CREST for Android</h3>
            <span>School research, shared.</span>
          </div>
        </div>
        <div className="landing-release-info" role="status" aria-live="polite">
          {loading ? (
            <>
              <span className="landing-release-skeleton" aria-hidden="true" />
              <span>Checking the latest release…</span>
            </>
          ) : release ? (
            <>
              <strong>Version {release.version}</strong>
              <span>
                Latest published Android release
                {release.sizeBytes ? ` · ${(release.sizeBytes / 1024 / 1024).toFixed(1)} MB` : ''}
              </span>
            </>
          ) : (
            <>
              <strong>Get the latest Android release</strong>
              <span>Check GitHub for the current version and release details.</span>
            </>
          )}
        </div>
        <Button size="lg" asChild>
          <a href={release?.downloadUrl ?? androidDownloadUrl}>
            <DownloadSimple size={20} />
            Download APK
          </a>
        </Button>
        <div className="landing-apk-details">
          <DeviceMobile size={16} />
          Android 7.0 or later · Free to download
        </div>
        <a
          className="landing-release-link"
          href={release?.notesUrl ?? androidReleasesUrl}
          target="_blank"
          rel="noopener noreferrer"
        >
          Release notes on GitHub <ArrowUpRight size={16} />
          <span className="sr-only"> (opens in a new tab)</span>
        </a>
        <div className="landing-install-guide">
          <h4>Installing for the first time?</h4>
          <ol>
            <li>Download the APK on your Android phone.</li>
            <li>Open the file and allow your browser to install this app if Android asks.</li>
            <li>Install CREST, then sign in with your school account.</li>
          </ol>
          <p>Already using CREST? Install the update over your existing app.</p>
        </div>
      </div>
    </section>
  )
}
