import type { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';

interface RetriableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean;
}

interface RefreshInterceptorOptions {
  /** Renueva el access token. El interceptor garantiza una sola llamada en vuelo. */
  refresh: () => Promise<string>;
  /** Se llama cuando el refresh falla: cerrar sesión. */
  onSessionExpired: () => void;
  /** URLs que nunca disparan refresh (login, refresh, etc.). */
  excludedUrls: readonly string[];
}

/**
 * Ante 401: UN solo refresh; las requests que fallan mientras tanto esperan esa misma
 * promesa (cola) y se reintentan una vez con el token nuevo. Si falla, logout.
 */
export function installRefreshInterceptor(
  instance: AxiosInstance,
  { refresh, onSessionExpired, excludedUrls }: RefreshInterceptorOptions,
): void {
  let inflight: Promise<string> | null = null;
  const refreshOnce = () => {
    inflight ??= refresh().finally(() => {
      inflight = null;
    });
    return inflight;
  };

  instance.interceptors.response.use(undefined, async (error: AxiosError) => {
    const config = error.config as RetriableConfig | undefined;
    const excluded = excludedUrls.some((url) => config?.url?.includes(url));
    if (error.response?.status !== 401 || !config || config._retried || excluded) {
      throw error;
    }
    config._retried = true;
    try {
      await refreshOnce();
    } catch {
      onSessionExpired();
      throw error;
    }
    return instance.request(config);
  });
}
