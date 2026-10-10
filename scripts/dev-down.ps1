# Detiene el stack de desarrollo. Los volumenes (DB, caches) se conservan.
# Uso: scripts\dev-down.ps1 [args extra de `docker compose down`]
. (Join-Path $PSScriptRoot 'lib\common.ps1')

Assert-Docker
Write-Info "docker compose down $($args -join ' ')"
Invoke-Compose down @args
