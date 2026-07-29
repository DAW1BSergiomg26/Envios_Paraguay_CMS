# Bloque 2: Controller Refactor + Exception Handling

## Meta

- **Date:** 2026-07-29
- **Project:** Envios_Paraguay_CMS
- **Branch:** feature/seguimiento-premium
- **Author:** Superpowers (subagent-driven development)
- **Status:** Approved for implementation

## Problem Statement

El código actual presenta dos problemas de calidad:

1. **PublicController (321 líneas):** 22 endpoints MVC (11 ES + 11 EN) con lógica completamente duplicada. Cada cambio en una página debe replicarse manualmente en su gemelo inglés.

2. **Manejo de excepciones disperso:** `TrackingApiExceptionHandler` mapea toda excepción a 404 (bug). Controladores API tienen try/catch inline con `ErrorDto` construido manualmente. No hay manejo de errores para controladores MVC.

## Scope

### Área 2 — PublicController Refactor

- Desduplicar ES/EN vía multi-ruta + helpers de idioma
- Reducir de 321 → ~140 líneas
- Sin cambios en templates ni funcionalidad

### Área 3 — Exception Handling Centralizado

- Jerarquía de excepciones personalizadas
- `@ControllerAdvice` unificado que distingue REST vs MVC
- Limpiar try/catch inline en API controllers
- Eliminar `TrackingApiExceptionHandler`

## Design

### 1. Exception Hierarchy (`exception/` package)

Tres clases runtime, todas en `com.monteastur.envios.exception`:

```
ResourceNotFoundException (→ 404)
  Uso: entidad no encontrada por ID/código, reemplaza Optional.isPresent checks

BadRequestException (→ 400)
  Uso: argumento inválido, validación de negocio, reemplaza IllegalArgumentException

ConflictException (→ 409)
  Uso: estado ilegal, conflicto de fechas, duplicados, reemplaza IllegalStateException
```

Cada clase es simple: extiende `RuntimeException`, constructor `(String message)` y opcionalmente `(String message, Throwable cause)`.

### 2. GlobalExceptionHandler (`controller/GlobalExceptionHandler.java`)

Un único `@ControllerAdvice` que reemplaza `TrackingApiExceptionHandler`.

**Detección REST vs MVC:**

```java
private boolean isRestRequest(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri.startsWith("/api/");
}
```

Para REST (URL empieza por `/api/`): retorna `ResponseEntity<ErrorDto>` con HTTP status correcto.
Para MVC: retorna vista error template con modelo.

**Exception Handlers:**

| Exception | REST | MVC View |
|-----------|------|----------|
| `ResourceNotFoundException` | 404 + ErrorDto | error.html, status=404 |
| `BadRequestException` | 400 + ErrorDto | error.html, status=400 |
| `ConflictException` | 409 + ErrorDto | error.html, status=409 |
| `Exception` (catch-all) | 500 + ErrorDto | error.html, status=500 |

Para MVC, el modelo incluye: `status`, `error`, `message`, `timestamp`. La vista se resuelve como `error` o `en/error` usando el mismo helper `template()` del `PublicController` (determinado por el locale del request).

### 3. PublicController Refactor

**URL Mapping multi-ruta:**

| Antes (ES) | Antes (EN) | Después (único) |
|------------|------------|-----------------|
| `GET /` | `GET /en` | `{"/", "/en"}` |
| `GET /casa`, `/lacasa` | `GET /en/casa` | `{"/casa", "/lacasa", "/en/casa"}` |
| `GET /entorno` | *(no existía)* | `{"/entorno", "/en/entorno"}` |
| `GET /reservas` | `GET /en/reservas` | `{"/reservas", "/en/reservas"}` |
| `POST /reservas` | `POST /en/reservas` | `{"/reservas", "/en/reservas"}` |
| `GET /contacto` | `GET /en/contacto` | `{"/contacto", "/en/contacto"}` |
| `POST /contacto` | `POST /en/contacto` | `{"/contacto", "/en/contacto"}` |
| `GET /operaciones` | `GET /en/operaciones` | `{"/operaciones", "/en/operaciones"}` |
| `GET /aviso-legal` | `GET /en/aviso-legal` | `{"/aviso-legal", "/en/aviso-legal"}` |
| `GET /politica-cookies` | `GET /en/politica-cookies` | `{"/politica-cookies", "/en/politica-cookies"}` |
| `GET /tracking` | `GET /en/tracking` | `{"/tracking", "/en/tracking"}` |
| `POST /tracking` | `POST /en/tracking` | `{"/tracking", "/en/tracking"}` |

