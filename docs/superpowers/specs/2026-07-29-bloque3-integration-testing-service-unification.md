# Bloque 3: Integration Testing + Service Layer Unification

## Meta

- **Date:** 2026-07-29
- **Project:** Envios_Paraguay_CMS
- **Branch:** feature/seguimiento-premium
- **Author:** Superpowers (subagent-driven development)
- **Status:** Approved for implementation

## Problem Statement

Dos áreas críticas de calidad pendientes tras Bloque 1 y Bloque 2:

1. **Cobertura de tests insuficiente:** El proyecto tiene solo 11 tests unitarios (TrackingApiController, PushSubscriptionController, SecurityConfig, ReservaService). No existe ningún test de integración para:
   - GlobalExceptionHandler (7 manejadores, 2 modos REST/MVC = 14 escenarios)
   - PublicController refactorizado (11 endpoints × 2 locales = 22 escenarios)
   - API controllers migrados (ReservaApiController, ReservaPublicApiController, AdminApiController, ClienteApiController)

2. **Capa de servicios inconsistente con la jerarquía de excepciones:** Los servicios ignoran las excepciones personalizadas del Bloque 2:
   - `ReservaService` lanza `IllegalArgumentException`/`IllegalStateException` en vez de `BadRequestException`/`ConflictException`
   - `EventoTrackingService.crearEvento()` retorna `null` crudo
   - `EvidenciaEnvioService.toggleVisibilidad()` es no-op silencioso cuando el ID no existe

## Scope

### Parte A — Cobertura de Integración

- Tests para `GlobalExceptionHandler` (REST + MVC, todos los manejadores)
- Tests parametrizados para `PublicController` (11 endpoints, ES/EN)
- Tests para API controllers migrados (que verifiquen que el GlobalExceptionHandler responde correctamente)

### Parte B — Unificación de Servicios

- Migrar `ReservaService` a excepciones personalizadas
- Eliminar `null` return en `EventoTrackingService.crearEvento()`
- Eliminar `ifPresent` silencioso en `EvidenciaEnvioService.toggleVisibilidad()`

## Design

### 1. Integration Test Infrastructure

#### 1.1 Test Controller para GlobalExceptionHandler

Se crea un controlador de prueba (solo en test scope) que expone endpoints para lanzar cada tipo de excepción:

```java
@RestController
@RequestMapping("/test/exception")
public class TestExceptionController {

    @GetMapping("/resource-not-found")
    public void resourceNotFound() {
        throw new ResourceNotFoundException("Test 404");
    }

    @GetMapping("/bad-request")
    public void badRequest() {
        throw new BadRequestException("Test 400");
    }

    @GetMapping("/conflict")
    public void conflict() {
        throw new ConflictException("Test 409");
    }

    @GetMapping("/illegal-argument")
    public void illegalArgument() {
        throw new IllegalArgumentException("Test illegal argument");
    }

    @GetMapping("/illegal-state")
    public void illegalState() {
        throw new IllegalStateException("Test illegal state");
    }

    @GetMapping("/date-time-parse")
    public void dateTimeParse() {
        throw new DateTimeParseException("Test bad date", "invalid", 0);
    }

    @GetMapping("/generic")
    public void generic() {
        throw new RuntimeException("Test 500");
    }
}
```

Este controlador se importa en el contexto de test via `@Import(TestExceptionController.class)`.

#### 1.2 REST Path Tests

Usar `@WebMvcTest(TestExceptionController.class)` para REST:
- Verificar status HTTP correcto (404, 400, 409, 400, 409, 400, 500)
- Verificar `Content-Type: application/json`
- Verificar campos `$.status`, `$.error` en el JSON

#### 1.3 MVC Path Tests

Usar MockMvc standalone setup (sin necesidad de vistas reales) para MVC:
- Verificar view name: `"error"` para ES, `"en/error"` para EN
- Verificar model attributes: `status`, `error`, `message`, `timestamp`

El test simula requests a rutas que NO empiezan por `/api/` para activar el branch MVC.

#### 1.4 PublicController Integration Tests

`@WebMvcTest(PublicController.class)` con mocks de repositorios:

| Grupo | Endpoints | Verificaciones |
|-------|-----------|----------------|
| GET estáticos | `/`, `/en`, `/entorno`, `/en/entorno`, `/operaciones`, `/en/operaciones` | Status 200, view name correcta |
| GET con datos | `/casa`, `/en/casa`, `/aviso-legal`, `/en/aviso-legal`, `/politica-cookies`, `/en/politica-cookies` | Status 200, modelo con datos |
| GET formularios | `/reservas`, `/en/reservas`, `/contacto`, `/en/contacto`, `/tracking`, `/en/tracking` | Status 200, flags en modelo |
| POST | `/reservas`, `/en/reservas`, `/contacto`, `/en/contacto`, `/tracking`, `/en/tracking` | Status 200/302, redirect o template |

Se usa `@ParameterizedTest` con `CsvSource` para probar cada endpoint con ES y EN.

#### 1.5 API Controller Cleanup Tests

Tests existentes se mantienen. Se añaden tests para:
- `ReservaApiController`: verificar que `buscarPorId(inexistente)` retorna 404 con ErrorDto (no 500 ni HTML)
- `ReservaPublicApiController`: verificar que `crear()` con datos inválidos retorna 400
- `AdminApiController`: verificar que `detalleEnvio(inexistente)` retorna 404
- `ClienteApiController`: verificar que `detalleEnvio(inexistente)` retorna 404

