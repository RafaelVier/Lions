import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/errors';
import {
  ACTIVITY_KEY,
  MIN_REFRESH_INTERVAL_MS,
  SESSION_KEY,
  type Session,
  endSession,
  getLastActivity,
  getLoadEndReason,
  getSession,
  hasRole,
  markActivity,
  resetSessionForTests,
  setSession,
  startSessionManager,
  subscribe,
  waitForRefresh,
} from '@/features/auth/session';
import type { AuthResponse } from '@/types/api';

const MIN = 60_000;
// Segundo cheio: o exp do JWT é em segundos.
const NOW = Date.UTC(2026, 10, 16, 12, 0, 0);
const nowSeconds = NOW / 1000;
const past = nowSeconds - 60;

function base64url(value: unknown): string {
  return btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function jwt(expSeconds: number): string {
  return `${base64url({ alg: 'HS256', typ: 'JWT' })}.${base64url({ sub: 'admin', exp: expSeconds })}.sig`;
}

const valid: Session = {
  token: jwt(nowSeconds + 30 * 60),
  username: 'admin',
  email: 'admin@lions.org',
  role: 'ROLE_ADMIN',
};

// Token de 60 min: quem encerra a sessão é a inatividade, não o exp.
const longLived: Session = { ...valid, token: jwt(nowSeconds + 60 * 60) };

const refreshed: AuthResponse = {
  token: jwt(nowSeconds + 58 * 60),
  type: 'Bearer',
  username: 'admin',
  email: 'admin@lions.org',
  role: 'ROLE_ADMIN',
};

function deferred<T>() {
  let resolve: (value: T) => void = () => {};
  const promise = new Promise<T>((r) => {
    resolve = r;
  });
  return { promise, resolve };
}

beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(NOW);
  resetSessionForTests();
});

afterEach(() => {
  vi.useRealTimers();
});

