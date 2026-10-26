// Shared by GitHub Actions and Netlify. Paths are relative to the repository root.
export function classifyPaths(paths) {
  const changed = { android: false, web: false, backend: false }
  for (const path of paths) {
    if (path.startsWith('docs/') || /\.(md|mdx|rst|adoc)$/i.test(path)) continue

    if (path === '.github/workflows/ci.yml' || path.startsWith('scripts/ci/')) {
      changed.android = changed.web = changed.backend = true
    }
    if (path.startsWith('android/') || path === '.github/workflows/release.yml') changed.android = true
    if (path.startsWith('frontend/') && !path.startsWith('frontend/backend-tests/')) changed.web = true
    if (path === 'netlify.toml') changed.web = true

    // These are shared design inputs; generated platform files are independent.
    if (['branding/tokens/', 'branding/scripts/', 'branding/assets/fonts/'].some(prefix => path.startsWith(prefix))) {
      changed.android = changed.web = true
    }
    if (path.startsWith('branding/generated/android/')) changed.android = true
    if (path.startsWith('branding/generated/web/')) changed.web = true

    if (path.startsWith('firebase/') || path.startsWith('supabase/') || path.startsWith('frontend/backend-tests/')
      || path === 'firebase.json' || path === '.firebaserc'
      || /^scripts\/(migrate-|download-appwrite)/.test(path)
      || ['frontend/package.json', 'frontend/package-lock.json'].includes(path)) changed.backend = true
  }
  return changed
}
