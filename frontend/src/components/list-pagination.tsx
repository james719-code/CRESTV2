import { Button } from '@/components/ui/button'
import { ArrowLeft, ArrowRight } from '@/icons'
import type { MouseEvent } from 'react'

export function ListPagination({
  page,
  pages,
  start,
  count,
  total,
  noun,
  onPage,
}: {
  page: number
  pages: number
  start: number
  count: number
  total: number
  noun: string
  onPage: (page: number) => void
}) {
  function changePage(next: number, event: MouseEvent<HTMLButtonElement>) {
    onPage(next)
    const card = event.currentTarget.closest<HTMLElement>('.table-card')
    const heading = card?.querySelector('h2')
    if (heading) {
      heading.tabIndex = -1
      heading.focus({ preventScroll: true })
    }
    card?.scrollIntoView({ block: 'start' })
  }
  return (
    <div className="list-pagination">
      <span role="status" aria-live="polite">
        {total ? `${start + 1}–${start + count}` : '0'} of {total} {noun}
      </span>
      {pages > 1 && (
        <nav aria-label={`${noun} pages`}>
          <Button
            variant="outline"
            size="sm"
            disabled={page === 1}
            onClick={(event) => changePage(page - 1, event)}
            aria-label={`Previous page of ${noun}`}
          >
            <ArrowLeft size={16} />
            Previous
          </Button>
          <span>
            Page {page} of {pages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page === pages}
            onClick={(event) => changePage(page + 1, event)}
            aria-label={`Next page of ${noun}`}
          >
            Next
            <ArrowRight size={16} />
          </Button>
        </nav>
      )}
    </div>
  )
}
