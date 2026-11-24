import { Link } from 'react-router';
import { Card } from '@/components/ui';

export function NotFoundPage() {
  return (
    <Card className="flex flex-col gap-2">
      <h1 className="text-2xl font-bold text-ink">Página não encontrada</h1>
      <p className="text-sm text-muted">O endereço acessado não existe ou foi movido.</p>
      <Link to="/" className="text-sm font-semibold text-primary hover:underline">
        Voltar ao início
      </Link>
    </Card>
  );
}
