# Architecture

CREST uses a single-activity Android architecture with Jetpack Compose, MVVM, repositories, and Hilt dependency injection.

The Android Gradle project lives in `android/`, the React teacher website lives in `frontend/`, and shared documentation lives in `docs/`. Paths below are relative to the repository root.

## Teacher website

The website adapts the Android teacher workspace for desktop and smaller screens. React and TypeScript provide the application, Vite builds it, and shadcn/ui (Radix primitives and Tailwind CSS) provides its controls, tables, tabs, sheets, dialogs, and notifications. Shared generated CREST tokens and the bundled Inter font provide its identity.

`frontend/src/domain.ts` defines the normalized records and submission workflow. `data/useWorkspace.ts` provides either a clearly labeled browser-local demo or the school connection. With complete web configuration, `data/firebase.ts` verifies the signed-in user's approved teacher profile before subscribing to the existing Firestore collections. Teacher access remains observed throughout the session. The live connection never falls back to fictional records after an error.

Reviews use Firestore transactions to recheck teacher access and submission state. Approving a submission creates its library paper and updates the group together, preventing duplicate approvals. Requesting changes appends a comment and clears `uploaded`; the website retains the previous title and file for context, using fields Android already understands. Student progress is derived from group submission flags and feedback, rather than invented chapter percentages or activity dates.

File access goes through the storage adapter. CREST builds select Supabase; the
explicit Appwrite provider retains legacy access, and the active Supabase
provider calls Firebase-authenticated Supabase Edge Functions for signed file
operations. Appwrite copies preserve old IDs through a private Supabase mapping.
Files remain retained when metadata changes or saving fails. The existing website
form accepts PDFs up to 5 MB and validates the header. Demo uploads stay in
IndexedDB; demo metadata stays in localStorage. Live school records are not
persisted in demo storage. See [backend setup](BACKEND_SETUP.md).

Firebase security rules protect Firestore records. The deployed Supabase Edge
Functions verify Firebase sessions and current school roles for file access and
FCM sending; Appwrite permissions still govern legacy access. The website's role
gate adds UI protection. Netlify serves static web files only.

`DisplayProvider` supplies browser-local reading and appearance preferences across the public site, sign-in, workspace, dialogs, and notifications. `preferences.ts` validates persisted choices and applies the initial theme before React renders. Light/dark use the shared generated brand colors; optional stronger contrast supplies additional text and outline values. Easy reading uses rem-based larger text, roomy controls, card layouts, and contextual guidance, independently of theme. Narrow content areas adapt from tables to cards. The mobile navigation locks background interaction and manages keyboard focus while open. These settings never change account roles or the research workflow.

## Layers

### UI

Package: `android/app/src/main/java/com/bdbshs/crest/ui`

- Compose screens own layout and user interaction.
- Reusable components live under `ui/components` and `ui/screens/common`.
- ViewModels expose screen state and actions.
- Navigation is centralized in `navigation/CrestApp.kt`.

### Domain

Package: `android/app/src/main/java/com/bdbshs/crest/domain`

- Use cases hold reusable business operations that should not belong to a composable.
- Domain code should not depend on Android UI classes.

### Data

Package: `android/app/src/main/java/com/bdbshs/crest/data`

- Repositories coordinate Firebase, Appwrite, DataStore, and local cache operations.
- `FirebaseClient` and `AppwriteClient` isolate third-party SDK setup.
- `CrestConfig.kt` keeps shared collection and storage identifiers in one place.

## Runtime Flow

```text
Compose screen -> ViewModel -> Use case or repository -> Firebase/Appwrite/DataStore
```

Screens should observe immutable state and send user events to ViewModels. ViewModels should convert repository results into UI state and avoid direct SDK calls.

## Backend Responsibilities

| System | Responsibility |
| --- | --- |
| Firebase Authentication | Google Sign-In and authenticated user identity |
| Cloud Firestore | User roles, research metadata, groups, favorites, approval status |
| Supabase Storage | Private PDF storage and verified Appwrite mappings |
| Supabase Edge Functions | Deployed file authorization; messaging endpoint disabled |
| Firebase Cloud Messaging | Prepared Android/web school updates, disabled by default |
| Netlify | Prepared static teacher website hosting |
| Appwrite Storage | Migration source and explicit legacy provider |
| DataStore | Local user/session preferences |
| File cache | Local cached PDF/document files |

## Testing Strategy

- Unit tests cover ViewModels, repositories where isolated, use cases, and local utilities.
- Compose UI tests cover important user flows and screen state.
- Macrobenchmarks cover startup, scrolling, authentication, document loading, and search paths.

## Design Principles

- Keep business rules out of composables.
- Keep SDK-specific code in the data layer.
- Prefer explicit UI state objects over ad hoc state spread across a screen.
- Treat approval, role, and storage flows as product-critical paths.
- Add tests around permission and approval changes before broad refactors.
