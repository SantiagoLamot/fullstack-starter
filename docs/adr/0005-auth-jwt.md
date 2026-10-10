# ADR 0005 — Autenticación con JWT + refresh token rotativo

- **Estado:** aceptado
- **Fecha:** 29/09/2026

## Contexto

Dos clientes (admin-web y app móvil) contra una API stateless. Sin registro público: el admin da de alta usuarios por invitación. Un único backend emite y consume los tokens. Roles base: ADMIN y USER (se pueden sumar más).

## Decisión

### Tokens

- **Spring Security OAuth2 Resource Server** con `NimbusJwtEncoder` / `NimbusJwtDecoder`, **HS256**.
- Secreto en `JWT_SECRET` (Base64). Al arrancar se valida que decodificado tenga **≥ 256 bits**; si no, la app no levanta.
- **Access token:** 15 min (`JWT_ACCESS_TOKEN_TTL`). Claims: `sub` = userId, `roles`, `iss` (`JWT_ISSUER`), `iat`, `exp`.
- **Refresh token:** valor opaco aleatorio (≥ 256 bits, `SecureRandom`), TTL `JWT_REFRESH_TOKEN_TTL` (30 días por defecto).
  - Persistido **hasheado con SHA-256** (nunca en claro).
  - **Rotación en cada uso:** refresh válido ⇒ se revoca y se emite uno nuevo de la misma `familyId`.
  - **Detección de reuso:** si llega un refresh ya revocado ⇒ se revoca **toda la familia** (posible robo) y se exige login.
  - Se revoca en logout (la familia actual) y al desactivar al usuario (todas sus familias).

### Clientes

- **Admin-web:** access token solo en memoria. Refresh en cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`. El endpoint de refresh exige además un **header custom** (p. ej. `X-Requested-With`) como defensa CSRF extra. Admin-web habla con la API **por mismo origen** (proxy de Vite en dev, nginx en prod) ⇒ no hace falta CORS con credenciales.
- **Mobile:** refresh en el body de login/refresh; se guarda en `expo-secure-store`. Access en memoria.

### Invitación y activación

1. Admin crea el usuario ⇒ `status = INVITED`, sin contraseña.
2. Se genera token de **un solo uso**, vigencia **72 h** (`INVITATION_TTL`), guardado hasheado.
3. Se envía mail (Mailpit en dev) con link a la página pública del admin-web `/activar?token=…` (funciona desde el navegador del celular).
4. El usuario define contraseña (`POST /api/v1/auth/invitations/accept`) ⇒ `status = ACTIVE`, token marcado como usado.
5. Reenviar invitación invalida los tokens anteriores.

### Contraseñas y fuerza bruta

- **BCrypt**.
- Por usuario: `failedLoginAttempts` + `lockedUntil` (**5 intentos / 15 min**, configurable con `LOGIN_MAX_ATTEMPTS`, `LOGIN_LOCK_DURATION`).
- Por IP: limitador en memoria (válido mientras haya una sola instancia).
- Mensajes de error de login genéricos (no revelan si el email existe).

## Alternativas consideradas

- **Sesiones con estado (JSESSIONID):** simple para web, incómodo para mobile y agrega estado al servidor. Descartado.
- **Cookie también en mobile:** manejo de cookies en RN es frágil; secure-store es el estándar. Descartado.
- **Código de activación dentro de la app:** agrega pantallas y flujo en mobile; el link web sirve para todos los roles. Descartado.
- **RS256:** útil con varios consumidores o emisor externo; hoy hay un único emisor/consumidor. Descartado por ahora (migrable sin cambiar clientes).
- **Refresh JWT (no opaco):** no revocable sin estado de todas formas. Descartado.

## Consecuencias

- Tablas `refresh_token` e `invitation_token` (ver `docs/domain.md`); conviene purgar expirados periódicamente.
- El limitador por IP en memoria deja de ser suficiente si se escala a varias instancias (pasaría a Redis/DB).
- Pendiente: SMTP real y dominio remitente (ver `docs/scope.md`).
- Detalle de endpoints y whitelist en `docs/security.md`.
