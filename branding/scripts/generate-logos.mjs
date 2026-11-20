import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { deflateSync, inflateSync } from 'node:zlib'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

// The original symbol is the canonical artwork. The checked-in raster is a
// 512px transparent render of that SVG for dependency-free bitmap exports.
const root = fileURLToPath(new URL('../', import.meta.url))
const master = JSON.parse(await readFile(path.join(root, 'assets/logos/mark.json'), 'utf8'))
const { version, brand, themes } = JSON.parse(await readFile(path.join(root, 'tokens/brand.tokens.json'), 'utf8'))
const { blue, ink, paper, mist } = brand.palette
const { wordmarkPath } = master
const checking = process.argv.includes('--check')
if (master.revision !== version) throw new Error('The logo master and brand tokens must use the same revision.')
const outputs = new Map()
const put = (name, content) => outputs.set(name, content)
const asset = (name, content) => put(`assets/logos/${name}`, content)
const web = (name, content) => put(`../frontend/public/${name}`, content)
const res = (name, content) => put(`../android/app/src/main/res/${name}`, content)
const generated = `Generated from the original CREST artwork and mark.json v${master.revision}. Do not edit.`
const svg = (width, height, content, title = 'CREST') =>
  `<!-- ${generated} -->\n<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" role="img" aria-label="${title}">\n  ${content}\n</svg>\n`
