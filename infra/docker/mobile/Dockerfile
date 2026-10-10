# syntax=docker/dockerfile:1
# -----------------------------------------------------------------------------
# Mobile (Expo / Metro) - solo dev. El código se monta en /repo (override).
# Contexto de build = infra/docker/mobile
# -----------------------------------------------------------------------------
ARG NODE_IMAGE=node:24-bookworm
FROM ${NODE_IMAGE}
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0 \
    PNPM_STORE_DIR=/pnpm/store \
    EXPO_NO_TELEMETRY=1
# corepack al día + @expo/ngrok global (lo requiere `expo start --tunnel`)
RUN npm install -g corepack@latest @expo/ngrok@^4.1.0 && corepack enable
WORKDIR /repo
COPY dev-entrypoint.sh /usr/local/bin/dev-entrypoint.sh
COPY poll-bridge.sh /usr/local/bin/poll-bridge.sh
RUN sed -i 's/\r$//' /usr/local/bin/dev-entrypoint.sh /usr/local/bin/poll-bridge.sh \
    && chmod +x /usr/local/bin/dev-entrypoint.sh /usr/local/bin/poll-bridge.sh
EXPOSE 8081
ENTRYPOINT ["/usr/local/bin/dev-entrypoint.sh"]
