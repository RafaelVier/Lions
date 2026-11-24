/* eslint-disable react-refresh/only-export-components -- provider e hook ficam juntos por design */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { configureHttp, resetHttp } from '@/api/http';
import {
  endSession,
  getLoadEndReason,
  getSession,
  hasRole as sessionHasRole,
  markActivity,
  sessionFromAuth,
  setSession,
  startSessionManager,
  subscribe,
  waitForRefresh,
  type Session,
  type SessionEndReason,
} from '@/features/auth/session';
import { authService } from '@/services/auth.service';
import type { LoginRequest, UserRole } from '@/types/api';

export interface AuthContextValue {
  session: Session | null;
  endReason: SessionEndReason | null;
  login(req: LoginRequest): Promise<Session>;
  logout(): Promise<void>;
  hasRole(...roles: UserRole[]): boolean;
}

interface AuthState {
  session: Session | null;
  endReason: SessionEndReason | null;
}

// Intervalo mínimo entre registros de atividade vindos dos eventos da janela.
const ACTIVITY_THROTTLE_MS = 15_000;
const ACTIVITY_EVENTS = ['pointerdown', 'keydown', 'touchstart', 'scroll'] as const;

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  // Síncrono: o F5 com sessão válida não passa pelo login. Se a sessão guardada foi descartada
  // por inatividade (F5 depois de 30 min parado), o Login mostra o aviso.
  const [state, setState] = useState<AuthState>(() => {
    const session = getSession();
    return { session, endReason: session === null ? getLoadEndReason() : null };
  });

  useEffect(() => {
    configureHttp({
      getToken: () => getSession()?.token ?? null,
      onUnauthorized: () => endSession('unauthorized'),
      beforeRequest: waitForRefresh,
    });
    const stopManager = startSessionManager({
      refresh: authService.refresh,
      // RN57: o fim por inatividade é registrado no servidor (erros ignorados no session.ts)
      revoke: (token) => authService.logout(token),
    });
    const unsubscribe = subscribe((session, endReason) => setState({ session, endReason }));

    let lastMark = 0;
    const onActivity = () => {
      const now = Date.now();
      if (now - lastMark < ACTIVITY_THROTTLE_MS) return;
      lastMark = now;
      markActivity(now);
    };
    for (const name of ACTIVITY_EVENTS)
      window.addEventListener(name, onActivity, { passive: true });

    return () => {
      for (const name of ACTIVITY_EVENTS) window.removeEventListener(name, onActivity);
      unsubscribe();
      stopManager();
      resetHttp();
    };
  }, []);

  const login = useCallback(async (req: LoginRequest): Promise<Session> => {
    const res = await authService.login(req);
    const session = sessionFromAuth(res);
    // atividade antes da sessão: um prazo de inatividade antigo encerraria a sessão nova
    markActivity();
    // valida como no F5 (perfil, exp); resposta inválida lança erro e a tela mostra a mensagem genérica
    setSession(session);
    return session;
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    const token = getSession()?.token;
    // encerra antes: um 401 do servidor no logout não pode virar "sessão expirada"
    endSession('logout');
    try {
      await authService.logout(token);
    } catch {
      // o logout local vale mesmo se o servidor falhar
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      session: state.session,
      endReason: state.endReason,
      login,
      logout,
      hasRole: (...roles: UserRole[]) => sessionHasRole(state.session, ...roles),
    }),
    [state, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (ctx === null) throw new Error('useAuth precisa estar dentro de <AuthProvider>');
  return ctx;
}
