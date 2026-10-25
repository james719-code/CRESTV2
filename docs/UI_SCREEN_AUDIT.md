# CREST UI screen audit and ADB walkthrough

This revision addresses missing information and misleading blank/loading states. The large research masthead and oversized editorial rows were removed so research content appears sooner.

## What was actually run

- Launched the installed application through ADB on the Android 16 Pixel emulator (1344 × 2992).
- Attempted the real Google sign-in entry point. The provider returned `NoCredentialException: No credentials available`.
- Did not bypass production authentication or create school records.
- Used `src/debug/.../UiReviewActivity.kt` to navigate the production content composables with clearly labeled local data.
- Navigated by bounds read from UI Automator, verified visible text after transitions, and saved screenshots and XML under `artifacts/ui-audit/`.
- The review activity is absent from the merged release manifest.

[Open the screen-state gallery](../artifacts/ui-audit/index.html).

## Problems corrected

| Finding | Result |
| --- | --- |
| Groups listener discarded errors and never stopped loading | Loading and refresh indicators stop; a persistent error and retry appear |
| Missing or failed paper lookups could load forever | Both collections resolve into paper details or a recoverable unavailable state |
| Storage exceptions could leave a loading page | Storage failures show a persistent retry state |
| Dashboard data projections lost authors, research type, downloads, and work-in-progress metadata | Existing fetched fields are retained; no additional queries were added |
| Student group panel omitted the submitted paper title and actual member names | Title, type, member names, status, group ID, and feedback appear together |
| Large banners displaced research information | Compact headings and substantive research cards show more content above the fold |
| Teacher Home hid useful counts behind decorative sections | Pending papers/accounts and school totals appear before recent research |
| Accounts defaulted to Pending without explaining an empty list | Visible Pending/Approved counts and a working “View approved accounts” action |
| Document and account errors resembled an empty collection | Failures stay visible and preserve already-loaded content |
| Legitimately empty lists gave little guidance | Context-specific explanations and working refresh/filter/navigation actions |
| The file picker claimed an unenforced 25 MB limit | The unsupported limit was removed |
| A selected file was labeled “Ready for submission” before the form was valid | File state and whole-form readiness are now separate |
| File selection used a fixed 140dp panel | A compact, expanding file card shows the filename and change/remove controls |
| Missing Google credentials produced only a generic canceled/failed message | A specific explanation and account-settings recovery action |
| Registration role cards could clip enlarged content | Removed the maximum-height cap and made the screen scroll |
| Registration name/email inputs were lost on recreation | Inputs use saved Compose state |

## Screen coverage

| Screen | Reviewed states or behavior |
| --- | --- |
| Real sign-in | Launch; missing Google account; recovery explanation |
| Main navigation | Home, Research, Documents, Groups; teacher account shortcut |
| Research | Populated, loading, error, filter recovery; dark theme; 200% text |
| Favorites | Empty shelf; independent favorite action |
| Teacher Home | Populated, empty, and error; review/account shortcuts |
| Student Home | Submitted paper, member names, feedback; no group; loading; error |
| Documents | Populated, empty, error, retry recovery |
| Groups | Populated; pending filter; empty; error |
| Accounts | Pending; approved; empty pending/approved; switch to approved; error |
| Paper details | Complete metadata; loading/error paths and retry |
| Group details | Submission and members; empty/error paths |
| Storage | Saved files; no local PDFs; error |
| Student submission | Empty, selected PDF, error, and readiness guidance |
| Teacher upload | Empty and populated fields, contributors, and file selection |
| Registration | Role selection, student form, teacher form |
| About | Product, school, and team attribution |
| Pending approval | Approval explanation and sign-out |
| PDF readers | Existing renderer preserved; live PDF integration requires a signed-in school account |
| Legacy drawer/helpers | Not separate active navigation destinations |

## Validation

The debug app and test APK build successfully. Regression coverage includes:

- Stopping Groups loading indicators after a listener failure and clearing the error on successful refresh.
- Resolving missing/failed paper detail lookups.
- Preserving authors, quantitative/qualitative type, downloads, submission title, and feedback in dashboard mappings.
- Recovering Documents after an error.
- Moving from an empty Pending accounts view to approved accounts.
- Showing a group's paper title, member count, and review state.
- Keeping paper metadata visible after a read failure.
- Explaining missing Google credentials.
- Disabling incomplete submission and explaining requirements.
- Explaining empty offline storage.

Build and lint logs are kept in ignored `android/ui-audit-*.log` files. Android lint completes with zero errors; warnings remain in `android/app/build/reports/lint-results-debug.html`.

## Review commands

From the repository root after building the debug APK:

```powershell
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.bdbshs.crest/.debug.UiReviewActivity --es page research --es mode data
```

The Screens menu includes the major pages. Data, Empty, Loading, and Error select local review states. This activity exists only in the debug source set.

The actual account flows still need a Google account and school access on the emulator. Uploads, approvals, account changes, and live PDF downloads were not executed against school data.
