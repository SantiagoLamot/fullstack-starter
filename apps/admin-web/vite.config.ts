/// <reference types="vitest/config" />
import { fileURLToPath, URL } from 'node:url';

import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// Windows + bind mounts de Docker no propagan eventos de archivos: DEV_FILE_POLLING=true.
const usePolling = process.env.DEV_FILE_POLLING === 'true';
const pollingInterval = Number(process.env.DEV_FILE_POLLING_INTERVAL_MS ?? 1000);

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: true,
    port: 5173,
    strictPort: true,
    watch: {
      usePolling,
      interval: pollingInterval,
    },
    proxy: {
      // Mismo origen que en prod (nginx): la cookie de refresh (Path=/api/v1/auth) funciona sin CORS.
      '/api': {
        target: process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    host: true,
    port: 4173,
    strictPort: true,
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    restoreMocks: true,
  },
});
