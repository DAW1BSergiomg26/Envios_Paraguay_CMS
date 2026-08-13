const JS_RE = /<script[^>]+src=["']([^"']+\.js)["']/g;

export function findEntryAssets(html) {
  const assets = [];
  let match;
  while ((match = JS_RE.exec(html)) !== null) {
    assets.push(match[1]);
  }
  return assets;
}

function resolvePath(asset, base) {
  if (/^https?:\/\//.test(asset)) return asset;
  return (base + asset).replace(/\/{2,}/g, '/');
}

export function isAboveLimit(assets, _limit) {
  const parsed = assets.map((asset) => asset.replace(/^\.?\/+/, ''));
  return { assets: parsed, above: parsed.length > 0 };
}

export function buildSizeReport(html, base, _limit) {
  const assets = findEntryAssets(html).map((asset) => resolvePath(asset, base));
  return assets.map((asset) => {
    const parts = asset.split('/');
    const name = parts[parts.length - 1];
    const path = asset.replace(/^\/+/, '');
    return { name, path, size: 0, bytes: 0 };
  });
}

export const DEFAULT_LIMIT = 400 * 1024;
