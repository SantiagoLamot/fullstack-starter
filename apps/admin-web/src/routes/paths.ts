import type { Role } from '@/shared/lib/auth';

/** `public`: sin sesión. Lista de roles: requiere sesión y al menos uno de esos roles. */
export type RouteAccess = 'public' | readonly Role[];

interface RouteDefinition {
  path: string;
  access: RouteAccess;
  /** Texto para navegación / títulos (español). */
  label: string;
}

/** Roles con acceso a las pantallas del panel: solo ADMIN (USER usa la app móvil). */
export const PANEL_ROLES = ['ADMIN'] as const satisfies readonly Role[];

/** Mi cuenta: todos los roles del panel. */
export const ACCOUNT_ROLES = ['ADMIN'] as const satisfies readonly Role[];

/**
 * Fuente única de rutas del panel. Nunca escribir paths como string suelto:
 * usar `paths.<ruta>` o `buildPath(...)`. Cada feature nueva agrega su entrada acá, en
 * `app/router.tsx` y (si va en el menú) en `NAV_ITEMS` de `app/layout/AppLayout.tsx`.
 */
export const routes = {
  login: { path: '/login', access: 'public', label: 'Iniciar sesión' },
  // Link del mail de invitación (ADR 0005).
  activate: { path: '/activar', access: 'public', label: 'Activar cuenta' },
  // Restablecer contraseña: públicas, también las usan los usuarios de la app (link del mail).
  forgotPassword: {
    path: '/olvide-contrasena',
    access: 'public',
    label: 'Restablecer contraseña',
  },
  resetPassword: { path: '/restablecer', access: 'public', label: 'Nueva contraseña' },
  account: { path: '/mi-cuenta', access: ACCOUNT_ROLES, label: 'Mi cuenta' },
  dashboard: { path: '/', access: PANEL_ROLES, label: 'Inicio' },
  users: { path: '/usuarios', access: PANEL_ROLES, label: 'Usuarios' },
} as const satisfies Record<string, RouteDefinition>;

export type RouteKey = keyof typeof routes;

export const paths = Object.fromEntries(
  Object.entries(routes).map(([key, route]) => [key, route.path]),
) as { readonly [K in RouteKey]: (typeof routes)[K]['path'] };

type PathParams<P extends string> = P extends `${string}:${infer Param}/${infer Rest}`
  ? { [K in Param | keyof PathParams<Rest>]: string | number }
  : P extends `${string}:${infer Param}`
    ? { [K in Param]: string | number }
    : Record<never, never>;

/** Reemplaza `:param` en el path con valores tipados. */
export function buildPath<P extends string>(path: P, params?: PathParams<P>): string {
  if (!params) return path;
  return path.replace(/:(\w+)/g, (_match, name: string) => {
    const value = (params as Record<string, string | number | undefined>)[name];
    if (value === undefined) throw new Error(`Falta el parámetro "${name}" para ${path}`);
    return encodeURIComponent(String(value));
  });
}

/** ¿El usuario con estos roles puede entrar a la ruta? */
export function canAccess(access: RouteAccess, userRoles: readonly Role[]): boolean {
  if (access === 'public') return true;
  return access.some((role) => userRoles.includes(role));
}

/**
 * Destino tras el login: `from` solo si es una ruta privada conocida a la que el rol puede
 * entrar; en cualquier otro caso (vacío, login/activar, ruta de otro rol o inexistente) la
 * pantalla inicial del rol.
 */
export function resolvePostLoginPath(from: string | undefined, userRoles: readonly Role[]): string {
  const home = homePath(userRoles);
  if (!from?.startsWith('/') || from.startsWith('//')) return home;
  const pathname = from.split(/[?#]/)[0] ?? '';
  const route = Object.values(routes).find((candidate) => candidate.path === pathname);
  if (!route || route.access === 'public') return home;
  if (!canAccess(route.access, userRoles)) return home;
  // El inicio ("/") lo resuelve HomeRoute, que reusa homePath.
  return from;
}

/**
 * Pantalla inicial según los roles. Hoy solo ADMIN entra al panel; si se suman roles con otra
 * pantalla inicial, resolverlo acá.
 */
export function homePath(_userRoles: readonly Role[]): string {
  return routes.dashboard.path;
}
