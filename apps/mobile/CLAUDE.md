# mobile — app de usuarios (Expo)

Expo SDK 57 (React Native 0.86.3, **React 19.2.3 exacto** — no confundir con los 19.2.8 de
admin-web, ver "React único" más abajo) + TS ~6 (strict) + expo-router + TanStack Query 5 +
zustand 5 + react-hook-form 7 + zod 4 + axios. Rol USER. Textos de UI en español; código en
inglés. Auth real conectada al backend (login, activación, refresh, guards).

## Estructura

```
app/                        Solo rutas expo-router (pantallas delgadas)
  _layout.tsx                Providers globales (QueryClient, SafeArea, tema) + <Stack/>
  (auth)/login.tsx           Pública
  (app)/_layout.tsx          Guard de sesión: sin sesión -> redirige a (auth)/login
  (app)/index.tsx            "/" real (protegida); NO agregar un app/index.tsx aparte:
                              el grupo (app) no agrega segmento y ya sirve "/".
  +not-found.tsx
src/
  theme/                     Tokens (colors, typography, spacing, radii, shadows) + ThemeProvider
  shared/ui/                 Componentes base (Screen, Text, Button, TextField), 1 por archivo
  shared/lib/                http (axios), secure-storage (expo-secure-store), query-client,
                              problem-details (mismo contrato que admin-web)
  features/<f>/{components,schemas,store,...}/ + index.ts público
```

## Reglas

- Alias `@/` → `src`. Una feature se consume solo vía su `index.ts` (ESLint lo bloquea:
  `no-restricted-imports` sobre `@/features/*/*`).
- Todo estilo sale de `src/theme/` (nunca tamaños/colores sueltos en un componente). Los tokens
  siguen la marca (ver "Marca" más abajo).
- DTOs desde `@app/api-types` (`Schema<'TokenResponse'>`, `RequestBody<...>`). Ojo: las respuestas
  del OpenAPI vienen como `*/*`, por eso se usa `Schema<...>` y no `ResponseBody<...>`.
- Solo dependencias nativas instalables con `npx expo install` (compatibles con Expo Go SDK 57).
  `expo-camera` llega recién con la feature de QR.

## Marca (obligatorio en toda pantalla)

Fuente única: **`docs/brand/brand.md`**. Mobile es la experiencia de marca del producto.

- **Tokens** en `src/theme/colors.ts` (paleta + semánticos) y `src/theme/typography.ts` (familias + escala): son los **tokens reemplazables**. Sin hex ni tamaños sueltos fuera de `src/theme/`. Los componentes usan solo los semánticos (`background`, `surface`, `border`, `textPrimary`, `textSecondary`, `primary`, `accent`, `link`, `success`, `danger`, `warning`).
- **Tipografía:** por defecto fuentes del sistema. Para fuentes propias: instalar el paquete `@expo-google-fonts/*` con `npx expo install`, cargarlas con `useFonts` en `app/_layout.tsx` (manteniendo el splash hasta que carguen) y poner sus nombres en `fonts`.
- **Splash e ícono:** `app.config.ts` no declara ícono ni splash; agregarlos (`icon`, config de `expo-splash-screen`) cuando haya marca.
- **Nombre del producto:** `EXPO_PUBLIC_APP_NAME` (se muestra en `AuthBackdrop`); al aplicar la marca, reemplazarlo por el logo.
- **Sin zoom / responsive web:** no aplica en apps nativas.

## React único (decisión del orquestador) — verificado

admin-web fija react/react-dom **19.2.8** en su propio `apps/admin-web/node_modules` (pnpm crea
ahí una copia nested porque su versión difiere de la del resto del workspace). El resto del
workspace (incluido mobile) resuelve react/react-dom **19.2.3** desde el `node_modules` raíz
(`nodeLinker: hoisted`). Como mobile no tiene `apps/mobile/node_modules` propio, siempre mira la
raíz: ya alcanza para no mezclar versiones en el código propio de mobile.

