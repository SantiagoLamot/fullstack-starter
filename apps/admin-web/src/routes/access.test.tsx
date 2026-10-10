import { screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { describe, expect, it, vi } from 'vitest';

import { RequireRole } from '@/app/guards/RequireRole';
import type { Role } from '@/shared/lib/auth';
import { renderWithProviders } from '@/test/render';

import { canAccess, homePath, type RouteKey, routes } from './paths';

const useAuthMock = vi.hoisted(() => vi.fn());
vi.mock('@/shared/lib/auth/useAuth', () => ({ useAuth: useAuthMock }));

const NAV: RouteKey[] = ['dashboard', 'users'];
const visible = (roles: Role[]) => NAV.filter((k) => canAccess(routes[k].access, roles));

describe('acceso por rol', () => {
  it('ADMIN ve todo el panel', () => {
    expect(visible(['ADMIN'])).toEqual(NAV);
  });
  it('USER no ve nada del panel', () => {
    expect(visible(['USER'])).toEqual([]);
  });
  it('pantalla inicial: ADMIN al inicio', () => {
    expect(homePath(['ADMIN'])).toBe('/');
  });
});

function renderGuard(roles: Role[], access: (typeof routes)[RouteKey]['access']) {
  useAuthMock.mockReturnValue({ status: 'authenticated', user: null, roles });
  const router = createMemoryRouter(
    [
      {
        element: <RequireRole access={access} />,
        children: [{ path: '/', element: <p>Contenido</p> }],
      },
    ],
    { initialEntries: ['/'] },
  );
  renderWithProviders(<RouterProvider router={router} />);
}

describe('RequireRole', () => {
  it('USER en una ruta de ADMIN ve "Sin permisos"', () => {
    renderGuard(['USER'], routes.users.access);
    expect(screen.queryByText('Contenido')).toBeNull();
    expect(screen.getByText(/sin permisos/i)).toBeTruthy();
  });
  it('ADMIN en una ruta de ADMIN ve el contenido', () => {
    renderGuard(['ADMIN'], routes.users.access);
    expect(screen.getByText('Contenido')).toBeTruthy();
  });
});
