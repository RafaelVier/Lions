import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router';
import { useAuth } from '@/features/auth/AuthContext';

export function RequireAuth({ children }: { children: ReactNode }) {
  const { session, endReason } = useAuth();
  const { pathname, search } = useLocation();
  if (session === null) {
    // Depois de "Sair" não guarda a página: o próximo usuário não herda o destino de quem saiu.
    const to =
      endReason === 'logout'
        ? '/login'
        : '/login?redirect=' + encodeURIComponent(pathname + search);
    return <Navigate replace to={to} state={{ reason: endReason }} />;
  }
  return <>{children}</>;
}
