const INTERNAL_ORIGIN = 'http://internal.invalid';
// eslint-disable-next-line no-control-regex -- rejeitar caracteres de controle é o objetivo
const CONTROL_OR_BACKSLASH = /[\u0000-\u001f\u007f\\]/;

// Aceita só caminhos internos; qualquer outro valor volta para a raiz (evita open redirect).
// O parser de URL descarta tab/LF/CR, então "/\t/evil.com" viraria "//evil.com": por isso
// caracteres de controle e barra invertida são recusados e o resultado vem do URL normalizado.
export function safeRedirect(raw: string | null | undefined): string {
  if (!raw || !raw.startsWith('/') || CONTROL_OR_BACKSLASH.test(raw)) return '/';
  let url: URL;
  try {
    url = new URL(raw, INTERNAL_ORIGIN);
  } catch {
    return '/';
  }
  if (url.origin !== INTERNAL_ORIGIN) return '/';
  const path = url.pathname.toLowerCase().replace(/\/+$/, '');
  if (path === '/login') return '/';
  const result = url.pathname + url.search + url.hash;
  // "//x" no início seria lido como URL com protocolo relativo
  return result.startsWith('//') ? '/' : result;
}
