import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';

const PAGES: [string, string][] = [
  ['/', 'Painel de Controle do Acervo'],
  ['/estoque', 'Estoque'],
  ['/estoque/novo', 'Novo Equipamento'],
  ['/estoque/LCM-CR-0089', 'Equipamento LCM-CR-0089'],
  ['/emprestimos', 'Empréstimos'],
  ['/relatorios', 'Relatórios'],
  ['/beneficiarios', 'Beneficiários'],
  ['/fila', 'Fila de espera'],
  ['/usuarios', 'Usuários'],
  ['/ajuda', 'Ajuda'],
];

describe('routes', () => {
  it.each(PAGES)('renders %s with its title', (path, title) => {
    renderApp(path, { role: 'ROLE_ADMIN' });
    expect(screen.getByRole('heading', { level: 1, name: title })).toBeInTheDocument();
  });

  it('shows Página não encontrada for unknown paths', () => {
    renderApp('/nao-existe', { role: 'ROLE_ADMIN' });
    expect(screen.getByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument();
  });

  it('redirects anonymous users to login', () => {
    renderApp('/fila');
    expect(screen.getByRole('button', { name: 'Acessar Plataforma' })).toBeInTheDocument();
  });
});
