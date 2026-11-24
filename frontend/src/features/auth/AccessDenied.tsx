import { Card } from '@/components/ui';

export function AccessDenied() {
  return (
    <Card role="alert" className="flex flex-col gap-2">
      <h1 className="text-2xl font-bold text-ink">Acesso negado</h1>
      <p className="text-sm text-muted">Seu perfil não tem permissão para acessar esta página.</p>
    </Card>
  );
}