### 2. Service Layer Unification

#### 2.1 ReservaService

Reemplazar throws de excepciones Java estándar por las personalizadas:

| Actual | Reemplazar por |
|--------|---------------|
| `throw new IllegalArgumentException("La fecha de entrada no puede ser en el pasado")` | `throw new BadRequestException("La fecha de entrada no puede ser en el pasado")` |
| `throw new IllegalArgumentException("La fecha de salida debe ser posterior a la de entrada")` | `throw new BadRequestException("La fecha de salida debe ser posterior a la de entrada")` |
| `throw new IllegalStateException("Las fechas seleccionadas no están disponibles")` | `throw new ConflictException("Las fechas seleccionadas no están disponibles")` |
| `throw new IllegalArgumentException("Estado no válido: " + nuevoEstado)` | `throw new BadRequestException("Estado no válido: " + nuevoEstado)` |
| `throw new IllegalStateException("Transición no permitida...")` | `throw new ConflictException("Transición no permitida...")` |

El `GlobalExceptionHandler` YA tiene handlers para `BadRequestException` (→ 400) y `ConflictException` (→ 409), así que el comportamiento HTTP no cambia. Los handlers de `IllegalArgumentException`/`IllegalStateException` se mantienen en el handler como safety net para otros posibles usos.

#### 2.2 EventoTrackingService.crearEvento()

**Problema:** `crearEvento()` retorna `null` en línea 23 cuando el estado no ha cambiado.

**Solución:** Cambiar tipo de retorno de `EventoTracking` a `Optional<EventoTracking>`. Cuando el estado no ha cambiado, retornar `Optional.empty()`.

```java
public Optional<EventoTracking> crearEvento(EnvioTracking envio, String estadoAnterior) {
    if (estadoAnterior != null && estadoAnterior.equals(envio.getEstado())) {
        return Optional.empty();
    }
    // ... crear evento ...
    return Optional.of(repo.save(evento));
}
```

**Impacto:** Los callers (`AdminController.java:272`, `AdminApiController.java:115`) no usan el valor de retorno, así que no necesitan cambios.

#### 2.3 EvidenciaEnvioService.toggleVisibilidad()

**Problema:** `toggleVisibilidad()` usa `ifPresent` que silenciosamente ignora IDs inexistentes.

**Solución:** Lanzar `ResourceNotFoundException` cuando el ID no existe.

```java
public void toggleVisibilidad(Long id) {
    EvidenciaEnvio ev = repo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Evidencia no encontrada: " + id));
    Boolean actual = ev.getVisibleCliente();
    ev.setVisibleCliente(actual == null ? true : !actual);
    repo.save(ev);
}
```

**Impacto:** El caller (`AdminController.java:360`) ya tiene un `try/catch(Exception)` que capturará la excepción y mostrará el mensaje de error al admin. Comportamiento mejorado: antes era no-op silencioso, ahora muestra mensaje de error.

### 3. Archivos a modificar/crear

| Archivo | Acción |
|---------|--------|
| `test/.../controller/GlobalExceptionHandlerTest.java` | **Crear** — tests REST + MVC para 7 manejadores |
| `test/.../controller/TestExceptionController.java` | **Crear** — controller de test que lanza excepciones |
| `test/.../controller/PublicControllerTest.java` | **Crear** — tests parametrizados 11 endpoints × 2 locales |
| `test/.../controller/api/ReservaApiControllerTest.java` | **Crear** — tests para endpoint con GlobalExceptionHandler |
| `test/.../controller/api/ReservaPublicApiControllerTest.java` | **Crear** — tests para creación/disponibilidad |
| `test/.../controller/api/AdminApiControllerTest.java` | **Crear** — tests para detalle no encontrado |
| `test/.../controller/api/ClienteApiControllerTest.java` | **Crear** — tests para detalle no encontrado |
| `service/ReservaService.java` | Modificar — cambiar a BadRequestException/ConflictException |
| `service/EventoTrackingService.java` | Modificar — Optional return, no null |
| `service/EvidenciaEnvioService.java` | Modificar — throw ResourceNotFoundException en toggleVisibilidad |
| `controller/GlobalExceptionHandler.java` | Sin cambios (ya maneja BadRequestException y ConflictException) |

## No incluye (fuera de scope)

- Tests de integración para AdminController (MVC) — controller de 394 líneas no refactorizado
- Tests de carga o rendimiento
- Tests E2E con Selenium/Playwright
- Refactor de AdminController o ClienteController MVC
- Migración de repositorios a servicios donde PublicController inyecta repos directamente

## Success Criteria

1. **Tests de GlobalExceptionHandler:** 14 escenarios (7 excepciones × REST + MVC) pasan
2. **Tests de PublicController:** 22+ escenarios (11 endpoints × ES/EN) pasan
3. **Tests de API controllers:** 4+ escenarios para rutas de error pasan
4. **ReservaService:** 0 usos de `IllegalArgumentException`/`IllegalStateException` — todos reemplazados por `BadRequestException`/`ConflictException`
5. **EventoTrackingService:** `crearEvento()` nunca retorna `null`
6. **EvidenciaEnvioService:** `toggleVisibilidad()` lanza `ResourceNotFoundException` para IDs inexistentes
7. **`mvn test`:** Todos los tests pasan (11 existentes + nuevos)
8. **`mvn clean package -DskipTests`:** Compila sin errores
