import { useEffect, useRef, useState, type FormEvent } from 'react'
import {
  ArrowLeft,
  ArrowRight,
  BookOpenText,
  DownloadSimple,
  MagnifyingGlass,
  Plus,
  X,
} from '@/icons'
import { Minus } from '@phosphor-icons/react/dist/csr/Minus'
import { Button } from '@/components/ui/button'
import { CrestLogo } from './crest-logo'
import { DisplaySettings } from './display-settings'
import { displayDate } from '../domain'
import type { ReaderPaper } from '../data/reader-paper'
import type { PdfView } from '../data/pdf-view'
import {
  loadReadingProgress,
  saveReadingProgress,
  type ProgressStorage,
} from '../data/reading-progress'
import 'pdfjs-dist/web/pdf_viewer.css'
import './paper-reader.css'

interface ReaderProps {
  paper: ReaderPaper | null
  scope: string
  readFile: (id: string) => Promise<Blob | undefined>
  isDemo: boolean
  backLabel: string
  onBack: () => void
}

export default function PaperReader(props: ReaderProps) {
  if (!props.paper)
    return (
      <main className="reader-unavailable">
        <CrestLogo size={52} />
        <h1>This paper is no longer available</h1>
        <p>Return to the library to choose an available paper.</p>
        <Button variant="outline" onClick={props.onBack}>
          <ArrowLeft size={18} />
          {props.backLabel}
        </Button>
      </main>
    )
  return <ReaderDocument {...props} paper={props.paper} />
}

