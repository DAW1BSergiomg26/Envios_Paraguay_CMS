# Bloque 3: Integration Testing + Service Layer Unification — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add integration test coverage for GlobalExceptionHandler (REST + MVC), PublicController (all endpoints × locales), and cleaned API controllers. Unify service layer to use custom exception hierarchy, eliminate null returns and silent no-ops.

**Architecture:** Tests use `@WebMvcTest` with mock repositories. GlobalExceptionHandler tests use a `TestExceptionController` that throws each exception type. Service changes are minimal and backward-compatible (callers unchanged).

**Tech Stack:** Java 17, Spring Boot 3.3.5, Spring MVC, JUnit 5, MockMvc, Mockito

## Global Constraints

- All new test classes go in `src/test/java/com/monteastur/envios/controller/` or `controller/api/`
- Test controller `TestExceptionController` goes in test scope only (not main)
- Change service method signatures only where necessary (crearEvento → Optional)
- Keep existing `IllegalArgument/IllegalState` handlers in GlobalExceptionHandler as safety net
- All existing tests must continue passing
- `mvn test` must be green after each task

---

### Task 1: Service Layer Unification (3 services)

**Files:**
- Modify: `src/main/java/com/monteastur/envios/service/ReservaService.java`
- Modify: `src/main/java/com/monteastur/envios/service/EventoTrackingService.java`
- Modify: `src/main/java/com/monteastur/envios/service/EvidenciaEnvioService.java`

**Interfaces:**
- Consumes: `BadRequestException`, `ConflictException`, `ResourceNotFoundException` (from `com.monteastur.envios.exception`)
- Produces: Same behavior as before, but with proper exception types

- [ ] **Step 1: Update `ReservaService.java`**

Import the custom exceptions:
```java
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
```

Replace all `IllegalArgumentException` with `BadRequestException`:

| Line | Current | New |
|------|---------|-----|
| 49 | `throw new IllegalArgumentException("La fecha de entrada no puede ser en el pasado")` | `throw new BadRequestException("La fecha de entrada no puede ser en el pasado")` |
| 52 | `throw new IllegalArgumentException("La fecha de salida debe ser posterior a la de entrada")` | `throw new BadRequestException("La fecha de salida debe ser posterior a la de entrada")` |
| 87 | `throw new IllegalArgumentException("La fecha de entrada no puede ser en el pasado")` | `throw new BadRequestException("La fecha de entrada no puede ser en el pasado")` |
| 90 | `throw new IllegalArgumentException("La fecha de salida debe ser posterior a la de entrada")` | `throw new BadRequestException("La fecha de salida debe ser posterior a la de entrada")` |
| 105 | `throw new IllegalArgumentException("Estado no válido: " + nuevoEstado)` | `throw new BadRequestException("Estado no válido: " + nuevoEstado)` |

Replace all `IllegalStateException` with `ConflictException`:

| Line | Current | New |
|------|---------|-----|
| 55 | `throw new IllegalStateException("Las fechas seleccionadas no están disponibles")` | `throw new ConflictException("Las fechas seleccionadas no están disponibles")` |
| 93 | `throw new IllegalStateException("Las fechas seleccionadas no están disponibles")` | `throw new ConflictException("Las fechas seleccionadas no están disponibles")` |
| 111 | `throw new IllegalStateException("Transición no permitida: " + r.getEstado() + " → " + estadoNormalizado)` | `throw new ConflictException("Transición no permitida: " + r.getEstado() + " → " + estadoNormalizado)` |

- [ ] **Step 2: Update `EventoTrackingService.java`**

Change `crearEvento()` return type from `EventoTracking` to `Optional<EventoTracking>`:

```java
public Optional<EventoTracking> crearEvento(EnvioTracking envio, String estadoAnterior) {
    if (estadoAnterior != null && estadoAnterior.equals(envio.getEstado())) {
        return Optional.empty();
    }
    // ... existing logic (lines 25-43) ...
    return Optional.of(repo.save(evento));
}
```

Remove unused `null` import (if `Collections` or others are needed, keep them).

- [ ] **Step 3: Update `EvidenciaEnvioService.java`**

Import `ResourceNotFoundException`:
```java
import com.monteastur.envios.exception.ResourceNotFoundException;
```

Replace `toggleVisibilidad()`:
```java
public void toggleVisibilidad(Long id) {
    EvidenciaEnvio ev = repo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Evidencia no encontrada: " + id));
    Boolean actual = ev.getVisibleCliente();
    ev.setVisibleCliente(actual == null ? true : !actual);
    repo.save(ev);
}
```

