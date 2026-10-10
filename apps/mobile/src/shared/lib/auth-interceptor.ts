import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { AxiosHeaders } from 'axios';

export interface AuthInterceptorDeps {
  getAccessToken: () => string | null;
  /** Renueva la sesión y devuelve el nuevo access token. Falla si el refresh no es posible. */
  refresh: () => Promise<string>;
  /** Se invoca UNA vez por ronda de refresh fallida (limpiar sesión y volver a login). */
  onAuthFailure: (error: unknown) => void | Promise<void>;
}

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean };

// Endpoints públicos: sin Bearer ni refresh. `PUT /auth/password` NO va acá (exige sesión).
const AUTH_PATHS = [
  '/auth/login',
  '/auth/refresh',
  '/auth/logout',
  '/auth/invitations/accept',
  '/auth/password-reset/',
];

function isAuthEndpoint(url: string | undefined): boolean {
  return AUTH_PATHS.some((path) => url?.includes(path));
}

/**
 * Bearer en cada request + ante 401 un ÚNICO refresh compartido: los requests que fallan
 * mientras tanto esperan la misma promesa (cola) y se reintentan con el token nuevo.
 * Si el refresh falla: `onAuthFailure` (una vez) y todos los requests en cola se rechazan.
 */
export function installAuthInterceptor(client: AxiosInstance, deps: AuthInterceptorDeps): void {
  let refreshing: Promise<string> | null = null;

  const startRefresh = (): Promise<string> => {
    refreshing ??= deps
      .refresh()
      .catch(async (error: unknown) => {
        await deps.onAuthFailure(error);
        throw error;
      })
      .finally(() => {
        refreshing = null;
      });
    return refreshing;
  };

  client.interceptors.request.use((config) => {
    const token = deps.getAccessToken();
    if (token && !isAuthEndpoint(config.url)) {
      const headers = AxiosHeaders.from(config.headers);
      headers.set('Authorization', `Bearer ${token}`);
      config.headers = headers;
    }
    return config;
  });

  client.interceptors.response.use(undefined, async (error: unknown) => {
    const original = (error as { config?: RetriableConfig }).config;
    const status = (error as { response?: { status?: number } }).response?.status;
    if (status !== 401 || !original || original._retried || isAuthEndpoint(original.url)) {
      throw error;
    }
    original._retried = true;
    let token: string;
    try {
      token = await startRefresh();
    } catch {
      throw error;
    }
    const headers = AxiosHeaders.from(original.headers);
    headers.set('Authorization', `Bearer ${token}`);
    original.headers = headers;
    return client.request(original);
  });
}
