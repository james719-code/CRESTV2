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

## Architecture

CREST follows a single-activity Compose architecture with MVVM and repository boundaries.

```text
app/
  src/main/java/com/bdbshs/crest/
    data/          Firebase, Appwrite, DataStore, cache, repositories
    di/            Hilt modules
    domain/        Use cases for reusable business flows
    navigation/    Compose NavHost and routes
    ui/            Compose screens, components, theme, ViewModels
    utils/         Shared utility types
benchmark/         Macrobenchmark test module
gradle/            Version catalog and wrapper
```

For more detail, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin 2.0 |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, repositories, use cases |
| Dependency injection | Hilt |
| Auth | Firebase Authentication with Google Sign-In |
| Database | Cloud Firestore |
| File storage | Appwrite Storage |
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

1. Clone the repository.

```bash
git clone https://github.com/james719-code/CRESTV2.git
cd CRESTV2
```

2. Open the project in Android Studio.

3. Add Firebase configuration.

Place `google-services.json` in `app/`. Enable Firebase Authentication and Cloud Firestore in the Firebase project.

4. Add Appwrite configuration.

Create or update `local.properties`:

```properties
APPWRITE_ENDPOINT=https://fra.cloud.appwrite.io/v1
APPWRITE_PROJECT_ID=your_project_id
APPWRITE_BUCKET_ID=your_bucket_id
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

Run these before opening a pull request:

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

1. `local.properties`
2. Gradle properties passed with `-P`
3. Build defaults in `app/build.gradle.kts`

### Release Signing

Release builds read signing values from environment variables:

| Variable | Description |
| --- | --- |
| `RELEASE_STORE_FILE` | Path to the keystore file |
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
