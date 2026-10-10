# Dominio

> Plantilla. Glosario y modelo de entidades del producto; lo mantiene el `architect`.

## Glosario

| Término | Definición |
|---|---|
| Usuario | Persona con cuenta. Rol `USER` (app móvil) o `ADMIN` (panel). Se crea por invitación. |
| Invitación | Token de un solo uso (hasheado en DB) enviado por mail para activar la cuenta y elegir contraseña. |
| Sesión | Familia de refresh tokens (`familyId`); su id viaja en el claim `sid` del access token. |

## Modelo de entidades inicial

### users
- `User`: email único, nombre, apellido, teléfono opcional, rol, estado (`INVITED`, `ACTIVE`, `DISABLED`), intentos fallidos y bloqueo. Hereda de `BaseEntity` (id, createdAt/By, updatedAt/By).

### auth
- `InvitationToken`, `PasswordResetToken`: hash del token, vencimiento, uso.
- `RefreshToken`: hash, `familyId`, vencimiento, revocación (rotación con detección de reuso).

### módulos del producto
- _Agregar acá cada módulo nuevo con sus entidades y relaciones._

## Sujeto a preguntas abiertas

_Decisiones de modelo que dependen de respuestas del cliente (ver `docs/scope.md`)._
