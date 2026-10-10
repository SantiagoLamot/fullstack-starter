import axios, { AxiosHeaders } from 'axios';

import { env } from '@/shared/config/env';
import { notifySessionExpired, refreshAccessToken, tokenStore } from '@/shared/lib/auth';

import { installRefreshInterceptor } from './refresh-interceptor';

/**
 * Cliente HTTP único. Mismo origen (proxy de Vite en dev / nginx en prod): baseURL
 * relativo y withCredentials para que viaje la cookie de refresh.
 */
export const http = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true,
  headers: { Accept: 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
});

http.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    const headers = AxiosHeaders.from(config.headers);
    headers.set('Authorization', `Bearer ${token}`);
    config.headers = headers;
  }
  return config;
});

installRefreshInterceptor(http, {
  refresh: refreshAccessToken,
  onSessionExpired: notifySessionExpired,
  excludedUrls: [
    '/auth/login',
    '/auth/refresh',
    '/auth/logout',
    '/auth/invitations',
    '/auth/password-reset',
  ],
});
