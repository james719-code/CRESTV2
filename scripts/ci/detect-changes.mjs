import { readFileSync, appendFileSync } from 'node:fs'
import { changedPaths } from './git-changes.mjs'
import { classifyPaths } from './paths.mjs'

const event = JSON.parse(readFileSync(process.env.GITHUB_EVENT_PATH, 'utf8'))
let changed
try {
  if (process.env.GITHUB_EVENT_NAME === 'workflow_dispatch') {
    const component = event.inputs?.component || 'all'
    if (!['all', 'android', 'web', 'backend'].includes(component)) throw new Error('Unknown component.')
    changed = Object.fromEntries(['android', 'web', 'backend'].map(key => [key, component === 'all' || component === key]))
  } else if (event.pull_request) {
    changed = classifyPaths(changedPaths(event.pull_request.base.sha, event.pull_request.head.sha, { mergeBase: true }))
  } else if (event.merge_group) {
    changed = classifyPaths(changedPaths(event.merge_group.base_sha, event.merge_group.head_sha))
  } else {
    changed = classifyPaths(changedPaths(event.before, process.env.GITHUB_SHA))
  }
} catch {
  // Missing history must never turn a required build into a false success.
  console.log('::warning::Could not compare changed files; validating all components.')
  changed = { android: true, web: true, backend: true }
}
for (const [component, run] of Object.entries(changed)) {
  appendFileSync(process.env.GITHUB_OUTPUT, `${component}=${run}\n`)
  console.log(`${component}: ${run ? 'run' : 'skip'}`)
}
if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY,
  `| Component | Decision |\n| --- | --- |\n${Object.entries(changed).map(([name, run]) => `| ${name} | ${run ? 'Run checks' : 'Skip: no relevant changes'} |`).join('\n')}\n`)
