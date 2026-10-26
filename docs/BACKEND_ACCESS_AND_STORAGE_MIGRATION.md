# Backend access and Appwrite-to-Supabase Storage migration

Implementation update, 2026-10-26: Firebase rules/indexes and the CREST Supabase
backend are deployed, storage is enabled, and all 12 local export PDFs have
checksum-verified private copies. See [BACKEND_SETUP.md](BACKEND_SETUP.md) for
current deployment status. The original planning/access history below is retained
for reference and does not describe the current configuration.

The original checklist retained Firebase Authentication and Firestore while
migrating Appwrite file storage to Supabase. That migration has now been applied.

## Provide access

### Preferred CLI workflow and current access

The user prefers Firebase CLI and Supabase CLI and will create the separate CREST Supabase project personally. Do not reuse SPECS Website or CodeCanvas as the destination or create a different cloud project on the user's behalf.

Verified on 26 October 2026:

- Firebase CLI `15.34.1` runs successfully. Its currently selected account lists only `PawPortion`; the configured CREST project is `crest-4fb99`. A direct read-only Firestore database-list request for CREST returned HTTP 403. Project access has not been established.
- Supabase CLI `2.122.0` runs successfully and is authenticated. It lists SPECS Website and CodeCanvas in the SPECS organization. The CLI organization listing does not expose the billing plan; no project or paid resource was created.
- Appwrite migration credentials are not configured. No bucket files have been read or copied.

Next local steps:

1. Authorize the Google account that has access to `crest-4fb99` using `npx -y firebase-tools@latest login:add`. Keep the existing unrelated project configuration unchanged. A later command will target CREST explicitly using `--project crest-4fb99` and the correct account when necessary.
2. Create the separate Supabase project, then supply its public project reference (or dashboard project URL). Singapore (`ap-southeast-1`) is a reasonable nearby region for this Philippine school. No Supabase account password needs to be shared; keep the database password in your password manager.
3. Put the Appwrite endpoint, project/bucket IDs, and temporary `files.read`/`buckets.read` API key in `.env.backend-migration.local`.
4. Verify CLI access again. Link only the explicitly identified Supabase project and fetch any necessary project keys directly into ignored local configuration. Do not print raw API-key responses. Never print the JSON response from a Firebase login-account listing, because it may include saved credentials; use a strict public-field projection when checking account status.

CLI login can remove the need to manually provide a service-account JSON or Supabase project secret for initial management checks. A trusted runtime may still need separate credentials later for a backend authorization gateway; that is an implementation/deployment step, not a reason to collect broad credentials in advance.

Local migration settings contain only Appwrite source settings and Supabase
file-copy settings. The copy script does not use Firebase,
ADC, or a Supabase publishable key. CLI sessions remain managed by the CLIs;
the script's direct Storage/REST copy still requires a destination server key.
An authorized Supabase CLI can retrieve that key later for the selected project
without printing it. The hosted Edge Functions need separate Firebase server
credentials; they cannot use the local Firebase CLI session.

Firebase client configuration already exists in `android/app/google-services.json` and `frontend/.env.local`. Those values identify client apps; they do not grant administrative access to Firestore or Firebase Authentication.

Firebase was installed after the initial checklist. CLI commands remain the user's preferred interface. Plugin installation alone does not imply access to the CREST project. The binary-copy and runtime authorization steps will use only verified project credentials.

For local scripts, use the restored `.env.backend-migration.example` as the input reference and enter real values in the ignored `.env.backend-migration.local` at the repository root when ready. The [credential guide](CREDENTIALS.md) explains where each value comes from. The local file, `/.secrets/`, and `/state/backend-migration/` are Git-ignored. Do not send passwords, private JSON contents, or service keys in chat. Git ignore prevents accidental commits; it is not encryption or an operating-system access control.

| Service | Required information/access | Purpose |
| --- | --- | --- |
| Firebase | Project ID; connected Firebase tooling, an authorized Application Default Credentials session, or a local service-account JSON path | Inventory collections, fields, existing file references, roles, and rules. Start with Firestore read access; the later update plan determines any additional permissions. |
| Appwrite | API endpoint, project ID, bucket ID, temporary API key with `files.read` and `buckets.read` | List source files and download the originals. Source write/delete access is not needed. |
| Supabase | Selected project URL and destination bucket; a temporary server secret key (or legacy `service_role` key) for the copy step; project reference for management context | Copy and verify objects. CLI login can retrieve authorized project keys later, but the copy script does not automatically reuse the CLI session. The chosen Edge Function clients need the public project URL and Firebase sign-in, with no Supabase publishable key. |

