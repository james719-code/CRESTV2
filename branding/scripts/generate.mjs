import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { createHash } from 'node:crypto';

// Run from any directory. All outputs stay inside this branding folder.
const root = fileURLToPath(new URL('../', import.meta.url));
const checking = process.argv.includes('--check');
const tokens = JSON.parse(await readFile(path.join(root, 'tokens/brand.tokens.json'), 'utf8'));
const kebab = value => value.replace(/[A-Z]/g, letter => `-${letter.toLowerCase()}`);
const upper = value => value[0].toUpperCase() + value.slice(1);
const rem = value => `${value / 16}rem`;
const color = value => `Color(0xFF${value.slice(1)})`;
const fail = message => { throw new Error(message); };
const sameKeys = (a, b) => JSON.stringify(Object.keys(a).sort()) === JSON.stringify(Object.keys(b).sort());

if (!/^\d+\.\d+\.\d+$/.test(tokens.version)) fail('Use a semantic version such as 1.0.0.');
if (!sameKeys(tokens.themes.light.material, tokens.themes.dark.material) ||
    !sameKeys(tokens.themes.light.extended, tokens.themes.dark.extended)) fail('Theme roles must match.');

function luminance(hex) {
  const channels = hex.slice(1).match(/../g).map(channel => {
    const value = parseInt(channel, 16) / 255;
    return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
  });
  return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
}
function contrast(a, b) {
  const values = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (values[0] + 0.05) / (values[1] + 0.05);
}

let pairCount = 0;
let minimum = { ratio: Infinity, description: '' };
function verifyPair(colors, foreground, background, theme, threshold = 4.5) {
  const ratio = contrast(colors[foreground], colors[background]);
  const description = `${theme}: ${foreground} on ${background}`;
  if (ratio < threshold) fail(`${description} is ${ratio.toFixed(2)}:1; expected ${threshold}:1.`);
  if (threshold === 4.5 && ratio < minimum.ratio) minimum = { ratio, description };
  pairCount++;
}

