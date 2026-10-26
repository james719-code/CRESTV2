# Android optimization and phone verification

This records the initial optimization pass. For the later complete light/dark
page audit, additional fixes, and current installed APK identity, see
[Android UI audit](ANDROID_UI_AUDIT.md).

Verified on October 26, 2026, on the connected realme RMX3834 running Android 15
at 720 × 1600 pixels, with the default font size.

## Changes

- Removed the fixed 1,200 ms splash delay and the additional 500 ms fade. App
  initialization starts immediately; the branded loading screen follows actual
  account loading instead of a timer.
- Made UI state collection lifecycle aware across the activity, navigation, and
  application screens, so background activities stop collecting UI updates.
- Shared the account ViewModel across main destinations to avoid duplicate
  account lookups, status listeners, and preference writes.
- Removed explicit adjacent-tab preloading to reduce initial screen work.
- Loaded qualitative and quantitative research pages concurrently. Refresh
  cancels stale requests, pagination removes overlapping paper IDs, and offline
  scrolling does not start additional page requests.
- Corrected the pagination trigger to use the actual list boundary, including
  headers and filter controls. Initial searches and clearing a query no longer
  incur the typing debounce.
- Moved PDF cache reads and writes in the download path to the IO dispatcher.
- Placed Google sign-in before the introductory feature list and reduced the
  brand header so the primary action is visible without scrolling on this phone.
- Gave paper titles and authors the full card width, reduced the decorative
  cover, and increased favorite/manage touch targets to 48 dp.
- Opened research filters fully so Apply Filters is visible immediately.

## Verification

| Check | Result |
| --- | --- |
| JVM unit suite | 47 passed, 0 failures |
| On-phone instrumented suite | 24 passed, 0 failures, 40.005 seconds |
| Release build | Successful, R8 and resource shrinking enabled |
| Release lint checks | Passed as part of assembleRelease |
| APK signing verification | Passed |
| Installed package | com.bdbshs.crest, version 1.0, non-debuggable release |
| Final APK size | 5,757,429 bytes (5.49 MiB) |
| Final release launch | 848 ms to initial activity display; one observation, not a comparative benchmark |

Instrumented tests exercised navigation and selection, independent card actions,
light and dark themes, enlarged text, teacher and student home screens, account
and document empty/error recovery, submission validation, sign-in loading and
missing-account states, and research-detail loading failures. The instrumented
suite used the debug test harness for the same source; the release was restored
afterward and checked separately on the phone.

Manual release checks covered successful Google sign-in, Home, Research,
Documents, Groups, unmatched search and recovery, and applying the qualitative
filter (12 papers reduced to 2). The full filter sheet displayed Apply Filters
within the visible screen. An existing 53-page research PDF loaded successfully,
and swiping advanced from page 1 to page 2. No AndroidRuntime error output was
observed for the final release process during these checks.

Screenshots, UI hierarchies, and the raw instrumented results are stored locally
under `artifacts/android-review/`, which is ignored by Git. The final library
capture is `artifacts/android-review/final-research.png`.

## Installed build and signing

The original release keystore password was unavailable. With explicit approval,
the original installation was removed, and this release was signed with the
existing local Android development certificate already registered for Google
sign-in. The user signed in again; subsequent test/release replacements retained
that session. The final installed app is the optimized, non-debuggable release,
not the temporary debug test build. Production signing configuration was not
changed.

APK: `android/app/build/outputs/apk/release/app-release.apk`

SHA-256: `8c2bd48bbb3e078a0db5da35230b3ee6be85277d7d6cb8d91cc6c9e9aa21f5f0`

This signing identity cannot update installations signed with the original
production key. Store distribution requires the appropriate production signing
setup.