const legacySvg = (await readFile(path.join(root, 'assets/logos', master.symbol), 'utf8')).replace(/\r\n/g, '\n')
const art = legacySvg.match(/<svg[^>]*>([\s\S]*?)<\/svg>/)?.[1].trim()
const originalPaths = [...legacySvg.matchAll(/<path d="([^"]+)" fill="(#[0-9a-f]{6})"\/>/gi)]
if (!art || originalPaths.length !== 180 || !legacySvg.includes('translate(-115.2 -115.2) scale(1.45)') ||
    !legacySvg.includes('translate(64 64) scale(0.75)')) throw new Error('The legacy SVG structure changed; review both platform transforms.')
const symbol = svg(512, 512, art)
const lockup = wordColor => svg(240, 64,
  `<g transform="scale(.125)">${art}</g>\n  <path d="${wordmarkPath}" fill="${wordColor}" transform="translate(79 48.4) scale(.022 -.022)"/>`)
const wordmark = wordColor => svg(164, 48,
  `<path d="${wordmarkPath}" fill="${wordColor}" transform="translate(1 40) scale(.022 -.022)"/>`)
asset('crest-symbol.svg', symbol)
asset('crest-lockup.svg', lockup(ink))
asset('crest-lockup-on-dark.svg', lockup(paper))
asset('crest-wordmark-ink.svg', wordmark(ink))
asset('crest-wordmark-white.svg', wordmark(paper))
asset('crest-adaptive-foreground.svg', svg(108, 108, `<g transform="translate(22 22) scale(.125)">${art}</g>`))
asset('crest-adaptive-background.svg', svg(108, 108, `<rect width="108" height="108" fill="${paper}"/>`))
web('crest-logo.svg', symbol)
web('favicon.svg', symbol)

// Decode and resize the retained 512px legacy raster. No recoloring or tracing.
const crcTable = Array.from({ length: 256 }, (_, i) => {
  for (let bit = 0; bit < 8; bit++) i = (i & 1) ? (0xEDB88320 ^ (i >>> 1)) : (i >>> 1)
  return i >>> 0
})
function chunk(type, data) {
  const payload = Buffer.concat([Buffer.from(type), data])
  const result = Buffer.alloc(data.length + 12)
  result.writeUInt32BE(data.length)
  payload.copy(result, 4)
  let crc = 0xffffffff
  for (const byte of payload) crc = (crc >>> 8) ^ crcTable[(crc ^ byte) & 0xff]
  result.writeUInt32BE((crc ^ 0xffffffff) >>> 0, result.length - 4)
  return result
}
const rasterFile = await readFile(path.join(root, 'assets/logos', master.raster))
const raster = pngPixels(rasterFile)
const sourceSize = raster.readUInt32BE(0)
if (sourceSize !== 512 || raster.readUInt32BE(4) !== sourceSize || raster[8] !== 8 || raster[9] !== 6 || raster[12] !== 0) {
  throw new Error('The legacy raster must be a 512px, non-interlaced RGBA PNG.')
}
const decoded = Buffer.alloc(sourceSize * sourceSize * 4)
const sourceStride = sourceSize * 4
function paeth(a, b, c) {
  const p = a + b - c, da = Math.abs(p - a), db = Math.abs(p - b), dc = Math.abs(p - c)
  return da <= db && da <= dc ? a : db <= dc ? b : c
}
for (let y = 0; y < sourceSize; y++) {
  const row = 13 + y * (sourceStride + 1)
  const filter = raster[row]
  if (filter > 4) throw new Error('Invalid legacy PNG filter.')
  for (let x = 0; x < sourceStride; x++) {
    const offset = y * sourceStride + x
    const a = x >= 4 ? decoded[offset - 4] : 0
    const b = y > 0 ? decoded[offset - sourceStride] : 0
    const c = x >= 4 && y > 0 ? decoded[offset - sourceStride - 4] : 0
    const prediction = [0, a, b, Math.floor((a + b) / 2), paeth(a, b, c)][filter]
    decoded[offset] = (raster[row + 1 + x] + prediction) & 255
  }
}
function png(size, { shape = 'round', padding = 0 } = {}) {
  const stride = size * 4 + 1
  const scanlines = Buffer.alloc(stride * size)
  const scale = 1 - padding / 32
  const sample = Math.min(16, Math.max(2, Math.ceil(sourceSize / (size * scale))))
  const total = sample * sample
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
    let alpha = 0, red = 0, green = 0, blue = 0
    for (let sy = 0; sy < sample; sy++) for (let sx = 0; sx < sample; sx++) {
      const px = ((x + (sx + .5) / sample) / size - .5) / scale + .5
      const py = ((y + (sy + .5) / sample) / size - .5) / scale + .5
      const inside = px >= 0 && px < 1 && py >= 0 && py < 1
      const source = (Math.floor(py * sourceSize) * sourceSize + Math.floor(px * sourceSize)) * 4
      const a = inside ? decoded[source + 3] : 0
      const background = shape === 'square' ? 255 - a : 0
      alpha += a + background
      red += (inside ? decoded[source] * a : 0) + 255 * background
      green += (inside ? decoded[source + 1] * a : 0) + 255 * background
      blue += (inside ? decoded[source + 2] * a : 0) + 255 * background
    }
    const offset = y * stride + 1 + x * 4
    scanlines[offset] = alpha ? Math.round(red / alpha) : 0
    scanlines[offset + 1] = alpha ? Math.round(green / alpha) : 0
    scanlines[offset + 2] = alpha ? Math.round(blue / alpha) : 0
    scanlines[offset + 3] = Math.round(alpha / total)
  }
  const header = Buffer.alloc(13)
  header.writeUInt32BE(size, 0)
  header.writeUInt32BE(size, 4)
  header[8] = 8
  header[9] = 6
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', header),
    chunk('IDAT', deflateSync(scanlines)), chunk('IEND', Buffer.alloc(0))])
}
const faviconPngs = [16, 24, 32, 48, 96].map(size => [size, png(size)])
for (const [size, bytes] of faviconPngs) asset(`favicon-${size}x${size}.png`, bytes)
const icoImages = faviconPngs.filter(([size]) => [16, 32, 48].includes(size))
const icoHeader = Buffer.alloc(6 + icoImages.length * 16)
icoHeader.writeUInt16LE(1, 2)
icoHeader.writeUInt16LE(icoImages.length, 4)
let offset = icoHeader.length
icoImages.forEach(([size, bytes], i) => {
  const start = 6 + i * 16
  icoHeader[start] = icoHeader[start + 1] = size
  icoHeader.writeUInt16LE(1, start + 4)
  icoHeader.writeUInt16LE(32, start + 6)
  icoHeader.writeUInt32LE(bytes.length, start + 8)
  icoHeader.writeUInt32LE(offset, start + 12)
  offset += bytes.length
})
const ico = Buffer.concat([icoHeader, ...icoImages.map(([, bytes]) => bytes)])
asset('favicon.ico', ico)
web('favicon.ico', ico)
web('favicon-96x96.png', faviconPngs.at(-1)[1])
const apple = png(180, { shape: 'square', padding: 3 })
asset('apple-touch-icon.png', apple)
web('apple-touch-icon.png', apple)
for (const size of [192, 512]) {
  const bytes = png(size, { shape: 'square', padding: 8 })
  asset(`web-maskable-${size}.png`, bytes)
  web(`crest-maskable-${size}.png`, bytes)
}
const store = await readFile(path.join(root, 'assets/logos', master.store))
asset('crest-app-icon-512.png', store)
put('../android/app/src/main/ic_launcher-playstore.png', store)
web('site.webmanifest', JSON.stringify({
  name: brand.name,
  short_name: brand.name,
  description: brand.descriptor,
  start_url: '/',
  display: 'standalone',
  theme_color: blue,
  background_color: mist,
  icons: [192, 512].map(size => ({ src: `/crest-maskable-${size}.png`, sizes: `${size}x${size}`, type: 'image/png', purpose: 'any maskable' }))
}, null, 2) + '\n')

// Preserve each original path, its color, and the legacy UI transforms. The
// 512-unit circle scales to 64dp centered in the 108dp adaptive/splash layer.
const vector = (size, content, viewport = size) => `<?xml version="1.0" encoding="utf-8"?>\n<!-- ${generated} -->\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="${size}dp" android:height="${size}dp"\n    android:viewportWidth="${viewport}" android:viewportHeight="${viewport}">\n    ${content}\n</vector>\n`
const circle = 'M0 256A256 256 0 1 0 512 256A256 256 0 1 0 0 256Z'
const androidPaths = originalPaths.map(([, d, fill]) =>
  `<path android:pathData="${d}" android:fillColor="${fill}"/>`).join('\n            ')
