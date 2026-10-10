#!/usr/bin/env bash
# Se ejecuta DENTRO del contenedor node-tools (ver scripts/gen-api-types.*).
set -euo pipefail
cd /repo

URL="${API_DOCS_URL:-http://backend:8080/v3/api-docs}"
OUT_JSON="packages/api-types/openapi.json"
OUT_TS="packages/api-types/src/schema.d.ts"

node - "$URL" "$OUT_JSON" <<'JS'
const fs = require('node:fs');
const [url, out] = process.argv.slice(2);
(async () => {
  let res;
  try {
    res = await fetch(url);
  } catch (e) {
    console.error(`[app] ERROR: no se pudo conectar a ${url} (${e.cause?.code ?? e.message}).`);
    console.error('[app] ¿Está el backend arriba? docker compose up -d backend');
    process.exit(1);
  }
  if (!res.ok) {
    console.error(`[app] ERROR: ${url} respondió HTTP ${res.status}. ¿Perfil dev activo (springdoc habilitado)?`);
    process.exit(1);
  }
  const spec = await res.json();
  fs.writeFileSync(out, JSON.stringify(spec, null, 2) + '\n');
  console.log(`[app] ${out} actualizado.`);
})();
JS

VERSION="$(node -p "require('./packages/api-types/package.json').devDependencies['openapi-typescript']")"
mkdir -p "$(dirname "$OUT_TS")"
echo "[app] openapi-typescript@${VERSION} -> ${OUT_TS}"
corepack pnpm dlx "openapi-typescript@${VERSION}" "$OUT_JSON" -o "$OUT_TS"
