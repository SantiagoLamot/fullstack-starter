# Marca: guía para el producto digital

> Plantilla. Es la fuente única de UI para admin-web y mobile. Mientras no haya marca, el tema neutro de la plantilla es válido. Al recibir el brandbook, completar este archivo y reemplazar los tokens (ver sección 7).

## 1. Esencia

_Qué transmite la marca, en 3-5 líneas._

## 2. Voz y tono

- Idioma: español rioplatense (voseo), claro y breve.
- Errores, fechas límite y pagos: claros y neutros, sin guiños.
- _Tono propio de la marca para bienvenida, vacíos y éxitos._

## 3. Logo

_Versiones, área de seguridad, usos prohibidos. Assets en `docs/brand/assets/` (copiar a cada app solo los que se usan, sin editarlos)._

## 4. Color

| Token | Hex | Uso |
|---|---|---|
| `brand` | `#4263EB` | Color principal (botones rellenos, links, nav activo) |
| `neutral` | `#495057` | Texto principal, bordes |
| `success` | `#2F9E44` | Éxito, estado activo |
| `danger` | `#E03131` | Errores, acciones destructivas |
| `warning` | `#B26B00` | Avisos, pendiente |

Reglas de contraste: definir qué combinaciones están prohibidas (ratio mínimo 4.5:1 para texto).

## 5. Tipografía

_Familias (títulos / cuerpo), tamaños y escala. Por defecto: fuentes del sistema._

## 6. Sistema visual

_Radios, sombras, íconos (línea fina), ilustraciones, patterns, movimiento._

## 7. Aplicación por app

- **admin-web:** tokens en `apps/admin-web/src/theme/theme.ts` (escalas de 10 tonos, color oficial en el índice 7), `APP_BG`, `FONT_*`. Logo en `AppLayout` y `PublicPageLayout`. Sobrio y denso.
- **mobile:** tokens en `apps/mobile/src/theme/colors.ts` y `typography.ts`. Splash e ícono en `app.config.ts`. Logo en `AuthBackdrop`. Experiencia de marca.
- **Responsive y sin zoom (web):** reglas fijas en `CLAUDE.md`; no dependen de la marca.

## 8. Preguntas abiertas de marca

_Lo que falta definir con el cliente._
