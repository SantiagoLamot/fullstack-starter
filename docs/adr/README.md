# Architecture Decision Records

Formato: Contexto, Decisión, Alternativas consideradas, Consecuencias. Numeración `NNNN-titulo.md`; un ADR aceptado no se reescribe: si cambia la decisión, se crea uno nuevo que lo reemplaza (y se marca el viejo como "reemplazado por NNNN"). Los ADR de la plantilla son el punto de partida: ajustalos o reemplazalos según el proyecto.

| # | Título | Estado |
|---|---|---|
| [0001](0001-stack.md) | Stack y versiones fijadas | aceptado |
| [0002](0002-backend-modular-monolith.md) | Backend como monolito modular (package-by-feature) | aceptado |
| [0003](0003-monorepo-pnpm.md) | Monorepo con pnpm workspaces | aceptado |
| [0004](0004-schema-hibernate-ddl-update.md) | Esquema con Hibernate `ddl-auto=update` (transitorio) | aceptado — Flyway pendiente antes de producción |
| [0005](0005-auth-jwt.md) | Autenticación con JWT + refresh token rotativo | aceptado |
| [0006](0006-ui-libraries.md) | Librerías de UI | aceptado |
| [0007](0007-dev-environment-docker.md) | Entorno de desarrollo 100 % en Docker | aceptado |
| [0009](0009-session-revocation.md) | Revocación inmediata de sesiones (claim `sid`) | aceptado e implementado |

Nota: la numeración salta el 0008 a propósito (reservado en el proyecto de origen); el próximo ADR nuevo es el 0010.
