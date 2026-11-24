import {
  useMutation,
  type UseMutationOptions,
  type UseMutationResult,
} from '@tanstack/react-query';
import { ApiError } from '@/api/errors';

export const OFFLINE_MESSAGE =
  'Sem conexão — mostrando os últimos dados carregados. Alterações ficam desativadas até a internet voltar.';

// Ponto único de gravação: a fila offline (RN60) entra aqui quando for implementada.
export function useAppMutation<TData, TVariables = void>(
  options: UseMutationOptions<TData, ApiError, TVariables>,
): UseMutationResult<TData, ApiError, TVariables> {
  const { mutationFn } = options;
  return useMutation<TData, ApiError, TVariables>({
    ...options,
    mutationFn: mutationFn
      ? (variables, context) => {
          if (navigator.onLine === false) {
            return Promise.reject(new ApiError(0, 'Sem conexão', OFFLINE_MESSAGE));
          }
          return mutationFn(variables, context);
        }
      : undefined,
  });
}
