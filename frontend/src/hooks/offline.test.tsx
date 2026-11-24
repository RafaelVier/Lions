import { act, renderHook, screen, waitFor } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createAppQueryClient } from '@/app/queryClient';
import { useAppMutation, OFFLINE_MESSAGE } from '@/hooks/useAppMutation';
import { useOnlineStatus } from '@/hooks/useOnlineStatus';
import { renderApp } from '@/test/renderApp';

function setOnline(value: boolean) {
  vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(value);
  window.dispatchEvent(new Event(value ? 'online' : 'offline'));
}

function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={createAppQueryClient()}>{children}</QueryClientProvider>;
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('offline preparation', () => {
  it('tracks online and offline events', () => {
    const { result } = renderHook(() => useOnlineStatus());
    expect(result.current).toBe(true);
    act(() => setOnline(false));
    expect(result.current).toBe(false);
    act(() => setOnline(true));
    expect(result.current).toBe(true);
  });

  it('refuses mutations offline without calling mutationFn', async () => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);
    const mutationFn = vi.fn().mockResolvedValue('ok');
    const { result } = renderHook(() => useAppMutation({ mutationFn }), { wrapper });
    act(() => result.current.mutate());
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error?.detail).toBe(OFFLINE_MESSAGE);
    expect(mutationFn).not.toHaveBeenCalled();
  });

  it('runs mutations online', async () => {
    const mutationFn = vi.fn().mockResolvedValue('ok');
    const { result } = renderHook(() => useAppMutation({ mutationFn }), { wrapper });
    act(() => result.current.mutate());
    await waitFor(() => expect(result.current.data).toBe('ok'));
    expect(mutationFn).toHaveBeenCalledTimes(1);
  });

  it('shows the offline banner in the app shell', async () => {
    renderApp('/', { role: 'ROLE_ADMIN' });
    await screen.findByRole('main');
    act(() => setOnline(false));
    expect(await screen.findByText(OFFLINE_MESSAGE)).toBeInTheDocument();
  });
});
