import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AxiosError, type AxiosResponse } from 'axios';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { describe, expect, it, vi } from 'vitest';

import type { AuthContextValue } from '@/shared/lib/auth';
import { renderWithProviders } from '@/test/render';

import { LoginPage } from './LoginPage';

const authMock = vi.hoisted(() => ({ login: vi.fn() }));

vi.mock('@/shared/lib/auth/useAuth', () => ({
  useAuth: (): AuthContextValue => ({
    status: 'unauthenticated',
    user: null,
    roles: [],
    login: authMock.login as AuthContextValue['login'],
    logout: () => Promise.resolve(),
  }),
}));

function httpError(status: number, data: unknown) {
  return new AxiosError('error', 'ERR', undefined, null, { status, data } as AxiosResponse);
}

function renderLogin() {
  const router = createMemoryRouter([{ path: '/login', element: <LoginPage /> }], {
    initialEntries: ['/login'],
  });
  return renderWithProviders(<RouterProvider router={router} />);
}

async function fillAndSubmit() {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText('Email'), 'admin@example.test');
  await user.type(screen.getByLabelText('Contraseña'), 'secreto');
  await user.click(screen.getByRole('button', { name: 'Ingresar' }));
}

describe('LoginPage', () => {
  it('muestra un mensaje genérico ante 401', async () => {
    authMock.login.mockRejectedValueOnce(httpError(401, {}));
    renderLogin();
    await fillAndSubmit();

    expect(await screen.findByRole('alert')).toHaveTextContent('Email o contraseña incorrectos.');
  });

  it('mapea errores 400 a los campos', async () => {
    authMock.login.mockRejectedValueOnce(
      httpError(400, {
        title: 'Bad Request',
        status: 400,
        errors: [{ field: 'email', message: 'formato inválido' }],
      }),
    );
    renderLogin();
    await fillAndSubmit();

    await waitFor(() => {
      expect(screen.getByText('formato inválido')).toBeInTheDocument();
    });
  });
});
