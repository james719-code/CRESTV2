import { helpVideoEmbedUrl } from './help-video'

// Public build settings only; importing these must not initialize a backend SDK.
const firebaseSettings = [
  import.meta.env.VITE_FIREBASE_API_KEY,
  import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  import.meta.env.VITE_FIREBASE_PROJECT_ID,
  import.meta.env.VITE_FIREBASE_APP_ID,
]

export const isDemo = firebaseSettings.every((value) => !value)
export const validConfig = firebaseSettings.every(Boolean)

// Optional walkthrough video shown on the Help page. Only approved video hosts are embedded.
export const helpVideoUrl = helpVideoEmbedUrl(import.meta.env.VITE_HELP_VIDEO_URL)
