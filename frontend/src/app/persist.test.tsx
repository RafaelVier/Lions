import { act, render } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AppProviders } from '@/app/providers';
import { PERSIST_BUSTER, QUERY_CACHE_KEY } from '@/app/persist';
import { createAppQueryClient } from '@/app/queryClient';
import { persistQueryClientSave } from '@tanstack/react-query-persist-client';
import { createQueryPersister } from '@/app/persist';
import {
  ACTIVITY_KEY,
  endSession,
  INACTIVITY_LIMIT_MS,
  resetSessionForTests,
  SESSION_KEY,
  setSession,
} from '@/features/auth/session';
import { createFakeJwt } from '@/mocks/auth.mock';
import { storeSession } from '@/test/session';

afterEach(() => {
  vi.useRealTimers();
});

async function mount(withSession: boolean) {
  vi.useFakeTimers();
  storeSession('ROLE_ADMIN');
  if (!withSession) endSession('logout');
  const queryClient = createAppQueryClient();
  render(
    <AppProviders queryClient={queryClient}>
      <div />
    </AppProviders>,
  );
  await act(async () => {
    await vi.advanceTimersByTimeAsync(0);
  });
  return queryClient;
}

async function persistX(queryClient: ReturnType<typeof createAppQueryClient>) {
  act(() => {
    queryClient.setQueryData(['x'], { a: 1 });
  });
  // maior que o throttle do persister (1 s)
  await act(async () => {
    await vi.advanceTimersByTimeAsync(5000);
  });
}

describe('query cache persistence', () => {
  it('persists query data and wipes it on logout', async () => {
    const queryClient = await mount(true);
    await persistX(queryClient);
    expect(localStorage.getItem(QUERY_CACHE_KEY)).not.toBeNull();

    act(() => endSession('logout'));
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(localStorage.getItem(QUERY_CACHE_KEY)).toBeNull();
    expect(queryClient.getQueryData(['x'])).toBeUndefined();
  });

  it('wipes the cache on any session end, not only logout', async () => {
    const queryClient = await mount(true);
    await persistX(queryClient);
    act(() => endSession('expired'));
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(localStorage.getItem(QUERY_CACHE_KEY)).toBeNull();
  });

  it('never writes the cache without a session', async () => {
    const queryClient = await mount(false);
    await persistX(queryClient);
    expect(localStorage.getItem(QUERY_CACHE_KEY)).toBeNull();
  });

  it('does not hydrate a leftover cache when there is no session', async () => {
    vi.useFakeTimers();
    storeSession('ROLE_ADMIN');
    const seed = createAppQueryClient();
    seed.setQueryData(['x'], { a: 1 });
    // mesmo buster do provider: senão a restauração descartaria o cache por outro motivo
    await persistQueryClientSave({
      queryClient: seed,
      persister: createQueryPersister(),
      buster: PERSIST_BUSTER,
    });
    await vi.advanceTimersByTimeAsync(5000);
    expect(localStorage.getItem(QUERY_CACHE_KEY)).not.toBeNull();
    // sessão some sem o provider montado
    localStorage.removeItem(SESSION_KEY);
    resetSessionForTests();

    const queryClient = createAppQueryClient();
    render(
      <AppProviders queryClient={queryClient}>
        <div />
      </AppProviders>,
    );
    await act(async () => {});
    expect(queryClient.getQueryData(['x'])).toBeUndefined();
    expect(localStorage.getItem(QUERY_CACHE_KEY)).toBeNull();
  });

  it('does not restore the cache after a reload past 30 idle minutes', async () => {
    vi.useFakeTimers();
    storeSession('ROLE_ADMIN');
    const seed = createAppQueryClient();
    seed.setQueryData(['x'], { a: 1 });
    // mesmo buster do provider: senão a restauração descartaria o cache por outro motivo
    await persistQueryClientSave({
      queryClient: seed,
      persister: createQueryPersister(),
      buster: PERSIST_BUSTER,
    });
    await vi.advanceTimersByTimeAsync(5000);
    expect(localStorage.getItem(QUERY_CACHE_KEY)).not.toBeNull();
    // F5 com o token ainda válido, mas sem atividade há 30 min
    localStorage.setItem(ACTIVITY_KEY, String(Date.now() - INACTIVITY_LIMIT_MS));
    resetSessionForTests();

    const queryClient = createAppQueryClient();
    render(
      <AppProviders queryClient={queryClient}>
        <div />
      </AppProviders>,
    );
    await act(async () => {});
    expect(queryClient.getQueryData(['x'])).toBeUndefined();
    expect(localStorage.getItem(QUERY_CACHE_KEY)).toBeNull();
  });

  it('wipes the cache when the session switches to another user', async () => {
    const queryClient = await mount(true);
    await persistX(queryClient);
    expect(localStorage.getItem(QUERY_CACHE_KEY)).not.toBeNull();

    act(() =>
      setSession({
        token: createFakeJwt('outro', 'ROLE_ADMIN', 30 * 60),
        username: 'outro',
        email: 'outro@example.com',
        role: 'ROLE_ADMIN',
      }),
    );
    expect(queryClient.getQueryData(['x'])).toBeUndefined();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });
    expect(localStorage.getItem(QUERY_CACHE_KEY)).toBeNull();
  });
});
