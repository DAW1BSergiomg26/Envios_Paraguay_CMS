# Bloque 5: API Documentation with OpenAPI / Swagger

**Date:** 2026-07-29
**Project:** Monteastur Envios CMS
**Branch:** feature/seguimiento-premium
**Status:** Design

## Objective

Add comprehensive OpenAPI 3.0 documentation to all 22 REST API endpoints using springdoc-openapi, with Swagger UI for interactive exploration.

## Motivation

- Zero API documentation exists — all endpoints are invisible to consumers
- No standardized error response contract visible to integrators
- API-first development needs documentation foundation
- Enables tools like code generation, contract testing, and client SDKs in the future

## Current State

### Dependencies

No OpenAPI/Swagger dependency in `pom.xml`.

### REST Controllers (6 controllers, 22 endpoints)

| Controller | Base Path | Endpoints |
|-----------|-----------|-----------|
| `AdminApiController` | `/api/v1/admin` | `GET /envios`, `GET /envios/{codigo}`, `PUT /envios/{codigo}/estado` |
| `ReservaApiController` | `/api/v1/admin/reservas` | `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |
| `ClienteApiController` | `/api/v1/cliente` | `GET /envios`, `GET /envios/{codigo}`, `GET /evidencias/{id}/archivo` |
| `TrackingApiController` | `/api/v1/tracking` | `GET /{codigo}` |
| `ReservaPublicApiController` | `/api/v1/reservas` | `POST /`, `GET /disponibilidad` |
| `PushSubscriptionController` | `/api/v1/push` | `POST /subscribe`, `POST /unsubscribe`, `POST /test` |

### DTOs (already exist in `dto/api/`)

- `TrackingDto`, `PublicTrackingDto`, `AdminEnvioResumenDto`, `ClienteEnvioResumenDto`
- `EventoDto`, `EvidenciaDto`, `ErrorDto`
- `ActualizarEstadoRequest`, `ActualizarReservaRequest`, `CrearReservaPublicRequest`
- `PushSubscriptionRequest`

### Security

- Admin endpoints: Basic Auth (InMemoryUserDetailsManager)
- Client endpoints: Session Cookie (`JSESSIONID`)
- Public endpoints: No auth required

## Design

### 1. Dependency

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

### 2. OpenAPI Bean

New class `OpenApiConfig` in `config/` package with `@Configuration` and `@OpenAPIDefinition`:

```java
@Bean
public OpenAPI enviosOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Monteastur Envíos API")
            .description("API REST de la plataforma logística premium España ⇢ Paraguay")
            .version("3.2")
            .contact(new Contact()
                .name("Monteastur Envíos")
                .email("admin@casarrural.com")))
        .components(new Components()
            .addSecuritySchemes("basicAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic")
                .description("Credenciales de administrador (usuario:contraseña)"))
            .addSecuritySchemes("cookieAuth", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("JSESSIONID")
                .description("Sesión de cliente autenticado vía portal /cliente/login")))
        .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
        .addSecurityItem(new SecurityRequirement().addList("cookieAuth"));
}
```

### 3. Configuration Properties

Add to `application.properties`:

```properties
springdoc.api-docs.path=/api/v1/docs
springdoc.swagger-ui.path=/api/v1/swagger-ui.html
springdoc.swagger-ui.tryItOutEnabled=false
springdoc.show-actuator=false
```

### 4. Tag Organization (6 tags)

| Tag | Controller | Security |
|-----|-----------|----------|
| `Admin Tracking` | `AdminApiController` | BasicAuth |
| `Admin Reservas` | `ReservaApiController` | BasicAuth |
| `Cliente` | `ClienteApiController` | CookieAuth |
| `Tracking Público` | `TrackingApiController` | None |
| `Reservas Público` | `ReservaPublicApiController` | None |
| `Push Notifications` | `PushSubscriptionController` | None |

### 5. Endpoint Documentation Annotations

Each endpoint gets:
- `@Operation(summary = "...", description = "...")`
- `@ApiResponse(responseCode = "200", description = "...", content = @Content(schema = @Schema(implementation = ...)))`
- `@ApiResponse(responseCode = "400|404|409|500", description = "...", content = @Content(schema = @Schema(implementation = ErrorDto.class)))`

Global error responses documented per-endpoint — each controller method annotates `@ApiResponse(responseCode = "400|404|409|500")` with `ErrorDto` schema for the exceptions it can throw. This keeps documentation accurate per operation rather than blanket-declaring errors that may not apply.

### 6. Schema Documentation

Add `@Schema(description = "...")` to key DTO fields:
- `ErrorDto.status` — Código de error HTTP
- `ErrorDto.timestamp` — Marca temporal del error
- `TrackingDto.codigoUnico` — Código único de seguimiento (formato MT-YYYY-NNNN)
- etc.

## Files to Modify

| File | Change |
|------|--------|
| `pom.xml` | Add springdoc dependency |
| `application.properties` | Add springdoc config |
| `config/SecurityConfig.java` | Permitir `/api/v1/docs`, `/api/v1/swagger-ui.html`, `/v3/api-docs/**`, `/swagger-ui/**` sin autenticación |
| `config/OpenApiConfig.java` | **NEW** — OpenAPI bean |
| `controller/api/AdminApiController.java` | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `controller/api/ReservaApiController.java` | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `controller/api/ClienteApiController.java` | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `controller/api/TrackingApiController.java` | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `controller/api/ReservaPublicApiController.java` | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `controller/api/PushSubscriptionController.java` | Add `@Tag`, `@Operation`, `@ApiResponse` |
| `controller/GlobalExceptionHandler.java` | No changes needed (errors documented per endpoint) |
| `dto/api/ErrorDto.java` | Add `@Schema` annotations |
| Other DTOs | Optional `@Schema` refinements |

## Non-Goals

- No API versioning changes (stays at `/api/v1/`)
- No endpoint behavior changes
- No new endpoints
- No autogenerated client code
- No migration of existing clients

## Out of Scope (Future Bloques)

- Real API versioning strategy (v1 → v2)
- Rate limiting documentation
- Client SDK generation
- API changelog automation