function ReaderDocument({
  paper,
  scope,
  readFile,
  isDemo,
  backLabel,
  onBack,
}: ReaderProps & { paper: ReaderPaper }) {
  const container = useRef<HTMLDivElement>(null)
  const pages = useRef<HTMLDivElement>(null)
  const heading = useRef<HTMLHeadingElement>(null)
  const searchInput = useRef<HTMLInputElement>(null)
  const engine = useRef<PdfView | null>(null)
  const [page, setPage] = useState(1)
  const [pageInput, setPageInput] = useState('1')
  const [count, setCount] = useState(0)
  const [scale, setScale] = useState(1)
  const [ready, setReady] = useState(false)
  const [url, setUrl] = useState('')
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const [savedPage, setSavedPage] = useState(1)
  const [storageAvailable, setStorageAvailable] = useState(true)
  const [searchOpen, setSearchOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [matches, setMatches] = useState({ current: 0, total: 0 })
  const sample = isDemo && paper.fileId.startsWith('sample-')
  const fileId = paper.fileId

  useEffect(() => {
    heading.current?.focus({ preventScroll: true })
  }, [])

  useEffect(() => {
    if (!fileId || sample || !container.current || !pages.current) return
    const controller = new AbortController()
    let objectUrl = ''
    let storage: ProgressStorage | undefined
    try {
      storage = window.localStorage
    } catch {
      /* Reading remains available. */
    }
    const fail = (cause: unknown) => {
      if (controller.signal.aborted) return
      setReady(false)
      setError(
        objectUrl
          ? 'The preview could not be displayed. Try again or download the PDF to open it in another reader.'
          : cause instanceof Error
            ? cause.message
            : 'The PDF could not be opened. Try again.',
      )
      controller.abort()
    }
    const viewerContainer = container.current
    const viewerPages = pages.current
    async function open() {
      // Keep the same current-access check and 30-day PDF cache as the library.
      const blob = await readFile(fileId)
      if (controller.signal.aborted) return
      if (!blob) throw new Error('No PDF is attached to this record.')
      objectUrl = URL.createObjectURL(blob)
      setUrl(objectUrl)
      const { createPdfView } = await import('../data/pdf-view')
      if (controller.signal.aborted) return
      const view = await createPdfView({
        container: viewerContainer,
        viewer: viewerPages,
        blob,
        signal: controller.signal,
        initialPage: (total) => {
          const progress = loadReadingProgress(storage, scope, fileId, total)
          setSavedPage(progress.page)
          setStorageAvailable(progress.available)
          return progress.page
        },
        onReady: (total, current) => {
          setCount(total)
          setPage(current)
          setPageInput(String(current))
          setReady(true)
        },
        onPage: (current, total) => {
          setPage(current)
          setPageInput(String(current))
          setStorageAvailable(saveReadingProgress(storage, scope, fileId, current, total))
        },
        onScale: setScale,
        onMatches: (current, total) => setMatches({ current, total }),
        onError: fail,
      })
      if (!controller.signal.aborted) engine.current = view
    }
    void open().catch(fail)
    return () => {
      controller.abort()
      engine.current = null
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [fileId, scope, readFile, sample, retry])

  function goToPage(event: FormEvent) {
    event.preventDefault()
    const value = Number(pageInput)
    if (Number.isInteger(value) && value >= 1 && value <= count) engine.current?.goToPage(value)
    else setPageInput(String(page))
  }

  function openSearch() {
    setSearchOpen(true)
    requestAnimationFrame(() => searchInput.current?.focus())
  }

  function find(value: string, previous = false, again = false) {
    setQuery(value)
    if (!value) setMatches({ current: 0, total: 0 })
    engine.current?.find(value, previous, again)
  }

  const authors = paper.authors.filter((author) => author.trim())
  return (
    <div
      className="paper-reader"
      onKeyDown={(event) => {
        if (ready && (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'f') {
          event.preventDefault()
          openSearch()
        }
      }}
    >
      <header className="reader-header">
        <Button variant="ghost" onClick={onBack}>
          <ArrowLeft size={18} />
          <span>{backLabel}</span>
        </Button>
        <div className="reader-brand">
          <CrestLogo size={34} decorative />
          <span>
            CREST <small>Paper reader</small>
          </span>
        </div>
        <DisplaySettings />
      </header>
      <main className="reader-layout">
        <section className="reader-document" aria-label="PDF reader">
          <div className="reader-toolbar" role="group" aria-label="PDF controls">
            <div className="reader-page-controls">
              <Button
                size="icon"
                variant="ghost"
                aria-label="Previous PDF page"
                disabled={!ready || page <= 1}
                onClick={() => engine.current?.goToPage(page - 1)}
              >
                <ArrowLeft size={18} />
              </Button>
              <form onSubmit={goToPage} className="reader-page-form">
                <label htmlFor="reader-page-number">Page</label>
                <input
                  id="reader-page-number"
                  aria-label="Page number"
                  type="number"
                  inputMode="numeric"
                  min={1}
                  max={count || 1}
                  value={pageInput}
                  disabled={!ready}
                  onChange={(event) => setPageInput(event.target.value)}
                  onBlur={() => {
                    const value = Number(pageInput)
                    if (Number.isInteger(value) && value >= 1 && value <= count)
                      engine.current?.goToPage(value)
                    else setPageInput(String(page))
                  }}
                />
                <span>of {count || '—'}</span>
              </form>
              <Button
                size="icon"
                variant="ghost"
                aria-label="Next PDF page"
                disabled={!ready || page >= count}
                onClick={() => engine.current?.goToPage(page + 1)}
              >
                <ArrowRight size={18} />
              </Button>
            </div>
            <div className="reader-zoom-controls">
              <Button
                size="icon"
                variant="ghost"
                aria-label="Zoom out"
                disabled={!ready || scale <= 0.25}
                onClick={() => engine.current?.zoom(-1)}
              >
                <Minus size={18} />
              </Button>
              <span className="reader-zoom-value" aria-label="Zoom level">
                {ready ? `${Math.round(scale * 100)}%` : '—'}
              </span>
              <Button
                size="icon"
                variant="ghost"
                aria-label="Zoom in"
                disabled={!ready || scale >= 4}
                onClick={() => engine.current?.zoom(1)}
              >
                <Plus size={18} />
              </Button>
              <Button variant="ghost" disabled={!ready} onClick={() => engine.current?.fitWidth()}>
                Fit width
              </Button>
            </div>
            <Button
              size="icon"
              variant="ghost"
              aria-label="Find in paper"
              aria-expanded={searchOpen}
              disabled={!ready}
              onClick={openSearch}
            >
              <MagnifyingGlass size={19} />
            </Button>
            {url ? (
              <Button variant="outline" size="icon" asChild>
                <a
                  href={url}
                  download={`${paper.title || 'CREST-document'}.pdf`}
                  aria-label="Download PDF"
                >
                  <DownloadSimple size={19} />
                </a>
              </Button>
            ) : (
              <Button variant="outline" size="icon" disabled aria-label="Download PDF">
                <DownloadSimple size={19} />
              </Button>
            )}
          </div>
          {searchOpen && (
            <form
              className="reader-find"
              aria-label="Find text in PDF"
              onSubmit={(event) => {
                event.preventDefault()
                find(query, false, true)
              }}
            >
              <input
                ref={searchInput}
                aria-label="Find text in paper"
                placeholder="Find in this paper…"
                value={query}
                onChange={(event) => find(event.target.value)}
              />
              <span role="status">
                {query
                  ? matches.total
                    ? `${matches.current} of ${matches.total}`
                    : 'No matches'
                  : ''}
              </span>
              <Button
                type="button"
                size="icon"
                variant="ghost"
                aria-label="Previous match"
                disabled={!query}
                onClick={() => find(query, true, true)}
              >
                <ArrowLeft size={16} />
              </Button>
              <Button
                type="submit"
                size="icon"
                variant="ghost"
                aria-label="Next match"
                disabled={!query}
              >
                <ArrowRight size={16} />
              </Button>
              <Button
                type="button"
                size="icon"
                variant="ghost"
                aria-label="Close PDF search"
                onClick={() => {
                  find('')
                  setSearchOpen(false)
                }}
              >
                <X size={17} />
              </Button>
            </form>
          )}
          <div className="reader-viewport">
            <div
              ref={container}
              className="reader-scroll"
              tabIndex={0}
              aria-label="Paper pages"
              style={error || sample || !fileId ? { visibility: 'hidden' } : undefined}
            >
              <div ref={pages} className="pdfViewer" />
            </div>
            {sample || !fileId ? (
              <div className="reader-message">
                <BookOpenText size={38} />
                <h2>{sample ? 'Explore the paper reader' : 'No PDF is attached yet'}</h2>
                <p>
                  {sample
                    ? 'This fictional record has no manuscript. Add your own PDF from Research or Documents to try reading and saved page progress.'
                    : 'Return to the group or library to choose an available PDF.'}
                </p>
              </div>
            ) : error ? (
              <div className="reader-message" role="alert">
                <BookOpenText size={38} />
                <h2>We couldn’t open this paper</h2>
                <p>{error}</p>
                <Button
                  variant="outline"
                  onClick={() => {
                    setError('')
                    setReady(false)
                    setUrl('')
                    setCount(0)
                    setQuery('')
                    setSearchOpen(false)
                    setRetry((value) => value + 1)
                  }}
                >
                  Try again
                </Button>
                {url && (
                  <a href={url} download={`${paper.title}.pdf`}>
                    Download the PDF
                  </a>
                )}
              </div>
            ) : (
              !ready && (
                <div className="reader-message" role="status">
                  <span className="reader-loading-mark" />
                  <h2>Opening your paper…</h2>
                  <p>Preparing the pages and checking your saved place.</p>
                </div>
              )
            )}
          </div>
          <div className="reader-bottom-status" role="status">
            <span>
              {ready
                ? `Page ${page} of ${count}`
                : sample
                  ? 'Sample record'
                  : error
                    ? 'Preview unavailable'
                    : 'Preparing PDF'}
            </span>
            <span>
              {ready
                ? storageAvailable
                  ? 'Reading position saved on this device'
                  : 'This browser could not save your reading position'
                : 'PDF document'}
            </span>
          </div>
        </section>
        <aside className="reader-info" aria-label="Paper information">
          <div className="reader-info-heading">
            <BookOpenText size={21} />
            <span>
              {paper.kind === 'document'
                ? 'School document'
                : paper.kind === 'submission'
                  ? 'Group manuscript'
                  : 'Research paper'}
            </span>
          </div>
          <h1 ref={heading} tabIndex={-1}>
            {paper.title}
          </h1>
          <p className="reader-paper-status">{paper.status}</p>
          {paper.description && <p className="reader-description">{paper.description}</p>}
          <section className="reader-authors">
            <h2>{paper.kind === 'submission' ? 'Group members' : 'Authors'}</h2>
            {authors.length ? (
              <ul>
                {authors.map((author, index) => (
                  <li key={`${index}-${author}`}>{author}</li>
                ))}
              </ul>
            ) : (
              <p>Authors not recorded</p>
            )}
          </section>
          <dl className="reader-metadata">
            {paper.groupName && (
              <div>
                <dt>Research group</dt>
                <dd>{paper.groupName}</dd>
              </div>
            )}
            {paper.strand && (
              <div>
                <dt>Strand</dt>
                <dd>{paper.strand}</dd>
              </div>
            )}
            {paper.researchType && (
              <div>
                <dt>Research type</dt>
                <dd className="capitalize">{paper.researchType}</dd>
              </div>
            )}
            {paper.kind !== 'submission' && (
              <div>
                <dt>Added to library</dt>
                <dd>{displayDate(paper.createdAt)}</dd>
              </div>
            )}
            <div>
              <dt>Format</dt>
              <dd>PDF{count > 0 ? ` · ${count} pages` : ''}</dd>
            </div>
          </dl>
          <section className="reader-progress-info">
            <h2>Your reading position</h2>
            <strong>{ready ? `Page ${page} of ${count}` : 'Ready when you are'}</strong>
            {ready && (
              <div
                className="reader-progress-track"
                role="progressbar"
                aria-label="Reading position"
                aria-valuemin={1}
                aria-valuemax={count}
                aria-valuenow={page}
              >
                <span style={{ width: `${(page / count) * 100}%` }} />
              </div>
            )}
            <p>
              {!storageAvailable
                ? 'Reading still works, but your browser is not allowing progress to be saved.'
                : savedPage > 1
                  ? `Continued from page ${savedPage}. Your place updates automatically as you read.`
                  : 'Your last page is remembered in this browser so you can pick up where you left off.'}
            </p>
          </section>
          <p className="reader-access-note">
            {isDemo
              ? 'Demo files and reading progress stay in this browser.'
              : 'Your school access is checked each time you open a paper.'}
          </p>
        </aside>
      </main>
    </div>
  )
}
