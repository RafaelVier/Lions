import { act, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { AppProviders } from '@/app/providers';
import { RequireAuth } from '@/features/auth/RequireAuth';
import { RequireRole } from '@/features/auth/RequireRole';
import {
  ACTIVITY_KEY,
  INACTIVITY_LIMIT_MS,
  endSession,
  resetSessionForTests,
} from '@/features/auth/session';
import { authService } from '@/services/auth.service';
import { resetHttp } from '@/api/http';
import { storeSession } from '@/test/session';

function LoginProbe() {
  const location = useLocation();
  const state = location.state as { reason?: string | null } | null;
  return (
    <div>
      <p data-testid="search">{location.search}</p>
      <p data-testid="reason">{String(state?.reason ?? 'none')}</p>
    </div>
  );
}

function renderAt(path: string) {
  return render(
    <AppProviders>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/login" element={<LoginProbe />} />
          <Route
            path="/estoque"
            element={
              <RequireAuth>
                <p>Estoque</p>
              </RequireAuth>
            }
          />
          <Route
            path="/usuarios"
            element={
              <RequireAuth>
                <RequireRole roles={['ROLE_ADMIN']}>
                  <p>Usuários</p>
                </RequireRole>
              </RequireAuth>
            }
          />
        </Routes>
      </MemoryRouter>
    </AppProviders>,
  );
}

beforeEach(() => resetSessionForTests());
afterEach(() => {
  resetSessionForTests();
  resetHttp();
});

describe('route guards', () => {
  it('redirects to login with the encoded redirect param', () => {
    renderAt('/estoque?x=1');
    expect(screen.getByTestId('search')).toHaveTextContent('?redirect=%2Festoque%3Fx%3D1');
  });

  it('sends a reload after 30 idle minutes to login with the inactivity reason', async () => {
    const stored = storeSession('ROLE_OPERATOR');
    localStorage.setItem(ACTIVITY_KEY, String(Date.now() - INACTIVITY_LIMIT_MS));
    const logout = vi.spyOn(authService, 'logout').mockResolvedValue(undefined);

    renderAt('/estoque');

    expect(screen.queryByText('Estoque')).not.toBeInTheDocument();
    expect(screen.getByTestId('reason')).toHaveTextContent('inactivity');
    expect(screen.getByTestId('search')).toHaveTextContent('?redirect=%2Festoque');
    await waitFor(() => expect(logout).toHaveBeenCalledExactlyOnceWith(stored.token));
  });

  it('does not carry the redirect param after an explicit logout', () => {
    // o próximo usuário não herda a página (nem a busca) de quem saiu
    storeSession('ROLE_OPERATOR');
    renderAt('/estoque?x=1');
    expect(screen.getByText('Estoque')).toBeInTheDocument();

    act(() => endSession('logout'));

    expect(screen.getByTestId('search').textContent).toBe('');
    expect(screen.getByTestId('reason')).toHaveTextContent('logout');
  });

  it('keeps the user on the page after reload when a valid session is stored', () => {
    storeSession('ROLE_OPERATOR');
    renderAt('/estoque');
    expect(screen.getByText('Estoque')).toBeInTheDocument();
  });

  it('shows Acesso negado to ROLE_OPERATOR on an admin route', () => {
    storeSession('ROLE_OPERATOR');
    renderAt('/usuarios');
    expect(screen.getByText('Acesso negado')).toBeInTheDocument();
    expect(
      screen.getByText('Seu perfil não tem permissão para acessar esta página.'),
    ).toBeInTheDocument();
  });

  it('renders admin routes for ROLE_ADMIN', () => {
    storeSession('ROLE_ADMIN');
    renderAt('/usuarios');
    expect(screen.getByText('Usuários')).toBeInTheDocument();
  });
});
