import axios from 'axios';

/**
 * Base URL de la API (incluye `/api/v1`). Se define con `EXPO_PUBLIC_API_URL` en el `.env`
 * del repo (en dev: `http://<IP-LAN-de-la-PC>:8080/api/v1`; el celular no ve `localhost`).
 * Viaja embebida en el bundle (no es secreta). Ver `apps/mobile/CLAUDE.md`.
 */
const apiUrl = process.env.EXPO_PUBLIC_API_URL;
if (!apiUrl) {
  console.warn('[http] EXPO_PUBLIC_API_URL no está definida; usando "/" como baseURL.');
}

const baseConfig = {
  baseURL: apiUrl ?? '/',
  timeout: 15_000,
  // Sin header X-Requested-With: así el backend envía/recibe el refresh token en el body.
  headers: { Accept: 'application/json' },
};

/** Cliente con interceptor de sesión (se instala en `features/auth/api/install-http-auth`). */
export const http = axios.create(baseConfig);

/** Cliente sin interceptores, para el refresh y /me de bootstrap (evita recursión). */
export const plainHttp = axios.create(baseConfig);
