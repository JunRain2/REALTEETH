# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture

Spring Boot 3.5.11 + Kotlin + WebFlux (reactive) + R2DBC (MySQL). All I/O uses **suspend functions** — no Flux/Mono in application code.

### DDD Layer Structure

```
controller/      → Presentation: @RestController, Request/Response DTOs
service/         → Application: orchestrates domain + infrastructure
domain/
  model/         → Pure Kotlin entities (no Spring annotations)
  repository/    → Port interfaces (dependency inversion point)
  service/       → Domain logic not belonging to a single entity
infrastructure/
  persistence/   → R2DBC entities, CoroutineCrudRepository, Repository adapters
  config/        → Spring configuration
```

**Dependency direction:** `controller → service → domain ← infrastructure`
- `domain` has zero dependency on Spring or infrastructure.
- `infrastructure/persistence/ExampleRepositoryImpl` implements `domain/repository/ExampleRepository` (Adapter pattern).
- `service` depends only on the domain repository interface (DIP).

### Configuration Externalization

Values that affect performance or behavior — but do not drive code branching — must not be hardcoded. Manage them under the `app.*` namespace in `application.yml`.

**Externalize:**
- Scheduler intervals (`fixed-delay-ms`), distributed lock durations (`lock-at-most-for`)
- Thread pool sizes, graceful shutdown timeouts
- Retry counts, polling intervals, external API timeouts

**Do not externalize:**
- Class-level annotation attributes that require compile-time constants (e.g. `@EnableSchedulerLock(defaultLockAtMostFor = ...)`)
- External API protocol constants (e.g. HTTP status codes)

### Key Conventions

- R2DBC driver: `io.asyncer:r2dbc-mysql:1.3.0` (not in Spring BOM — version must be explicit).
- DB schema is managed by `src/main/resources/init-db.sql`, mounted into Docker at `/docker-entrypoint-initdb.d/`. No Flyway.
- `@EnableR2dbcRepositories` is omitted — Spring Boot auto-configuration handles repository scanning.
- Global exception handling is in `controller/GlobalExceptionHandler.kt` (`@RestControllerAdvice`). All business errors must be thrown as `BusinessException(ErrorCode.XXX)` — do **not** throw `NoSuchElementException` or `IllegalArgumentException` directly.

### Exception Handling (Strict Design)

- `domain/exception/ErrorCode.kt` — enum defining all business error codes (`status: HttpStatus`, `message: String`).
- `domain/exception/BusinessException.kt` — wraps an `ErrorCode`. Always use this for business errors.
- `controller/GlobalExceptionHandler.kt` handles three categories:
  | Exception | Handler | HTTP |
  |-----------|---------|------|
  | `BusinessException` | `handleBusiness` | `errorCode.status` |
  | `WebExchangeBindException` / `ConstraintViolationException` | validation handlers | 400 |
  | `Exception` (catch-all) | `handleUnexpected` (logs via kLogger) | 500 |
- All handlers return `ResponseEntity<ApiResponse<Nothing>>` using `ApiResponse.error(...)`.
- New error codes go in `ErrorCode.kt` only — never add new exception types.

### API Response Convention

All endpoints use `ApiResponse<T>` (`controller/dto/ApiResponse.kt`) as the unified response wrapper:

```json
// 성공 (data 있음)
{ "success": true, "data": { ... }, "error": null }

// 성공 (data 없음)
{ "success": true, "data": null, "error": null }

// 실패
{ "success": false, "data": null, "error": { "code": "...", "message": "...", "details": [...] } }
```

- Success: `ApiResponse.ok(data)` or `ApiResponse.ok()`
- Error: `ApiResponse.error(code, message, details)` — used only in `GlobalExceptionHandler`
- `ApiError` (`controller/dto/ApiError.kt`) holds `code`, `message`, `details`
- `ErrorResponse` is removed — do not use it.

### Code Style (ktlint)

- ktlint `1.5.0` via `org.jlleitschuh.gradle.ktlint 12.1.2`. Google code style (`ktlint_code_style = google` in `.editorconfig`).
- Run `./gradlew ktlintCheck` to lint, `./gradlew ktlintFormat` to auto-fix.
- `.editorconfig` is the single source of truth for formatting rules (indent 4, max line 140, trailing comma allowed, wildcard imports disabled).
- Logging uses `io.github.oshai:kotlin-logging-jvm` — declare logger as top-level: `private val log = KotlinLogging.logger {}`.