**Helpers privados:**

```java
private String template(String view, HttpServletRequest request) {
    return isEnglish(request) ? "en/" + view : view;
}

private boolean isEnglish(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/en/")
        || request.getRequestURI().equals("/en");
}

private String[] monthNames(HttpServletRequest request) {
    return isEnglish(request) ? MESES_EN : MESES_ES;
}
```

**Cambios específicos:**

- Métodos GET sin lógica (home, entorno, operaciones, aviso-legal, politica-cookies): solo aplican `template()`
- Métodos GET con datos (casa, reservas, contacto, tracking): aplican `template()` + resuelven datos según locale
- Métodos POST (reservas, contacto, tracking): misma lógica de negocio, template según locale
- Los DTOs `MesCalendario` y `DiaCalendario` se mantienen como nested static classes en `PublicController`

**Resultado:** 11 endpoints, ~140 líneas. Sin dependencia de `LocaleContextHolder` ni filtros — puro multi-routing.

### 4. API Controller Cleanup

**ReservaApiController:**

- `detalle(Long id)`: `return ResponseEntity.ok(toDto(reservaService.buscarPorId(id).orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"))))`
- `actualizar(Long id, ActualizarReservaRequest)`: eliminar try/catch, el handler capturará BadRequestException y ConflictException
- `cambiarEstado(Long id, ActualizarEstadoRequest)`: mismo tratamiento
- `eliminar(Long id)`: `orElseThrow` + `noContent()`

**ReservaPublicApiController:**

- `crear(@Valid CrearReservaPublicRequest)`: eliminar try/catch
- `verificarDisponibilidad(...)`: `DateTimeParseException` se deja propagar → handler lo trata como BadRequestException. Opcionalmente se puede capturar explícitamente y lanzar `BadRequestException`

**AdminApiController y ClienteApiController:** revisar si hay patrones con `Optional.isPresent()` + 404 manual o try/catch, aplicar mismo tratamiento.

### 5. Archivos a modificar

| Archivo | Acción |
|---------|--------|
| `controller/PublicController.java` | Refactor completo |
| `controller/api/TrackingApiExceptionHandler.java` | **Eliminar** |
| `controller/GlobalExceptionHandler.java` | **Crear** (nuevo) |
| `exception/ResourceNotFoundException.java` | **Crear** |
| `exception/BadRequestException.java` | **Crear** |
| `exception/ConflictException.java` | **Crear** |
| `controller/api/ReservaApiController.java` | Limpiar try/catch, usar custom exceptions |
| `controller/api/ReservaPublicApiController.java` | Limpiar try/catch |
| `controller/api/AdminApiController.java` | Revisar: tiene `ResponseEntity` con `ErrorDto` inline en 403/404 — reemplazar por `throw new ResourceNotFoundException()` + delegar al handler |
| `controller/api/ClienteApiController.java` | Revisar: tiene `Optional.orElse(null)` + null checks — reemplazar por `orElseThrow()` |
| `templates/error.html` | **Crear** |
| `templates/en/error.html` | **Crear** |
| `dto/api/ErrorDto.java` | Sin cambios — `timestamp` ISO 8601 vía `Instant.now().toString()` es correcto |

## No incluye (fuera de scope)

- Refactor de `AdminController` (394 líneas) o `ClienteController` — no tienen duplicación ES/EN
- Migración de repositorios a servicios donde `PublicController` inyecta repos directamente — se deja para otro bloque
- Tests automatizados — se añadirán en bloque posterior si aplica

## Success Criteria

1. `PublicController.java` pasa de 22 endpoints a 11, de ~321 líneas a ~140
2. Todas las rutas ES y EN existentes responden igual que antes (misma URL, mismo contenido)
3. `GlobalExceptionHandler` reemplaza a `TrackingApiExceptionHandler` sin pérdida de funcionalidad
4. API controllers existentes funcionan sin try/catch inline
5. `mvn clean package -DskipTests` pasa sin errores
6. `docker compose build app` pasa sin errores
7. Páginas de error personalizadas renderizan correctamente para MVC y devuelven JSON para REST
