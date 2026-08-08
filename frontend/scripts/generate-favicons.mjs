// Renders the Mycellis favicon mark (cream rounded square, 5-petal flower —
// fatter ellipse petals rather than the loader's thin bezier curve, since
// that detail disappears below ~32px) at each required output size, then
// rasterizes with sharp.
import sharp from 'sharp';
import { writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const OUT_DIR = fileURLToPath(new URL('../public/', import.meta.url));

const BG = '#efe6d0';
const PETAL = '#eccb52';
const CENTER = '#1e3d2e';
const RADIUS_PCT = 0.22;
const PETAL_ANGLES = [0, 72, 144, 216, 288];

const outputs = [
  { file: 'favicon-16.png', size: 16 },
  { file: 'favicon-32.png', size: 32 },
  { file: 'favicon-48.png', size: 48 },
  { file: 'apple-touch-icon.png', size: 180 },
  { file: 'icon-192.png', size: 192 },
  { file: 'icon-512.png', size: 512 },
];

function markSvg(size) {
  const radius = size * RADIUS_PCT;
  const center = size / 2;
  const petalCy = size * 0.266;
  const petalRx = size * 0.1016;
  const petalRy = size * 0.1875;
  const centerR = size * 0.1172;
  const petals = PETAL_ANGLES.map(
    (angle) =>
      `<ellipse cx="${center}" cy="${petalCy}" rx="${petalRx}" ry="${petalRy}" transform="rotate(${angle} ${center} ${center})"/>`
  ).join('\n    ');

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <rect width="${size}" height="${size}" rx="${radius}" fill="${BG}"/>
  <g fill="${PETAL}">
    ${petals}
  </g>
  <circle cx="${center}" cy="${center}" r="${centerR}" fill="${CENTER}"/>
</svg>`;
}

for (const { file, size } of outputs) {
  await sharp(Buffer.from(markSvg(size))).png().toFile(`${OUT_DIR}${file}`);
  console.log(`wrote ${file} (${size}x${size})`);
}

// Scalable master.
writeFileSync(`${OUT_DIR}favicon.svg`, markSvg(64));
console.log('wrote favicon.svg');
