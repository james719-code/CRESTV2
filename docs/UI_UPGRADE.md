# Android UI branding upgrade

> Superseded layouts: see [the content and blank-state audit](UI_SCREEN_AUDIT.md) for the current revision and ADB evidence.

The Android app now uses CREST brand baseline 1.0.0. The existing repository reorganization and backend code were preserved.

## Material 3 alignment (latest)

Replaced the remaining custom "editorial" patterns with standard Android components, keeping the CREST colors, Inter type and shapes:

- Standard `NavigationBar` with the Material pill indicator; a `NavigationRail` replaces it at 600dp width and above.
- Standard `TopAppBar`: round CREST mark and screen title on main tabs, back arrow on secondary screens, round account avatar with a 48dp target.
- Filled, rounded search field instead of an underline.
- Home screens: plain greeting plus tonal metric cards; content is capped at 840dp and centered on wide screens.
- Research list no longer repeats the top bar title above search; cards are tighter.
- Sign in is a centered logo, wordmark, tagline and one Google button (no decorative background).
- Removed `EditorialComponents.kt` and the eyebrow/masthead/numbered-section styles.

Visual layer (`ui/components/VisualComponents.kt`):

- `BrandHero`: CREST blue to ink gradient card with a time-aware greeting, metric chips and an original book-stack illustration (Home and sign-in).
- `PaperCover`: strand-colored cover tile with a strand icon on every paper, group and Home row.
- `StepProgress`: Group → Submit → Review stepper on the student group card, with `AvatarStack` for members.
- `ActionTile`: icon tiles with counts for the teacher review queue and upload actions.
- `InitialsAvatar` for accounts, icon badges for documents, illustrated empty states with a tips panel.

Verified: 44 unit tests and 37 emulator tests pass.

## Editorial revision (superseded)

The first pass was replaced with a more distinctive library composition:

- Ink mastheads, an original geometric folio motif, restrained teal accents, and asymmetric folio corners.
- Publication-style research entries with catalogue numbers, larger titles, plain metadata, and ruled separators.
- A teacher review desk with clear publication/resource actions.
- A student group panel with a visible Group → Submit → Review journey and quieter library metrics.
- A typographic sign-in page and paper details laid out like a publication title page.
- Underlined search fields and compact blue navigation markers, preserving native selection semantics.

[Compare the first pass and editorial revision](ui-review/comparison.html). The earlier captures remain in `ui-review/first-pass/`.

## What changed

- Bundled Inter Regular, Medium, Semibold, and Bold, with the OFL notice distributed in app assets.
- Generated light/dark color schemes and typography, theme-aware status/category badges, and branded shapes.
- Stable, equally spaced navigation items with visible labels, native selected semantics, and a working offline notice.
- Full-width research rows with readable titles/authors, full research-type labels, download counts, and separate favorites/management actions.
- Search, filter summaries, result counts, and clear-filter recovery in one scrollable discovery layout.
- Student home prioritizes the group/submission state, allowed actions, and teacher feedback before library statistics.
- Teacher home prioritizes group reviews and research/document actions. The Groups shortcut switches the current pager; direct Groups navigation waits for the role to resolve.
- Simplified sign-in, splash, About, shared empty states, document/group headers, and submission styling.
- Research details show title, authors, metadata, and a Read paper action before PDF loading. The PDF retains its original typography.
- Upload forms respect keyboard insets and use restrained surfaces; the group form scrolls with large text.
- Existing launcher artwork and school/team attribution remain in place.

The branding generator updates the installed Kotlin token file as well as the shared reference and web CSS. Its drift check includes the Android copy.

## Screenshots

Captured on the Android 16 Pixel emulator using local fixtures, without signing into a school account or changing school data.

[Open the screenshot gallery](ui-review/index.html).

| Screen | Capture |
| --- | --- |
| Research, light | [Screenshot](ui-review/research-light.png) |
| Research, dark | [Screenshot](ui-review/research-dark.png) |
| Research, 200% text | [Screenshot](ui-review/research-large-text.png) |
| Student home | [Screenshot](ui-review/student-home.png) |
| Teacher home | [Screenshot](ui-review/teacher-home.png) |
| Research details before reading | [Screenshot](ui-review/paper-details.png) |
| Sign in | [Screenshot](ui-review/sign-in.png) |

## Validation

Completed on 2026-10-24:

- Debug APK and instrumented-test APK built successfully.
- All 39 JVM unit tests passed.
- All 28 Android emulator tests passed, including the two Groups-navigation regressions.
- Android lint completed with 0 errors and 150 warnings. The full report is in `android/app/build/reports/lint-results-debug.html`.
- Seven emulator captures were visually reviewed, including light/dark research and 200% text. Long titles wrap, navigation remains labeled, and content scrolls.
- All 70 brand contrast checks, six source font/license hashes, and generated-token drift checks passed.

Run from `android/`:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
```

UI checks cover stable navigation labels/selection, role-dependent Groups navigation, independent paper/favorite actions, search clearing, empty-filter recovery, student submission status, teacher review access, details-to-read, and sign-in loading/click behavior.

The build output is `android/app/build/outputs/apk/debug/app-debug.apk`.

Brand source checks, run from the repository root:

```powershell
node branding/scripts/generate.mjs --check
```

The checker verifies 70 defined contrast pairs (lowest text contrast 5.11:1), six source font/license hashes, and generated-file synchronization. This is a token-level check, not a complete accessibility certification.

## Manual checks still needed

Live Google sign-in, school-account permissions, actual uploads/reviews, PDF reading/downloads, offline cache transitions, and TalkBack should be checked with the deployment accounts and representative school data. The emulator interaction tests use fixtures and exercise the UI callbacks. Existing Android lint warnings remain available in `android/app/build/reports/lint-results-debug.html`.
