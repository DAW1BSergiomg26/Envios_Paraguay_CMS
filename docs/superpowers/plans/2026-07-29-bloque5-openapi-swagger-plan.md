# Bloque 5: OpenAPI / Swagger Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Document all 22 REST API endpoints with OpenAPI 3.0 via springdoc-openapi, accessible via Swagger UI.

**Architecture:** Springdoc auto-discovers `@RestController` endpoints and infers schemas from existing DTOs (`dto/api/`). We add `@Operation`, `@ApiResponse`, `@Tag` annotations to controllers and `@Schema` to key DTO fields. A single `OpenApiConfig` bean defines metadata and security schemes (BasicAuth for admin, Cookie auth for client portal).

**Tech Stack:** Springdoc-openapi v2.6.0, Spring Boot 3.3.5, Jakarta EE

## Global Constraints

- Use `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0`
- OpenAPI paths: `/api/v1/docs` (JSON), `/api/v1/swagger-ui.html` (UI)
- Security: `/api/v1/docs`, `/api/v1/swagger-ui.html`, `/v3/api-docs/**`, `/swagger-ui/**` must be publicly accessible (SecurityConfig)
- All existing tests (47) must keep passing
- No endpoint behavior changes — documentation only

---

### Task 1: Dependency + Base Configuration

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/java/com/monteastur/envios/config/SecurityConfig.java`

**Interfaces:**
- Consumes: (none)
- Produces: Springdoc auto-configuration activated, Swagger UI paths publicly accessible

- [ ] **Step 1: Add springdoc dependency to pom.xml**

Insert after the `spring-boot-starter-test` block:

```xml
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.6.0</version>
        </dependency>
```

- [ ] **Step 2: Add springdoc properties to application.properties**

Append at the end of `application.properties`:

```properties
# =========================
# OPENAPI / SWAGGER
# =========================

springdoc.api-docs.path=/api/v1/docs
springdoc.swagger-ui.path=/api/v1/swagger-ui.html
springdoc.swagger-ui.tryItOutEnabled=false
springdoc.show-actuator=false
```

- [ ] **Step 3: Permit Swagger UI paths in SecurityConfig**

Edit `SecurityConfig.java` line 47. Change:

```java
.requestMatchers("/admin/**", "/api/v1/admin/**").authenticated()
```

to:

```java
.requestMatchers("/admin/**", "/api/v1/admin/**").authenticated()
.requestMatchers("/api/v1/docs", "/api/v1/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
```

- [ ] **Step 4: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/application.properties src/main/java/com/monteastur/envios/config/SecurityConfig.java
git commit -m "feat(docs): add springdoc-openapi dependency and configure Swagger UI paths"
```

---

### Task 2: OpenAPI Configuration Bean

**Files:**
- Create: `src/main/java/com/monteastur/envios/config/OpenApiConfig.java`

**Interfaces:**
- Consumes: (none — Spring Bean)
- Produces: `OpenAPI` bean with title "Monteastur Envíos API", version "3.2", security schemes `basicAuth` and `cookieAuth`

- [ ] **Step 1: Create OpenApiConfig.java**

```java
package com.monteastur.envios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

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
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/monteastur/envios/config/OpenApiConfig.java
git commit -m "feat(docs): add OpenAPI bean with metadata and security schemes"
```

---

### Task 3: Annotate Admin REST Controllers (AdminApiController + ReservaApiController)

**Files:**
- Modify: `src/main/java/com/monteastur/envios/controller/api/AdminApiController.java`
- Modify: `src/main/java/com/monteastur/envios/controller/api/ReservaApiController.java`

**Interfaces:**
- Consumes: Springdoc auto-detection
- Produces: Documented admin endpoints with `@Tag("Admin Tracking")` and `@Tag("Admin Reservas")`

- [ ] **Step 1: Add imports to AdminApiController.java**

Add after existing imports (line 3-19):

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

- [ ] **Step 2: Add @Tag and @Operation to AdminApiController class and methods**

Add `@Tag` on the class (before line 21 `@RestController`):

```java
@Tag(name = "Admin Tracking", description = "Gestión de envíos del panel de administración (requiere Basic Auth)")
```

Add annotations on `listarEnvios` (before `@GetMapping("/envios")`):

```java
@Operation(summary = "Listar envíos", description = "Devuelve una página de envíos filtrable por estado, código, rango de fechas o búsqueda general")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista paginada de envíos",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.AdminEnvioResumenDto.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

Add annotations on `detalleEnvio` (before `@GetMapping("/envios/{codigo}")`):

```java
@Operation(summary = "Detalle de envío", description = "Obtiene el detalle completo de un envío con eventos y evidencias")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Detalle del envío",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.TrackingDto.class))),
    @ApiResponse(responseCode = "404", description = "Envío no encontrado",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

Add annotations on `actualizarEstado` (before `@PutMapping("/envios/{codigo}/estado")`):

```java
@Operation(summary = "Actualizar estado", description = "Cambia el estado de un envío y registra un evento de tracking")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Estado actualizado",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.TrackingDto.class))),
    @ApiResponse(responseCode = "404", description = "Envío no encontrado",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

- [ ] **Step 3: Add imports to ReservaApiController.java**

Add after existing imports:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

- [ ] **Step 4: Add @Tag and @Operation to ReservaApiController class and methods**

Add `@Tag`:

```java
@Tag(name = "Admin Reservas", description = "Gestión de reservas del panel de administración (requiere Basic Auth)")
```

Add annotations on `listar` (before `@GetMapping`):

```java
@Operation(summary = "Listar reservas", description = "Devuelve todas las reservas, opcionalmente filtradas por estado")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de reservas",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ReservaAdminDto.class)))
})
```

Add annotations on `detalle` (before `@GetMapping("/{id}")`):

```java
@Operation(summary = "Detalle de reserva", description = "Obtiene una reserva por su ID")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Reserva encontrada",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ReservaAdminDto.class))),
    @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

