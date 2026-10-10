#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Mobile dev: pnpm install (filtrado) + expo start en 8081.
# Si apps/mobile todavía no existe, avisa y queda en espera sin romper el stack;
# arranca solo en cuanto aparezca apps/mobile/package.json.
# -----------------------------------------------------------------------------
set -euo pipefail
cd /repo

if [ ! -f apps/mobile/package.json ]; then
  echo "=============================================================================="
  echo "[mobile] apps/mobile/package.json no existe todavía."
  echo "[mobile] El servicio queda en espera (no afecta al resto del stack)."
  echo "[mobile] Arranca solo cuando aparezca la app, o reiniciá con:"
  echo "[mobile]   docker compose restart mobile"
  echo "=============================================================================="
  while [ ! -f apps/mobile/package.json ]; do sleep 15; done
  echo "[mobile] apps/mobile/package.json detectado, continuando."
fi

# Guarda un hash de pnpm-lock.yaml + pnpm-workspace.yaml dentro del propio
# volumen de node_modules (mobile-node-modules); si cambió (o es la primera
# vez), limpia el CONTENIDO de los volúmenes de node_modules de mobile (no el
# mountpoint) antes de reinstalar. Mismo guard que admin-web (ver
# docker-compose.override.yml) para no quedar con un layout de pnpm viejo que
# `pnpm install` normal no corrige solo porque el lockfile "ya está al día".
NEW_HASH="$(cat pnpm-lock.yaml pnpm-workspace.yaml 2>/dev/null | md5sum | cut -d' ' -f1)"
OLD_HASH="$(cat node_modules/.install-guard 2>/dev/null || true)"
if [ "$NEW_HASH" != "$OLD_HASH" ]; then
  echo "[mobile] pnpm-lock.yaml/pnpm-workspace.yaml cambiaron (o primer arranque del volumen): limpiando node_modules antes de reinstalar"
  for d in node_modules packages/api-types/node_modules; do
    find "$d" -mindepth 1 -maxdepth 1 -exec rm -rf {} + 2>/dev/null || true
  done
fi

pnpm install --filter "@app/mobile..." \
  --store-dir "${PNPM_STORE_DIR:-/pnpm/store}" --config.confirm-modules-purge=false
mkdir -p node_modules
echo "$NEW_HASH" > node_modules/.install-guard

args=(--port 8081)
if [ "${EXPO_TUNNEL:-false}" = "true" ]; then
  echo "[mobile] EXPO_TUNNEL=true -> expo start --tunnel"
  args+=(--tunnel)
fi

RESTART_REQUEST_FILE="/tmp/.mobile-metro-restart-request"
rm -f "$RESTART_REQUEST_FILE"

# -----------------------------------------------------------------------------
# Puente de polling (ver poll-bridge.sh): en este entorno /repo esta montado
# por 9p (Docker Desktop / WSL2), que no implementa notificacion de cambios en
# absoluto -> el watcher de Metro (inotify puro) nunca se entera de nada, ni
# siquiera de cambios hechos DENTRO del contenedor (verificado con un
# `fs.watch` de Node: cero eventos). No hay forma de "fabricar" un evento
# inotify en un mount que no los propaga. En cambio, poll-bridge.sh detecta
# cambios reales por polling (stat, no notificacion: eso si funciona sobre 9p,
# igual que el watcher del backend) y pide, dejando un archivo, que el
# supervisor de abajo reinicie el PROCESO `expo start` (no el contenedor):
# un `expo start` nuevo hace un recrawl completo y sirve el contenido
# actualizado. Solo se activa si DEV_FILE_POLLING=true (Windows); en Mac
# (false) no arranca y `expo start` corre una sola vez, sin supervisor.
# -----------------------------------------------------------------------------
if [ "${DEV_FILE_POLLING:-false}" = "true" ]; then
  /usr/local/bin/poll-bridge.sh &

  METRO_PID=""

  start_metro() {
    echo "[mobile] expo start ${args[*]} (host=${REACT_NATIVE_PACKAGER_HOSTNAME:-?}, polling=true)"
    # setsid: arranca expo/metro como lider de un grupo de procesos propio,
    # asi el reinicio de abajo puede matar TODO el grupo (pnpm + el proceso
    # real de metro que lanza) y no solo el pnpm de primer nivel; sin esto,
    # `kill $METRO_PID` mataba pnpm pero dejaba a Metro vivo con el puerto
    # 8081 tomado, y el siguiente `expo start` preguntaba interactivamente
    # "Use port 8082 instead?" y se quedaba colgado.
    # CI=true: evita esa clase de prompt interactivo en este proceso
    # supervisado (no afecta al modo sin polling, que es el que usa las
    # teclas interactivas de Expo CLI en Mac).
    CI=true setsid pnpm --filter @app/mobile exec expo start "${args[@]}" &
    METRO_PID=$!
  }

  stop_and_wait_metro() {
    [ -n "$METRO_PID" ] || return 0
    kill -TERM -- "-$METRO_PID" 2>/dev/null || true
    local i=0
    while kill -0 "$METRO_PID" 2>/dev/null; do
      i=$((i + 1))
      if [ "$i" -ge 50 ]; then
        kill -KILL -- "-$METRO_PID" 2>/dev/null || true
        break
      fi
      sleep 0.2
    done
    wait "$METRO_PID" 2>/dev/null || true
    METRO_PID=""
  }

  shutdown() {
    echo "[mobile] señal recibida, apagando expo start..."
    stop_and_wait_metro
    exit 0
  }
  trap shutdown TERM INT

  start_metro
  while true; do
    if ! kill -0 "$METRO_PID" 2>/dev/null; then
      echo "[mobile] expo start terminó inesperadamente, reiniciando..."
      start_metro
    elif [ -f "$RESTART_REQUEST_FILE" ]; then
      rm -f "$RESTART_REQUEST_FILE"
      echo "[mobile] DEV_FILE_POLLING: cambios detectados, reiniciando expo start (proceso, no contenedor)..."
      stop_and_wait_metro
      start_metro
    fi
    sleep 1
  done
else
  echo "[mobile] expo start ${args[*]} (host=${REACT_NATIVE_PACKAGER_HOSTNAME:-?}, polling=false)"
  exec pnpm --filter @app/mobile exec expo start "${args[@]}"
fi
