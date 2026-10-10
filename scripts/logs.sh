#!/usr/bin/env bash
# Sigue los logs del stack o de un servicio.
# Uso: scripts/logs.sh [servicio...]
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_docker
compose logs -f --tail=200 "$@"
