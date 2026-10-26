# Builds, checks, releases, and credentials

## Which changes run which checks?

The `CI` workflow in `.github/workflows/ci.yml` runs on pull requests, pushes to
`master`/`main`, merge-queue events, and manual runs. A small job compares Git
changes and selects independent Android, web, and backend jobs.

| Changed files | Android build/tests | Web build/tests | Backend security checks |
| --- | --- | --- | --- |
| `android/**` | Run | Skip | Skip |
| `frontend/**` application files | Skip | Run | Skip |
| Both applications | Run | Run | Skip |
| Only `docs/**` or Markdown documentation anywhere | Skip | Skip | Skip |
| `firebase/**`, `firebase.json`, `.firebaserc` | Skip | Skip | Run |
| `supabase/**`, migration/download scripts, `frontend/backend-tests/**` | Skip | Skip | Run |
| `frontend/package.json` or `frontend/package-lock.json` | Skip | Run | Run |
| `netlify.toml` | Skip | Run | Skip |
| Shared branding tokens, generator, or font sources | Run | Run | Skip |
| Generated branding files for one platform | That platform only | That platform only | Skip |
| CI workflow or shared routing code | Run | Run | Run |
| Android release workflow | Run | Skip | Skip |

The shared rules are in `scripts/ci/paths.mjs`. Documentation changes still run
the lightweight routing tests and final status job; neither application is built.
Backend checks install their test dependencies but do not run the Android or
website build. They use a local Firestore emulator and check Edge Function types;
they never deploy to the live backend.

Changes are compared across the entire push. Pull requests use their merge base,
so unrelated changes on the target branch do not cause unnecessary app builds.
Deleted files and both sides of cross-platform moves are included. First runs
without history, or unavailable comparisons, validate all components instead of
silently skipping checks.

## Required checks

If you enable branch protection, require **CI passed**. It runs after the selected
jobs, fails if any required check fails/cancels, and succeeds when documentation
changes correctly skip all app builds. The default branch is currently `master`;
its classic branch-protection endpoint reported that it is not protected during
setup. No remote repository protection settings were changed.

The workflow deliberately uses conditional jobs rather than skipping its entire
run with `paths` filters. GitHub documents that skipped required workflows can
remain pending, whereas conditional jobs can be skipped successfully. See
[GitHub required-check guidance](https://docs.github.com/en/enterprise-cloud%40latest/pull-requests/how-tos/merge-and-close-pull-requests/troubleshooting-required-status-checks?apiVersion=2022-11-28).

To run a component explicitly after these files are pushed, open GitHub
**Actions → CI → Run workflow** and choose `android`, `web`, `backend`, or `all`.
An intentional manual run overrides file-change filtering.

## Android credentials

Ordinary Android CI needs no secrets: it creates a placeholder Firebase client
configuration and disables storage for the test build. The test APK is not a
production-connected release.

Signed releases use `.github/workflows/release.yml`. These repository secret
names are already present in `james719-code/CRESTV2`:

| Secret | Value required |
| --- | --- |
| `SIGNING_KEY` | Base64 of the existing release keystore file |
| `KEY_STORE_PASSWORD` | Keystore password |
| `ALIAS` | Signing-key alias in that keystore |
| `KEY_PASSWORD` | Password for that key |
| `GOOGLE_SERVICES_JSON` | Base64 of `android/app/google-services.json` for CREST |

Only secret names were inspected. GitHub does not expose their stored values;
their validity is checked by decoding/building when a release is triggered.
Keep the original signing keystore so existing Android users can install updates.
Decoded signing inputs are removed at the end of the release job.

Create/change these under GitHub **Settings → Secrets and variables → Actions**.
Never put their values in commits, workflow YAML, or chat. GitHub's built-in
`GITHUB_TOKEN` publishes release assets; a personal GitHub access token is not
needed. See [GitHub secret setup](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets).

Prefer Android-specific tags such as `android-v1.1.0`. Existing `v1.1.0` tags
remain supported for compatibility. A release tag is an explicit request to build
and publish Android, even if the tagged commit itself only changes documentation.
Ordinary app/docs commits never create a signed release automatically. Web-only
tags such as `web-v1.1.0` do not trigger Android Release.

## Website deployment and credentials

Web CI uses Node 22 to install dependencies, run ESLint/tests, and build Vite. It
does not publish a site and needs no private Firebase or Supabase credentials.

For deployment, connect the chosen Netlify site to this GitHub repository and
select `master` as its production branch. Authorize Netlify's GitHub integration
to access the repository. `netlify.toml` supplies the base directory (`frontend`),
build command, output directory (`dist`), and public Firebase/Supabase settings.
These client identifiers are public; server keys never belong in `VITE_*` values.

No `NETLIFY_AUTH_TOKEN` or `NETLIFY_SITE_ID` GitHub secret is needed with this
Git-connected Netlify setup. Those would be needed for a different design that
uploads deployments through the Netlify CLI from GitHub Actions.

The configured Netlify ignore command uses the same file rules as CI. It skips
Android-only and documentation-only commits, includes shared web branding inputs,
and builds when there is no cached commit or a cache clear was requested. It runs
before package installation and requires no installed npm dependencies. Netlify's
ignore convention is exit `0` to skip and exit `1` to continue. Builds triggered
through a Netlify build hook deliberately bypass the ignore cancellation; avoid
triggering a hook on every repository commit. See [Netlify ignore-build guidance](https://docs.netlify.com/build/configure-builds/ignore-builds/).

The production hostname has not been selected. Once chosen, authorize it in
Firebase Authentication and add its exact HTTPS origin to the existing
`CREST_ALLOWED_ORIGINS` Supabase server secret before testing production sign-in.
See [backend setup](BACKEND_SETUP.md).

## Backend credentials

Backend CI uses only the `demo-crest` Firestore emulator and the Deno type checker.
It requires no Firebase service-account key, Supabase server key, database
password, or cloud CLI login. The live Firebase read-only gateway key remains in
Supabase secrets, separate from GitHub pull-request checks.

This change does not add automatic backend deployments. Future cloud deployment
automation would need separately configured deployment permissions; do not copy
runtime admin keys into the current CI jobs.

## Validation and activation

Run the routing regression tests from the repository root:

```powershell
node --test scripts/ci/*.test.mjs
```

They exercise real temporary Git history, not the working school repository.
Tests cover Android/web/docs changes, shared/backend dependencies, merge bases,
moves, deletions, manual choices, merge-queue events, Netlify skips, and missing
history. Workflow YAML and expressions were also checked with `actionlint`.

The updated files must be committed and pushed before GitHub uses them. Include
the earlier backend integration sources, new CI scripts, replacement CI workflow,
release workflow updates, Netlify configuration, and documentation. Do not commit
ignored local credentials, downloaded school files, or migration backups.

After the first remote run, confirm the job summary matches the changed files.
To roll back routing, revert the CI/scripts/Netlify-ignore changes; deployed school
records and server secrets are independent of these repository files.
