import type { ExpoConfig } from 'expo/config';

/**
 * Config dinámica (permite leer env vars). `EXPO_PUBLIC_API_URL` viaja embebida en el
 * bundle (no es secreta) y se lee en runtime desde `src/shared/lib/http.ts`.
 *
 * PLACEHOLDERS a reemplazar al crear un proyecto desde la plantilla: name, slug, scheme,
 * bundleIdentifier y package (ver README, "Renombrar la plantilla").
 */
const config: ExpoConfig = {
  name: 'App',
  slug: 'app',
  scheme: 'app',
  version: '0.1.0',
  orientation: 'portrait',
  userInterfaceStyle: 'light',
  // Nueva arquitectura: default de Expo SDK 57 (no hace falta declararla acá).
  // Ícono y splash: agregar `icon` y la config de `expo-splash-screen` cuando haya marca.
  ios: {
    bundleIdentifier: 'com.example.app',
    supportsTablet: false,
  },
  android: {
    package: 'com.example.app',
  },
  plugins: ['expo-router', 'expo-secure-store', 'expo-image', 'expo-status-bar'],
  experiments: {
    typedRoutes: true,
  },
  extra: {
    apiUrl: process.env.EXPO_PUBLIC_API_URL ?? null,
    // Proyecto EAS (eas init), solo si se usan builds EAS.
    eas: { projectId: process.env.EAS_PROJECT_ID ?? null },
  },
};

export default config;
