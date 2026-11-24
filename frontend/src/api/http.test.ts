import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { GENERIC_ERROR_MESSAGE, NETWORK_ERROR_MESSAGE, isApiError } from '@/api/errors';
import { configureHttp, http, resetHttp } from '@/api/http';

const fetchMock = vi.fn();

function jsonResponse(body: unknown, status = 200, contentType = 'application/json') {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': contentType } });
}

function headersOf(call = 0): Headers {
  return new Headers((fetchMock.mock.calls[call][1] as RequestInit).headers);
}

async function catchError(promise: Promise<unknown>): Promise<unknown> {
  try {
    await promise;
  } catch (e) {
    return e;
  }
  throw new Error('expected rejection');
}

// fetch que só rejeita quando o signal é abortado
function hangUntilAborted(_url: string, init: RequestInit) {
  return new Promise((_resolve, reject) => {
    if (init.signal?.aborted) reject(init.signal.reason);
    init.signal?.addEventListener('abort', () => reject(init.signal?.reason));
  });
}

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
  resetHttp();
});

describe('http', () => {
  it('prefixes the path with config.apiUrl', async () => {
    fetchMock.mockResolvedValue(jsonResponse({}));
    await http.get('/equipamentos');
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/equipamentos');
  });

  it('sends the bearer token from getToken', async () => {
    configureHttp({ getToken: () => 't1' });
    fetchMock.mockResolvedValue(jsonResponse({}));
    await http.get('/x');
    expect(headersOf().get('Authorization')).toBe('Bearer t1');
  });

  it('omits the token when auth is false', async () => {
    const onUnauthorized = vi.fn();
    configureHttp({ getToken: () => 't1', onUnauthorized });
    fetchMock.mockResolvedValue(jsonResponse({ title: 'Não autorizado', status: 401 }, 401));
    await catchError(http.post('/auth/login', {}, { auth: false }));
    expect(headersOf().has('Authorization')).toBe(false);
    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it('prefers options.token over getToken', async () => {
    configureHttp({ getToken: () => 't1' });
    fetchMock.mockResolvedValue(jsonResponse({}));
    await http.get('/x', { token: 't2' });
    expect(headersOf().get('Authorization')).toBe('Bearer t2');
  });

  it('awaits beforeRequest before calling fetch', async () => {
    let release: () => void = () => {};
    configureHttp({
      beforeRequest: () =>
        new Promise<void>((r) => {
          release = r;
        }),
    });
    fetchMock.mockResolvedValue(jsonResponse({}));
    const pending = http.get('/x');
    await Promise.resolve();
    expect(fetchMock).not.toHaveBeenCalled();
    release();
    await pending;
    expect(fetchMock).toHaveBeenCalledOnce();
  });

  it('does not wait for beforeRequest when the caller passes its own token', async () => {
    // é o caso do próprio refresh: esperar a renovação em andamento seria esperar a si mesmo
    configureHttp({ getToken: () => 'old', beforeRequest: () => new Promise<void>(() => {}) });
    fetchMock.mockResolvedValue(jsonResponse({}));
    await http.post('/x', undefined, { token: 't' });
    expect(headersOf().get('Authorization')).toBe('Bearer t');
  });

  it('sends JSON content type only when there is a body', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(jsonResponse({})));
    await http.post('/x', { a: 1 });
    await http.get('/x');
    expect(headersOf(0).get('Content-Type')).toBe('application/json');
    expect(headersOf(0).get('Accept')).toBe('application/json');
    expect((fetchMock.mock.calls[0][1] as RequestInit).body).toBe('{"a":1}');
    expect(headersOf(1).has('Content-Type')).toBe(false);
  });

  it('returns parsed JSON on 200 and undefined on 204', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ ok: true }));
    expect(await http.get('/x')).toEqual({ ok: true });
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    expect(await http.delete('/x')).toBeUndefined();
  });

  it('throws ApiError from a ProblemDetail body', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        { title: 'Regra', status: 422, detail: 'Beneficiário bloqueado' },
        422,
        'application/problem+json',
      ),
    );
    const e = await catchError(http.post('/x', {}));
    expect(isApiError(e)).toBe(true);
    expect(e).toMatchObject({ status: 422, title: 'Regra', detail: 'Beneficiário bloqueado' });
  });

  it('maps gateway errors with HTML body to the network message', async () => {
    fetchMock.mockResolvedValue(
      new Response('<html>Bad Gateway</html>', {
        status: 502,
        headers: { 'Content-Type': 'text/html' },
      }),
    );
    const e = await catchError(http.get('/x'));
    expect(e).toMatchObject({ status: 502, detail: NETWORK_ERROR_MESSAGE });
  });

  it('maps non-JSON 500 to the generic message', async () => {
    fetchMock.mockResolvedValue(
      new Response('boom', { status: 500, headers: { 'Content-Type': 'text/plain' } }),
    );
    const e = await catchError(http.get('/x'));
    expect(e).toMatchObject({ status: 500, detail: GENERIC_ERROR_MESSAGE });
  });

  it('calls onUnauthorized on 401 for authenticated requests', async () => {
    const onUnauthorized = vi.fn();
    configureHttp({ onUnauthorized });
    fetchMock.mockResolvedValue(jsonResponse({ title: 'Não autorizado', status: 401 }, 401));
    const e = await catchError(http.get('/x'));
    expect(onUnauthorized).toHaveBeenCalledOnce();
    expect(e).toMatchObject({ status: 401 });
  });

  it('does not call onUnauthorized on 401 when the caller passes its own token', async () => {
    // refresh com token antigo (outra aba já renovou): o 401 não pode derrubar a sessão atual
    const onUnauthorized = vi.fn();
    configureHttp({ getToken: () => 'current', onUnauthorized });
    fetchMock.mockResolvedValue(jsonResponse({ title: 'Não autenticado', status: 401 }, 401));
    const e = await catchError(http.post('/auth/refresh', undefined, { token: 'stale' }));
    expect(onUnauthorized).not.toHaveBeenCalled();
    expect(isApiError(e)).toBe(true);
    expect(e).toMatchObject({ status: 401 });
  });

  it('maps a non-JSON 2xx body to the generic ApiError', async () => {
    // ex.: NGINX servindo o index.html do front no lugar da API
    fetchMock.mockResolvedValue(
      new Response('<!doctype html><html></html>', {
        status: 200,
        headers: { 'Content-Type': 'text/html' },
      }),
    );
    const e = await catchError(http.get('/x'));
    expect(isApiError(e)).toBe(true);
    expect(e).toMatchObject({ status: 200, title: 'Erro', detail: GENERIC_ERROR_MESSAGE });
  });

  it('maps a rejected fetch to status 0 with the network message', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'));
    const e = await catchError(http.get('/x'));
    expect(e).toMatchObject({ status: 0, title: 'Sem conexão', detail: NETWORK_ERROR_MESSAGE });
  });

  it('maps a timeout to status 0', async () => {
    fetchMock.mockImplementation(hangUntilAborted);
    const e = await catchError(http.get('/x', { timeoutMs: 10 }));
    expect(e).toMatchObject({ status: 0, detail: NETWORK_ERROR_MESSAGE });
  });

  it('rethrows the original error when the caller aborted', async () => {
    const controller = new AbortController();
    fetchMock.mockImplementation(hangUntilAborted);
    const pending = catchError(http.get('/x', { signal: controller.signal }));
    controller.abort();
    const e = await pending;
    expect(isApiError(e)).toBe(false);
  });

  describe('without AbortSignal.any (Safari < 17.4, Chrome < 116)', () => {
    const descriptor = Object.getOwnPropertyDescriptor(AbortSignal, 'any');

    beforeEach(() => {
      Object.defineProperty(AbortSignal, 'any', { value: undefined, configurable: true });
    });

    afterEach(() => {
      if (descriptor) Object.defineProperty(AbortSignal, 'any', descriptor);
    });

    it('still times out when the caller also passes a signal', async () => {
      fetchMock.mockImplementation(hangUntilAborted);
      const controller = new AbortController();
      const e = await catchError(http.get('/x', { signal: controller.signal, timeoutMs: 10 }));
      expect(e).toMatchObject({ status: 0, detail: NETWORK_ERROR_MESSAGE });
    });

    it('still aborts the request when the caller aborts', async () => {
      fetchMock.mockImplementation(hangUntilAborted);
      const controller = new AbortController();
      const reason = new Error('cancelado');
      const pending = catchError(http.get('/x', { signal: controller.signal }));
      await Promise.resolve();
      controller.abort(reason);
      expect(await pending).toBe(reason);
    });
  });
});
