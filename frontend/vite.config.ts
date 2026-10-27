import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'
import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'
import { defineConfig, type Plugin } from 'vite'
import { scopePdfCss } from './scripts/scope-pdf-css.ts'

function scopedPdfStyles(): Plugin {
  return {
    name: 'crest-scope-pdf-viewer-css',
    enforce: 'pre',
    transform(source, id) {
      const path = id.replaceAll('\\', '/').split('?')[0]
      if (path.endsWith('/pdfjs-dist/web/pdf_viewer.css'))
        return { code: scopePdfCss(source), map: null }
    },
  }
}

// Serve PDF.js support data locally, including fonts and decoders needed by
// scanned/CJK papers. Versioned paths are safe for the existing asset cache.
function pdfAssets(): Plugin {
  const root = fileURLToPath(new URL('./node_modules/pdfjs-dist/', import.meta.url))
  const version = JSON.parse(readFileSync(join(root, 'package.json'), 'utf8')).version
  const prefix = `/assets/pdfjs-${version}/`
  const files = new Map<string, string>()
  function collect(directory: string, relative: string) {
    for (const file of readdirSync(directory, { withFileTypes: true })) {
      const name = `${relative}/${file.name}`
      if (file.isDirectory()) collect(join(directory, file.name), name)
      else files.set(name, join(directory, file.name))
    }
  }
  for (const directory of ['cmaps', 'standard_fonts', 'wasm', 'iccs'])
    collect(join(root, directory), directory)
  collect(join(root, 'web/images'), 'images')
  return {
    name: 'crest-pdf-support-assets',
    generateBundle() {
      for (const [name, path] of files)
        this.emitFile({
          type: 'asset',
          fileName: `${prefix.slice(1)}${name}`,
          source: readFileSync(path),
        })
    },
    configureServer(server) {
      server.middlewares.use(prefix, (request, response, next) => {
        const name = (request.url || '').split('?')[0].replace(/^\//, '')
        const file = files.get(name)
        if (!file) return next()
        const mime = name.endsWith('.wasm')
          ? 'application/wasm'
          : name.endsWith('.js')
            ? 'text/javascript; charset=utf-8'
            : name.endsWith('.svg')
              ? 'image/svg+xml'
              : name.endsWith('.png')
                ? 'image/png'
                : 'application/octet-stream'
        response.setHeader('Content-Type', mime)
        response.end(readFileSync(file))
      })
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [scopedPdfStyles(), react(), tailwindcss(), pdfAssets()],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  build: { manifest: true },
  server: {
    fs: {
      allow: [
        fileURLToPath(new URL('.', import.meta.url)),
        fileURLToPath(new URL('../branding', import.meta.url)),
      ],
    },
  },
})
