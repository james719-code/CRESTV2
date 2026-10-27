import {copyFile, mkdir} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

const root = fileURLToPath(new URL('../', import.meta.url));
const repo = path.resolve(root, '..');
const files = {
  'branding/assets/fonts/InterVariable.woff2': 'fonts/InterVariable.woff2',
  'branding/assets/fonts/LICENSE.txt': 'fonts/LICENSE.txt',
  'frontend/public/crest-logo.svg': 'crest-logo.svg',
};
for (const [source, destination] of Object.entries(files)) {
  const target = path.join(root, 'public', destination);
  await mkdir(path.dirname(target), {recursive: true});
  await copyFile(path.join(repo, source), target);
}
console.log(`Prepared ${Object.keys(files).length} existing brand and fictional demo assets.`);
