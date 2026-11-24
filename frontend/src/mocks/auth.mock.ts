import { config } from '@/api/config';
import { ApiError } from '@/api/errors';
import { decodeJwtPayload, getJwtExpMs } from '@/lib/jwt';
import { mockDelay } from '@/mocks/delay';
import type { AuthResponse, LoginRequest, UserRole } from '@/types/api';

export const MOCK_PASSWORD = 'lions2026';

export const MOCK_USERS: ReadonlyArray<{ username: string; email: string; role: UserRole }> = [
  { username: 'admin', email: 'admin@lions.org', role: 'ROLE_ADMIN' },
  { username: 'operador', email: 'operador@lions.org', role: 'ROLE_OPERATOR' },
  { username: 'consulta', email: 'consulta@lions.org', role: 'ROLE_VIEWER' },
];

const UNAUTHENTICATED_TITLE = 'Não autenticado';
const INVALID_CREDENTIALS = 'Usuário ou senha inválidos.';

function base64Url(text: string): string {
  const bytes = new TextEncoder().encode(text);
  let binary = '';
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

// JWT falso (assinatura fixa "mock"); só serve para o front ler o exp.
export function createFakeJwt(
  sub: string,
  role: UserRole,
  ttlSeconds: number,
  nowMs: number = Date.now(),
): string {
  const iat = Math.floor(nowMs / 1000);
  const header = base64Url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = base64Url(JSON.stringify({ sub, role, iat, exp: iat + ttlSeconds }));
  return `${header}.${payload}.mock`;
}

function buildResponse(user: (typeof MOCK_USERS)[number]): AuthResponse {
  return {
    token: createFakeJwt(user.username, user.role, config.mockTokenTtlSeconds),
    type: 'Bearer',
    username: user.username,
    email: user.email,
    role: user.role,
  };
}

export const mockAuth = {
  async login(req: LoginRequest): Promise<AuthResponse> {
    await mockDelay();
    const login = req.username.trim().toLowerCase();
    const user = MOCK_USERS.find((u) => u.username === login || u.email === login);
    if (!user || req.password !== MOCK_PASSWORD) {
      throw new ApiError(401, UNAUTHENTICATED_TITLE, INVALID_CREDENTIALS);
    }
    return buildResponse(user);
  },

  async refresh(token: string): Promise<AuthResponse> {
    await mockDelay();
    const expMs = getJwtExpMs(token);
    const sub = decodeJwtPayload(token)?.sub;
    const user = MOCK_USERS.find((u) => u.username === sub);
    if (expMs === null || expMs <= Date.now() || !user) {
      throw new ApiError(401, UNAUTHENTICATED_TITLE, 'Sessão expirada. Entre novamente.');
    }
    return buildResponse(user);
  },

  async logout(): Promise<void> {
    await mockDelay();
  },
};
