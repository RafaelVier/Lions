import { isApiError } from '@/api/errors';
import { getJwtExpMs } from '@/lib/jwt';
import { USER_ROLE_LABEL, type AuthResponse, type UserRole } from '@/types/api';

// Sessão em TypeScript puro; o AuthProvider expõe isto ao React.

export interface Session {
  token: string;
  username: string;
  email: string;
  role: UserRole;
}

export type SessionEndReason = 'logout' | 'expired' | 'inactivity' | 'unauthorized';

type Listener = (session: Session | null, reason: SessionEndReason | null) => void;

export interface SessionManagerOptions {
  refresh(token: string): Promise<AuthResponse>;
  // RN57: registra no servidor o encerramento por inatividade decidido nesta aba
  // (POST /auth/logout). Falhas são ignoradas: o encerramento local já valeu.
  revoke?(token: string): Promise<void>;
  now?: () => number;
}

export const SESSION_KEY = 'lions.session';
// Fica no localStorage para valer entre abas.
export const ACTIVITY_KEY = 'lions.lastActivity';
// RN57: a sessão expira após 30 min de inatividade.
export const INACTIVITY_LIMIT_MS = 30 * 60_000;
// Antecedência da renovação em relação ao exp do token.
export const REFRESH_LEAD_MS = 2 * 60_000;
// Intervalo mínimo entre renovações: com o relógio do cliente adiantado, todo token novo já
// chegaria dentro da antecedência e a renovação viraria um laço.
export const MIN_REFRESH_INTERVAL_MS = 60_000;

const VALID_ROLES: readonly string[] = Object.keys(USER_ROLE_LABEL);

// Estado do módulo; resetSessionForTests() zera tudo.
let current: Session | null = null;
let loaded = false;
let refreshInFlight: Promise<void> | null = null;
let lastRefreshSettledAt = 0;
// Por que a sessão guardada foi descartada ao abrir o app (F5 depois de 30 min parado).
let loadEndReason: SessionEndReason | null = null;
// Token dessa sessão descartada: o gerenciador registra o encerramento no servidor ao começar.
let revokeOnStart: string | null = null;
const listeners = new Set<Listener>();
const runningManagers = new Set<() => void>();

function isUserRole(value: unknown): value is UserRole {
  return typeof value === 'string' && VALID_ROLES.includes(value);
}

// Mesmas regras para o F5, o login e o refresh: textos, perfil conhecido e exp no futuro.
function toValidSession(data: unknown): Session | null {
  if (data === null || typeof data !== 'object') return null;
  const { token, username, email, role } = data as Record<string, unknown>;
  if (
    typeof token !== 'string' ||
    typeof username !== 'string' ||
    typeof email !== 'string' ||
    !isUserRole(role)
  ) {
    return null;
  }
  const exp = getJwtExpMs(token);
  if (exp === null || exp <= Date.now()) return null;
  return { token, username, email, role };
}

function parseSession(raw: string): Session | null {
  try {
    return toValidSession(JSON.parse(raw));
  } catch {
    return null;
  }
}

// Sem atividade registrada (chave perdida) a sessão conta como ativa.
function isStale(now: number): boolean {
  const last = getLastActivity();
  return last > 0 && now - last >= INACTIVITY_LIMIT_MS;
}

// Sessão inválida, vencida ou parada há 30 min é descartada do storage.
function loadStoredSession(): Session | null {
  const raw = localStorage.getItem(SESSION_KEY);
  if (raw === null) return null;
  const session = parseSession(raw);
  if (session !== null && isStale(Date.now())) {
    // RN57: a sessão já acabou por inatividade. Descartar aqui, antes do primeiro render,
    // impede que a tela protegida e o cache persistido apareçam por um instante.
    localStorage.removeItem(SESSION_KEY);
    loadEndReason = 'inactivity';
    revokeOnStart = session.token;
    return null;
  }
  if (session === null) localStorage.removeItem(SESSION_KEY);
  return session;
}

