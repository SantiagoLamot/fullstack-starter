import { isAxiosError } from 'axios';

import { getProblemDetails } from '@/shared/lib/problem-details';

export const TOO_MANY_REQUESTS_MESSAGE =
  'Hiciste demasiados intentos. Esperá unos minutos y volvé a probar.';
export const GENERIC_ERROR_MESSAGE = 'Ocurrió un error. Intentá de nuevo.';

export function httpStatus(error: unknown): number | undefined {
  return isAxiosError(error) ? error.response?.status : undefined;
}

/** Mensaje en español para 429 (usa el detail del backend si viene). */
export function tooManyRequestsMessage(error: unknown): string {
  return getProblemDetails(error)?.detail ?? TOO_MANY_REQUESTS_MESSAGE;
}
