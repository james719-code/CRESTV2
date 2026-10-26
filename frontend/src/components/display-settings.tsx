import { useId, useState } from 'react'
import {
  ArrowCounterClockwise,
  Check,
  Eye,
  Monitor,
  Moon,
  SlidersHorizontal,
  Sun,
  TextAa,
} from '@/icons'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { useDisplayPreferences } from '../display-context'
import type { ReadingLayout, TextSize, ThemePreference } from '../preferences'

export function DisplaySettings({ className = '' }: { className?: string }) {
  const [open, setOpen] = useState(false)
  const id = useId()
  const { preferences, storageAvailable, update, preset, reset } = useDisplayPreferences()
  const themes = [
    { value: 'light', title: 'Light', icon: Sun },
    { value: 'dark', title: 'Dark', icon: Moon },
    { value: 'system', title: 'Use device', icon: Monitor },
  ] as const
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button
          variant="outline"
          className={`display-trigger ${className}`}
          aria-label="Display and reading settings"
        >
          <SlidersHorizontal size={20} />
          <span>Display</span>
        </Button>
      </DialogTrigger>
      <DialogContent className="display-dialog">
        <DialogHeader>
          <span className="display-dialog-icon">
            <TextAa size={26} />
          </span>
          <DialogTitle>Make CREST comfortable for you</DialogTitle>
          <DialogDescription>
            Choose how you like to read and work. Every feature stays available in either layout.
          </DialogDescription>
        </DialogHeader>
        <div className="display-options">
          <fieldset className="display-fieldset">
            <legend>Start with a layout</legend>
            <div className="reading-presets">
              {(
                [
                  {
                    value: 'standard',
                    title: 'Standard',
                    description: 'A tidy layout for everyday work.',
                    icon: Monitor,
                  },
                  {
                    value: 'easy-reading',
                    title: 'Easy reading',
                    description: 'Bigger text, more space, and helpful steps.',
                    icon: Eye,
                  },
                ] as const
              ).map((option) => (
                <label className="reading-preset" key={option.value}>
                  <input
                    type="radio"
                    name={`${id}-layout`}
                    value={option.value}
                    checked={preferences.layout === option.value}
                    onChange={() => preset(option.value as ReadingLayout)}
                  />
                  <span>
                    <option.icon size={24} />
                    <strong>{option.title}</strong>
                    <small>{option.description}</small>
                    <Check size={17} className="preference-selected" />
                  </span>
                </label>
              ))}
            </div>
          </fieldset>
          <fieldset className="display-fieldset">
            <legend>Theme</legend>
            <div className="theme-choices">
              {themes.map((option) => (
                <label className="display-choice" key={option.value}>
                  <input
                    type="radio"
                    name={`${id}-theme`}
                    value={option.value}
                    checked={preferences.theme === option.value}
                    onChange={() => update({ theme: option.value as ThemePreference })}
                  />
                  <span>
                    <option.icon size={21} />
                    {option.title}
                    <Check size={14} className="preference-selected" />
                  </span>
                </label>
              ))}
            </div>
          </fieldset>
          <fieldset className="display-fieldset">
            <legend>Text size</legend>
            <div className="text-size-choices">
              {(
                [
                  { value: 'standard', title: 'Standard' },
                  { value: 'large', title: 'Large' },
                  { value: 'extra-large', title: 'Extra large' },
                ] as const
              ).map((option) => (
                <label className="display-choice" key={option.value}>
                  <input
                    type="radio"
                    name={`${id}-text`}
                    value={option.value}
                    checked={preferences.textSize === option.value}
                    onChange={() => update({ textSize: option.value as TextSize })}
                  />
                  <span>
                    {option.title}
                    <Check size={14} className="preference-selected" />
                  </span>
                </label>
              ))}
            </div>
          </fieldset>
          <div className="display-toggles">
            <label>
              <span>
                <strong>Stronger contrast</strong>
                <small>Make text and control borders more distinct.</small>
              </span>
              <input
                type="checkbox"
                checked={preferences.strongContrast}
                onChange={(event) => update({ strongContrast: event.target.checked })}
              />
            </label>
            <label>
              <span>
                <strong>Less motion</strong>
                <small>
                  Minimize movement. Your device’s accessibility setting is always respected.
                </small>
              </span>
              <input
                type="checkbox"
                checked={preferences.reduceMotion}
                onChange={(event) => update({ reduceMotion: event.target.checked })}
              />
            </label>
            <label>
              <span>
                <strong>Show guidance</strong>
                <small>Keep a short next-step guide on each workspace page.</small>
              </span>
              <input
                type="checkbox"
                checked={preferences.guidance}
                onChange={(event) => update({ guidance: event.target.checked })}
              />
            </label>
          </div>
          <p className="display-save-note" role="status">
            {storageAvailable
              ? 'Your choices are remembered in this browser for your next visit.'
              : 'Your choices apply now. This browser is not allowing them to be saved.'}
          </p>
        </div>
        <div className="display-dialog-footer">
          <Button variant="ghost" onClick={reset}>
            <ArrowCounterClockwise size={17} />
            Reset display
          </Button>
          <Button onClick={() => setOpen(false)}>
            Done
            <Check size={17} />
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
