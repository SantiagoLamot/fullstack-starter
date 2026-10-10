# ADR 0001 — Stack y versiones fijadas

- **Estado:** aceptado
- **Fecha:** 29/09/2026 (versiones verificadas ese día en Maven Central, npm y Docker Hub; revisar al iniciar cada proyecto)

## Contexto

Proyecto nuevo que necesita versiones con soporte vigente durante toda su vida, sin betas, y compatibles entre sí dentro de un monorepo (un solo React, un solo TypeScript).

## Decisión

### Backend

| Pieza | Versión | Motivo |
|---|---|---|
| Java | 21 (imagen `eclipse-temurin:21-jdk`) | LTS; soportada por Boot 4. |
| Spring Boot | **4.1.1** | Última GA. 4.2 está en M2 (no se usa). 3.5.x quedó fuera de soporte OSS (~jun-2026): arrancar ahí sería deuda desde el día uno. Boot 4 trae Spring Framework 7, Spring Security 7, Hibernate 7 y Jakarta EE 11. |
| springdoc-openapi | 3.1.1 | Línea compatible con Boot 4. Genera el OpenAPI, fuente de verdad del contrato. |
| MapStruct | 1.6.3 | Última estable (1.7 en beta). |
| Testcontainers | 2.0.5 | Tests de integración contra MySQL real. |
| MySQL | 8.4.11 LTS | Línea LTS. |
| Build | Maven wrapper | Sin Maven en el host (todo en Docker). |

### Frontends y tooling

| Pieza | Versión | Motivo |
|---|---|---|
| Node | 24 LTS (solo en contenedores) | LTS vigente. |
| pnpm | 12.8.1 (`packageManager` en `package.json`) | Workspaces (ADR 0003). |
| TypeScript | **6.0.3** (admin-web) | typescript-eslint 8 solo soporta TS < 6.1; no se usa 7.0.2 por eso. `openapi-typescript` 7.13 declara peer `typescript ^5`: warning de pnpm aceptado (funciona igual con TS 6). |
| React | **19.2.8 en admin-web**, **19.2.3 en mobile** | Expo SDK 57 fija React 19.2.3 (RN exige coincidencia exacta con su renderer, no se puede subir sin subir Expo/RN); admin-web usa 19.2.8 porque React Router 8.x exige `react >=19.2.7` y esa versión trae parches de seguridad sobre 19.2.3. Metro (bundler de mobile) fuerza la resolución de `react`/`react-dom` a la copia de `apps/mobile` para que RN nunca vea la 19.2.8 del hoist. **Plan B** si conviven mal en el hoisted `node_modules`: bajar admin-web a React Router 7 (sin el piso `>=19.2.7`) y unificar todo en una sola versión de React. |
| Expo SDK | 57 (React Native 0.86.3, React 19.2.3) | Última estable; SDK 58 en beta (no se usa). |
| Vite | **8.3.1** | admin-web. |
| React Router | 8.4 | admin-web. |
| TanStack Query | 5.104 | Estado de servidor en ambas apps. |
| Mantine | 9.6 | UI de admin-web (ADR 0006). |
| zod | 4.6 | Validación de formularios. |
| react-hook-form | 7.89 | Formularios. |
| axios | 1.20 | Cliente HTTP con interceptores (refresh). |
| zustand | 5 | Estado de cliente mínimo (sesión). |
| openapi-typescript | 7.13 | Genera `packages/api-types` (ADR 0003). Peer `typescript ^5`; en la práctica corre con TS 6 sin problema (solo `pnpm install` avisa). |
| eslint | **10.11.0** (admin-web) | — |
| typescript-eslint | **8.71.0** (admin-web) | Fija el techo de TypeScript en 6.0 (ver arriba). |
| vitest | **5.0.2** (admin-web) | Tests unitarios + Testing Library, jsdom. |

Las versiones exactas viven en `pom.xml`, `package.json` de cada app y Dockerfiles. Este ADR es la referencia; si cambian, se actualiza acá.

## Alternativas consideradas

- **Spring Boot 3.5.x:** más material disponible, pero sin soporte OSS durante la vida del proyecto. Descartado.
- **Spring Boot 4.2 (milestone) / Expo SDK 58 (beta) / MapStruct 1.7 (beta):** descartados por no ser GA.
- **TypeScript 7:** descartado por incompatibilidad con typescript-eslint 8.
- **Una sola versión de React en todo el monorepo:** ideal en teoría (ver ADR 0003), pero Expo/RN 0.86 exige React 19.2.3 exacto y React Router 8 exige `>=19.2.7`; no hay una versión que satisfaga ambos pisos a la vez. Se resolvió con dos versiones + resolución forzada de Metro hacia la de mobile (ver Decisión). Si esto da problemas de hooks duplicados o de tipos entre paquetes compartidos, aplicar el Plan B (React Router 7 en admin-web, una sola versión de React).

## Consecuencias

- Boot 4 / Security 7 / Hibernate 7 son recientes: parte de la documentación y ejemplos online son de Boot 3; verificar APIs contra la documentación oficial de la versión.
- Upgrades menores se hacen de forma explícita (PR propio) y actualizan este ADR.
- Dos versiones de React conviven en el `node_modules` hoisted (ADR 0003); `packages/api-types` y cualquier código compartido entre admin-web y mobile no debe importar React directamente (o usarlo solo por tipos) para no atarse a una de las dos copias.
