# ADR 0002 — Backend como monolito modular (package-by-feature)

- **Estado:** aceptado
- **Fecha:** 29/09/2026

## Contexto

Un equipo chico, un solo VPS, dominio acotado (definido en docs/scope.md). Microservicios agregarían costo operativo sin beneficio. Pero sí queremos límites claros entre áreas para que el código no se enrede y los agentes trabajen en paralelo.

## Decisión

Un único deployable Spring Boot organizado por feature:

```
com.example.app
├── config/                         # Security, OpenAPI, Jackson, JPA auditing, props
├── common/
│   ├── exception/                  # Handler global → ProblemDetail, excepciones base
│   ├── security/                   # Meta-anotaciones de rol, acceso al principal, JWT
│   ├── validation/                 # Validadores reutilizables
│   ├── persistence/                # BaseEntity, auditoría
│   ├── web/                        # PageResponse, utilidades de controller
│   └── util/
└── modules/<modulo>/
    ├── controller/
    ├── service/                    # API pública del módulo (puerto)
    ├── repository/
    ├── entity/                     # Entidades JPA y enums
    ├── dto/request/                # records con Bean Validation
    ├── dto/response/
    ├── mapper/                     # MapStruct
    └── exception/
```

**Módulos base:** `auth` y `users` (slice de referencia). Los módulos del producto se crean recién cuando una tarea los necesita.

### Reglas de capas

1. `controller → service → repository`. El controller no toca repositories ni entidades.
2. Las entidades nunca salen por la API: siempre DTOs (mapeados con MapStruct).
3. **Entre módulos solo vía service/puerto público** del otro módulo. Prohibido inyectar el repository o usar la entidad de otro módulo para mutarla. Las relaciones JPA entre módulos se permiten (p. ej. `Order → User`), pero la lógica sobre esa entidad la ejecuta su módulo dueño.
4. Sin dependencias cíclicas entre módulos. Si aparecen, se extrae un puerto o se invierte con un evento de aplicación (`ApplicationEventPublisher`).
5. `common/` no depende de `modules/`.
6. **JPA puro:** métodos derivados, `Specification`, `@EntityGraph`, JPQL si hace falta. **Prohibido** SQL nativo, `@Query(nativeQuery = true)` y `JdbcTemplate`.
7. Transacciones en el service (`@Transactional`, `readOnly = true` en lecturas).
8. Control de pertenencia (el usuario solo ve lo suyo) en el service, con el id del principal del JWT.

## Alternativas consideradas

- **Package-by-layer** (`controllers/`, `services/`…): mezcla dominios y dificulta ver límites. Descartado.
- **Microservicios:** costo operativo y de consistencia injustificado. Descartado.
- **Spring Modulith:** útil para verificar límites; no se adopta ahora para no sumar dependencia. Se puede agregar luego (o un test ArchUnit) sin reestructurar.

## Consecuencias

- Los límites dependen de disciplina y revisión (`qa-reviewer`); conviene un test de arquitectura (ArchUnit) cuando haya 3+ módulos.
- Separar un módulo en el futuro es viable porque ya se comunica vía servicios.
- Sin SQL nativo, reportes complejos (métricas) se resuelven con JPQL/Criteria y proyecciones.
