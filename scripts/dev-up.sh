#!/usr/bin/env bash
# Levanta el stack de desarrollo (base + override).
# Uso: scripts/dev-up.sh [-f|--foreground] [servicio...]
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

foreground=false
services=()
for arg in "$@"; do
  case "$arg" in
    -f | --foreground) foreground=true ;;
    -h | --help) echo "Uso: $0 [-f|--foreground] [servicio...]"; exit 0 ;;
    *) services+=("$arg") ;;
  esac
done

require_docker
ensure_env

# IP LAN del host (la usa el celular con Expo Go). APP_LAN_IP la fuerza a mano.
detect_lan_ip() {
  local ip="" iface="" ps
  case "$(uname -s)" in
    MINGW* | MSYS* | CYGWIN*)
      # Interfaz con default gateway; sin vEthernet/WSL/Hyper-V/Docker, loopback ni APIPA.
      ps='Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq "Up" -and $_.InterfaceAlias -notmatch "vEthernet|WSL|Hyper-V|Docker|Loopback" } | ForEach-Object { $_.IPv4Address.IPAddress } | Where-Object { $_ -notmatch "^(127[.]|169[.]254[.])" } | Select-Object -First 1'
      ip="$(powershell.exe -NoProfile -NonInteractive -Command "$ps" 2>/dev/null | tr -d '\r' | head -n 1 || true)" ;;
    Darwin)
      ip="$(ipconfig getifaddr en0 2>/dev/null || true)"
      if [ -z "$ip" ]; then
        iface="$(route get default 2>/dev/null | awk '/interface:/ {print $2}' | head -n 1 || true)"
        if [ -n "$iface" ]; then ip="$(ipconfig getifaddr "$iface" 2>/dev/null || true)"; fi
      fi ;;
    *)
      ip="$(ip route get 1.1.1.1 2>/dev/null | awk '{for (i=1;i<=NF;i++) if ($i=="src") {print $(i+1); exit}}' || true)" ;;
  esac
  printf '%s' "$ip"
}

LAN_IP="${APP_LAN_IP:-$(detect_lan_ip)}"
BACKEND_PORT="${BACKEND_HOST_PORT:-$(env_get BACKEND_HOST_PORT)}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
EXPO_PORT="${EXPO_HOST_PORT:-$(env_get EXPO_HOST_PORT)}"
EXPO_PORT="${EXPO_PORT:-8081}"
if [ -n "$LAN_IP" ]; then
  # Las variables del proceso ganan sobre .env en Compose.
  export REACT_NATIVE_PACKAGER_HOSTNAME="$LAN_IP"
  export EXPO_PUBLIC_API_URL="http://$LAN_IP:$BACKEND_PORT/api/v1"
else
  warn "No se pudo detectar la IP LAN: se usan los valores de .env (o exportá APP_LAN_IP)."
fi

if [ "$foreground" = true ]; then
  log "docker compose up --build ${services[*]:-} (foreground, Ctrl+C para detener)"
  compose up --build ${services[@]+"${services[@]}"}
else
  log "docker compose up --build -d ${services[*]:-}"
  compose up --build -d ${services[@]+"${services[@]}"}
  compose ps
  log "Admin web (Vite): http://localhost:$(env_get ADMIN_WEB_DEV_HOST_PORT) | API: http://localhost:$(env_get BACKEND_HOST_PORT)"
  log "Mailpit: http://localhost:$(env_get MAILPIT_UI_HOST_PORT) | Adminer: http://localhost:$(env_get ADMINER_HOST_PORT)"
  if [ -n "$LAN_IP" ]; then
    log "App móvil: exp://$LAN_IP:$EXPO_PORT"
    log "API para el celular: http://$LAN_IP:$BACKEND_PORT/api/v1"
  fi
  log "Logs: scripts/logs.sh [servicio]"
fi
