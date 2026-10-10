# Genera un secreto Base64 de 48 bytes (p. ej. para JWT_SECRET), sin openssl en el host.
# Uso: scripts\gen-secret.ps1
. (Join-Path $PSScriptRoot 'lib\common.ps1')

Assert-Docker
Write-Output (New-Secret)
