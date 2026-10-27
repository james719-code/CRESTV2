# CREST teacher website

The teacher side of CREST, adapted for the web with React, TypeScript, Vite, Tailwind CSS, and shadcn/ui. A public landing page introduces the project before teacher sign-in. It credits creator and maintainer James Ryan S. Gallego and the project's 3+ years of continued maintenance. The workspace uses the same Firebase collections, teacher roles, submission flags, and Appwrite file references as the Android application.

## Run locally

Use Node.js 22.15 or newer and npm.

```sh
npm ci
npm run dev
```

Open the local URL shown by Vite to see the public landing page. `#home` opens the landing page, `#signin` opens teacher sign-in, and `#overview` opens the workspace. Without Firebase settings, the landing page provides an explicit “Explore the teacher demo” button. Sample student and group records are fictional; edits are stored only in this browser. Demo PDFs you upload are stored in IndexedDB and remain viewable after a reload. Sample records do not include actual manuscripts. In a configured school deployment, the demo button is absent and the workspace requires an approved teacher session.

## Teacher workflows

The landing page also provides a direct Android APK download and shows the school
seal supplied by the maintainer (`public/bdbshs-school-logo.jpg`). Download metadata
comes from the latest published GitHub Release; an unreleased development version
is never used as the label for an older APK. The permanent download works even
when release metadata cannot be fetched. See [the release process](../docs/RELEASE.md)
for versioning, signing, tags, and the stable asset name.

- Overview: student and group counts, submission progress, review queue, and recent research.
- Research groups: search, strand and status filters, members, PDF viewing, and feedback history.
- Students: search all profiles, filter approved/pending/declined accounts, approve or decline registrations, revoke or restore access, and follow group research progress.
- Submission reviews: request changes with required feedback, or confirm approval to add the paper to the library.
- Research library: search papers and authors, filter by research type, read PDFs, and add teacher research.
- Documents: search and share PDF templates, rubrics, and guides.
- Account approvals: approve pending student and teacher registrations, or decline student registrations with confirmation. Declined students remain available under Students.
- Global search: find groups, students, papers, or documents; Ctrl/Cmd+K opens it.
- Export progress: download a CSV of students, groups, titles, and submission states. Spreadsheet formulas in user-provided names/titles are escaped.
- Deadlines & milestones: create, edit, complete, reopen, and delete proposal, chapter, defense, and final manuscript milestones. Filter by group, stage, or status; the overview highlights upcoming and overdue work. Group details show the same plan.
- Notification inbox: review submissions, research approvals, revision feedback, and pending registrations, with per-teacher read/unread state, type filters, and links to the related records. The bell and sidebar show the unread count.

Submission progress means preparing, pending review, changes requested, or approved. Teacher-entered milestones are a separate plan and are completed manually; uploading or approving a manuscript does not complete chapters automatically. The shared Android group schema still has no grades, submission dates, assigned classes, or teacher ownership. Counts cover the school records accessible to the approved teacher. Approval progress is the fraction of groups with an accepted manuscript, not a measure of individual learning or time spent.

### Deadlines and inbox storage

Deploy `firebase/firestore.rules` before publishing this website version. The live workspace now waits for `group_milestones` and the signed-in teacher's `teacher_inboxes/{uid}/items` in addition to the six existing collections. No data migration or new composite index is required. The Android app now displays the same milestones and teacher inbox. Its teacher read/unread state synchronizes with the website. Approved students can read only their own group's milestones and use a separate private `student_inboxes/{uid}/items` collection.

Milestones contain `groupId`, `title`, `stage`, an ISO `dueDate`, completion time, and a version counter. Creation/update metadata records the teacher and server time. Approved teachers can manage school plans. Approved students can read only their own group's milestones and cannot alter them; pending and signed-out accounts cannot access them. Transactions reject stale edits, completion actions, and deletion attempts so one teacher cannot accidentally overwrite another teacher's change. Due dates include the entire selected day in Asia/Manila; incomplete milestones become overdue on the following day. The page refreshes date status automatically while open.

The inbox works independently of FCM and browser push permission. When the workspace synchronizes, the website captures current pending submissions, accepted group manuscripts, all retained revision comments, and pending registrations into the current teacher's private inbox. Immutable PDF IDs and feedback identities produce stable event IDs. Transactions create missing items without overwriting existing content or resetting read status, including across tabs and devices. Updates already captured stay available after their source record changes or is deleted; unavailable groups are labeled accordingly. Read/unread changes affect only the current teacher.

This is an inbox of observed school updates, not a server-side audit log: transitions that begin and end while every website session for that teacher is closed may not be captured. The timestamp is explicitly labeled **Added to inbox**, since legacy records do not include an original submission/review time. Capturing every intermediate event and delivery while the website is closed would require a server event producer. Push delivery remains under the existing FCM feature flag.

Demo milestones and inbox history use the existing browser-only demo storage. Previous saved demos remain compatible and can add milestones from the new page.

## Connect the school app

