# Corre `./mvnw -B verify` del backend en un contenedor con acceso al socket de
# Docker (Testcontainers). Args extra se pasan a Maven.
# Uso: scripts\backend-test.ps1 [args de maven, p. ej. -Dtest=FooTest]
. (Join-Path $PSScriptRoot 'lib\common.ps1')

Assert-Docker
Initialize-EnvFile
Write-Info "./mvnw -B verify $($args -join ' ') (contenedor backend-test)"
Invoke-Compose --profile tools run --rm --no-deps backend-test verify @args
