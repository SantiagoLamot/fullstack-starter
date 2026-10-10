#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Puente de polling para Metro en Windows.
#
# Por que existe: en este entorno (Docker Desktop / WSL2), el bind mount del
# repo esta montado por 9p (`mount | grep /repo` -> `type 9p`). El protocolo 9p
# no implementa notificacion de cambios: se probo con un `fs.watch` de Node
# apuntando a un archivo de este mount que NI SIQUIERA un `touch`/escritura
# hecha DESDE DENTRO del contenedor dispara un evento (el mismo `fs.watch`
# sobre /tmp, que es overlay nativo, si los recibe). Por eso "fabricar" un
# evento inotify tocando el archivo (touch -c) no sirve aqui: no hay forma de
# hacer que fs.watch/inotify se entere de un cambio en /repo, sin importar
# quien lo origine. El watcher de Metro (@expo/metro-file-map) depende
# enteramente de inotify (ver docs/agents/mobile-dev.md) y por eso nunca ve
# nada.
#
# Que hace en cambio: sondea (barato, via `find -printf` + hash, igual que el
# watcher de polling del backend, que SI funciona sobre este mismo mount
# porque solo necesita `stat`, no notificaciones) apps/mobile y
# packages/api-types/src (excluyendo node_modules, .expo, dist, .git). Cuando
# detecta un cambio real y el arbol se estabiliza (debounce), pide un reinicio
# del PROCESO `expo start` (no del contenedor) dejando un archivo de pedido
# que consume el supervisor de dev-entrypoint.sh. Un `expo start` nuevo hace
# un recrawl completo y sirve el contenido actualizado, sin volver a instalar
# dependencias ni recrear el contenedor.
#
# Uso: dev-entrypoint.sh lo lanza en background solo si DEV_FILE_POLLING=true.
# -----------------------------------------------------------------------------
set -uo pipefail
cd /repo

INTERVAL_MS="${DEV_FILE_POLLING_INTERVAL_MS:-1000}"
DEBOUNCE_MS="${DEV_RECOMPILE_DEBOUNCE_MS:-500}"
REQUEST_FILE="/tmp/.mobile-metro-restart-request"

log() { echo "[poll-bridge] $*"; }
ms_to_s() { awk -v ms="$1" 'BEGIN { printf "%.3f", ms / 1000 }'; }

INTERVAL_S="$(ms_to_s "$INTERVAL_MS")"
DEBOUNCE_S="$(ms_to_s "$DEBOUNCE_MS")"

# Directorios que interesa vigilar en dev (codigo de la app + tipos
# compartidos). apps/mobile puede no existir todavia al arrancar.
WATCH_PATHS=(apps/mobile packages/api-types/src)

# apps/mobile/expo-env.d.ts lo regenera el propio `expo start` en cada arranque
# (no es una edicion real): si se incluye en la huella, cada reinicio de Metro
# se auto-detecta como "cambio" y dispara otro reinicio sin fin. Se excluye
# igual que node_modules/.expo/dist/.git.

existing_paths() {
  local p
  for p in "${WATCH_PATHS[@]}"; do
    [ -e "$p" ] && printf '%s\n' "$p"
  done
}

# Huella del arbol vigilado: cambia ante alta, baja, modificacion o rename de
# cualquier archivo (mismo enfoque que infra/docker/backend/dev-entrypoint.sh).
fingerprint() {
  local paths=()
  while IFS= read -r p; do paths+=("$p"); done < <(existing_paths)
  if [ "${#paths[@]}" -eq 0 ]; then
    echo "empty"
    return
  fi
  find "${paths[@]}" \
    \( -name node_modules -o -name .expo -o -name dist -o -name .git \) -prune -o \
    -type f -not -name 'expo-env.d.ts' -printf '%T@ %s %p\n' 2>/dev/null | sort | md5sum | cut -d' ' -f1
}

log "activo (DEV_FILE_POLLING=true, intervalo=${INTERVAL_MS}ms, reinicia el proceso expo start ante cambios, NO el contenedor)"

# Esperar a que exista al menos un directorio a vigilar.
while [ -z "$(existing_paths)" ]; do
  sleep 5
done

prev="$(fingerprint)"
while true; do
  sleep "$INTERVAL_S"
  cur="$(fingerprint)"
  if [ "$cur" != "$prev" ]; then
    # debounce: esperar a que el arbol se estabilice antes de actuar
    while true; do
      sleep "$DEBOUNCE_S"
      next="$(fingerprint)"
      [ "$next" = "$cur" ] && break
      cur="$next"
    done
    prev="$cur"
    log "cambios detectados en apps/mobile o packages/api-types/src -> pidiendo reinicio de Metro"
    touch "$REQUEST_FILE"
  fi
done
