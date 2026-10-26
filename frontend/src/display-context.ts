import { createContext, useContext } from 'react'
import type { DisplayPreferences, ReadingLayout } from './preferences'

export interface DisplayContextValue {
  preferences: DisplayPreferences
  resolvedTheme: 'light' | 'dark'
  storageAvailable: boolean
  update: (changes: Partial<DisplayPreferences>) => void
  preset: (layout: ReadingLayout) => void
  reset: () => void
}
export const DisplayContext = createContext<DisplayContextValue | null>(null)
export const ReadingLayoutContext = createContext<ReadingLayout>('standard')

export function useDisplayPreferences() {
  const context = useContext(DisplayContext)
  if (!context) throw new Error('Display preferences require DisplayProvider.')
  return context
}
