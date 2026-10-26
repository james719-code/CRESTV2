import {
  CheckCircle as CircleCheckIcon,
  Info as InfoIcon,
  SpinnerGap as Loader2Icon,
  XCircle as OctagonXIcon,
  Warning as TriangleAlertIcon,
} from '@/icons'
import { Toaster as Sonner, type ToasterProps } from 'sonner'
import { useDisplayPreferences } from '../../display-context'

const Toaster = ({ ...props }: ToasterProps) => {
  const { resolvedTheme } = useDisplayPreferences()
  return (
    <Sonner
      theme={resolvedTheme}
      className="toaster group"
      icons={{
        success: <CircleCheckIcon className="size-4" />,
        info: <InfoIcon className="size-4" />,
        warning: <TriangleAlertIcon className="size-4" />,
        error: <OctagonXIcon className="size-4" />,
        loading: <Loader2Icon className="size-4 animate-spin" />,
      }}
      style={
        {
          '--normal-bg': 'var(--popover)',
          '--normal-text': 'var(--popover-foreground)',
          '--normal-border': 'var(--border)',
          '--border-radius': 'var(--radius)',
        } as React.CSSProperties
      }
      {...props}
    />
  )
}

export { Toaster }