function notify(session: Session | null, reason: SessionEndReason | null): void {
  // cópia: um listener pode cancelar a inscrição durante a notificação
  for (const listener of [...listeners]) listener(session, reason);
}

export function sessionFromAuth(res: AuthResponse): Session {
  return { token: res.token, username: res.username, email: res.email, role: res.role };
}

export function getSession(): Session | null {
  if (!loaded) {
    current = loadStoredSession();
    loaded = true;
  }
  return current;
}

// Motivo do descarte da sessão guardada ao abrir o app ('inactivity'); null se não houve.
export function getLoadEndReason(): SessionEndReason | null {
  getSession();
  return loadEndReason;
}

// Lança erro se a sessão não passar nas regras do F5 (perfil, exp): nada inválido é guardado.
export function setSession(session: Session): void {
  const valid = toValidSession(session);
  if (valid === null) throw new Error('Sessão inválida.');
  localStorage.setItem(SESSION_KEY, JSON.stringify(valid));
  current = valid;
  loaded = true;
  loadEndReason = null;
  notify(valid, null);
}

export function endSession(reason: SessionEndReason): void {
  if (getSession() === null) return;
  localStorage.removeItem(SESSION_KEY);
  current = null;
  notify(null, reason);
}

export function subscribe(listener: Listener): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function markActivity(now: number = Date.now()): void {
  localStorage.setItem(ACTIVITY_KEY, String(now));
}

export function getLastActivity(): number {
  const value = Number(localStorage.getItem(ACTIVITY_KEY));
  return Number.isFinite(value) && value > 0 ? value : 0;
}

export function hasRole(session: Session | null, ...roles: UserRole[]): boolean {
  return session !== null && roles.includes(session.role);
}

// Uma renovação por vez: quem chegar durante ela recebe a mesma promessa.
function refreshOnce(
  token: string,
  refresh: SessionManagerOptions['refresh'],
  now: () => number,
  onFailure: () => void,
): Promise<void> {
  if (refreshInFlight) return refreshInFlight;
  // se a sessão mudou no meio do caminho (logout, outra aba), o resultado é ignorado
  const stillCurrent = () => getSession()?.token === token;
  // Invariante: refresh(token) roda de forma síncrona, ANTES de refreshInFlight apontar para
  // esta promessa; assim um waitForRefresh() logo no início do refresh não espera por si mesmo.
  // As chamadas HTTP do refresh passam { token } e por isso pulam o beforeRequest (http.ts).
  const flight: Promise<void> = new Promise<AuthResponse>((resolve) => resolve(refresh(token)))
    .then(
      (res) => {
        // antes do setSession: o reagendamento disparado por ele já respeita o intervalo
        lastRefreshSettledAt = now();
        if (!stillCurrent()) return;
        // resposta fora do contrato (perfil desconhecido, exp vencido) conta como falha
        const session = toValidSession(res);
        if (session === null) onFailure();
        else setSession(session);
      },
      (error: unknown) => {
        lastRefreshSettledAt = now();
        if (!stillCurrent()) return;
        if (isApiError(error) && error.status === 401) endSession('unauthorized');
        else onFailure();
      },
    )
    .finally(() => {
      if (refreshInFlight === flight) refreshInFlight = null;
    });
  refreshInFlight = flight;
  return flight;
}

export function waitForRefresh(): Promise<void> {
  return refreshInFlight ?? Promise.resolve();
}

// Temporizador que guarda só o último agendamento.
function createTimer(now: () => number) {
  let id: ReturnType<typeof setTimeout> | undefined;
  const clear = (): void => {
    if (id !== undefined) clearTimeout(id);
    id = undefined;
  };
  const at = (timeMs: number, task: () => void): void => {
    clear();
    id = setTimeout(
      () => {
        id = undefined;
        task();
      },
      Math.max(0, timeMs - now()),
    );
  };
  return { clear, at };
}

