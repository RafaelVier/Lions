import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { renderApp } from '@/test/renderApp';

// matchMedia controlável: o teste muda a largura da tela e avisa quem está ouvindo.
function stubMatchMedia() {
  const matches = new Map<string, boolean>();
  const listeners = new Map<string, Set<(event: MediaQueryListEvent) => void>>();
  vi.spyOn(window, 'matchMedia').mockImplementation(
    (query) =>
      ({
        get matches() {
          return matches.get(query) ?? false;
        },
        media: query,
        onchange: null,
        addEventListener: (_type: string, listener: (event: MediaQueryListEvent) => void) => {
          if (!listeners.has(query)) listeners.set(query, new Set());
          listeners.get(query)?.add(listener);
        },
        removeEventListener: (_type: string, listener: (event: MediaQueryListEvent) => void) => {
          listeners.get(query)?.delete(listener);
        },
        addListener: () => {},
        removeListener: () => {},
        dispatchEvent: () => false,
      }) as unknown as MediaQueryList,
  );
  return {
    set(query: string, value: boolean) {
      matches.set(query, value);
      for (const listener of [...(listeners.get(query) ?? [])]) {
        listener({ matches: value, media: query } as MediaQueryListEvent);
      }
    },
  };
}

describe('app layout', () => {
  it('shows admin-only Usuários only to ROLE_ADMIN', () => {
    const admin = renderApp('/', { role: 'ROLE_ADMIN' });
    expect(screen.getAllByRole('link')).toHaveLength(9);
    expect(screen.getByRole('link', { name: 'Usuários' })).toBeInTheDocument();
    admin.unmount();

    renderApp('/', { role: 'ROLE_OPERATOR' });
    expect(screen.getAllByRole('link')).toHaveLength(8);
    expect(screen.queryByRole('link', { name: 'Usuários' })).not.toBeInTheDocument();
  });

  it('denies /usuarios to ROLE_VIEWER', () => {
    renderApp('/usuarios', { role: 'ROLE_VIEWER' });
    expect(screen.getByText('Acesso negado')).toBeInTheDocument();
  });

  it('marks only Novo Item as current on /estoque/novo', () => {
    renderApp('/estoque/novo', { role: 'ROLE_ADMIN' });
    const current = screen
      .getAllByRole('link')
      .filter((l) => l.getAttribute('aria-current') === 'page');
    expect(current.map((l) => l.textContent)).toEqual(['Novo Item']);
  });

  it('shows the logged user name and e-mail', () => {
    renderApp('/', { role: 'ROLE_ADMIN' });
    const aside = screen.getByRole('complementary');
    expect(within(aside).getByText('admin')).toBeInTheDocument();
    expect(within(aside).getByText(/@/)).toBeInTheDocument();
  });

  it('logs out with Sair without carrying the current page to the next login', async () => {
    const { router } = renderApp('/estoque?x=1', { role: 'ROLE_ADMIN' });
    await userEvent.click(screen.getByRole('button', { name: 'Sair' }));
    expect(await screen.findByRole('button', { name: 'Acessar Plataforma' })).toBeInTheDocument();
    expect(localStorage.getItem('lions.session')).toBeNull();
    expect(router.state.location.pathname).toBe('/login');
    expect(router.state.location.search).toBe('');
  });

  it('closes the drawer when the viewport grows to desktop width', async () => {
    const media = stubMatchMedia();
    renderApp('/', { role: 'ROLE_ADMIN' });
    await userEvent.click(screen.getByRole('button', { name: 'Abrir menu' }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();

    act(() => media.set('(min-width: 1024px)', true));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('opens and closes the mobile drawer', async () => {
    renderApp('/', { role: 'ROLE_ADMIN' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Abrir menu' }));
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    await userEvent.click(within(dialog).getByRole('link', { name: 'Estoque' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Estoque' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Abrir menu' }));
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('keeps Tab and Shift+Tab inside the open drawer', async () => {
    renderApp('/', { role: 'ROLE_ADMIN' });
    await userEvent.click(screen.getByRole('button', { name: 'Abrir menu' }));
    const dialog = screen.getByRole('dialog');
    const focusable = [...dialog.querySelectorAll<HTMLElement>('a[href], button')];
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    expect(first).toHaveAccessibleName('Fechar menu');
    expect(last).toHaveAccessibleName('Sair');

    last.focus();
    await userEvent.tab();
    expect(first).toHaveFocus();
    await userEvent.tab({ shift: true });
    expect(last).toHaveFocus();
  });

  it.each([
    ['Esc', () => userEvent.keyboard('{Escape}')],
    ['Fechar menu', () => userEvent.click(screen.getByRole('button', { name: 'Fechar menu' }))],
  ])('returns focus to Abrir menu when closing with %s', async (_label, close) => {
    renderApp('/', { role: 'ROLE_ADMIN' });
    const opener = screen.getByRole('button', { name: 'Abrir menu' });
    await userEvent.click(opener);
    await close();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(opener).toHaveFocus();
  });
});
