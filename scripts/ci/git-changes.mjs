import { execFileSync } from 'node:child_process'

export function changedPaths(base, head = 'HEAD', { cwd, mergeBase = false } = {}) {
  const git = args => execFileSync('git', args, { cwd, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'], maxBuffer: 32 * 1024 * 1024 })
  // New branches have no "before" commit; their first run validates the full tree.
  if (!base || /^0+$/.test(base)) return git(['ls-tree', '--full-tree', '-r', '--name-only', '-z', head]).split('\0').filter(Boolean)
  if (mergeBase) base = git(['merge-base', base, head]).trim()
  // Disable rename detection so a move across components includes both paths.
  return git(['diff', '--name-only', '--no-relative', '--no-renames', '-z', base, head, '--']).split('\0').filter(Boolean)
}
