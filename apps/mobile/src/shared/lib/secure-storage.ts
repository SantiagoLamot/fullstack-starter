import * as SecureStore from 'expo-secure-store';

/**
 * Wrapper fino sobre expo-secure-store (Keychain/Keystore). Claves cortas,
 * sin espacios (requisito de SecureStore).
 */
export const secureStorage = {
  async get(key: string): Promise<string | null> {
    return SecureStore.getItemAsync(key);
  },
  async set(key: string, value: string): Promise<void> {
    await SecureStore.setItemAsync(key, value);
  },
  async remove(key: string): Promise<void> {
    await SecureStore.deleteItemAsync(key);
  },
} as const;

/** Claves usadas por la app. Centralizadas para evitar strings sueltos. */
export const SECURE_STORAGE_KEYS = {
  refreshToken: 'app-refresh-token',
} as const;

/** El refresh token vive SOLO acá (SecureStore); el access token, solo en memoria. */
export const refreshTokenStorage = {
  get: () => secureStorage.get(SECURE_STORAGE_KEYS.refreshToken),
  set: (token: string) => secureStorage.set(SECURE_STORAGE_KEYS.refreshToken, token),
  clear: () => secureStorage.remove(SECURE_STORAGE_KEYS.refreshToken),
};