for (const [theme, groups] of Object.entries(tokens.themes)) {
  const colors = { ...groups.material, ...groups.extended };
  for (const [key, value] of Object.entries(colors)) {
    if (!/^[a-z][a-zA-Z0-9]*$/.test(key) || !/^#[0-9A-F]{6}$/.test(value)) fail(`Invalid color ${theme}.${key}.`);
  }
  for (const background of ['primary', 'primaryContainer', 'secondary', 'secondaryContainer',
    'tertiary', 'tertiaryContainer', 'background', 'surface', 'surfaceVariant', 'error',
    'errorContainer', 'success', 'warning', 'info', 'strandStem', 'strandTvl', 'strandHumss', 'strandAbm', 'strandGas']) {
    verifyPair(colors, `on${upper(background)}`, background, theme);
  }
  verifyPair(colors, 'inverseOnSurface', 'inverseSurface', theme);
  verifyPair(colors, 'inversePrimary', 'inverseSurface', theme);
  for (const surface of ['surfaceContainerLowest', 'surfaceContainerLow', 'surfaceContainer',
    'surfaceContainerHigh', 'surfaceContainerHighest']) {
    verifyPair(colors, 'onSurface', surface, theme);
    verifyPair(colors, 'onSurfaceVariant', surface, theme);
  }
  for (const surface of ['surface', 'background']) {
    verifyPair(colors, 'primary', surface, theme);
    verifyPair(colors, 'outline', surface, theme, 3);
  }
}

for (const [role, style] of Object.entries(tokens.typography)) {
  if (![400, 500, 600, 700].includes(style.weight) || style.lineHeight < style.size ||
      !Object.values(style).every(Number.isFinite)) fail(`Invalid typography: ${role}.`);
}
for (const group of ['spacing', 'radius', 'sizes', 'motion']) {
  for (const value of Object.values(tokens[group])) {
    if (!Number.isFinite(value) || value <= 0) fail(`Invalid ${group} dimension.`);
  }
}
if (tokens.sizes.touchMinimum < 48) fail('Minimum touch target must be at least 48.');

const fontManifest = JSON.parse(await readFile(path.join(root, 'assets/fonts/source.json'), 'utf8'));
const requiredFonts = ['InterVariable.woff2', 'Inter-Regular.ttf', 'Inter-Medium.ttf',
  'Inter-SemiBold.ttf', 'Inter-Bold.ttf', 'LICENSE.txt'];
for (const name of requiredFonts) {
  const record = fontManifest.files.find(file => file.name === name);
  if (!record) fail(`Missing font manifest entry: ${name}.`);
  const bytes = await readFile(path.join(root, 'assets/fonts', name));
  const actual = createHash('sha256').update(bytes).digest('hex');
  if (actual !== record.sha256) fail(`Font hash mismatch: ${name}.`);
  if (name.endsWith('.woff2') && bytes.subarray(0, 4).toString() !== 'wOF2') fail('Invalid WOFF2 signature.');
  if (name.endsWith('.ttf') && bytes.readUInt32BE(0) !== 0x00010000) fail(`Invalid TTF signature: ${name}.`);
}

const declaration = (key, value) => `  --crest-${key}: ${value};`;
const common = [
  declaration('font-family', `'${tokens.brand.fontFamily}', system-ui, sans-serif`),
  declaration('wordmark-weight', tokens.brand.wordmarkWeight),
  declaration('wordmark-tracking', `${tokens.brand.wordmarkTrackingEm}em`),
  ...Object.entries(tokens.brand.palette).map(([key, value]) => declaration(`brand-${key}`, value)),
  ...Object.entries(tokens.typography).flatMap(([role, style]) => [
    declaration(`type-${kebab(role)}-size`, rem(style.size)),
    declaration(`type-${kebab(role)}-line-height`, rem(style.lineHeight)),
    declaration(`type-${kebab(role)}-weight`, style.weight),
    declaration(`type-${kebab(role)}-tracking`, rem(style.tracking))
  ]),
  ...[['spacing', 'space'], ['radius', 'radius'], ['sizes', 'size']].flatMap(([group, prefix]) =>
    Object.entries(tokens[group]).map(([key, value]) => declaration(`${prefix}-${kebab(key)}`, rem(value)))),
  ...Object.entries(tokens.motion).map(([key, value]) => declaration(`motion-${key}`, `${value}ms`))
].join('\n');
function themeCss(name) {
  const theme = tokens.themes[name];
  return `  color-scheme: ${name};\n` + Object.entries({ ...theme.material, ...theme.extended })
    .map(([key, value]) => declaration(`color-${kebab(key)}`, value)).join('\n');
}
const css = `/* Generated from brand.tokens.json v${tokens.version}. Do not edit directly. */
@font-face {
  font-family: '${tokens.brand.fontFamily}';
  src: url('../../assets/fonts/InterVariable.woff2') format('woff2');
  font-style: normal;
  font-weight: 100 900;
  font-display: swap;
}
:root {
${common}
}
:root, :root[data-theme="light"] {
${themeCss('light')}
}
@media (prefers-color-scheme: dark) {
  :root:not([data-theme]) {
${themeCss('dark')}
  }
}
:root[data-theme="dark"] {
${themeCss('dark')}
}
@media (prefers-reduced-motion: reduce) {
  :root {
    --crest-motion-fast: 0ms;
    --crest-motion-standard: 0ms;
    --crest-motion-slow: 0ms;
  }
}
`;

const schemes = Object.entries(tokens.themes).map(([name, theme]) =>
  `val Crest${upper(name)}ColorScheme = ${name}ColorScheme(\n` +
  Object.entries(theme.material).map(([key, value]) => `    ${key} = ${color(value)},`).join('\n') + '\n)'
).join('\n\n');
const extendedFields = Object.keys(tokens.themes.light.extended)
  .map(key => `    val ${key}: Color,`).join('\n');
const extended = Object.entries(tokens.themes).map(([name, theme]) =>
  `val Crest${upper(name)}ExtendedColors = CrestExtendedColors(\n` +
  Object.entries(theme.extended).map(([key, value]) => `    ${key} = ${color(value)},`).join('\n') + '\n)'
).join('\n\n');
const type = Object.entries(tokens.typography).map(([role, style]) =>
  `    ${role} = TextStyle(fontFamily = CrestInter, fontWeight = FontWeight(${style.weight}), fontSize = ${style.size}.sp, lineHeight = ${style.lineHeight}.sp, letterSpacing = ${style.tracking}.sp),`
).join('\n');
const dimensions = [['spacing', 'CrestSpacing'], ['radius', 'CrestRadii'], ['sizes', 'CrestSizes']]
  .map(([group, name]) => `object ${name} {\n` + Object.entries(tokens[group])
    .map(([key, value]) => `    val ${key} = ${value}.dp`).join('\n') + '\n}').join('\n\n');
const kotlin = `// Generated from brand.tokens.json v${tokens.version}. Do not edit directly.
// Uses the bundled inter_* font resources in the Android app.
package com.bdbshs.crest.branding

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bdbshs.crest.R

const val CrestBrandVersion = "${tokens.version}"

val CrestInter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

${schemes}

@Immutable
data class CrestExtendedColors(
${extendedFields}
)

${extended}

val CrestTypography = Typography(
${type}
)

${dimensions}

val CrestShapes = Shapes(
    extraSmall = RoundedCornerShape(CrestRadii.small),
    small = RoundedCornerShape(CrestRadii.control),
    medium = RoundedCornerShape(CrestRadii.card),
    large = RoundedCornerShape(CrestRadii.sheet),
    extraLarge = RoundedCornerShape(CrestRadii.sheet),
)

object CrestMotion {
${Object.entries(tokens.motion).map(([key, value]) => `    const val ${key} = ${value}`).join('\n')}
}
`;

let drift = false;
for (const [name, output] of [
  ['generated/web/crest-tokens.css', css],
  ['generated/android/CrestBrandTokens.kt', kotlin],
  ['../android/app/src/main/java/com/bdbshs/crest/branding/CrestBrandTokens.kt', kotlin],
]) {
  const location = path.join(root, name);
  if (checking) {
    let current;
    try { current = await readFile(location, 'utf8'); } catch { current = ''; }
    if (current.replace(/\r\n/g, '\n') !== output) {
      console.error(`Out of date: ${name}`);
      drift = true;
    }
  } else {
    await mkdir(path.dirname(location), { recursive: true });
    await writeFile(location, output, 'utf8');
    console.log(`Generated ${name}`);
  }
}
if (drift) process.exitCode = 1;
console.log(`${pairCount} contrast pairs passed; lowest text contrast ${minimum.ratio.toFixed(2)}:1 (${minimum.description}).`);
console.log('Six font/license hashes verified.');
if (checking && !drift) console.log(`Brand v${tokens.version}: generated files are in sync.`);
