# CREST branding

CREST's shared identity for Android and the teacher website.

**Direction:** a clear, welcoming school research library. Keep the CREST name and recognizable blue identity; introduce ink navy, restrained teal, and Inter typography. Suggested tagline: **School research, shared.**

Status: brand kit **v1.1.0** is implemented in the Android and web source. Both use the bundled Inter family and generated light/dark semantic values. The original CREST document/search icon is retained and shared by the website, browser icons, Android UI, splash and launcher. The refresh is not an institutional approval or a deployed release.

The visual board is the product UI reference: the website's public landing, sign-in, teacher workspace, collections, forms, planning, help and reader chrome, and Android's sign-in, home, research, groups, documents, review/account, submission, planning and supporting screens use its semantic palette, Inter hierarchy, outlined paper surfaces, 12-unit controls, 16-unit cards, and shared status/strand pills. Platform-native navigation and layouts remain responsive rather than being identical screenshots.

## Start here

| File | Purpose |
| --- | --- |
| [BRAND_GUIDE.md](BRAND_GUIDE.md) | Identity, naming, palette, typography, logo direction, and voice |
| [PLATFORM_GUIDE.md](PLATFORM_GUIDE.md) | Android/web mappings, component rules, and adoption steps |
| [preview/index.html](preview/index.html) | Local visual board with light/dark themes and UI examples |
| [tokens/brand.tokens.json](tokens/brand.tokens.json) | Canonical machine-readable design values |
| [generated/web/crest-tokens.css](generated/web/crest-tokens.css) | Generated CSS variables and self-hosted Inter declaration |
| [generated/android/CrestBrandTokens.kt](generated/android/CrestBrandTokens.kt) | Generated Compose colors, typography, shapes, and dimensions |
| [assets/README.md](assets/README.md) | Font provenance and logo asset direction |
| [assets/logos/](assets/logos/) | Editable logo master, SVG lockups and portable icon exports |
| [scripts/generate.mjs](scripts/generate.mjs) | Dependency-free generator, drift check, and contrast checks |
| [scripts/generate-logos.mjs](scripts/generate-logos.mjs) | Dependency-free generator and drift check for all identity artwork |

Open `preview/index.html` in a browser. Its fonts and styling are local, so it also works offline after checkout.

## Keeping both platforms consistent

Edit `tokens/brand.tokens.json`, then run from the repository root:

```sh
node branding/scripts/generate.mjs
node branding/scripts/generate-logos.mjs
node branding/scripts/generate.mjs --check
node branding/scripts/generate-logos.mjs --check
```

The token generator also updates `android/app/src/main/java/com/bdbshs/crest/branding/CrestBrandTokens.kt`; the logo generator updates `frontend/public/` and Android resources, with matching originals in `assets/logos/`. Both `--check` commands detect drift in installed copies, and CI runs them for shared branding changes. Commit the source changes and regenerated outputs together. The checkers verify defined text/background pairs, control outlines, and adaptive artwork geometry; they do not certify complete screen accessibility. Do not fix generated values or logo exports by hand.

The JSON is a small CREST-specific token format, not an assertion of compatibility with a third-party token standard. Layout dimensions map to Android dp and web CSS pixels; text maps to Android sp and web rem. Device scaling can differ while hierarchy and proportions stay consistent.

## Adoption order

1. Review the visual board and the proposed identity.
2. Maintain the bundled Inter fonts, generated tokens and shared mark on each platform using the platform guide.
3. For new screens and components apply the shared status, typography and control rules.
4. Check both themes, long content, enlarged text, keyboard use, and TalkBack/screen-reader behavior.

See [the UI improvement plan](../docs/UI_IMPROVEMENT_PLAN.md) for the screen-by-screen sequence. This folder owns the shared brand values; the UI plan owns the redesign work order.

## Changes and ownership

Version 1.0.0 established the proposed palette, Inter, scale, voice, and platform contract. Version 1.1.0 adds the retained original document/search vector artwork, portable lockups, browser and Android icons, shared status colors and a restrained UI treatment. Increase the token and logo revisions together when adopted values/artwork change; record the reason here and regenerate both outputs. Artwork provenance is recorded in `assets/README.md`.

## Validation of this kit

On 2026-10-24, the generator and drift check passed all 70 defined contrast checks and verified the six bundled font/license hashes. The lowest checked text contrast was 5.11:1. The preview was checked in light/dark mode at desktop width and at 390 CSS pixels, with no horizontal overflow observed. Inter loaded locally; sample search, empty results, and save toggles worked.

On 2026-11-19, the v1.1 refresh was validated locally:

- All 70 token contrast pairs, the six font/license hashes and installed copies, and generated artwork outputs passed the drift checks.
- The original circular artwork is centered in the 66dp adaptive safe circle; the logo board shows actual 16/24/32px icons and circle/rounded-square launcher masks.
- The brand board rendered offline in light and dark at 1440px and 390px; search, empty results and save toggles were exercised.
- The built website was checked in isolated demo mode: light and dark landing pages at 1440px, 390px and 320px, plus desktop sign-in and teacher workspace. Inter and the new mark loaded, and the checked pages had no horizontal overflow or page-script errors.
- Web lint, 76 web tests, production build, 28 build-routing tests, 83 Android unit tests and the Android debug build passed.

The subsequent screen alignment was verified with:

- 78 web tests, lint and the production build; 83 Android unit tests and debug build; and 14 branded Android UI tests on an isolated Pixel emulator.
- 14 website routes at desktop width and nine representative routes at both 320px and 390px in light and dark themes, with no page-level horizontal overflow or page-script errors observed. Extra-large text, strong contrast, forms and reader controls were also exercised.
- All 70 defined contrast checks and 46 artwork outputs passed the branding drift checks; the original CREST symbol remains intact.

These are local source, build, browser and emulator checks. The launcher has not been visually checked on a physical device, and neither platform has been released as part of this alignment. See [the Android UI validation report](../docs/UI_UPGRADE.md) for earlier theme implementation and emulator checks.
