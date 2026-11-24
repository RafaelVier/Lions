export const NETWORK_ERROR_MESSAGE = 'Não foi possível conectar ao servidor.';
export const GENERIC_ERROR_MESSAGE = 'Ocorreu um erro inesperado. Tente novamente.';

const GATEWAY_STATUSES = [502, 503, 504];

export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly detail: string;

  constructor(status: number, title: string, detail: string) {
    super(detail);
    this.name = 'ApiError';
    this.status = status;
    this.title = title;
    this.detail = detail;
  }
}

export function isApiError(e: unknown): e is ApiError {
  return e instanceof ApiError;
}

export async function toApiError(res: Response): Promise<ApiError> {
  if (GATEWAY_STATUSES.includes(res.status)) {
    return new ApiError(res.status, 'Servidor indisponível', NETWORK_ERROR_MESSAGE);
  }
  try {
    const body: unknown = await res.json();
    if (body && typeof body === 'object' && 'title' in body && typeof body.title === 'string') {
      const detail = 'detail' in body && typeof body.detail === 'string' ? body.detail : undefined;
      return new ApiError(res.status, body.title, detail ?? body.title);
    }
  } catch {
    // corpo vazio ou não-JSON: cai no erro genérico
  }
  return new ApiError(res.status, 'Erro', GENERIC_ERROR_MESSAGE);
}
