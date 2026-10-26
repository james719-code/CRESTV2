# Android UI audit — October 26, 2026

Device: realme RMX3834, Android 15, 720 × 1600 pixels, default text size.

This follow-up reviewed every reachable page layout in light and dark mode,
including the ends of long forms, and exercised the signed-in teacher flows on
the phone. Student and registration screens used local sample state so the audit
could test their UI without registering accounts or changing school groups.

## Coverage

| Page or layout | Light | Dark | Evidence and interaction scope |
| --- | --- | --- | --- |
| Teacher Home | Reviewed | Reviewed | Live counts, review links, upload actions; sample populated state |
| Student Home | Reviewed | Reviewed | Local group/submission state, feedback, recent papers; create/join dialogs with mocked actions |
| Research library | Reviewed | Reviewed | Live papers, search, empty search recovery, filters, card actions |
| Favorites | Reviewed | Reviewed | Live empty state and locally populated favorites |
| School documents | Reviewed | Reviewed | Live empty state, sort sheet, populated local documents |
| Research groups | Reviewed | Reviewed | Live groups and filters; local pending/approved cards |
| School accounts | Reviewed | Reviewed | Live pending accounts, filters, review/cancel; local approved state |
| Research overview | Reviewed | Reviewed | Titles, authors, metadata, reading action |
| Group details | Reviewed | Reviewed | Live unsubmitted groups and members; local submitted paper/review controls |
| Storage | Reviewed | Reviewed | Live cached PDF, selection, delete confirmation and cancellation |
| Student research submission | Reviewed | Reviewed | Local form, selected file, incomplete submission validation |
| Teacher research upload | Reviewed | Reviewed | Live typing, dropdowns, PDF picker/cancellation; local complete form and file removal |
| School role selection | Reviewed | Reviewed | Student and teacher cards |
| Student registration | Reviewed | Reviewed | Full form and its action, including bottom of the page |
| Teacher registration | Reviewed | Reviewed | Name/email fields and registration action |
| Google sign-in | Reviewed | Reviewed | Brand, visible sign-in action, loading/disabled and missing-account recovery |
| Pending approval | Reviewed | Reviewed | Status, guidance, sign-out action; themed background |
| About | Reviewed | Reviewed | Header, school/team sections, bottom/version, scrolling |
| Profile and appearance | Reviewed | Reviewed | Full menu, theme selection, selection semantics, safe status-bar spacing |

The live audit additionally covered the PDF reader, upload-document dialog,
keyboard-open forms, research/document/group/account filters, account-review
cancellation, and storage confirmation dialogs. These checks did not approve,
deny, upload, or delete real school records.

## Problems found and fixed

1. **Home could return to Groups.** Opening Accounts from Groups and selecting
   Home restored the old pager page. Selecting a main tab from a secondary page
   now opens the requested tab. A navigation regression test covers both themes.
2. **Storage text was squeezed into narrow columns.** Dates could wrap one
   character per line. Saved paper titles and dates now use the card width, with
   file size and selection kept in a separate header. The page title also fits
   beside its toolbar actions, and a single file is labeled correctly.
3. **Delete overlapped Android's system buttons.** The selection bar did not
   reserve the system navigation inset. The button now sits above that area;
   device tests verify its bounds and that tapping it reaches the app. Live
   confirmation/cancellation was checked without removing the cached PDF.
4. **Navigation and panels faded or swept across the screen.** Disabled NavHost
   crossfades, removed bottom-bar fades, switched tab taps directly to their
   destination, and removed decorative fades/scaling from search, registration,
   student-group updates, storage selection, loading placeholders, and upload
   success. Direct gestures and functional progress indicators remain available.
5. **Profile controls were hidden by partial expansion.** The profile menu opens
   fully, reserves status-bar space, scrolls when needed, and exposes selected
   appearance options to accessibility services.
