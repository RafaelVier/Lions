import { act, render, renderHook, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AppProviders } from '@/app/providers';
import { http, resetHttp } from '@/api/http';
import { useAuth } from '@/features/auth/AuthContext';
import { RequireAuth } from '@/features/auth/RequireAuth';
import {
  ACTIVITY_KEY,
  INACTIVITY_LIMIT_MS,
  getSession,
  resetSessionForTests,
} from '@/features/auth/session';
import { config } from '@/api/config';
import { authService } from '@/services/auth.service';
import { storeSession } from '@/test/session';

const wrapper = ({ children }: { children: ReactNode }) => <AppProviders>{children}</AppProviders>;

beforeEach(() => resetSessionForTests());
afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  resetSessionForTests();
  resetHttp();
});

describe('AuthProvider', () => {
  it('login stores the session and later requests carry the token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('{}', { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);
    const { result } = renderHook(() => useAuth(), { wrapper });

    let token = '';
    await act(async () => {
      const session = await result.current.login({ username: 'admin', password: 'lions2026' });
      token = session.token;
    });
    expect(result.current.session?.username).toBe('admin');
    expect(getSession()?.token).toBe(token);

    await http.get('/x');
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect((init.headers as Record<string, string>).Authorization).toBe(`Bearer ${token}`);
  });

  it('logout clears the session even if the service rejects', async () => {
    storeSession('ROLE_ADMIN');
    vi.spyOn(authService, 'logout').mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.session).not.toBeNull();

    await act(async () => {
      await result.current.logout();
    });
    expect(result.current.session).toBeNull();
    expect(result.current.endReason).toBe('logout');
    expect(getSession()).toBeNull();
  });

  it('registers on the server an inactivity ending decided by this tab', async () => {
    vi.useFakeTimers();
    storeSession('ROLE_ADMIN');
    // falha do servidor é ignorada (sem rejeição solta)
    const logout = vi.spyOn(authService, 'logout').mockRejectedValue(new Error('offline'));
    const { result } = renderHook(() => useAuth(), { wrapper });

    await act(async () => {
      await vi.advanceTimersByTimeAsync(INACTIVITY_LIMIT_MS - 1);
    });
    // o token pode ter sido renovado aos 28 min: vale o token em uso no fim
    const token = getSession()?.token;
    expect(token).toBeDefined();
    expect(logout).not.toHaveBeenCalled();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1);
    });
    expect(result.current.session).toBeNull();
    expect(result.current.endReason).toBe('inactivity');
    expect(logout).toHaveBeenCalledExactlyOnceWith(token);
  });

  it('starts logged out with the inactivity reason after F5 past 30 idle minutes', async () => {
    const stored = storeSession('ROLE_ADMIN');
    localStorage.setItem(ACTIVITY_KEY, String(Date.now() - INACTIVITY_LIMIT_MS));
    const logout = vi.spyOn(authService, 'logout').mockResolvedValue(undefined);

    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.session).toBeNull();
    expect(result.current.endReason).toBe('inactivity');
    // o encerramento também é registrado no servidor, com o token descartado
    await waitFor(() => expect(logout).toHaveBeenCalledExactlyOnceWith(stored.token));
  });

  it('ends the session as unauthorized when an authenticated request gets 401', async () => {
    storeSession('ROLE_ADMIN');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{}', { status: 401 })));

    function Probe() {
      const state = useLocation().state as { reason?: string } | null;
      return <p>reason:{state?.reason}</p>;
    }
    render(
      <AppProviders>
        <MemoryRouter initialEntries={['/estoque']}>
          <Routes>
            <Route path="/login" element={<Probe />} />
            <Route
              path="/estoque"
              element={
                <RequireAuth>
                  <p>Estoque</p>
                </RequireAuth>
              }
            />
          </Routes>
        </MemoryRouter>
      </AppProviders>,
    );
    expect(screen.getByText('Estoque')).toBeInTheDocument();

    await act(async () => {
      await http.get('/x').catch(() => {});
    });
    expect(screen.getByText('reason:unauthorized')).toBeInTheDocument();
  });

  it('explicit logout is not reported as unauthorized when the server answers 401', async () => {
    storeSession('ROLE_ADMIN');
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ title: 'Unauthorized', status: 401 }), {
        status: 401,
        headers: { 'Content-Type': 'application/problem+json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);
    const original = config.useMocks;
    config.useMocks = false;
    try {
      let auth: ReturnType<typeof useAuth> | undefined;
      function Grab() {
        auth = useAuth();
        return null;
      }
      function Probe() {
        const state = useLocation().state as { reason?: string } | null;
        return <p>reason:{state?.reason}</p>;
      }
      render(
        <AppProviders>
          <Grab />
          <MemoryRouter initialEntries={['/estoque']}>
            <Routes>
              <Route path="/login" element={<Probe />} />
              <Route
                path="/estoque"
                element={
                  <RequireAuth>
                    <p>Estoque</p>
                  </RequireAuth>
                }
              />
            </Routes>
          </MemoryRouter>
        </AppProviders>,
      );
      await act(async () => {
        await auth!.logout();
      });
      expect(fetchMock).toHaveBeenCalledTimes(1);
      expect(getSession()).toBeNull();
      expect(auth!.endReason).toBe('logout');
      expect(screen.getByText('reason:logout')).toBeInTheDocument();
    } finally {
      config.useMocks = original;
    }
  });
});
