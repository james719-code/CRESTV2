// Gathers the license text of every bundled open-source package into one public file.
// Run `npm run licenses:sync` after changing dependencies; `--check` fails if it is stale.
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { credits } from '../src/data/credits.ts'

const root = new URL('../', import.meta.url)
const output = fileURLToPath(new URL('public/licenses/THIRD-PARTY-NOTICES.txt', root))
const rule = '='.repeat(72)

const shadcn = `shadcn/ui
License: MIT
https://ui.shadcn.com

MIT License

Copyright (c) 2023 shadcn

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.`

function packageFile(name, file) {
  return fileURLToPath(new URL(`node_modules/${name}/${file}`, root))
}

function licenseText(name, spdx) {
  for (const file of ['LICENSE', 'LICENSE.md', 'LICENSE.txt', 'license', 'LICENCE']) {
    const path = packageFile(name, file)
    if (existsSync(path)) return readFileSync(path, 'utf8').trim()
  }
  // The Firebase SDK ships without a license file; it is published under Apache-2.0.
  if (spdx === 'Apache-2.0') {
    const apache = readFileSync(packageFile('class-variance-authority', 'LICENSE'), 'utf8')
    const end = 'END OF TERMS AND CONDITIONS'
    return `Copyright Google LLC\n\n${apache.slice(0, apache.indexOf(end) + end.length).trim()}`
  }
  throw new Error(`No license text found for ${name}`)
}

function build() {
  const parts = [
    'CREST web: third-party notices\n\nThe CREST website includes the open-source software and fonts below. Each is used under the license shown, and the full license text follows.\n',
  ]
  for (const credit of credits) {
    if (credit.name === 'shadcn/ui') {
      parts.push(`${rule}\n${shadcn}`)
      continue
    }
    if (credit.name === 'Inter') {
      const text = readFileSync(
        fileURLToPath(new URL('public/licenses/Inter-OFL.txt', root)),
        'utf8',
      )
      parts.push(
        `${rule}\nInter (font)\nLicense: SIL Open Font License 1.1\n${credit.url}\n\n${text.trim()}`,
      )
      continue
    }
    for (const name of credit.packages) {
      const { version } = JSON.parse(readFileSync(packageFile(name, 'package.json'), 'utf8'))
      parts.push(
        `${rule}\n${name} ${version}\nLicense: ${credit.license}\n${credit.url}\n\n${licenseText(name, credit.license)}`,
      )
    }
  }
  return `${parts.join('\n\n')}\n`
}

const next = build()
if (process.argv.includes('--check')) {
  const current = existsSync(output) ? readFileSync(output, 'utf8') : ''
  if (current !== next) {
    console.error('THIRD-PARTY-NOTICES.txt is out of date. Run `npm run licenses:sync`.')
    process.exit(1)
  }
  console.log('Third-party notices are up to date.')
} else {
  writeFileSync(output, next)
  console.log(`Wrote ${output}`)
}