Supabase storage and its Firebase-authenticated Edge Function are deployed to
the CREST project. Netlify build settings select this live connection; messaging
remains disabled. Follow [the backend setup guide](../docs/BACKEND_SETUP.md) for
current status and the website-hostname step before publishing.

Copy `.env.example` to `.env.local` and fill in the public web client settings. Restart Vite after changing them. All four Firebase settings are required; partial configuration displays a setup state instead of silently switching to demo mode.

1. Use the registered `CREST Teacher Web` Firebase app in `crest-4fb99`. Its public settings are in `netlify.toml`; local development reads `.env.local`.
2. Enable the Google sign-in provider and add the development/production website hostnames to Firebase Authentication's authorized domains.
3. Sign in with an existing approved teacher. The record must be `users/user_details/teachers/{uid}` with `access: true`. The website does not create teacher access or allow students into the teacher workspace.
4. Set `VITE_STORAGE_PROVIDER=supabase` and `VITE_SUPABASE_URL=https://mkmirlnypehadajrtxvg.supabase.co`. Existing Appwrite IDs resolve through the private migration registry; no Appwrite session is needed.
5. The deployed rules in `firebase/firestore.rules` enforce approved teacher access. Run the emulator security suite before changing them.

`VITE_*` settings are public browser configuration. Never add service account credentials or Appwrite server keys. Actual environment files are ignored by Git.

### Collection compatibility

| Data | Existing path |
| --- | --- |
| Students | `users/user_details/students` |
| Teachers | `users/user_details/teachers` |
| Groups and feedback | `groups` |
| Qualitative papers | `researches/research_details/qualitative` |
| Quantitative papers | `researches/research_details/quantitative` |
| School documents | `documents` |

Reads subscribe to live Firestore updates after teacher verification. Losing teacher access closes the workspace. Account approval writes `accepted: true, declined: false` for students or `access: true` for teachers. Declining a student or revoking their access writes `accepted: false, declined: true`; profiles, membership, and research remain intact. Legacy profiles without `declined` retain their existing pending/approved states. Student decisions recheck current teacher access and the expected account state in a transaction, so a stale decision cannot overwrite another teacher's different decision. Android continues to use `accepted` for access; it does not yet display a separate declined label. A review transaction rechecks access and current submission state before writing. Approval adds the research and updates `accepted_research`/`uploaded` atomically. Requesting changes appends to `comments`, sets `uploaded: false`, and preserves the previous manuscript and title. Android's denial action clears submission metadata while retaining the stored object during migration; submission status and feedback remain compatible.

The existing upload form limits PDFs to 5 MB and validates the PDF signature. The
legacy provider uses a single Appwrite upload. The active Supabase provider
uses a signed direct upload authorized by an Edge Function. Files are retained
when metadata saving fails; physical cleanup is excluded during migration. PDFs
open in an embedded browser viewer with a download fallback. Live gateway sign-in,
role checks, private downloads, and student/teacher uploads passed smoke tests.
Server credentials are stored only in Supabase secrets and ignored local files.

### PDF bandwidth and browser storage

Papers now open on their own reader page (`#read/qualitative/{id}` or
`#read/quantitative/{id}`), with the PDF on the left and the saved paper information
on the right. Documents and group manuscripts use corresponding reader links.
On small screens, the information follows the PDF. The reader supports selectable
text, search, page navigation, zoom, fit width, and downloading the original PDF.

The last visible page is saved automatically in localStorage as you scroll or
change pages, separately for each account/backend and immutable PDF ID. Reopening
or refreshing the reader restores that page after the PDF loads. Only file ID,
page number, and update time are saved; the most recent 200 files per account are
retained. A new uploaded PDF has its own position. If browser storage is blocked
or full, the PDF remains readable and the reader shows that progress could not be
saved. Progress belongs to this browser/device and is not synchronized to another.

Mozilla PDF.js, its worker, fonts, character maps, and image decoders are served
locally. Reader code and styles load only when a reader link is opened. PDF access
continues to use the existing authorization and byte cache described below.

The active Supabase provider saves validated, viewed PDFs in IndexedDB for **30 days from download**, keyed by Firebase project, Supabase backend, user, and immutable file ID. Opening a cached copy does not extend its expiry. Each open first rechecks current Firebase approval and file permissions through the storage gateway; cached bytes then replace the PDF download. Signed URLs are never persisted or extended. Expired entries are removed on cache use, and the least recently viewed copies are evicted at a **100 MiB total browser cache limit**. New uploads have new IDs and therefore cannot reuse an older PDF.

Signing out or losing teacher approval clears local copies. Failed authorization removes that file's cached copy; offline access is deliberately unavailable. Concurrent opens share one in-flight request. Storage denial, browser eviction, or quota errors fall back to a normal authorized download. The cache reduces repeat PDF bandwidth; authorization calls and their Firestore reads still occur. Browsers can remove cached copies before 30 days, and signing out ends their retention early. Demo uploads use their existing separate storage.

## UI and branding

