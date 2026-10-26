import { changedPaths } from './git-changes.mjs'
import { classifyPaths } from './paths.mjs'

export function shouldBuildWeb(base, head, options) {
  // Netlify uses equal commit refs when a build is requested without cache.
  if (!base || !head || base === head) return true
  try { return classifyPaths(changedPaths(base, head, options)).web }
  catch { return true }
}

if (process.argv.includes('--run')) {
  const build = shouldBuildWeb(process.env.CACHED_COMMIT_REF, process.env.COMMIT_REF)
  console.log(build ? 'Website inputs changed or comparison unavailable; continuing build.' : 'No website inputs changed; skipping Netlify build.')
  // Netlify's ignore command uses 0 = skip and 1 = build.
  process.exitCode = build ? 1 : 0
}
