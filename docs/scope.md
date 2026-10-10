# Alcance

> Plantilla. El orquestador completa este archivo a partir del brief del cliente. **Regla: si algo no está acá, no se implementa**; se anota como pregunta abierta.

## Objetivo

_Qué es el producto, para quién y qué problema resuelve (3-5 líneas)._

## Incluido (por prioridad de desarrollo)

Ya viene resuelto por la plantilla (no hace falta pedirlo): login, invitación/activación de cuentas, refresh de sesión con rotación, logout, restablecer y cambiar contraseña, roles ADMIN/USER, ABM de usuarios en el admin, mail por SMTP.

1. _Funcionalidad 1_
2. _Funcionalidad 2_

## Excluido

- _Lo que explícitamente no se hace._

## Cronograma

_Hitos y fechas (se detalla por fases en `docs/roadmap.md`)._

## Preguntas abiertas

| # | Pregunta | Supuesto vigente | Estado |
|---|---|---|---|
| P1 | _..._ | _..._ | abierta |

## Pendientes técnicos

- Migrar de `ddl-auto=update` a Flyway + `validate` antes de producción (ADR 0004).
- CI y deploy (definir al llegar a producción).
