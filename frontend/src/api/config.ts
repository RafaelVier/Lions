export interface AppConfig {
  apiUrl: string;
  useMocks: boolean;
  mockTokenTtlSeconds: number;
  mockDelayMs: number;
}

const DEFAULT_API_URL = '/api/v1';
const DEFAULT_MOCK_TOKEN_TTL_SECONDS = 1800;
const DEFAULT_MOCK_DELAY_MS = 400;

function parseInteger(value: string | undefined, min: number, fallback: number): number {
  if (value === undefined || !/^\d+$/.test(value.trim())) return fallback;
  const n = Number(value);
  return n >= min ? n : fallback;
}

export function readConfig(env: Partial<Record<string, string | undefined>>): AppConfig {
  const rawUrl = env.VITE_API_URL?.trim();
  return {
    apiUrl: rawUrl ? rawUrl.replace(/\/+$/, '') : DEFAULT_API_URL,
    useMocks: env.VITE_USE_MOCKS !== 'false',
    mockTokenTtlSeconds: parseInteger(
      env.VITE_MOCK_TOKEN_TTL_SECONDS,
      1,
      DEFAULT_MOCK_TOKEN_TTL_SECONDS,
    ),
    mockDelayMs: parseInteger(env.VITE_MOCK_DELAY_MS, 0, DEFAULT_MOCK_DELAY_MS),
  };
}

// Objeto comum (não congelado): testes podem sobrescrever campos.
export const config: AppConfig = readConfig(import.meta.env);

export const REQUEST_TIMEOUT_MS = 15_000;