No Google/Appwrite/Supabase account password is needed. No Supabase database password or personal access token is needed for the initial credential checklist or the storage data API copy. Additional deployment access depends on the reviewed implementation, rather than requesting broad credentials upfront. A publishable key alone cannot perform a privileged migration. Firebase's public API key and Android's `google-services.json` are not substitutes for administrative authentication.

Firebase supports Application Default Credentials. A service-account JSON file is an alternative for trusted local/server tooling; keep it outside source control and provide its path via `GOOGLE_APPLICATION_CREDENTIALS`. A CLI sign-in and Application Default Credentials are separate authentication mechanisms. See [Firebase Admin setup](https://firebase.google.com/docs/admin/setup).

Appwrite's documented scopes distinguish `files.read`/`buckets.read` from write/delete scopes. Create a migration-only key in the source project and revoke it when the migration is complete. See [Appwrite API keys](https://appwrite.io/docs/partners/project/api-keys).

Supabase secret keys bypass Row Level Security and belong only in trusted administration/server tooling. A public publishable key is the appropriate key for shipped browser/Android clients. See [Supabase API keys](https://supabase.com/docs/guides/getting-started/api-keys).

## Inventory before organizing Firebase

The code currently reads these paths, so renaming them in production would require a coordinated Android and website update:

- `users/user_details/students`
- `users/user_details/teachers`
- `groups`
- `researches/research_details/qualitative`
- `researches/research_details/quantitative`
- `documents`
- `user_favorites` in Android

First inventory document counts and field types, missing/orphaned file references, inconsistent group memberships, approval flags, and current rules/indexes. Report a concrete organization plan before applying structural changes. Export the affected metadata before any backfill. Treat exports as private school data; do not print raw student records or credentials into logs.

## Storage migration sequence

1. Authenticate and verify the explicitly identified source and destination projects. Inventory source bucket settings, file permissions, IDs, content types, sizes, and every Firestore reference. Do not infer access rules from the frontend UI.
2. Produce a dry-run manifest with deterministic destination paths, source references, expected sizes, and conflicts. Plan for both published research and unapproved group submissions. Preserve shared references and existing IDs where feasible.
3. Prepare a private Supabase bucket and a reviewed authorization design. Test it with authorized and unauthorized users before client cutover. Do not make the bucket public just to get downloads working.
4. Copy files without deleting Appwrite originals or overwriting unrelated destination files. Record resumable state, compare byte counts and SHA-256 hashes after downloading destination objects, and report every missing/failed copy.
5. Update the Android app and teacher website together, including upload, viewing, download/cache, deletion, failure cleanup, and file-reference compatibility. Older installed Android releases must continue to work during transition; retain Appwrite originals and compatible metadata until the release/cutover plan is verified. Do not store expiring signed URLs as permanent file references.
6. Backfill only verified references using a restorable mapping, with preconditions so concurrent edits are not overwritten. Validate representative live records and access revocation before changing the default storage provider.
7. Keep source originals through an agreed retention window. Source deletion is separate from copying and cutover. Revoke temporary administrative keys after verification.

## Firebase identity with Supabase storage

Supabase can accept Firebase Authentication tokens through its third-party auth integration. It requires explicit project/provider configuration, JWT claim handling, and Storage RLS policies. Existing Firestore teacher approval and student/group membership are not automatically enforced by Supabase: the implementation must verify them in trusted backend code or maintain a secure authorization projection. Student submissions, approved school papers, documents, and teacher administration have different authorization needs.

Firebase user IDs are strings; do not assume a Firebase subject is a Supabase UUID. Preserve existing Firebase custom claims when adding integration claims. Do not equate a generic `authenticated` role with approved teacher access. If a backend gateway is selected instead, it must verify Firebase ID tokens and current permissions before issuing storage operations or short-lived file access. The choice will be made after examining the existing rules and deployment environment.

See [Supabase Firebase integration](https://supabase.com/docs/guides/auth/third-party/firebase-auth) and [Storage access control](https://supabase.com/docs/guides/storage/security/access-control).
