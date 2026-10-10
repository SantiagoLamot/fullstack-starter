# syntax=docker/dockerfile:1
# -----------------------------------------------------------------------------
# Backend (Spring Boot, Java 21). Contexto de build = raíz del repo.
# Filtro de contexto: infra/docker/backend/Dockerfile.dockerignore
#   dev     -> JDK + inotify-tools; el código se monta por volumen (override)
#   build   -> ./mvnw package -> target/app.jar
#   runtime -> JRE 21 alpine, usuario no root (stage por defecto)
# -----------------------------------------------------------------------------
ARG JDK_IMAGE=eclipse-temurin:21-jdk-noble
ARG JRE_IMAGE=eclipse-temurin:21-jre-alpine

# ---------- dev ----------
FROM ${JDK_IMAGE} AS dev
RUN apt-get update \
 && apt-get install -y --no-install-recommends inotify-tools wget procps \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY infra/docker/backend/dev-entrypoint.sh /usr/local/bin/dev-entrypoint.sh
# Defensa ante CRLF (checkout sin .gitattributes) y bit de ejecución perdido
RUN sed -i 's/\r$//' /usr/local/bin/dev-entrypoint.sh && chmod +x /usr/local/bin/dev-entrypoint.sh
EXPOSE 8080 5005
ENTRYPOINT ["/usr/local/bin/dev-entrypoint.sh"]

# ---------- build ----------
FROM ${JDK_IMAGE} AS build
WORKDIR /src
COPY apps/backend/ ./
RUN --mount=type=cache,id=app-m2,target=/root/.m2 \
    sed -i 's/\r$//' mvnw \
 && sh ./mvnw -B -DskipTests package \
 && cp target/app.jar /app.jar

# ---------- runtime ----------
FROM ${JRE_IMAGE} AS runtime
RUN addgroup -S app && adduser -S -G app -h /app app
WORKDIR /app
COPY --from=build --chown=app:app /app.jar /app/app.jar
# Directorio de imágenes (volumen media-data): creado con dueño app para que el volumen nuevo sea escribible
RUN mkdir -p /data/media && chown app:app /data/media
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
