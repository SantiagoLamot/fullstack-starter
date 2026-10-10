import { Outlet } from 'react-router';

import { canAccess, type RouteAccess } from '@/routes';
import { useAuth } from '@/shared/lib/auth';

import { ForbiddenPage } from '../pages/ForbiddenPage';

interface RequireRoleProps {
  access: RouteAccess;
}

/**
 * Exige alguno de los roles indicados. Usar DENTRO de RequireAuth.
 * Es solo UX: la autorización real la aplica el backend.
 */
export function RequireRole({ access }: RequireRoleProps) {
  const { roles } = useAuth();
  return canAccess(access, roles) ? <Outlet /> : <ForbiddenPage />;
}
