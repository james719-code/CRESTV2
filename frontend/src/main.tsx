import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { DisplayProvider } from './components/display-provider'
import { initializeAppearance } from './preferences'
import './appearance.css'
import './polish.css'
import './modern.css'

initializeAppearance()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <DisplayProvider>
      <App />
    </DisplayProvider>
  </StrictMode>,
)
