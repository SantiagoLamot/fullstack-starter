#!/usr/bin/env bash
# Descarga el OpenAPI del backend (debe estar arriba, perfil dev) y regenera
# packages/api-types/openapi.json + packages/api-types/src/schema.d.ts.
# Todo corre en contenedores (node:24 + corepack/pnpm) dentro de la red de compose.
# Uso: scripts/gen-api-types.sh
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_docker
log "Generando tipos desde http://backend:8080/v3/api-docs (red interna de compose)..."
compose --profile tools run --rm --no-deps node-tools bash scripts/lib/gen-api-types.inner.sh
log "Listo. Revisá y commiteá packages/api-types."
