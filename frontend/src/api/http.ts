import { REQUEST_TIMEOUT_MS, config } from '@/api/config';
import { ApiError, GENERIC_ERROR_MESSAGE, NETWORK_ERROR_MESSAGE, toApiError } from '@/api/errors';

export interface HttpHooks {
  getToken(): string | null;
  onUnauthorized(): void;
  beforeRequest(): Promise<void>;
}

export interface RequestOptions {
  auth?: boolean;
  token?: string;
  signal?: AbortSignal;
  timeoutMs?: number;
}

const defaultHooks: HttpHooks = {
  getToken: () => null,
  onUnauthorized: () => {},
  beforeRequest: () => Promise.resolve(),
};

let hooks: HttpHooks = { ...defaultHooks };

export function configureHttp(partial: Partial<HttpHooks>): void {
  hooks = { ...hooks, ...partial };
}

export function resetHttp(): void {
  hooks = { ...defaultHooks };
}

// AbortSignal.any só existe a partir do Safari 17.4 / Chrome 116; antes disso, liga os sinais à mão.
function anySignal(signals: AbortSignal[]): AbortSignal {
  if (typeof AbortSignal.any === 'function') return AbortSignal.any(signals);
  const controller = new AbortController();
  for (const signal of signals) {
    if (signal.aborted) {
      controller.abort(signal.reason);
      break;
    }
    signal.addEventListener('abort', () => controller.abort(signal.reason), { once: true });
  }
  return controller.signal;
}

async function request<T>(
  method: string,
  path: string,
  body: unknown,
  options: RequestOptions = {},
): Promise<T> {
  const authenticated = options.auth !== false;
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  if (authenticated) {
    // Token explícito traz a própria credencial (ex.: o próprio refresh) e não espera a
    // renovação em andamento; senão o refresh esperaria por si mesmo.
    if (options.token === undefined) await hooks.beforeRequest();
    const token = options.token ?? hooks.getToken();
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  const timeout = AbortSignal.timeout(options.timeoutMs ?? REQUEST_TIMEOUT_MS);
  const signal = options.signal ? anySignal([options.signal, timeout]) : timeout;

  let res: Response;
  try {
    res = await fetch(config.apiUrl + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (e) {
    // abortado pelo chamador: não é falha de rede
    if (options.signal?.aborted) throw e;
    throw new ApiError(0, 'Sem conexão', NETWORK_ERROR_MESSAGE);
  }

  if (!res.ok) {
    // Só o 401 da sessão atual encerra a sessão. Com token explícito (refresh, logout) o token
    // pode ser antigo — outra aba já renovou — e quem chamou trata o erro (session.ts).
    if (res.status === 401 && authenticated && options.token === undefined) {
      hooks.onUnauthorized();
    }
    throw await toApiError(res);
  }

  if (res.status === 204) return undefined as T;
  const text = await res.text();
  if (text === '') return undefined as T;
  try {
    return JSON.parse(text) as T;
  } catch {
    // 2xx sem JSON (ex.: NGINX devolvendo o index.html do front para /api)
    throw new ApiError(res.status, 'Erro', GENERIC_ERROR_MESSAGE);
  }
}

export const http = {
  get: <T>(path: string, options?: RequestOptions) => request<T>('GET', path, undefined, options),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('POST', path, body, options),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('PUT', path, body, options),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('PATCH', path, body, options),
  delete: <T>(path: string, options?: RequestOptions) =>
    request<T>('DELETE', path, undefined, options),
};