Add annotations on `actualizar` (before `@PutMapping("/{id}")`):

```java
@Operation(summary = "Actualizar reserva", description = "Actualiza los datos de una reserva existente")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Reserva actualizada",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ReservaAdminDto.class))),
    @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

Add annotations on `cambiarEstado` (before `@PatchMapping("/{id}/estado")`):

```java
@Operation(summary = "Cambiar estado de reserva", description = "Cambia el estado de una reserva (pendiente/aprobada/cancelada)")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Estado actualizado",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ReservaAdminDto.class))),
    @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

Add annotations on `eliminar` (before `@DeleteMapping("/{id}")`):

```java
@Operation(summary = "Eliminar reserva", description = "Elimina una reserva por su ID")
@ApiResponses({
    @ApiResponse(responseCode = "204", description = "Reserva eliminada (sin contenido)"),
    @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

- [ ] **Step 5: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/monteastur/envios/controller/api/AdminApiController.java src/main/java/com/monteastur/envios/controller/api/ReservaApiController.java
git commit -m "feat(docs): add OpenAPI annotations to admin controllers (AdminApiController + ReservaApiController)"
```

---

### Task 4: Annotate Client + Public + Push Controllers

**Files:**
- Modify: `src/main/java/com/monteastur/envios/controller/api/ClienteApiController.java`
- Modify: `src/main/java/com/monteastur/envios/controller/api/TrackingApiController.java`
- Modify: `src/main/java/com/monteastur/envios/controller/api/ReservaPublicApiController.java`
- Modify: `src/main/java/com/monteastur/envios/controller/api/PushSubscriptionController.java`

**Interfaces:**
- Consumes: Springdoc auto-detection
- Produces: Documented client (CookieAuth), public, and push endpoints

- [ ] **Step 1: Annotate ClienteApiController**

Add imports:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

Add `@Tag` on class:

```java
@Tag(name = "Cliente", description = "Portal del cliente autenticado por sesión (requiere cookie JSESSIONID)")
```

On `listarEnvios`:

