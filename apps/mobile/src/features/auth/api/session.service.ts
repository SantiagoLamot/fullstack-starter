import { isAxiosError } from 'axios';

import { queryClient } from '@/shared/lib/query-client';
import { refreshTokenStorage } from '@/shared/lib/secure-storage';

import { useSessionStore } from '../store/session.store';
import { authApi, type LoginPayload, type TokenResponse } from './auth.api';

/** Guarda la sesión de un login/refresh (rotación: siempre persiste el refresh nuevo). */
async function persist(res: TokenResponse): Promise<string> {
  if (!res.accessToken || !res.refreshToken) throw new Error('Respuesta de sesión incompleta');
  await refreshTokenStorage.set(res.refreshToken);
  if (res.user) {
    useSessionStore.getState().setSession({ user: res.user, accessToken: res.accessToken });
  } else {
    useSessionStore.getState().setAccessToken(res.accessToken);
  }
  return res.accessToken;
}

export async function login(payload: LoginPayload): Promise<void> {
  await persist(await authApi.login(payload));
}

/** Renueva el access token con el refresh guardado. Lo usa el interceptor. */
export async function refreshAccessToken(): Promise<string> {
  const stored = await refreshTokenStorage.get();
  if (!stored) throw new Error('No hay refresh token');
  return persist(await authApi.refresh(stored));
}

/**
 * Limpia la sesión en memoria. El refresh token se conserva solo ante un corte de red
 * (sin respuesta del servidor): así un bache de conectividad no desloguea al usuario.
 */
export async function handleAuthFailure(error: unknown): Promise<void> {
  const networkError = isAxiosError(error) && !error.response;
  if (!networkError) await refreshTokenStorage.clear();
  useSessionStore.getState().clearSession();
}

/** Al abrir la app: refresh con el token guardado + /me. Sin token o con fallo ⇒ sin sesión. */
export async function bootstrapSession(): Promise<void> {
  const stored = await refreshTokenStorage.get();
  if (!stored) {
    useSessionStore.getState().clearSession();
    return;
  }
  try {
    const res = await authApi.refresh(stored);
    const accessToken = await persist(res);
    if (!res.user) {
      const user = await authApi.me(accessToken);
      useSessionStore.getState().setSession({ user, accessToken });
    }
  } catch (error) {
    await handleAuthFailure(error);
  }
}

/** Logout: avisa al backend (best-effort) y limpia SecureStore + memoria. */
export async function logout(): Promise<void> {
  try {
    // El logout del backend es público y revoca la sesión por el refresh guardado.
    const stored = await refreshTokenStorage.get();
    if (stored) await authApi.logout(stored);
  } catch {
    // Backend inalcanzable: igual cerramos localmente.
  } finally {
    await refreshTokenStorage.clear();
    useSessionStore.getState().clearSession();
    // Otro usuario en el mismo teléfono no debe ver datos del anterior.
    queryClient.clear();
  }
}
