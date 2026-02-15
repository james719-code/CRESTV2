# CREST

**Compiled Research as an Educational Application for Students and Teachers**

CREST is a comprehensive mobile platform designed to empower educators and students at Bonifacio D. Borebor Sr. High School. It provides centralized access to compiled research and educational documents, streamlining the organization, retrieval, and sharing of valuable academic resources.

## Why CREST?

I created this project because there was no e-backup for research at our school. With CREST, we make research easily available to any student within the school, fostering an environment of collaborative learning and efficient knowledge dissemination.

## Features

### 🔐 Authentication & Roles
*   **Google Sign-In:** Secure and easy login using institutional or personal Google accounts.
*   **User Roles:** Distinct interfaces and permissions for **Students** and **Teachers**.
    *   **Students:** Can browse research, view details, and upload group research for approval.
    *   **Teachers:** Can browse research, upload their own materials, and manage student group submissions (Approve/Deny).

### 📚 Research Management
*   **Centralized Repository:** A single place for all school research papers.
*   **Upload System:**
    *   Easy-to-use form for uploading PDFs.
    *   Metadata fields: Title, Strand (STEM, ABM, etc.), Research Type, and Authors.
    *   **Teacher Approval:** Student uploads go into a "Pending" state until approved by a teacher.

### 🔍 Discovery & Viewing
*   **Search & Filter:** Quickly find research by title, status (Pending/Accepted), or sort by name.
*   **Responsive UI:** Optimized for various Android screen sizes.
*   **Integrated PDF Viewer:**
    *   **High Performance:** Uses lazy loading to render PDF pages on demand, ensuring smooth scrolling even for large documents.
    *   **Secure:** Views documents directly within the app without needing external tools.

### 👥 About Us
*   **Responsive Team Page:** improved UI to showcase the developer and co-researchers involved in the project.

## Tech Stack

This project is built using modern Android development practices:

*   **Language:** [Kotlin](https://kotlinlang.org/)
*   **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) - Google's modern toolkit for building native UI.
    *   **Material Design 3:** Follows the latest Material Design guidelines.
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Backend & Data:**
    *   **Firebase Authentication:** For secure user sign-in.
    *   **Firebase Firestore:** NoSQL database for storing user profiles, research metadata, and groups.
    *   **Appwrite:** Used for secure file storage and management of research documents.
*   **Concurrency:** Kotlin Coroutines & Flow.
*   **Dependency Injection:** Manual / ViewModel factory (based on current scope).
*   **Build System:** Gradle (Kotlin DSL).

## Requirements

*   **Android Studio:** Ladybug (2024.2.1) or newer
*   **JDK:** 11 (project `sourceCompatibility`/`targetCompatibility` is Java 11)
*   **Android SDK:**
    *   App module: `minSdk 24`, `targetSdk 35`
    *   Benchmark module: `targetSdk 36`
*   **Firebase project:** Auth + Firestore enabled
*   **Appwrite project:** endpoint/project/bucket available

## Configuration

### 1) Firebase

Place `google-services.json` in `app/`.

### 2) Appwrite values

The app reads Appwrite values from `local.properties` first, then Gradle properties, then fallback defaults.

Add these keys to `local.properties` (or pass as Gradle properties):

```properties
APPWRITE_ENDPOINT=https://fra.cloud.appwrite.io/v1
APPWRITE_PROJECT_ID=your_project_id
APPWRITE_BUCKET_ID=your_bucket_id
```

### 3) Release signing (for release builds)

Release signing uses environment variables:

*   `RELEASE_STORE_FILE`
*   `RELEASE_STORE_PASSWORD`
*   `RELEASE_KEY_ALIAS`
*   `RELEASE_KEY_PASSWORD`

If these are not set, `release` signing cannot be fully configured.

## Build & Run

From the project root:

### Windows (PowerShell/CMD)

```bat
gradlew.bat assembleDebug
gradlew.bat installDebug
```

### macOS/Linux

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Build Variants

This project supports multiple build variants for different use cases:

### Debug (Dev)
- **Application ID:** `com.bdbshs.crest`
- **Version Suffix:** `-dev`
- **Use Case:** Development and testing
- **Features:** No code minification, debuggable
- **Commands:** `gradlew.bat assembleDebug` (Windows) / `./gradlew assembleDebug` (macOS/Linux)

