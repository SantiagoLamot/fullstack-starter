import { isAxiosError } from 'axios';
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';

/** Error de validación por campo que devuelve el backend. */
export interface ProblemFieldError {
  field: string;
  message: string;
}

/**
 * ProblemDetail (RFC 9457) + extensión `errors` del backend.
 * Mismo contrato que `apps/admin-web/src/shared/lib/problem-details.ts`.
 * TODO: reemplazar por el tipo de `@app/api-types` cuando el OpenAPI lo exponga.
 */
export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: ProblemFieldError[];
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function isProblemFieldError(value: unknown): value is ProblemFieldError {
  return isRecord(value) && typeof value.field === 'string' && typeof value.message === 'string';
}

export function isProblemDetails(value: unknown): value is ProblemDetails {
  if (!isRecord(value)) return false;
  const hasShape = 'title' in value || 'status' in value || 'detail' in value;
  const errorsOk =
    value.errors === undefined ||
    (Array.isArray(value.errors) && value.errors.every(isProblemFieldError));
  return hasShape && errorsOk;
}

/** Extrae el ProblemDetail de un error de axios (o de un body ya parseado). */
export function getProblemDetails(error: unknown): ProblemDetails | null {
  const body: unknown = isAxiosError(error) ? error.response?.data : error;
  return isProblemDetails(body) ? body : null;
}

/**
 * Mapea `errors[{field,message}]` a los campos de react-hook-form.
 * Solo aplica los errores de campos que el formulario conoce (`fields`) y hace foco en
 * el primero. Devuelve los que no pudo mapear, para mostrarlos como error general.
 */
export function applyProblemFieldErrors<T extends FieldValues>(
  problem: ProblemDetails | null,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
): ProblemFieldError[] {
  const known = new Set<string>(fields);
  const unmapped: ProblemFieldError[] = [];
  let focused = false;

  for (const fieldError of problem?.errors ?? []) {
    if (!known.has(fieldError.field)) {
      unmapped.push(fieldError);
      continue;
    }
    setError(
      fieldError.field as Path<T>,
      { type: 'server', message: fieldError.message },
      { shouldFocus: !focused },
    );
    focused = true;
  }
  return unmapped;
}
