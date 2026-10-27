import { Activity, lazy, Suspense, useEffect, useState } from 'react'
import { LandingPage } from './components/landing-page'
import { isDemo } from './data/config'
import { parseRoute, type Route } from './routing'
import './App.css'
import './shared.css'

const TeacherApp = lazy(() => import('./TeacherApp'))

export default function App() {
  const [route, setRoute] = useState(() => parseRoute(location.hash))
  const [signedIn, setSignedIn] = useState(false)
  const [teacherOpened, setTeacherOpened] = useState(() => route !== 'home')

  useEffect(() => {
    const onHashChange = () => {
      const next = parseRoute(location.hash)
      setRoute(next)
      if (next !== 'home') setTeacherOpened(true)
      window.scrollTo(0, 0)
    }
    window.addEventListener('hashchange', onHashChange)
    return () => window.removeEventListener('hashchange', onHashChange)
  }, [])

  useEffect(() => {
    if (route === 'home') document.title = 'CREST · School research, shared.'
  }, [route])

  function navigate(next: Route) {
    location.hash = next
    setRoute(next)
    if (next !== 'home') setTeacherOpened(true)
    window.scrollTo(0, 0)
  }

  return (
    <>
      {route === 'home' && (
        <LandingPage
          isDemo={isDemo}
          signedIn={signedIn}
          onSignIn={() => navigate('signin')}
          onWorkspace={() => navigate('overview')}
        />
      )}
      {teacherOpened && (
        <Activity mode={route === 'home' ? 'hidden' : 'visible'}>
          <Suspense
            fallback={
              <div className="content-loading" role="status">
                Loading your workspace…
              </div>
            }
          >
            <TeacherApp routeName={route} onNavigate={navigate} onSessionChange={setSignedIn} />
          </Suspense>
        </Activity>
      )}
    </>
  )
}
