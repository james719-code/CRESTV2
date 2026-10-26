# Changelog

All notable project changes should be recorded here.

This project follows a lightweight version of [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and uses semantic version tags where practical.

## Unreleased — 2.0.0

This is the planned second CREST product generation. Android uses version name
`2.0.0` and version code `2`; a GitHub release has not yet been published for it.

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
