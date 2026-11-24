import { Bell, Menu, Moon, Search, Sun } from 'lucide-react';
import { useAuth } from '@/features/auth/AuthContext';
import { useTheme } from '@/hooks/useTheme';

const ICON_BUTTON =
  'flex size-9 items-center justify-center rounded-lg text-muted-fg transition hover:bg-field hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary';

export function Topbar({ onOpenMenu }: { onOpenMenu: () => void }) {
  const { session } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const initial = (session?.username ?? '?').charAt(0).toUpperCase();

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-line bg-surface px-4 lg:px-8">
      <button
        type="button"
        aria-label="Abrir menu"
        onClick={onOpenMenu}
        className={`${ICON_BUTTON} lg:hidden`}
      >
        <Menu aria-hidden="true" className="size-5" />
      </button>

      {/* busca sem ação por enquanto: campo no desktop, só ícone abaixo de lg */}
      <div className="relative hidden max-w-md flex-1 lg:block">
        <Search
          aria-hidden="true"
          className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted"
        />
        <input
          type="search"
          aria-label="Buscar"
          placeholder="Buscar por tombamento, beneficiário ou item..."
          className="h-10 w-full rounded-lg bg-field pl-9 pr-12 text-sm text-ink placeholder:text-muted-fg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
        />
        <kbd className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded bg-surface px-1.5 py-0.5 text-[10px] font-semibold text-muted-fg">
          ⌘K
        </kbd>
      </div>
      <button type="button" aria-label="Buscar" className={`${ICON_BUTTON} lg:hidden`}>
        <Search aria-hidden="true" className="size-5" />
      </button>

      <div className="ml-auto flex items-center gap-2">
        <span className="inline-flex items-center gap-1.5 rounded-full bg-success/10 px-2.5 py-1 text-[11px] font-semibold text-success-fg">
          <span aria-hidden="true" className="size-1.5 rounded-full bg-success" />
          Distrito LD-1
        </span>
        <button type="button" aria-label="Notificações" className={ICON_BUTTON}>
          <Bell aria-hidden="true" className="size-5" />
        </button>
        <button
          type="button"
          aria-label={theme === 'dark' ? 'Ativar tema claro' : 'Ativar tema escuro'}
          onClick={toggleTheme}
          className={ICON_BUTTON}
        >
          {theme === 'dark' ? (
            <Sun aria-hidden="true" className="size-5" />
          ) : (
            <Moon aria-hidden="true" className="size-5" />
          )}
        </button>
        <span
          aria-hidden="true"
          className="flex size-9 items-center justify-center rounded-full bg-primary text-sm font-bold text-on-primary"
        >
          {initial}
        </span>
      </div>
    </header>
  );
}
