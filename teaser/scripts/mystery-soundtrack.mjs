import {mkdir, writeFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

// Original suspense sound design: low pulse, aperture sweeps, silence, reveal.
// Every sound is synthesized here; no external recordings or samples.
const rate = 48000;
const duration = 16;
const count = rate * duration;
const channels = [new Float64Array(count), new Float64Array(count)];
let seed = 719;
const random = () => {seed = (1664525 * seed + 1013904223) >>> 0; return seed / 2147483648 - 1;};
function add(start, length, gain, pan, synth) {
  const offset = Math.round(start * rate);
  for (let i = 0; i < Math.min(length * rate, count - offset); i++) {
    const t = i / rate;
    const value = synth(t, length) * gain;
    channels[0][offset + i] += value * Math.sqrt((1 - pan) / 2);
    channels[1][offset + i] += value * Math.sqrt((1 + pan) / 2);
  }
}
const tau = 2 * Math.PI;
add(0, 10.5, .12, 0, (t, len) => {
  const envelope = Math.min(1, t / 1.5) * Math.min(1, (len - t) / .45);
  return (Math.sin(tau * 55 * t) + .38 * Math.sin(tau * 82.4 * t)) * envelope * (.72 + .28 * Math.sin(tau * .4 * t));
});
add(.3, 10.2, .035, -.35, (t, len) => (Math.sin(tau * 220.1 * t) + Math.sin(tau * 221.2 * t)) * Math.min(1, t / 2) * Math.min(1, (len - t) / .6));
for (const at of [.6, 2.2, 3.85, 5, 6.18, 7.35, 8.3, 9.2]) {
  add(at, .7, .22, 0, (t) => Math.sin(tau * (43 * t + 1.2 * (1 - Math.exp(-t * 24)))) * Math.exp(-t * 8) * Math.min(1, t / .012));
}
for (const [at, pan] of [[3.65, -.6], [4.8, .6], [5.98, -.3]]) {
  let filtered = 0;
  add(at, .8, .13, pan, (t, len) => {
    filtered = .83 * filtered + .17 * random();
    return filtered * Math.sin(Math.PI * t / len) ** 2 * (1.5 + .6 * Math.sin(tau * 8 * t));
  });
}
let rising = 0;
add(7.15, 3.35, .12, .2, (t, len) => {
  rising = rising * .9 + random() * .1;
  const amount = (t / len) ** 1.6;
  return (rising + .12 * Math.sin(tau * (160 * t + 70 * t * t))) * amount * Math.min(1, (len - t) / .12);
});
// The half-second silence is intentional and matches the black visual beat.
add(11.1, 1.8, .47, 0, (t) => Math.sin(tau * (38 * t + 2 * (1 - Math.exp(-t * 22)))) * Math.exp(-t * 3.6) * Math.min(1, t / .008));
let impactNoise = 0;
add(11.1, 1.2, .12, .1, (t) => {impactNoise = .76 * impactNoise + .24 * random(); return impactNoise * Math.exp(-t * 7);});
for (const [i, frequency] of [146.83, 220, 293.66, 440, 587.33].entries()) {
  add(11.1 + i * .018, 4.8 - i * .018, i < 3 ? .12 : .055, (i - 2) * .23, (t, len) => (Math.sin(tau * frequency * t) + .1 * Math.sin(tau * frequency * 2.001 * t)) * Math.min(1, t / .045) * Math.exp(-t * .65) * Math.min(1, (len - t) / .7));
}
add(12.35, 2.8, .045, .5, (t) => Math.sin(tau * 880 * t) * Math.exp(-t * 2) * Math.min(1, t / .005));
const wav = Buffer.alloc(44 + count * 4);
wav.write('RIFF', 0); wav.writeUInt32LE(wav.length - 8, 4); wav.write('WAVEfmt ', 8);
wav.writeUInt32LE(16, 16); wav.writeUInt16LE(1, 20); wav.writeUInt16LE(2, 22);
wav.writeUInt32LE(rate, 24); wav.writeUInt32LE(rate * 4, 28); wav.writeUInt16LE(4, 32); wav.writeUInt16LE(16, 34);
wav.write('data', 36); wav.writeUInt32LE(count * 4, 40);
for (let i = 0; i < count; i++) {
  const t = i / rate;
  const envelope = Math.min(1, t / .12, (duration - t) / .5);
  for (let ch = 0; ch < 2; ch++) wav.writeInt16LE(Math.round(Math.tanh(channels[ch][i] * 1.9) * .9 * envelope * 32767), 44 + i * 4 + ch * 2);
}
const target = fileURLToPath(new URL('../public/audio/crest-mystery.wav', import.meta.url));
await mkdir(path.dirname(target), {recursive: true});
await writeFile(target, wav);
console.log('Created 16-second original suspense score.');
