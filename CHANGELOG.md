# Changelog

All notable project changes should be recorded here.

This project follows a lightweight version of [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and uses semantic version tags where practical.

## 2.1.0 — 2026-11-20

Android version name `2.1.0`, version code `3`. Tag: `android-v2.1.0`.

### Added

- Research planning: teacher-managed deadlines and milestones per research group,
  with overdue, due today, upcoming and completed overviews, and a notification
  inbox for submissions, approvals, revision feedback and account registrations.
- Research certificates and a 2.x announcement teaser video.
- Declined school accounts and research group deletion for teachers.
- Scheduled Supabase database health checks.
- Website help page, expanded About page, open-source notices and a dedicated
  PDF paper reader.
- Shared logo generator: the original CREST mark now produces the Android
  launcher, round launcher, splash, website favicon and browser icons, checked in CI.

### Changed

- Every Android screen follows the CREST brand system: shared CREST controls,
  title-first cards, labelled status badges with icons, sentence-case labels,
  and semantic colors (red only for real errors and destructive confirmations).
- Teachers now **Request changes** with written feedback instead of "Deny".
- Refreshed Android Home and research browsing layouts, and the teacher
  website's landing page and workspace.
- The teacher website loads the workspace and Firebase on demand.
- Local release signing can be configured in `android/keystore.properties`.

### Fixed

- Saved research papers load in offline mode.
- Certificate text uses line breaking compatible with older Android versions.

## 2.0.0 — 2026-10-27

The second CREST product generation. Android version name `2.0.0`, version code `2`.

### Added

- Public Android download section, installation steps, and the supplied Bonifacio
  D. Borebor Sr. High School seal on the website.
- Download version and file size taken from the published GitHub Release, with a
  permanent APK download and release-notes fallback.
- Release validation that requires the Git tag to match the APK's version name.
- Public contribution, security, architecture, and release documentation.
- Pull request checklist and issue templates.
- CI workflow for regular validation.
- Repository hygiene rules for IDE state, generated logs, local docs, and secrets.

### Changed

- Android release metadata now lives in `android/version.properties`; the app's
  About screen automatically reflects the generated build version.
- Reworked the README into a public-facing project overview with setup, configuration, and quality gate guidance.

### Removed

- IDE project metadata and Kotlin diagnostic logs from version control.
