export type ThemePreference = 'light' | 'dark' | 'system'
export type TextSize = 'standard' | 'large' | 'extra-large'
export type ReadingLayout = 'standard' | 'easy-reading'
export interface DisplayPreferences {
  theme: ThemePreference
  textSize: TextSize
  layout: ReadingLayout
  strongContrast: boolean
  reduceMotion: boolean
  guidance: boolean
  hintDismissed: boolean
}

export const preferencesKey = 'crest-display-preferences-v1'
export const defaultPreferences: DisplayPreferences = {
  theme: 'system',
  textSize: 'standard',
  layout: 'standard',
  strongContrast: false,
  reduceMotion: false,
  guidance: false,
  hintDismissed: false,
}

export function parsePreferences(value: unknown): DisplayPreferences {
  const data =
    value && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : {}
  return {
    theme:
      typeof data.theme === 'string' && ['light', 'dark', 'system'].includes(data.theme)
        ? (data.theme as ThemePreference)
        : 'system',
    textSize:
      typeof data.textSize === 'string' &&
      ['standard', 'large', 'extra-large'].includes(data.textSize)
        ? (data.textSize as TextSize)
        : 'standard',
    layout: data.layout === 'easy-reading' ? 'easy-reading' : 'standard',
    strongContrast: data.strongContrast === true,
    reduceMotion: data.reduceMotion === true,
    guidance: data.guidance === true,
    hintDismissed: data.hintDismissed === true,
  }
}

export function withReadingPreset(
  current: DisplayPreferences,
  layout: ReadingLayout,
): DisplayPreferences {
  return {
    ...current,
    layout,
    textSize: layout === 'easy-reading' ? 'large' : 'standard',
    strongContrast: layout === 'easy-reading',
    reduceMotion: layout === 'easy-reading',
    guidance: layout === 'easy-reading',
    hintDismissed: true,
  }
}

export function resolveTheme(preference: ThemePreference, systemDark: boolean): 'light' | 'dark' {
  return preference === 'system' ? (systemDark ? 'dark' : 'light') : preference
}

export function loadPreferences(): DisplayPreferences {
  try {
    return parsePreferences(JSON.parse(localStorage.getItem(preferencesKey) || 'null'))
  } catch {
    return { ...defaultPreferences }
  }
}

export function applyPreferences(
  preferences: DisplayPreferences,
  systemDark: boolean,
  systemReducedMotion: boolean,
) {
  const root = document.documentElement
  const theme = resolveTheme(preferences.theme, systemDark)
  root.dataset.theme = theme
  root.classList.toggle('dark', theme === 'dark')
  root.dataset.reading = preferences.layout
  root.dataset.textSize = preferences.textSize
  root.dataset.contrast = preferences.strongContrast ? 'strong' : 'standard'
  root.dataset.reduceMotion = preferences.reduceMotion || systemReducedMotion ? 'true' : 'false'
  // Percentages preserve the browser's own font-size preference and zoom.
  root.style.fontSize = { standard: '100%', large: '112.5%', 'extra-large': '125%' }[
    preferences.textSize
  ]
}

export function initializeAppearance() {
  applyPreferences(
    loadPreferences(),
    matchMedia('(prefers-color-scheme: dark)').matches,
    matchMedia('(prefers-reduced-motion: reduce)').matches,
  )
}
