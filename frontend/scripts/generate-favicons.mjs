// Renders the Mycellis favicon mark (forest-green rounded square, cream Fraunces M)
// at each required output size, then rasterizes with sharp.
//
// Font notes: sharp's SVG renderer (librsvg/Pango) ignores `font-variation-settings`
// entirely, so the spec's opsz/SOFT axis values aren't controllable here — the font
// renders at its default resting instance for those axes. Standard CSS `font-weight`
// *is* respected against the embedded variable font, so per-size weight compensation
// (the documented table) works correctly and is what actually varies below.
import sharp from 'sharp';
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const FONT_PATH = fileURLToPath(new URL('./fonts/Fraunces-Variable.woff2', import.meta.url));
const OUT_DIR = fileURLToPath(new URL('../public/', import.meta.url));

const BG = '#1e3d2e';
const FG = '#efe6d0';
const RADIUS_PCT = 0.22;

const outputs = [
  { file: 'favicon-16.png', size: 16, weight: 800 },
  { file: 'favicon-32.png', size: 32, weight: 750 },
  { file: 'favicon-48.png', size: 48, weight: 720 },
  { file: 'apple-touch-icon.png', size: 180, weight: 620 },
  { file: 'icon-192.png', size: 192, weight: 600 },
  { file: 'icon-512.png', size: 512, weight: 600 },
];

const fontBase64 = readFileSync(FONT_PATH).toString('base64');

function markSvg(size, weight) {
  const radius = size * RADIUS_PCT;
  const fontSize = size * 0.64;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <defs>
    <style>
      @font-face {
        font-family: 'Fraunces Mark';
        src: url(data:font/woff2;base64,${fontBase64}) format('woff2');
        font-weight: 100 900;
      }
      text {
        font-family: 'Fraunces Mark';
        font-weight: ${weight};
      }
    </style>
  </defs>
  <rect width="${size}" height="${size}" rx="${radius}" fill="${BG}"/>
  <text x="50%" y="64.5%" text-anchor="middle" font-size="${fontSize}" fill="${FG}">M</text>
</svg>`;
}

for (const { file, size, weight } of outputs) {
  await sharp(Buffer.from(markSvg(size, weight))).png().toFile(`${OUT_DIR}${file}`);
  console.log(`wrote ${file} (${size}x${size}, wght ${weight})`);
}

// Scalable master — modern browsers apply one weight across all sizes.
writeFileSync(`${OUT_DIR}favicon.svg`, markSvg(64, 700));
console.log('wrote favicon.svg (wght 700)');
