#!/usr/bin/env bash
# Borra la base de datos de desarrollo (volumen de MySQL) y la recrea vacía.
# Uso: scripts/reset-db.sh [-y|--yes]
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

yes=false
for arg in "$@"; do
  case "$arg" in
    -y | --yes) yes=true ;;
    *) die "Argumento desconocido: $arg (uso: $0 [-y|--yes])" ;;
  esac
done

require_docker
ensure_env

volume="fullstack-starter_mysql-data"
if [ "$yes" != true ]; then
  warn "Se van a BORRAR todos los datos de MySQL (volumen $volume)."
  read -r -p "Escribí 'si' para continuar: " answer
  [ "$answer" = "si" ] || die "Cancelado."
fi

log "Deteniendo backend..."
compose stop backend || true
log "Eliminando contenedor de MySQL..."
compose rm -f -s mysql
if docker volume inspect "$volume" >/dev/null 2>&1; then
  log "Eliminando volumen $volume..."
  docker volume rm "$volume" >/dev/null
fi
log "Levantando MySQL (esperando healthy)..."
compose up -d --wait mysql
log "Levantando backend (Hibernate recrea el esquema; el perfil dev siembra datos)..."
compose up -d backend
log "Listo."
