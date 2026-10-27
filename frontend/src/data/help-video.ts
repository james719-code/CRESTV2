// Turns a shared video link into a safe embed URL. Only well-known video hosts are
// accepted, so a mistyped or malicious setting can never frame an arbitrary site.
const youtubeHosts = new Set(['youtube.com', 'www.youtube.com', 'm.youtube.com'])

function youtube(id: string | null | undefined) {
  return id && /^[\w-]{6,20}$/.test(id) ? `https://www.youtube-nocookie.com/embed/${id}` : null
}

export function helpVideoEmbedUrl(value: string | undefined): string | null {
  if (!value?.trim()) return null
  let url: URL
  try {
    url = new URL(value.trim())
  } catch {
    return null
  }
  if (url.protocol !== 'https:') return null
  const host = url.hostname.toLowerCase()
  const parts = url.pathname.split('/').filter(Boolean)

  if (youtubeHosts.has(host)) {
    if (parts[0] === 'watch') return youtube(url.searchParams.get('v'))
    if (parts[0] === 'embed' || parts[0] === 'shorts') return youtube(parts[1])
    return null
  }
  if (host === 'youtu.be') return youtube(parts[0])
  if (host === 'www.youtube-nocookie.com') return parts[0] === 'embed' ? youtube(parts[1]) : null
  if (host === 'vimeo.com' || host === 'player.vimeo.com') {
    const id = parts.find((part) => /^\d+$/.test(part))
    return id ? `https://player.vimeo.com/video/${id}` : null
  }
  if (host === 'drive.google.com') {
    const index = parts.indexOf('d')
    const id = index >= 0 ? parts[index + 1] : null
    return id && /^[\w-]+$/.test(id) ? `https://drive.google.com/file/d/${id}/preview` : null
  }
  if (host === 'www.loom.com' || host === 'loom.com') {
    const id = parts[0] === 'share' || parts[0] === 'embed' ? parts[1] : null
    return id && /^\w+$/.test(id) ? `https://www.loom.com/embed/${id}` : null
  }
  return null
}
