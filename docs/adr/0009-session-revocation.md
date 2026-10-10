# ADR 0009 — Revocación inmediata de sesiones (claim `sid`)

- **Estado:** aceptado e implementado (03/10/2026)
- **Fecha:** 03/10/2026
- **Complementa:** [ADR 0005](0005-auth-jwt.md) (no lo reemplaza: tokens, rotación y clientes siguen igual)

## Contexto

Con ADR 0005 el access token (15 min) se valida solo por firma, emisor y expiración. Consecuencias observadas:

- En admin-web, cerrar sesión con el access token vencido no revocaba nada (el logout exigía JWT) y F5 volvía a entrar con la cookie de refresh.
- Aun con un logout correcto, un access token emitido seguía sirviendo hasta 15 min; lo mismo al deshabilitar un usuario o al pedir la baja.
- Santiago pidió (03/10/2026) un logout real y que restablecer o cambiar la contraseña revoque las sesiones. Spec: `docs/specs/password-auth.md`.

## Decisión

- El access token lleva el claim **`sid`** = `familyId` de la familia de refresh que lo originó.
- Un `OAuth2TokenValidator<Jwt>` en el `JwtDecoder` verifica en **cada request** que la familia tenga un refresh no revocado y no vencido (método derivado sobre `refresh_token`, índice `(family_id, revoked_at)`). Si no, 401.
- `config` depende de un puerto en `common/security` (`SessionValidityChecker`) que implementa el módulo `auth`.
- Cache en memoria de resultados positivos con TTL corta (`SESSION_CHECK_CACHE_TTL`, 30 s) e **invalidación explícita después del commit** de cada revocación ⇒ revocación inmediata con una instancia.
- `POST /auth/logout` pasa a ser público: identifica la sesión por el refresh token (cookie o body), no por el access token.
- Cambio de contraseña revoca todas las familias salvo la actual; restablecer revoca todas.

## Alternativas consideradas

- **`tokenVersion` en `User` (claim `ver`):** una consulta por usuario, simple; pero revoca todas las sesiones a la vez, no permite cerrar solo la sesión que hace logout ni conservar la actual al cambiar la contraseña. Descartado.
- **Lista negra de `jti` revocados:** solo cubre los tokens que se conocen al revocar (el logout no conoce los access tokens emitidos a otras pestañas); necesita guardar cada `jti`. Descartado.
- **Access token más corto (1–2 min) sin validación:** reduce la ventana pero no la elimina y multiplica los refresh. Descartado.
- **Sesiones con estado en servidor (JSESSIONID):** ya descartado en ADR 0005.
- **Tabla propia de sesiones (`auth_session`):** más clara a futuro (dispositivo, último uso), pero duplica lo que la familia de refresh ya representa. Descartado por ahora; migrable sin cambiar el contrato.

## Consecuencias

- La API deja de ser 100 % stateless en la validación: una consulta indexada por sesión cada 30 s como máximo (cache). Aceptable para el volumen previsto.
- Deshabilitar, pedir la baja o anonimizar corta el acceso al instante (antes, hasta 15 min).
- Con varias instancias, la cache local permitiría hasta `SESSION_CHECK_CACHE_TTL` de desfase tras una revocación; si se escala, bajar la TTL a 0 o mover la invalidación a un canal compartido.
- Los access tokens previos al despliegue (sin `sid`) reciben 401 una vez y el cliente se recupera con un refresh.
- Los endpoints públicos de auth ignoran el header `Authorization` (resolver propio), para que un Bearer vencido no rompa el logout.
