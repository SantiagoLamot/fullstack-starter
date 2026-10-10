# shellcheck shell=bash
# Helpers comunes para scripts/*.sh (se incluye con `source`).
# Resuelve la raíz del repo relativa a este archivo (funciona desde cualquier cwd
# y con la ruta del repo, que tiene espacios y apóstrofo).

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

# Git Bash (Windows): no reescribir argumentos tipo /repo o /var/run/... a C:\...
export MSYS_NO_PATHCONV=1
export MSYS2_ARG_CONV_EXCL='*'

SECRET_IMAGE="alpine:3.24"

log()  { printf '\033[1;34m[app]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[app] AVISO:\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[app] ERROR:\033[0m %s\n' "$*" >&2; exit 1; }

require_docker() {
  command -v docker >/dev/null 2>&1 || die "No se encontró 'docker' en el PATH. Instalá Docker Desktop."
  docker info >/dev/null 2>&1 || die "Docker no está corriendo. Abrí Docker Desktop y reintentá."
}

compose() { docker compose "$@"; }

# Windows (Git Bash/MSYS/Cygwin) o WSL sobre /mnt/*: los bind mounts no propagan
# eventos de archivos -> hay que usar polling.
needs_polling() {
  case "$(uname -s)" in
    MINGW* | MSYS* | CYGWIN*) return 0 ;;
  esac
  if [ -n "${WSL_DISTRO_NAME:-}" ]; then
    case "$REPO_ROOT" in /mnt/*) return 0 ;; esac
  fi
  return 1
}

# Valor de una variable en .env (vacío si no existe)
env_get() {
  [ -f .env ] || return 0
  grep -E "^$1=" .env | tail -n 1 | cut -d= -f2- | sed -e 's/\r$//' -e 's/^"//' -e 's/"$//'
}

# Reemplaza KEY=... en .env (portable Mac/Linux/Git Bash, sin sed -i)
env_set() {
  local key="$1" value="$2" tmp
  tmp="$(mktemp)"
  awk -v k="$key" -v v="$value" 'BEGIN { done = 0 }
    $0 ~ "^" k "=" { print k "=" v; done = 1; next }
    { print }
    END { if (!done) print k "=" v }' .env > "$tmp"
  cat "$tmp" > .env
  rm -f "$tmp"
}

# Base64 de 48 bytes aleatorios, generado en un contenedor (sin openssl en el host)
gen_secret() {
  docker run --rm "$SECRET_IMAGE" sh -c 'head -c 48 /dev/urandom | base64 -w 0'
}

# Crea .env desde .env.example si falta y completa JWT_SECRET si está vacío.
ensure_env() {
  if [ ! -f .env ]; then
    cp .env.example .env
    if needs_polling; then
      env_set DEV_FILE_POLLING true
    else
      env_set DEV_FILE_POLLING false
    fi
    warn ".env no existía: se creó desde .env.example (DEV_FILE_POLLING=$(env_get DEV_FILE_POLLING))."
    warn "Revisá REACT_NATIVE_PACKAGER_HOSTNAME y EXPO_PUBLIC_API_URL (IP LAN) y las contraseñas."
  fi
  if [ -z "$(env_get JWT_SECRET)" ]; then
    env_set JWT_SECRET "$(gen_secret)"
    log "JWT_SECRET vacío en .env: se generó uno nuevo."
  fi
}
