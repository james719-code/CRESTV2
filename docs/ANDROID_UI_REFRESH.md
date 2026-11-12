# Android UI and usability refresh

November 8, 2026

This update improves browsing research and the shared components used by student and teacher Home. It retains CREST's existing branding and role-based navigation.

## Changes

- Search has a defined outline, a clear focus state, more space for titles and author names, and an explicit “Clear search” accessibility label. Recent suggestions return when a focused query is cleared.
- A labeled “Filter & sort” control exposes filtering without requiring users to recognize an icon. Selected strand chips show a checkmark. Results have a heading, count, and readable sorting description.
- Filter choices remain a draft until Apply. Cancel, Back, and outside dismissal discard the draft. Reset clears the draft's type, strands, and sorting while preserving the search query and Favorites selection.
- Research cards use consistent borders, strand colors, full-width titles and authors, and a visible “View paper” action. Favorite and teacher management controls remain independent. Enlarged text can show the complete title and author list. Cards without a management action no longer expose an empty long-press action.
- Home summary counts wrap into separate readable items. Paired actions stack with large text or less than 320dp of available width.
- The debug review screen now exercises the real filter sheet and filtering rules with local sample papers.
- Android lint's certificate line-breaking constant error is corrected with an equivalent constant selected for each supported Android version.

## Validation

- Debug app and instrumented-test APK builds passed.
- All 82 unit tests passed, including draft cancellation, atomic application, and scoped reset regressions.
- 41 emulator UI tests passed across `BrandUiTest`, `CollectionStateUiTest`, and `ThemeInteractionTest`. These cover light/dark themes, 200% text, search recovery, independent paper actions, collection recovery, Home actions, and form states.
- A final targeted document-filter UI test passed after the shared accessibility-label adjustment.
- The local walkthrough verified Cancel preserves results and Apply updates the results and filter count. Light/dark, teacher/student Home, and 320dp previews were reviewed. The filter sheet was additionally checked using Android's actual 200% font setting; Cancel and Apply remain visible while options scroll. Emulator display and font settings were restored.
- Android lint: zero errors, 133 existing warnings.
- `git diff --check` passed.

The walkthrough uses local fixtures without school-data writes. Build logs remain in ignored `android/ui-refresh-*.log` files; UI test results and screenshots are in ignored `artifacts/ui-refresh/`.

## Review locally

After installing the debug APK on an emulator:

```powershell
adb shell am start -n com.bdbshs.crest/.debug.UiReviewActivity --es page research --es mode data
```

Use `--ez dark true` for dark mode and `--ef fontScale 2.0` for 200% text. The review activity is only included in debug builds.
