// @ts-check
const { getDefaultConfig } = require('expo/metro-config');
const path = require('node:path');

const projectRoot = __dirname;
const workspaceRoot = path.resolve(projectRoot, '../..');

const config = getDefaultConfig(projectRoot);

// -----------------------------------------------------------------------------
// Soporte monorepo (pnpm, nodeLinker: hoisted): Metro tiene que poder resolver
// módulos que viven en la raíz del workspace, no solo en apps/mobile/node_modules.
// -----------------------------------------------------------------------------
config.watchFolders = [workspaceRoot];
config.resolver.nodeModulesPaths = [
  path.resolve(projectRoot, 'node_modules'),
  path.resolve(workspaceRoot, 'node_modules'),
];

// -----------------------------------------------------------------------------
// React único (decisión del orquestador): admin-web fija react/react-dom 19.2.8 en
// su propio apps/admin-web/node_modules (override local de pnpm), mobile fija
// 19.2.3 en la raíz del workspace (root node_modules). Con `nodeLinker: hoisted`
// eso ya alcanza para que cada app resuelva "su" React por proximidad de carpeta,
// pero fijamos acá la resolución explícita de react/react-dom/react-native/
// react/jsx-runtime a la copia de la raíz para blindarnos de que algún paquete con
// una dependencia mal declarada termine arrastrando la copia de admin-web (o
// cualquier otra nested) al bundle de Metro.
// -----------------------------------------------------------------------------
const PINNED_TO_ROOT = new Set([
  'react',
  'react/jsx-runtime',
  'react/jsx-dev-runtime',
  'react-dom',
  'react-native',
]);

const originalResolveRequest = config.resolver.resolveRequest;

config.resolver.resolveRequest = (context, moduleName, platform) => {
  if (PINNED_TO_ROOT.has(moduleName)) {
    return context.resolveRequest(
      { ...context, originModulePath: path.join(workspaceRoot, 'package.json') },
      moduleName,
      platform,
    );
  }
  if (originalResolveRequest) {
    return originalResolveRequest(context, moduleName, platform);
  }
  return context.resolveRequest(context, moduleName, platform);
};

// -----------------------------------------------------------------------------
// Polling de archivos (DEV_FILE_POLLING=true, Windows):
// Metro no tiene modo polling (sus watchers usan fs.watch/inotify) y en Docker
// Desktop Windows el repo se monta por 9p, que no emite ningún evento de cambio.
// Por eso la detección vive FUERA de Metro: infra/docker/mobile/poll-bridge.sh
// sondea los archivos y el entrypoint reinicia el proceso `expo start` (no el
// contenedor) cuando algo cambia. Detalle en docs/adr/0007 y apps/mobile/CLAUDE.md.
// Acá solo se desactiva Watchman (no está en la imagen).
// -----------------------------------------------------------------------------
const pollingEnabled = process.env.DEV_FILE_POLLING === 'true';
if (pollingEnabled) {
  config.resolver.useWatchman = false;
}

module.exports = config;