6. **System icons could stay white in light mode.** Theme handling now unwraps
   activity contexts and reapplies the chosen icon appearance when the window
   regains focus after sheets or external activities. Separate dialog and sheet
   windows also use the selected app theme instead of inheriting the device's
   system theme. This uses Android's
   [system-bar appearance controller](https://developer.android.com/reference/androidx/core/view/WindowInsetsControllerCompat).
7. **Document upload advertised unsupported formats.** The picker and hint now
   agree with the upload service: PDF, up to 20 MB. The dialog scrolls with the
   keyboard open, and the remove-file target is 48 dp.
8. **Account review had no explicit Cancel action.** Approval and denial are now
   separate from cancellation. Tests verify Cancel calls neither access-changing
   action in either theme.
9. **Some supporting UI needed clearer contrast and spacing.** Feedback actions
   use their container's content color, disabled review actions retain their
   disabled appearance, document covers leave more title space, the app supplies
   a themed root background, and startup has a dark background resource.

## Evidence

Local screenshots and UI hierarchies are in
`artifacts/android-review/theme-audit/` (ignored by Git):

- `light-<page>-data.png` and `dark-<page>-data.png`: all 19 layouts, using sample data.
- `*-bottom.png`: the ends of long forms and scrolling pages.
- `sheet-light-*.png` and `sheet-dark-*.png`: visual review contact sheets.
- `live-light-*.png`: live checks and findings before the final corrections.
- `final-light-*.png` and `final-dark-*.png`: signed-release verification.
- `contrast-checks.json`: 22 standard theme text/background pairs; all exceed
  4.5:1, with the lowest checked ratio 5.45:1.
- `final-interaction-results.txt`: the main phone test run and regression tests.

The first PDF-picker handoff produced a one-off ANR in the phone's system Files
app (`com.google.android.documentsui`, no focused window). CREST returned with
the draft intact after that process was closed. Retrying opened the picker
normally, its intent was verified as `application/pdf`, and cancellation returned
to the form. No Files app settings or data were reset.

## Build and signing

The final build uses R8 and resource shrinking and is non-debuggable. As agreed
earlier, phone builds use the existing local signing certificate registered for
Google sign-in because the original release-key password is unavailable.
Subsequent installations preserve the user's session. Production distribution
still requires the appropriate production signing setup.

## Final validation

| Check | Result |
| --- | --- |
| Unit tests | 47 passed, 0 failures |
| Phone UI/regression tests | 51 passed, 0 failures; 75.415 seconds |
| Page layout review | 19 layouts × 2 themes, plus long-page bottom captures |
| Theme contrast | 22 checked pairs passed; lowest ratio 5.45:1 |
| Device regression coverage | Home return, storage layout and selection, safe Delete bounds/tap, account Cancel, PDF removal, student create/join validation, activity and sheet system icons |

The instrumented suite uses a temporary debug test harness for the same source.
The signed, optimized release is then restored and checked separately on the
phone. The original sign-in session is retained during these replacements.

APK identity and the last release smoke-check results are recorded below.

### Installed release

- APK: `android/app/build/outputs/apk/release/app-release.apk`
- Version: 1.0 (1), 5,758,617 bytes (5.49 MiB).
- SHA-256: `7b8d40b7f1d374dde98abf764557350b806c7c1220255fd970dfa31b5ebb8508`.
- The installed APK has the same SHA-256 as the built artifact. Signature
  verification passed, the installed package has no `DEBUGGABLE` flag, and the
  debug-only review activity is absent.
- Release lint and the optimized release build passed.

### Last phone checks

The final release retained the signed-in session. Live Home, account-menu theme
selection, storage selection and delete cancellation, account-review cancellation,
and the PDF reader were checked again. The cached 53-page PDF opened in both
themes, its details panel remained readable, and swiping advanced from page 2 to
page 3. Status and navigation icons were checked in light and dark app windows
and in the profile sheet.

The final navigation pass visited Documents, Groups, Research, and Home, cleared
the temporary search query, and recorded the direct tab changes in
`artifacts/android-review/theme-audit/navigation-motion.mp4`. The final release
process produced zero `AndroidRuntime` error lines during this check; the saved
log is `final-release-runtime.txt` in the same evidence folder.

Appearance was restored to **System**, and the phone was left at Home. The
temporary test package and audit files on shared phone storage were removed;
the installed app, login, and cached paper were retained.

Coverage limits: student and registration interactions used local sample state
and mocked actions, rather than a second live account. Approval, denial, actual
uploads, and deletion of school records were not exercised against live data.
The one-off system Files picker hang described above remains an observed device
event; its retry succeeded.