const vArt = `<clip-path android:pathData="${circle}"/>\n        <path android:pathData="${circle}" android:fillColor="#FFFFFF"/>\n        <group android:translateX="-115.2" android:translateY="-115.2" android:scaleX="1.45" android:scaleY="1.45">\n          <group android:translateX="64" android:translateY="64" android:scaleX=".75" android:scaleY=".75">\n            ${androidPaths}\n          </group>\n        </group>`
const padded = `<group android:translateX="22" android:translateY="22" android:scaleX=".125" android:scaleY=".125">\n        ${vArt}\n    </group>`
res('drawable/crest_mark.xml', vector(64, vArt, 512))
res('drawable/crest_launcher_foreground.xml', vector(108, padded))
res('drawable/crest_launcher_background.xml', vector(108, `<path android:fillColor="${paper}" android:pathData="M0 0H108V108H0Z"/>`))
res('drawable/crest_splash.xml', vector(108, padded))
// The detailed legacy identity is not flattened into a misleading silhouette.
const adaptive = () => `<?xml version="1.0" encoding="utf-8"?>\n<!-- ${generated} -->\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@drawable/crest_launcher_background"/>\n    <foreground android:drawable="@drawable/crest_launcher_foreground"/>\n</adaptive-icon>\n`
for (const name of ['crest_launcher', 'crest_launcher_round']) {
  res(`mipmap-anydpi-v26/${name}.xml`, adaptive())
  res(`mipmap-anydpi-v33/${name}.xml`, adaptive())
}
for (const [density, size] of Object.entries({ mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 })) {
  res(`mipmap-${density}/crest_launcher.png`, png(size, { shape: 'square' }))
  res(`mipmap-${density}/crest_launcher_round.png`, png(size))
}
for (const [theme, qualifier] of [['light', 'values'], ['dark', 'values-night']]) {
  res(`${qualifier}/crest_brand.xml`, `<?xml version="1.0" encoding="utf-8"?>\n<!-- ${generated} -->\n<resources>\n    <color name="crest_splash_background">${themes[theme].material.background}</color>\n</resources>\n`)
}

// Compare all portable masters and installed copies, not just web assets.
// Compression bytes may change with the platform's zlib build. Exact matches
// take the fast path; otherwise verify the same image headers and pixel rows.
function pngPixels(bytes) {
  if (!bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))) throw new Error('Not a PNG')
  let header, end = false
  const idat = []
  for (let cursor = 8; cursor + 12 <= bytes.length;) {
    const length = bytes.readUInt32BE(cursor)
    if (cursor + length + 12 > bytes.length) throw new Error('Truncated PNG')
    const type = bytes.toString('ascii', cursor + 4, cursor + 8)
    const data = bytes.subarray(cursor + 8, cursor + 8 + length)
    if (!chunk(type, data).equals(bytes.subarray(cursor, cursor + length + 12))) throw new Error('Invalid PNG chunk')
    if (type === 'IHDR') header = data
    if (type === 'IDAT') idat.push(data)
    if (type === 'IEND') end = true
    cursor += length + 12
  }
  if (!header || !end || !idat.length) throw new Error('Incomplete PNG')
  return Buffer.concat([header, inflateSync(Buffer.concat(idat))])
}
function icoPixels(bytes) {
  if (bytes.readUInt32LE(0) !== 0x00010000) throw new Error('Not an ICO')
  const count = bytes.readUInt16LE(4)
  return Array.from({ length: count }, (_, i) => {
    const entry = 6 + i * 16
    const length = bytes.readUInt32LE(entry + 8), offset = bytes.readUInt32LE(entry + 12)
    return Buffer.concat([bytes.subarray(entry, entry + 8), pngPixels(bytes.subarray(offset, offset + length))])
  })
}
function sameRaster(name, actual, expected) {
  if (!actual) return false
  if (actual.equals(expected)) return true
  try {
    if (name.endsWith('.png')) return pngPixels(actual).equals(pngPixels(expected))
    if (name.endsWith('.ico')) {
      const a = icoPixels(actual), b = icoPixels(expected)
      return a.length === b.length && a.every((pixels, i) => pixels.equals(b[i]))
    }
  } catch {
    return false
  }
  return false
}
let drift = false
for (const [name, content] of outputs) {
  const destination = path.join(root, name)
  if (checking) {
    const actual = await readFile(destination).catch(() => null)
    const matches = typeof content === 'string'
      ? actual?.toString('utf8').replace(/\r\n/g, '\n') === content
      : sameRaster(name, actual, content)
    if (!matches) {
      console.error(`Out of date: ${name}`)
      drift = true
    }
  } else {
    await mkdir(path.dirname(destination), { recursive: true })
    await writeFile(destination, content)
  }
}
if (drift) process.exitCode = 1
else console.log(`CREST legacy identity v${master.revision}: ${outputs.size} assets ${checking ? 'in sync' : 'generated'}; all 180 original logo paths preserved.`)
