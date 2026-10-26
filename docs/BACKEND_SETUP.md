# CREST backend setup and deployment status

Updated: 2026-10-26 (Asia/Manila).

The Firestore access review, deployed index settings, Android read improvements,
and repeatable query checks are documented in
[Firestore optimization](FIRESTORE_OPTIMIZATION.md).

## Connected services

| Service | Selected project / status |
| --- | --- |
| Firebase Authentication and Firestore | `crest-4fb99` |
| Firebase web app | CREST Teacher Web, `1:247751332436:web:26a54ddd61413ee90bd3c1` |
| Supabase | CREST, `mkmirlnypehadajrtxvg`, Tokyo |
| Supabase URL | https://mkmirlnypehadajrtxvg.supabase.co |
| Storage | Private `crest-files` bucket; PDFs up to 20 MiB |
| Edge Functions | `crest-storage` enabled; `crest-notifications` deployed but disabled |
| Android | Supabase provider selected in build defaults and local configuration |
| Teacher website | Netlify build settings connected; production hostname not chosen |
| Notifications | Disabled in server and clients; FCM send permission not granted |

The website itself has not been published. The user chose to defer selecting its
production hostname. No Appwrite originals were deleted and no original Firestore
file IDs were rewritten.

## Authentication and private files

Clients send their Firebase ID token to the storage function. The function checks
Google's JWT signature, issuer, audience, expiry, account disable/revocation status,
and current approval records in Firestore. Approved teachers and students can read
published library PDFs. Private submissions require current membership in the
group bound to the stored file, or approved teacher access. Uploads require the
appropriate teacher role or group membership. Download URLs expire in 60 seconds.

The Supabase gateway setting `verify_jwt=false` is intentional: these requests use
Firebase tokens rather than Supabase-issued tokens. Authentication happens inside
the function. Disabling or bypassing its authentication code is unsafe.

The dedicated Firebase account is
`crest-supabase-gateway@crest-4fb99.iam.gserviceaccount.com`. The user approved its
creation and server key. Its only project roles are `roles/datastore.viewer` and
`roles/firebaseauth.viewer`. The key is in Supabase server secrets and ignored
local files under `.secrets/` and `supabase/functions/.env.local`. It must never
be copied into a browser setting, Android configuration, source control, or logs.
Firebase web identifiers in `netlify.toml` are public client configuration.

All three `crest_*` tables have RLS enabled and no anon/authenticated grants.
There are no direct client Storage policies. Storage operations go through the
function, which uses a server key after checking the Firebase user. Physical
deletion is deliberately retained as a separate administrative operation.

## Firestore rules, indexes, and data

The former live rules allowed unauthenticated reads and writes. Deployed rules
are now versioned in `firebase/firestore.rules`, default-deny unknown paths,
prevent self-approval, restrict publication/approvals to approved teachers, and
limit student changes to their own profile and group. Group membership changes
must be atomic with the student's profile references. Students can read their
own profile and current group peers; teachers can manage school records. Favorites
are private to their owner. Approved teachers can approve other teachers, matching
the existing school workflow; there is no separate administrator role.

`firebase/firestore.indexes.json` preserves the existing legacy index, adds the
pending-submission composite index, and exempts eight fields unused by server
queries. Required indexes for file references, approval filters, strand counts,
group names, and creation times remain available. The composite index is READY.
The web teacher workspace still subscribes to all school records; these indexes
do not reduce that subscription's read volume. Larger deployments should add
server-side pagination before the school dataset grows substantially.

The original dataset contained 88 students, 7 teachers, 2 groups, 12 research
records, and no school documents. An atomic update normalized group pointers on
81 student profiles and added empty missing research-title fields on 2 groups.
All original record counts, approvals, profile details, research metadata, and file
references were verified unchanged. Conflicting concurrent edits would have
stopped the normalization through document-version checks.

## File migration and audit evidence

All 12 verified PDFs from `appwrite-downloads/manifest.json` were uploaded: exactly
19,083,349 bytes. Every destination was downloaded again and checked by SHA-256
before creating its private legacy-ID mapping. Every existing Firestore file
reference resolves to a verified Supabase mapping.

The resumable local-export importer can be run without contacting Appwrite:

~~~powershell
node scripts/migrate-local-storage.mjs
node scripts/migrate-local-storage.mjs --copy --firestore-backup state/backend-migration/firestore-before.json
~~~

The first command verifies local files and prints a plan only. The copy mode
requires the ignored `.env.backend-migration.local` with the selected Supabase
URL, bucket, and server key. It refuses conflicting mappings and never overwrites
destination objects. Do not rerun the original normalization against a stale
backup. Private backup/receipt files are excluded from source control:

