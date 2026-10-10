# Helpers comunes para scripts/*.ps1 (se incluye con dot-sourcing).
# Compatible con Windows PowerShell 5.1. Resuelve la raiz del repo relativa a
# este archivo (funciona desde cualquier cwd y con la ruta del repo).

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

$script:RepoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
Set-Location -LiteralPath $script:RepoRoot

$script:SecretImage = 'alpine:3.24'
$script:Utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Write-Info([string]$Message) { Write-Host "[app] $Message" -ForegroundColor Cyan }
function Write-Warn([string]$Message) { Write-Host "[app] AVISO: $Message" -ForegroundColor Yellow }
function Stop-WithError([string]$Message) {
    Write-Host "[app] ERROR: $Message" -ForegroundColor Red
    exit 1
}

# Ejecuta docker con los argumentos dados y falla si el exit code no es 0.
function Invoke-Docker {
    & docker @args
    if ($LASTEXITCODE -ne 0) { throw "docker $($args -join ' ') fallo (exit $LASTEXITCODE)" }
}

function Invoke-Compose {
    & docker compose @args
    if ($LASTEXITCODE -ne 0) { throw "docker compose $($args -join ' ') fallo (exit $LASTEXITCODE)" }
}

# Ejecuta docker descartando la salida y devuelve $true si el exit code es 0.
# (EAP local en Continue: en PS 5.1 redirigir stderr de un nativo con Stop lanza error)
function Test-DockerCommand {
    $ErrorActionPreference = 'Continue'
    & docker @args *> $null
    return ($LASTEXITCODE -eq 0)
}

function Assert-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Stop-WithError "No se encontro 'docker' en el PATH. Instala Docker Desktop."
    }
    if (-not (Test-DockerCommand info)) { Stop-WithError 'Docker no esta corriendo. Abri Docker Desktop y reintenta.' }
}

# Windows -> polling (los bind mounts no propagan eventos). Mac/Linux (pwsh) -> false.
function Test-NeedsPolling {
    if ($PSVersionTable.PSVersion.Major -lt 6) { return $true }
    return [bool]$IsWindows
}

function Get-EnvPath { return (Join-Path $script:RepoRoot '.env') }

# Valor de una variable en .env (vacio si no existe)
function Get-EnvValue([string]$Key) {
    $path = Get-EnvPath
    if (-not (Test-Path -LiteralPath $path)) { return '' }
    $value = ''
    foreach ($line in [System.IO.File]::ReadAllLines($path)) {
        if ($line.StartsWith("$Key=")) { $value = $line.Substring($Key.Length + 1).Trim().Trim('"') }
    }
    return $value
}

# Reemplaza KEY=... en .env; escribe UTF-8 sin BOM y LF (compatible con compose/bash)
function Set-EnvValue([string]$Key, [string]$Value) {
    $path = Get-EnvPath
    $text = [System.IO.File]::ReadAllText($path) -replace "`r`n", "`n"
    $pattern = '(?m)^' + [regex]::Escape($Key) + '=.*$'
    $line = "$Key=$Value"
    if ([regex]::IsMatch($text, $pattern)) {
        $text = [regex]::Replace($text, $pattern, { param($m) $line })
    } else {
        if (-not $text.EndsWith("`n")) { $text += "`n" }
        $text += "$line`n"
    }
    [System.IO.File]::WriteAllText($path, $text, $script:Utf8NoBom)
}

# Base64 de 48 bytes aleatorios, generado en un contenedor (sin openssl en el host)
function New-Secret {
    $out = & docker run --rm $script:SecretImage sh -c 'head -c 48 /dev/urandom | base64 -w 0'
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el secreto con docker.' }
    return ([string]($out -join '')).Trim()
}

# Crea .env desde .env.example si falta y completa JWT_SECRET si esta vacio.
function Initialize-EnvFile {
    $path = Get-EnvPath
    if (-not (Test-Path -LiteralPath $path)) {
        $example = [System.IO.File]::ReadAllText((Join-Path $script:RepoRoot '.env.example')) -replace "`r`n", "`n"
        [System.IO.File]::WriteAllText($path, $example, $script:Utf8NoBom)
        if (Test-NeedsPolling) { Set-EnvValue 'DEV_FILE_POLLING' 'true' } else { Set-EnvValue 'DEV_FILE_POLLING' 'false' }
        Write-Warn ".env no existia: se creo desde .env.example (DEV_FILE_POLLING=$(Get-EnvValue 'DEV_FILE_POLLING'))."
        Write-Warn 'Revisa REACT_NATIVE_PACKAGER_HOSTNAME y EXPO_PUBLIC_API_URL (IP LAN) y las contrasenas.'
    }
    if ([string]::IsNullOrEmpty((Get-EnvValue 'JWT_SECRET'))) {
        Set-EnvValue 'JWT_SECRET' (New-Secret)
        Write-Info 'JWT_SECRET vacio en .env: se genero uno nuevo.'
    }
}
