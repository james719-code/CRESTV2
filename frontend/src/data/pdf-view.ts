import * as pdfjs from 'pdfjs-dist'
import workerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url'

export interface PdfView {
  goToPage(page: number): void
  zoom(direction: number): void
  fitWidth(): void
  find(query: string, previous?: boolean, again?: boolean): void
}

export async function createPdfView(options: {
  container: HTMLDivElement
  viewer: HTMLDivElement
  blob: Blob
  signal: AbortSignal
  initialPage: (count: number) => number
  onReady: (count: number, page: number) => void
  onPage: (page: number, count: number) => void
  onScale: (scale: number) => void
  onMatches: (current: number, total: number) => void
  onError: (error: unknown) => void
}): Promise<PdfView> {
  // The viewer's generic distribution expects the core PDF.js module to have
  // initialized first. Keep both modules behind the reader route.
  const { PDFViewer, PDFLinkService, PDFFindController, EventBus, LinkTarget } =
    await import('pdfjs-dist/web/pdf_viewer.mjs')
  options.signal.throwIfAborted()
  pdfjs.GlobalWorkerOptions.workerSrc = workerUrl
  const assets = `${import.meta.env.BASE_URL}assets/pdfjs-${pdfjs.version}/`
  const eventBus = new EventBus()
  const links = new PDFLinkService({
    eventBus,
    externalLinkTarget: LinkTarget.BLANK,
    externalLinkRel: 'noopener noreferrer',
  })
  const find = new PDFFindController({ eventBus, linkService: links })
  const viewerOptions = {
    container: options.container,
    viewer: options.viewer,
    eventBus,
    linkService: links,
    findController: find,
    annotationMode: pdfjs.AnnotationMode.ENABLE,
    imageResourcesPath: `${assets}images/`,
    maxCanvasPixels: 8 * 1024 * 1024,
    abortSignal: options.signal,
  }
  const viewer = new PDFViewer(viewerOptions)
  links.setViewer(viewer)
  let initialized = false
  const loading: { task?: ReturnType<typeof pdfjs.getDocument> } = {}
  let lastWidth = 0
  let resizeFrame = 0
  const resize = new ResizeObserver(([entry]) => {
    const width = entry.contentRect.width
    if (Math.abs(width - lastWidth) < 1) return
    lastWidth = width
    if (!initialized || viewer.currentScaleValue !== 'page-width') return
    cancelAnimationFrame(resizeFrame)
    // Scaling changes page layout. Apply it after the observer's delivery cycle.
    resizeFrame = requestAnimationFrame(() => {
      if (!options.signal.aborted && viewer.currentScaleValue === 'page-width')
        viewer.currentScaleValue = 'page-width'
    })
  })
  resize.observe(options.container)
  options.signal.addEventListener(
    'abort',
    () => {
      initialized = false
      cancelAnimationFrame(resizeFrame)
      resize.disconnect()
      viewer.setDocument(null)
      links.setDocument(null)
      void viewer.l10n?.destroy()
      void loading.task?.destroy().catch(() => undefined)
    },
    { once: true },
  )
  eventBus.on('pagesinit', () => {
    if (options.signal.aborted) return
    viewer.currentScaleValue = 'page-width'
    const page = Math.max(1, Math.min(viewer.pagesCount, options.initialPage(viewer.pagesCount)))
    viewer.currentPageNumber = page
    initialized = true
    options.onReady(viewer.pagesCount, page)
  })
  eventBus.on('pagechanging', ({ pageNumber }: { pageNumber: number }) => {
    if (initialized && !options.signal.aborted) options.onPage(pageNumber, viewer.pagesCount)
  })
  eventBus.on('scalechanging', ({ scale }: { scale: number }) => {
    if (!options.signal.aborted) options.onScale(scale)
  })
  eventBus.on(
    'updatefindmatchescount',
    ({ matchesCount }: { matchesCount: { current: number; total: number } }) => {
      if (!options.signal.aborted) options.onMatches(matchesCount.current, matchesCount.total)
    },
  )
  eventBus.on('updatefindcontrolstate', ({ state }: { state: number }) => {
    if (!options.signal.aborted && state === 1) options.onMatches(0, 0)
  })
  eventBus.on('pagerendered', ({ error }: { error?: unknown }) => {
    if (error && !options.signal.aborted) options.onError(error)
  })
  const data = new Uint8Array(await options.blob.arrayBuffer())
  options.signal.throwIfAborted()
  loading.task = pdfjs.getDocument({
    data,
    cMapUrl: `${assets}cmaps/`,
    cMapPacked: true,
    standardFontDataUrl: `${assets}standard_fonts/`,
    wasmUrl: `${assets}wasm/`,
    iccUrl: `${assets}iccs/`,
    enableXfa: false,
  })
  const document = await loading.task.promise
  options.signal.throwIfAborted()
  links.setDocument(document)
  viewer.setDocument(document)
  return {
    goToPage: (page) => {
      if (initialized && Number.isInteger(page) && page >= 1 && page <= viewer.pagesCount)
        viewer.currentPageNumber = page
    },
    zoom: (direction) => {
      viewer.currentScale = Math.min(
        4,
        Math.max(0.25, viewer.currentScale * (direction > 0 ? 1.15 : 1 / 1.15)),
      )
    },
    fitWidth: () => {
      viewer.currentScaleValue = 'page-width'
    },
    find: (query, previous = false, again = false) =>
      eventBus.dispatch('find', {
        source: viewer,
        type: again ? 'again' : '',
        query,
        caseSensitive: false,
        entireWord: false,
        highlightAll: true,
        findPrevious: previous,
        matchDiacritics: false,
      }),
  }
}
