import { describe, expect, it } from 'vitest';
import { readConfig } from '@/api/config';

describe('readConfig', () => {
  it('returns defaults for an empty env', () => {
    expect(readConfig({})).toEqual({
      apiUrl: '/api/v1',
      useMocks: true,
      mockTokenTtlSeconds: 1800,
      mockDelayMs: 400,
    });
  });

  it('disables mocks only when VITE_USE_MOCKS is "false"', () => {
    expect(readConfig({ VITE_USE_MOCKS: 'false' }).useMocks).toBe(false);
  });

  it('strips the trailing slash from the api url', () => {
    expect(readConfig({ VITE_API_URL: '/x/' }).apiUrl).toBe('/x');
  });

  it('falls back to 1800 when the token ttl is not a positive integer', () => {
    expect(readConfig({ VITE_MOCK_TOKEN_TTL_SECONDS: 'abc' }).mockTokenTtlSeconds).toBe(1800);
  });

  it('reads a valid token ttl', () => {
    expect(readConfig({ VITE_MOCK_TOKEN_TTL_SECONDS: '60' }).mockTokenTtlSeconds).toBe(60);
  });

  it('accepts a zero mock delay', () => {
    expect(readConfig({ VITE_MOCK_DELAY_MS: '0' }).mockDelayMs).toBe(0);
  });
});
