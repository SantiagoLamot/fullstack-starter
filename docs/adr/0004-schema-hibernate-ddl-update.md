# ADR 0004 — Esquema con Hibernate `ddl-auto=update` (transitorio)

- **Estado:** aceptado (transitorio; se reemplaza antes de producción)
- **Fecha:** 29/09/2026

## Contexto

En las primeras fases el modelo cambia seguido y el plazo es corto. Escribir migraciones por cada iteración del modelo frena. Pero en producción necesitamos cambios de esquema reproducibles y revisables.

## Decisión

- **Por ahora:** `spring.jpa.hibernate.ddl-auto=update` en dev.
- **Seeds de dev:** `CommandLineRunner` anotados con `@Profile("dev")`, idempotentes (crean solo si no existe). El admin inicial toma sus credenciales de variables de entorno (`ADMIN_INITIAL_*`), nunca hardcodeadas.
- **PENDIENTE OBLIGATORIO antes de producción:** migrar a **Flyway** con migración baseline generada desde el esquema vigente y pasar a `ddl-auto=validate`. Ningún despliegue productivo se hace con `update`.

## Riesgos de `update` (por eso es transitorio)

- No borra columnas ni tablas: quedan columnas huérfanas (a veces `NOT NULL`, que rompen inserts).
- No renombra: un rename crea columna nueva y deja la vieja con los datos.
- No cambia tipos ni longitudes de forma confiable, ni ajusta constraints existentes.
- No migra datos.
- El esquema resultante depende del historial de cada base: dos entornos pueden divergir.

Mitigación en dev: ante cambios de modelo que `update` no resuelve, recrear la base (`docker compose down -v` sobre el volumen de MySQL) y re-seedear.

## Alternativas consideradas

- **Flyway desde el día uno:** lo correcto a largo plazo; se difiere por velocidad de iteración.
- **Liquibase:** más verboso; Flyway con SQL alcanza.
- **`create-drop`:** pierde datos en cada reinicio; incómodo para probar a mano.

## Consecuencias

- Tarea explícita antes de cualquier deploy productivo): baseline Flyway + `validate`.
- Las entidades deben declarar constraints (`unique`, `nullable`, longitudes) en las anotaciones JPA para que el baseline salga correcto.
- Nota: las migraciones Flyway son SQL de esquema (DDL), no contradicen la regla "sin SQL nativo" que aplica al código de acceso a datos.
