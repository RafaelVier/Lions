interface ImportMetaEnv {
  readonly VITE_API_URL: string | undefined;
  readonly VITE_USE_MOCKS: string | undefined;
  readonly VITE_BACKEND_URL: string | undefined;
  readonly VITE_MOCK_TOKEN_TTL_SECONDS: string | undefined;
  readonly VITE_MOCK_DELAY_MS: string | undefined;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
