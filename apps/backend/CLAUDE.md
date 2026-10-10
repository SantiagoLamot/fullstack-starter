# Backend — reglas (apps/backend)

Java 21 + Spring Boot 4.1.1 (Spring 7, Security 7, Hibernate 7, Jackson 3), Maven wrapper. Paquete raíz `com.example.app` (placeholder; ver README para renombrarlo). Nombre visible del producto en mails: `APP_NAME`.

## Estructura

```
com.example.app
├── Application                  (@ConfigurationPropertiesScan)
├── config/        AppProperties (app.*), SecurityConfig, JpaConfig (auditing), TimeConfig (Clock), WebConfig, OpenApiConfig
├── common/
│   ├── exception/   ApplicationException → BadRequest(400) / Forbidden(403) / NotFound(404) / Conflict(409) / Business(422),
│   │                ProblemType (type URN + title ES), ProblemDetails, GlobalExceptionHandler
│   ├── security/    Role, @AdminOnly @UserOnly @AdminOrUser @Authenticated, @CurrentUser, SecurityUtils, JwtSecrets, SessionValidityChecker (puerto sid), PublicPostBearerTokenResolver
│   ├── mail/        MailDispatcher (único punto de envío; respeta MAIL_ENABLED)
│   ├── validation/  @MaxUtf8Bytes (límite de bytes UTF-8; BCrypt = 72)
│   ├── persistence/ BaseEntity (id IDENTITY + created/updated At/By)
│   └── web/         PageResponse<T>
└── modules/<modulo>/{controller,service,repository,entity,dto/request,dto/response,mapper,exception}
```

**Módulo nuevo:** crear `modules/<nombre>/` con esas subcarpetas (solo las que se usen). Entidades (y sus enums) van **siempre** en `entity/`, nunca en `domain/`, `model/` ni `entities/` (regla fija), y extienden `BaseEntity`. Registrar endpoints en `docs/security.md`.

## Reglas

- **Capas:** Controller → Service (interfaz + impl) → Repository. Controller sin lógica. Entidades nunca salen por la API: DTO `record` + MapStruct (`componentModel=spring`, `unmappedTargetPolicy=ERROR` ya configurados en el pom).
- Un módulo no usa el repository de otro: usa su service.
- **Persistencia = Hibernate, siempre** (JPA vía Spring Data JPA). Prohibido otro ORM o acceso sin ORM (MyBatis, jOOQ, JDBC, etc.).
- **JPA puro:** métodos derivados, Specifications, `@EntityGraph`. PROHIBIDO SQL nativo, `@Query(nativeQuery=true)`, `JdbcTemplate`. Esquema por `ddl-auto=update` (Flyway antes de prod, ADR 0004).
- Entidades: constructor `protected` vacío + factory/métodos de dominio (nada de setters públicos genéricos). Enums `@Enumerated(STRING)`. `@Version` en entidades editables concurrentemente.
- **Nombres de DTO únicos en todo el backend** (el OpenAPI usa el nombre simple de la clase): prefijar con el dominio (`CreateUserRequest`, no `CreateRequest`). Lo verifica `DtoNamesUniqueTest`.
- **Validación:** request DTO `record` con Bean Validation en cada campo; `@Valid @RequestBody`; `@Validated` en la clase. Mensajes en `ValidationMessages.properties` (español, UTF-8). Reglas de negocio en el service lanzando `ConflictException`/`BusinessException`/`NotFoundException` con mensaje en español (va al `detail`).
- **Errores:** ProblemDetail RFC 9457, `type` = `urn:app:problem:<slug>` (ver `ProblemType`), `title` en español; 400 con `errors: [{field, message}]`. 500 nunca expone detalles.
- **Seguridad:** todo controller con meta-anotación de rol a nivel de clase. Usuario actual SOLO por `@CurrentUser Long userId` (claim `sub`) o `SecurityUtils.currentUserId()`. JWT HS256, roles en claim `roles` (`["ADMIN"]` → `ROLE_ADMIN`). Whitelist en `SecurityConfig` (login, refresh, logout, invitations/accept, password-reset/request|confirm, health; Swagger solo perfil `dev`). Todo access token lleva `sid` (= familyId del refresh) y el `JwtDecoder` rechaza (401) si la sesión no está vigente (ADR 0009); en tests con token real usar `RefreshTokenService.startFamily(...).familyId()`. Mails siempre vía `MailDispatcher`, nunca `JavaMailSender` directo.
- "Ahora" siempre desde el bean `Clock`, nunca `Instant.now()` en código de negocio.
- Constructor injection, campos `final`. Lombok solo si no oculta lógica (no `@Data` en entidades).
- Config nueva: agregarla a `AppProperties` (validada) + `application.yml` leyendo env + `.env.example` (avisar a devops).
- **Seed dev:** `DevSeedRunner` (`@Profile("dev")`, corre antes del bootstrap) crea ADMIN y USER activos desde `DEV_SEED_*` (`app.dev-seed.*`); idempotente; sin password no crea el usuario.

## Build y tests (siempre en Docker)

Comando recomendado, desde la raíz del repo (usa el servicio `backend-test` del
compose, perfil `tools`; ya trae `TESTCONTAINERS_HOST_OVERRIDE`, el socket de
Docker y el volumen `fullstack-starter_m2` correctamente etiquetado por Compose):

```bash
scripts/backend-test.sh          # Mac / Linux / WSL2 (Git Bash en Windows)
scripts/backend-test.ps1         # Windows (PowerShell)
```

Podés pasar argumentos de Maven, p. ej. `scripts/backend-test.sh -Dtest=FooTest`.

**No uses un `docker run` suelto para esto** (quedó deprecado): crea el volumen
`fullstack-starter_m2` sin las labels que Compose espera, y Compose después avisa
`volume "fullstack-starter_m2" already exists but was not created by Docker Compose`
en cada comando (detectado y corregido por devops el 2026-09-29; ver
`docs/agents/devops.md`).

- Integración: extender `support.AbstractIntegrationTest` (MockMvc + MySQL 8.4 Testcontainers vía `@ServiceConnection`, perfil `test`).
- Seguridad: `jwt().authorities(new SimpleGrantedAuthority(Role.X.authority()))` para 403/OK; tokens reales con el bean `JwtEncoder`.
- Unitarios de services con Mockito (agente cargado por surefire).

## Contrato runtime (lo usa devops)

Puerto 8080 · perfil por `SPRING_PROFILES_ACTIVE` (sin default: si no se setea no hay Swagger) · MySQL en host `mysql` (`DB_HOST`/`DB_PORT`) · `/actuator/health` (+ `/liveness`, `/readiness`) · `/v3/api-docs` y `/swagger-ui.html` solo en `dev` · dev: `./mvnw spring-boot:run` con devtools (reinicia al cambiar `target/classes`) · prod: `./mvnw -B -DskipTests package` → `target/app.jar`.
