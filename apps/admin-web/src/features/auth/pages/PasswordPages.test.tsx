import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AxiosError, type AxiosResponse } from 'axios';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { AuthContextValue } from '@/shared/lib/auth';
import { renderWithProviders } from '@/test/render';

import { AccountPage } from './AccountPage';
import { ForgotPasswordPage } from './ForgotPasswordPage';
import { ResetPasswordPage } from './ResetPasswordPage';

const api = vi.hoisted(() => ({
  requestPasswordReset: vi.fn(),
  confirmPasswordReset: vi.fn(),
  changePassword: vi.fn(),
}));
vi.mock('../api/password', () => api);

vi.mock('@/shared/lib/auth/useAuth', () => ({
  useAuth: (): AuthContextValue => ({
    status: 'authenticated',
    user: { id: 1, email: 'ana@example.test', fullName: 'Ana Admin', roles: ['ADMIN'] },
    roles: ['ADMIN'],
    login: () => Promise.resolve(),
    logout: () => Promise.resolve(),
  }),
}));

function httpError(status: number, data: unknown) {
  return new AxiosError('error', 'ERR', undefined, null, { status, data } as AxiosResponse);
}

function renderAt(path: string, element: React.ReactNode, route: string) {
  const router = createMemoryRouter([{ path: route, element }], { initialEntries: [path] });
  return renderWithProviders(<RouterProvider router={router} />);
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('ForgotPasswordPage', () => {
  it('muestra el mensaje neutro al enviar', async () => {
    api.requestPasswordReset.mockResolvedValueOnce(undefined);
    renderAt('/olvide-contrasena', <ForgotPasswordPage />, '/olvide-contrasena');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText('Email'), 'a@example.test');
    await user.click(screen.getByRole('button', { name: 'Enviar link' }));

    expect(await screen.findByText(/Si el email corresponde a una cuenta activa/)).toBeVisible();
    expect(api.requestPasswordReset).toHaveBeenCalledWith({ email: 'a@example.test' });
  });

  it('muestra el mensaje del backend ante 429', async () => {
    api.requestPasswordReset.mockRejectedValueOnce(
      httpError(429, { title: 'Demasiados intentos', status: 429, detail: 'Esperá un rato.' }),
    );
    renderAt('/olvide-contrasena', <ForgotPasswordPage />, '/olvide-contrasena');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText('Email'), 'a@example.test');
    await user.click(screen.getByRole('button', { name: 'Enviar link' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Esperá un rato.');
  });
});

describe('ResetPasswordPage', () => {
  async function fill() {
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText('Contraseña nueva'), 'clave12345a');
    await user.type(screen.getByLabelText('Repetir contraseña'), 'clave12345a');
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }));
  }

  it('sin token muestra link inválido', async () => {
    renderAt('/restablecer', <ResetPasswordPage />, '/restablecer');
    expect(await screen.findByText('Link inválido')).toBeVisible();
  });

  it('éxito: confirma con el token y ofrece ir a iniciar sesión', async () => {
    api.confirmPasswordReset.mockResolvedValueOnce(undefined);
    renderAt('/restablecer?token=abc', <ResetPasswordPage />, '/restablecer');
    await fill();

    expect(await screen.findByRole('link', { name: 'Ir a iniciar sesión' })).toBeVisible();
    expect(api.confirmPasswordReset).toHaveBeenCalledWith({
      token: 'abc',
      newPassword: 'clave12345a',
    });
  });

  it('422: link inválido con acción para pedir otro', async () => {
    api.confirmPasswordReset.mockRejectedValueOnce(httpError(422, { status: 422, title: 'x' }));
    renderAt('/restablecer?token=abc', <ResetPasswordPage />, '/restablecer');
    await fill();

    expect(await screen.findByText('El link es inválido o venció. Pedí uno nuevo.')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Pedir un link nuevo' })).toHaveAttribute(
      'href',
      '/olvide-contrasena',
    );
  });
});

describe('AccountPage', () => {
  async function fill() {
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText('Contraseña actual'), 'vieja12345a');
    await user.type(screen.getByLabelText('Contraseña nueva'), 'nueva12345a');
    await user.type(screen.getByLabelText('Repetir contraseña nueva'), 'nueva12345a');
    await user.click(screen.getByRole('button', { name: 'Cambiar contraseña' }));
  }

  it('muestra los datos del usuario', async () => {
    renderAt('/mi-cuenta', <AccountPage />, '/mi-cuenta');
    expect(await screen.findByText('Ana Admin')).toBeVisible();
    expect(screen.getByText('ana@example.test')).toBeVisible();
  });

  it('422 de contraseña actual incorrecta se muestra bajo el campo', async () => {
    api.changePassword.mockRejectedValueOnce(
      httpError(422, {
        status: 422,
        detail: 'Revisá los datos.',
        errors: [{ field: 'currentPassword', message: 'La contraseña actual no es correcta.' }],
      }),
    );
    renderAt('/mi-cuenta', <AccountPage />, '/mi-cuenta');
    await fill();

    await waitFor(() => {
      expect(screen.getByText('La contraseña actual no es correcta.')).toBeVisible();
    });
    expect(api.changePassword).toHaveBeenCalledWith({
      currentPassword: 'vieja12345a',
      newPassword: 'nueva12345a',
    });
  });

  it('422 sin errors muestra el detail como alerta general', async () => {
    api.changePassword.mockRejectedValueOnce(
      httpError(422, { status: 422, detail: 'Algo falló.' }),
    );
    renderAt('/mi-cuenta', <AccountPage />, '/mi-cuenta');
    await fill();

    expect(await screen.findByRole('alert')).toHaveTextContent('Algo falló.');
  });

  it('429 muestra el aviso general', async () => {
    api.changePassword.mockRejectedValueOnce(httpError(429, { status: 429 }));
    renderAt('/mi-cuenta', <AccountPage />, '/mi-cuenta');
    await fill();

    expect(await screen.findByRole('alert')).toHaveTextContent('demasiados intentos');
  });
});
