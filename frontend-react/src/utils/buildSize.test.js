import { describe, it, expect } from 'vitest';
import { findEntryAssets, isAboveLimit, buildSizeReport } from './buildSize';

describe('findEntryAssets', () => {
  it('extrae los scripts del entry desde index.html', () => {
    const html =
      '<html><head><script type="module" crossorigin src="/react-dashboard/assets/index-abc.js"></script></head><body></body></html>';
    expect(findEntryAssets(html)).toContain('/react-dashboard/assets/index-abc.js');
  });
});

describe('isAboveLimit', () => {
  it('normaliza rutas absolutas y detecta presencia de entry', () => {
    const result = isAboveLimit(['/react-dashboard/assets/index-abc.js'], 400 * 1024);
    expect(result.assets).toEqual(['react-dashboard/assets/index-abc.js']);
    expect(result.above).toBe(true);
  });
});

describe('buildSizeReport', () => {
  it('resuelve rutas relativas contra la base del SPA', () => {
    const html =
      '<script type="module" crossorigin src="assets/index-abc.js"></script>';
    const report = buildSizeReport(html, '/react-dashboard/', 400 * 1024);
    expect(report).toHaveLength(1);
    expect(report[0].path).toBe('react-dashboard/assets/index-abc.js');
  });
});