export function startSessionManager(options: SessionManagerOptions): () => void {
  const now = options.now ?? (() => Date.now());
  const refreshTimer = createTimer(now);
  const inactivityTimer = createTimer(now);
  let stopped = false;

  // RN57: vale a última atividade de qualquer aba (ACTIVITY_KEY é compartilhada).
  function isIdle(): boolean {
    return now() - getLastActivity() >= INACTIVITY_LIMIT_MS;
  }

  // RN57: o encerramento é registrado no servidor; falha ali não muda nada aqui.
  function revoke(token: string): void {
    const { revoke: revokeToken } = options;
    if (!revokeToken) return;
    Promise.resolve()
      .then(() => revokeToken(token))
      .catch(() => {});
  }

  // Só a aba que decide o fim por inatividade o registra; as outras recebem pelo evento storage.
  function endByInactivity(): void {
    const token = getSession()?.token;
    endSession('inactivity');
    if (token !== undefined) revoke(token);
  }

  // Prazo de inatividade independente do exp do token.
  function watchInactivity(): void {
    inactivityTimer.at(getLastActivity() + INACTIVITY_LIMIT_MS, () => {
      // a atividade pode ter avançado desde o agendamento
      if (isIdle()) endByInactivity();
      else watchInactivity();
    });
  }

  function check(token: string, exp: number): void {
    // temporizador atrasado (suspensão, aba em segundo plano): a inatividade vence e não renova
    if (isIdle()) {
      endByInactivity();
      return;
    }
    void refreshOnce(token, options.refresh, now, () => {
      if (stopped) return;
      // Falha de rede ou 5xx: tenta de novo após o intervalo mínimo enquanto o token valer (a nova
      // verificação também confere a inatividade). Sem tempo para outra tentativa, vale até o exp.
      const retryAt = lastRefreshSettledAt + MIN_REFRESH_INTERVAL_MS;
      if (retryAt < exp) refreshTimer.at(retryAt, () => check(token, exp));
      else refreshTimer.at(exp, () => endSession('expired'));
    });
  }

  // Roda dentro da notificação: só agenda, nunca encerra a sessão aqui.
  function schedule(): void {
    refreshTimer.clear();
    inactivityTimer.clear();
    const session = getSession();
    if (session === null) return;
    watchInactivity();
    const exp = getJwtExpMs(session.token);
    if (exp === null) return;
    const checkAt = Math.max(exp - REFRESH_LEAD_MS, lastRefreshSettledAt + MIN_REFRESH_INTERVAL_MS);
    refreshTimer.at(checkAt, () => check(session.token, exp));
  }

  // Sair ou renovar em outra aba chega por aqui.
  function onStorage(event: StorageEvent): void {
    if (event.key !== SESSION_KEY) return;
    if (event.newValue === null) {
      const hadSession = current !== null;
      current = null;
      loaded = true;
      // 30 min sem atividade em nenhuma aba: foi a inatividade (a outra aba só chegou antes)
      if (hadSession) notify(null, isIdle() ? 'inactivity' : 'logout');
    } else {
      loaded = false;
      notify(getSession(), null);
    }
  }

  // Sem atividade registrada (chave perdida), um F5 não pode derrubar a sessão na hora.
  if (getSession() !== null && getLastActivity() === 0) markActivity(now());

  // F5 depois de 30 min parado: registra o fim da sessão descartada ao abrir (uma vez só).
  if (revokeOnStart !== null) {
    const token = revokeOnStart;
    revokeOnStart = null;
    revoke(token);
  }

  const unsubscribe = subscribe(schedule);
  window.addEventListener('storage', onStorage);
  schedule();

  const stop = () => {
    if (stopped) return;
    stopped = true;
    refreshTimer.clear();
    inactivityTimer.clear();
    unsubscribe();
    window.removeEventListener('storage', onStorage);
    runningManagers.delete(stop);
  };
  runningManagers.add(stop);
  return stop;
}

export function resetSessionForTests(): void {
  for (const stop of [...runningManagers]) stop();
  listeners.clear();
  current = null;
  loaded = false;
  refreshInFlight = null;
  lastRefreshSettledAt = 0;
  loadEndReason = null;
  revokeOnStart = null;
}
