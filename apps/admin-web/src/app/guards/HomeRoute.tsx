import { Navigate } from 'react-router';

import { DashboardPage } from '@/features/dashboard';
import { canAccess, homePath, routes } from '@/routes';
import { useAuth } from '@/shared/lib/auth';

/** Inicio del panel; los roles sin inicio se redirigen a su pantalla (ver homePath). */
export function HomeRoute() {
  const { roles } = useAuth();
  if (canAccess(routes.dashboard.access, roles)) return <DashboardPage />;
  return <Navigate to={homePath(roles)} replace />;
}
