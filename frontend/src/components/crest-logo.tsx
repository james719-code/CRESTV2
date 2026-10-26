import { cn } from '@/lib/utils'

export function CrestLogo({
  className,
  size = 48,
  decorative = false,
}: {
  className?: string
  size?: number
  decorative?: boolean
}) {
  return (
    <img
      src="/crest-logo.svg"
      width={size}
      height={size}
      className={cn('crest-logo', className)}
      alt={decorative ? '' : 'CREST logo'}
    />
  )
}
