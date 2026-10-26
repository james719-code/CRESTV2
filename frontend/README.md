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

- Overview: student and group counts, submission progress, review queue, and recent research.
- Research groups: search, strand and status filters, members, PDF viewing, and feedback history.
- Students: individual records linked to their group's submission state, including students without a group.
- Submission reviews: request changes with required feedback, or confirm approval to add the paper to the library.
- Research library: search papers and authors, filter by research type, read PDFs, and add teacher research.
- Documents: search and share PDF templates, rubrics, and guides.
- Account approvals: approve pending student and teacher registrations.
- Global search: find groups, students, papers, or documents; Ctrl/Cmd+K opens it.
- Export progress: download a CSV of students, groups, titles, and submission states. Spreadsheet formulas in user-provided names/titles are escaped.

Progress means submission state: preparing, pending review, changes requested, or approved. The current Android schema has no chapter-level milestones, grades, submission dates, assigned classes, or teacher ownership of groups, so the website does not invent those fields. Counts cover the school records accessible to the approved teacher. Approval progress is the fraction of groups with an accepted manuscript, not a measure of individual learning or time spent.

## Connect the school app

Copy `.env.example` to `.env.local` and fill in the public web client settings. Restart Vite after changing them. All four Firebase settings are required; partial configuration displays a setup state instead of silently switching to demo mode.

1. Register a Firebase web application in the same Firebase project used by Android. Copy its API key, auth domain, project ID, and app ID into the corresponding `VITE_FIREBASE_*` settings.
2. Enable the Google sign-in provider and add the development/production website hostnames to Firebase Authentication's authorized domains.
3. Sign in with an existing approved teacher. The record must be `users/user_details/teachers/{uid}` with `access: true`. The website does not create teacher access or allow students into the teacher workspace.
4. Match the Android Appwrite endpoint, project, and bucket using `VITE_APPWRITE_*`. Register the website hostname as an Appwrite web platform and verify the existing file permissions permit the intended operations. The current Android integration uses an Appwrite client without a separate Appwrite login; this website follows those existing client permissions. Firebase authentication does not automatically establish an Appwrite session. Projects requiring authenticated Appwrite access need their existing trusted session integration before uploads/read access will work. Do not add public write permissions to work around this.
5. Verify Firestore rules enforce approved-teacher access on the server for collection reads and mutations. The browser's role gate and transaction checks supplement these rules. No production rules or bucket permissions are changed by this implementation.

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

Reads subscribe to live Firestore updates after teacher verification. Losing teacher access closes the workspace. Account approval writes `accepted: true` for students or `access: true` for teachers. A review transaction rechecks access and current submission state before writing. Approval adds the research and updates `accepted_research`/`uploaded` atomically. Requesting changes appends to `comments`, sets `uploaded: false`, and preserves the previous manuscript and title. This differs from Android's current denial action, which clears/deletes the previous submission; the resulting status and feedback remain compatible with Android's student workflow.

PDF uploads are limited to 5 MB and must start with a PDF signature. The adapter intentionally uses a single Appwrite upload; larger chunked uploads are not implemented. PDFs open in an embedded browser viewer with a download fallback. The website's live sign-in, storage permissions, and cross-device synchronization require verification against the configured school project; no production credentials are bundled.

## UI and branding

Official shadcn/ui component sources are under `src/components/ui`; `components.json` records the registry configuration. The controls use Radix primitives, local variant utilities, and Phosphor icons. `src/index.css` maps shadcn's semantic CSS variables to `../branding/generated/web/crest-tokens.css`. Page styles are in `src/App.css`. The shared font is bundled with the production build and its license is served at `/licenses/Inter-OFL.txt`. No remote fonts or stock photos are required.

The website uses the Android app's original logo in the landing page, teacher sidebar, sign-in page, and favicon. `public/crest-logo.svg` preserves all 180 foreground paths, colors, the white circular base, and the 1.45 scaling used by Android's `CrestMark` in `AppLogo.kt`. It is generated by `npm run logo:sync` from `android/app/src/main/res/drawable/ic_launcher_foreground.xml`; `npm run logo:check` verifies it remains in sync. The original Android assets are unchanged.

Responsive layouts include a collapsible mobile sidebar and independently scrollable data tables. Dialogs and review sheets use Radix focus handling; search, filters, empty states, busy states, validation errors, keyboard focus, and reduced motion are supported. Hash navigation supports browser back/forward and direct page links.

Student cards include strand colors, the group's research title, and three submission stages derived from actual group status. Research papers have static illustrated covers, documents use a book-style PDF cover, and empty results have a small research illustration with useful next steps. These designs use local SVG/CSS and the shared theme tokens; they require no remote images or continuous animation.

### List performance

Lists render at most 12 records per page (five groups in the overview). Searches and filters still cover the complete collection and return to page one when changed. Pagination returns keyboard focus to the section heading. A container observer selects either a table or cards; the hidden alternative is no longer built. Easy reading always uses cards. Student/group joins and searchable member names are indexed when the workspace changes, while search input can update ahead of its filtered results.

Icons and Radix controls import only their used modules. The overview, library, public landing page, and file/detail windows load on demand; closed detail windows are unmounted, and PDF object URLs are released on close. Global search scans records only while open. Modal backdrops use a plain tint to avoid an extra blur pass.

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

The Node test suite covers status precedence, legacy membership lookup, atomic demo approval, duplicate review protection, required feedback, incomplete submissions, research types, CSV escaping, demo record consistency, display preference validation, theme resolution, reading presets, JSON persistence, and stronger-contrast text/control color pairs. `tests/fixtures/teacher-workspace-sample.pdf` is a harmless local PDF for testing upload and viewing.

To serve the built output locally:

```sh
npm run preview
```
