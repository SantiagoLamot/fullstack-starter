# Sigue los logs del stack o de un servicio.
# Uso: scripts\logs.ps1 [servicio...]
. (Join-Path $PSScriptRoot 'lib\common.ps1')

Assert-Docker
Invoke-Compose logs -f --tail=200 @args
