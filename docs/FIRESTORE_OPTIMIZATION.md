# Firestore access and optimization review

Reviewed on 26 October 2026 for Firebase project `crest-4fb99`, database `(default)`.

## Inspected data

| Collection | Records | Current access |
| --- | ---: | --- |
| `users/user_details/students` | 87 | Own profile; approved teacher account management; group peers by document ID |
| `users/user_details/teachers` | 8 | Own role/status; approved teachers; notification recipients filtered by `access` |
| `groups` | 2 | Teacher list ordered by `group_name`; student lookup by group code; pending-submission count |
| `researches/research_details/qualitative` | 2 | Recent papers, cursor pagination, strand totals, PDF-reference lookup |
| `researches/research_details/quantitative` | 10 | Same access as qualitative research |
| `documents` | 0 | Date ordering and PDF-reference lookup, prepared for future uploads |
| `users` directly | 2 | Legacy account plus metadata; denied to client applications |
| `researches` directly | 24 | 23 legacy records plus metadata; denied to client applications |

There were two pending students, one pending teacher, and no pending research
submissions. Student group aliases (`groupId` and `group_id`) agreed. Every group
member had a student profile, and the profile links agreed with membership.
All 12 published research records had integer millisecond `createdAt` values.

The legacy account has a `password` field. Its value was not included in audit
output. The deployed catch-all rules deny access to these parent records for
anonymous, pending, student, and teacher clients. Authentication uses Firebase
Auth; the old record is excluded from indexing. Deciding whether to remove the
legacy record and its credential field is separate retention work.

## Deployed index changes

`firebase/firestore.indexes.json` now defines one composite index and 31 field
overrides. Collection IDs match subcollections: changing `users` indexing does
not change `students` or `teachers` indexing, and changing `researches` does not
change `qualitative` or `quantitative` indexing.

- Keep the `groups(uploaded ASC, accepted_research ASC)` composite index for the
  pending-submission dashboard count.
- Remove the obsolete parent `users(access, type, name)` composite index. No
  current application query uses it.
- Default the six active collection groups to ascending scalar indexes. Keep
  both ascending and descending `createdAt` indexes for research and documents.
- Exempt display-only arrays and text: group comments, group membership,
  submission titles, research titles/authors, and document descriptions.
- Exempt counters with no server-side filtering or sorting (`downloads`, legacy
  `views`, and teacher `upload_count`) and redundant profile fields used only in
  record reads (`uid`, legacy `group_id`, `research_accepted`, `lrn`, `gender`).
- Exempt all fields in the unused parent `users` and `researches` collections
  and the owner-scoped favorites `items` collections. Reading/listening to known
  documents and collections still works without secondary field indexes.

The single-field estimate for the 109 active records changes from approximately
1,499 entries to 456 (70% fewer). This excludes document-name and composite
indexes, legacy records, and storage-byte overhead. It is a structural estimate,
not a measured billing or latency reduction. The main gain is lower index fanout
on future writes, especially frequently incremented counters. Firebase recommends
removing unused descending/array indexes and exempting fields that are not
queried. See [Firestore best practices](https://firebase.google.com/docs/firestore/best-practices).

The following access remains indexed:

| Operation | Index |
| --- | --- |
| Research/document newest-first pages | `createdAt DESC` |
| Research oldest-first pages | `createdAt ASC` |
| Student dashboard research counts by strand | `strand ASC` in both research collections |
| Pending accounts | `students.accepted ASC`, `teachers.access ASC` |
| Teacher notification recipients | `teachers.access ASC` |
| Group browser | `groups.group_name ASC` |
| Pending submissions | `groups.uploaded ASC, accepted_research ASC` |
| Private PDF authorization and Android storage lookup | `file_link ASC` in groups, both research collections, and documents |
| Group peers and role/profile lookup | Built-in document-name index |

## Android read improvements

Dashboard counts continue to use Firestore aggregation queries. The offline
fallback now reads `Source.CACHE` only on `UNAVAILABLE`. Permission, index, and
cancellation errors propagate instead of triggering an unbounded collection
download. Offline totals can still be incomplete because the cache may contain
only part of a collection. Aggregations reduce document reads and transferred
bytes compared with downloading documents just to count them; see
[Firestore aggregation queries](https://firebase.google.com/docs/firestore/query-data/aggregation-queries).

Download-counter updates use the research type already resolved by the detail
screen, removing one existence read per increment and selecting the correct
collection even if two research types have the same document ID. These client
improvements require distributing the updated Android application.

## Validation and repeatable audit

- 52 Android unit tests passed, including offline counts, denied/missing-index
  errors, cancellation, and direct counter updates for both research types.
- The Android debug APK built successfully.
- 18 backend tests passed: 14 access-rule tests in the local emulator and four
  index contract checks. Legacy parent-record access was explicitly denied for
  all client roles.
- The production audit checks 20 real query shapes using Google IAM: exact
  aggregation counts plus Query Explain plans for library, group, account,
  notification, and file-reference access. `--verify-config` also checks every
  configured field override and the composite index against the live `READY`
  configuration. Emulator tests alone do not establish production index coverage.
- Final production verification passed all 20 checks after the index build
  completed. All 31 configured field overrides matched, every retained index was
  `READY`, the unused composite index was gone, and all aggregation counts agreed
  with the before-deployment results. Firestore access rules were not modified.

Run from the repository root after signing in with the Firebase CLI:

```powershell
node scripts/audit-firestore.mjs --project crest-4fb99 --verify-config
```

The script only reads Firestore. It reports aggregate counts and index metadata,
never profile values, passwords, access tokens, or PDF references. The default
Firebase CLI location follows the Windows npm installation; use
`--firebase-tools-dir <directory>` for another installation. `--config-only`
inspects index state without running the application queries.

Before/after evidence, configuration snapshots, and test output are in the
ignored `state/backend-migration/firestore-optimization/` directory. The final
verification is `audit-after.json`; `configurationVerified` must be true and
`queries` must contain all 20 checks. Google can take several minutes to finish
field-index updates after the deployment command exits.

The original configuration is saved as `indexes-local-before.json` in that
directory. To restore the former indexing behavior, copy that snapshot back to
`firebase/firestore.indexes.json`, deploy only `firestore:indexes`, and wait for
the restored indexes to become ready. No record migration is required.

## Growth considerations

The current teacher website listens to all six active collections, supporting
whole-workspace search, exports, summaries, and cross-record joins. Its initial
read cost grows with the complete workspace. At larger enrollment, introduce
server-side cursor pages and separate aggregate summaries together; adding a
bare query limit would make those existing features incomplete.

When moving strand/status/date combinations to server-side filters, add only
the composite indexes those new queries require, such as `strand, createdAt`.
Title/author search currently runs on the client; it does not use a Firestore
full-text index. Android research filters/search also operate on loaded pages.
Keep that limitation visible when designing search over a larger library.

Keep both group aliases until all supported clients use a single field. No
profile, membership, approval, file reference, historical view count, or research
record was rewritten during this optimization.
