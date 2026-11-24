import type { RouteObject } from 'react-router';
import { NotFoundPage } from '@/app/NotFoundPage';
import { AppLayout } from '@/components/layout/AppLayout';
import { LoginPage } from '@/features/auth/LoginPage';
import { RequireAuth } from '@/features/auth/RequireAuth';
import { RequireRole } from '@/features/auth/RequireRole';
import { BeneficiariesPage } from '@/features/beneficiaries/BeneficiariesPage';
import { DashboardPage } from '@/features/dashboard/DashboardPage';
import { EquipmentDetailPage } from '@/features/equipment/EquipmentDetailPage';
import { EquipmentListPage } from '@/features/equipment/EquipmentListPage';
import { EquipmentNewPage } from '@/features/equipment/EquipmentNewPage';
import { HelpPage } from '@/features/help/HelpPage';
import { LoansPage } from '@/features/loans/LoansPage';
import { ReportsPage } from '@/features/reports/ReportsPage';
import { UsersPage } from '@/features/users/UsersPage';
import { WaitlistPage } from '@/features/waitlist/WaitlistPage';

export const routes: RouteObject[] = [
  { path: '/login', element: <LoginPage /> },
  {
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'estoque', element: <EquipmentListPage /> },
      { path: 'estoque/novo', element: <EquipmentNewPage /> },
      { path: 'estoque/:patrimonio', element: <EquipmentDetailPage /> },
      { path: 'emprestimos', element: <LoansPage /> },
      { path: 'relatorios', element: <ReportsPage /> },
      { path: 'beneficiarios', element: <BeneficiariesPage /> },
      { path: 'fila', element: <WaitlistPage /> },
      {
        path: 'usuarios',
        element: (
          <RequireRole roles={['ROLE_ADMIN']}>
            <UsersPage />
          </RequireRole>
        ),
      },
      { path: 'ajuda', element: <HelpPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