Official shadcn/ui component sources are under `src/components/ui`; `components.json` records the registry configuration. The controls use Radix primitives, local variant utilities, and Phosphor icons. `src/index.css` maps shadcn's semantic CSS variables to `../branding/generated/web/crest-tokens.css`. Page styles are in `src/App.css`. The shared font is bundled with the production build and its license is served at `/licenses/Inter-OFL.txt`. No remote fonts or stock photos are required.

The website uses the Android app's original logo in the landing page, teacher sidebar, sign-in page, and favicon. `public/crest-logo.svg` preserves all 180 foreground paths, colors, the white circular base, and the 1.45 scaling used by Android's `CrestMark` in `AppLogo.kt`. It is generated by `npm run logo:sync` from `android/app/src/main/res/drawable/ic_launcher_foreground.xml`; `npm run logo:check` verifies it remains in sync. The original Android assets are unchanged.

Responsive layouts include a collapsible mobile sidebar and independently scrollable data tables. Dialogs and review sheets use Radix focus handling; search, filters, empty states, busy states, validation errors, keyboard focus, and reduced motion are supported. Hash navigation supports browser back/forward and direct page links.

Student cards include strand colors, the group's research title, and three submission stages derived from actual group status. Research papers have static illustrated covers, documents use a book-style PDF cover, and empty results have a small research illustration with useful next steps. These designs use local SVG/CSS and the shared theme tokens; they require no remote images or continuous animation.

### List performance

Lists render at most 12 records per page (five groups in the overview). Searches and filters still cover the complete collection and return to page one when changed. Pagination returns keyboard focus to the section heading. A container observer selects either a table or cards; the hidden alternative is no longer built. Easy reading always uses cards. Student/group joins and searchable member names are indexed when the workspace changes, while search input can update ahead of its filtered results.

Icons and Radix controls import only their used modules. The public landing page loads first; the teacher application, Firebase connection, overview, library, and file/detail windows load on demand. Returning home pauses school-data subscriptions while retaining operation tracking, so an ongoing group deletion can still report a failure and offer a retry after returning. Closed detail windows are unmounted, and PDF object URLs are released on close. Global search scans records only while open. Modal backdrops use a plain tint to avoid an extra blur pass.

Each Firestore listener maps only its changed collection. Student/group indexes retain their results when unrelated research or documents change. The production build checks the complete static JavaScript entry graph against a 420 kB raw / 125 kB gzip budget. Netlify caches fingerprinted `/assets/*` for one year and revalidates HTML and the messaging worker.

See [the measurement notes](../docs/frontend-ui-performance.md) for before/after results and limits. The repeatable synthetic join/search benchmark is:

```sh
node --experimental-strip-types scripts/benchmark-lists.mjs
```

### Display and reading preferences

The labeled **Display** button is available in the workspace header, landing page, and sign-in page. Teachers can choose **Light**, **Dark**, or **Use device**, independently of their reading layout. Device theme changes are observed automatically in Use device mode. The original logo remains unaltered in both themes.

**Standard** uses larger, clearer default type and controls, with tables when there is enough room. **Easy reading** increases text size and spacing, strengthens text/control contrast, minimizes motion, and enables a short next-step guide on each workspace page. Group, student, and account records become readable cards with labeled actions. Cards also replace narrow tables automatically on smaller screens or within a narrow content area. Every feature remains available in either layout. These choices are based on comfort and preference, not age.

Research status uses clear segmented tabs with counts on wide screens, and a labeled dropdown in card layouts. The selected filter survives layout changes. Tabs grow with their content and show a clear selected/focus state without an inner scrollbar. Filter selections can wrap to keep longer labels readable. Account approvals use cards in narrow layouts so names, contact details, and approval buttons remain available without sideways scrolling.

The workspace header also responds to its available width in rem units. Larger text collapses optional search/account labels before they become crowded, and very narrow layouts give the labeled Display control its own row while retaining the other header actions.

Text size can be set separately to Standard, Large, or Extra large. Stronger contrast, Less motion, and Show guidance can also be changed independently. Font sizes use rem units and percentage-based root scaling so browser zoom and the user's own text-size preference remain supported. Device reduced-motion preferences are always respected. The Display dialog keeps its Done button visible while its options scroll.

Preferences are stored separately from school/demo records in the browser under `crest-display-preferences-v1`, restored before the app renders, and synchronized across tabs. Invalid saved values fall back to safe defaults. If storage is unavailable, choices still apply for the current visit and the dialog explains that they could not be saved. Reset display restores the default device theme and Standard layout.

## Checks

```sh
npm run test
npm run lint
npm run build
npm run logo:check
node ../branding/scripts/generate.mjs --check
```

The Node test suite also covers the PDF cache's fixed expiry, account/backend isolation, LRU storage limit, repeat-download suppression, corrupt files, unavailable storage, access denial, sign-out, and account-switch races. Student tests cover decline/restore, record preservation, pending counts, legacy profiles, and stale decisions. The Firestore emulator suite exercises the actual student-management transactions and rejects self-approval/decline changes. `tests/fixtures/teacher-workspace-sample.pdf` is a harmless local PDF for testing upload and viewing.

To serve the built output locally:

```sh
npm run preview
```
