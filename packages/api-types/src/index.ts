/**
 * Punto de entrada público de @app/api-types.
 * `schema.d.ts` es GENERADO (openapi-typescript); este archivo solo agrega helpers
 * genéricos sobre él y se mantiene a mano.
 */
import type { components, operations, paths } from './schema';

export type { components, operations, paths };

/** Todos los DTOs del OpenAPI: `Schemas['UserResponse']`. */
export type Schemas = components['schemas'];

/** Un DTO por nombre: `Schema<'UserResponse'>`. */
export type Schema<Name extends keyof Schemas> = Schemas[Name];

export type ApiPath = keyof paths;

export type HttpMethod = 'get' | 'put' | 'post' | 'delete' | 'patch';

/** Operación de un path + método (never si no existe). */
export type Operation<P extends ApiPath, M extends HttpMethod> = paths[P] extends {
  [K in M]: infer Op;
}
  ? Op
  : never;

type JsonContent<T> = T extends { content: { 'application/json': infer Body } } ? Body : never;

/** Body JSON del request de una operación. */
export type RequestBody<P extends ApiPath, M extends HttpMethod> =
  Operation<P, M> extends { requestBody?: infer Req } ? JsonContent<NonNullable<Req>> : never;

/** Body JSON de la respuesta para un status (por defecto 200). */
export type ResponseBody<P extends ApiPath, M extends HttpMethod, Status extends number = 200> =
  Operation<P, M> extends { responses: infer Responses }
    ? Status extends keyof Responses
      ? JsonContent<Responses[Status]>
      : never
    : never;

/** Query params de una operación. */
export type QueryParams<P extends ApiPath, M extends HttpMethod> =
  Operation<P, M> extends { parameters: { query?: infer Query } } ? NonNullable<Query> : never;

/** Path params de una operación. */
export type PathParams<P extends ApiPath, M extends HttpMethod> =
  Operation<P, M> extends { parameters: { path?: infer Params } } ? NonNullable<Params> : never;