```java
@Operation(summary = "Listar envíos del cliente", description = "Devuelve los envíos asociados al cliente autenticado")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de envíos del cliente",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ClienteEnvioResumenDto.class))),
    @ApiResponse(responseCode = "403", description = "No autenticado",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

On `detalleEnvio`:

```java
@Operation(summary = "Detalle de envío del cliente", description = "Obtiene el detalle completo de un envío con eventos y evidencias visibles")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Detalle del envío",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.TrackingDto.class))),
    @ApiResponse(responseCode = "403", description = "No autenticado o no autorizado",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class))),
    @ApiResponse(responseCode = "404", description = "Envío no encontrado",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

On `descargarEvidencia`:

```java
@Operation(summary = "Descargar evidencia", description = "Descarga un archivo de evidencia asociado a un envío del cliente")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Archivo descargado"),
    @ApiResponse(responseCode = "403", description = "No autenticado, archivo no visible o nombre no permitido",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class))),
    @ApiResponse(responseCode = "404", description = "Evidencia o archivo no encontrado",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

- [ ] **Step 2: Annotate TrackingApiController**

Add imports:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

Add `@Tag` on class:

```java
@Tag(name = "Tracking Público", description = "Consulta pública de tracking por código (no requiere autenticación)")
```

On `getTrackingByCodigo`:

```java
@Operation(summary = "Consultar tracking", description = "Obtiene el estado actual de un envío mediante su código único")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Estado del envío",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.PublicTrackingDto.class))),
    @ApiResponse(responseCode = "404", description = "Código de tracking no encontrado",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

- [ ] **Step 3: Annotate ReservaPublicApiController**

Add imports:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

Add `@Tag` on class:

```java
@Tag(name = "Reservas Público", description = "Creación y consulta de reservas desde la web pública (no requiere autenticación)")
```

On `crear`:

```java
@Operation(summary = "Crear reserva", description = "Registra una nueva reserva en el sistema")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Reserva creada",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ReservaAdminDto.class))),
    @ApiResponse(responseCode = "400", description = "Datos de reserva inválidos o fechas no disponibles",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class))),
    @ApiResponse(responseCode = "409", description = "Conflicto — las fechas ya están ocupadas",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

On `verificarDisponibilidad`:

```java
@Operation(summary = "Verificar disponibilidad", description = "Comprueba si un rango de fechas está disponible para reservar")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Resultado de disponibilidad (disponible: true/false)"),
    @ApiResponse(responseCode = "400", description = "Formato de fecha inválido",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

- [ ] **Step 4: Annotate PushSubscriptionController**

Add imports:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

Add `@Tag` on class:

```java
@Tag(name = "Push Notifications", description = "Suscripción y prueba de notificaciones push para la PWA (no requiere autenticación)")
```

On `subscribe`:

```java
@Operation(summary = "Suscribir a notificaciones push", description = "Registra un endpoint de suscripción push para recibir notificaciones")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Suscripción registrada")
})
```

On `unsubscribe`:

```java
@Operation(summary = "Desuscribir de notificaciones push", description = "Elimina un endpoint de suscripción push")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Suscripción eliminada")
})
```

On `testPush`:

```java
@Operation(summary = "Probar notificaciones push", description = "Simula el envío de una notificación a todos los dispositivos suscritos (no disponible en producción)")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Simulación ejecutada"),
    @ApiResponse(responseCode = "403", description = "Endpoint deshabilitado en producción",
        content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
})
```

- [ ] **Step 5: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/monteastur/envios/controller/api/ClienteApiController.java src/main/java/com/monteastur/envios/controller/api/TrackingApiController.java src/main/java/com/monteastur/envios/controller/api/ReservaPublicApiController.java src/main/java/com/monteastur/envios/controller/api/PushSubscriptionController.java
git commit -m "feat(docs): add OpenAPI annotations to client, public and push controllers"
```

---

### Task 5: Add @Schema Annotations to Key DTOs

**Files:**
- Modify: `src/main/java/com/monteastur/envios/dto/api/ErrorDto.java`
- Modify: `src/main/java/com/monteastur/envios/dto/api/TrackingDto.java`
- Modify: `src/main/java/com/monteastur/envios/dto/api/ReservaAdminDto.java`
- Modify: `src/main/java/com/monteastur/envios/dto/api/CrearReservaPublicRequest.java`
- Modify: `src/main/java/com/monteastur/envios/dto/api/ActualizarEstadoRequest.java`

**Interfaces:**
- Consumes: (none — pure documentation)
- Produces: Improved schema docs in Swagger UI

- [ ] **Step 1: Add @Schema to ErrorDto**

Add import: `import io.swagger.v3.oas.annotations.media.Schema;`

Annotate fields:

```java
@Schema(description = "Respuesta de error estándar de la API")
public class ErrorDto {

    @Schema(description = "Marca temporal del error (ISO 8601)", example = "2026-07-29T12:00:00Z")
    private String timestamp;

    @Schema(description = "Código de estado HTTP", example = "404")
    private int status;

    @Schema(description = "Mensaje descriptivo del error", example = "Recurso no encontrado")
    private String error;
```

- [ ] **Step 2: Add @Schema to TrackingDto**

Add import: `import io.swagger.v3.oas.annotations.media.Schema;`

Annotate key fields:

```java
@Schema(description = "Detalle completo de un envío con eventos y evidencias")
public class TrackingDto {

    @Schema(description = "Código único de seguimiento", example = "MT-2026-0001")
    private String codigoUnico;

    @Schema(description = "Estado actual del envío", example = "EN_TRANSITO",
        allowableValues = {"RECIBIDO", "EN_ADUANA_ORIGEN", "EN_TRANSITO", "EN_ADUANA_DESTINO", "EN_REPARTO", "ENTREGADO"})
    private String estado;

    @Schema(description = "Nombre del destinatario", example = "María González")
    private String destinatario;

    @Schema(description = "Ciudad o país de origen", example = "Asturias, España")
    private String origen;

    @Schema(description = "Ciudad o país de destino", example = "Asunción, Paraguay")
    private String destino;
```

- [ ] **Step 3: Add @Schema to ReservaAdminDto**

Add import: `import io.swagger.v3.oas.annotations.media.Schema;`

```java
@Schema(description = "Reserva visible para el administrador")
public class ReservaAdminDto {

    @Schema(description = "ID único de la reserva", example = "42")
    private Long id;

    @Schema(description = "Nombre del cliente que realizó la reserva", example = "Juan Pérez")
    private String nombreCliente;

    @Schema(description = "Email de contacto", example = "juan@example.com")
    private String email;

    @Schema(description = "Estado de la reserva", example = "pendiente",
        allowableValues = {"pendiente", "aprobada", "cancelada", "confirmada"})
    private String estado;
```

- [ ] **Step 4: Add @Schema to CrearReservaPublicRequest**

Add import: `import io.swagger.v3.oas.annotations.media.Schema;`

```java
@Schema(description = "Solicitud de creación de reserva desde la web pública")
public class CrearReservaPublicRequest {
```

- [ ] **Step 5: Add @Schema to ActualizarEstadoRequest**

Add import: `import io.swagger.v3.oas.annotations.media.Schema;`

```java
@Schema(description = "Solicitud de actualización de estado")
public class ActualizarEstadoRequest {

    @Schema(description = "Nuevo estado", example = "EN_TRANSITO",
        allowableValues = {"RECIBIDO", "EN_ADUANA_ORIGEN", "EN_TRANSITO", "EN_ADUANA_DESTINO", "EN_REPARTO", "ENTREGADO"})
    private String estado;
```

- [ ] **Step 6: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/monteastur/envios/dto/api/ErrorDto.java src/main/java/com/monteastur/envios/dto/api/TrackingDto.java src/main/java/com/monteastur/envios/dto/api/ReservaAdminDto.java src/main/java/com/monteastur/envios/dto/api/CrearReservaPublicRequest.java src/main/java/com/monteastur/envios/dto/api/ActualizarEstadoRequest.java
git commit -m "feat(docs): add @Schema annotations to key DTOs for OpenAPI documentation"
```

---

### Task 6: Verification — Compile + Test Suite

**Files:**
- (none — verification only)

**Interfaces:**
- Consumes: All previous tasks
- Produces: Verified working state, 47/47 tests passing

- [ ] **Step 1: Run full test suite**

Run: `mvn test`
Expected: BUILD SUCCESS — Tests run: 47, Failures: 0, Errors: 0

- [ ] **Step 2: Verify Swagger UI is served (manual check — optional)**

Start the application and visit:
- `http://localhost:8080/api/v1/swagger-ui.html` — should show Swagger UI
- `http://localhost:8080/api/v1/docs` — should return OpenAPI JSON

- [ ] **Step 3: Commit any remaining changes**

If all tests pass and verification is successful, ensure all modified files are committed. If all previous commits were made, only the final verification commit may be needed.

```bash
git status
```

If all clean, no commit needed.
