# Brand assets

## Inter fonts

The `fonts/` directory contains the selected Inter 4.1 assets from the [official release](https://github.com/rsms/inter/releases/tag/v4.1):

- `InterVariable.woff2`: web, upright variable font.
- `Inter-Regular.ttf`: Android weight 400.
- `Inter-Medium.ttf`: Android weight 500.
- `Inter-SemiBold.ttf`: Android weight 600.
- `Inter-Bold.ttf`: Android weight 700.
- `LICENSE.txt`: the upstream license, retained unchanged.
- `source.json`: source archive, selected entry paths, and SHA-256 hashes.

Use the font files locally. Do not require Google Fonts/CDN access for the sign-in screen or offline papers. Do not modify font artwork or rename the font family. Include the license whenever distributing these assets.

No italic face is bundled. Request and include the matching upstream italic face if an interface actually needs italic text.

## Logo artwork

The v1.1.0 kit retains the project's original CREST icon at the user's request. `legacy-symbol.svg` is the previous website export of all 180 paths from the original Android launcher foreground, including its original colors and circular UI transforms. `legacy-symbol-raster.png` is a transparent 512px render of that SVG for bitmap exports. `legacy-original.png` is the original, unchanged 512px store artwork. The wordmark outlines are formed from Inter Bold 4.1 with 0.06em tracking. No third-party logo, school seal or stock artwork was introduced.

| Files | Use |
| --- | --- |
| `mark.json` | References to original artwork and the outlined wordmark |
| `legacy-symbol.svg`, `legacy-symbol-raster.png`, `legacy-original.png` | Original vector symbol, corresponding raster and original store source |
| `crest-symbol.svg` | Full-color original circular symbol |
| `crest-lockup*.svg` | Horizontal primary/light and primary/dark lockups |
| `crest-wordmark*.svg` | Text-only ink and reversed white wordmarks |
| `crest-adaptive-*.svg` | Portable 108-unit Android foreground and light background layers |
| `favicon-*.png`, `favicon.ico` | 16/24/32/48/96px browser artwork; ICO includes 16/32/48px |
| `apple-touch-icon.png` | 180px Apple touch icon |
| `web-maskable-*.png` | 192 and 512px full-bleed, mask-safe website icons |
| `crest-app-icon-512.png` | Square 512px store/source export |

Keep the original artwork unchanged. Regenerate using `node branding/scripts/generate-logos.mjs` from the repository root. The script preserves all 180 original paths and their colors while producing SVGs, raster images, Android density resources, vectors and launcher configuration, website copies and manifest. It centers the full circle within the adaptive safe area. No alternate monochrome launcher is supplied because flattening the detailed icon would lose its identity. School artwork is unchanged.
