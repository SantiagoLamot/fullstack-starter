# Convenciones de API

Fuente de verdad del contrato: el OpenAPI que genera el backend. Seguridad y roles: `docs/security.md`.

## Generales

- Base: `/api/v1`. Audiencias: `/admin/**` (ADMIN) y `/me/**` (USER); sumar otras solo si el alcance lo pide, registrándolas en `docs/security.md`.
- JSON (`application/json`), propiedades en `camelCase`, enums en `UPPER_SNAKE_CASE`.
- Recursos en plural y en inglés (`/admin/users`). Acciones que no son CRUD como sub-recurso con `POST` (`/admin/users/{id}/disable`).
- Códigos: `200` lectura/actualización, `201` creación (con `Location`), `204` sin cuerpo.
- Fechas/horas: ISO-8601 **con zona** (`2026-11-10T23:59:59-03:00`); fechas sin hora como `2026-11-10`. Zona de negocio configurable (`TZ`, default `America/Argentina/Buenos_Aires`).

## Paginación

Request: `?page=0&size=20&sort=createdAt,desc` (page base 0; `size` con máximo configurado, p. ej. 100).

Response:

```json
{
  "content": [ { "id": 1, "email": "a@b.com" } ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Se devuelve un `PageResponse` propio (`common/web`), no el `Page` de Spring serializado.

## Errores (ProblemDetail, RFC 9457)

`Content-Type: application/problem+json`. Ejemplo 400 de validación:

```json
{
  "type": "urn:app:problem:validation-error",
  "title": "Datos inválidos",
  "status": 400,
  "detail": "La solicitud tiene errores de validación.",
  "instance": "/api/v1/admin/users",
  "errors": [
    { "field": "email", "message": "Debe ser un email válido." },
    { "field": "firstName", "message": "Es obligatorio." }
  ]
}
```

| Código | Cuándo |
|---|---|
| 400 | Validación de Bean Validation o request mal formado (con `errors[]`). |
| 401 | Sin autenticación o token inválido/expirado. |
| 403 | Autenticado sin permiso. |
| 404 | No existe o no pertenece al usuario actual. |
| 409 | Conflicto de estado/unicidad (email duplicado, `@Version` desactualizada). |
| 422 | Regla de negocio incumplida con datos válidos (p. ej. operación fuera de plazo). |

Los frontends mapean `errors[].field` a los campos del formulario. Los mensajes van en español.

## Swagger / OpenAPI (solo dev)

- UI: `http://localhost:8080/swagger-ui.html`
- Spec: `http://localhost:8080/v3/api-docs`

## Regenerar tipos

Con el backend corriendo:

```
scripts/gen-api-types.sh      # Mac / Linux
scripts/gen-api-types.ps1     # Windows
```

Actualiza `packages/api-types/openapi.json` y `src/schema.d.ts`. Commitear ambos junto con el cambio de API.
