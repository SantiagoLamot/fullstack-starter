# ADR 0006 — Librerías de UI

- **Estado:** aceptado
- **Fecha:** 29/09/2026

## Contexto

El panel admin es una herramienta interna: prioridad claridad y funcionalidad (tablas, formularios, fechas, modales). La app móvil es la cara de marca para los usuarios finales y debe reflejar la guía de marca (docs/brand/brand.md).

## Decisión

- **Admin-web: Mantine 9.** Trae listos tablas, formularios, date pickers (`@mantine/dates`), modales, notificaciones y theming. Menos código propio que armar componentes.
- **Mobile: sin librería de UI.** Design system propio mínimo:
  - Tokens (colores, tipografías, espaciados, radios) en `apps/mobile/src/theme`, para cargar el brandbook cuando llegue.
  - Componentes base propios en `src/shared/ui` que consumen esos tokens.
  - `expo-image` para imágenes (cache) y `expo-font` para tipografías de marca.

## Alternativas consideradas

- **shadcn/ui + Tailwind (admin):** muy flexible, pero implica copiar y mantener cada componente y armar tablas/fechas a mano. Más código para un panel interno. Descartado.
- **MUI (admin):** equivalente funcional, más pesado y con estilo Material más marcado.
- **Librerías de UI para RN (Tamagui, NativeBase/gluestack, RN Paper):** imponen estilo o complejidad de build; con un brandbook propio terminaríamos sobreescribiendo casi todo.

## Consecuencias

- El admin-web tiene aspecto "Mantine" con el tema ajustado a colores de marca; aceptable para uso interno.
- Mobile requiere construir componentes base (botón, input, card, header), pero el brandbook se aplica en un solo lugar.
