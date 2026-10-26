# Applying the brand on web and Android

## Shared contract

Both platforms use `tokens/brand.tokens.json` as the source for color, typography, spacing, shape, and motion values. Its `version` identifies the baseline. Run the generator and commit both generated outputs whenever values change.

Share identity, language, color meanings, type hierarchy, and component intent. Let navigation, density, hover/press behavior, and layout respond to each platform. A desktop page should not be a stretched phone screen.

| Concept | Web | Android Compose |
| --- | --- | --- |
| Colors | `--crest-color-primary`, `--crest-color-on-surface`, etc. | Generated `CrestLightColorScheme` / `CrestDarkColorScheme` |
| Status/category colors | `--crest-color-success`, `--crest-color-strand-stem`, paired `on-*` roles | `CrestLightExtendedColors` / `CrestDarkExtendedColors` |
| Typography | `--crest-type-title-medium-size`, `-line-height`, `-weight`, `-tracking` | `CrestTypography` |
| Font | Bundled `InterVariable.woff2` | Bundled regular/medium/semibold/bold TTF resources |
| Spacing | `--crest-space-md` in rem | `CrestSpacing.md` in dp |
| Shape | `--crest-radius-control`, `-card`, `-sheet` | `CrestRadii` / `CrestShapes` |
| Interaction size | `--crest-size-touch-minimum` | `CrestSizes.touchMinimum` |
| Motion | `--crest-motion-fast`, `-standard` | `CrestMotion.fast`, `standard` in milliseconds |

## Web installation

The repository's `frontend/` is a React teacher workspace built with Vite, TypeScript, Tailwind CSS, and shadcn/ui. Its CSS imports the generated web tokens directly, including the bundled Inter font. The shadcn semantic variables map to CREST roles in light and dark themes. Display preferences select a theme explicitly or follow the device, and provide optional larger text, stronger contrast, reduced motion, and guidance. The Inter license is distributed in `frontend/public/licenses/`.

1. Copy or import the generated CSS into the web project and copy `assets/fonts/InterVariable.woff2` and its license alongside the app's local font assets.
2. Preserve the CSS's relative font path or update it to the final asset URL. The path in this kit is designed for `branding/generated/web/crest-tokens.css`.
3. Apply semantic variables in component styles. Set body text to Inter, `onSurface`, and `bodyLarge`; do not leave the font applied only to headings.
4. Set `data-theme="light"` or `data-theme="dark"` on `<html>` for a user choice. Without an explicit choice, the CSS follows `prefers-color-scheme`.
5. Keep a 16px root text baseline for the specified rem scale, while honoring browser zoom and user text-size preferences. Never lock zoom or shrink the root to force layouts to fit.

```css
body {
  font-family: var(--crest-font-family);
  font-size: var(--crest-type-body-large-size);
  line-height: var(--crest-type-body-large-line-height);
  color: var(--crest-color-on-surface);
  background: var(--crest-color-background);
}

.research-title {
  font-size: var(--crest-type-title-medium-size);
  line-height: var(--crest-type-title-medium-line-height);
  font-weight: var(--crest-type-title-medium-weight);
}
```

Use semantic HTML, actual labels, visible keyboard focus, and buttons/links with the correct behavior. The generated CSS supplies values, not a component library. Define hover, focus, disabled, loading, and error states in the components. Use the `outline` color for meaningful input/control boundaries; `outlineVariant` is for decorative separators and should not be relied on to identify a control.

## Android installation

The Android application now uses this baseline. The generator writes both the shared Kotlin reference and its installed copy under `android/app/src/main/java/com/bdbshs/crest/branding/`. The steps below describe the integration and how to maintain it.

1. Copy `Inter-Regular.ttf`, `Inter-Medium.ttf`, `Inter-SemiBold.ttf`, and `Inter-Bold.ttf` to `android/app/src/main/res/font/` as `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, and `inter_bold.ttf`. Static fonts avoid relying on variable-font support on API 24/25.
2. Copy `generated/android/CrestBrandTokens.kt` into `android/app/src/main/java/com/bdbshs/crest/branding/`. Keep the Inter license in the distributed app's notices/assets.
3. Change `ui/theme/Theme.kt` to pass the generated light/dark scheme, `CrestTypography`, and `CrestShapes` into `MaterialTheme`.
4. Provide the matching extended color set through a theme `CompositionLocal`. Status and strand badges should consume that theme value; never choose a light-only color directly.
5. Migrate existing spacing/dimension providers to the generated values or map them explicitly. Remove old aliases only after auditing active call sites.
6. Update `AppLogo.kt` and the About/sign-in copy to the agreed descriptor and type treatment. Keep existing launcher artwork until replacement masters are prepared.
7. Build and check real screenshots before removing the old theme definitions.

```kotlin
MaterialTheme(
    colorScheme = if (darkTheme) CrestDarkColorScheme else CrestLightColorScheme,
    typography = CrestTypography,
    shapes = CrestShapes,
    content = content
)
```

The app bundles all four `R.font.inter_*` resources and the Inter license at `assets/licenses/Inter-OFL.txt`. Its theme consumes the generated schemes and typography and provides extended colors through `MaterialTheme.crestColors`. The generator's `--check` validates the installed Kotlin copy too. Dynamic wallpaper colors are disabled to preserve the branded palette.

## Component agreement

| Component | Shared rule | Device adaptation |
| --- | --- | --- |
| Primary button | Blue with paired `onPrimary`, Inter semibold, one dominant action per section, 12-unit radius | At least 48-unit target; Android uses native press/ripple, web adds keyboard focus/hover |
| Secondary action | Text or outline treatment with primary text color; avoid competing filled actions | Touch and keyboard accessible |
| Research item | Title first, authors second, labeled strand/type metadata; separate favorite action | Full-width phone row; regular grid or wider list on web/tablet when titles remain readable |
| Search/filter | Search field plus a compact filter/sort entry; show current filters and clear action | Sheet on phones; popover/sidebar where suitable on desktop |
| Navigation | Stable labeled destinations, consistent names and selected state | Bottom bar on phones; rail or top/side navigation in wider windows |
| Status badge | Text plus theme-specific paired colors; no color-only meaning | Can wrap or expand with text scaling |
| Form field | Persistent label, short help text, specific inline error; meaningful outline | Keyboard insets on Android; keyboard navigation and visible focus on web |
| Feedback state | Specific title, short explanation, working recovery action when available | Preserve readable width and logical screen-reader order |
| Dialog/sheet | Clear title, specific action verbs, consequence for destructive actions | Native focus/back behavior; no background interaction while modal |

## Review checklist

- The same brand version, Inter weights, semantic colors, and vocabulary are used on both platforms.
- Logo and school attribution follow the guide; final artwork provenance is recorded.
- No essential body copy uses a display style or sub-12 text.
- Theme colors are used without arbitrary opacity changes that weaken contrast.
- Normal text/background pairs meet 4.5:1; meaningful boundaries/focus indicators meet 3:1 against adjacent surfaces. Check actual rendered states, including hover and disabled styling.
- Targets are at least 48dp on Android and 48 CSS px in the web design baseline; text scaling can expand them.
- Both themes, 200% text/zoom, long titles, and translated labels fit without overlapping controls.
- Screen readers, keyboard users, and reduced-motion preferences are supported.
- Shared identity does not conceal differing role permissions or missing features.

## References

- [Material 3 theming in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Android accessibility defaults and touch targets](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- [CSS custom properties](https://developer.mozilla.org/en-US/docs/Web/CSS/Guides/Cascading_variables/Using_custom_properties)
- [Inter source and releases](https://github.com/rsms/inter)
