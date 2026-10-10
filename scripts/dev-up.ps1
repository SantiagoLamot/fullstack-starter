# Levanta el stack de desarrollo (base + override).
# Uso: scripts\dev-up.ps1 [-Foreground] [servicio...]
param(
    [switch]$Foreground,
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$Services = @()
)
. (Join-Path $PSScriptRoot 'lib\common.ps1')

Assert-Docker
Initialize-EnvFile

# IP LAN del host (la usa el celular con Expo Go). APP_LAN_IP la fuerza a mano.
function Get-LanIp {
    try {
        $cfg = Get-NetIPConfiguration | Where-Object {
            $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' -and
            $_.InterfaceAlias -notmatch 'vEthernet|WSL|Hyper-V|Docker|Loopback'
        }
        foreach ($c in $cfg) {
            foreach ($a in $c.IPv4Address) {
                if ($a.IPAddress -notmatch '^(127[.]|169[.]254[.])') { return $a.IPAddress }
            }
        }
    } catch { }
    return ''
}

$LanIp = $env:APP_LAN_IP
if (-not $LanIp) { $LanIp = Get-LanIp }
$BackendPort = $env:BACKEND_HOST_PORT
if (-not $BackendPort) { $BackendPort = Get-EnvValue 'BACKEND_HOST_PORT' }
if (-not $BackendPort) { $BackendPort = '8080' }
$ExpoPort = $env:EXPO_HOST_PORT
if (-not $ExpoPort) { $ExpoPort = Get-EnvValue 'EXPO_HOST_PORT' }
if (-not $ExpoPort) { $ExpoPort = '8081' }
if ($LanIp) {
    # Las variables del proceso ganan sobre .env en Compose.
    $env:REACT_NATIVE_PACKAGER_HOSTNAME = $LanIp
    $env:EXPO_PUBLIC_API_URL = "http://${LanIp}:${BackendPort}/api/v1"
} else {
    Write-Warn 'No se pudo detectar la IP LAN: se usan los valores de .env (o defini APP_LAN_IP).'
}

if ($Foreground) {
    Write-Info "docker compose up --build $($Services -join ' ') (foreground, Ctrl+C para detener)"
    Invoke-Compose up --build @Services
} else {
    Write-Info "docker compose up --build -d $($Services -join ' ')"
    Invoke-Compose up --build -d @Services
    Invoke-Compose ps
    Write-Info "Admin web (Vite): http://localhost:$(Get-EnvValue 'ADMIN_WEB_DEV_HOST_PORT') | API: http://localhost:$(Get-EnvValue 'BACKEND_HOST_PORT')"
    Write-Info "Mailpit: http://localhost:$(Get-EnvValue 'MAILPIT_UI_HOST_PORT') | Adminer: http://localhost:$(Get-EnvValue 'ADMINER_HOST_PORT')"
    if ($LanIp) {
        Write-Info "App movil: exp://${LanIp}:${ExpoPort}"
        Write-Info "API para el celular: http://${LanIp}:${BackendPort}/api/v1"
    }
    Write-Info 'Logs: scripts\logs.ps1 [servicio]'
}