describe('session storage', () => {
  it('loads a valid session from storage', () => {
    localStorage.setItem(SESSION_KEY, JSON.stringify(valid));

    expect(getSession()).toEqual(valid);
    expect(getLoadEndReason()).toBeNull();
  });

  it('discards on load a session idle for 30 minutes and reports inactivity', () => {
    // F5 depois de 30 min parado: nem a tela protegida nem o cache podem voltar
    localStorage.setItem(ACTIVITY_KEY, String(NOW - 30 * MIN));
    localStorage.setItem(SESSION_KEY, JSON.stringify(longLived));

    expect(getSession()).toBeNull();
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
    expect(getLoadEndReason()).toBe('inactivity');

    // uma sessão nova apaga o motivo antigo
    setSession(valid);
    expect(getLoadEndReason()).toBeNull();
  });

  it('keeps on load a session whose last activity is under 30 minutes old', () => {
    localStorage.setItem(ACTIVITY_KEY, String(NOW - 30 * MIN + 1));
    localStorage.setItem(SESSION_KEY, JSON.stringify(longLived));

    expect(getSession()).toEqual(longLived);
    expect(getLoadEndReason()).toBeNull();
  });

  it.each([
    '{bad json',
    JSON.stringify({ token: jwt(past) }),
    JSON.stringify({ ...valid, role: 'ROLE_X' }),
    JSON.stringify({ ...valid, token: 'no-exp' }),
  ])('discards invalid stored session %#', (raw) => {
    localStorage.setItem(SESSION_KEY, raw);

    expect(getSession()).toBeNull();
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it('discards a complete stored session whose token already expired', () => {
    localStorage.setItem(SESSION_KEY, JSON.stringify({ ...valid, token: jwt(past) }));

    expect(getSession()).toBeNull();
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it('persists and notifies on setSession', () => {
    const listener = vi.fn();
    subscribe(listener);

    setSession(valid);

    expect(listener).toHaveBeenCalledExactlyOnceWith(valid, null);
    expect(JSON.parse(localStorage.getItem(SESSION_KEY) ?? 'null')).toEqual(valid);
    expect(getSession()).toEqual(valid);
  });

  it.each([
    { name: 'unknown role', session: { ...valid, role: 'ROLE_X' as Session['role'] } },
    { name: 'expired token', session: { ...valid, token: jwt(past) } },
    { name: 'token without exp', session: { ...valid, token: 'no-exp' } },
  ])('rejects an invalid session in setSession ($name) without storing it', ({ session }) => {
    const listener = vi.fn();
    subscribe(listener);

    expect(() => setSession(session)).toThrow();

    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
    expect(getSession()).toBeNull();
    expect(listener).not.toHaveBeenCalled();
  });

  it('clears storage and notifies with the reason on endSession', () => {
    setSession(valid);
    const listener = vi.fn();
    const unsubscribe = subscribe(listener);

    endSession('logout');
    endSession('expired');

    expect(listener).toHaveBeenCalledExactlyOnceWith(null, 'logout');
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
    expect(getSession()).toBeNull();

    unsubscribe();
    setSession(valid);
    expect(listener).toHaveBeenCalledOnce();
  });

  it('tracks the last activity in storage', () => {
    expect(getLastActivity()).toBe(0);

    markActivity();
    expect(getLastActivity()).toBe(NOW);

    markActivity(NOW + 5_000);
    expect(localStorage.getItem(ACTIVITY_KEY)).toBe(String(NOW + 5_000));
    expect(getLastActivity()).toBe(NOW + 5_000);
  });

  it('checks roles with hasRole', () => {
    const operator: Session = { ...valid, role: 'ROLE_OPERATOR' };

    expect(hasRole(null, 'ROLE_ADMIN')).toBe(false);
    expect(hasRole(valid, 'ROLE_ADMIN')).toBe(true);
    expect(hasRole(operator, 'ROLE_ADMIN')).toBe(false);
    expect(hasRole(operator, 'ROLE_ADMIN', 'ROLE_OPERATOR')).toBe(true);
    expect(hasRole(valid)).toBe(false);
  });
});

describe('session manager', () => {
  it('refreshes 2 minutes before exp when the user was active', async () => {
    setSession(valid);
    markActivity();
    const refresh = vi.fn().mockResolvedValue(refreshed);
    startSessionManager({ refresh });

    await vi.advanceTimersByTimeAsync(28 * MIN - 1);
    expect(refresh).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1);
    expect(refresh).toHaveBeenCalledExactlyOnceWith(valid.token);
    expect(getSession()?.token).toBe(refreshed.token);
  });

  it('ends with inactivity 30 minutes after the last activity without refreshing', async () => {
    setSession(longLived);
    markActivity();
    const refresh = vi.fn().mockResolvedValue(refreshed);
    const listener = vi.fn();
    subscribe(listener);
    startSessionManager({ refresh });

    await vi.advanceTimersByTimeAsync(30 * MIN - 1);
    expect(getSession()).toEqual(longLived);
    expect(listener).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1);
    expect(listener).toHaveBeenCalledExactlyOnceWith(null, 'inactivity');
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();

    await vi.advanceTimersByTimeAsync(60 * MIN);
    expect(refresh).not.toHaveBeenCalled();
    expect(listener).toHaveBeenCalledOnce();
  });

  it.each([
    {
      name: 'postpones the inactivity deadline when there is new activity',
      act: () => markActivity(),
    },
    {
      name: 'counts activity recorded by another tab',
      // a outra aba grava direto no localStorage compartilhado
      act: () => localStorage.setItem(ACTIVITY_KEY, String(Date.now())),
    },
  ])('$name', async ({ act }) => {
    setSession(longLived);
    markActivity();
    const listener = vi.fn();
    subscribe(listener);
    startSessionManager({ refresh: vi.fn().mockResolvedValue(refreshed) });

    await vi.advanceTimersByTimeAsync(20 * MIN);
    act();
    await vi.advanceTimersByTimeAsync(10 * MIN);
    expect(getSession()).toEqual(longLived);

    await vi.advanceTimersByTimeAsync(20 * MIN - 1);
    expect(listener).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1);
    expect(listener).toHaveBeenCalledExactlyOnceWith(null, 'inactivity');
  });

  it('treats manager start without recorded activity as activity', async () => {
    localStorage.setItem(SESSION_KEY, JSON.stringify(longLived));
    const listener = vi.fn();
    subscribe(listener);
    startSessionManager({ refresh: vi.fn().mockResolvedValue(refreshed) });

    await vi.advanceTimersByTimeAsync(29 * MIN);

    expect(getLastActivity()).toBe(NOW);
    expect(getSession()).toEqual(longLived);
    expect(listener).not.toHaveBeenCalled();
  });

  it('ends with inactivity instead of refreshing when timers fire late', async () => {
    setSession(valid);
    markActivity();
    const refresh = vi.fn().mockResolvedValue(refreshed);
    const revoke = vi.fn().mockResolvedValue(undefined);
    const listener = vi.fn();
    subscribe(listener);
    startSessionManager({ refresh, revoke });

    // computador suspenso: o relógio anda 3 min e os temporizadores atrasam junto,
    // então a verificação (exp − 2 min) dispara quando já são 31 min sem atividade
    vi.setSystemTime(NOW + 3 * MIN);
    await vi.advanceTimersByTimeAsync(28 * MIN);
    expect(refresh).not.toHaveBeenCalled();
    expect(listener).toHaveBeenCalledExactlyOnceWith(null, 'inactivity');
    expect(revoke).toHaveBeenCalledExactlyOnceWith(valid.token);

    await vi.advanceTimersByTimeAsync(5 * MIN);
    expect(listener).toHaveBeenCalledOnce();
  });

  it('revokes the token on the server when this tab ends the session by inactivity', async () => {
    setSession(longLived);
    markActivity();
    // falha no servidor é ignorada: o encerramento local já valeu
    const revoke = vi.fn().mockRejectedValue(new ApiError(0, 'Sem conexão', 'Sem rede.'));
    startSessionManager({ refresh: vi.fn().mockResolvedValue(refreshed), revoke });

    await vi.advanceTimersByTimeAsync(30 * MIN - 1);
    expect(revoke).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1);
    expect(revoke).toHaveBeenCalledExactlyOnceWith(longLived.token);
    expect(getSession()).toBeNull();
  });

  it('revokes once, at manager start, a session discarded on load for inactivity', async () => {
    localStorage.setItem(ACTIVITY_KEY, String(NOW - 31 * MIN));
    localStorage.setItem(SESSION_KEY, JSON.stringify(longLived));
    const revoke = vi.fn().mockResolvedValue(undefined);
    const refresh = vi.fn().mockResolvedValue(refreshed);

    // o React em modo estrito monta, desmonta e monta de novo
    startSessionManager({ refresh, revoke })();
    startSessionManager({ refresh, revoke });
    await vi.advanceTimersByTimeAsync(0);

    expect(revoke).toHaveBeenCalledExactlyOnceWith(longLived.token);
    expect(getSession()).toBeNull();
  });

  it('runs a single refresh even when waitForRefresh is awaited concurrently', async () => {
    setSession(valid);
    markActivity();
    const pending = deferred<AuthResponse>();
    const refresh = vi.fn(() => pending.promise);
    // Dois gerenciadores disparam a verificação no mesmo instante.
    startSessionManager({ refresh });
    startSessionManager({ refresh });

    await vi.advanceTimersByTimeAsync(28 * MIN);
    const tokensSeen: (string | undefined)[] = [];
    const waits = [waitForRefresh(), waitForRefresh()].map((p) =>
      p.then(() => tokensSeen.push(getSession()?.token)),
    );
    await vi.advanceTimersByTimeAsync(10);
    expect(tokensSeen).toEqual([]);

    pending.resolve(refreshed);
    await Promise.all(waits);

    expect(refresh).toHaveBeenCalledOnce();
    expect(tokensSeen).toEqual([refreshed.token, refreshed.token]);
    await expect(waitForRefresh()).resolves.toBeUndefined();
  });

  it('limits refresh attempts when tokens arrive inside the lead window (clock skew)', async () => {
    // relógio do cliente adiantado: todo token chega com exp a 1 min de "agora"
    const shortToken = () => jwt(Math.floor(Date.now() / 1000) + 60);
    setSession({ ...valid, token: shortToken() });
    markActivity();
    const refresh = vi.fn(async () => ({ ...refreshed, token: shortToken() }));
    startSessionManager({ refresh });

    await vi.advanceTimersByTimeAsync(5 * MIN);

    // t0, +1, +2, +3, +4 e +5 min (MIN_REFRESH_INTERVAL_MS)
    expect(MIN_REFRESH_INTERVAL_MS).toBe(MIN);
    expect(refresh).toHaveBeenCalledTimes(6);
    expect(getSession()).not.toBeNull();
  });

  it('completes a refresh that itself awaits waitForRefresh', async () => {
    setSession(valid);
    markActivity();
    const refresh = vi.fn(async () => {
      await waitForRefresh();
      return refreshed;
    });
    startSessionManager({ refresh });

    await vi.advanceTimersByTimeAsync(28 * MIN + 1_000);

    expect(refresh).toHaveBeenCalledOnce();
    expect(getSession()?.token).toBe(refreshed.token);
  });

  it('ignores the refresh result when the user logs out meanwhile', async () => {
    setSession(valid);
    markActivity();
    const pending = deferred<AuthResponse>();
    startSessionManager({ refresh: () => pending.promise });

    await vi.advanceTimersByTimeAsync(28 * MIN);
    endSession('logout');
    pending.resolve(refreshed);
    await waitForRefresh();

    expect(getSession()).toBeNull();
    expect(localStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it('ends with unauthorized when refresh returns 401', async () => {
    setSession(valid);
    markActivity();
    const refresh = vi
      .fn()
      .mockRejectedValue(new ApiError(401, 'Não autenticado', 'Token inválido.'));
    const listener = vi.fn();
    subscribe(listener);
    startSessionManager({ refresh });

    await vi.advanceTimersByTimeAsync(28 * MIN);

    expect(listener).toHaveBeenCalledExactlyOnceWith(null, 'unauthorized');
    expect(getSession()).toBeNull();
  });

  it('retries a refresh that failed for network reasons after the minimum interval', async () => {
    setSession(valid);
    markActivity();
    const refresh = vi
      .fn()
      .mockRejectedValueOnce(new ApiError(0, 'Sem conexão', 'Sem rede.'))
      .mockResolvedValueOnce(refreshed);
    const listener = vi.fn();
    subscribe(listener);
    startSessionManager({ refresh });

    await vi.advanceTimersByTimeAsync(20 * MIN);
    markActivity();
    await vi.advanceTimersByTimeAsync(8 * MIN);
    expect(refresh).toHaveBeenCalledOnce();
    expect(getSession()?.token).toBe(valid.token);

    await vi.advanceTimersByTimeAsync(MIN - 1);
    expect(refresh).toHaveBeenCalledOnce();

    await vi.advanceTimersByTimeAsync(1);
    expect(refresh).toHaveBeenCalledTimes(2);
    expect(getSession()?.token).toBe(refreshed.token);

    // passa do exp do token antigo sem encerrar
    await vi.advanceTimersByTimeAsync(5 * MIN);
    expect(getSession()?.token).toBe(refreshed.token);
    expect(listener).not.toHaveBeenCalledWith(null, expect.anything());
  });

  it('treats an invalid refresh response as a failure and retries', async () => {
    setSession(valid);
    markActivity();
    const refresh = vi
      .fn()
      .mockResolvedValueOnce({ ...refreshed, role: 'ROLE_X' })
      .mockResolvedValueOnce(refreshed);
    startSessionManager({ refresh });

    await vi.advanceTimersByTimeAsync(28 * MIN);
    expect(refresh).toHaveBeenCalledOnce();
    expect(getSession()).toEqual(valid);
    expect(JSON.parse(localStorage.getItem(SESSION_KEY) ?? 'null')).toEqual(valid);

    await vi.advanceTimersByTimeAsync(MIN);
    expect(refresh).toHaveBeenCalledTimes(2);
    expect(getSession()?.token).toBe(refreshed.token);
  });

  it('ends with expired at exp when refresh keeps failing for network reasons', async () => {
    setSession(valid);
    markActivity();
    const refresh = vi.fn().mockRejectedValue(new ApiError(0, 'Sem conexão', 'Sem rede.'));
    const revoke = vi.fn().mockResolvedValue(undefined);
    const listener = vi.fn();
    subscribe(listener);
    startSessionManager({ refresh, revoke });

    // usuário ativo: quem encerra é o exp, não a inatividade
    await vi.advanceTimersByTimeAsync(20 * MIN);
    markActivity();
    await vi.advanceTimersByTimeAsync(10 * MIN - 1);
    // tentativas aos 28 e aos 29 min; a próxima (30 min) já cairia no exp
    expect(refresh).toHaveBeenCalledTimes(2);
    expect(listener).not.toHaveBeenCalled();
    await expect(waitForRefresh()).resolves.toBeUndefined();

    await vi.advanceTimersByTimeAsync(1);
    expect(listener).toHaveBeenCalledExactlyOnceWith(null, 'expired');
    expect(refresh).toHaveBeenCalledTimes(2);
    // só o fim por inatividade é registrado no servidor
    expect(revoke).not.toHaveBeenCalled();
  });

  it('logs out when another tab removes the session', async () => {
    setSession(valid);
    markActivity();
    const refresh = vi.fn().mockResolvedValue(refreshed);
    const listener = vi.fn();
    subscribe(listener);
    startSessionManager({ refresh });

    localStorage.removeItem(SESSION_KEY);
    window.dispatchEvent(new StorageEvent('storage', { key: SESSION_KEY, newValue: null }));

    expect(listener).toHaveBeenCalledExactlyOnceWith(null, 'logout');
    expect(getSession()).toBeNull();
    await vi.advanceTimersByTimeAsync(30 * MIN);
    expect(refresh).not.toHaveBeenCalled();
  });

  it('reports inactivity when another tab removes the session after 30 idle minutes', async () => {
    setSession(longLived);
    markActivity();
    const revoke = vi.fn().mockResolvedValue(undefined);
    const listener = vi.fn();
    subscribe(listener);
    startSessionManager({ refresh: vi.fn().mockResolvedValue(refreshed), revoke });

    // aba em segundo plano: o relógio anda, o temporizador desta aba ainda não disparou
    vi.setSystemTime(NOW + 30 * MIN);
    localStorage.removeItem(SESSION_KEY);
    window.dispatchEvent(new StorageEvent('storage', { key: SESSION_KEY, newValue: null }));

    expect(listener).toHaveBeenCalledExactlyOnceWith(null, 'inactivity');
    // quem registra o encerramento é a aba que o decidiu
    await vi.advanceTimersByTimeAsync(MIN);
    expect(revoke).not.toHaveBeenCalled();
  });

  it('reloads the session when another tab writes a new one', async () => {
    setSession(valid);
    markActivity();
    const refresh = vi.fn().mockResolvedValue(refreshed);
    const listener = vi.fn();
    subscribe(listener);
    startSessionManager({ refresh });

    // atividade registrada em outra aba não mexe na sessão
    window.dispatchEvent(new StorageEvent('storage', { key: ACTIVITY_KEY, newValue: String(NOW) }));
    expect(listener).not.toHaveBeenCalled();

    localStorage.setItem(SESSION_KEY, JSON.stringify(longLived));
    window.dispatchEvent(
      new StorageEvent('storage', { key: SESSION_KEY, newValue: JSON.stringify(longLived) }),
    );

    expect(listener).toHaveBeenCalledExactlyOnceWith(longLived, null);
    expect(getSession()).toEqual(longLived);
    // A verificação passa a seguir o exp do token novo (o antigo renovaria aos 28 min).
    await vi.advanceTimersByTimeAsync(29 * MIN);
    expect(refresh).not.toHaveBeenCalled();
    expect(getSession()).toEqual(longLived);
  });

  it('stops timers and the storage listener when stopped', async () => {
    setSession(valid);
    markActivity();
    const refresh = vi.fn().mockResolvedValue(refreshed);
    const listener = vi.fn();
    subscribe(listener);
    const stop = startSessionManager({ refresh });

    stop();
    await vi.advanceTimersByTimeAsync(30 * MIN);
    window.dispatchEvent(new StorageEvent('storage', { key: SESSION_KEY, newValue: null }));

    expect(refresh).not.toHaveBeenCalled();
    expect(listener).not.toHaveBeenCalled();
  });
});
