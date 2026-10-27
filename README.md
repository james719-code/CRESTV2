<div align="center">

# CREST

**Compiled Research as an Educational Application for Students and Teachers**

<p>
  <img src="https://img.shields.io/badge/Android-24%2B-3DDC84.svg?logo=android&logoColor=white" alt="Android 24+" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg?logo=kotlin&logoColor=white" alt="Kotlin 2.0" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose Material 3" />
  <img src="https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-DD2C00.svg?logo=firebase&logoColor=white" alt="Firebase Auth and Firestore" />
  <img src="https://img.shields.io/badge/Appwrite-Storage-F02E65.svg?logo=appwrite&logoColor=white" alt="Appwrite Storage" />
  <img src="https://img.shields.io/badge/Benchmarks-Macrobenchmark-0F9D58.svg" alt="Macrobenchmark" />
</p>

</div>

CREST is an Android e-library for Bonifacio D. Borebor Sr. High School. It gives students and teachers a central place to upload, approve, search, read, and preserve research papers and school documents.

The app is built as a production-style Android project: Kotlin, Jetpack Compose, Material 3, MVVM, Hilt, Firebase Authentication, Cloud Firestore, Appwrite Storage, unit tests, instrumented UI tests, and macrobenchmarks.

## Highlights

- Google Sign-In with student and teacher roles
- Teacher approval workflow for student research uploads
- PDF upload, metadata capture, storage, and in-app viewing
- Research search, filtering, sorting, favorites, and detail views
- Student group management for collaborative submissions
- Teacher account and storage management screens
- Dependency injection with Hilt and repository-backed ViewModels
- Unit, instrumented, and macrobenchmark test modules

## Repository Layout

```text
android/           Android Gradle project, app, and macrobenchmarks
frontend/          React + TypeScript teacher website, built with shadcn/ui
branding/          Shared identity, Inter fonts, design tokens, and platform guidance
docs/              Shared architecture and release documentation
```

Open `android/` in Android Studio and run Gradle commands from that directory. Repository configuration, including GitHub workflows, stays at the root.

## Architecture

CREST follows a single-activity Compose architecture with MVVM and repository boundaries.

```text
android/
  app/
    src/main/java/com/bdbshs/crest/
      data/        Firebase, Appwrite, DataStore, cache, repositories
      di/          Hilt modules
      domain/      Use cases for reusable business flows
      navigation/  Compose NavHost and routes
      ui/          Compose screens, components, theme, ViewModels
      utils/       Shared utility types
  benchmark/       Macrobenchmark test module
  gradle/          Version catalog and wrapper
```

For more detail, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Shared Branding

The Android app uses the shared identity in [branding/README.md](branding/README.md): bundled Inter fonts, CREST blue, teal accents, and matching light/dark themes. One token source generates CSS and the app's Compose values. The existing launcher artwork is retained. See [the UI validation report](docs/UI_UPGRADE.md) for the implementation and checks.

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin 2.0 |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, repositories, use cases |
| Dependency injection | Hilt |
| Auth | Firebase Authentication with Google Sign-In |
| Database | Cloud Firestore |
| File storage | Private Supabase Storage; verified Appwrite file mappings |
| Server | Deployed Supabase Edge Functions with Firebase authentication |
| Notifications | Firebase Cloud Messaging prepared, disabled by default |
| Website hosting | Netlify static hosting prepared |
| Local storage | DataStore Preferences, local file cache |
| Testing | JUnit 4, MockK, Turbine, Espresso, Compose UI tests |
| Performance | Android Macrobenchmark |
| Build | Gradle Kotlin DSL, version catalog |

## Requirements

| Tool | Version |
| --- | --- |
| Android Studio | Ladybug 2024.2.1 or newer |
| JDK | 17 recommended |
| Android Gradle Plugin | 8.10.1 |
| Min SDK | 24 |
| Compile SDK | 35 for app, 36 for benchmark |

## Getting Started

Firebase authentication, Firestore rules/indexes, and private Supabase storage
are connected and deployed. The 12 existing PDFs have verified Supabase copies.
Android and Netlify build settings select the Supabase provider. Notifications
remain disabled. See [backend setup and deployment status](docs/BACKEND_SETUP.md).
Choose and authorize the website hostname before publishing the teacher site.

GitHub CI builds Android and web independently; documentation-only changes build
neither. See [CI/CD routing and credentials](docs/CI_CD.md) for setup and release
triggers.

1. Clone the repository.

```bash
git clone https://github.com/james719-code/CRESTV2.git
cd CRESTV2/android
```

2. Open the repository's `android/` directory in Android Studio.

3. Add Firebase configuration.

Place `google-services.json` in `android/app/` (relative to the repository root). Enable Firebase Authentication and Cloud Firestore in the Firebase project.

4. Add Appwrite configuration.

Create or update `android/local.properties` (relative to the repository root):

```properties
APPWRITE_ENDPOINT=https://fra.cloud.appwrite.io/v1
APPWRITE_PROJECT_ID=your_project_id
APPWRITE_BUCKET_ID=your_bucket_id
STORAGE_PROVIDER=appwrite
FCM_ENABLED=false
```

5. Sync Gradle and run the app.

```bash
./gradlew assembleDebug
```

Windows:

```bat
gradlew.bat assembleDebug
```

## Quality Gates

Run these from `android/` before opening a pull request:

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

Notes:

- `connectedDebugAndroidTest` requires a connected device or emulator.
- Macrobenchmarks should run on a physical device when possible.
- Release builds require signing environment variables. See [docs/RELEASE.md](docs/RELEASE.md).

## Configuration

### Firebase

The app expects a Firebase Android client for package `com.bdbshs.crest`. Keep the real `google-services.json` out of version control.

### Appwrite

Appwrite values are resolved in this order:

1. `android/local.properties`
2. Gradle properties passed with `-P`
3. Build defaults in `android/app/build.gradle.kts`

### Release Signing

For local builds, fill in `android/keystore.properties` using the tracked
`android/keystore.properties.example` template. This ignored file contains
`storeFile`, `storePassword`, `keyAlias`, and `keyPassword`. Relative keystore
paths start from `android/`. Keep the original release keystore for compatible
updates; do not substitute the development key for a published release.

Release builds also accept these environment variables, which take priority over
the local file so GitHub Actions continues to work:

| Variable | Description |
| --- | --- |
| `RELEASE_STORE_FILE` | Absolute keystore path, or a path relative to `android/` |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Signing key alias |
| `RELEASE_KEY_PASSWORD` | Signing key password |

## Repository Hygiene

- IDE state, build outputs, generated logs, local configs, signing keys, and Firebase files are ignored.
- Public docs live in `docs/`.
- Contribution expectations live in [CONTRIBUTING.md](CONTRIBUTING.md).
- Security reporting instructions live in [SECURITY.md](SECURITY.md).
- Release process lives in [docs/RELEASE.md](docs/RELEASE.md).

## Project Status

CREST is maintained for a real school deployment. The codebase is public-facing, but the production Firebase, Appwrite, and signing assets are intentionally private.

## Maintainer

James Ryan S. Gallego
