<div align="center">

# CREST

**Compiled Research as an Educational Application for Students and Teachers**

![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-DD2C00?logo=firebase&logoColor=white)
![Appwrite](https://img.shields.io/badge/Appwrite-Storage-F02E65?logo=appwrite&logoColor=white)
![License](https://img.shields.io/badge/License-Private-lightgrey)

</div>

---

CREST is a mobile platform built for **Bonifacio D. Borebor Sr. High School** that gives educators and students centralized access to compiled research papers and educational documents. It streamlines how academic resources are organized, discovered, and shared.

> **Why does this exist?** There was no electronic backup for research at our school. CREST makes research easily available to every student, fostering collaborative learning and efficient knowledge sharing.

---

## Table of Contents

- [CREST](#crest)
  - [Table of Contents](#table-of-contents)
  - [Features](#features)
    - [Authentication \& Roles](#authentication--roles)
    - [Research Management](#research-management)
    - [Discovery \& Viewing](#discovery--viewing)
    - [Groups \& Collaboration](#groups--collaboration)
    - [Administration](#administration)
  - [Architecture](#architecture)
  - [Tech Stack](#tech-stack)
  - [Project Structure](#project-structure)
  - [Requirements](#requirements)
  - [Getting Started](#getting-started)
    - [1. Clone the repository](#1-clone-the-repository)
    - [2. Open in Android Studio](#2-open-in-android-studio)
    - [3. Configure services](#3-configure-services)
    - [4. Sync and build](#4-sync-and-build)
  - [Configuration](#configuration)
    - [Firebase](#firebase)
    - [Appwrite](#appwrite)
    - [Release Signing](#release-signing)
  - [Build \& Run](#build--run)
  - [Build Variants](#build-variants)
  - [Testing](#testing)
    - [Unit Tests](#unit-tests)
    - [Instrumented Tests](#instrumented-tests)
    - [Benchmarks](#benchmarks)
    - [Running Tests in Android Studio](#running-tests-in-android-studio)
  - [Troubleshooting](#troubleshooting)
  - [Developer](#developer)

---

## Features

### Authentication & Roles
- **Google Sign-In** — Secure login using institutional or personal Google accounts
- **Role-based access** — Distinct interfaces and permissions for **Students** and **Teachers**
  - **Students** — Browse research, view details, favorite papers, and upload group research for approval
  - **Teachers** — Browse research, upload materials, manage student submissions (approve / deny), and administer accounts

### Research Management
- **Centralized repository** — Single source of truth for all school research papers
- **Structured uploads** — PDF upload form with metadata (title, strand, research type, authors)
- **Teacher approval workflow** — Student uploads enter a "Pending" state until a teacher approves them
- **Favorites** — Bookmark research papers for quick access

### Discovery & Viewing
- **Search & filter** — Find research by title, status (Pending / Accepted), or sort alphabetically
- **Integrated PDF viewer** — Lazy-loading renderer for smooth scrolling of large documents, directly in-app
- **Responsive UI** — Optimized for phones and tablets

### Groups & Collaboration
- **Student groups** — Organize students into groups for collaborative research uploads
- **Group detail views** — Manage group membership and track submissions

### Administration
- **Account management** — Teachers can view and manage user accounts
- **Storage management** — Monitor and manage cloud storage usage

---

## Architecture

CREST follows **MVVM + Repository** with a single-activity Compose architecture:

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer                            │
│  Screens (Compose) ──► ViewModels ──► UI State (Flow)  │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                    Data Layer                           │
│               13 Repository classes                     │
│    ┌──────────────┬──────────────┬──────────────┐       │
│    │   Firebase    │   Appwrite   │    Local     │       │
│    │  Auth + DB    │   Storage    │ FileCache /  │       │
│    │  (Firestore)  │  (Documents) │  UserPrefs   │       │
│    └──────────────┴──────────────┴──────────────┘       │
└─────────────────────────────────────────────────────────┘
```

- **Single Activity** (`MainActivity`) hosts a Compose `NavHost` with 15+ routes
- **Hilt** provides dependency injection across the entire app
- **Dual backend** — Firebase handles auth & Firestore; Appwrite handles file storage
- **Kotlin Coroutines & Flow** for reactive, asynchronous data streams

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| **Language** | [Kotlin 2.0](https://kotlinlang.org/) |
| **UI** | [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material Design 3](https://m3.material.io/) |
| **Architecture** | MVVM + Repository pattern |
| **Navigation** | [Compose Navigation](https://developer.android.com/jetpack/compose/navigation) |
| **DI** | [Hilt](https://dagger.dev/hilt/) (Dagger) |
| **Auth** | [Firebase Authentication](https://firebase.google.com/docs/auth) (Google Sign-In) |
| **Database** | [Cloud Firestore](https://firebase.google.com/docs/firestore) |
| **File Storage** | [Appwrite SDK for Android](https://appwrite.io/docs/sdks#android) |
| **Async** | Kotlin Coroutines & Flow |
| **Local Storage** | [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) + custom file cache |
| **Testing** | JUnit 4, [MockK](https://mockk.io/), [Turbine](https://github.com/cashapp/turbine), Espresso, Compose UI tests |
| **Benchmarking** | [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview) |
| **Build** | Gradle (Kotlin DSL) with version catalog |

---

## Project Structure

```
CREST/
├── app/
│   └── src/main/java/com/bdbshs/crest/
│       ├── CrestApplication.kt          # @HiltAndroidApp — init Firebase, Appwrite
│       ├── MainActivity.kt              # Single-activity entry point
│       ├── data/
│       │   ├── AppwriteClient.kt         # Appwrite SDK wrapper
│       │   ├── FirebaseClient.kt         # Firebase SDK wrapper
│       │   ├── CrestConfig.kt            # App configuration constants
│       │   ├── FileCache.kt              # Local file caching
│       │   ├── UserPrefs.kt              # DataStore preferences
│       │   └── repository/               # 13 repositories (Auth, Research, Groups, …)
│       ├── di/
│       │   └── AppModule.kt              # Hilt @Module for app-wide dependencies
│       ├── navigation/
│       │   └── CrestApp.kt              # NavHost, routes, navigation actions
│       ├── ui/
│       │   ├── components/               # 8 reusable composables (NavBar, Cards, …)
│       │   ├── screens/                  # 18 screens + shared composables
│       │   │   ├── common/               # ActionBottomSheet, EmptyState, Shimmer, …
│       │   │   └── drawer/               # Navigation drawer
│       │   ├── viewmodels/               # 14 ViewModels
│       │   └── theme/                    # Material 3 theme (Color, Type, Shapes, …)
│       └── utils/
│           └── FileUtils.kt             # File utility helpers
├── benchmark/                            # Macrobenchmark module
├── gradle/
│   └── libs.versions.toml               # Version catalog
├── build.gradle.kts                      # Root build file
└── settings.gradle.kts
```

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Android Studio | Ladybug (2024.2.1) or newer |
| JDK | 11 |
| Compile SDK | 35 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 |
| Firebase | Auth + Firestore enabled |
| Appwrite | Endpoint, project, and bucket configured |

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/james719-code/CRESTV2.git
cd CRESTV2
```

### 2. Open in Android Studio

1. Launch Android Studio
2. **File > Open** — navigate to the cloned folder and click **OK**
3. Wait for Gradle sync to complete (may take a few minutes on first import)

### 3. Configure services

See [Configuration](#configuration) below for Firebase, Appwrite, and signing setup.

### 4. Sync and build

- Click **Sync Project with Gradle Files** (elephant icon in the toolbar)
- **Build > Make Project** to verify everything compiles

---

## Configuration

### Firebase

Place your `google-services.json` in the `app/` directory.  
Download it from the [Firebase Console](https://console.firebase.google.com/) — ensure **Authentication** and **Firestore** are enabled in your project.

### Appwrite

The app resolves Appwrite values in this order: `local.properties` → Gradle properties → fallback defaults.

Add to `local.properties`:

```properties
APPWRITE_ENDPOINT=https://fra.cloud.appwrite.io/v1
APPWRITE_PROJECT_ID=your_project_id
APPWRITE_BUCKET_ID=your_bucket_id
```

### Release Signing

Release builds read signing config from environment variables (used by CI / GitHub Actions):

| Variable | Description |
|----------|-------------|
| `RELEASE_STORE_FILE` | Path to the keystore file |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |

---

## Build & Run

All commands are run from the project root.

| Task | Windows | macOS / Linux |
|------|---------|---------------|
| Debug build | `gradlew.bat assembleDebug` | `./gradlew assembleDebug` |
| Install debug | `gradlew.bat installDebug` | `./gradlew installDebug` |
| Release build | `gradlew.bat assembleRelease` | `./gradlew assembleRelease` |

---

## Build Variants

| Variant | Suffix | Minify | Debuggable | Use Case |
|---------|--------|--------|------------|----------|
| **debug** | `-dev` | No | Yes | Development & testing |
| **release** | — | Yes (R8 + resource shrinking) | No | Production deployment |
| **benchmark** | — | Yes (inherits release) | No | Performance profiling |

---

## Testing

### Unit Tests

```bash
# All unit tests
./gradlew testDebugUnitTest

# Specific test class
./gradlew testDebugUnitTest --tests "com.bdbshs.crest.data.FileCacheTest"
```

<details>
<summary>Windows equivalent</summary>

```bat
gradlew.bat testDebugUnitTest
gradlew.bat testDebugUnitTest --tests "com.bdbshs.crest.data.FileCacheTest"
```

</details>

### Instrumented Tests

Requires a connected device or emulator:

```bash
# All instrumented tests
./gradlew connectedDebugAndroidTest

# Specific test
./gradlew connectedDebugAndroidTest --tests "com.bdbshs.crest.ui.screens.DocumentsScreenTest"
```

<details>
<summary>Windows equivalent</summary>

```bat
gradlew.bat connectedDebugAndroidTest
gradlew.bat connectedDebugAndroidTest --tests "com.bdbshs.crest.ui.screens.DocumentsScreenTest"
```

</details>

### Benchmarks

Requires a **physical device** (recommended) or API 29+ emulator:

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

Results are saved under `benchmark/build/outputs/`.

### Running Tests in Android Studio

| Test Type | Steps |
|-----------|-------|
| **Unit** | Navigate to `app/src/test/java/` → right-click a class or package → **Run Tests** |
| **Instrumented** | Connect a device → navigate to `app/src/androidTest/java/` → right-click → **Run Tests** |
| **Benchmark** | Open Gradle panel → `benchmark > Tasks > verification` → run `connectedBenchmarkAndroidTest` |

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Gradle sync fails | Check internet connection; try **File > Invalidate Caches** |
| Missing `google-services.json` | Download from [Firebase Console](https://console.firebase.google.com/) → place in `app/` |
| Appwrite config not applied | Verify `APPWRITE_*` keys in `local.properties` or passed as `-P` Gradle properties |
| Tests not running | Ensure the **debug** build variant is selected |
| Benchmark crashes | Use a physical device; ensure benchmark variant is selected |
| JDK version mismatch | **File > Project Structure > SDK Location** → set to JDK 11 |

---

## Developer

**James Ryan S. Gallego**
