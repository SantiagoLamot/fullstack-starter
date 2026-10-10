import {
  createTheme,
  type CSSVariablesResolver,
  Input,
  type MantineColorsTuple,
} from '@mantine/core';

/*
 * TOKENS DE MARCA (reemplazables). Ver docs/brand/brand.md.
 *
 * Para aplicar la marca de un proyecto nuevo: cambiar solo los hex de las escalas de abajo, la
 * tipografía (FONT_*) y el fondo (APP_BG). Todas las escalas tienen 10 tonos (0 = más claro, 9 =
 * más oscuro) y el color "oficial" va SIEMPRE en el índice 7 (= primaryShade), así `filled` usa ese hex.
 *
 *  brand    color principal: botones rellenos, links, nav activo.
 *  neutral  texto principal, bordes y superficies neutras.
 *  success  funcional: éxito, estado "activo".
 *  danger   funcional: errores, acciones destructivas.
 *  warning  funcional: avisos, estado "pendiente".
 *
 * Los nombres `red`, `yellow` y `green` de Mantine se redirigen a danger/warning/success para que
 * cualquier componente que los use por defecto respete la paleta.
 */
const brand: MantineColorsTuple = [
  '#EDF2FF',
  '#DBE4FF',
  '#BAC8FF',
  '#91A7FF',
  '#748FFC',
  '#5C7CFA',
  '#4C6EF5',
  '#4263EB',
  '#3B5BDB',
  '#364FC7',
];
const neutral: MantineColorsTuple = [
  '#F8F9FA',
  '#F1F3F5',
  '#E9ECEF',
  '#DEE2E6',
  '#CED4DA',
  '#ADB5BD',
  '#868E96',
  '#495057',
  '#343A40',
  '#212529',
];
const success: MantineColorsTuple = [
  '#EBFBEE',
  '#D3F9D8',
  '#B2F2BB',
  '#8CE99A',
  '#69DB7C',
  '#51CF66',
  '#40C057',
  '#2F9E44',
  '#2B8A3E',
  '#237032',
];
const danger: MantineColorsTuple = [
  '#FFF5F5',
  '#FFE3E3',
  '#FFC9C9',
  '#FFA8A8',
  '#FF8787',
  '#FF6B6B',
  '#FA5252',
  '#E03131',
  '#C92A2A',
  '#A61E1E',
];
const warning: MantineColorsTuple = [
  '#FFF9DB',
  '#FFF3BF',
  '#FFEC99',
  '#FFE066',
  '#FFD43B',
  '#FCC419',
  '#FAB005',
  '#B26B00',
  '#8F5600',
  '#6F4300',
];

const FONT_BODY =
  'system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif';
const FONT_HEADINGS = FONT_BODY;
const APP_BG = '#F8F9FA';

/** Fondo de app y borde por defecto. Paper/Header/Navbar/Modal siguen en blanco. */
export const cssVariablesResolver: CSSVariablesResolver = () => ({
  variables: { '--app-bg': APP_BG },
  light: { '--mantine-color-default-border': 'var(--mantine-color-neutral-3)' },
  dark: {},
});

export const theme = createTheme({
  primaryColor: 'brand',
  primaryShade: 7,
  autoContrast: true,
  colors: {
    brand,
    neutral,
    success,
    danger,
    warning,
    red: danger,
    yellow: warning,
    green: success,
  },
  black: '#212529',
  fontFamily: FONT_BODY,
  headings: { fontFamily: FONT_HEADINGS, fontWeight: '600' },
  defaultRadius: 'md',
  cursorType: 'pointer',
  components: {
    Paper: { defaultProps: { withBorder: true, radius: 'lg' } },
    // Regla "Sin zoom": iOS hace zoom al enfocar un campo con font-size < 16px. Todos los campos de
    // Mantine (TextInput, PasswordInput, Select, NumberInput, Textarea, ...) se apoyan en Input.
    Input: Input.extend({ styles: { input: { fontSize: 16 } } }),
  },
});