- `state/backend-migration/firestore-before.json`: original school records.
- `state/backend-migration/indexes-before.json`: original index settings.
- `state/backend-migration/auth-config-before.json`: original Auth settings.
- `state/backend-migration/normalization-plan.json`: exact conditional updates.
- `state/backend-migration/local-upload-receipt.json`: per-file verification.
- `state/backend-migration/live-smoke-results.json`: 14 live gateway checks.
- `state/backend-migration/final-verification.json`: final cloud/data checks.

## Validation

Follow-up validation for web PDF caching and student management (2026-10-26):

- 38 frontend tests and 13 Firestore emulator security tests passed, along with ESLint and the production build.
- Repeated the 14 live storage gateway checks successfully, including uploads, migrated-file checksums, private access, and membership revocation. Temporary test data was removed.
- Exercised the actual web student-management transaction against live Firestore using temporary teacher/student accounts: decline, restore, revoke, stale-decision protection, and denied student impersonation all passed.
- Exercised the actual PDF reader/cache with live Firebase authentication and Supabase downloads: two opens performed two access checks and one PDF download, with the expected original SHA-256. Revoking the temporary student's access denied reuse and removed cached bytes; restoring access allowed a fresh download. Local persistence used the IndexedDB test implementation for this automated live check.
- Checked the web demo in a browser: decline confirmation, pending-queue removal, declined-account filtering, and restoration with an immediately updated student dialog.
- Independently checked the PDF reader with actual browser IndexedDB and the harmless test fixture: two opens downloaded once, reloading preserved the cached PDF with zero further downloads, and simulated revoked access blocked reuse and removed the copy. Temporary browser test pages were removed.
- Follow-up evidence: ignored `state/backend-migration/student-cache-smoke-results.json`. No school profiles or existing file mappings were changed by these checks.

The web cache holds PDFs for up to 30 days with a 100 MiB browser-wide cap, per-account/backend keys, eviction, and sign-out cleanup. It still checks access on every open, so it reduces repeat PDF transfer rather than eliminating gateway/Firestore usage. The new optional student `declined` flag is compatible with existing deployed rules; no backend deployment or data migration is needed for it. The website needs redeployment to serve the updated web code.

Original migration validation:

- 11 Firestore emulator security tests passed, covering anonymous access,
  self-approval, teacher management, student/group ownership, atomic membership,
  download-counter updates, and private favorites.
- 22 existing frontend tests, ESLint, and the production build passed.
- Both Edge Functions passed the Deno type checker.
- Android unit tests and the debug APK build passed with Java 17.
- Live checks verified rejected anonymous/forged requests, CORS restrictions,
  pending-account denial, migrated-file reads and checksums, signed PDF uploads,
  teacher permissions, and immediate membership revocation. Temporary cloud
  accounts, records, and files used for testing were removed.
- Live rules match the tested local rules; the private bucket, RLS, absence of
  client grants, and the gateway's two approved read-only IAM roles were checked.

To repeat the rules suite, start the emulator from the repository root, then run
`npm run test:rules` from `frontend`:

~~~powershell
firebase emulators:exec --only firestore --project demo-crest "npm --prefix frontend run test:rules"
~~~

## Before publishing the website

1. Choose the final Netlify/custom hostname. The repository's `netlify.toml`
   already contains the selected public Firebase/Supabase build settings.
2. Add that hostname to Firebase Authentication's authorized domains.
3. Add its exact HTTPS origin to `CREST_ALLOWED_ORIGINS` in Supabase secrets,
   preserving any other intended origins. Wildcard website origins are not used.
4. Deploy the static site to Netlify and check Google sign-in with an existing
   approved teacher account from the final hostname. A real browser/device sign-in
   from that future hostname has not been tested yet.

Current origins allow localhost ports 5173/4173 and the existing Firebase Hosting
hostnames. Android requests do not require an Origin header. The backend is live;
website hosting and release signing follow the project's normal release process
in [RELEASE.md](RELEASE.md). No tag, GitHub release, or public website publication
was created during this backend setup.

## Future backend updates

Use the existing signed-in CLIs and selected projects:

~~~powershell
firebase deploy --only firestore --project crest-4fb99
supabase db push --linked
supabase secrets set --project-ref mkmirlnypehadajrtxvg --env-file supabase/functions/.env.local
supabase functions deploy crest-storage crest-notifications --project-ref mkmirlnypehadajrtxvg --use-api
~~~

Apply new SQL changes as new migration files. Keep storage private and never
publish the server key. Enabling notifications later requires FCM send permission,
client messaging configuration, and separate validation; its current account has
read-only Firebase permissions.
