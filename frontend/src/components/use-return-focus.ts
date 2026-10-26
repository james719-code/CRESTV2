import { useState } from 'react'

// Programmatically opened details have no Radix DialogTrigger to restore focus to.
export function useReturnFocus() {
  const [origin] = useState(() =>
    document.activeElement instanceof HTMLElement ? document.activeElement : null,
  )
  return (event: Event) => {
    event.preventDefault()
    requestAnimationFrame(() => {
      const activeDialog = document.querySelector('[role="dialog"][data-state="open"]')
      if (activeDialog && !activeDialog.contains(origin)) return
      if (origin?.isConnected) origin.focus({ preventScroll: true })
      else document.getElementById('main-content')?.focus({ preventScroll: true })
    })
  }
}
