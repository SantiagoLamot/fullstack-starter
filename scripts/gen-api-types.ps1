# Descarga el OpenAPI del backend (debe estar arriba, perfil dev) y regenera
# packages/api-types/openapi.json + packages/api-types/src/schema.d.ts.
# Todo corre en contenedores (node:24 + corepack/pnpm) dentro de la red de compose.
# Uso: scripts\gen-api-types.ps1
. (Join-Path $PSScriptRoot 'lib\common.ps1')

Assert-Docker
Write-Info 'Generando tipos desde http://backend:8080/v3/api-docs (red interna de compose)...'
Invoke-Compose --profile tools run --rm --no-deps node-tools bash scripts/lib/gen-api-types.inner.sh
Write-Info 'Listo. Revisa y commitea packages/api-types.'
