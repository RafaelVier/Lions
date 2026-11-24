import { describe, expect, it } from 'vitest';
import { safeRedirect } from '@/lib/safeRedirect';

describe('safeRedirect', () => {
  it.each([
    ['/estoque', '/estoque'],
    ['/estoque?x=1', '/estoque?x=1'],
    ['https://evil.com', '/'],
    ['//evil.com', '/'],
    ['/\\evil.com', '/'],
    ['/login', '/'],
    [null, '/'],
    ['/\t/evil.com', '/'],
    ['/\n/evil.com', '/'],
    ['/Login', '/'],
    ['/login/', '/'],
    ['/login?x=1', '/'],
    ['/login#a', '/'],
    ['/estoque#topo', '/estoque#topo'],
  ])('maps %j to %j', (raw, expected) => {
    expect(safeRedirect(raw)).toBe(expected);
  });

  it('keeps encoded slashes as a same-origin path', () => {
    const result = safeRedirect('/%2F%2Fevil.com');
    expect(result.startsWith('/')).toBe(true);
    expect(result.startsWith('//')).toBe(false);
  });
});
