/**
 * Acepta el token suelto o un link completo (`https://.../activar?token=XYZ` o
 * `app://activar?token=XYZ`) y devuelve solo el token.
 */
export function extractInvitationToken(input: string): string {
  const value = input.trim();
  const match = /[?&]token=([^&#\s]+)/.exec(value);
  if (!match?.[1]) return value;
  try {
    return decodeURIComponent(match[1]);
  } catch {
    return match[1];
  }
}
