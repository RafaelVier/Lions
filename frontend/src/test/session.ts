import { createFakeJwt, MOCK_USERS } from '@/mocks/auth.mock';
import {
  markActivity,
  resetSessionForTests,
  SESSION_KEY,
  type Session,
} from '@/features/auth/session';
import { USER_ROLE_LABEL, type AuthResponse, type UserRole } from '@/types/api';

// Só para testes: resposta de login no formato do backend, para o usuário de exemplo do perfil.
export function fakeAuthResponse(token: string, role: UserRole = 'ROLE_ADMIN'): AuthResponse {
  const user = MOCK_USERS.find((u) => u.role === role) ?? MOCK_USERS[0];
  return {
    token,
    type: 'Bearer',
    expiresIn: 1800,
    expiresAt: new Date(Date.now() + 1800 * 1000).toISOString(),
    user: {
      id: user.id,
      username: user.username,
      fullName: user.fullName,
      email: user.email,
      role,
      roleDescription: USER_ROLE_LABEL[role],
      active: true,
      lastLoginAt: null,
      createdAt: null,
      updatedAt: null,
    },
  };
}

// Só para testes: grava uma sessão válida por 30 min no storage.
export function storeSession(role: UserRole): Session {
  resetSessionForTests();
  const user = MOCK_USERS.find((u) => u.role === role) ?? MOCK_USERS[0];
  const session: Session = {
    token: createFakeJwt(user.username, role, 30 * 60),
    username: user.username,
    email: user.email,
    role,
  };
  // atividade antes da sessão: sem ela o prazo de inatividade encerraria a sessão nova
  markActivity();
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}
