import type { ReactNode } from 'react';
import { AccessDenied } from '@/features/auth/AccessDenied';
import { useAuth } from '@/features/auth/AuthContext';
import type { UserRole } from '@/types/api';

export function RequireRole({ roles, children }: { roles: UserRole[]; children: ReactNode }) {
  const { hasRole } = useAuth();
  if (!hasRole(...roles)) return <AccessDenied />;
  return <>{children}</>;
}
