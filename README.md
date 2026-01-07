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

## Build Variants

This project supports multiple build variants for different use cases:

### Debug (Dev)
- **Application ID:** `com.bdbshs.crest`
- **Version Suffix:** `-dev`
- **Use Case:** Development and testing
- **Features:** No code minification, debuggable
- **Command:** `./gradlew assembleDebug`

### Release
- **Application ID:** `com.bdbshs.crest`
- **Use Case:** Production deployment
- **Features:** Code minification, resource shrinking, optimized
- **Command:** `./gradlew assembleRelease`

### Benchmark
- **Use Case:** Performance testing
- **Features:** Based on release configuration, uses debug signing
- **Command:** `./gradlew assembleBenchmark`

## Testing

### Running Unit Tests

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "com.bdbshs.crest.data.FileCacheTest"
```

### Running Instrumented Tests

Requires a connected device or emulator:

```bash
# Run all instrumented tests
./gradlew connectedDebugAndroidTest

# Run specific instrumented test
./gradlew connectedDebugAndroidTest --tests "com.bdbshs.crest.ui.screens.DocumentsScreenTest"
```

### Running Benchmarks

Requires a physical device or API 29+ emulator:

```bash
# Build and run benchmarks
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

Benchmark results are saved to: `benchmark/build/outputs/connected_android_test_additional_output/`

## Android Studio Setup

### Prerequisites

*   **Android Studio:** Ladybug (2024.2.1) or newer recommended
*   **JDK:** Java 11 or higher (bundled with Android Studio)
*   **Android SDK:** API 24 (minimum) to API 35 (target)
*   **Gradle:** 8.x (managed automatically via Gradle Wrapper)

### Step-by-Step Import Guide

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/CREST.git
    cd CREST
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
    *   Update Appwrite credentials in the relevant config file

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
| Tests not running | Ensure correct build variant is selected (debug) |
| Benchmark crashes | Use a physical device; ensure app is debuggable for benchmark variant |
| JDK version mismatch | Set JDK in **File > Project Structure > SDK Location** |

## Developer

**James Ryan S. Gallego**
