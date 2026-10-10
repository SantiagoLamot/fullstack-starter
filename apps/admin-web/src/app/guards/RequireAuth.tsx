import { Center, Loader } from '@mantine/core';
import { Navigate, Outlet, useLocation } from 'react-router';

import { paths } from '@/routes';
import { useAuth } from '@/shared/lib/auth';

/** Estado que recibe la página de login para volver a donde estaba el usuario. */
export interface LoginRedirectState {
  from: string;
}

/** Exige sesión. Sin sesión redirige a login recordando la URL pedida. */
export function RequireAuth() {
  const { status } = useAuth();
  const location = useLocation();

  if (status === 'loading') {
    return (
      <Center h="100vh">
        <Loader aria-label="Cargando sesión" />
      </Center>
    );
  }

  if (status === 'unauthenticated') {
    const state: LoginRedirectState = {
      from: `${location.pathname}${location.search}${location.hash}`,
    };
    return <Navigate to={paths.login} replace state={state} />;
  }

  return <Outlet />;
}