### Release
- **Application ID:** `com.bdbshs.crest`
- **Use Case:** Production deployment
- **Features:** Code minification, resource shrinking, optimized
- **Commands:** `gradlew.bat assembleRelease` (Windows) / `./gradlew assembleRelease` (macOS/Linux)

### Benchmark
- **Use Case:** Performance testing
- **Features:** Dedicated benchmark module and benchmark build types for macrobenchmarking
- **Commands:** `gradlew.bat :benchmark:assembleBenchmark` (Windows) / `./gradlew :benchmark:assembleBenchmark` (macOS/Linux)

## Testing

### Running Unit Tests

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "com.bdbshs.crest.data.FileCacheTest"
```

Windows equivalent:

```bat
gradlew.bat testDebugUnitTest
gradlew.bat testDebugUnitTest --tests "com.bdbshs.crest.data.FileCacheTest"
```

### Running Instrumented Tests

Requires a connected device or emulator:

```bash
# Run all instrumented tests
./gradlew connectedDebugAndroidTest

# Run specific instrumented test
./gradlew connectedDebugAndroidTest --tests "com.bdbshs.crest.ui.screens.DocumentsScreenTest"
```

Windows equivalent:

```bat
gradlew.bat connectedDebugAndroidTest
gradlew.bat connectedDebugAndroidTest --tests "com.bdbshs.crest.ui.screens.DocumentsScreenTest"
```

### Running Benchmarks

Requires a physical device or API 29+ emulator:

```bash
# Build and run benchmarks
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

Windows equivalent:

```bat
gradlew.bat :benchmark:connectedBenchmarkAndroidTest
```

Benchmark results are saved under `benchmark/build/outputs/`.

## Android Studio Setup

### Step-by-Step Import Guide

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/james719-code/CRESTV2.git
    cd CRESTV2
    ```

2.  **Open in Android Studio:**
    *   Launch Android Studio
    *   Select **File > Open**
    *   Navigate to the cloned `CREST` folder and click **OK**
    *   Wait for Gradle sync to complete (may take a few minutes on first import)

3.  **Configure Firebase:**
    *   Copy your `google-services.json` to `app/` directory
    *   Ensure Firebase project has Authentication and Firestore enabled

4.  **Configure Appwrite:**
    *   Add `APPWRITE_ENDPOINT`, `APPWRITE_PROJECT_ID`, and `APPWRITE_BUCKET_ID` in `local.properties` (or Gradle properties)

5.  **Sync and Build:**
    *   Click **Sync Project with Gradle Files** (elephant icon in toolbar)
    *   Select **Build > Make Project** to verify setup

### Run Configurations

| Configuration | Purpose | How to Run |
|---------------|---------|------------|
| `app` | Run the main application | ▶️ Select "app" and click Run |
| Unit Tests | Run local JVM tests | Right-click `test` folder > **Run Tests** |
| Instrumented Tests | Run on-device UI tests | Right-click `androidTest` folder > **Run Tests** |
| Benchmark | Run performance tests | Select "benchmark" module, run "connectedBenchmarkAndroidTest" |

### Run Tests from Android Studio

**Unit Tests:**
*   Navigate to `app/src/test/java/`
*   Right-click on a test class or package
*   Select **Run 'TestClass'** or **Run Tests in 'package'**

**Instrumented Tests:**
*   Connect a device or start an emulator
*   Navigate to `app/src/androidTest/java/`
*   Right-click on a test class
*   Select **Run 'TestClass'**

**Benchmarks:**
*   Connect a **physical device** (recommended) or API 29+ emulator
*   Open Gradle panel (View > Tool Windows > Gradle)
*   Navigate to `benchmark > Tasks > verification`
*   Double-click `connectedBenchmarkAndroidTest`

### Troubleshooting

| Issue | Solution |
|-------|----------|
| Gradle sync fails | Check internet connection, invalidate caches: **File > Invalidate Caches** |
| Missing `google-services.json` | Download from Firebase Console and place in `app/` |
| Appwrite config not applied | Verify `APPWRITE_*` keys exist in `local.properties` or are passed as Gradle properties |
| Tests not running | Ensure correct build variant is selected (debug) |
| Benchmark crashes | Use a physical device; ensure app is debuggable for benchmark variant |
| JDK version mismatch | Set JDK in **File > Project Structure > SDK Location** |

## Developer

**James Ryan S. Gallego**
