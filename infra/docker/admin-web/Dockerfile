# syntax=docker/dockerfile:1
# -----------------------------------------------------------------------------
# Admin web (React + Vite). Contexto de build = raíz del repo (workspace pnpm).
# Filtro de contexto: infra/docker/admin-web/Dockerfile.dockerignore
#   deps    -> pnpm install filtrado a @app/admin-web y sus dependencias
#   dev     -> Vite dev server (código montado por volumen en el override)
#   build   -> pnpm --filter @app/admin-web build -> apps/admin-web/dist
#   runtime -> nginx sirviendo el SPA + proxy /api (stage por defecto)
# -----------------------------------------------------------------------------
ARG NODE_IMAGE=node:24-bookworm-slim
ARG NGINX_IMAGE=nginx:1.30-alpine

# ---------- base ----------
FROM ${NODE_IMAGE} AS base
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0 \
    PNPM_STORE_DIR=/pnpm/store
# corepack actualizado (claves de firma al día); pnpm sale de "packageManager"
RUN npm install -g corepack@latest && corepack enable
WORKDIR /repo

# ---------- deps ----------
FROM base AS deps
# "pnpm-lock.yam[l]": patrón que tolera que el lockfile todavía no exista
COPY package.json pnpm-workspace.yaml pnpm-lock.yam[l] ./
RUN corepack install
# Solo los package.json del workspace (mejor cache de capas)
RUN --mount=type=bind,source=apps,target=/ctx/apps \
    --mount=type=bind,source=packages,target=/ctx/packages \
    for f in /ctx/apps/*/package.json /ctx/packages/*/package.json; do \
      [ -f "$f" ] || continue; \
      rel="${f#/ctx/}"; mkdir -p "$(dirname "$rel")"; cp "$f" "$rel"; \
    done
RUN --mount=type=cache,id=app-pnpm-store,target=/pnpm/store \
    if [ -f pnpm-lock.yaml ]; then FROZEN="--frozen-lockfile"; \
    else FROZEN=""; echo "AVISO: sin pnpm-lock.yaml -> pnpm install sin --frozen-lockfile"; fi; \
    pnpm install $FROZEN --filter "@app/admin-web..." \
      --store-dir /pnpm/store --config.confirm-modules-purge=false

# ---------- dev ----------
# Los node_modules de esta imagen pre-cargan el volumen nombrado la primera vez.
FROM deps AS dev
ENV NODE_ENV=development
EXPOSE 5173
CMD ["pnpm", "--filter", "@app/admin-web", "dev", "--host", "0.0.0.0"]

# ---------- build ----------
FROM deps AS build
COPY . .
RUN pnpm --filter @app/admin-web build

# ---------- runtime ----------
FROM ${NGINX_IMAGE} AS runtime
COPY infra/docker/admin-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY infra/docker/admin-web/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /repo/apps/admin-web/dist /usr/share/nginx/html
EXPOSE 80
