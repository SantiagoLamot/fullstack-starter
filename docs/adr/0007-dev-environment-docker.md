# ADR 0007 — Entorno de desarrollo 100 % en Docker

- **Estado:** aceptado
- **Fecha:** 29/09/2026

## Contexto

Se desarrolla en Windows y Mac. Queremos que el host solo necesite Docker y git, sin instalar Java, Maven ni Node, y con hot reload en todas las apps. En Windows los bind mounts (desde el filesystem de Windows) no propagan eventos de cambio de archivos a Linux, así que los watchers basados en eventos no se enteran.

## Decisión

- **Todo corre en Docker**, incluidas herramientas puntuales (`docker compose run`/`exec`).
- **Compose base + override:**
  - `docker-compose.yml`: base, cercano a producción (backend empaquetado, admin-web servido por nginx, MySQL).
  - `docker-compose.override.yml`: dev (bind mounts, hot reload, debug remoto 5005, Vite, Metro, Mailpit, Adminer, puertos expuestos).
  - `name: <nombre del proyecto>` fijo en compose (nombres de contenedores/volúmenes estables sin importar la carpeta).
- **`DEV_FILE_POLLING`** (`true` en Windows, `false` en Mac; intervalo en `DEV_FILE_POLLING_INTERVAL_MS`) lo respetan **todos** los watchers:
  - **Backend:** `dev-entrypoint.sh` corre `spring-boot:run` (JDWP en `*:5005`) más un watcher propio (inotify con eventos, o polling por huella de mtimes si `DEV_FILE_POLLING=true`, con debounce) que ejecuta `./mvnw -q -o compile` y, recién si compila OK, toca un *trigger file* (`target/classes/.reloadtrigger`); `SPRING_DEVTOOLS_RESTART_TRIGGERFILE=.reloadtrigger` hace que DevTools reinicie una sola vez por compilación (sin ese trigger, DevTools reiniciaba dos veces: al tocarse cualquier `.class` y de nuevo al terminar Maven). El watcher arranca recién cuando la app ya responde HTTP, para no compilar en paralelo con el arranque inicial. `/app/target` vive en un volumen nombrado (`backend-target`), no en el bind mount: I/O nativo en vez de bind mount de Windows, y no se pisa con builds del IDE del host. Cambios en `pom.xml` no los recompila el watcher: hace falta `docker compose restart backend`.
  - **admin-web:** Vite con `server.watch.usePolling` / `CHOKIDAR_USEPOLLING` según la variable (ver `x-node-dev-env` en `docker-compose.override.yml`).
  - **mobile (Metro):** Metro no tiene modo polling propio (`@expo/metro-file-map` depende enteramente de inotify) y `/repo` esta montado por 9p, que no propaga notificaciones de cambio ni siquiera para ediciones hechas desde dentro del contenedor (verificado). Mitigacion real: `infra/docker/mobile/poll-bridge.sh` sondea por `stat` (funciona sobre 9p) y hace que `dev-entrypoint.sh` reinicie el *proceso* `expo start` (no el contenedor) cuando detecta cambios; verificado de punta a punta en Windows. `docker compose restart mobile` (recrawl completo) sigue como respaldo. Detalle en `docs/agents/devops.md` y `apps/mobile/CLAUDE.md`.
- **Scripts** en `scripts/` con versión `.sh` y `.ps1` de comportamiento idéntico; ambos hacen `cd` a la raíz del repo. `.sh` compatible con bash 3.2 (arrays vacíos con `${a[@]+"${a[@]}"}`, sin bashismos recientes) y con bit `+x` en el índice de git. `.ps1` para PowerShell 5.1, **solo ASCII** (PS 5.1 lee `.ps1` UTF-8 sin BOM como ANSI y puede corromper acentos/guiones largos y romper el parseo) y sin `&&`/`||` (no existen en 5.1).
- **LF forzado** vía `.gitattributes` (y `.editorconfig`), para que scripts y entrypoints funcionen dentro de Linux aunque se editen en Windows.
- **MySQL configurado por flags en `command:`** (no con `conf.d` montado): en Windows los bind mounts son world-writable y MySQL ignora `.cnf` con esos permisos. El healthcheck usa `-h 127.0.0.1` para forzar TCP, porque durante la inicialización el server temporal no escucha por red y daría "healthy" antes de tiempo.
- **`ports: !override`** en `admin-web` (Compose ≥ 2.24): en dev solo se publica el puerto de Vite (5173); el 8088 de nginx de la base no se publica (no tiene sentido en dev, ese build ni siquiera corre).
- **Servicios auxiliares en el profile `tools`** (no arrancan con `docker compose up`, solo bajo demanda vía `--profile tools run`): `node-tools` (imagen `node:24-bookworm-slim` genérica, la usa `gen-api-types`) y `backend-test` (Maven `verify` con Testcontainers: monta `/var/run/docker.sock` y usa `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` para que los contenedores de test alcancen al Docker del host/Desktop).

## Alternativas consideradas

- **Toolchains instaladas en el host:** divergencias de versión entre máquinas. Descartado.
- **Dev Containers:** buena opción, pero ata el flujo a VS Code; compose sirve con cualquier editor.
- **Polling siempre:** más CPU en Mac sin necesidad. Se deja por variable.

## Consecuencias

- Primer arranque lento (descarga de imágenes y dependencias); se cachean `~/.m2` y el store de pnpm en volúmenes.
- Mobile en dispositivo físico necesita `REACT_NATIVE_PACKAGER_HOSTNAME` con la IP LAN (o modo tunnel) y `EXPO_PUBLIC_API_URL` apuntando a la IP LAN.
- Metro con polling: resuelto con el puente de reinicio de proceso (ver Decision). Si en el futuro dejara de ser confiable, WSL2 pasa a ser requisito para quien trabaje en mobile desde Windows (se registraria en este ADR).
