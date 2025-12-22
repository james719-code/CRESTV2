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

## Developer

**James Ryan S. Gallego**
