# Teacher card design and frontend performance

## Modern visual refresh — 27 October 2026

The web UI keeps the CREST palette (blue, ink, teal, mist) and Inter, and adds depth
with two paint-only CSS layers: `src/modern.css` (public site, sign-in, shared controls)
and `src/workspace-modern.css` (teacher workspace, loaded with the workspace chunk).

- Landing: floating pill header, dot-grid and soft colour glows, gradient headline
  accent, a tilted gradient plate and two decorative notes around the workspace preview,
  a bento feature grid with one ink feature card, step cards, and a gradient project
  and closing panel.
- Workspace: a sidebar that follows the theme (light with a blue-tinted active item in
  the light theme, ink gradient with a glowing active item in the dark theme), softer
  top bar, tinted stat cards, pill segmented tabs, larger card radii, layered shadows,
  and hover lift on cards (disabled on touch devices).
- Controls: gradient primary buttons, raised outline buttons, and consistent dialog,
  menu and card radii in light and dark themes.

Constraints kept: no backdrop blur, no looping or entrance animation, no remote assets,
and no new JavaScript beyond two decorative `aria-hidden` notes on the landing preview.
The gradient headline falls back to a solid colour under strong contrast and forced
colours. Display settings (theme, text size, easy reading, reduce motion) still apply.
The two new stylesheets add about 30 kB raw and 6.9 kB gzip, and the public entry
JavaScript budget still passes (386.40 kB / 117.27 kB gzip).

Validation: TypeScript build, ESLint on changed files, all 69 Node tests and the
production build passed. Screenshots of the landing page, sign-in, overview, students,
library, group detail, help, about, settings, and the Display and
search dialogs were reviewed in light and dark at 1440 and 1280 pixels, and the landing
page, overview and open navigation at 390 pixels. These checks used the fictional demo
workspace; no frame-rate or real-device measurements were taken.

## Web loading follow-up — 27 October 2026

The public entry now loads the landing page directly and downloads the teacher
application only when a visitor opens sign-in or a workspace link. A public-page
visit does not initialize Firebase or subscribe to school records. After the
teacher application has been opened, a React Activity boundary retains its
operation state while home is visible and cleans up its subscriptions. Returning
to the workspace rechecks teacher access before displaying school records.
See the [React Activity reference](https://react.dev/reference/react/Activity).

The Firebase observer now maps only the collection that changed. Other collection
arrays retain their references, and student/group indexes depend only on students
and groups. Research remains sorted across both research collections, and the
workspace stays loading until all six initial collection snapshots arrive.

Fresh production builds on the same machine measured:

| Asset | Before | After |
| --- | ---: | ---: |
| Static public-entry JavaScript, raw | 677.97 kB | 382.49 kB |
| Static public-entry JavaScript, Vite gzip estimate | 201.09 kB | 115.63 kB |

The public JavaScript entry is about 44% smaller before compression. The old home
page also fetched a separate 29.27 kB landing module; that page is included in the
new entry. Teacher and Firebase modules still download when needed. These figures
measure transfer size, not a guaranteed change in page-load time or Core Web
Vitals. The shared 352.24 kB font remains a future optimization opportunity.

Every production build now checks the complete static entry graph using Vite's
manifest. It must remain below 420,000 raw bytes and 125,000 gzip bytes and must
keep the teacher and Firebase entries dynamic. The guard uses Node's gzip defaults,
which differ slightly from Vite's gzip estimate above. It does not hide large-chunk
warnings. Netlify's fingerprinted assets receive a one-year immutable cache policy;
HTML and the messaging worker revalidate.

Validation: TypeScript/production build, bundle budgets, ESLint, and all 55 Node
tests passed, including collection readiness, stable references, cross-collection
research sorting/removal, and independent observer state. Browser checks covered
the live-configured public page and sign-in screen plus an isolated teacher demo,
student pagination/search, and return navigation. Deployment and real-device
network timings remain separate release checks.

An isolated browser check also exercised the actual app with a simulated backend:
starting a group deletion, visiting home, and returning kept the same pending
operation without starting another request. The active observer count changed
from one to zero to one. Rejecting the simulated request after returning displayed
the failure message and “Retry delete” action. This check used fictional records
and made no production database changes.

## Dedicated reader follow-up — 27 October 2026

Research, document, group-manuscript, overview, and search links now open a
dedicated, refreshable reader route. Existing Firestore metadata supplies the
information panel; no abstract, publication date, or author details are invented.
Mozilla PDF.js provides observable page changes for localStorage restoration,
including changes from scrolling. Progress keys isolate backend, account, and
immutable file ID; corrupt or unavailable storage falls back to reading from page
one. The most recent 200 positions per account are retained.

The reader and PDF.js load dynamically. Workers, fonts, character maps, and
decoders use local versioned assets. The public-entry size budget still passes.
PDF URLs remain temporary, and leaving the reader destroys its worker, observers,
and object URL. The existing access checks and 30-day authorized PDF cache remain
in use.

Validation includes all 64 frontend tests, ESLint, TypeScript, the production
build and entry budget. An isolated five-page PDF was uploaded through the actual
demo interface and rendered in both development and production builds. Page 3
survived closing/reopening and reloading the route; scrolling advanced to page 4,
which was restored after another reload. Text search found all five matches.
Phone layouts at 390 and 320 pixels and the desktop information panel were
checked; viewport settings were restored after testing. These checks used
fictional local records and did not change production school data.

The reader-return regression was caused by PDF.js's generic `.sidebar` rule
remaining active after navigating back. Vite now scopes all top-level PDF.js
selectors and root variables to `.paper-reader`, preserving nested selectors,
media queries, and the viewer's live height measurement. Regression tests check
the entire installed PDF.js stylesheet. Browser checks with a five-page PDF
confirmed that both the reader's Back button and browser Back restore the
library's original sidebar width and toolbar position, while page 3 remains saved.
The production build and a 390-pixel mobile layout passed the same return check
without page overflow or browser errors.

## Earlier card and layout optimization

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
