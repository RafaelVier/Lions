import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, type InitialEntry } from 'react-router';
import { AppProviders } from '@/app/providers';
import { config } from '@/api/config';
import { GENERIC_ERROR_MESSAGE } from '@/api/errors';
import { resetHttp } from '@/api/http';
import { LoginPage } from '@/features/auth/LoginPage';
import { resetSessionForTests, SESSION_KEY } from '@/features/auth/session';
import { authService } from '@/services/auth.service';
import { storeSession } from '@/test/session';
import type { AuthResponse } from '@/types/api';

function renderLogin(entry: InitialEntry = '/login') {
  return render(
    <AppProviders>
      <MemoryRouter initialEntries={[entry]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<h1>Início</h1>} />
          <Route path="/estoque" element={<h1>Estoque</h1>} />
        </Routes>
      </MemoryRouter>
    </AppProviders>,
  );
}

async function fillAndSubmit(
  user: ReturnType<typeof userEvent.setup>,
  username: string,
  password: string,
) {
  await user.type(screen.getByLabelText('E-MAIL OU MATRÍCULA'), username);
  await user.type(screen.getByLabelText('SENHA'), password);
  await user.click(screen.getByRole('button', { name: 'Acessar Plataforma' }));
}

// JWT mínimo com exp daqui a 30 min (sem importar mocks)
function tokenValidFor30Min(): string {
  const payload = btoa(JSON.stringify({ sub: 'admin', exp: Math.floor(Date.now() / 1000) + 1800 }));
  return `e30.${payload}.sig`;
}

beforeEach(() => resetSessionForTests());
afterEach(() => {
  resetSessionForTests();
  resetHttp();
});

describe('LoginPage', () => {
  it('shows both validation messages and does not call the service on empty submit', async () => {
    const login = vi.spyOn(authService, 'login');
    const user = userEvent.setup();
    renderLogin();

    await user.click(screen.getByRole('button', { name: 'Acessar Plataforma' }));

    expect(await screen.findByText('O usuário ou email é obrigatório.')).toBeInTheDocument();
    expect(screen.getByText('A senha é obrigatória.')).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it('shows the invalid-credentials message on 401', async () => {
    const user = userEvent.setup();
    renderLogin();

    await fillAndSubmit(user, 'admin', 'errada');

    expect(await screen.findByRole('alert')).toHaveTextContent('Usuário ou senha inválidos.');
  });

  it('shows the network message when the server is unreachable', async () => {
    const previous = config.useMocks;
    config.useMocks = false;
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    try {
      const user = userEvent.setup();
      renderLogin();

      await fillAndSubmit(user, 'admin', 'lions2026');

      expect(await screen.findByRole('alert')).toHaveTextContent(
        'Não foi possível conectar ao servidor.',
      );
    } finally {
      config.useMocks = previous;
      vi.unstubAllGlobals();
    }
  });

  it('navigates to / after a successful login', async () => {
    const user = userEvent.setup();
    renderLogin();

    await fillAndSubmit(user, 'admin', 'lions2026');

    expect(await screen.findByRole('heading', { name: 'Início' })).toBeInTheDocument();
  });

  it('navigates to the redirect param after login', async () => {
    const user = userEvent.setup();
    renderLogin('/login?redirect=%2Festoque');

    await fillAndSubmit(user, 'admin', 'lions2026');

    expect(await screen.findByRole('heading', { name: 'Estoque' })).toBeInTheDocument();
  });

  it('ignores external redirect targets', async () => {
    const user = userEvent.setup();
    renderLogin('/login?redirect=https%3A%2F%2Fevil.com');

    await fillAndSubmit(user, 'admin', 'lions2026');

    expect(await screen.findByRole('heading', { name: 'Início' })).toBeInTheDocument();
  });

  it.each([
    { name: 'unknown role', patch: { role: 'ROLE_X' } },
    { name: 'token without exp', patch: { token: 'sem-exp' } },
  ])(
    'shows the generic error and stores no session for an invalid login response ($name)',
    async ({ patch }) => {
      vi.spyOn(authService, 'login').mockResolvedValue({
        token: tokenValidFor30Min(),
        type: 'Bearer',
        username: 'admin',
        email: 'admin@lions.org',
        role: 'ROLE_ADMIN',
        ...patch,
      } as AuthResponse);
      const user = userEvent.setup();
      renderLogin();

      await fillAndSubmit(user, 'admin', 'lions2026');

      expect(await screen.findByRole('alert')).toHaveTextContent(GENERIC_ERROR_MESSAGE);
      expect(localStorage.getItem(SESSION_KEY)).toBeNull();
      expect(screen.queryByRole('heading', { name: 'Início' })).not.toBeInTheDocument();
    },
  );

  it('calls login only once on double submit', async () => {
    // promessa pendente: o segundo envio acontece com a mutação ainda em andamento
    const login = vi
      .spyOn(authService, 'login')
      .mockReturnValue(new Promise<AuthResponse>(() => {}));
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText('E-MAIL OU MATRÍCULA'), 'admin');
    await user.type(screen.getByLabelText('SENHA'), 'lions2026');
    const button = screen.getByRole('button', { name: 'Acessar Plataforma' });
    await user.click(button);
    await waitFor(() => expect(button).toBeDisabled());
    expect(button).toHaveAttribute('aria-busy', 'true');

    await user.click(button);
    await user.type(screen.getByLabelText('SENHA'), '{Enter}');

    expect(login).toHaveBeenCalledTimes(1);
  });

  it('shows the inactivity message when redirected after inactivity', () => {
    renderLogin({ pathname: '/login', state: { reason: 'inactivity' } });

    expect(screen.getByRole('status')).toHaveTextContent(
      'Sua sessão expirou por inatividade. Entre novamente.',
    );
  });

  it('redirects an authenticated user away from /login', async () => {
    storeSession('ROLE_ADMIN');
    renderLogin();

    expect(await screen.findByRole('heading', { name: 'Início' })).toBeInTheDocument();
  });

  it('explains how to recover the password', async () => {
    const user = userEvent.setup();
    renderLogin();

    await user.click(screen.getByRole('button', { name: 'Esqueci minha senha' }));

    expect(
      screen.getByText('Fale com o administrador do clube para redefinir sua senha.'),
    ).toBeInTheDocument();
  });

  it('explains how to request access', async () => {
    const user = userEvent.setup();
    renderLogin();

    await user.click(screen.getByRole('button', { name: 'Solicitar acesso' }));

    expect(
      screen.getByText('Fale com o administrador do clube para liberar seu acesso.'),
    ).toBeInTheDocument();
  });

  it('shows the demo banner with credentials when using mocks', () => {
    renderLogin();

    expect(screen.getByText('Modo demonstração — dados de exemplo')).toBeInTheDocument();
    expect(
      screen.getByText('Entre com admin, operador ou consulta e a senha lions2026.'),
    ).toBeInTheDocument();
  });
});
