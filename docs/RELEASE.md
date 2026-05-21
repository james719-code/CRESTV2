# Release Process

Releases are built from version tags and published by GitHub Actions.

## Prerequisites

Configure these GitHub repository secrets:

| Secret | Purpose |
| --- | --- |
| `SIGNING_KEY` | Base64-encoded release keystore |
| `KEY_STORE_PASSWORD` | Keystore password |
| `ALIAS` | Release key alias |
| `KEY_PASSWORD` | Release key password |
| `GOOGLE_SERVICES_JSON` | Base64-encoded Firebase `google-services.json` |

The release workflow decodes these files at build time. Do not commit the decoded files.

## Local Release Check

Before tagging:

```bash
./gradlew testDebugUnitTest
./gradlew assembleRelease
```

Windows:

```bat
gradlew.bat testDebugUnitTest
gradlew.bat assembleRelease
```

`assembleRelease` requires the release signing environment variables described in `README.md`.

## Tagging

Use semantic version tags:

```bash
git tag v1.1.0
git push origin v1.1.0
```

The workflow builds a signed release APK and attaches it to a GitHub release.

## Release Notes

Release notes should include:

- User-visible changes
- Teacher workflow changes
- Student workflow changes
- Backend/configuration changes
- Known limitations
- Migration or rollout steps
