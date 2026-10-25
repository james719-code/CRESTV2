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

The current logo stays at `android/app/src/main/ic_launcher-playstore.png` with its existing Android resource versions. The preview reads that file directly; there is no duplicate master in this kit.

This branding proposal defines a simplified document/search direction in the brand guide. Final SVG lockups, monochrome artwork, favicons, and Android adaptive layers remain a separate logo-design deliverable. Do not use CSS text treatments as a substitute for final asset exports in store listings.

When adding final artwork, create `logos/` here and record its author/source, intended variants, export sizes, and revision. Keep institutional school seals separate from CREST product artwork.
