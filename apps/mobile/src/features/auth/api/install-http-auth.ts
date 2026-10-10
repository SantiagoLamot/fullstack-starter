import { installAuthInterceptor } from '@/shared/lib/auth-interceptor';
import { http } from '@/shared/lib/http';

import { useSessionStore } from '../store/session.store';
import { handleAuthFailure, refreshAccessToken } from './session.service';

let installed = false;

/** Conecta el cliente HTTP con la sesión (Bearer + refresh único). Idempotente. */
export function installHttpAuth(): void {
  if (installed) return;
  installed = true;
  installAuthInterceptor(http, {
    getAccessToken: () => useSessionStore.getState().accessToken,
    refresh: refreshAccessToken,
    onAuthFailure: handleAuthFailure,
  });
}
