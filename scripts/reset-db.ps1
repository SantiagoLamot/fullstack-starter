# Borra la base de datos de desarrollo (volumen de MySQL) y la recrea vacia.
# Uso: scripts\reset-db.ps1 [-Yes]
param([switch]$Yes)
. (Join-Path $PSScriptRoot 'lib\common.ps1')

Assert-Docker
Initialize-EnvFile

$volume = 'fullstack-starter_mysql-data'
if (-not $Yes) {
    Write-Warn "Se van a BORRAR todos los datos de MySQL (volumen $volume)."
    $answer = Read-Host "Escribi 'si' para continuar"
    if ($answer -ne 'si') { Stop-WithError 'Cancelado.' }
}

Write-Info 'Deteniendo backend...'
$null = Test-DockerCommand compose stop backend
Write-Info 'Eliminando contenedor de MySQL...'
Invoke-Compose rm -f -s mysql
if (Test-DockerCommand volume inspect $volume) {
    Write-Info "Eliminando volumen $volume..."
    Invoke-Docker volume rm $volume | Out-Null
}
Write-Info 'Levantando MySQL (esperando healthy)...'
Invoke-Compose up -d --wait mysql
Write-Info 'Levantando backend (Hibernate recrea el esquema; el perfil dev siembra datos)...'
Invoke-Compose up -d backend
Write-Info 'Listo.'
