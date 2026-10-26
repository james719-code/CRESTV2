# What credentials to enter

The application backend runs in **Supabase Edge Functions**. Firebase provides
sign-in, Firestore, and Cloud Messaging. Your local CLI logins stay managed by
their CLIs; do not paste account passwords or CLI tokens into these files.

## 1. Copy existing Appwrite files into Supabase

Use `.env.backend-migration.example` as the reference. Enter actual values in the
ignored `.env.backend-migration.local` at the repository root. If that local file
already exists, fill its missing values without replacing existing credentials.

| Field | What to enter | Where to find it |
| --- | --- | --- |
| `APPWRITE_ENDPOINT` | The old project's HTTPS API endpoint ending in `/v1` | Appwrite project console; use that project's region |
| `APPWRITE_PROJECT_ID` | Project ID, not the display name | Appwrite project settings |
| `APPWRITE_BUCKET_ID` | Bucket ID containing the old files | Appwrite Storage; open the source bucket |
| `APPWRITE_API_KEY` | Temporary read-only API key with `files.read` and `buckets.read` | Appwrite project sidebar, API Keys, Create API key |
| `SUPABASE_URL` | Project URL such as `https://your-project-ref.supabase.co` | The selected CREST Supabase project's Connect dialog |
| `SUPABASE_STORAGE_BUCKET` | `crest-files` | Already matches the prepared SQL; keep this value |
| `SUPABASE_SECRET_KEY` | A server secret key starting with `sb_secret_`, or an existing legacy `service_role` key | Supabase project settings, API Keys |

The destination secret is required only for the copy step. The script does not
automatically use your Supabase CLI session. An authorized CLI can retrieve the
selected project's key later, so manually finding it is optional. No Firebase
credential or Supabase public/publishable key is needed for this file copy.

See [Appwrite API keys](https://appwrite.io/docs/partners/project/api-keys) and
[Supabase API keys](https://supabase.com/docs/guides/getting-started/api-keys).

## 2. Let the Supabase functions access Firebase

Use `supabase/functions/.env.example` as the reference. These values ultimately
belong in **Supabase Dashboard, Edge Functions, Secrets**. No Firebase Cloud
Functions deployment is involved.

| Field | What to enter |
| --- | --- |
| `CREST_FIREBASE_PROJECT_ID` | Firebase Console, Project settings, General, Project ID |
| `CREST_FIREBASE_SERVICE_ACCOUNT_JSON` | The entire downloaded service-account JSON from Firebase Console, Project settings, Service accounts, Generate new private key |
| `CREST_STORAGE_BUCKET` | Keep `crest-files` |
| `CREST_SUPABASE_KEY_NAME` | Keep `default` unless the intended hosted Supabase secret key has another name |
| `CREST_ALLOWED_ORIGINS` | The final teacher website origin; fill this when Netlify is linked |
| `CREST_STORAGE_ENABLED` / `CREST_NOTIFICATIONS_ENABLED` | Keep both `false` for now |

The service-account value is the complete JSON content, including its private key,
not the downloaded file's path. The Supabase secret editor accepts the full JSON
as the secret value. For a local `.env` file, use single-quoted JSON on one line
while preserving escaped newlines inside the private key. Use a dedicated service
account with Firestore read, Authentication user read, and FCM sending permissions.
See [Firebase server setup](https://firebase.google.com/docs/admin/setup).

## 3. Client configuration later

The existing Android Firebase client uses `android/app/google-services.json`.
The website's public Firebase settings belong in `frontend/.env.local` and match
the web app configuration under Firebase Project settings. Web notifications also
need the public VAPID key from Firebase Project settings, Cloud Messaging, Web
Push certificates. These client files must never contain the server secret key
or the service-account private JSON.

The prepared integrations remain disabled. Entering credentials alone does not
run the app, copy files, change database settings, or deploy anything. Keep actual
secrets in the ignored local files or Supabase's secret store, rather than chat.
