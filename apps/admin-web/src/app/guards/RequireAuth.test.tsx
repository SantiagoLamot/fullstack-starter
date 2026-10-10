import { screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider, useLocation } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { paths } from '@/routes';
import type { AuthContextValue, AuthState } from '@/shared/lib/auth';
import { renderWithProviders } from '@/test/render';

import { RequireAuth } from './RequireAuth';
import { RequireRole } from './RequireRole';

const authMock = vi.hoisted(() => ({ state: null as AuthState | null }));

// Por defecto "no autenticado"; los tests pueden forzar un estado.
vi.mock('@/shared/lib/auth/useAuth', () => ({
  useAuth: (): AuthContextValue => ({
    ...(authMock.state ?? { status: 'unauthenticated', user: null, roles: [] }),
    login: () => Promise.resolve(),
    logout: () => Promise.resolve(),
  }),
}));

function LoginProbe() {
  const location = useLocation();
  const state = location.state as { from?: string } | null;
  return <p>Login (desde {state?.from})</p>;
}

function renderAt(url: string) {
  const router = createMemoryRouter(
    [
      { path: paths.login, element: <LoginProbe /> },
      {
        element: <RequireAuth />,
        children: [
          {
            element: <RequireRole access={['ADMIN']} />,
            children: [{ path: '/privado', element: <p>Contenido privado</p> }],
          },
        ],
      },
    ],
    { initialEntries: [url] },
  );
  return renderWithProviders(<RouterProvider router={router} />);
}

describe('guards', () => {
  beforeEach(() => {
    authMock.state = null;
  });

  it('RequireAuth redirige a login si no hay sesión, recordando la URL', async () => {
    renderAt('/privado?tab=1');

    expect(await screen.findByText('Login (desde /privado?tab=1)')).toBeInTheDocument();
    expect(screen.queryByText('Contenido privado')).not.toBeInTheDocument();
  });

  it('renderiza la ruta con sesión y rol válido', async () => {
    authMock.state = {
      status: 'authenticated',
      user: { id: 1, email: 'admin@example.test', fullName: 'Admin', roles: ['ADMIN'] },
      roles: ['ADMIN'],
    };
    renderAt('/privado');

    expect(await screen.findByText('Contenido privado')).toBeInTheDocument();
  });

  it('RequireRole muestra "Sin permisos" si el rol no alcanza', async () => {
    authMock.state = {
      status: 'authenticated',
      user: { id: 2, email: 'user@example.test', fullName: 'Usuario', roles: ['USER'] },
      roles: ['USER'],
    };
    renderAt('/privado');

    expect(await screen.findByText('Sin permisos')).toBeInTheDocument();
  });
});
