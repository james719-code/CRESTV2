import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

// Original 120 BPM stereo score with a humanized drum groove.
// Scene cuts land on beats 6, 14, 22, 30 and 38.
const sampleRate = 48000;
const duration = 24;
const samples = sampleRate * duration;
const mix = [new Float32Array(samples), new Float32Array(samples)];
const tau = 2 * Math.PI;
const hz = (note) => 440 * 2 ** ((note - 69) / 12);
let seed = 227;
const noise = () => {
  seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
  return seed / 2147483648 - 1;
};

function voice(start, length, level, pan, wave) {
  const offset = Math.round(start * sampleRate);
  const limit = Math.min(Math.round(length * sampleRate), samples - offset);
  const left = Math.sqrt((1 - pan) / 2) * level;
  const right = Math.sqrt((1 + pan) / 2) * level;
  for (let i = 0; i < limit; i++) {
    const sample = wave(i / sampleRate);
    mix[0][offset + i] += sample * left;
    mix[1][offset + i] += sample * right;
  }
}

const attack = (t) => Math.min(1, t / .007);
const kick = (t) => {
  const body = Math.sin(tau * (48 * t + 1.75 * (1 - Math.exp(-t * 33))));
  const beater = .31 * Math.sin(tau * 1380 * t) * Math.exp(-t * 170);
  return (body + beater) * Math.min(1, t / .0025) * Math.exp(-t * 13);
};
const pluck = (note, decay) => {
  const f = hz(note);
  return (t) => (Math.sin(tau * f * t) + .28 * Math.sin(tau * 2 * f * t) + .12 * Math.sin(tau * 3.006 * f * t)) * attack(t) * Math.exp(-t * decay);
};

function snare(at, level) {
  let low = 0;
  voice(at, .32, level, -.08, (t) => {
    const raw = noise();
    low = .86 * low + .14 * raw;
    const wires = (raw - low) * .82 * Math.exp(-t * 18);
    const shell = (.65 * Math.sin(tau * 192 * t) + .28 * Math.sin(tau * 348 * t)) * Math.exp(-t * 44);
    return (wires + shell) * Math.min(1, t / .002);
  });
  for (const [delay, pan] of [[.012, -.48], [.026, .44]]) {
    voice(at + delay, .18, level * .22, pan, (t) => noise() * Math.min(1, t / .002) * Math.exp(-t * 35));
  }
}

function hat(at, level, pan, open = false) {
  let previous = 0;
  voice(at, open ? .32 : .105, level, pan, (t) => {
    const raw = noise();
    const sizzle = (raw - previous) * .55;
    previous = raw;
    const metal = .16 * (Math.sin(tau * 4687 * t) + Math.sin(tau * 6719 * t));
    return (sizzle + metal) * Math.min(1, t / .003) * Math.exp(-t * (open ? 13 : 56));
  });
}

for (let beat = 0; beat < 46; beat++) {
  const at = beat / 2;
  voice(at, .43, beat % 2 ? .43 : .58, 0, kick);
  if (beat % 4 === 2) voice(at + .365, .36, .40, 0, kick);
  if (beat % 2) snare(at + .007, beat % 4 === 1 ? .43 : .48);
  hat(at + .015, .10, -.4);
  hat(at + .259, beat % 2 ? .14 : .19, .44, beat % 2 === 0);
  if (beat % 4 === 3) hat(at + .388, .095, -.36);
}

const chords = [[45, 57, 61, 64], [40, 56, 59, 64], [42, 57, 61, 66], [38, 54, 57, 62]];
for (let bar = 0; bar < 12; bar++) {
  const start = bar * 2;
  const [bass, low, middle, top] = bar >= 10 ? chords[0] : chords[bar % chords.length];
  for (const step of [0, .375, .75, 1, 1.375, 1.75]) {
    voice(start + step, .34, .26, 0, pluck(bass, 8));
  }
  for (const [index, note] of [middle + 12, top + 12, low + 12, middle + 12, top + 12, middle + 12].entries()) {
    const at = start + [0, .25, .625, 1, 1.375, 1.625][index];
    const pan = (index % 2 ? 1 : -1) * .45;
    voice(at, .58, .135, pan, pluck(note, 11));
    voice(at + .19, .55, .055, -pan, pluck(note, 9));
  }
}

for (const [index, cut] of [3, 7, 11, 15, 19].entries()) {
  snare(cut - .22, .19);
  snare(cut - .105, .27);
  hat(cut + .012, .27, index % 2 ? -.54 : .54, true);
  let filtered = 0;
  voice(cut - .23, .33, .22, index % 2 ? .32 : -.32, (t) => {
    filtered = .75 * filtered + .25 * noise();
    return filtered * Math.sin(Math.PI * t / .33) ** 2;
  });
  voice(cut, .52, .14, 0, pluck(45, 7));
}

for (const [index, note] of [69, 73, 76, 81].entries()) {
  voice(19 + index * .068, 4.2, .15, (index - 1.5) * .31, pluck(note, 1.3));
}

const wav = Buffer.alloc(44 + samples * 4);
wav.write('RIFF', 0);
wav.writeUInt32LE(wav.length - 8, 4);
wav.write('WAVEfmt ', 8);
wav.writeUInt32LE(16, 16);
wav.writeUInt16LE(1, 20);
wav.writeUInt16LE(2, 22);
wav.writeUInt32LE(sampleRate, 24);
wav.writeUInt32LE(sampleRate * 4, 28);
wav.writeUInt16LE(4, 32);
wav.writeUInt16LE(16, 34);
wav.write('data', 36);
wav.writeUInt32LE(samples * 4, 40);
for (let i = 0; i < samples; i++) {
  const endFade = Math.min(1, (samples - i) / (sampleRate * 1.45));
  for (let ch = 0; ch < 2; ch++) {
    const sample = Math.tanh(mix[ch][i] * 1.25) * endFade;
    wav.writeInt16LE(Math.round(sample * 32767), 44 + i * 4 + ch * 2);
  }
}

const target = fileURLToPath(new URL('../public/audio/crest-announcement.mp3', import.meta.url));
execFileSync('ffmpeg', ['-hide_banner', '-loglevel', 'error', '-y', '-i', 'pipe:0', '-af', 'loudnorm=I=-15:TP=-1.5:LRA=7', '-ar', '48000', '-c:a', 'libmp3lame', '-b:a', '192k', target], {input: wav, windowsHide: true});
console.log('Created the 24-second CREST launch score.');
