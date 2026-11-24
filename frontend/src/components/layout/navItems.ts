import {
  Archive,
  CircleHelp,
  ClipboardList,
  FileBarChart,
  HandHeart,
  LayoutGrid,
  PlusCircle,
  Repeat,
  Users,
  type LucideIcon,
} from 'lucide-react';
import type { UserRole } from '@/types/api';

export interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  roles?: UserRole[];
  section: 'main' | 'footer';
  isActive?(pathname: string): boolean;
}

export const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Início', icon: LayoutGrid, section: 'main', isActive: (p) => p === '/' },
  {
    to: '/estoque',
    label: 'Estoque',
    icon: Archive,
    section: 'main',
    isActive: (p) => p !== '/estoque/novo' && (p === '/estoque' || p.startsWith('/estoque/')),
  },
  { to: '/estoque/novo', label: 'Novo Item', icon: PlusCircle, section: 'main' },
  { to: '/emprestimos', label: 'Empréstimos', icon: Repeat, section: 'main' },
  { to: '/relatorios', label: 'Relatórios', icon: FileBarChart, section: 'main' },
  { to: '/beneficiarios', label: 'Beneficiários', icon: HandHeart, section: 'main' },
  { to: '/fila', label: 'Fila de espera', icon: ClipboardList, section: 'main' },
  { to: '/usuarios', label: 'Usuários', icon: Users, roles: ['ROLE_ADMIN'], section: 'main' },
  { to: '/ajuda', label: 'Ajuda', icon: CircleHelp, section: 'footer' },
];