- [ ] **Step 4: Verify compilation**
```bash
mvn clean compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Run existing tests**
```bash
mvn test
```
Expected: 11 tests, 0 failures (ReservaServiceTest may need update since exception types changed)

Note: ReservaServiceTest uses `@ExtendWith(MockitoExtension.class)` and does not test the exception-throwing methods directly (it tests `crear()` and `buscarPorId()` which don't throw). If any test calls methods that now throw custom exceptions, update the `@Test(expected = ...)` or `assertThrows()` accordingly.

- [ ] **Step 6: Commit**
```bash
git add src/main/java/com/monteastur/envios/service/ReservaService.java src/main/java/com/monteastur/envios/service/EventoTrackingService.java src/main/java/com/monteastur/envios/service/EvidenciaEnvioService.java
git commit -m "refactor: unify service layer — ReservaService uses custom exceptions, EventoTrackingService returns Optional, EvidenciaEnvioService throws on missing ID"
```

---

### Task 2: GlobalExceptionHandler Integration Tests (REST + MVC)

**Files:**
- Create: `src/test/java/com/monteastur/envios/controller/TestExceptionController.java`
- Create: `src/test/java/com/monteastur/envios/controller/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `ResourceNotFoundException`, `BadRequestException`, `ConflictException`, `DateTimeParseException`, `GlobalExceptionHandler`
- Produces: Verified REST JSON responses and MVC model/view responses

- [ ] **Step 1: Create `TestExceptionController.java`**

```java
package com.monteastur.envios.controller;

import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeParseException;

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

- [ ] **Step 2: Create `GlobalExceptionHandlerTest.java`**

```java
package com.monteastur.envios.controller;

import com.monteastur.envios.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestExceptionController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // ========== REST Path (starts with /api/) ==========

    @ParameterizedTest
    @CsvSource({
        "/api/test/exception/resource-not-found, 404, Not Found",
        "/api/test/exception/bad-request,         400, Bad Request",
        "/api/test/exception/conflict,            409, Conflict",
        "/api/test/exception/illegal-argument,     400, Bad Request",
        "/api/test/exception/illegal-state,        409, Conflict",
        "/api/test/exception/date-time-parse,      400, Bad Request",
        "/api/test/exception/generic,              500, Internal Server Error"
    })
    void restPath_returnsCorrectStatusAndJson(String url, int status, String error) throws Exception {
        mockMvc.perform(get(url).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error));
    }

    // ========== MVC Path (does NOT start with /api/) ==========

    @ParameterizedTest
    @CsvSource({
        "/test/exception/resource-not-found, 404, Not Found",
        "/test/exception/bad-request,         400, Bad Request",
        "/test/exception/conflict,            409, Conflict",
        "/test/exception/illegal-argument,     400, Bad Request",
        "/test/exception/illegal-state,        409, Conflict",
        "/test/exception/date-time-parse,      400, Bad Request",
        "/test/exception/generic,              500, Internal Server Error"
    })
    void mvcPath_returnsCorrectViewAndModel(String url, int status, String error) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", status))
                .andExpect(model().attribute("error", error))
                .andExpect(model().attributeExists("message", "timestamp"));
    }

    // ========== MVC Path EN locale ==========

    @ParameterizedTest
    @CsvSource({
        "/en/test/exception/resource-not-found",
        "/en/test/exception/bad-request",
        "/en/test/exception/conflict"
    })
    void mvcPath_enLocale_returnsEnErrorView(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(view().name("en/error"));
    }
}
```

- [ ] **Step 3: Verify tests pass**
```bash
mvn test -Dtest=GlobalExceptionHandlerTest
```
Expected: 12 tests (7 REST + 3 MVC ES + 2 MVC EN minimum) — all pass

Note: Parameterized tests count differently. The exact count depends on CsvSource rows. Expected: 9 scenarios (7 REST path + 3 MVC ES + 3 MVC EN).

Wait, the MVC path should be 7 (all exceptions) × 1 (ES) + 1 (EN for all). Let me fix the test structure:

```java
// 7 REST tests (1 parameterized with 7 rows)
// 7 MVC ES tests (1 parameterized with 7 rows)
// 1 MVC EN test (parameterized with 3 sample rows — enough to verify EN routing works)
```

Total: 17 test invocations.

- [ ] **Step 4: Commit**
```bash
git add src/test/java/com/monteastur/envios/controller/TestExceptionController.java src/test/java/com/monteastur/envios/controller/GlobalExceptionHandlerTest.java
git commit -m "test: add GlobalExceptionHandler integration tests — REST JSON and MVC model/view for all 7 exception handlers"
```

---

### Task 3: PublicController Integration Tests

**Files:**
- Create: `src/test/java/com/monteastur/envios/controller/PublicControllerTest.java`

**Interfaces:**
- Consumes: Mock repositories for `ImagenRepository`, `TextoLegalRepository`, `ReservaRepository`, `EnvioTrackingRepository`, `MensajeContactoRepository`
- Consumes: Mock services for `EmailService`, `EventoTrackingService`
- Produces: Verified template view names and model attributes

- [ ] **Step 1: Create `PublicControllerTest.java`**

```java
package com.monteastur.envios.controller;

