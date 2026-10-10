import axios from 'axios';

import type { Schemas } from '@app/api-types';

import { env } from '@/shared/config/env';

import { tokenStore } from './token-store';
import type { AuthUser } from './types';

type AuthUserResponse = Schemas['AuthUserResponse'];

/**
 * Cliente "crudo" (sin interceptores) para los endpoints de sesión. Con el header
 * `X-Requested-With` el backend entrega/lee el refresh token por cookie httpOnly.
 */
export const authClient = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true,
  headers: { Accept: 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
});

export function toAuthUser(dto: AuthUserResponse): AuthUser {
  return {
    id: dto.id ?? 0,
    email: dto.email ?? '',
    fullName: [dto.firstName, dto.lastName].filter(Boolean).join(' '),
    roles: dto.role ? [dto.role] : [],
  };
}

let inflight: Promise<string> | null = null;

/**
 * Renueva el access token con la cookie de refresh. Es de vuelo único: las llamadas
 * concurrentes comparten la misma promesa (el refresh rota el token, no se puede repetir).
 */
export function refreshAccessToken(): Promise<string> {
  inflight ??= authClient
    .post<Schemas['TokenResponse']>('/auth/refresh')
    .then((response) => {
      const token = response.data.accessToken;
      if (!token) throw new Error('El refresh no devolvió access token');
      tokenStore.set(token);
      return token;
    })
    .finally(() => {
      inflight = null;
    });
  return inflight;
}

export async function loginRequest(email: string, password: string): Promise<AuthUser> {
  const { data } = await authClient.post<Schemas['TokenResponse']>('/auth/login', {
    email,
    password,
  });
  if (!data.accessToken || !data.user) throw new Error('Respuesta de login inválida');
  tokenStore.set(data.accessToken);
  return toAuthUser(data.user);
}

export async function fetchMe(accessToken: string): Promise<AuthUser> {
  const { data } = await authClient.get<AuthUserResponse>('/auth/me', {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  return toAuthUser(data);
}

/**
 * `POST /auth/logout` es público: revoca por la cookie de refresh (sin Authorization), responde
 * 204 aunque el access esté vencido y borra la cookie. Siempre limpia el token en memoria.
 */
export async function logoutRequest(): Promise<void> {
  try {
    await authClient.post('/auth/logout');
  } finally {
    tokenStore.clear();
  }
}

/** Handler que registra el AuthProvider para cerrar la sesión si el refresh falla. */
let sessionExpiredHandler: (() => void) | null = null;

export function setSessionExpiredHandler(handler: (() => void) | null): void {
  sessionExpiredHandler = handler;
}

export function notifySessionExpired(): void {
  tokenStore.clear();
  sessionExpiredHandler?.();
}
