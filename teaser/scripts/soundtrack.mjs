import {mkdir, writeFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

// An original, deterministic instrumental: 120 BPM, D major, stereo PCM.
// No samples, recordings, external tracks, or third-party music rights.
const rate = 48000;
const duration = 30;
const frames = rate * duration;
const channels = [new Float64Array(frames), new Float64Array(frames)];
let seed = 719;
const noise = () => {seed = (1664525 * seed + 1013904223) >>> 0; return seed / 2147483648 - 1;};
const hz = (midi) => 440 * 2 ** ((midi - 69) / 12);
function add(start, length, fn, gain = 1, pan = 0) {
  const offset = Math.round(start * rate);
  const n = Math.min(Math.round(length * rate), frames - offset);
  for (let i = 0; i < n; i++) {
    const t = i / rate;
    const value = fn(t, i, length) * gain;
    channels[0][offset + i] += value * Math.sqrt((1 - pan) / 2);
    channels[1][offset + i] += value * Math.sqrt((1 + pan) / 2);
  }
}
const chords = [[50, 57, 62, 66], [47, 54, 59, 62], [43, 50, 55, 59], [45, 52, 57, 61]];
for (let bar = 0; bar < 15; bar++) {
  const start = bar * 2;
  const chord = chords[bar % chords.length];
  for (const [index, midi] of chord.entries()) {
    const f = hz(midi);
    add(start, 3, (t, _i, length) => {
      const envelope = Math.min(1, t / .55) * Math.min(1, (length - t) / .9);
      return (Math.sin(t * f * Math.PI * 2) + .25 * Math.sin(t * f * 2.002 * Math.PI * 2)) * envelope;
    }, .09, (index - 1.5) * .35);
  }
  const notes = [chord[0] + 24, chord[2] + 12, chord[3] + 12, chord[1] + 24, chord[2] + 24, chord[3] + 12, chord[1] + 24, chord[2] + 12];
  for (let step = 0; step < 8; step++) {
    const f = hz(notes[step]);
    const at = start + step * .25;
    if (at >= 27) continue;
    const amplitude = at < 3 ? .06 : at < 24.75 ? .105 : .065;
    const tone = (t) => Math.sin(2 * Math.PI * f * t) * Math.exp(-t * 8) * Math.min(1, t / .006);
    add(at, .8, tone, amplitude, step % 2 ? .45 : -.45);
    add(at + .375, .8, tone, amplitude * .27, step % 2 ? -.6 : .6);
  }
}
for (let beat = 6; beat < 53; beat++) {
  const at = beat * .5;
  add(at, .35, (t) => Math.sin(2 * Math.PI * (48 * t + 50 * .025 * (1 - Math.exp(-t / .025)))) * Math.exp(-t * 16) * Math.min(1, t / .003), .35);
  if (beat % 2) add(at, .18, (t) => noise() * Math.exp(-t * 40) + .12 * Math.sin(2 * Math.PI * 190 * t) * Math.exp(-t * 35), .08, .1);
  for (const shift of [0, .25]) add(at + shift, .055, (t) => noise() * Math.exp(-t * 95), .025, shift ? .5 : -.5);
}
for (const at of [3, 6, 11, 16, 21, 25]) {
  add(at, 1.3, (t) => (Math.sin(2 * Math.PI * 48 * t) * .7 + noise() * .17) * Math.exp(-t * 6) * Math.min(1, t / .008), .24);
  if (at > 3) add(at - .35, .35, (t, _i, length) => noise() * (t / length) ** 3, .1, -.2);
}
for (const [index, midi] of [62, 66, 69, 74].entries()) add(25.05 + index * .075, 4.8, (t) => Math.sin(2 * Math.PI * hz(midi) * t) * Math.exp(-t * 1.15) * Math.min(1, t / .008), .19, (index - 1.5) * .3);

const wav = Buffer.alloc(44 + frames * 4);
wav.write('RIFF', 0); wav.writeUInt32LE(wav.length - 8, 4); wav.write('WAVEfmt ', 8);
wav.writeUInt32LE(16, 16); wav.writeUInt16LE(1, 20); wav.writeUInt16LE(2, 22);
wav.writeUInt32LE(rate, 24); wav.writeUInt32LE(rate * 4, 28); wav.writeUInt16LE(4, 32); wav.writeUInt16LE(16, 34);
wav.write('data', 36); wav.writeUInt32LE(frames * 4, 40);
for (let i = 0; i < frames; i++) {
  const t = i / rate;
  const fade = Math.min(1, t / .3, (duration - t) / 1.2);
  for (let channel = 0; channel < 2; channel++) wav.writeInt16LE(Math.round(Math.tanh(channels[channel][i] * 1.35) * .86 * fade * 32767), 44 + i * 4 + channel * 2);
}
const target = fileURLToPath(new URL('../public/audio/crest-original.wav', import.meta.url));
await mkdir(path.dirname(target), {recursive: true});
await writeFile(target, wav);
console.log('Created original 30-second stereo soundtrack, 48 kHz.');
