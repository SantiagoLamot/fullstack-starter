# fullstack-starter — reglas globales del repo

Plantilla de monorepo: app móvil (Expo), panel admin web (React) y backend (Spring Boot), con el slice de referencia auth + users listo. Al arrancar un proyecto nuevo, este archivo se conserva (se ajusta el nombre del producto) y es la guía de todos los agentes.
**Fuente de verdad del alcance:** `docs/scope.md`. Si algo no está ahí, NO se implementa: se anota como pregunta abierta para el cliente.

## Mapa del repo

| Ruta | Qué es | CLAUDE.md propio |
|---|---|---|
| `apps/backend` | Java 21 + Spring Boot 4.1 (Maven wrapper), monolito modular | sí |
| `apps/admin-web` | React 19.2 + TS 6 + Vite 8 + Mantine 9 | sí |
| `apps/mobile` | Expo SDK 57 (RN 0.86, React 19.2) + TS 6 + expo-router | sí |
| `packages/api-types` | Tipos TS generados desde el OpenAPI del backend (no editar a mano) | — |
| `infra/docker` | Dockerfiles, nginx, entrypoints de dev, config MySQL | — |
| `scripts` | Helpers `.sh` (Mac) y `.ps1` (Windows) con idéntico comportamiento | — |
| `docs` | roadmap (fases), architecture, security (matriz endpoint→rol), api, domain, scope, `adr/`, `agents/` | — |
| `docs/brand` | **Guía de marca** (`brand.md`, fuente única de UI para ambos frontends) | — |
| `.claude/agents` | Subagentes del proyecto | — |

## Reglas no negociables

- **Todo corre en Docker.** El host solo necesita Docker y git. Nunca pedir instalar Java/Maven/Node en el host. Para correr herramientas usar `docker compose run`/`exec` o `docker run` con imágenes oficiales.
- **Multiplataforma.** LF forzado (`.gitattributes`). Todo watcher respeta `DEV_FILE_POLLING` (`true` en Windows, `false` en Mac). Todo script existe en `.sh` y `.ps1`.
- **Memoria solo dentro del repo.** Nada en `~/.claude/`. Cada agente mantiene `docs/agents/<nombre>.md`.
- **Secretos** solo por variables de entorno. Nunca commitear `.env`, claves ni tokens. `.env.example` siempre completo y comentado.
- **Idioma:** código, identificadores y commits en inglés. Documentación, mensajes al usuario final y textos de UI en español.
- **Commits:** Conventional Commits (`feat(backend): ...`, `chore(infra): ...`), chicos, al cerrar cada tarea coherente.
- **Nada "funciona" sin haberlo corrido:** build, tests, lint y contenedores arriba.
- **Contrato de API:** el OpenAPI generado por el backend es la fuente de verdad. Tras cambiar la API: `scripts/gen-api-types.(sh|ps1)` y commitear `packages/api-types`.

## Backend (resumen; detalle en `apps/backend/CLAUDE.md`)

- **Package by feature** (monolito modular): paquete raíz `com.example.app` (placeholder: se renombra al crear el proyecto, ver README); `config/`, `common/`, y cada feature en `modules/<feature>/` con sus capas adentro `{controller,service,repository,entity,dto/request,dto/response,mapper,exception}`. Nunca carpetas globales por capa.
- **Regla fija:** las entidades JPA y sus enums van SIEMPRE en `modules/<feature>/entity/`. Nunca crear carpetas `domain/` (ni `model/`, `entities/`).
- Controller → Service → Repository (3 capas, SOLID: responsabilidad única por clase, dependencias por constructor). Entrada y salida siempre por DTO; entidades nunca salen por la API. Un módulo no toca el repository de otro.
- **Persistencia = Hibernate, siempre:** ORM de JPA vía Spring Data JPA. No se reemplaza por otro ORM ni por acceso a datos sin ORM (MyBatis, jOOQ, JDBC, etc.).
- **JPA puro:** métodos derivados, Specifications, `@EntityGraph`. **Prohibido** SQL nativo, `@Query(nativeQuery=true)`, `JdbcTemplate`.
- Esquema por Hibernate `ddl-auto=update` (pendiente: Flyway + `validate` antes de producción, ver ADR 0001).
- Todo request DTO es `record` con Bean Validation; `@Valid` + `@Validated`. Errores como ProblemDetail (RFC 9457) con lista `errors[{field,message}]`.
- Todo endpoint exige JWT salvo la whitelist de `docs/security.md`. Todo controller tiene `@PreAuthorize` (vía meta-anotaciones) a nivel de clase. El usuario actual sale SIEMPRE del JWT.
- API bajo `/api/v1`; audiencias `/admin/**` (ADMIN) y `/me/**` (USER). Roles base: ADMIN y USER.

## Frontends (resumen)

