import { useEffect, useState, type ReactNode } from 'react'
import { DisplayContext, ReadingLayoutContext } from '../display-context'
import {
  applyPreferences,
  defaultPreferences,
  loadPreferences,
  parsePreferences,
  preferencesKey,
  resolveTheme,
  withReadingPreset,
  type DisplayPreferences,
  type ReadingLayout,
} from '../preferences'

export function DisplayProvider({ children }: { children: ReactNode }) {
  const [preferences, setPreferences] = useState(loadPreferences)
  const [systemDark, setSystemDark] = useState(
    () => matchMedia('(prefers-color-scheme: dark)').matches,
  )
  const [systemMotion, setSystemMotion] = useState(
    () => matchMedia('(prefers-reduced-motion: reduce)').matches,
  )
  const [storageAvailable, setStorageAvailable] = useState(true)

  useEffect(() => {
    const dark = matchMedia('(prefers-color-scheme: dark)')
    const motion = matchMedia('(prefers-reduced-motion: reduce)')
    const onDark = () => setSystemDark(dark.matches)
    const onMotion = () => setSystemMotion(motion.matches)
    const onStorage = (event: StorageEvent) => {
      if (event.key === preferencesKey || event.key === null) setPreferences(loadPreferences())
    }
    dark.addEventListener('change', onDark)
    motion.addEventListener('change', onMotion)
    window.addEventListener('storage', onStorage)
    return () => {
      dark.removeEventListener('change', onDark)
      motion.removeEventListener('change', onMotion)
      window.removeEventListener('storage', onStorage)
    }
  }, [])

  useEffect(() => {
    applyPreferences(preferences, systemDark, systemMotion)
  }, [preferences, systemDark, systemMotion])

  function save(next: DisplayPreferences) {
    const checked = parsePreferences(next)
    setPreferences(checked)
    try {
      localStorage.setItem(preferencesKey, JSON.stringify(checked))
      setStorageAvailable(true)
    } catch {
      setStorageAvailable(false)
    }
  }
  function update(changes: Partial<DisplayPreferences>) {
    save({ ...preferences, ...changes })
  }
  function preset(layout: ReadingLayout) {
    save(withReadingPreset(preferences, layout))
  }
  return (
    <DisplayContext
      value={{
        preferences,
        resolvedTheme: resolveTheme(preferences.theme, systemDark),
        storageAvailable,
        update,
        preset,
        reset: () => save({ ...defaultPreferences }),
      }}
    >
      <ReadingLayoutContext value={preferences.layout}>{children}</ReadingLayoutContext>
    </DisplayContext>
  )
}
