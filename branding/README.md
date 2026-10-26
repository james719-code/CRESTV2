# CREST branding

CREST's shared identity for Android and the teacher website.

**Direction:** a clear, welcoming school research library. Keep the CREST name and recognizable blue identity; introduce ink navy, restrained teal, and Inter typography. Suggested tagline: **School research, shared.**

Status: brand baseline v1.0.0 is integrated into the Android UI. The app bundles Inter and uses the generated light/dark themes and semantic status colors. The existing launcher artwork is retained, and the web app has not been built.

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
| [scripts/generate.mjs](scripts/generate.mjs) | Dependency-free generator, drift check, and contrast checks |

Open `preview/index.html` in a browser. Its fonts and styling are local, so it also works offline after checkout.

## Keeping both platforms consistent

Edit `tokens/brand.tokens.json`, then run from the repository root:

```sh
node branding/scripts/generate.mjs
node branding/scripts/generate.mjs --check
```

The generator also updates `android/app/src/main/java/com/bdbshs/crest/branding/CrestBrandTokens.kt`; `--check` detects drift in that installed copy. Commit the token changes and regenerated outputs together. The checker verifies the defined text/background pairs and meaningful control outlines; it does not certify complete screen accessibility. Do not fix generated values by hand.

The JSON is a small CREST-specific token format, not an assertion of compatibility with a third-party token standard. Layout dimensions map to Android dp and web CSS pixels; text maps to Android sp and web rem. Device scaling can differ while hierarchy and proportions stay consistent.

## Adoption order

1. Review the visual board and the proposed tagline/logo direction.
2. Install the bundled Inter fonts and generated tokens on each platform using the platform guide.
3. Apply the foundation to navigation and research browsing, then the home screens.
4. Finish upload, reader, supporting screens, and feedback states using the same component rules.
5. Check both themes, long content, enlarged text, keyboard use, and TalkBack/screen-reader behavior.

See [the UI improvement plan](../docs/UI_IMPROVEMENT_PLAN.md) for the screen-by-screen sequence. This folder owns the shared brand values; the UI plan owns the redesign work order.

## Changes and ownership

Version 1.0.0 establishes the proposed palette, Inter, scale, voice, and platform contract. Increase the version when adopted values change; record the reason here and regenerate both outputs. A future logo refresh must add master artwork and exports together, with provenance recorded in `assets/README.md`.

## Validation of this kit

On 2026-10-24, the generator and drift check passed all 70 defined contrast checks and verified the six bundled font/license hashes. The lowest checked text contrast was 5.11:1. The preview was checked in light/dark mode at desktop width and at 390 CSS pixels, with no horizontal overflow observed. Inter loaded locally; sample search, empty results, and save toggles worked.

The Android app now consumes these values. See [the Android UI validation report](../docs/UI_UPGRADE.md) for build results, emulator screenshots, and remaining manual checks.
