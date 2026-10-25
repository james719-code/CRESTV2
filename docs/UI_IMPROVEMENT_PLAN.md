# CREST Android UI Improvement Plan

Status: proposed plan; UI implementation has not started.

Shared brand values and Inter typography are now specified in [the branding kit](../branding/README.md). Use its canonical tokens when implementing this plan; the visual suggestions below describe the screen hierarchy and initial direction.

## Goal and scope

Make CREST feel like a clear, cohesive school research library. Students should quickly find papers and understand their submission status. Teachers should quickly reach review and management tasks.

This plan covers the Compose Android app in `android/`. `frontend/` currently contains only a placeholder README. Findings below come from source inspection, not a running-device visual audit. Start implementation by capturing representative current screens so layout assumptions can be checked.

## Findings from the current source

| Finding | Evidence | Planned response |
| --- | --- | --- |
| Inactive navigation destinations lose their visible labels, and selecting a tab changes its width. | `ui/components/BottomNavBar.kt`: selected-only text, animated item weights, custom click handling with no indication. | Use stable-width Material navigation items with persistent labels, selected semantics, and press feedback. Preserve existing role-based destinations. |
| Research browsing uses a staggered grid with a 160dp minimum card width. Long academic titles compete with badges, contributor avatars, and metrics. | `ui/screens/ResearchesScreen.kt`, `ui/components/ResearchCard.kt`. | Use full-width research rows on compact phones; consider a regular grid on wider windows after visual checks. Prioritize title, author, strand, and date. |
| Teacher home gives substantial space to a decorative gradient greeting, followed by quick actions and a statistics grid. | `ui/screens/TeacherHomeScreen.kt`: decorative circles, 38sp greeting, generous header spacing. | Reduce the greeting and put relevant work first. Avoid repeating the same counts in multiple sections. |
| Related UI has several implementations that can drift apart. | Shared research cards, local research cards, recent-research cards, and empty states in both `ui/components/` and `ui/screens/common/`. | Audit active call sites and converge on shared components with purposeful compact and expanded variants. |
| Theme foundations exist, but screens also define local shapes, gradients, colors, and sizes. | `ui/theme/`, login, teacher home, upload, and research card code. | Refine existing tokens and migrate the affected screens to them. |
| The minimum touch-target token is 44dp. | `ui/theme/Spacing.kt`. | Raise the minimum to 48dp and verify actual hit areas and separation. |
| Some visible actions need behavior verification alongside visual changes. | `ResearchDetailScreen.kt` has a retry button with a placeholder callback; research edit handling contains a future-edit comment. | Wire the intended action or remove misleading affordances. Track missing product behavior separately from cosmetic changes. |

## Proposed visual direction

- Retain CREST's blue identity with neutral backgrounds and restrained blue emphasis for primary actions and selection.
- Prefer readable content, flat surfaces, subtle dividers, and occasional cards for meaningful groups. Reduce decorative gradients, background circles, and heavy shadows.
- Use a small typography hierarchy: page title, section title, content title, body, and metadata. Start with 16sp body text and 14sp secondary text; verify font scaling and contrast before finalizing.
- Apply a consistent 4dp spacing scale: 16dp screen gutters on compact phones, 12–16dp component padding, and 24dp between sections.
- Start with 12–16dp component corners, reserving larger shapes for sheets and dialogs. Use shared theme roles for light and dark colors.
- Use plain labels such as “Research papers,” “Read paper,” and full research-type names instead of unexplained “QL” and “QN.”

These are initial design choices to validate in previews, not a requirement to replace every existing component.

## Implementation sequence

### 1. Establish a visual baseline and screen specification

Capture login, student home, teacher home, research list, research details/reader, upload, documents, and groups. Include empty, loading, error, and offline examples using preview fixtures where live data is unavailable.

Produce before/after mockups for Research and both Home variants first. Agree on page hierarchy, component styling, and primary actions by reviewing those concrete screens.

Deliverable: screenshot baseline, screen inventory, and three proposed layouts. No backend work is required for this step.

### 2. Refine the design system and navigation

Update the existing theme and create or consolidate reusable page headers, research rows, status badges, search/filter controls, form sections, and feedback states. Keep colors semantic and support both themes from the start.

