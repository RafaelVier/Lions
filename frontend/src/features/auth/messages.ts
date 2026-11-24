import type { SessionEndReason } from '@/features/auth/session';

export const SESSION_END_MESSAGE: Partial<Record<SessionEndReason, string>> = {
  expired: 'Sua sessão expirou. Entre novamente.',
  unauthorized: 'Sua sessão expirou. Entre novamente.',
  inactivity: 'Sua sessão expirou por inatividade. Entre novamente.',
};
