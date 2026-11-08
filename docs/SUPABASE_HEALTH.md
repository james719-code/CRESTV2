# Supabase health and inactivity checks

`crest-health` is a public, read-only Edge Function for uptime monitoring:

```text
https://mkmirlnypehadajrtxvg.supabase.co/functions/v1/crest-health
```

Deployed and verified against the live database on 8 November 2026. The eight-hour
GitHub workflow is versioned in this repository and runs from its default branch,
`master`.

GET returns HTTP 200 only after a successful database read, including when the
library is empty. The response contains `status`, `database`, and `checkedAt`.
Configuration problems, failed database requests, and the 10-second database
timeout return HTTP 503. HEAD performs the same check with no response body.
Other methods return HTTP 405 without querying the database. Responses use
`Cache-Control: no-store` so each request checks the live database.

The function reads at most one ID from the existing `crest_storage_files` table
using the server credentials already configured for CREST. It returns no file
IDs, school records, credentials, or database error details. It requires no new
table, client changes, Firebase sign-in, or monitoring secrets. Its intentionally
public gateway setting applies only to `crest-health`.

## Deployment and scheduled checks

Deploy from the repository root with the existing authorized Supabase CLI login:

```powershell
supabase functions deploy crest-health --project-ref mkmirlnypehadajrtxvg --use-api
```

`.github/workflows/supabase-health.yml` calls the function every eight hours, at
00:17, 08:17, and 16:17 UTC (08:17, 16:17, and 00:17 in Manila).
It retries failures three times and fails the workflow on an unsuccessful HTTP
response. It needs no repository secrets and runs only in `james719-code/CRESTV2`.

**The schedule becomes active only after the workflow reaches the repository's
default branch and GitHub Actions is enabled.** After deployment and pushing the
workflow, run **Actions → Supabase health → Run workflow** once to verify it.
An external uptime monitor can also call the public URL with GET or HEAD on an
eight-hour schedule; it should require HTTP 200 and avoid caching the response.

Supabase Free projects can pause after a week of insufficient database activity.
These checks create real database reads, but keep-alive traffic is not a guarantee
against pausing. Supabase states that paid projects are not paused for inactivity.
See [Supabase's project-pausing guidance](https://supabase.com/kb/faqs/platform/free-project-pausing).
A project that is already paused must first be restored from the Supabase
dashboard; this endpoint cannot resume it.

GitHub can also disable scheduled workflows in public repositories after 60 days
without repository activity. Re-enable the workflow if this happens, or use an
independent uptime monitor. See [GitHub's scheduled-workflow guidance](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/disable-and-enable-workflows).

## Schedule choice and free-tier usage

Reviewed against official documentation on 8 November 2026. Supabase evaluates
insufficient database activity over a seven-day window, rather than promising
that one request resets a seven-day timer. Its guidance says a few user database
requests each day are typically enough, but does not publish a guaranteed minimum.
One weekly request is therefore not a supported minimum. Three checks spread
across each day are our practical choice, not a Supabase guarantee. This reduces
the original four-checks-per-day schedule by 25%. See
[Supabase project pausing](https://supabase.com/docs/guides/platform/free-project-pausing).

CRESTV2 was confirmed public through GitHub on 8 November 2026. The workflow uses
a standard `ubuntu-latest` runner; standard runners in public repositories are
free and do not consume the private-repository included compute allowance. The
workflow downloads no source, installs no dependencies, and creates no artifacts
or caches. See [GitHub Actions billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions).

For comparison if the repository becomes private, GitHub Free lists 2,000 Actions
minutes per billing month, shared with the owner's other eligible usage. A job's
billable duration rounds up to a whole minute. Estimates below assume a successful
job completes within one minute, and exclude manual runs and other workflows.
Use GitHub's run summaries to check actual duration. Retries remain in the same
job and can increase its duration. See
[included usage](https://docs.github.com/en/billing/reference/product-usage-included)
and [job execution time](https://docs.github.com/en/actions/how-tos/monitor-workflows/view-job-execution-time).

| Schedule | Scheduled runs in 31 days | Estimated private Linux minutes | Assessment |
| --- | --- | --- | --- |
| Once weekly | 4–5 | 4–5 | No published assurance this is enough activity |
| Once daily | 31 | 31 | Below the documented suggestion of a few requests daily |
| Every eight hours (selected) | 93 | 93 (4.65% of 2,000) | Three small reads per day |
| Every six hours (previous) | 124 | 124 (6.2% of 2,000) | Four small reads per day |

The selected schedule makes 93 Edge Function calls in a 31-day month if no retry
is needed, or up to 372 calls if every scheduled run makes all four attempts.
That is below 0.08% of Supabase's Free-plan allowance of 500,000 invocations per
billing cycle. Other CREST function calls share that allowance. See
[Edge Function pricing](https://supabase.com/docs/guides/functions/pricing).

The main ongoing limitation for a public repository is GitHub's 60-day repository
inactivity rule for scheduled workflows, not the monthly runtime allowance.
Scheduled checks do not make the setup maintenance-free. If the repository will
remain untouched for months, use an independent uptime monitor or re-enable the
workflow as needed. No external monitor has been configured here.

## Local validation

```powershell
deno check --config supabase/functions/deno.json supabase/functions/crest-health/index.ts
deno test --config supabase/functions/deno.json --allow-env=SUPABASE_URL,SUPABASE_SECRET_KEYS,CREST_SUPABASE_KEY_NAME,SUPABASE_SERVICE_ROLE_KEY supabase/functions/crest-health/handler.test.ts
```

Tests replace outgoing requests and use fake credentials; they do not access the
live database or write records.
