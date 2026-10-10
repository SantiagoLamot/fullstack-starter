import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import reactHooks from 'eslint-plugin-react-hooks';
import simpleImportSort from 'eslint-plugin-simple-import-sort';
import { defineConfig, globalIgnores } from 'eslint/config';
import globals from 'globals';
import tseslint from 'typescript-eslint';

// Sin eslint-config-expo: no tiene release compatible con ESLint 10 / TS 6 al
// momento de escribir esto. Replica las reglas de apps/admin-web/eslint.config.js
// (mismo criterio de import sort y "una feature solo vía su index.ts") + reglas
// básicas de React Native (sin `react-native/*` porque tampoco soporta ESLint 10
// todavía; se puede sumar cuando publiquen una versión compatible).
export default defineConfig(
  globalIgnores(['dist', 'coverage', 'node_modules', '.expo']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [js.configs.recommended, ...tseslint.configs.recommendedTypeChecked],
    languageOptions: {
      ecmaVersion: 2023,
      globals: { ...globals.node, ...globals['shared-node-browser'] },
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    plugins: {
      'react-hooks': reactHooks,
      'simple-import-sort': simpleImportSort,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'simple-import-sort/imports': [
        'error',
        {
          // 1) side effects, 2) node:, 3) paquetes, 4) @app/*, 5) alias @/, 6) relativos
          groups: [
            [String.raw`^\u0000`],
            ['^node:'],
            [String.raw`^@?\w`],
            ['^@app/'],
            ['^@/'],
            [String.raw`^\.`],
          ],
        },
      ],
      'simple-import-sort/exports': 'error',
      '@typescript-eslint/consistent-type-imports': 'error',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      // Una feature solo se consume vía su index.ts público.
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['@/features/*/*'],
              message: 'Importá la feature desde su index.ts (@/features/<feature>).',
            },
          ],
        },
      ],
    },
  },
  {
    files: ['*.config.{js,ts}', 'jest.setup.ts'],
    languageOptions: { globals: globals.node },
  },
  {
    files: ['**/*.js'],
    extends: [tseslint.configs.disableTypeChecked],
  },
  prettier,
);
