# ADR 0003 — Monorepo con pnpm workspaces

- **Estado:** aceptado
- **Fecha:** 29/09/2026

## Contexto

Tres apps (backend, admin-web, mobile) que comparten el contrato de la API. Queremos un solo repo, tipos compartidos y que los cambios de contrato sean visibles en el diff. El desarrollo corre en Docker con bind mounts, en Windows y Mac.

## Decisión

- **pnpm workspaces** (`pnpm-workspace.yaml`): `apps/admin-web`, `apps/mobile`, `packages/*`.
- **El backend queda fuera del workspace**: usa su Maven wrapper (`apps/backend/mvnw`).
- **`nodeLinker: hoisted`** en `pnpm-workspace.yaml` (no en `.npmrc`): `node_modules` plano, sin symlinks por paquete. Motivo: Metro/Expo resuelve mejor sin symlinks y los bind mounts de Docker en Windows manejan mal los symlinks. pnpm ≥ 11 dejó de leer settings de layout (como `node-linker`) desde `.npmrc` (solo auth/registry); por eso el `.npmrc` del repo se eliminó y la config vive en `pnpm-workspace.yaml`.
- **`node_modules` en volúmenes Docker nombrados, separados por app** (`admin-web-node-modules`, `admin-web-app-node-modules`, `mobile-node-modules`, etc., ver `docker-compose.override.yml`): admin-web y mobile no comparten un `node_modules` raíz para evitar carreras entre sus `pnpm install --filter` al levantar el stack. El store de pnpm (`pnpm-store`) sí es compartido entre ambos servicios.
- **`packages/api-types`** (`@app/api-types`): tipos generados con `openapi-typescript` desde `/v3/api-docs` del backend.
  - Se commitean `openapi.json` y `src/schema.d.ts` → todo cambio de contrato aparece en el diff del PR.
  - Se regenera con `scripts/gen-api-types.sh` / `.ps1` (baja el spec del backend corriendo y ejecuta `pnpm --filter @app/api-types generate` en un contenedor).
  - No se edita a mano.

## Alternativas consideradas

- **`node-linker=isolated`** (default de pnpm): más estricto con dependencias fantasma, pero problemático con Metro y bind mounts en Windows. Descartado por ahora; se puede reevaluar.
- **npm/yarn workspaces:** pnpm es más rápido y maneja mejor el store; sin ventaja concreta en las otras.
- **Nx/Turborepo:** sobredimensionado para 2 apps JS.
- **Generar cliente completo (openapi-generator):** más código generado y acoplamiento; con tipos + axios alcanza.
- **No commitear lo generado:** los cambios de contrato quedarían invisibles en revisión. Descartado.

## Consecuencias

- Con `hoisted` es posible importar dependencias no declaradas: cada `package.json` debe declarar lo que usa (lo revisa `qa-reviewer`).
- Una sola versión de TypeScript en todo el workspace; React tiene dos versiones conviviendo (admin-web 19.2.8, mobile 19.2.3) por las restricciones de Expo/React Router — ver ADR 0001.
- Olvidar regenerar tipos tras cambiar la API rompe el typecheck de los frontends (efecto buscado).
