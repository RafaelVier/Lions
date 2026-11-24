import { config } from '@/api/config';
import { ApiError } from '@/api/errors';
import { decodeJwtPayload, getJwtExpMs } from '@/lib/jwt';
import { mockDelay } from '@/mocks/delay';
import { USER_ROLE_LABEL, type AuthResponse, type LoginRequest, type UserRole } from '@/types/api';

export const MOCK_PASSWORD = 'lions2026';

export const MOCK_USERS: ReadonlyArray<{
  id: number;
  username: string;
  fullName: string;
  email: string;
  role: UserRole;
}> = [
  {
    id: 1,
    username: 'admin',
    fullName: 'Administrador',
    email: 'admin@lions.org',
    role: 'ROLE_ADMIN',
  },
  {
    id: 2,
    username: 'operador',
    fullName: 'Operador',
    email: 'operador@lions.org',
    role: 'ROLE_OPERATOR',
  },
  {
    id: 3,
    username: 'consulta',
    fullName: 'Consulta',
    email: 'consulta@lions.org',
    role: 'ROLE_VIEWER',
  },
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

// Mesmo formato do AuthResponse do backend (usuário aninhado em `user`).
function buildResponse(user: (typeof MOCK_USERS)[number]): AuthResponse {
  const ttl = config.mockTokenTtlSeconds;
  return {
    token: createFakeJwt(user.username, user.role, ttl),
    type: 'Bearer',
    expiresIn: ttl,
    expiresAt: new Date(Date.now() + ttl * 1000).toISOString(),
    user: {
      id: user.id,
      username: user.username,
      fullName: user.fullName,
      email: user.email,
      role: user.role,
      roleDescription: USER_ROLE_LABEL[user.role],
      active: true,
      lastLoginAt: null,
      createdAt: null,
      updatedAt: null,
    },
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
