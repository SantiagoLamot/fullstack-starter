#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Backend dev: spring-boot:run (perfil dev, JDWP en *:5005) + watcher que
# recompila ante cambios en src/main/** para que DevTools reinicie la app.
#   DEV_FILE_POLLING=false -> inotifywait (eventos nativos, Mac/Linux)
#   DEV_FILE_POLLING=true  -> polling por huella de mtimes (Windows: los bind
#                             mounts no propagan eventos inotify)
# -----------------------------------------------------------------------------
set -uo pipefail

cd /app

POLLING="${DEV_FILE_POLLING:-false}"
INTERVAL_MS="${DEV_FILE_POLLING_INTERVAL_MS:-1000}"
DEBOUNCE_MS="${DEV_RECOMPILE_DEBOUNCE_MS:-500}"
PROFILE="${SPRING_PROFILES_ACTIVE:-dev}"
DEBUG_PORT="${JDWP_PORT:-5005}"
WATCH_DIR="src/main"
# Debe coincidir con SPRING_DEVTOOLS_RESTART_TRIGGERFILE (docker-compose.override.yml)
TRIGGER_FILE="${SPRING_DEVTOOLS_RESTART_TRIGGERFILE:-.reloadtrigger}"

log() { echo "[dev-entrypoint] $*"; }
ms_to_s() { awk -v ms="$1" 'BEGIN { printf "%.3f", ms / 1000 }'; }

INTERVAL_S="$(ms_to_s "$INTERVAL_MS")"
DEBOUNCE_S="$(ms_to_s "$DEBOUNCE_MS")"

if [ ! -f mvnw ] || [ ! -f pom.xml ]; then
  log "apps/backend todavía no tiene mvnw/pom.xml. Esperando a que existan..."
  while [ ! -f mvnw ] || [ ! -f pom.xml ]; do sleep 5; done
fi

# Huella del árbol de fuentes: cambia ante alta, baja, modificación o rename.
fingerprint() {
  find "$WATCH_DIR" -type f -printf '%T@ %s %p\n' 2>/dev/null | sort | md5sum | cut -d' ' -f1
}

compile() {
  local start end
  start="$(date +%s)"
  log "Cambios en $WATCH_DIR -> ./mvnw -o compile"
  if sh ./mvnw -q -o -B compile; then
    end="$(date +%s)"
    # Un solo restart de DevTools por compilación (spring.devtools.restart.trigger-file)
    date +%s%N > "target/classes/$TRIGGER_FILE"
    log "Compilación OK ($((end - start))s). DevTools reinicia la app."
  else
    log "Compilación con errores: la app sigue corriendo con las clases anteriores."
  fi
}

# Espera a que la app responda (o 10 min) para no compilar en paralelo con el
# arranque inicial de spring-boot:run.
wait_for_app() {
  local i=0
  # Cualquier respuesta HTTP (aunque sea 401/503) indica que el servidor levantó.
  # (`|| true`: con pipefail, el exit != 0 de wget ante 4xx/5xx rompería el until)
  until { wget -q -S -O /dev/null -T 2 "http://127.0.0.1:8080/actuator/health" 2>&1 || true; } | grep -q 'HTTP/'; do
    i=$((i + 1))
    [ "$i" -ge 300 ] && break
    sleep 2
  done
}

watch_polling() {
  local prev="$1" cur next
  log "Watcher: polling cada ${INTERVAL_MS}ms (DEV_FILE_POLLING=true)"
  while true; do
    cur="$(fingerprint)"
    if [ "$cur" != "$prev" ]; then
      # debounce: esperar a que el árbol se estabilice
      while true; do
        sleep "$DEBOUNCE_S"
        next="$(fingerprint)"
        [ "$next" = "$cur" ] && break
        cur="$next"
      done
      prev="$cur"
      compile
    fi
    sleep "$INTERVAL_S"
  done
}

watch_inotify() {
  local baseline="$1"
  local events="modify,create,delete,move,attrib"
  log "Watcher: inotify (DEV_FILE_POLLING=false)"
  # Cambios hechos durante el arranque (antes de engancharse a inotify)
  [ "$(fingerprint)" != "$baseline" ] && compile
  while true; do
    if ! inotifywait -r -qq -e "$events" "$WATCH_DIR"; then
      sleep 2
      continue
    fi
    # debounce: absorber la ráfaga de eventos (1s sin eventos)
    while inotifywait -r -qq -t 1 -e "$events" "$WATCH_DIR"; do :; done
    compile
  done
}

watcher() {
  local baseline
  baseline="$(fingerprint)"
  wait_for_app
  if [ "$POLLING" = "true" ]; then
    watch_polling "$baseline"
  else
    watch_inotify "$baseline"
  fi
}

watcher &

log "Arrancando spring-boot:run (perfil=$PROFILE, JDWP=*:$DEBUG_PORT)"
exec sh ./mvnw -B spring-boot:run \
  -Dspring-boot.run.profiles="$PROFILE" \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}"
