# admin-web — panel administrativo

React 19.2 + TS 6 (strict) + Vite 8 + Mantine 9 + React Router 8 + TanStack Query 5 + react-hook-form/zod 4 + axios. Roles: ADMIN (panel) y USER (usa la app móvil). Textos de UI en español; código en inglés.

## Estructura

```
src/
  main.tsx                 CSS de Mantine + render de <App/>
  app/                     App.tsx, router.tsx, providers/, layout/ (AppShell), guards/, pages/ (404, sin permisos)
  routes/                  paths tipados con rol requerido (fuente única de URLs)
  features/<f>/            {api,components,hooks,pages,schemas,types} + index.ts público (crear solo lo que se usa)
  shared/config/env.ts     variables VITE_* validadas con zod
  shared/lib/              http/ (axios), auth/ (useAuth, tokenStore, Role), query-client.ts, problem-details.ts
  theme/theme.ts           tema Mantine
  test/                    setup de Vitest + renderWithProviders
```

## Reglas

- Alias `@/` → `src`. Una feature importa de otra solo vía `@/features/<f>` (ESLint lo bloquea). Un componente por archivo.
- Imports ordenados por ESLint (`simple-import-sort`): css → `node:` → paquetes → `@app/*` → `@/` → relativos. `pnpm lint --fix` los ordena.
- DTOs siempre de `@app/api-types` (`Schemas['X']`, `RequestBody<path, method>`, `ResponseBody<...>`); nunca redefinirlos a mano.
- Datos del servidor con TanStack Query (`queryClient` en `shared/lib/query-client.ts`; no reintenta 4xx). Llamadas HTTP solo con `http` de `@/shared/lib/http` (baseURL `/api/v1`, `withCredentials`).

## Marca, responsive y sin zoom (obligatorio en toda pantalla)

Fuente única de marca: **`docs/brand/brand.md`**. El admin es una herramienta sobria: prioridad a densidad, legibilidad y velocidad de uso.

- **Tema Mantine** (`src/theme/theme.ts`): sin hex fuera del tema. Las escalas `brand`, `neutral`, `success`, `danger`, `warning` son los **tokens reemplazables** de la marca (color oficial en el índice 7 = `primaryShade`); `red`/`yellow`/`green` se redirigen a los funcionales. Fondo de app: `--app-bg` (`APP_BG` en el tema).
- **Contraste:** respetar los ratios que defina `brand.md`; estados siempre con texto, no solo color.
- **Íconos** Tabler de línea (stroke 1.5). Nombre del producto: `env.appName` (`VITE_APP_NAME`); al aplicar la marca, reemplazarlo por el logo en `AppLayout` y `PublicPageLayout`.
- **Responsive siempre:** toda pantalla se ve bien en 360, 768, 1024, 1280 y 1920 px, sin scroll horizontal de página. `AppLayout` usa burger + drawer en mobile; tablas dentro de `Table.ScrollContainer`; filtros en `Group` que envuelve.
- **Sin zoom:** `index.html` con viewport `width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no`; el tema fija `font-size: 16px` en `Input` (cubre TextInput, PasswordInput, Select, NumberInput, Textarea...); `theme/global.css` aplica `touch-action: manipulation` en `body` y 16px a inputs nativos. No introducir inputs con tamaño menor a 16px.

## Rutas y guards

- Toda ruta se declara en `src/routes/paths.ts` (`routes.<key> = { path, access, label }`, `access: 'public' | Role[]`). Usar `paths.<key>` / `buildPath(path, params)`; nada de strings sueltos.
- En `app/router.tsx`: rutas privadas bajo `<RequireAuth/>` (sin sesión → `/login` con `state.from`) y luego `<RequireRole access={routes.x.access}/>` (sin rol → "Sin permisos"). Es solo UX: el backend autoriza.
- `useAuth()` → `{ status, user, roles, login, logout }` (contexto de `AuthProvider`, montado en `AppProviders`). Solo ADMIN entra al panel (`PANEL_ROLES`); otros roles ven "Sin permisos". Para sumar un rol o una pantalla: `routes/paths.ts` + `app/router.tsx` + `NAV_ITEMS`.
- El menú lateral (`app/layout/AppLayout.tsx`, `NAV_ITEMS`) se filtra por rol con `canAccess`.

## Formularios y errores

- react-hook-form + `zodResolver(schema)`; el schema en `features/<f>/schemas/` replica las validaciones del request DTO del backend.
- Errores 400 (ProblemDetail RFC 9457 con `errors[{field,message}]`): `applyProblemFieldErrors(getProblemDetails(err), setError, [campos])` los asigna a los campos y devuelve los no mapeados → mostrarlos con `notifications.show`.

## Auth (ADR 0005)

Access token solo en memoria (`tokenStore`); el interceptor de request agrega `Authorization: Bearer`. Header `X-Requested-With` en todas las requests: el refresh viaja en cookie httpOnly (`Path=/api/v1/auth`, mismo origen vía proxy de Vite / nginx), nunca en el body. Arranque: `AuthProvider` hace `refreshAccessToken()` (vuelo único) + `/auth/me`. `shared/lib/http/refresh-interceptor.ts`: ante 401 un solo refresh con cola, reintento único y, si falla, `notifySessionExpired` (logout + limpia caché). Los endpoints de sesión usan `authClient` (sin interceptores) en `shared/lib/auth/session.ts`.

## Comandos (siempre en Docker, nunca Node en el host)

Con el stack de dev arriba: `docker compose exec admin-web pnpm --filter @app/admin-web <script>`.
Sin stack: `docker compose run --rm admin-web pnpm --filter @app/admin-web <script>`.

Scripts: `dev`, `build` (`tsc -b && vite build`), `preview`, `lint`, `typecheck` (`tsc -b`), `format`, `format:check`, `test` (Vitest + Testing Library, jsdom), `test:watch`.

- `node_modules` viven en volúmenes nombrados del compose, no en el bind mount. **No borrar `node_modules` desde el host** mientras haya contenedores: rompe los mountpoints (errores `File exists (os error 17)` en `pnpm install`).
- Vite: puerto 5173 (`strictPort`), `host: true`, proxy `/api` → `VITE_API_PROXY_TARGET` (default `http://localhost:8080`), polling si `DEV_FILE_POLLING=true` (intervalo `DEV_FILE_POLLING_INTERVAL_MS`).
- TS: `tsconfig.json` (solución) → `tsconfig.app.json` (src) + `tsconfig.node.json` (vite.config.ts); ambos extienden `../../tsconfig.base.json`.