- TS `strict`, ESLint + Prettier, alias `@/`. Organización por features (`features/<f>/{api,components,hooks,pages,schemas,types,index.ts}`); una feature importa de otra solo vía su `index.ts`.
- Formularios react-hook-form + zod; errores 400 del backend mapeados a campos.
- axios con interceptor: Bearer, refresh único ante 401 con cola, logout si falla.
- Guards por rol; la seguridad real la aplica el backend.
- **Marca:** toda UI sigue `docs/brand/brand.md` (tokens de tema listos para reemplazar). Mobile es la experiencia de marca y admin la versión sobria.
- **Responsive siempre (web):** toda pantalla se ve bien en 360, 768, 1024, 1280 y 1920 px, sin scroll horizontal de página. El header usa burger + drawer en mobile.
- **Sin zoom (web):** viewport `width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no`; todo input, textarea y select con `font-size` ≥ 16px (global, desde el tema de Mantine); `touch-action: manipulation` en `body`. En apps nativas (Expo / React Native) no aplica.

## Flujo de trabajo con agentes

- El orquestador planifica, delega, integra y revisa. Los subagentes implementan. Toda entrega pasa por `qa-reviewer`.
- **Regla fija: todo lo que el usuario le pide al orquestador se delega a un agente.** El orquestador NO escribe código, specs ni documentación por su cuenta (ni `docs/`, ni `scope.md`, ni `roadmap.md`): solo lee lo mínimo para armar el prompt, delega al agente que corresponda (`architect` para specs y decisiones, `docs-writer` para docs, `market-researcher` para investigación de mercado y viabilidad, `backend-dev`/`mobile-dev`/`admin-web-dev`/`db-engineer`/`devops` para código, `qa-reviewer` para revisar) e integra el resultado. Única excepción: este archivo, cuando el usuario pide registrar una regla.
- Las mejoras VIABLES de `market-researcher` entran a `docs/scope.md` con id R# y se implementan como cualquier ítem del alcance, pasando por `qa-reviewer`.
- Cambios de estructura o convenciones globales ⇒ actualizar este archivo y `docs/architecture.md`.
- Versiones fijadas: ver `docs/adr/0001-stack.md`.
- **Arbitraje autónomo:** cuando el usuario pide avanzar las fases sin frenar, el orquestador encadena fase tras fase y consulta a `arbiter` en vez de preguntarle. `arbiter` decide y registra en `docs/decisions-log.md`; sus límites (lo reservado al dueño) están en `.claude/agents/arbiter.md`. Lo reservado se salta y se reporta al final; nunca se hace push ni deploy sin pedido explícito.
- **Alcance:** el brief va a `docs/scope.md` (preguntas abiertas P# y supuestos vigentes). Roadmap por fases en `docs/roadmap.md`: cada fase va backend primero y frontends en paralelo.

## Uso eficiente de la sesión (límite de uso del plan)

- **Modelos:** Sonnet por defecto: la sesión del orquestador (`model: sonnet` en `.claude/settings.json`) y todos los agentes, porque los módulos siguientes imitan la plantilla. Opus solo en estos casos, y el orquestador lo pide explícitamente al lanzar el agente: `arbiter` (siempre Opus), el slice de referencia, ADR y decisiones de arquitectura, y la QA de cierre de las fases con dinero, fórmulas o aislamiento entre inmobiliarias (pagos, contratos, motor de ajustes, recálculo, liquidación, endurecimiento). La QA de fases de solo pantallas y listados va con Sonnet.
- **Prompts cerrados:** cada delegación indica tarea, archivos a leer y criterio de terminado, para que el agente no explore de más.
- **Reusar antes que relanzar:** correcciones al mismo agente vía SendMessage (conserva su contexto) en vez de un agente nuevo en frío.
- **Paralelismo con criterio:** solo tareas realmente independientes; no más de 3–4 agentes a la vez.
- **Salidas cortas:** comandos en modo quiet, `tail` de logs, solo el resumen de tests. Nunca volcar logs completos al contexto.
- **QA por bloque:** una revisión al cerrar cada módulo/fase, no por micro-entrega.
- **Sesión nueva por fase (`/clear`):** todo el conocimiento vive en el repo (`docs/roadmap.md`, `docs/specs/`, `docs/scope.md`, `docs/agents/*.md`). Hacer `/clear` al cerrar cada fase (todo commiteado y marcado en el roadmap), a mitad de una fase grande, al cambiar de tema o cuando la conversación se vuelve larga. Nunca con agentes corriendo en segundo plano ni con cambios a medio commitear. Para retomar: "Sos el orquestador. Seguimos con la Fase X según `docs/roadmap.md`".
- **Regla fija: cortar antes de cambiar de tema.** `/clear` lo ejecuta el usuario: el orquestador no puede correrlo. Si un pedido nuevo cambia de tema o de fase y arrastra contexto que ya no sirve, ANTES de leer archivos o delegar, el orquestador deja el estado en `docs/agents/*.md`, avisa qué falta commitear y responde solo con el pedido de `/clear` y el prompt de retomada listo para pegar (incluye el pedido nuevo). No aplica a preguntas puntuales que se responden sin explorar, ni si el usuario pide seguir sin limpiar. Con agentes en segundo plano, primero espera a que terminen.
- **Si el usuario avisa que el uso está alto:** no lanzar agentes nuevos, dejar todo commiteado y el estado anotado en `docs/agents/*.md` para retomar.
