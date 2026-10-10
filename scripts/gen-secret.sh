#!/usr/bin/env bash
# Genera un secreto Base64 de 48 bytes (p. ej. para JWT_SECRET), sin openssl en el host.
# Uso: scripts/gen-secret.sh
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_docker
gen_secret
echo
