# Release Process

Releases are built from version tags and published by GitHub Actions.

Android and web CI run independently according to the changed files. Documentation
changes build neither app. See [CI/CD routing and credentials](CI_CD.md) for the
trigger table, Netlify setup, and required-check configuration.

## Android version policy

`android/version.properties` is the source of the APK's version. The upcoming
CREST product relaunch uses `versionName=2.0.0` and `versionCode=2`. The published
`v1.0.0` source used `versionName=1.0` and `versionCode=1`.

Android treats these values separately:

- `versionName` is the user-facing release label. `2.0.0` identifies the second
  product generation, including the redesigned Android interface, private storage,
  school access controls, and connected teacher website. This major release label
  is a product decision, not a number calculated from commits.
- `versionCode` is a positive integer that must increase for every published
  Android release, including patch releases. Increment it again before the next
  release. Local edits, test builds, and Git commits do not each count as releases.

Keep the application ID and existing release signing key so installed copies can
receive updates. The app's About screen reads its version from `BuildConfig`.
Debug builds append `-dev` to the version name.

See [Android app versioning](https://developer.android.com/studio/publish/versioning)
and [app signing](https://developer.android.com/studio/publish/app-signing).
If applying strict [Semantic Versioning](https://semver.org/) to a compatibility
contract, reserve major increments for incompatible changes, minor increments for
compatible features, and patch increments for compatible fixes.

## Prerequisites

Configure these GitHub repository secrets:

| Secret | Purpose |
| --- | --- |
| `SIGNING_KEY` | Base64-encoded release keystore |
| `KEY_STORE_PASSWORD` | Keystore password |
| `ALIAS` | Release key alias |
| `KEY_PASSWORD` | Release key password |
| `GOOGLE_SERVICES_JSON` | Base64-encoded Firebase `google-services.json` |

The release workflow runs Gradle from `android/`, decodes Firebase configuration to `android/app/google-services.json`, and uploads APKs from `android/app/build/outputs/apk/release/`. Do not commit the decoded files.

## Local Release Check

Local signing can be configured once in `android/keystore.properties`, which is
ignored by Git. Copy `android/keystore.properties.example` if the local file does
not exist. For the existing `android/crest_jks` keystore, use:

```properties
storeFile=crest_jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=crest_jks
keyPassword=YOUR_KEY_PASSWORD
```

Enter the real passwords privately in that local file, without surrounding
quotes. Paths are resolved relative to `android/`; use forward slashes in
absolute Windows paths. `local.properties` continues to hold SDK and backend
settings, while the keystore file contains the signing key itself.

The four `RELEASE_*` environment variables still override the corresponding
local values for CI or an explicitly selected signing key. The GitHub workflow
continues to use repository secrets rather than the local file.
Release packaging rejects incomplete signing details instead of creating an
unsigned APK or bundle. Debug builds do not require release credentials.

Before tagging, run these commands from `android/`:

```bash
./gradlew testDebugUnitTest
./gradlew assembleRelease
```

Windows:

```bat
gradlew.bat testDebugUnitTest
gradlew.bat assembleRelease
```

`assembleRelease` requires complete signing credentials in the local file or the
release environment variables described in [`README.md`](../README.md).

## Tagging

Use semantic version tags:

```bash
git tag android-v2.0.0
git push origin android-v2.0.0
```

The workflow builds a signed release APK and attaches it to a GitHub release.
Legacy `v*` Android tags remain supported. `web-v*` tags do not trigger Android
Release. Creating an Android release tag deliberately builds Android regardless
of the files changed in the tagged commit.

Update both values in `android/version.properties` before committing and tagging.
The workflow refuses a tag whose version differs from the APK's `versionName`.
It retains the asset name `app-release.apk` for the website's permanent download
link. A normal commit does not publish a release.

## Website downloads

The public website links directly to
[`releases/latest/download/app-release.apk`](https://github.com/james719-code/CRESTV2/releases/latest/download/app-release.apk).
The latest published GitHub Release supplies the displayed version, file size,
and version-specific download/notes links. Until `2.0.0` is actually published,
the website describes the existing `1.0.0` download rather than labeling the old
APK as the new release. If the public GitHub API is unavailable, the stable
download link still works and the page points visitors to GitHub for details.

Keep GitHub's **latest** release an Android release with `app-release.apk`.
If publishing a separate web release in this repository, mark it as **not latest**
so the permanent APK link continues to resolve.

## Release Notes

Release notes should include:

- User-visible changes
- Teacher workflow changes
- Student workflow changes
- Backend/configuration changes
- Known limitations
- Migration or rollout steps
