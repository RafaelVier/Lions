import { render, type RenderResult } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { AppProviders } from '@/app/providers';
import { createAppQueryClient } from '@/app/queryClient';
import { routes } from '@/app/routes';
import { resetSessionForTests } from '@/features/auth/session';
import { storeSession } from '@/test/session';
import type { UserRole } from '@/types/api';

// Renderiza o app real (rotas + providers) numa rota inicial, opcionalmente já logado.
export function renderApp(
  path: string,
  options: { role?: UserRole } = {},
): RenderResult & { router: ReturnType<typeof createMemoryRouter> } {
  // sem perfil: zera o estado em memória do módulo de sessão (o storage já é limpo a cada teste)
  if (options.role) storeSession(options.role);
  else resetSessionForTests();
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  const result = render(
    <AppProviders queryClient={createAppQueryClient()}>
      <RouterProvider router={router} />
    </AppProviders>,
  );
  return { ...result, router };
}
