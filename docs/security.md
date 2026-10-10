# Seguridad

## Modelo de autenticación (resumen)

Detalle y alternativas en [ADR 0005](adr/0005-auth-jwt.md) y [ADR 0009](adr/0009-session-revocation.md).

- API stateless: `Authorization: Bearer <access JWT>` (HS256, 15 min, `sub` = userId, claim `roles`, claim `sid`).
- Refresh opaco, rotativo, hasheado en DB, con detección de reuso por `familyId`.
- Admin-web: refresh en cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth` + header custom (`X-Requested-With`) obligatorio en refresh. Mobile: refresh en body, guardado en `expo-secure-store`.
- Alta solo por invitación (sin registro público). Contraseñas BCrypt (10–72 caracteres, con letra y número). Bloqueo tras 5 intentos fallidos por 15 min + limitador por IP.
- **Sesión revocable (ADR 0009):** el claim `sid` (= `familyId`) se verifica en cada request contra la familia de refresh no revocada (cache `SESSION_CHECK_CACHE_TTL`, default 30 s). Token sin `sid` = 401 y el cliente hace refresh. Logout público por refresh token.

## Roles

| Rol | Quién | Cliente |
|---|---|---|
| `USER` | Usuario final | App móvil |
| `ADMIN` | Administración | Admin-web |

Un usuario tiene un único rol. Para sumar un rol: agregarlo a `Role`, crear su meta-anotación en `common/security`, la audiencia por prefijo en `SecurityConfig`, y actualizar esta tabla y la matriz.

## Meta-anotaciones (`common/security`)

Todo controller lleva una a **nivel de clase** (puede refinarse por método).

| Anotación | Equivale a |
|---|---|
| `@AdminOnly` | `@PreAuthorize("hasRole('ADMIN')")` |
| `@UserOnly` | `@PreAuthorize("hasRole('USER')")` |
| `@AdminOrUser` | `@PreAuthorize("hasAnyRole('ADMIN','USER')")` |
| `@Authenticated` | `@PreAuthorize("isAuthenticated()")` |

## Whitelist pública (exacta)

Todo lo que no esté acá exige JWT válido.

| Método | Ruta | Perfil |
|---|---|---|
| POST | `/api/v1/auth/login` | todos |
| POST | `/api/v1/auth/refresh` | todos |
| POST | `/api/v1/auth/invitations/accept` | todos |
| POST | `/api/v1/auth/logout` (identifica la sesión por el refresh; ignora `Authorization`) | todos |
| POST | `/api/v1/auth/password-reset/request` | todos |
| POST | `/api/v1/auth/password-reset/confirm` | todos |
| GET | `/actuator/health`, `/actuator/health/**` | todos |
| GET | `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` | **solo `dev`** |

Agregar una ruta pública requiere actualizar este archivo y `SecurityConfig` en el mismo cambio. Los POST públicos ignoran el header `Authorization` (`PublicPostBearerTokenResolver`): un Bearer vencido o inválido no los convierte en 401.

## Audiencias por prefijo

Se aplican en `SecurityFilterChain` (primera barrera, con test de 403 por prefijo en `SecurityIntegrationTest`) y se repiten con meta-anotaciones en cada controller.

| Prefijo | Roles |
|---|---|
| `/api/v1/admin/**` | ADMIN |
| `/api/v1/me/**` | USER — el id **siempre** sale del JWT, nunca de path/body |
| `GET /api/v1/auth/me`, `PUT /api/v1/auth/password` | cualquier autenticado |

## Control de pertenencia

- El service recibe el userId del principal autenticado (`@CurrentUser` / `SecurityUtils`), nunca del request.
- Endpoints `/me/**` solo operan sobre recursos del usuario actual; acceder a un recurso ajeno responde **404** (no revela existencia).
- Las consultas filtran por dueño en la propia query (método derivado / Specification), no en memoria después.

## Matriz endpoint → roles

Se completa módulo por módulo. Estado: *planificado* / *implementado*.

### auth

| Método | Ruta | Roles | Estado |
|---|---|---|---|
| POST | `/api/v1/auth/login` | público | implementado |
| POST | `/api/v1/auth/refresh` | público (+ `X-Requested-With` si viene por cookie) | implementado |
| POST | `/api/v1/auth/invitations/accept` | público | implementado |
| POST | `/api/v1/auth/logout` (revoca la familia del refresh; 204 siempre) | público | implementado |
| POST | `/api/v1/auth/password-reset/request` `{email}` (204 siempre; mail solo a cuentas ACTIVE; 429 por IP) | público | implementado |
| POST | `/api/v1/auth/password-reset/confirm` `{token, newPassword}` (204; 422 enlace inválido/usado/vencido; revoca todas las sesiones y desbloquea) | público | implementado |
| GET | `/api/v1/auth/me` | autenticado | implementado |
| PUT | `/api/v1/auth/password` `{currentPassword, newPassword}` (204; 422 actual incorrecta o igual a la actual; 429 tras 5 fallos; revoca las demás sesiones) | autenticado | implementado |

### users

| Método | Ruta | Roles | Estado |
|---|---|---|---|
| GET | `/api/v1/admin/users` (paginado; filtros `q`, `role`, `status`) | ADMIN | implementado |
| GET | `/api/v1/admin/users/{id}` | ADMIN | implementado |
| POST | `/api/v1/admin/users` (alta + invitación) | ADMIN | implementado |
| PATCH | `/api/v1/admin/users/{id}` (nombre, apellido, teléfono; email y rol no se editan) | ADMIN | implementado |
| POST | `/api/v1/admin/users/{id}/invitation` (reenviar; 422 si no está INVITED; 204) | ADMIN | implementado |
| POST | `/api/v1/admin/users/{id}/disable` / `/enable` (disable revoca sesiones; no a sí mismo, 422) | ADMIN | implementado |

## Reglas transversales

- Respuestas de error sin detalles internos (500 genérico). Logs sin tokens, contraseñas ni emails completos.
- Rate limiting en login y restablecer contraseña (en memoria; con varias instancias, moverlo a un store compartido).
- Headers de seguridad en nginx (`infra/docker/admin-web/security-headers.conf`); `Referrer-Policy: no-referrer` por los tokens en la URL de `/restablecer`.
- Secretos solo por variables de entorno (`JWT_SECRET` Base64 de >= 32 bytes, validado al arrancar).
