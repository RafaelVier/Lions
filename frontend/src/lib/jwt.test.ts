import { describe, expect, it } from 'vitest';
import { decodeJwtPayload, getJwtExpMs } from '@/lib/jwt';

// Só para payloads ASCII.
function base64url(value: unknown): string {
  return btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

describe('jwt', () => {
  it('decodes base64url payloads without padding', () => {
    // {"sub":"joão","note":">>>?","exp":1800000000}: tem '-' e '_', e o base64 padrão teria '=='
    const payload = 'eyJzdWIiOiJqb8OjbyIsIm5vdGUiOiI-Pj4_IiwiZXhwIjoxODAwMDAwMDAwfQ';
    const token = `${base64url({ alg: 'HS256' })}.${payload}.sig`;

    expect(decodeJwtPayload(token)).toEqual({ sub: 'joão', note: '>>>?', exp: 1_800_000_000 });
    expect(getJwtExpMs(token)).toBe(1_800_000_000_000);
  });

  it('returns null exp for malformed tokens', () => {
    const header = base64url({ alg: 'HS256' });

    expect(getJwtExpMs('abc')).toBeNull();
    expect(getJwtExpMs('a.b')).toBeNull();
    expect(getJwtExpMs(`${header}.${base64url({ sub: 'admin' })}.sig`)).toBeNull();
    expect(getJwtExpMs(`${header}.${base64url({ exp: '1800000000' })}.sig`)).toBeNull();
    expect(decodeJwtPayload(`${header}.%%%.sig`)).toBeNull();
    expect(decodeJwtPayload(`${header}.${base64url([1, 2])}.sig`)).toBeNull();
  });
});
