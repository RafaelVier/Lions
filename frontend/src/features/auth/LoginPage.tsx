import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { ArrowRight, IdCard } from 'lucide-react';
import { Navigate, useLocation, useSearchParams } from 'react-router';
import { MockBanner } from '@/components/layout/MockBanner';
import { Alert, Button, Card, PasswordField, TextField } from '@/components/ui';
import { useAuth } from '@/features/auth/AuthContext';
import { loginSchema } from '@/features/auth/login.schema';
import { loginErrorMessage } from '@/features/auth/loginErrorMessage';
import { SESSION_END_MESSAGE } from '@/features/auth/messages';
import type { SessionEndReason } from '@/features/auth/session';
import { safeRedirect } from '@/lib/safeRedirect';
import type { LoginRequest } from '@/types/api';

const FORGOT_PASSWORD_HINT = 'Fale com o administrador do clube para redefinir sua senha.';
const REQUEST_ACCESS_HINT = 'Fale com o administrador do clube para liberar seu acesso.';

const LINK_BUTTON =
  'rounded font-semibold text-primary hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary';

export function LoginPage() {
  const { session, login } = useAuth();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const [hint, setHint] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginRequest>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: '', password: '' },
  });

  // Exceção deliberada à regra "gravação só por useAppMutation" (liberada no .oxlintrc.json só
  // para este arquivo): o login não grava dados do sistema, só abre a sessão, e é uma requisição
  // comum que precisa da internet. Nas outras telas, use useAppMutation.
  const mutation = useMutation({ mutationFn: login });

  // Login bem-sucedido e usuário já logado caem aqui: a sessão nova renderiza este redirecionamento
  // (um navigate() no onSuccess disputaria com ele e perderia o destino do parâmetro redirect).
  if (session) return <Navigate to={safeRedirect(searchParams.get('redirect'))} replace />;

  const reason = (location.state as { reason?: SessionEndReason } | null)?.reason;
  const sessionMessage = reason ? SESSION_END_MESSAGE[reason] : undefined;

  const onSubmit = (values: LoginRequest) => {
    if (mutation.isPending) return;
    mutation.mutate(values);
  };

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <MockBanner showCredentials />
      <main className="relative flex flex-1 flex-col items-center justify-center overflow-hidden px-4 py-10">
        <div aria-hidden="true" className="pointer-events-none absolute inset-0">
          <div className="absolute left-1/2 top-1/4 size-96 -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary/15 blur-3xl" />
          <div className="absolute bottom-1/4 left-1/2 size-72 translate-x-12 translate-y-1/2 rounded-full bg-accent/20 blur-3xl" />
        </div>

        <Card className="relative w-full max-w-sm p-8">
          <div className="flex flex-col items-center gap-1 text-center">
            <img src="/logo-lions.png" alt="Lions Clube" className="mb-3 h-16 w-auto" />
            <h1 className="text-lg font-bold text-ink">Banco Ortopédico</h1>
            <p className="text-xs text-muted-fg">Lions Medianeira</p>
          </div>

          <div className="mt-6 flex flex-col gap-4">
            {sessionMessage && <Alert tone="info">{sessionMessage}</Alert>}

            <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
              <TextField
                label="E-MAIL OU MATRÍCULA"
                placeholder="ex: 202488 ou nome@lions.org"
                autoComplete="username"
                icon={<IdCard className="size-4" />}
                error={errors.username?.message}
                {...register('username')}
              />
              <PasswordField
                label="SENHA"
                placeholder="••••••••"
                autoComplete="current-password"
                error={errors.password?.message}
                labelAction={
                  <button
                    type="button"
                    className={`text-[11px] ${LINK_BUTTON}`}
                    onClick={() => setHint(FORGOT_PASSWORD_HINT)}
                  >
                    Esqueci minha senha
                  </button>
                }
                {...register('password')}
              />

              {mutation.isError && <Alert tone="danger">{loginErrorMessage(mutation.error)}</Alert>}

              <Button
                type="submit"
                size="lg"
                fullWidth
                loading={mutation.isPending}
                iconRight={<ArrowRight className="size-4" />}
              >
                Acessar Plataforma
              </Button>
            </form>

            <p className="mt-4 text-center text-xs text-muted-fg">
              Novo voluntário?{' '}
              <button
                type="button"
                className={`text-sm ${LINK_BUTTON}`}
                onClick={() => setHint(REQUEST_ACCESS_HINT)}
              >
                Solicitar acesso
              </button>
            </p>

            {hint && <Alert tone="info">{hint}</Alert>}
          </div>
        </Card>

        <p className="relative mt-5 text-[10px] uppercase tracking-wide text-muted">
          DISTRITO LD-1 • LIONS CLUBE MEDIANEIRA
        </p>
      </main>
    </div>
  );
}
