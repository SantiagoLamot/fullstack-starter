#!/usr/bin/env bash
# Detiene el stack de desarrollo. Los volúmenes (DB, caches) se conservan.
# Uso: scripts/dev-down.sh [args extra de `docker compose down`]
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_docker
log "docker compose down $*"
compose down "$@"