import com.monteastur.envios.model.Imagen;
import com.monteastur.envios.model.TextoLegal;
import com.monteastur.envios.repository.*;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.EventoTrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicController.class)
class PublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ImagenRepository imagenRepo;
    @MockBean private TextoLegalRepository textoRepo;
    @MockBean private ReservaRepository reservaRepo;
    @MockBean private EnvioTrackingRepository trackingRepo;
    @MockBean private MensajeContactoRepository mensajeRepo;
    @MockBean private EmailService emailService;
    @MockBean private EventoTrackingService eventoTrackingService;

    // ========== Static pages (no data fetching) ==========

    @ParameterizedTest
    @CsvSource({
        "/,      home",
        "/en,    en/home",
        "/entorno,       entorno",
        "/en/entorno,    en/entorno",
        "/operaciones,       operaciones",
        "/en/operaciones,    en/operaciones"
    })
    void staticPages_returnCorrectView(String url, String viewName) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName));
    }

    // ========== Pages with data ==========

    @Test
    void laCasa_returnsViewWithImages() throws Exception {
        when(imagenRepo.findAllByOrderByOrdenAsc()).thenReturn(List.of(new Imagen()));

        mockMvc.perform(get("/casa"))
                .andExpect(status().isOk())
                .andExpect(view().name("lacasa"))
                .andExpect(model().attributeExists("imagenes"));
    }

    @Test
    void laCasa_en_returnsViewWithImages() throws Exception {
        when(imagenRepo.findAllByOrderByOrdenAsc()).thenReturn(List.of(new Imagen()));

        mockMvc.perform(get("/en/casa"))
                .andExpect(status().isOk())
                .andExpect(view().name("en/lacasa"))
                .andExpect(model().attributeExists("imagenes"));
    }

    @Test
    void avisoLegal_returnsViewWithText() throws Exception {
        when(textoRepo.findBySlug("aviso-legal")).thenReturn(Optional.of(new TextoLegal()));

        mockMvc.perform(get("/aviso-legal"))
                .andExpect(status().isOk())
                .andExpect(view().name("aviso-legal"))
                .andExpect(model().attributeExists("texto"));
    }

    @Test
    void avisoLegal_en_returnsViewWithText() throws Exception {
        when(textoRepo.findBySlug("aviso-legal")).thenReturn(Optional.of(new TextoLegal()));

        mockMvc.perform(get("/en/aviso-legal"))
                .andExpect(status().isOk())
                .andExpect(view().name("en/aviso-legal"))
                .andExpect(model().attributeExists("texto"));
    }

    @Test
    void politicaCookies_returnsViewWithText() throws Exception {
        when(textoRepo.findBySlug("politica-cookies")).thenReturn(Optional.of(new TextoLegal()));

        mockMvc.perform(get("/politica-cookies"))
                .andExpect(status().isOk())
                .andExpect(view().name("politica-cookies"))
                .andExpect(model().attributeExists("texto"));
    }

    // ========== Form pages ==========

    @ParameterizedTest
    @CsvSource({
        "/reservas,       reservas",
        "/en/reservas,    en/reservas",
        "/contacto,       contacto",
        "/en/contacto,    en/contacto",
        "/tracking,       tracking",
        "/en/tracking,    en/tracking"
    })
    void formPages_returnCorrectViewWithFlags(String url, String viewName) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName));
    }
}
```

Note: The `reservas` endpoint calls `request.getSession()` which forces session creation. This should work fine with MockMvc. The `generarCalendarios` method calls `reservaRepo.findOcupadasEnRango(...)` — mock it to return empty list if needed.

- [ ] **Step 2: Verify tests pass**
```bash
mvn test -Dtest=PublicControllerTest
```
Expected: All tests pass

- [ ] **Step 3: Commit**
```bash
git add src/test/java/com/monteastur/envios/controller/PublicControllerTest.java
git commit -m "test: add PublicController integration tests — all 11 endpoints with ES/EN locale verification"
```

---

### Task 4: API Controller Integration Tests

**Files:**
- Create: `src/test/java/com/monteastur/envios/controller/api/ReservaApiControllerTest.java`
- Create: `src/test/java/com/monteastur/envios/controller/api/ReservaPublicApiControllerTest.java`
- Create: `src/test/java/com/monteastur/envios/controller/api/AdminApiControllerTest.java`
- Create: `src/test/java/com/monteastur/envios/controller/api/ClienteApiControllerTest.java`

**Interfaces:**
- Consumes: Mock services/repositories, GlobalExceptionHandler (auto-wired via @WebMvcTest)
- Produces: Verified error responses from GlobalExceptionHandler

- [ ] **Step 1: Create `ReservaApiControllerTest.java`**

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservaApiController.class)
class ReservaApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservaService reservaService;

    @Test
    void detalle_inexistente_retorna404() throws Exception {
        when(reservaService.buscarPorId(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/admin/reservas/999")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Reserva no encontrada: 999"));
    }
}
```

