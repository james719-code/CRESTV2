import { ArrowLeft, ArrowRight, GoogleLogo, GraduationCap } from '@/icons'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { CrestLogo } from './crest-logo'
import { DisplaySettings } from './display-settings'
import type { SessionState } from '../data/firebase'

export function AuthPage({
  isDemo,
  validConfig,
  session,
  error,
  signingIn,
  authError,
  onLogin,
  onRetry,
  onLogout,
  onHome,
  onDemo,
}: {
  isDemo: boolean
  validConfig: boolean
  session: SessionState
  error: string
  signingIn: boolean
  authError: string
  onLogin: () => void
  onRetry: () => void
  onLogout: () => void
  onHome: () => void
  onDemo: () => void
}) {
  const setup = isDemo || !validConfig
  return (
    <div className="auth-page">
      <div className="auth-page-toolbar">
        <button className="auth-home-link" onClick={onHome}>
          <ArrowLeft size={17} />
          Back to CREST
        </button>
        <DisplaySettings />
      </div>
      <a
        href="#home"
        className="auth-brand"
        onClick={(event) => {
          event.preventDefault()
          onHome()
        }}
      >
        <CrestLogo size={76} decorative />
        <span className="wordmark">CREST</span>
        <span>School research, shared.</span>
      </a>
      <Card className="auth-card">
        <CardContent>
          {session === 'loading' && validConfig && !isDemo ? (
            <div className="auth-loading" role="status">
              <Skeleton className="h-12 w-12 rounded-xl" />
              <Skeleton className="h-7 w-3/4" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-2/3" />
              <p>Opening your teacher workspace…</p>
            </div>
          ) : (
            <>
              <span className="eyebrow">CREST FOR TEACHERS</span>
              <h1>
                {session === 'denied' && !setup
                  ? 'Teacher access is required.'
                  : session === 'error' && !setup
                    ? 'Let’s reconnect your workspace.'
                    : 'Welcome to your teacher workspace.'}
              </h1>
              <p>
                {setup
                  ? isDemo
                    ? 'School sign-in is being set up for this website. You can explore the teacher workspace with fictional sample records in the demo.'
                    : 'School sign-in is being set up for this website. Please try again once the school connection is ready.'
                  : session === 'denied'
                    ? 'This Google account does not have an approved teacher profile. Ask an approved teacher to review your registration.'
                    : session === 'error'
                      ? error
                      : 'Sign in with your approved school Google account to follow student progress and review research.'}
              </p>
              {authError && (
                <p className="inline-error" role="alert">
                  {authError}
                </p>
              )}
              {!setup &&
                (session === 'error' ? (
                  <Button onClick={onRetry}>
                    Try again
                    <ArrowRight size={17} />
                  </Button>
                ) : session === 'denied' ? (
                  <Button onClick={onLogout}>
                    Use another account
                    <ArrowRight size={17} />
                  </Button>
                ) : (
                  <Button onClick={onLogin} disabled={signingIn}>
                    <GoogleLogo size={19} />
                    {signingIn ? 'Signing in…' : 'Continue with Google'}
                    <ArrowRight size={17} />
                  </Button>
                ))}
              {isDemo && (
                <Button onClick={onDemo}>
                  Explore the teacher demo
                  <ArrowRight size={17} />
                </Button>
              )}
              {!validConfig && !isDemo && (
                <p className="auth-setup-note">
                  The school connection is not ready yet. Please try again once setup is complete.
                </p>
              )}
              <div className="auth-school">
                <GraduationCap size={20} />
                <span>
                  Bonifacio D. Borebor Sr. High School
                  <br />
                  <small>Teacher workspace</small>
                </span>
              </div>
            </>
          )}
        </CardContent>
      </Card>
      <p className="auth-project-credit">
        Built by <strong>James Ryan S. Gallego</strong> · Maintained for <strong>3+ years</strong>
      </p>
    </div>
  )
}
