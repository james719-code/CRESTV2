const repositoryUrl = 'https://github.com/james719-code/CRESTV2'
export const androidDownloadUrl = `${repositoryUrl}/releases/latest/download/app-release.apk`
export const androidReleasesUrl = `${repositoryUrl}/releases/latest`
export const latestReleaseApiUrl =
  'https://api.github.com/repos/james719-code/CRESTV2/releases/latest'

export interface PublishedAppRelease {
  version: string
  downloadUrl: string
  notesUrl: string
  sizeBytes: number | null
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

// Only describe an actual, published Android APK; a Git tag alone is not a download.
export function parseAppRelease(value: unknown): PublishedAppRelease | null {
  if (!isRecord(value) || value.draft !== false || value.prerelease !== false) return null
  if (typeof value.tag_name !== 'string' || !Array.isArray(value.assets)) return null
  const version = /^(?:android-)?v(\d+\.\d+\.\d+)$/.exec(value.tag_name)?.[1]
  if (!version) return null
  const downloadUrl = `${repositoryUrl}/releases/download/${encodeURIComponent(value.tag_name)}/app-release.apk`
  const apk = value.assets.find(
    (asset: unknown) =>
      isRecord(asset) &&
      asset.name === 'app-release.apk' &&
      asset.state === 'uploaded' &&
      asset.browser_download_url === downloadUrl,
  )
  if (!isRecord(apk)) return null
  return {
    version,
    downloadUrl,
    notesUrl: `${repositoryUrl}/releases/tag/${encodeURIComponent(value.tag_name)}`,
    sizeBytes:
      typeof apk.size === 'number' && Number.isFinite(apk.size) && apk.size > 0 ? apk.size : null,
  }
}

export async function fetchAppRelease(
  signal: AbortSignal,
  fetchRelease: typeof fetch = fetch,
): Promise<PublishedAppRelease | null> {
  try {
    const response = await fetchRelease(latestReleaseApiUrl, {
      headers: { Accept: 'application/vnd.github+json' },
      signal,
    })
    if (!response.ok) return null
    return parseAppRelease(await response.json())
  } catch {
    // Rate limits, timeouts, and offline clients can still use the permanent download link.
    return null
  }
}
