import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { getInitialTheme, THEME_KEY, useTheme } from '@/hooks/useTheme';

afterEach(() => vi.restoreAllMocks());

function mockPrefersDark(matches: boolean) {
  vi.spyOn(window, 'matchMedia').mockImplementation(
    (query) =>
      ({
        matches,
        media: query,
        addEventListener() {},
        removeEventListener() {},
      }) as unknown as MediaQueryList,
  );
}

describe('useTheme', () => {
  it('reads the stored theme first', () => {
    mockPrefersDark(true);
    localStorage.setItem(THEME_KEY, 'light');
    expect(getInitialTheme()).toBe('light');
    expect(renderHook(() => useTheme()).result.current.theme).toBe('light');
  });

  it('falls back to prefers-color-scheme', () => {
    mockPrefersDark(true);
    expect(getInitialTheme()).toBe('dark');
    mockPrefersDark(false);
    expect(getInitialTheme()).toBe('light');
  });

  it('toggles the dark class and persists the choice', () => {
    const { result } = renderHook(() => useTheme());
    expect(document.documentElement).not.toHaveClass('dark');
    act(() => result.current.toggleTheme());
    expect(result.current.theme).toBe('dark');
    expect(document.documentElement).toHaveClass('dark');
    expect(localStorage.getItem(THEME_KEY)).toBe('dark');
    act(() => result.current.toggleTheme());
    expect(document.documentElement).not.toHaveClass('dark');
    expect(localStorage.getItem(THEME_KEY)).toBe('light');
  });
});
