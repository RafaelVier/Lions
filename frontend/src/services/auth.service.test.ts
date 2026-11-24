import { afterEach, describe, expect, it, vi } from 'vitest';
import { config } from '@/api/config';
import { http } from '@/api/http';
import { mockAuth } from '@/mocks/auth.mock';
import { authService } from '@/services/auth.service';
import type { AuthResponse } from '@/types/api';

const RESPONSE: AuthResponse = {
  token: 'a.b.c',
  type: 'Bearer',
  username: 'admin',
  email: 'admin@lions.org',
  role: 'ROLE_ADMIN',
};

afterEach(() => {
  vi.restoreAllMocks();
});

describe('authService', () => {
  it('delegates to mockAuth when config.useMocks is true', async () => {
    const spy = vi.spyOn(mockAuth, 'login').mockResolvedValue(RESPONSE);
    const post = vi.spyOn(http, 'post');
    const req = { username: 'admin', password: 'x' };
    await expect(authService.login(req)).resolves.toBe(RESPONSE);
    expect(spy).toHaveBeenCalledWith(req);
    expect(post).not.toHaveBeenCalled();
  });

  it('posts to /auth/login without auth when useMocks is false', async () => {
    const post = vi.spyOn(http, 'post').mockResolvedValue(RESPONSE);
    const original = config.useMocks;
    config.useMocks = false;
    try {
      const req = { username: 'admin', password: 'x' };
      await expect(authService.login(req)).resolves.toBe(RESPONSE);
      expect(post).toHaveBeenCalledWith('/auth/login', req, { auth: false });
    } finally {
      config.useMocks = original;
    }
  });

  it('posts to /auth/refresh with the explicit token and to /auth/logout', async () => {
    const post = vi.spyOn(http, 'post').mockResolvedValue(RESPONSE);
    const original = config.useMocks;
    config.useMocks = false;
    try {
      await authService.refresh('tok');
      expect(post).toHaveBeenCalledWith('/auth/refresh', undefined, { token: 'tok' });
      await authService.logout('tok');
      expect(post).toHaveBeenLastCalledWith('/auth/logout', undefined, { token: 'tok' });
    } finally {
      config.useMocks = original;
    }
  });
});
