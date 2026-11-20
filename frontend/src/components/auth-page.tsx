import { useState, type FormEvent } from 'react'
import {
  ArrowLeft,
  ArrowRight,
  BookOpenText,
  CheckCircle,
  GoogleLogo,
  GraduationCap,
  UsersThree,
} from '@/icons'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { CrestLogo } from './crest-logo'
import { DisplaySettings } from './display-settings'
import type { AccountInfo, SessionState } from '../data/session'

export function AuthPage({
  isDemo,
  validConfig,
  session,
  error,
  signingIn,
  authError,
  account,
  onRegister,
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
  account: AccountInfo | null
  onRegister: (name: string) => Promise<void>
  onLogin: () => void
  onRetry: () => void
  onLogout: () => void
  onHome: () => void
  onDemo: () => void
}) {
  const setup = isDemo || !validConfig
  // null until the person edits it, so the Google name is the starting value.
  const [typedName, setName] = useState<string | null>(null)
  const [registering, setRegistering] = useState(false)
  const [registerError, setRegisterError] = useState('')
  const name = typedName ?? account?.name ?? ''
  const live = !setup
  const unregistered = live && session === 'unregistered'
  const pending = live && session === 'pending'
  const studentAccount = live && session === 'student'
  const signedOut = live && !unregistered && !pending && !studentAccount && session !== 'error'

  async function register(event: FormEvent) {
    event.preventDefault()
    if (!name.trim()) {
      setRegisterError('Enter your full name.')
      return
    }
    setRegistering(true)
    setRegisterError('')
    try {
      await onRegister(name)
    } catch (error) {
      setRegisterError(
        error instanceof Error && error.message
          ? error.message
          : 'Your registration could not be saved. Check your connection and try again.',
      )
    } finally {
      setRegistering(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-page-toolbar">
        <button className="auth-home-link" onClick={onHome}>
          <ArrowLeft size={17} />
          Back to CREST
        </button>
        <DisplaySettings />
      </div>
      <div className="auth-layout">
        <div className="auth-intro">
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
          <div className="auth-benefits">
            <h2>Your school’s research, in one workspace</h2>
            <ul>
              <li>
                <BookOpenText size={22} aria-hidden="true" />
                <span>
                  <strong>Review submissions</strong>
                  Read each group’s paper, approve it, or send feedback.
                </span>
              </li>
              <li>
                <UsersThree size={22} aria-hidden="true" />
                <span>
                  <strong>Follow student progress</strong>
                  See every group, strand, and milestone at a glance.
                </span>
              </li>
              <li>
                <CheckCircle size={22} aria-hidden="true" />
                <span>
                  <strong>Approve accounts</strong>
                  Welcome new students and teachers to the library.
                </span>
              </li>
            </ul>
          </div>
        </div>
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
                <span className="eyebrow">CREST for teachers</span>
                <h1>
                  {unregistered
                    ? 'Finish your teacher registration.'
                    : pending
                      ? 'Waiting for approval.'
                      : studentAccount
                        ? 'This is a student account.'
                        : session === 'error' && !setup
                          ? 'Let’s reconnect your workspace.'
                          : 'Sign in to your teacher workspace.'}
                </h1>
                <p>
                  {setup
                    ? isDemo
                      ? 'School sign-in is being set up for this website. You can explore the teacher workspace with fictional sample records in the demo.'
                      : 'School sign-in is being set up for this website. Please try again once the school connection is ready.'
                    : unregistered
                      ? 'Add your name to request access. A teacher reviews every registration, and your Google email is used automatically.'
                      : pending
                        ? 'A teacher needs to approve your registration. This page opens your workspace by itself once you are approved.'
                        : studentAccount
                          ? 'The website is for teachers. Open the CREST app to use your student account, or switch to a teacher account.'
                          : session === 'error'
                            ? error
                            : 'Sign in with your school Google account. New teachers can register right after signing in.'}
                </p>
                {authError && (
                  <p className="inline-error" role="alert">
                    {authError}
                  </p>
                )}
                {(unregistered || pending || studentAccount) && account && (
                  <div className="auth-account">
                    <span className="auth-account-who">
                      {account.name && <strong>{account.name}</strong>}
                      <small>{account.email}</small>
                    </span>
                    <button type="button" className="auth-account-switch" onClick={onLogout}>
                      Switch
                    </button>
                  </div>
                )}
                {unregistered && (
                  <form className="auth-register" onSubmit={register} noValidate>
                    <Label htmlFor="teacher-name">Full name</Label>
                    <Input
                      id="teacher-name"
                      value={name}
                      onChange={(event) => {
                        setName(event.target.value)
                        setRegisterError('')
                      }}
                      autoComplete="name"
                      maxLength={200}
                      aria-invalid={registerError ? true : undefined}
                      aria-describedby={registerError ? 'teacher-name-error' : undefined}
                      disabled={registering}
                      required
                    />
                    {registerError && (
                      <p className="inline-error" id="teacher-name-error" role="alert">
                        {registerError}
                      </p>
                    )}
                    <Button type="submit" disabled={registering}>
                      {registering ? 'Submitting…' : 'Submit for approval'}
                      <ArrowRight size={17} />
                    </Button>
                  </form>
                )}
                {live &&
                  (session === 'error' ? (
                    <Button onClick={onRetry}>
                      Try again
                      <ArrowRight size={17} />
                    </Button>
                  ) : studentAccount ? (
                    <Button onClick={onLogout}>
                      Use a teacher account
                      <ArrowRight size={17} />
                    </Button>
                  ) : signedOut ? (
                    <Button onClick={onLogin} disabled={signingIn}>
                      <GoogleLogo size={19} />
                      {signingIn ? 'Signing in…' : 'Continue with Google'}
                      <ArrowRight size={17} />
                    </Button>
                  ) : null)}
                {signedOut && (
                  <section className="auth-steps" aria-labelledby="auth-steps-title">
                    <h2 id="auth-steps-title">New teacher? Register in three steps</h2>
                    <ol>
                      <li>
                        <span>Continue with your Google account.</span>
                      </li>
                      <li>
                        <span>Add your name. Your Google email is used automatically.</span>
                      </li>
                      <li>
                        <span>A teacher approves you, and this page opens your workspace.</span>
                      </li>
                    </ol>
                  </section>
                )}
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
      </div>
      <p className="auth-project-credit">
        Built by <strong>James Ryan S. Gallego</strong> · Maintained for <strong>3+ years</strong>
      </p>
    </div>
  )
}
