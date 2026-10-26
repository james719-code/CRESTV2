-- Private storage and server-only metadata. Firebase identity is checked by Edge Functions.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('crest-files', 'crest-files', false, 20971520, array['application/pdf'])
on conflict (id) do update set public = false, file_size_limit = 20971520,
  allowed_mime_types = array['application/pdf'];

create table public.crest_storage_files (
  id text primary key check (id ~ '^sb_[0-9a-f-]{36}$'),
  legacy_file_id text unique,
  bucket text not null default 'crest-files',
  object_path text not null unique,
  owner_uid text,
  kind text not null check (kind in ('research', 'document', 'group', 'migration')),
  group_id text,
  size_bytes bigint not null check (size_bytes > 0 and size_bytes <= 20971520),
  sha256 text,
  created_at timestamptz not null default now()
);

create table public.crest_push_devices (
  id text primary key,
  firebase_uid text not null,
  token text not null unique,
  platform text not null check (platform in ('android', 'web')),
  updated_at timestamptz not null default now()
);
create index crest_push_devices_uid on public.crest_push_devices(firebase_uid);

create table public.crest_notification_events (
  id text primary key,
  status text not null default 'sending',
  sent_count integer not null default 0,
  failed_count integer not null default 0,
  created_at timestamptz not null default now()
);

alter table public.crest_storage_files enable row level security;
alter table public.crest_push_devices enable row level security;
alter table public.crest_notification_events enable row level security;
revoke all on public.crest_storage_files from anon, authenticated;
revoke all on public.crest_push_devices from anon, authenticated;
revoke all on public.crest_notification_events from anon, authenticated;
grant all on public.crest_storage_files to service_role;
grant all on public.crest_push_devices to service_role;
grant all on public.crest_notification_events to service_role;
