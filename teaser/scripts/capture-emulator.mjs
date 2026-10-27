import {execFileSync} from 'node:child_process';
import {copyFileSync, mkdirSync, writeFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

const root = fileURLToPath(new URL('../', import.meta.url));
const output = path.resolve(root, '../artifacts/teaser-emulator');
const adb = process.env.ADB_PATH || path.join(process.env.LOCALAPPDATA || '', 'Android/Sdk/platform-tools/adb.exe');
const run = (...args) => execFileSync(adb, args, {windowsHide: true, encoding: 'utf8', timeout: 20000}).trim();
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
mkdirSync(output, {recursive: true});
mkdirSync(path.join(root, 'public/screens'), {recursive: true});
run('wait-for-device');
for (let i = 0; i < 120 && run('shell', 'getprop', 'sys.boot_completed') !== '1'; i++) await sleep(500);
const resolution = run('shell', 'wm', 'size');
console.log(resolution);
// Production Compose components, rendered by Android with offline fictional
// records through the existing debug-only activity. No HTML screen mockups.
const screens = [
  ['student', 'student-home', false], ['teacher', 'teacher-home', false],
  ['research', 'research', false], ['paper', 'paper-details', false],
  ['groups', 'groups', false], ['documents', 'documents', false],
  ['milestones', 'milestones', false], ['inbox', 'inbox', false],
  ['research', 'research-dark', true], ['login', 'sign-in', false],
];
const captures = [];
for (const [page, name, dark] of screens) {
  run('shell', 'am', 'force-stop', 'com.bdbshs.crest');
  run('shell', 'am', 'start', '-W', '-n', 'com.bdbshs.crest/.debug.UiReviewActivity', '--es', 'page', page, '--ez', 'audit', 'true', '--ez', 'dark', String(dark));
  await sleep(1400);
  const image = execFileSync(adb, ['exec-out', 'screencap', '-p'], {windowsHide: true, timeout: 20000, maxBuffer: 20 * 1024 * 1024});
  if (image.readUInt32BE(0) !== 0x89504e47) throw new Error(`Invalid Android capture: ${name}`);
  writeFileSync(path.join(output, `${name}.png`), image);
  copyFileSync(path.join(output, `${name}.png`), path.join(root, 'public/screens', `${name}.png`));
  captures.push({name, page, dark, capturedAt: new Date().toISOString(), width: image.readUInt32BE(16), height: image.readUInt32BE(20), sha256: createHash('sha256').update(image).digest('hex')});
  console.log(`Captured ${name}: ${captures.at(-1).width} × ${captures.at(-1).height}`);
}
writeFileSync(path.join(output, 'manifest.json'), JSON.stringify({source: 'Android emulator via adb exec-out screencap; real production Compose components, debug offline fixtures', resolution, captures}, null, 2));
console.log(`Saved ${captures.length} real emulator screenshots and provenance manifest.`);
