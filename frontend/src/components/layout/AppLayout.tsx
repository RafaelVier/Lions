import { useCallback, useState } from 'react';
import { Outlet } from 'react-router';
import { MockBanner } from '@/components/layout/MockBanner';
import { OfflineBanner } from '@/components/layout/OfflineBanner';
import { MobileDrawer, Sidebar } from '@/components/layout/Sidebar';
import { Topbar } from '@/components/layout/Topbar';

// O documento rola (menu e barra de topo são sticky): a sessão conta o scroll da janela como atividade.
export function AppLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const closeDrawer = useCallback(() => setDrawerOpen(false), []);

  return (
    <div className="min-h-screen bg-background">
      <div className="flex">
        <Sidebar />
        <div className="flex min-h-screen min-w-0 flex-1 flex-col">
          <MockBanner />
          <OfflineBanner />
          <Topbar onOpenMenu={() => setDrawerOpen(true)} />
          <main className="flex-1 p-4 lg:p-8">
            <Outlet />
          </main>
        </div>
      </div>
      <MobileDrawer open={drawerOpen} onClose={closeDrawer} />
    </div>
  );
}
