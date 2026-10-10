#!/usr/bin/env bash
# Corre `./mvnw -B verify` del backend en un contenedor con acceso al socket de
# Docker (Testcontainers). Args extra se pasan a Maven.
# Uso: scripts/backend-test.sh [args de maven, p. ej. -Dtest=FooTest]
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib/common.sh"

require_docker
ensure_env
log "./mvnw -B verify $* (contenedor backend-test)"
compose --profile tools run --rm --no-deps backend-test verify "$@"
