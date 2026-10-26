import { readFileSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

// Reuse the original Android foreground paths without redrawing or recoloring them.
// The round white base and 1.45 scale match ui/components/AppLogo.kt's CrestMark.
const source = new URL(
  '../../android/app/src/main/res/drawable/ic_launcher_foreground.xml',
  import.meta.url,
)
const xml = readFileSync(source, 'utf8')
const paths = [...xml.matchAll(/<path\s+([\s\S]*?)\/>/g)].map(([, attributes]) => {
  const values = Object.fromEntries(
    [...attributes.matchAll(/android:(\w+)="([^"]*)"/g)].map(([, key, value]) => [key, value]),
  )
  for (const key of Object.keys(values)) {
    if (!['pathData', 'fillColor'].includes(key))
      throw new Error(`Unsupported Android logo attribute: ${key}`)
  }
  if (!values.pathData || !/^#[0-9a-f]{6}$/i.test(values.fillColor))
    throw new Error('Unexpected Android logo path format.')
  return `    <path d="${values.pathData}" fill="${values.fillColor}"/>`
})
if (
  paths.length !== 180 ||
  !xml.includes('android:scaleX="0.75"') ||
  !xml.includes('android:translateX="64"')
) {
  throw new Error('The Android logo has changed. Review its transforms before exporting.')
}
const svg = `<!-- Generated from the Android launcher foreground by scripts/sync-logo.mjs. -->
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" fill="none">
  <defs><clipPath id="crest-circle"><circle cx="256" cy="256" r="256"/></clipPath></defs>
  <g clip-path="url(#crest-circle)">
    <circle cx="256" cy="256" r="256" fill="#FFFFFF"/>
    <g transform="translate(-115.2 -115.2) scale(1.45)">
      <g transform="translate(64 64) scale(0.75)">
${paths.join('\n')}
      </g>
    </g>
  </g>
</svg>
`
const output = new URL('../public/crest-logo.svg', import.meta.url)
if (process.argv.includes('--check')) {
  if (readFileSync(output, 'utf8') !== svg)
    throw new Error('The web logo is out of sync with Android.')
  console.log('All 180 Android logo paths and the CrestMark transforms match the website asset.')
} else {
  writeFileSync(output, svg)
  console.log(`Exported the original Android logo to ${fileURLToPath(output)}`)
}
