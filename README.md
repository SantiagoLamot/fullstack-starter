# fullstack-starter

Plantilla de monorepo para arrancar una app nueva con el mismo stack y la misma forma de trabajo: **backend** Spring Boot 4.1 (Java 21, JPA puro, 3 capas, package by feature) + **admin web** React 19 / Vite / Mantine + **app móvil** Expo (React Native), todo dockerizado, con JWT + refresh rotativo, y un equipo de agentes de Claude Code (orquestador + especialistas) con memoria dentro del repo.

Ya incluye el **slice de referencia auth + users**: invitación y activación de cuentas, login, refresh rotativo, logout con revocación de sesión, restablecer y cambiar contraseña, roles `ADMIN` / `USER`, ABM de usuarios en el admin, mail por SMTP (Mailpit en dev) y seed de dev. No incluye nada del negocio: eso lo define el brief de cada proyecto (`docs/scope.md`).

Reglas del repo: [`CLAUDE.md`](CLAUDE.md). Arquitectura: [`docs/architecture.md`](docs/architecture.md). Seguridad: [`docs/security.md`](docs/security.md). Decisiones: [`docs/adr/`](docs/adr/README.md).

## Requisitos

Solo **Docker Desktop** y **git**. Nada de Java, Maven o Node en el host: todo corre en contenedores. En Windows, `DEV_FILE_POLLING=true` (lo ajusta `dev-up`); en Mac, `false`.

## Empezar un proyecto nuevo desde la plantilla

1. **Crear el repo** desde la plantilla (en GitHub: "Use this template") o clonarla y reiniciar la historia (`rm -rf .git && git init -b main`).
2. **Renombrar** (buscar y reemplazar; todo en inglés salvo textos de UI):
   - Backend: paquete `com.example.app` (carpetas `apps/backend/src/{main,test}/java/com/example/app`), `groupId`/`artifactId` en `pom.xml`, `spring.application.name` y base de datos en `application.yml`, `JWT_ISSUER`, prefijo `urn:app:problem:` (`ProblemType`).
   - Nombre visible: `APP_NAME` (mails del backend), `VITE_APP_NAME` y `<title>` en `apps/admin-web/index.html`, `EXPO_PUBLIC_APP_NAME`, `name`/`slug`/`scheme`/`bundleIdentifier`/`package` en `apps/mobile/app.config.ts`.
   - Workspaces: scope `@app/*` en los `package.json` y `pnpm-workspace.yaml`, y en los Dockerfiles (`--filter "@app/..."`).
   - Docker: `name:` y nombres de red/volúmenes en `docker-compose*.yml`, `MYSQL_DATABASE`/`MYSQL_USER` en `.env.example`.
3. **Configurar**: `cp .env.example .env` (o dejar que `dev-up` lo cree) y revisar puertos, contraseñas y `DEV_SEED_*`.
4. **Levantar**: `scripts/dev-up.sh` (Mac/Linux/WSL2) o `scripts\dev-up.ps1` (Windows). Equivale a `docker compose up --build -d` + `.env` + `JWT_SECRET` + IP LAN para el celular.
5. **Generar tipos**: con el backend arriba, `scripts/gen-api-types.(sh|ps1)` y commitear `packages/api-types` (el repo ya trae los tipos del slice).
6. **Probar**: admin en http://localhost:5173, login con `DEV_SEED_ADMIN_EMAIL` / `DEV_SEED_ADMIN_PASSWORD` (valores de `.env.example`, descomentados en tu `.env`). Mailpit en http://localhost:8025.
7. **Arrancar con el orquestador** (prompt abajo).

### Prompt de arranque

