# Teacher card design and frontend performance

Measured locally on 26 October 2026. These results describe this workspace and browser, not a guarantee for every device or live school dataset.

## Changes

- Student cards show a strand-colored header, research group/title, submission status, and Preparing → Review/Revising → Approved stages. Stages use the existing submission state; there are no invented grades or completion percentages.
- Research and document cards have static SVG/CSS covers. Empty results use a small research illustration and the existing recovery actions.
- Each student/group view renders one responsive layout, selected from container width and reading preferences, instead of building both a hidden table and a visible card list.
- All main collections paginate at 12 records, with five groups in the overview. Search and filters operate on the complete collection. Page changes return focus to the section heading, and programmatically opened detail windows restore focus on close.
- Indexed student/group joins replace repeated full-array scans. Deferred search keeps typing ahead of result rendering.
- Phosphor icons and Radix primitives use module subpaths. The overview, library, landing page, and detail/file windows load on demand. Global search does no record scans while closed. Closed detail windows are unmounted; PDFs retain their URL cleanup.
- Modal overlays no longer apply backdrop blur. New decoration has no animation, remote assets, or image decoding cost.

## Measurements

| Check | Before | After |
| --- | ---: | ---: |
| Student entries rendered for 37 records, narrow Standard layout | 37 table rows + 37 cards | 12 cards, 0 table rows |
| DOM elements on that student page, dark theme | 1,300 | 487 |
| Initial production JavaScript, uncompressed | 631,388 bytes | approximately 568,400 bytes |
| Initial production JavaScript, gzip | 178.62 kB | approximately 167.52 kB |

The DOM measurement used the same saved demo records and default narrow browser viewport. The new cards contain more useful content despite having about 63% fewer total page elements. At 1366 CSS pixels in Standard layout, the student page renders 12 table rows and no cards; Easy reading renders 12 cards and no table rows.

The production figures come from `npm run build`. Lazy Firebase remains approximately 539 kB uncompressed / 160 kB gzip and is loaded only for a configured live connection. It is not part of the demo entry download. Vite still reports its large-chunk advisory; that advisory is not suppressed.

`scripts/benchmark-lists.mjs` compares the previous join/search algorithm with the indexed implementation using synthetic data: 2,000 students, 500 groups, and five successive queries. Two local runs, each taking the median of nine samples, measured 76.65–78.30 ms before and 2.66–2.93 ms after, including index construction. This is an algorithm benchmark, not a browser frame-rate or interaction latency measurement. Re-run it on the target machine rather than treating these timings as a fixed requirement.

The previously optimized Phosphor barrel was about 6.38 MB in development. Source imports now reference the 47 used icons directly rather than the package root, which avoids eagerly transforming the entire icon catalog. Old files may remain in Vite's development cache until it is regenerated; they are no longer imported by the source.

## Validation

Production build, lint, and 22 Node tests pass, including new checks for legacy membership precedence, searchable member names, page bounds, and preserving every record across pages. Browser checks cover full-collection student search, pagination/focus, opening details, paper covers and empty results, light/dark themes, and switching Standard/Easy reading. Responsive layout checks use temporary browser viewports; the viewport and original display settings are restored after testing.

## Overflow follow-up

The research-status tab list had a three-pixel vertical overflow from an invisible line indicator extending beyond its buttons. The shared tab component now only creates that indicator for its line variant. The teacher status tabs use a content-sized segmented style with visible selection and counts. Narrow/card layouts use a labeled status dropdown and preserve the current selection when the layout changes. Long select values wrap instead of being clipped.

The audit also found a 545-pixel account table inside a 286-pixel mobile container. Account approvals now render one responsive card layout on small screens or in Easy reading, keeping names, details, and the approval action visible. Tables remain available in the wide Standard layout. Pagination still bounds both layouts to 12 records.

All seven teacher pages were inspected at 320, 1024, and 1366 CSS pixels (21 page/width checks): no horizontal page overflow or unintended inner scroll containers remained. Native page scrolling and scrolling for genuinely long menus/dialogs remain available. Tab clicks, arrow-key activation, dropdown filtering, status counts, and large-text layout changes were also checked. These layout checks complement the existing tests; they do not measure frame rate or live-project data latency.

Extra-large text revealed crowded header labels at desktop width. Container queries now collapse optional search/account labels according to actual available space and text size. At 320 pixels with extra-large text, Display takes its own row and the other actions remain at least 55 pixels tall. A wrapped “Changes requested (3)” selection measured 177 × 54 pixels with identical client/scroll dimensions, confirming that both lines are visible. Light, Dark, and Easy reading were checked, then the original Dark/Standard preferences and default viewport were restored.