Replace the expanding custom bottom navigation with stable labeled items. Keep Home, Research, Documents, and the existing teacher Groups destination. Preserve drawer destinations and role access. Use an adaptive rail on wider windows when supported by the project's dependencies.

Deliverable: component previews and a consistent app shell with checked system-bar, keyboard, and navigation insets.

### 3. Redesign research discovery and role-specific home screens

**Research:** place search first, then a compact filter/sort row and a result count. Show applied filters with an obvious clear action. Keep the existing filters and favorites behavior while avoiding repeated filter controls. Use full-width rows on phones, with readable titles, author information, strand/type metadata, and a separate favorite action. Keep edit/delete reachable through an explicit menu rather than only a hidden gesture.

**Student home:** use a compact greeting, followed by the student's group/submission status and the next valid action. Show teacher feedback clearly when present. Put recent papers below that, with supporting statistics given less visual weight. Preserve existing status and permission rules.

**Teacher home:** use a compact greeting, prioritize existing review work through a clear Groups entry, and keep upload actions easy to find. Put summary statistics and recent papers below the main tasks. If a dedicated review queue needs new queries or data, scope that separately.

Deliverable: complete browse-to-read and home-to-task flows using the shared components. This phase should provide the largest visible improvement.

### 4. Simplify submission, details, and supporting screens

**Uploads:** organize the form into file, research details, and contributors. Show selected filename, supported type/limits from the actual upload rules, inline validation, upload progress, failure recovery, and a clear completion state. Keep the primary action visible above the keyboard without covering fields. Introduce multiple steps only if a preview shows that grouping alone is insufficient.

**Details and reader:** make the title, authors, strand/type, and reading action available before loading the PDF. Keep reader controls compact, with accessible page navigation and clear loading/retry states. Avoid making metadata discoverable only through an information sheet after loading.

**Login:** retain the school identity and Google sign-in while simplifying decorative content and using shorter explanatory copy.

**Documents, groups, accounts, storage, signup, and approval screens:** apply the same headers, list rows, status badges, dialogs, and feedback patterns. Preserve the distinction between teacher and student actions.

Deliverable: a cohesive secondary-screen pass with actionable loading, empty, offline, success, and error states.

### 5. Validate and finish

- Check compact phone widths, landscape, a wider window, long titles/names, and large result sets.
- Check light/dark themes, default and 200% font scaling, TalkBack reading order, selected navigation semantics, focus order, and meaningful icon labels.
- Require at least 48dp touch targets. Measure text contrast; target 4.5:1 for normal text and 3:1 for large text.
- Verify search, filters, sorting, favorites, reading, uploads, group actions, approval access, and offline cached-paper access still work.
- Run the applicable existing unit and Compose navigation/screen tests, adding focused interaction coverage where behavior changes. Build the debug app. Use existing scrolling/document benchmarks if the changed lists or reader show performance concerns.
- Compare final screenshots with the baseline and resolve clipping, inconsistent spacing, misleading actions, and theme failures.

Deliverable: screenshot comparison and a short validation report that distinguishes passed checks from checks blocked by device, account, or data availability.

## Acceptance criteria

1. Every primary navigation destination remains visibly labeled and stable when selected.
2. On compact phones, research titles and authors have enough width to scan; longer titles can be read in details without truncation.
3. Student home clearly shows the current group/submission state and its next allowed action.
4. Teacher home makes the existing review destination and upload actions easy to locate.
5. Search and active filters are understandable, and an empty result offers a clear recovery action.
6. Upload forms explain invalid input, show progress, preserve editable input after recoverable failures, and make completion clear.
7. Loading, empty, error, and offline states follow a shared visual pattern and display working actions.
8. Core flows remain usable in both themes, with enlarged text and TalkBack, without controls being obscured by system bars or the keyboard.

## First implementation slice

Implement theme refinements, shared research rows, labeled navigation, and the Research screen together. Review that result before expanding the styling to both dashboards and the remaining screens. Keep each subsequent slice buildable and visually reviewable.

## Reference guidance

- [Android Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility)
- [Compose accessibility defaults and touch targets](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- [Android accessibility principles and contrast](https://developer.android.com/guide/topics/ui/accessibility/principles)
- [Build adaptive navigation](https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation)
