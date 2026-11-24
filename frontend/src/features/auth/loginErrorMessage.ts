import { GENERIC_ERROR_MESSAGE, isApiError, NETWORK_ERROR_MESSAGE } from '@/api/errors';

export function loginErrorMessage(error: unknown): string {
  if (!isApiError(error)) return GENERIC_ERROR_MESSAGE;
  if (error.status === 401) return 'Usuário ou senha inválidos.';
  if (error.status === 0) return NETWORK_ERROR_MESSAGE;
  return error.detail;
}
