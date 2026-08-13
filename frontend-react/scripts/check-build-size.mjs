import { readFileSync, statSync } from 'node:fs';
import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { findEntryAssets, isAboveLimit, DEFAULT_LIMIT } from '../src/utils/buildSize.js';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const dist = join(root, 'dist');
const html = readFileSync(join(dist, 'index.html'), 'utf8');
const base = '/react-dashboard/';
const assets = findEntryAssets(html);

const { above } = isAboveLimit(assets, DEFAULT_LIMIT);
if (!above) {
  console.error('[build-size] ERROR: no se encontraron assets JS de entry en dist/index.html');
  process.exit(1);
}

const sizes = assets.map((asset) => {
  const clean = asset.replace(/^\//, '');
  const file = join(dist, clean.replace(/^react-dashboard\//, ''));
  const size = statSync(file).size;
  console.log(`[build-size] ${clean} -> ${(size / 1024).toFixed(1)} kB`);
  return { clean, size };
});

const max = Math.max(...sizes.map((s) => s.size));
if (max > DEFAULT_LIMIT) {
  console.error(
    `[build-size] FAIL: entry JS ${(max / 1024).toFixed(1)} kB supera el límite ` +
    `${(DEFAULT_LIMIT / 1024).toFixed(0)} kB. Aplicar más code-splitting o manualChunks.`
  );
  process.exit(1);
}

console.log(`[build-size] OK: entry JS max ${(max / 1024).toFixed(1)} kB <= ${(DEFAULT_LIMIT / 1024).toFixed(0)} kB`);