Problema real encontrado: paquetes compartidos entre ambas apps (`react-hook-form`,
`@tanstack/react-query`, y sus dependencias transitivas `react-remove-scroll*`,
`use-callback-ref`, `use-sidecar`) también terminan con una copia **nested** de react 19.2.8
bajo su propio `node_modules/<paquete>/node_modules/react` (pnpm resuelve su peer contra la
versión "ganadora" del workspace). `npx expo-doctor` lo marca como "duplicate dependencies".

Mitigación en `metro.config.js`: `resolver.resolveRequest` intercepta _cualquier_ `require`
de `react` / `react-dom` / `react-native` / `react/jsx-runtime` / `react/jsx-dev-runtime`
(sin importar quién lo pida) y lo resuelve siempre contra la copia de la raíz del workspace
(19.2.3). Verificado con `expo export --platform android --dev --source-maps`: greppeando el
bundle resultante, **todas** las referencias a `node_modules/react/...` apuntan a
`../../node_modules/react` (raíz) y no aparece ningún módulo bajo
`react-hook-form/node_modules/react` ni ninguna otra copia nested — un solo React 19.2.3 en el
bundle final, pese a que `expo-doctor` sigue avisando sobre los duplicados en disco (advertencia
esperada/aceptada: es un análisis estático de `node_modules`, no del bundle real de Metro).

Si en algún momento esto se rompe (por ejemplo al agregar una lib que importe React de forma que
esquive `resolveRequest`), el plan B es el del orquestador: bajar admin-web a React Router 7 /
alinear versiones de React entre apps.

## Polling de archivos — RESUELTO por devops via puente de reinicio de proceso (2026-09-29)

Metro (via `@expo/metro-file-map`) no tiene modo polling propio: sus tres watchers posibles
(Watchman, `NativeWatcher` con `fs.watch` recursivo —deshabilitado en Linux—, `FallbackWatcher`
con `fs.watch` por directorio) dependen todos de inotify. `CHOKIDAR_USEPOLLING` /
`WATCHPACK_POLLING` no los lee Metro (son convenciones de Vite/webpack). Ademas, devops verifico
que `/repo` esta montado por **9p** (`cat /proc/mounts` dentro del contenedor), que directamente
no implementa notificacion de cambios: ni siquiera un `touch`/escritura hecha **desde dentro**
del contenedor dispara un evento `fs.watch` (probado con un script de Node apuntando a un archivo
real de este mount; el mismo `fs.watch` sobre `/tmp`, overlay nativo, si los recibe). No hay forma
de "fabricar" un evento inotify en este punto de montaje.

**Mitigacion real, activa por defecto en Windows (`DEV_FILE_POLLING=true`):**
`infra/docker/mobile/poll-bridge.sh` sondea (por `stat`, que si funciona bien sobre 9p)
`apps/mobile` y `packages/api-types/src` cada `DEV_FILE_POLLING_INTERVAL_MS`; al detectar un
cambio real y estable (con debounce), pide que `dev-entrypoint.sh` reinicie el **proceso**
`expo start` (no el contenedor) — un `expo start` nuevo hace un recrawl completo y sirve el
contenido actualizado. Verificado de punta a punta: editar un archivo de `src/` desde Windows,
sin tocar el contenedor, el bundle servido por `curl` a
`/node_modules/expo-router/entry.bundle?platform=android&dev=true` trae el cambio en unos
segundos. El reinicio de proceso es mas lento que un hot-reload real (recrawl completo de Metro),
pero no requiere `docker compose restart mobile` ni reinstalar dependencias. `docker compose
restart mobile` sigue siendo la opcion de respaldo si el puente no estuviera activo o algo se
traba. Detalle completo (por que `touch -c` no sirve en este mount, diseno del watcher, exclusion
de `expo-env.d.ts` para evitar bucles) en `docs/agents/devops.md`.

## Comandos (siempre en Docker, nunca Node en el host)

Con el stack de dev arriba: `docker compose exec mobile pnpm --filter @app/mobile <script>`.
Sin stack: `docker compose run --rm mobile pnpm --filter @app/mobile <script>`.

Scripts: `start` (`expo start`, lo usa el entrypoint del contenedor), `android`/`ios` (con
device/emulador), `lint`, `typecheck` (`tsc -b`), `format`/`format:check`, `test` (jest-expo +
@testing-library/react-native).