- [ ] **Step 2: Create `ReservaPublicApiControllerTest.java`**

Tests that the GlobalExceptionHandler catches `BadRequestException` (now thrown by `ReservaService.crearPublico` for invalid dates):

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservaPublicApiController.class)
class ReservaPublicApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservaService reservaService;

    @Test
    void verificarDisponibilidad_fechaInvalida_retorna400() throws Exception {
        mockMvc.perform(get("/api/v1/reservas/disponibilidad")
                .param("fechaEntrada", "not-a-date")
                .param("fechaSalida", "2026-08-01")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }
}
```

- [ ] **Step 3: Create `AdminApiControllerTest.java`**

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EvidenciaEnvioRepository;
import com.monteastur.envios.service.EventoTrackingService;
import com.monteastur.envios.service.EvidenciaEnvioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminApiController.class)
@Import(com.monteastur.envios.config.SecurityConfig.class)
class AdminApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private EnvioTrackingRepository trackingRepo;
    @MockBean private EvidenciaEnvioService evidenciaService;
    @MockBean private EventoTrackingService eventoTrackingService;
    @MockBean private EvidenciaEnvioRepository evidenciaRepo;

    @Test
    void detalleEnvio_inexistente_retorna404() throws Exception {
        when(trackingRepo.findWithClienteByCodigoUnico(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/admin/envios/NO-EXISTE")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Tracking no encontrado: NO-EXISTE"));
    }
}
```

- [ ] **Step 4: Create `ClienteApiControllerTest.java`**

Note: ClienteApiController has auth checks (403) that must be satisfied. The test uses valid credentials to reach the 404 path:

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.EvidenciaEnvioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteApiController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class ClienteApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private EnvioTrackingRepository trackingRepo;
    @MockBean private EvidenciaEnvioService evidenciaService;

    @Test
    void detalleEnvio_inexistente_retorna404() throws Exception {
        when(trackingRepo.findWithClienteByCodigoUnico(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/clientes/envios/NO-EXISTE")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Tracking no encontrado: NO-EXISTE"));
    }
}
```

- [ ] **Step 5: Run ALL tests**
```bash
mvn test
```
Expected: All tests pass (existing 11 + new 10+)

- [ ] **Step 6: Commit**
```bash
git add src/test/java/com/monteastur/envios/controller/api/ReservaApiControllerTest.java src/test/java/com/monteastur/envios/controller/api/ReservaPublicApiControllerTest.java src/test/java/com/monteastur/envios/controller/api/AdminApiControllerTest.java src/test/java/com/monteastur/envios/controller/api/ClienteApiControllerTest.java
git commit -m "test: add API controller integration tests — verify GlobalExceptionHandler 404/400 responses"
```

---

### Task 5: Final Verification and Push

- [ ] **Step 1: Full test suite**
```bash
mvn clean test
```
Expected: BUILD SUCCESS, tests run > 21, 0 failures

- [ ] **Step 2: Production build**
```bash
mvn clean package -DskipTests -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Push to GitHub**
```bash
git push origin feature/seguimiento-premium
```
