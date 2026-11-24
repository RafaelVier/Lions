import { useEffect, useRef } from 'react';
import { LogOut, User, X } from 'lucide-react';
import { Link, useLocation } from 'react-router';
import { NAV_ITEMS, type NavItem } from '@/components/layout/navItems';
import { useAuth } from '@/features/auth/AuthContext';
import { cn } from '@/lib/cn';

function SidebarLink({ item, onNavigate }: { item: NavItem; onNavigate?: () => void }) {
  const { pathname } = useLocation();
  const Icon = item.icon;
  const active = item.isActive ? item.isActive(pathname) : pathname === item.to;
  return (
    <Link
      to={item.to}
      onClick={onNavigate}
      aria-current={active ? 'page' : undefined}
      className={cn(
        'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary',
        active
          ? 'bg-field font-semibold text-primary'
          : 'text-muted-fg hover:bg-field hover:text-ink',
      )}
    >
      <Icon aria-hidden="true" className="size-[18px] shrink-0" />
      {item.label}
    </Link>
  );
}

function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  const { session, logout, hasRole } = useAuth();
  const visible = NAV_ITEMS.filter((item) => !item.roles || hasRole(...item.roles));
  const main = visible.filter((item) => item.section === 'main');
  const footer = visible.filter((item) => item.section === 'footer');

  return (
    <div className="flex h-full flex-col gap-4 p-3">
      <div className="flex items-center gap-2.5 px-2 pt-1">
        <img src="/logo-lions.png" alt="" className="size-9 object-contain" />
        <div className="leading-tight">
          <p className="text-base font-bold text-primary-strong">Banco Ortopédico</p>
          <p className="text-[10px] font-medium tracking-wide text-muted-fg">LIONS CLUBE</p>
        </div>
      </div>

      <div className="flex items-center justify-between rounded-lg bg-field px-3 py-2 text-[11px]">
        <span className="font-medium tracking-wide text-muted-fg">UNIDADE</span>
        <span className="font-bold text-primary-strong">Medianeira • LD-1</span>
      </div>

      <nav aria-label="Menu principal" className="flex flex-1 flex-col gap-1 overflow-y-auto">
        {main.map((item) => (
          <SidebarLink key={item.to} item={item} onNavigate={onNavigate} />
        ))}
      </nav>

      {footer.length > 0 && (
        <div className="flex flex-col gap-1 border-t border-line pt-3">
          {footer.map((item) => (
            <SidebarLink key={item.to} item={item} onNavigate={onNavigate} />
          ))}
        </div>
      )}

      {session && (
        <div className="flex items-center gap-2.5 rounded-xl bg-field p-2.5">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary text-on-primary">
            <User aria-hidden="true" className="size-4" />
          </span>
          <div className="min-w-0 flex-1 leading-tight">
            <p className="truncate text-sm font-semibold text-ink">{session.username}</p>
            <p className="truncate text-[11px] text-muted-fg">{session.email}</p>
          </div>
          <button
            type="button"
            aria-label="Sair"
            onClick={() => void logout()}
            className="rounded-md p-1.5 text-muted-fg transition hover:bg-surface hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <LogOut aria-hidden="true" className="size-4" />
          </button>
        </div>
      )}
    </div>
  );
}

// Menu fixo da versão desktop (some abaixo de 1024 px).
export function Sidebar() {
  return (
    <aside className="sticky top-0 hidden h-screen w-64 shrink-0 border-r border-line bg-surface lg:block">
      <SidebarContent />
    </aside>
  );
}

// Mesma largura do `lg` do Tailwind: a partir dela o menu fixo aparece e a gaveta some.
const DESKTOP_QUERY = '(min-width: 1024px)';

// Menu como gaveta nas telas menores; só existe no DOM enquanto aberto.
export function MobileDrawer({ open, onClose }: { open: boolean; onClose: () => void }) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const closeRef = useRef<HTMLButtonElement>(null);
  // Ao navegar por um link o foco não volta ao botão "Abrir menu": a página mudou e o foco fica no documento.
  const skipFocusRestore = useRef(false);

  useEffect(() => {
    if (!open) return;
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    skipFocusRestore.current = false;
    closeRef.current?.focus();
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
        return;
      }
      if (event.key !== 'Tab' || !dialogRef.current) return;
      // laço de foco: Tab no último volta ao primeiro, Shift+Tab no primeiro vai ao último
      const focusable = dialogRef.current.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled])',
      );
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const active = document.activeElement;
      if (event.shiftKey && (active === first || !dialogRef.current.contains(active))) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && (active === last || !dialogRef.current.contains(active))) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener('keydown', onKeyDown);
    // Tela cresceu para desktop (girar o tablet, redimensionar): a gaveta fica escondida pelo CSS,
    // mas o laço de foco continuaria preso nela. Fecha; o botão "Abrir menu" também sumiu.
    const desktop = window.matchMedia(DESKTOP_QUERY);
    const onViewportChange = (event: MediaQueryListEvent) => {
      if (!event.matches) return;
      skipFocusRestore.current = true;
      onClose();
    };
    desktop.addEventListener('change', onViewportChange);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      desktop.removeEventListener('change', onViewportChange);
      if (!skipFocusRestore.current) opener?.focus();
    };
  }, [open, onClose]);

  const onNavigate = () => {
    skipFocusRestore.current = true;
    onClose();
  };

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 lg:hidden">
      <div aria-hidden="true" className="absolute inset-0 bg-ink/50" onClick={onClose} />
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-label="Menu"
        className="absolute inset-y-0 left-0 w-72 max-w-[85vw] bg-surface shadow-xl"
      >
        <button
          ref={closeRef}
          type="button"
          aria-label="Fechar menu"
          onClick={onClose}
          className="absolute right-2 top-2 rounded-md p-1.5 text-muted-fg hover:bg-field focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
        >
          <X aria-hidden="true" className="size-5" />
        </button>
        <SidebarContent onNavigate={onNavigate} />
      </div>
    </div>
  );
}