> Sos el orquestador. Leé `CLAUDE.md` y `README.md`. El brief es este: […]. Armá `docs/scope.md` (con las preguntas abiertas P# y los supuestos vigentes) y el roadmap por fases en `docs/roadmap.md`. Delegá todo a los agentes con prompts cerrados (tarea, archivos a leer, criterio de terminado). Cada fase: backend primero, frontends en paralelo, QA al cierre y `/clear`.

## Servicios y puertos (desarrollo)

| Servicio | URL / puerto | Variable |
|---|---|---|
| Backend API | http://localhost:8080/api/v1 (Swagger en `/swagger-ui.html`, solo dev) | `BACKEND_HOST_PORT` |
| Debug remoto (JDWP) | 127.0.0.1:5005 | `BACKEND_DEBUG_PORT` |
| Admin-web (Vite, HMR) | http://localhost:5173 | `ADMIN_WEB_DEV_HOST_PORT` |
| Admin-web (nginx, tipo prod) | http://localhost:8088 | `ADMIN_WEB_HOST_PORT` |
| Metro / Expo | http://localhost:8081 | `EXPO_HOST_PORT` |
| Mailpit | http://localhost:8025 | `MAILPIT_UI_HOST_PORT` |
| Adminer | http://localhost:8089 | `ADMINER_HOST_PORT` |
| MySQL | 127.0.0.1:3306 | `MYSQL_HOST_PORT` |

Si un puerto está ocupado, cambialo en tu `.env` (no se versiona).

## Scripts (`scripts/`, cada uno en `.sh` y `.ps1`)

`dev-up`, `dev-down`, `logs`, `backend-test` (Maven `verify` con Testcontainers), `gen-api-types`, `gen-secret`, `reset-db`.

## Tests y checks (siempre en Docker)

- Backend: `scripts/backend-test.sh` (acepta args de Maven, p. ej. `-Dtest=FooTest`). Sin `-q` para ver el resumen de tests.
- admin-web: `docker compose exec admin-web pnpm --filter @app/admin-web <lint|typecheck|test|build>`.
- mobile: `docker compose exec -T mobile sh -c "cd apps/mobile && pnpm <lint|typecheck|test>"`.

## Probar la app en el celular (Expo Go)

`dev-up` detecta la IP LAN y arma `REACT_NATIVE_PACKAGER_HOSTNAME` y `EXPO_PUBLIC_API_URL` (puerto del backend incluido). En Expo Go: "Enter URL manually" → `exp://<IP>:8081`. En Windows el celular debe estar en la misma Wi-Fi con el firewall abierto para 8081 y el puerto del backend (red privada). `EXPO_TUNNEL=true` si no comparten red. Para iOS con Expo Go hace falta `EXPO_TOKEN` (token personal de la cuenta Expo, solo en `.env`).

## Errores a no repetir

Aprendidos en el proyecto de origen:

- **Colisión de nombres de DTO en OpenAPI:** springdoc usa el nombre simple de la clase; dos DTO con el mismo nombre en módulos distintos se pisan. Prefijar con el dominio (`CreateUserRequest`). Lo verifica `DtoNamesUniqueTest`.
- **`node_modules` en volúmenes nombrados:** nunca borrarlos desde el host con el stack arriba (los mountpoints quedan "fantasma" y `pnpm install` falla con `File exists (os error 17)`); borrar desde un contenedor. admin-web y mobile guardan un hash del lockfile y limpian solos si cambia.
- **Polling en Windows (9p):** los bind mounts de Windows no emiten inotify, ni siquiera desde dentro del contenedor. Por eso `DEV_FILE_POLLING=true`, el watcher por huella del backend y el puente de polling de Metro (`infra/docker/mobile/poll-bridge.sh`, que reinicia el proceso `expo start`). MySQL se configura por flags y no por `conf.d` montado (en Windows lo ignora por permisos).
- **Expo Go en iOS:** exige sesión en la app y en el CLI; sin `EXPO_TOKEN` personal en `.env` no abre. Metro anuncia siempre el puerto 8081 al celular: no cambiar `EXPO_HOST_PORT`.
- **React duplicado (Metro y jest):** admin-web usa React 19.2.8 y mobile 19.2.3 (Expo lo fija). `metro.config.js` fuerza una sola copia en el bundle y `jest.config.js` mapea `react` a la raíz; no quitarlo.
- **pnpm 12 y scripts nativos:** `ERR_PNPM_IGNORED_BUILDS` si falta aprobar un paquete con build nativo: agregarlo a `allowBuilds` en `pnpm-workspace.yaml`. Los paquetes de Expo recién publicados caen en `minimumReleaseAgeExclude`.
- **Maven en Docker:** no usar `docker run` suelto para el volumen `m2` (Compose avisa que no lo creó); usar `scripts/backend-test.*`. Con `-q` Maven no imprime el resumen de tests. Cambios en `pom.xml` requieren `docker compose restart backend`.
- **Records de request con `boolean` primitivo:** fallan con 400 si el campo falta (Jackson 3); usar `Boolean`.
- **Tests con token real:** el access token lleva `sid`; el decoder responde 401 si la sesión no está vigente. Crear la familia con `RefreshTokenService.startFamily(...)`.
- **Scripts `.ps1`:** solo ASCII (PowerShell 5.1 lee UTF-8 sin BOM como ANSI), sin `&&`, y no redirigir stderr de ejecutables nativos con `$ErrorActionPreference='Stop'`. En `.sh`: `set -o pipefail` rompe `cmd | grep -q` (usar `{ cmd || true; } | grep`) y Git Bash reescribe rutas de `docker run -v` (`MSYS_NO_PATHCONV=1`).
- **Heredocs largos en la herramienta Bash de Claude Code:** pueden romperse por comillas o apóstrofos en la ruta o en el texto; para archivos largos usar la herramienta Write. `curl` en Git Bash rompe UTF-8 en bodies JSON: usar `--data-binary @archivo.json`.
- **nginx:** `X-Forwarded-For $remote_addr` (no `$proxy_add_x_forwarded_for`, permite evadir el rate limit por IP) y `Referrer-Policy: no-referrer` (hay tokens en la URL de `/activar` y `/restablecer`).
- **Web sin zoom y responsive:** viewport con `maximum-scale=1, user-scalable=no`, inputs >= 16px desde el tema de Mantine y `touch-action: manipulation`; header con burger + drawer en mobile. Revisar 360/768/1024/1280/1920 px.
- **Colisión de puertos con otros proyectos en la misma máquina:** cambiarlos en `.env`; el celular necesita que `EXPO_PUBLIC_API_URL` use el mismo puerto del backend.

## Flujo de trabajo con IA

Abrí Claude Code en la raíz. Los agentes están en `.claude/agents/` y sus memorias en `docs/agents/` (viajan en git). El orquestador no escribe código: delega. Modelos: Opus para arquitectura y slice de referencia, Sonnet para el resto. Detalle en [`CLAUDE.md`](CLAUDE.md).