- Metro sirve en `0.0.0.0:8081` (puerto `EXPO_HOST_PORT` del `.env`, expuesto al LAN para el
  celular). `REACT_NATIVE_PACKAGER_HOSTNAME` (IP de la máquina en la LAN) es la que anuncia el QR
  de Expo Go; `EXPO_TUNNEL=true` cambia a `expo start --tunnel` (usa `@expo/ngrok`, instalado
  global en la imagen) para redes donde el celular no llega por IP directa (ver
  `infra/docker/mobile/dev-entrypoint.sh`, de devops).
- `EXPO_PUBLIC_API_URL` (del `.env`) viaja embebida en el bundle (no es secreta) y la lee
  `src/shared/lib/http.ts` vía `process.env.EXPO_PUBLIC_API_URL`; también queda en
  `Constants.expoConfig.extra.apiUrl` por si hace falta desde código nativo/config plugins.
- `node_modules` vive en volúmenes nombrados del compose (raíz y `packages/api-types`); **no
  hay volumen para `apps/mobile/node_modules`** (es un bind mount normal: si hace falta
  reinstalar desde cero, `docker compose exec mobile sh -c "rm -rf apps/mobile/node_modules"`
  es seguro porque NO es un mountpoint, a diferencia de la raíz o de `packages/api-types`).
- pnpm en esta versión (12.8.1) exige aprobar scripts de instalación nativos
  (`ERR_PNPM_IGNORED_BUILDS`) para `@parcel/watcher` (usado por jest-expo/watchman) y
  `unrs-resolver` (usado por ESLint): ya está resuelto vía `allowBuilds` en
  `pnpm-workspace.yaml` (raíz, compartido con el resto del repo). Si `pnpm add`/`install` corta
  con ese error para un paquete nuevo, es la misma causa: agregarlo a `allowBuilds` y volver a
  instalar (`pnpm approve-builds --all` lo hace y reescribe el yaml solo).
- TS: un único `tsconfig.json` (no hay solución con referencias como admin-web) que extiende
  `expo/tsconfig.base` + `../../tsconfig.base.json`; `types: ["jest"]` explícito porque sin eso
  TS no toma los globals de `@types/jest` en los archivos `*.test.ts`.

## Auth

- Refresh token solo en `expo-secure-store` (`refreshTokenStorage`); access token solo en memoria
  (zustand `useSessionStore`, arranca en `status: 'loading'`). Mobile NO manda `X-Requested-With`
  ⇒ el backend usa el refresh token en el body.
- Bootstrap (`app/_layout.tsx` → `bootstrapSession`): refresh con el token guardado (+ `/auth/me` si
  hace falta). Error de red en el refresh conserva el token (no desloguea); rechazo del servidor lo borra.
- Interceptor en `src/shared/lib/auth-interceptor.ts` (Bearer + UN refresh compartido con cola +
  `onAuthFailure` una vez). Se instala con `installHttpAuth()` (feature auth) para que `shared/lib`
  no importe features. `plainHttp` (sin interceptores) se usa para refresh y `/me`.
- Guards: `(auth)/_layout` (con sesión ⇒ `/`), `(app)/_layout` (sin sesión ⇒ login; rol != USER ⇒
  `NotUserNotice` con logout; `loading` ⇒ `LoadingScreen`).
- Activación: `app/(auth)/activar.tsx`, deep link `app://activar?token=...` (scheme en
  `app.config.ts`); sin token se pega token o link completo. Reglas de contraseña = DTO del backend
  (10-72, letra + número). Éxito ⇒ `/login?activated=1` con aviso.
- Base URL: `EXPO_PUBLIC_API_URL` en el `.env` raíz, incluye `/api/v1`; en dev
  `http://<IP-LAN-de-la-PC>:8080/api/v1` (el celular no ve `localhost`). Tras cambiarla, reiniciar Metro.
- `docker compose run --rm mobile <cmd>` NO sirve para checks (el entrypoint levanta Metro): usar
  `docker compose up -d mobile` + `docker compose exec -T mobile sh -c "cd apps/mobile && pnpm <script>"`.
