import {bundle} from '@remotion/bundler';
import {renderMedia, renderStill, selectComposition} from '@remotion/renderer';
import {mkdir, readFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

const root = fileURLToPath(new URL('../', import.meta.url));
const output = path.join(root, 'out');
const mode = process.argv[2] || 'landscape';
const announcement = mode.startsWith('announcement');
const stills = mode.endsWith('stills');
const title = announcement ? 'CREST-V2-Announcement' : 'CREST-V2-Cinematic';
const posterFrame = announcement ? 660 : 420;
await mkdir(output, {recursive: true});
if (!announcement) {
  const captureManifest = JSON.parse(await readFile(path.resolve(root, '../artifacts/teaser-emulator/manifest.json'), 'utf8'));
  if (!captureManifest.captures?.length || !captureManifest.source.includes('Android emulator')) throw new Error('Capture the real Android emulator with npm run capture before rendering.');
  const raw = await readFile(path.resolve(root, '../artifacts/teaser-emulator/research-dark.png'));
  const asset = await readFile(path.join(root, 'public/screens/research-dark.png'));
  if (!raw.equals(asset)) throw new Error('The research-dark asset differs from its actual emulator capture.');
}
const browserExecutable = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const serveUrl = await bundle({entryPoint: path.join(root, 'src/index.ts'), publicDir: path.join(root, 'public'), outDir: path.join(root, '.cache/bundle'), onProgress: (value) => {if (value === 100) console.log('Composition bundled.');}});
const compositions = mode === 'all' || mode === 'announcement' || stills ? [title, `${title}-Vertical`] : [mode.endsWith('vertical') ? `${title}-Vertical` : title];
for (const id of compositions) {
  const composition = await selectComposition({serveUrl, id, browserExecutable});
  console.log(`${id}: ${composition.width}×${composition.height}, ${composition.durationInFrames / composition.fps}s at ${composition.fps}fps`);
  if (stills) {
    const frames = announcement
      ? [['opening', 55], ['research', 158], ['groups', 278], ['milestones', 398], ['review', 518], ['poster', posterFrame]]
      : [['opening', 65], ['device-edge', 125], ['device-reveal', 165], ['device-turn', 190], ['closer', 245], ['poster', posterFrame]];
    for (const [name, frame] of frames) {
      await renderStill({serveUrl, composition, browserExecutable, frame, imageFormat: 'png', output: path.join(output, `${id}-${name}.png`)});
      console.log(`Preview: ${id}-${name}`);
    }
  } else {
    let last = -1;
    await renderMedia({serveUrl, composition, browserExecutable, outputLocation: path.join(output, `${id}.mp4`), codec: 'h264', crf: 18, pixelFormat: 'yuv420p', audioCodec: 'aac', audioBitrate: '192k', imageFormat: 'jpeg', jpegQuality: 95, concurrency: 3, chromiumOptions: {gl: 'angle'}, onProgress: ({progress}) => {const p = Math.floor(progress * 10) * 10; if (p !== last) {last = p; console.log(`${id} ${p}%`);}}});
    await renderStill({serveUrl, composition, browserExecutable, frame: posterFrame, imageFormat: 'png', output: path.join(output, `${id}-poster.png`)});
    console.log(`Saved ${id}.mp4 and poster.`);
  }
}
