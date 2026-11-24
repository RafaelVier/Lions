import { config } from '@/api/config';

// Faixa de aviso do modo demonstração; some quando o front fala com a API real.
export function MockBanner({ showCredentials = false }: { showCredentials?: boolean }) {
  if (!config.useMocks) return null;
  return (
    <div className="border-b border-warning/40 bg-warning/10 px-4 py-2 text-center text-xs text-ink">
      <p className="font-semibold text-warning-fg">Modo demonstração — dados de exemplo</p>
      {showCredentials && <p>Entre com admin, operador ou consulta e a senha lions2026.</p>}
    </div>
  );
}
