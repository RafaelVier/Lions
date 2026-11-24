import { expect, it } from 'vitest';

const files = import.meta.glob('/src/{features,components}/**/*.{ts,tsx}', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>;

// Pega import estático (from '...'), de efeito colateral (import '...') e dinâmico (import('...')).
const MOCKS_IMPORT = /(?:from\s*|import\s*\(?\s*)['"][^'"]*\/mocks(?:\/|['"])/;

it('detects static, side-effect and dynamic imports of mocks', () => {
  expect(MOCKS_IMPORT.test(`import x from '@/mocks/x';`)).toBe(true);
  expect(MOCKS_IMPORT.test(`import '@/mocks/x';`)).toBe(true);
  expect(MOCKS_IMPORT.test(`const m = await import('../mocks/x');`)).toBe(true);
  expect(MOCKS_IMPORT.test(`import x from '@/services/auth.service';`)).toBe(false);
});

it('screens and components never import mocks', () => {
  const offenders = Object.entries(files)
    .filter(([, src]) => MOCKS_IMPORT.test(src))
    .map(([path]) => path);
  expect(offenders).toEqual([]);
});
