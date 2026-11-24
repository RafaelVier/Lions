import { Loader2 } from 'lucide-react';

export function Spinner({ label = 'Carregando' }: { label?: string }) {
  return (
    <span role="status" aria-label={label} className="inline-flex">
      <Loader2 aria-hidden="true" className="size-4 animate-spin" />
    </span>
  );
}
