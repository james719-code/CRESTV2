import { useContext, useEffect, useRef, useState } from 'react'
import { ReadingLayoutContext } from '../display-context'

export function useCardLayout() {
  const layout = useContext(ReadingLayoutContext)
  const ref = useRef<HTMLDivElement>(null)
  const [narrow, setNarrow] = useState(() => window.innerWidth < 1100)
  useEffect(() => {
    const element = ref.current
    if (!element) return
    const observer = new ResizeObserver(([entry]) => {
      const rem = parseFloat(getComputedStyle(document.documentElement).fontSize)
      setNarrow(entry.contentRect.width < 46 * rem)
    })
    observer.observe(element)
    return () => observer.disconnect()
  }, [])
  return { ref, cards: layout === 'easy-reading' || narrow }
}
