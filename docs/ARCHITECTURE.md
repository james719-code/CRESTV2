# Architecture

CREST uses a single-activity Android architecture with Jetpack Compose, MVVM, repositories, and Hilt dependency injection.

## Layers

### UI

Package: `app/src/main/java/com/bdbshs/crest/ui`

- Compose screens own layout and user interaction.
- Reusable components live under `ui/components` and `ui/screens/common`.
- ViewModels expose screen state and actions.
- Navigation is centralized in `navigation/CrestApp.kt`.

### Domain

Package: `app/src/main/java/com/bdbshs/crest/domain`

- Use cases hold reusable business operations that should not belong to a composable.
- Domain code should not depend on Android UI classes.

### Data

Package: `app/src/main/java/com/bdbshs/crest/data`

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
| Appwrite Storage | PDF and document file storage |
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
