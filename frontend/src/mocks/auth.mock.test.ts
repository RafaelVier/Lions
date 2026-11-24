import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { isApiError } from '@/api/errors';
import { getJwtExpMs } from '@/lib/jwt';
import { MOCK_PASSWORD, MOCK_USERS, createFakeJwt, mockAuth } from '@/mocks/auth.mock';

const NOW = new Date('2026-11-16T12:00:00Z').getTime();

describe('mockAuth', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it.each(MOCK_USERS)('logs in $username by username and by e-mail', async (user) => {
    for (const login of [user.username, user.email]) {
      const res = await mockAuth.login({ username: login, password: MOCK_PASSWORD });
      expect(res.user.role).toBe(user.role);
      expect(res.type).toBe('Bearer');
      expect(res.user.username).toBe(user.username);
      expect(res.user.email).toBe(user.email);
      expect(getJwtExpMs(res.token)).toBe(NOW + 1800 * 1000);
      expect(res.expiresIn).toBe(1800);
      expect(res.expiresAt).toBe(new Date(NOW + 1800 * 1000).toISOString());
    }
  });

  it('accepts surrounding spaces and upper case in the username', async () => {
    const res = await mockAuth.login({ username: '  ADMIN@Lions.org ', password: MOCK_PASSWORD });
    expect(res.user.username).toBe('admin');
  });

  it('rejects a wrong password with 401 and the exact message', async () => {
    const error = await mockAuth
      .login({ username: 'admin', password: 'errada' })
      .catch((e: unknown) => e);
    expect(isApiError(error)).toBe(true);
    expect(error).toMatchObject({ status: 401, detail: 'Usuário ou senha inválidos.' });
    await expect(
      mockAuth.login({ username: 'ninguem', password: MOCK_PASSWORD }),
    ).rejects.toMatchObject({
      status: 401,
      detail: 'Usuário ou senha inválidos.',
    });
  });

  it('refreshes a valid token for the same user and rejects garbage with 401', async () => {
    const { token } = await mockAuth.login({ username: 'operador', password: MOCK_PASSWORD });
    vi.setSystemTime(NOW + 60_000);
    const res = await mockAuth.refresh(token);
    expect(res.user.username).toBe('operador');
    expect(res.user.role).toBe('ROLE_OPERATOR');
    expect(getJwtExpMs(res.token)).toBe(NOW + 60_000 + 1800 * 1000);

    await expect(mockAuth.refresh('lixo')).rejects.toMatchObject({ status: 401 });
    const expired = createFakeJwt('admin', 'ROLE_ADMIN', 10, NOW - 60_000);
    await expect(mockAuth.refresh(expired)).rejects.toMatchObject({ status: 401 });
  });
});
