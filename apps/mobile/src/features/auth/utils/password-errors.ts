import { isAxiosError } from 'axios';

export const TOO_MANY_REQUESTS_MESSAGE =
  'Hiciste demasiados intentos. Esperá unos minutos y volvé a probar.';

export const NETWORK_ERROR_MESSAGE = 'No pudimos conectar con el servidor. Revisá tu conexión.';

/** 429 del backend (`too-many-requests`). */
export function isTooManyRequests(error: unknown): boolean {
  return isAxiosError(error) && error.response?.status === 429;
}

/** Sin respuesta del servidor (corte de red). */
export function isNetworkError(error: unknown): boolean {
  return isAxiosError(error) && !error.response;
}
