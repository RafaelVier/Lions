import { config } from '@/api/config';
import { http } from '@/api/http';
import { mockAuth } from '@/mocks/auth.mock';
import type { AuthResponse, LoginRequest } from '@/types/api';

// Escolhe dados de exemplo ou API real a cada chamada, conforme config.useMocks.
export const authService = {
  login(req: LoginRequest): Promise<AuthResponse> {
    if (config.useMocks) return mockAuth.login(req);
    return http.post<AuthResponse>('/auth/login', req, { auth: false });
  },

  refresh(token: string): Promise<AuthResponse> {
    if (config.useMocks) return mockAuth.refresh(token);
    return http.post<AuthResponse>('/auth/refresh', undefined, { token });
  },

  // token explícito: o logout roda depois de a sessão local ser encerrada
  logout(token?: string): Promise<void> {
    if (config.useMocks) return mockAuth.logout();
    return http.post<void>('/auth/logout', undefined, token ? { token } : undefined);
  },
};
