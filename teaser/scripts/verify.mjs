import {execFileSync} from 'node:child_process';
import {readFile, writeFile} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

const root = fileURLToPath(new URL('../', import.meta.url));
const report = {verifiedAt: new Date().toISOString(), videos: [], captures: []};
for (const [name, width, height] of [['CREST-V2-Cinematic', 1920, 1080], ['CREST-V2-Cinematic-Vertical', 1080, 1920]]) {
  const videoPath = path.join(root, 'out', `${name}.mp4`);
  const info = JSON.parse(execFileSync('ffprobe', ['-v', 'error', '-show_streams', '-show_format', '-of', 'json', videoPath], {encoding: 'utf8', windowsHide: true}));
  const video = info.streams.find((stream) => stream.codec_type === 'video');
  const audio = info.streams.find((stream) => stream.codec_type === 'audio');
  if (video.width !== width || video.height !== height || video.nb_frames !== '480' || video.r_frame_rate !== '30/1' || video.codec_name !== 'h264') throw new Error(`Unexpected video format: ${name}`);
  if (!audio || audio.codec_name !== 'aac' || audio.channels !== 2) throw new Error(`Missing stereo audio: ${name}`);
  if (Math.abs(Number(info.format.duration) - 16) > .1) throw new Error(`Unexpected duration: ${name}`);
  execFileSync('ffmpeg', ['-v', 'error', '-i', videoPath, '-f', 'null', '-'], {windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'], timeout: 120000});
  report.videos.push({name: `${name}.mp4`, width, height, frames: Number(video.nb_frames), fps: 30, seconds: Number(info.format.duration), video: video.codec_name, audio: audio.codec_name, channels: audio.channels, bytes: Number(info.format.size), decodedWithoutErrors: true, sha256: createHash('sha256').update(await readFile(videoPath)).digest('hex')});
  console.log(`Verified ${name}: ${width}×${height}, 480 frames, 16 seconds, H.264 + stereo AAC; full decode passed.`);
}
for (const name of ['research-dark']) {
  const original = await readFile(path.resolve(root, `../artifacts/teaser-emulator/${name}.png`));
  const asset = await readFile(path.join(root, `public/screens/${name}.png`));
  if (!original.equals(asset)) throw new Error(`Emulator capture mismatch: ${name}`);
  report.captures.push({name, matchesRawEmulatorCapture: true, sha256: createHash('sha256').update(asset).digest('hex')});
}
await writeFile(path.join(root, 'out/verification.json'), JSON.stringify(report, null, 2));
console.log('Every screen in the teaser matches its raw Android emulator capture. Report: out/verification.json');
