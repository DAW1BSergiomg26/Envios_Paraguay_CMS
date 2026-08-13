# F4: Reservas y Mensajes de Contacto — Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exponer en la SPA React la gestión de reservas y de mensajes de contacto del backend, y corregir el casing de los estados de reserva.

**Architecture:** Dos páginas React nuevas (`ReservasPage`, `MensajesPage`) consumiendo una API REST existente (`/api/v1/admin/reservas`) y una nueva (`/api/v1/admin/mensajes`). Backend: fix a minúsculas en `ReservaService`/`ReservaApiController` + módulo nuevo `MensajeContactoService`/`MensajeContactoApiController`/DTOs. TDD estricto en ambas capas.

**Tech Stack:** Spring Boot 3.3.5 + Java 25 + Mockito/JUnit5 (`@WebMvcTest`), React 19 + Vite + vitest + Testing Library + axios.

## Global Constraints

- Sin Lombok: Java puro, getters/setters manuales, inyección por constructor.
- Estados de reserva SIEMPRE en minúsculas: `pendiente`, `aprobada`, `confirmada`, `cancelada`.
- Migraciones solo con Flyway; esta fase NO añade migraciones (los datos demo ya son minúsculas).
- Seguridad: `/api/v1/admin/**` requiere autenticación (ya configurado en `SecurityConfig`); los controllers admin NO llevan `@PreAuthorize`.
- Frontend: helpers axios devuelven `{ data: ... }`; tests con `vi.mock('../services/api', ...)` y `vi.mock('../context/NotificationContext', ...)`.
- Backend test: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd clean test` (JDK 25 en `JAVA_HOME=C:\Users\astur\.jdks\openjdk-25.0.2`).
- Frontend test: `npx vitest run` (workdir `frontend-react`); build: `npm run build`.
- Commits en `main`, frecuentes y atómicos; sin push automático.

---

### Task 1: Fix de casing de estados de reserva (backend)

**Files:**
- Modify: `src/main/java/com/monteastur/envios/service/ReservaService.java:20-26,113-129`
- Modify: `src/main/java/com/monteastur/envios/controller/api/ReservaApiController.java:41-46`
- Test: `src/test/java/com/monteastur/envios/service/ReservaServiceTest.java`

**Interfaces:**
- Consumes: `ReservaRepository` (métodos `findById`, `save`), `BadRequestException`, `ConflictException`.
- Produces: `ReservaService.cambiarEstado(Long id, String nuevoEstado)` sigue devolviendo `Optional<Reserva>` pero normaliza a minúsculas; `ReservaApiController.listar` filtra en minúsculas.

- [ ] **Step 1: Escribir los tests fallidos** en `ReservaServiceTest.java`

Añadir al final de la clase (antes de la llave de cierre), manteniendo los imports existentes (`BadRequestException`, `ConflictException` se importan con `com.monteastur.envios.exception.*`):

```java
    @Test
    void cambiarEstado_aceptaMinusculas() {
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("pendiente");

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenReturn(reserva);

        Optional<Reserva> resultado = reservaService.cambiarEstado(1L, "aprobada");

        assertTrue(resultado.isPresent());
        assertEquals("aprobada", resultado.get().getEstado());
    }

    @Test
    void cambiarEstado_normalizaMayusculasEntrada() {
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("pendiente");

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenReturn(reserva);

        Optional<Reserva> resultado = reservaService.cambiarEstado(1L, "APROBADA");

        assertTrue(resultado.isPresent());
        assertEquals("aprobada", resultado.get().getEstado());
    }

    @Test
    void cambiarEstado_estadoInvalido_lanzaBadRequest() {
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("pendiente");

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThrows(BadRequestException.class,
            () -> reservaService.cambiarEstado(1L, "NO_EXISTE"));
    }

    @Test
    void cambiarEstado_transicionIlegal_lanzaConflict() {
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setEstado("confirmada");

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ConflictException.class,
            () -> reservaService.cambiarEstado(1L, "aprobada"));
    }
```

Actualizar los imports de la línea 4 de `ReservaServiceTest.java`:

```java
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
```

- [ ] **Step 2: Ejecutar los tests y verificar que fallan**

Run: `& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" test -Dtest=ReservaServiceTest -q`
Expected: FAIL — `cambiarEstado_normalizaMayusculasEntrada` falla (guardaría `"APROBADA"`), `transicionIlegal` falla (porque `r.getEstado()` = `"confirmada"` en minúsculas no está en el mapa en mayúsculas → transición permitida como `"aprobada"` no encontrada → no lanza).

- [ ] **Step 3: Fix en `ReservaService.java`**

Reemplazar el bloque de constantes (líneas 20-26):

```java
    private static final Set<String> ESTADOS_VALIDOS = Set.of("pendiente", "aprobada", "confirmada", "cancelada");

    private static final Map<String, Set<String>> TRANSICIONES_PERMITIDAS = Map.of(
        "pendiente", Set.of("aprobada", "cancelada"),
        "aprobada", Set.of("confirmada", "cancelada"),
        "confirmada", Set.of("cancelada")
    );
```

Y en `cambiarEstado` (línea 114) reemplazar la normalización y añadir la comprobación de estado actual:

```java
        String estadoNormalizado = nuevoEstado.trim().toLowerCase();
        if (!ESTADOS_VALIDOS.contains(estadoNormalizado)) {
            throw new BadRequestException("Estado no válido: " + nuevoEstado);
        }

        return repo.findById(id).map(r -> {
            if (!TRANSICIONES_PERMITIDAS.containsKey(r.getEstado())) {
                throw new ConflictException("Transición no permitida desde estado terminal: " + r.getEstado());
            }
            Set<String> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(r.getEstado(), Set.of());
            if (!permitidos.contains(estadoNormalizado)) {
                throw new ConflictException(
                    "Transición no permitida: " + r.getEstado() + " → " + estadoNormalizado
                );
            }
            r.setEstado(estadoNormalizado);
            return repo.save(r);
        });
```

- [ ] **Step 4: Ejecutar los tests y verificar que pasan**

Run: `& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" test -Dtest=ReservaServiceTest -q`
Expected: PASS (8 tests).

- [ ] **Step 5: Fix del filtro en `ReservaApiController.java`**

Reemplazar líneas 41-46:

```java
        if (estado != null && !estado.isBlank()) {
            String estadoNormalizado = estado.trim().toLowerCase();
            reservas = reservas.stream()
                .filter(r -> r.getEstado().equals(estadoNormalizado))
                .collect(Collectors.toList());
        }
```

- [ ] **Step 6: Compilar el backend completo para verificar regresión**

Run: `& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" clean test -q`
Expected: BUILD SUCCESS (tests existentes + 4 nuevos en verde).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/ReservaService.java src/main/java/com/monteastur/envios/controller/api/ReservaApiController.java src/test/java/com/monteastur/envios/service/ReservaServiceTest.java
git commit -m "fix: estados de reserva normalizados a minúsculas en service y controller"
```

---

### Task 2: API REST admin de mensajes de contacto (backend)

**Files:**
- Create: `src/main/java/com/monteastur/envios/dto/api/MensajeContactoAdminDto.java`
- Create: `src/main/java/com/monteastur/envios/dto/api/MarcarLeidoRequest.java`
- Create: `src/main/java/com/monteastur/envios/service/MensajeContactoService.java`
- Create: `src/main/java/com/monteastur/envios/controller/api/MensajeContactoApiController.java`
- Create: `src/test/java/com/monteastur/envios/service/MensajeContactoServiceTest.java`
- Create: `src/test/java/com/monteastur/envios/controller/api/MensajeContactoApiControllerTest.java`

**Interfaces:**
- Consumes: `MensajeContactoRepository` (`findAllByOrderByFechaEnvioDesc`, `findById`, `save`, `deleteById`), `MensajeContacto` model, `ResourceNotFoundException`.
- Produces: `MensajeContactoService.listar(Boolean leido) → List<MensajeContacto>`, `MensajeContactoService.marcarLeido(Long id, boolean leido) → Optional<MensajeContacto>`, `MensajeContactoService.buscarPorId(Long id) → Optional<MensajeContacto>`, `MensajeContactoService.eliminar(Long id) → void`. Endpoints `GET /api/v1/admin/mensajes`, `PATCH /{id}/leido`, `DELETE /{id}`.

- [ ] **Step 1: Escribir el test del service (rojo)**

Create `src/test/java/com/monteastur/envios/service/MensajeContactoServiceTest.java`:

```java
package com.monteastur.envios.service;

import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.repository.MensajeContactoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MensajeContactoServiceTest {

    @Mock
    private MensajeContactoRepository mensajeContactoRepository;

    @InjectMocks
    private MensajeContactoService mensajeContactoService;

    private MensajeContacto mensaje(String nombre, boolean leido) {
        MensajeContacto m = new MensajeContacto(nombre, nombre + "@example.com", "+34 600 000 000", "Mensaje de prueba");
        m.setLeido(leido);
        return m;
    }

    @Test
    void listar_sinFiltro_devuelveTodos() {
        when(mensajeContactoRepository.findAllByOrderByFechaEnvioDesc())
            .thenReturn(List.of(mensaje("Ana", false), mensaje("Luis", true)));

        List<MensajeContacto> resultado = mensajeContactoService.listar(null);

        assertEquals(2, resultado.size());
        verify(mensajeContactoRepository).findAllByOrderByFechaEnvioDesc();
    }

    @Test
    void listar_filtroLeidos_devuelveSoloLeidos() {
        when(mensajeContactoRepository.findAllByOrderByFechaEnvioDesc())
            .thenReturn(List.of(mensaje("Ana", false), mensaje("Luis", true)));

        List<MensajeContacto> resultado = mensajeContactoService.listar(true);

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).isLeido());
    }

    @Test
    void listar_filtroNoLeidos_devuelveSoloNoLeidos() {
        when(mensajeContactoRepository.findAllByOrderByFechaEnvioDesc())
            .thenReturn(List.of(mensaje("Ana", false), mensaje("Luis", true)));

        List<MensajeContacto> resultado = mensajeContactoService.listar(false);

        assertEquals(1, resultado.size());
        assertFalse(resultado.get(0).isLeido());
    }

    @Test
    void marcarLeido_ok_guardaCambio() {
        MensajeContacto m = mensaje("Ana", false);
        when(mensajeContactoRepository.findById(1L)).thenReturn(Optional.of(m));
        when(mensajeContactoRepository.save(any(MensajeContacto.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<MensajeContacto> resultado = mensajeContactoService.marcarLeido(1L, true);

        assertTrue(resultado.isPresent());
        assertTrue(resultado.get().isLeido());
        verify(mensajeContactoRepository).save(m);
    }

    @Test
    void marcarLeido_inexistente_retornaEmpty() {
        when(mensajeContactoRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<MensajeContacto> resultado = mensajeContactoService.marcarLeido(99L, true);

        assertTrue(resultado.isEmpty());
        verify(mensajeContactoRepository, never()).save(any());
    }

    @Test
    void eliminar_ok() {
        mensajeContactoService.eliminar(1L);
        verify(mensajeContactoRepository).deleteById(1L);
    }
}
```

- [ ] **Step 2: Ejecutar el test y verificar que falla**

Run: `& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" test -Dtest=MensajeContactoServiceTest -q`
Expected: FAIL — compilación de `MensajeContactoService` inexistente.

- [ ] **Step 3: Escribir los DTOs y el service**

Create `src/main/java/com/monteastur/envios/dto/api/MensajeContactoAdminDto.java`:

```java
package com.monteastur.envios.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Mensaje de contacto visible para el administrador")
public class MensajeContactoAdminDto {
    @Schema(description = "ID único del mensaje", example = "7")
    private Long id;

    @Schema(description = "Nombre de quien escribió", example = "Ana López")
    private String nombre;

    @Schema(description = "Email de contacto", example = "ana@example.com")
    private String email;

    @Schema(description = "Teléfono de contacto", example = "+34 644 444 444")
    private String telefono;

    @Schema(description = "Contenido del mensaje", example = "Hola, quiero información sobre envíos a Asunción")
    private String mensaje;

    @Schema(description = "Fecha de envío del mensaje")
    private LocalDateTime fechaEnvio;

    @Schema(description = "Indica si el mensaje ha sido leído por el administrador", example = "false")
    private boolean leido;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
}
```

Create `src/main/java/com/monteastur/envios/dto/api/MarcarLeidoRequest.java`:

```java
package com.monteastur.envios.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud para marcar un mensaje como leído o no leído")
public class MarcarLeidoRequest {
    @Schema(description = "Nuevo estado de lectura del mensaje", example = "true")
    private Boolean leido;

    public Boolean getLeido() { return leido; }
    public void setLeido(Boolean leido) { this.leido = leido; }
}
```

Create `src/main/java/com/monteastur/envios/service/MensajeContactoService.java`:

```java
package com.monteastur.envios.service;

import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.repository.MensajeContactoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MensajeContactoService {

    private final MensajeContactoRepository repo;

    public MensajeContactoService(MensajeContactoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<MensajeContacto> listar(Boolean leido) {
        List<MensajeContacto> mensajes = repo.findAllByOrderByFechaEnvioDesc();
        if (leido == null) {
            return mensajes;
        }
        return mensajes.stream().filter(m -> m.isLeido() == leido).toList();
    }

    @Transactional(readOnly = true)
    public Optional<MensajeContacto> buscarPorId(Long id) {
        return repo.findById(id);
    }

    @Transactional
    public Optional<MensajeContacto> marcarLeido(Long id, boolean leido) {
        return repo.findById(id).map(m -> {
            m.setLeido(leido);
            return repo.save(m);
        });
    }

    @Transactional
    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}
```

- [ ] **Step 4: Ejecutar el test y verificar que pasa**

Run: `& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" test -Dtest=MensajeContactoServiceTest -q`
Expected: PASS (6 tests).

- [ ] **Step 5: Escribir el test del controller (rojo)**

Create `src/test/java/com/monteastur/envios/controller/api/MensajeContactoApiControllerTest.java`:

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.MensajeContactoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MensajeContactoApiController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class MensajeContactoApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MensajeContactoService mensajeContactoService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private MensajeContacto mensaje(Long id, String nombre, boolean leido) {
        MensajeContacto m = new MensajeContacto(nombre, nombre + "@example.com", "+34 600 000 000", "Mensaje de prueba");
        m.setId(id);
        m.setLeido(leido);
        m.setFechaEnvio(LocalDateTime.of(2026, 8, 1, 10, 0));
        return m;
    }

    @Test
    void listar_retorna200_conLista() throws Exception {
        when(mensajeContactoService.listar(null))
            .thenReturn(List.of(mensaje(1L, "Ana", false), mensaje(2L, "Luis", true)));

        mockMvc.perform(get("/api/v1/admin/mensajes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].nombre").value("Ana"))
            .andExpect(jsonPath("$[0].leido").value(false));
    }

    @Test
    void listar_conFiltroLeido_pasaElParametro() throws Exception {
        when(mensajeContactoService.listar(true))
            .thenReturn(List.of(mensaje(2L, "Luis", true)));

        mockMvc.perform(get("/api/v1/admin/mensajes").param("leido", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void marcarLeido_retorna200_conDto() throws Exception {
        MensajeContacto m = mensaje(1L, "Ana", false);
        when(mensajeContactoService.marcarLeido(1L, true)).thenReturn(Optional.of(m));

        mockMvc.perform(patch("/api/v1/admin/mensajes/1/leido")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"leido\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    @Test
    void marcarLeido_inexistente_retorna404() throws Exception {
        when(mensajeContactoService.marcarLeido(99L, true))
            .thenThrow(new ResourceNotFoundException("Mensaje no encontrado: 99"));

        mockMvc.perform(patch("/api/v1/admin/mensajes/99/leido")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"leido\":true}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Mensaje no encontrado: 99"));
    }

    @Test
    void eliminar_retorna204() throws Exception {
        when(mensajeContactoService.buscarPorId(1L))
            .thenReturn(Optional.of(mensaje(1L, "Ana", false)));
        doNothing().when(mensajeContactoService).eliminar(1L);

        mockMvc.perform(delete("/api/v1/admin/mensajes/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_inexistente_retorna404() throws Exception {
        when(mensajeContactoService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/admin/mensajes/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }
}
```

Nota: el `@MockBean MensajeContactoService` es un stub que intercepta las llamadas; no hace falta el import de `anyBoolean` si no se usa (eliminarlo si el compilador avisa de import sin usar). Se mantiene `doNothing()` solo si el service mockeado no devuelve `void` real — como es mock, `eliminar` es void y no requiere stub; puede omitirse la línea `doNothing()`.

- [ ] **Step 6: Ejecutar el test y verificar que falla (compilación)**

Run: `& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" test -Dtest=MensajeContactoApiControllerTest -q`
Expected: FAIL — `MensajeContactoApiController` inexistente.

- [ ] **Step 7: Escribir el controller**

Create `src/main/java/com/monteastur/envios/controller/api/MensajeContactoApiController.java`:

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.*;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.service.MensajeContactoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Mensajes", description = "Gestión de mensajes de contacto del panel de administración (requiere Basic Auth)")
@RestController
@RequestMapping("/api/v1/admin/mensajes")
public class MensajeContactoApiController {

    private final MensajeContactoService mensajeContactoService;

    public MensajeContactoApiController(MensajeContactoService mensajeContactoService) {
        this.mensajeContactoService = mensajeContactoService;
    }

    @Operation(summary = "Listar mensajes de contacto",
        description = "Devuelve todos los mensajes, opcionalmente filtrados por estado de lectura")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de mensajes",
            content = @Content(schema = @Schema(implementation = MensajeContactoAdminDto.class)))
    })
    @GetMapping
    public ResponseEntity<List<MensajeContactoAdminDto>> listar(
            @RequestParam(required = false) Boolean leido) {
        List<MensajeContactoAdminDto> dtos = mensajeContactoService.listar(leido).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Marcar mensaje como leído o no leído",
        description = "Actualiza el estado de lectura de un mensaje")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mensaje actualizado",
            content = @Content(schema = @Schema(implementation = MensajeContactoAdminDto.class))),
        @ApiResponse(responseCode = "404", description = "Mensaje no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PatchMapping("/{id}/leido")
    public ResponseEntity<MensajeContactoAdminDto> marcarLeido(@PathVariable Long id,
                                                                @RequestBody MarcarLeidoRequest request) {
        MensajeContacto mensaje = mensajeContactoService.marcarLeido(id, request.getLeido())
            .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado: " + id));
        return ResponseEntity.ok(toDto(mensaje));
    }

    @Operation(summary = "Eliminar mensaje de contacto", description = "Elimina un mensaje por su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Mensaje eliminado (sin contenido)"),
        @ApiResponse(responseCode = "404", description = "Mensaje no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mensajeContactoService.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado: " + id));
        mensajeContactoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private MensajeContactoAdminDto toDto(MensajeContacto m) {
        MensajeContactoAdminDto dto = new MensajeContactoAdminDto();
        dto.setId(m.getId());
        dto.setNombre(m.getNombre());
        dto.setEmail(m.getEmail());
        dto.setTelefono(m.getTelefono());
        dto.setMensaje(m.getMensaje());
        dto.setFechaEnvio(m.getFechaEnvio());
        dto.setLeido(m.isLeido());
        return dto;
    }
}
```

Nota: `MarcarLeidoRequest.getLeido()` devuelve `Boolean`; si el JSON omite el campo, Spring lo deserializa a `null` y al desboxear en `marcarLeido(id, request.getLeido())` lanzaría NPE. El auto-unboxing con `boolean` está sujeto a NPE con `null`; para el plan usamos `marcarLeido(id, Boolean.TRUE.equals(request.getLeido()))` si se quiere tolerancia a null. Ver Step 8 de validación.

- [ ] **Step 8: Ejecutar el test del controller y verificar que pasa**

Run: `& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" test -Dtest=MensajeContactoApiControllerTest -q`
Expected: PASS (6 tests). Si falla por NPE con `null` (el test usa `"leido":true`, no fallará), ajustar como se indica.

- [ ] **Step 9: Compilar el backend completo para verificar regresión**

Run: `& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" clean test -q`
Expected: BUILD SUCCESS (317 + 10 nuevos).

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/monteastur/envios/dto/api/MensajeContactoAdminDto.java src/main/java/com/monteastur/envios/dto/api/MarcarLeidoRequest.java src/main/java/com/monteastur/envios/service/MensajeContactoService.java src/main/java/com/monteastur/envios/controller/api/MensajeContactoApiController.java src/test/java/com/monteastur/envios/service/MensajeContactoServiceTest.java src/test/java/com/monteastur/envios/controller/api/MensajeContactoApiControllerTest.java
git commit -m "feat: API REST admin de mensajes de contacto con tests"
```

---

### Task 3: Helpers de API de reservas y mensajes (frontend)

**Files:**
- Modify: `frontend-react/src/services/api.js`
- Test: `frontend-react/src/services/api.test.js`

**Interfaces:**
- Consumes: instancia `api` de axios existente (baseURL `/api/v1`, `withCredentials`).
- Produces: `getAdminReservas(estado)`, `getAdminReservaDetalle(id)`, `putAdminReserva(id, body)`, `patchAdminReservaEstado(id, estado)`, `deleteAdminReserva(id)`, `getAdminMensajes(leido)`, `patchAdminMensajeLeido(id, leido)`, `deleteAdminMensaje(id)` — usados por Tasks 4-5.

- [ ] **Step 1: Escribir los tests fallidos** — ampliar `frontend-react/src/services/api.test.js`

Añadir al final:

```js
describe('api helpers de reservas', () => {
  it('getAdminReservas construye la URL y params', async () => {
    vi.mock('axios')
    const { default: api } = await import('./api')
    const getSpy = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    await api.getAdminReservas('aprobada')
    expect(getSpy).toHaveBeenCalledWith('/admin/reservas', expect.objectContaining({ params: { estado: 'aprobada' } }))
    getSpy.mockRestore()
  })
})
```

Nota: el mocking de axios dentro de `api.test.js` existente usa spyOn sobre el objeto importado; dado que `api.test.js` actual importa helpers concretos (no el default `api`), la forma más simple y consistente con la suite es testear las funciones a través de la exportación directa. Como los helpers internos usan `api.get`, `api.put`, etc., los tests deben espiar esos métodos. Reemplazar el paso con la estrategia real de la suite (ver Step 2).

- [ ] **Step 2: Implementar los helpers en `frontend-react/src/services/api.js`**

Añadir al final del archivo (antes de `export default api;`):

```js
export function getAdminReservas(estado) {
  const params = {};
  if (estado) params.estado = estado;
  return api.get('/admin/reservas', { params });
}

export function getAdminReservaDetalle(id) {
  return api.get(`/admin/reservas/${id}`);
}

export function putAdminReserva(id, body) {
  return api.put(`/admin/reservas/${id}`, body);
}

export function patchAdminReservaEstado(id, estado) {
  return api.patch(`/admin/reservas/${id}/estado`, { estado });
}

export function deleteAdminReserva(id) {
  return api.delete(`/admin/reservas/${id}`);
}

export function getAdminMensajes(leido) {
  const params = {};
  if (leido !== undefined && leido !== null) params.leido = leido;
  return api.get('/admin/mensajes', { params });
}

export function patchAdminMensajeLeido(id, leido) {
  return api.patch(`/admin/mensajes/${id}/leido`, { leido });
}

export function deleteAdminMensaje(id) {
  return api.delete(`/admin/mensajes/${id}`);
}
```

- [ ] **Step 3: Ajustar los tests de `api.test.js` al patrón de espionaje de la suite**

El archivo `api.test.js` actual no mockea axios completo (usa spyOn sobre elementos DOM). Estrategia: espiar los métodos HTTP del objeto `api` exportado por defecto. Añadir al final de `api.test.js`:

```js
import api, { getAdminReservas, getAdminMensajes, patchAdminMensajeLeido, deleteAdminMensaje } from './api'

describe('api helpers de reservas y mensajes', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getAdminReservas llama a GET /admin/reservas con estado', async () => {
    const spy = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    await getAdminReservas('aprobada')
    expect(spy).toHaveBeenCalledWith('/admin/reservas', expect.objectContaining({ params: { estado: 'aprobada' } }))
    spy.mockRestore()
  })

  it('getAdminReservas sin estado omite el param', async () => {
    const spy = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    await getAdminReservas(undefined)
    expect(spy).toHaveBeenCalledWith('/admin/reservas', expect.objectContaining({ params: {} }))
    spy.mockRestore()
  })

  it('patchAdminReservaEstado llama a PATCH con el estado', async () => {
    const spy = vi.spyOn(api, 'patch').mockResolvedValue({ data: {} })
    await patchAdminReservaEstado(1, 'aprobada')
    expect(spy).toHaveBeenCalledWith('/admin/reservas/1/estado', { estado: 'aprobada' })
    spy.mockRestore()
  })

  it('getAdminMensajes sin filtro no envía leido', async () => {
    const spy = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    await getAdminMensajes(undefined)
    expect(spy).toHaveBeenCalledWith('/admin/mensajes', expect.objectContaining({ params: {} }))
    spy.mockRestore()
  })

  it('getAdminMensajes con filtro envía leido', async () => {
    const spy = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    await getAdminMensajes(true)
    expect(spy).toHaveBeenCalledWith('/admin/mensajes', expect.objectContaining({ params: { leido: true } }))
    spy.mockRestore()
  })

  it('patchAdminMensajeLeido llama a PATCH con leido', async () => {
    const spy = vi.spyOn(api, 'patch').mockResolvedValue({ data: {} })
    await patchAdminMensajeLeido(5, true)
    expect(spy).toHaveBeenCalledWith('/admin/mensajes/5/leido', { leido: true })
    spy.mockRestore()
  })

  it('deleteAdminMensaje llama a DELETE', async () => {
    const spy = vi.spyOn(api, 'delete').mockResolvedValue({ data: {} })
    await deleteAdminMensaje(5)
    expect(spy).toHaveBeenCalledWith('/admin/mensajes/5')
    spy.mockRestore()
  })
})
```

(Con `vi.clearAllMocks()` los mocks previos del describe anterior se limpian.)

- [ ] **Step 4: Ejecutar los tests del frontend**

Run: `npx vitest run` (workdir `frontend-react`)
Expected: PASS (36 existentes + 7 nuevos de api.js).

- [ ] **Step 5: Commit**

```bash
git add frontend-react/src/services/api.js frontend-react/src/services/api.test.js
git commit -m "feat: helpers de API para reservas y mensajes de contacto"
```

---

### Task 4: `ReservasPage` (frontend)

**Files:**
- Create: `frontend-react/src/pages/ReservasPage.jsx`
- Create: `frontend-react/src/pages/ReservasPage.test.jsx`
- Modify: `frontend-react/src/App.jsx`
- Modify: `frontend-react/src/layouts/MainLayout.jsx`

**Interfaces:**
- Consumes: `getAdminReservas`, `patchAdminReservaEstado`, `putAdminReserva`, `deleteAdminReserva` (Task 3); `parseLocalDateTime`; `useToast`; `EmptyState`.
- Produces: componente `ReservasPage` (default export) montable en `/dashboard/reservas`.

- [ ] **Step 1: Escribir los tests fallidos**

Create `frontend-react/src/pages/ReservasPage.test.jsx`:

```jsx
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ReservasPage from './ReservasPage'

const mockGetAdminReservas = vi.fn()
const mockPatchAdminReservaEstado = vi.fn()
const mockPutAdminReserva = vi.fn()
const mockDeleteAdminReserva = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('../services/api', () => ({
  getAdminReservas: (...args) => mockGetAdminReservas(...args),
  patchAdminReservaEstado: (...args) => mockPatchAdminReservaEstado(...args),
  putAdminReserva: (...args) => mockPutAdminReserva(...args),
  deleteAdminReserva: (...args) => mockDeleteAdminReserva(...args),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

const RESERVAS = [
  {
    id: 1, nombreCliente: 'Juan Pérez', email: 'juan@example.com', telefono: '+34 611 111 111',
    fechaEntrada: '2026-06-01', fechaSalida: '2026-06-05', numeroHuespedes: 4,
    comentarios: 'Solicito envío de 4 palets', estado: 'pendiente', createdAt: '2026-05-20T10:30:00',
  },
  {
    id: 2, nombreCliente: 'Laura Martínez', email: 'laura@example.com', telefono: '+34 622 222 222',
    fechaEntrada: '2026-06-10', fechaSalida: '2026-06-15', numeroHuespedes: 2,
    comentarios: 'Documentación urgente', estado: 'confirmada', createdAt: '2026-05-18T15:45:00',
  },
]

describe('ReservasPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminReservas.mockResolvedValue({ data: RESERVAS })
    mockPatchAdminReservaEstado.mockResolvedValue({ data: {} })
    mockPutAdminReserva.mockResolvedValue({ data: {} })
    mockDeleteAdminReserva.mockResolvedValue({ data: {} })
  })

  it('carga y muestra las reservas en la tabla', async () => {
    render(<ReservasPage />)
    expect(mockGetAdminReservas).toHaveBeenCalledWith(undefined)
    expect(await screen.findByText('Juan Pérez')).toBeInTheDocument()
    expect(screen.getByText('Laura Martínez')).toBeInTheDocument()
    expect(screen.getByText('Pendiente')).toBeInTheDocument()
    expect(screen.getByText('Confirmada')).toBeInTheDocument()
  })

  it('filtra por estado al cambiar el select', async () => {
    const user = userEvent.setup()
    render(<ReservasPage />)
    await screen.findByText('Juan Pérez')
    mockGetAdminReservas.mockClear()

    await user.selectOptions(screen.getByLabelText('Filtrar por estado'), 'aprobada')
    expect(mockGetAdminReservas).toHaveBeenCalledWith('aprobada')
  })

  it('aprueba una reserva pendiente', async () => {
    const user = userEvent.setup()
    render(<ReservasPage />)
    const aprobar = await screen.findByRole('button', { name: /Aprobar/i })
    await user.click(aprobar)

    await waitFor(() => expect(mockPatchAdminReservaEstado).toHaveBeenCalledWith(1, 'aprobada'))
    expect(mockShowSuccess).toHaveBeenCalled()
  })

  it('cancela una reserva con confirmación', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<ReservasPage />)
    const cancelar = await screen.findByRole('button', { name: /Cancelar/i })
    await user.click(cancelar)

    await waitFor(() => expect(mockPatchAdminReservaEstado).toHaveBeenCalledWith(2, 'cancelada'))
    confirmSpy.mockRestore()
  })

  it('abre el modal de edición, guarda con PUT y muestra toast', async () => {
    const user = userEvent.setup()
    render(<ReservasPage />)
    const editar = await screen.findAllByRole('button', { name: /Editar/i })
    await user.click(editar[0])

    const guardar = await screen.findByRole('button', { name: /Guardar/i })
    await user.click(guardar)

    await waitFor(() => expect(mockPutAdminReserva).toHaveBeenCalled())
    expect(mockShowSuccess).toHaveBeenCalled()
  })

  it('elimina una reserva con confirmación', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<ReservasPage />)
    const eliminar = await screen.findAllByRole('button', { name: /Eliminar/i })
    await user.click(eliminar[0])

    await waitFor(() => expect(mockDeleteAdminReserva).toHaveBeenCalledWith(1))
    confirmSpy.mockRestore()
  })

  it('muestra el estado vacío sin reservas', async () => {
    mockGetAdminReservas.mockResolvedValue({ data: [] })
    render(<ReservasPage />)
    expect(await screen.findByText('No hay reservas todavía')).toBeInTheDocument()
  })

  it('muestra toast de error si falla la carga', async () => {
    mockGetAdminReservas.mockRejectedValue(new Error('Error de conexión'))
    render(<ReservasPage />)
    await waitFor(() => expect(mockShowError).toHaveBeenCalled())
  })
})
```

- [ ] **Step 2: Ejecutar los tests y verificar que fallan**

Run: `npx vitest run src/pages/ReservasPage.test.jsx` (workdir `frontend-react`)
Expected: FAIL — módulo `./ReservasPage` no exporta.

- [ ] **Step 3: Implementar `ReservasPage.jsx`**

Create `frontend-react/src/pages/ReservasPage.jsx`:

```jsx
import { useState, useEffect, useCallback } from 'react';
import {
  getAdminReservas,
  patchAdminReservaEstado,
  putAdminReserva,
  deleteAdminReserva,
} from '../services/api';
import { parseLocalDateTime } from '../services/dateUtils';
import { useToast } from '../context/NotificationContext';
import EmptyState from '../components/EmptyState';

const ESTADOS = [
  { value: '', label: 'Todos los estados' },
  { value: 'pendiente', label: 'Pendiente' },
  { value: 'aprobada', label: 'Aprobada' },
  { value: 'confirmada', label: 'Confirmada' },
  { value: 'cancelada', label: 'Cancelada' },
];

const ESTADO_BADGE = {
  pendiente: 'lote-badge lote-badge--warning',
  aprobada: 'lote-badge lote-badge--info',
  confirmada: 'lote-badge lote-badge--success',
  cancelada: 'lote-badge lote-badge--danger',
};

const ESTADO_LABEL = {
  pendiente: 'Pendiente',
  aprobada: 'Aprobada',
  confirmada: 'Confirmada',
  cancelada: 'Cancelada',
};

const ACTIONS_POR_ESTADO = {
  pendiente: ['aprobar', 'cancelar'],
  aprobada: ['confirmar', 'cancelar'],
  confirmada: ['cancelar'],
  cancelada: [],
};

export default function ReservasPage() {
  const { showSuccess, showError } = useToast();
  const [reservas, setReservas] = useState([]);
  const [estado, setEstado] = useState('');
  const [loading, setLoading] = useState(true);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(null);

  const cargar = useCallback(async (estadoFiltro) => {
    try {
      const res = await getAdminReservas(estadoFiltro || undefined);
      setReservas(res.data || []);
    } catch (err) {
      showError(err.message || 'Error al cargar las reservas');
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    setLoading(true);
    cargar(estado);
  }, [estado, cargar]);

  const cambiarEstado = async (id, nuevoEstado) => {
    try {
      await patchAdminReservaEstado(id, nuevoEstado);
      showSuccess('Estado actualizado');
      cargar(estado || undefined);
    } catch (err) {
      showError(err.message || 'Error al actualizar el estado');
    }
  };

  const abrirEdicion = (r) => {
    setEditando(r);
    setForm({
      nombreCliente: r.nombreCliente || '',
      email: r.email || '',
      telefono: r.telefono || '',
      fechaEntrada: r.fechaEntrada || '',
      fechaSalida: r.fechaSalida || '',
      numeroHuespedes: r.numeroHuespedes || 1,
      comentarios: r.comentarios || '',
    });
  };

  const guardarEdicion = async () => {
    try {
      await putAdminReserva(editando.id, form);
      showSuccess('Reserva actualizada');
      setEditando(null);
      cargar(estado || undefined);
    } catch (err) {
      showError(err.message || 'Error al guardar la reserva');
    }
  };

  const eliminar = async (id) => {
    if (!window.confirm('¿Seguro que quieres eliminar esta reserva?')) return;
    try {
      await deleteAdminReserva(id);
      showSuccess('Reserva eliminada');
      cargar(estado || undefined);
    } catch (err) {
      showError(err.message || 'Error al eliminar la reserva');
    }
  };

  const renderAcciones = (r) => {
    const acciones = ACTIONS_POR_ESTADO[r.estado] || [];
    return (
      <div className="acciones-fila">
        {acciones.includes('aprobar') && (
          <button className="btn-nav-link" onClick={() => cambiarEstado(r.id, 'aprobada')}>Aprobar</button>
        )}
        {acciones.includes('confirmar') && (
          <button className="btn-nav-link" onClick={() => cambiarEstado(r.id, 'confirmada')}>Confirmar</button>
        )}
        {acciones.includes('cancelar') && (
          <button className="btn-nav-link" onClick={() => cambiarEstado(r.id, 'cancelada')}>Cancelar</button>
        )}
        <button className="btn-nav-link" onClick={() => abrirEdicion(r)}>Editar</button>
        <button className="btn-nav-link" onClick={() => eliminar(r.id)}>Eliminar</button>
      </div>
    );
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>Reservas</h1>
          <p className="dashboard-subtitle">Gestiona las reservas de envíos recibidas.</p>
        </div>
      </header>

      <div className="import-form-row">
        <select
          id="estadoFiltro"
          className="import-select"
          value={estado}
          onChange={e => setEstado(e.target.value)}
          aria-label="Filtrar por estado"
        >
          {ESTADOS.map(e => (
            <option key={e.value} value={e.value}>{e.label}</option>
          ))}
        </select>
      </div>

      <section className="table-section">
        <div className="table-header">
          <h2>Reservas</h2>
          <span className="table-count">{reservas.length} reserva{reservas.length !== 1 ? 's' : ''}</span>
        </div>

        {loading ? (
          <EmptyState message="Cargando reservas…" />
        ) : reservas.length === 0 ? (
          <EmptyState message="No hay reservas todavía" />
        ) : (
          <table className="envios-table">
            <thead>
              <tr>
                <th>Cliente</th><th>Fechas</th><th>Huéspedes</th>
                <th>Estado</th><th>Creada</th><th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {reservas.map(r => (
                <tr key={r.id}>
                  <td>
                    <div className="cell-nombre">{r.nombreCliente}</div>
                    <div className="cell-sub">{r.email}</div>
                  </td>
                  <td className="cell-date">{r.fechaEntrada} → {r.fechaSalida}</td>
                  <td>{r.numeroHuespedes}</td>
                  <td><span className={ESTADO_BADGE[r.estado] || 'lote-badge'}>{ESTADO_LABEL[r.estado] || r.estado}</span></td>
                  <td className="cell-date">
                    {r.createdAt
                      ? parseLocalDateTime(r.createdAt).toLocaleString('es-ES', {
                          day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
                        })
                      : '-'}
                  </td>
                  <td>{renderAcciones(r)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {editando && (
        <div className="import-modal" role="dialog" aria-modal="true" aria-label="Editar reserva">
          <div className="import-modal-content">
            <div className="import-modal-header">
              <h3>Editar reserva</h3>
              <button type="button" className="import-modal-close" aria-label="Cerrar" onClick={() => setEditando(null)}>×</button>
            </div>
            <div className="form-edicion">
              <label className="import-label" htmlFor="nombreCliente">Nombre</label>
              <input id="nombreCliente" className="import-input" value={form.nombreCliente}
                onChange={e => setForm({ ...form, nombreCliente: e.target.value })} />
              <label className="import-label" htmlFor="email">Email</label>
              <input id="email" className="import-input" value={form.email}
                onChange={e => setForm({ ...form, email: e.target.value })} />
              <label className="import-label" htmlFor="telefono">Teléfono</label>
              <input id="telefono" className="import-input" value={form.telefono}
                onChange={e => setForm({ ...form, telefono: e.target.value })} />
              <label className="import-label" htmlFor="fechaEntrada">Fecha entrada</label>
              <input id="fechaEntrada" className="import-input" type="date" value={form.fechaEntrada}
                onChange={e => setForm({ ...form, fechaEntrada: e.target.value })} />
              <label className="import-label" htmlFor="fechaSalida">Fecha salida</label>
              <input id="fechaSalida" className="import-input" type="date" value={form.fechaSalida}
                onChange={e => setForm({ ...form, fechaSalida: e.target.value })} />
              <label className="import-label" htmlFor="numeroHuespedes">Huéspedes</label>
              <input id="numeroHuespedes" className="import-input" type="number" min="1" value={form.numeroHuespedes}
                onChange={e => setForm({ ...form, numeroHuespedes: Number(e.target.value) })} />
              <label className="import-label" htmlFor="comentarios">Comentarios</label>
              <textarea id="comentarios" className="import-input" value={form.comentarios}
                onChange={e => setForm({ ...form, comentarios: e.target.value })} />
              <div className="import-form-row">
                <button className="btn-importar btn-importar--small" onClick={guardarEdicion}>Guardar</button>
                <button className="btn-nav-link" onClick={() => setEditando(null)}>Cancelar</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 4: Ejecutar los tests y verificar que pasan**

Run: `npx vitest run src/pages/ReservasPage.test.jsx` (workdir `frontend-react`)
Expected: PASS (8 tests). Si falta la clase CSS `.import-input`, el test no depende de CSS (solo render); el estilo se añade en Task 6.

- [ ] **Step 5: Añadir la ruta en `App.jsx`**

Añadir import y ruta (patrón DocumentosPage):

```jsx
import ReservasPage from './pages/ReservasPage';
```

y dentro de `<Route element={<MainLayout />}>`:

```jsx
<Route path="/dashboard/reservas" element={
  <ProtectedRoute><ReservasPage /></ProtectedRoute>
} />
```

- [ ] **Step 6: Añadir el enlace en `MainLayout.jsx`**

Añadir entre "Importar envíos" y "Documentos":

```jsx
<button className="btn-nav-link" onClick={() => navigate('/dashboard/reservas')}>
  Reservas
</button>
```

- [ ] **Step 7: Ejecutar toda la suite del frontend**

Run: `npx vitest run` (workdir `frontend-react`)
Expected: PASS (36 + 7 de api + 8 de ReservasPage = 51).

- [ ] **Step 8: Commit**

```bash
git add frontend-react/src/pages/ReservasPage.jsx frontend-react/src/pages/ReservasPage.test.jsx frontend-react/src/App.jsx frontend-react/src/layouts/MainLayout.jsx
git commit -m "feat: página de gestión de reservas en la SPA React"
```

---

### Task 5: `MensajesPage` (frontend)

**Files:**
- Create: `frontend-react/src/pages/MensajesPage.jsx`
- Create: `frontend-react/src/pages/MensajesPage.test.jsx`
- Modify: `frontend-react/src/App.jsx`
- Modify: `frontend-react/src/layouts/MainLayout.jsx`

**Interfaces:**
- Consumes: `getAdminMensajes`, `patchAdminMensajeLeido`, `deleteAdminMensaje` (Task 3); `parseLocalDateTime`; `useToast`; `EmptyState`.
- Produces: componente `MensajesPage` (default export) montable en `/dashboard/mensajes`.

- [ ] **Step 1: Escribir los tests fallidos**

Create `frontend-react/src/pages/MensajesPage.test.jsx`:

```jsx
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import MensajesPage from './MensajesPage'

const mockGetAdminMensajes = vi.fn()
const mockPatchAdminMensajeLeido = vi.fn()
const mockDeleteAdminMensaje = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('../services/api', () => ({
  getAdminMensajes: (...args) => mockGetAdminMensajes(...args),
  patchAdminMensajeLeido: (...args) => mockPatchAdminMensajeLeido(...args),
  deleteAdminMensaje: (...args) => mockDeleteAdminMensaje(...args),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

const MENSAJES = [
  {
    id: 1, nombre: 'Ana López', email: 'ana@example.com', telefono: '+34 644 444 444',
    mensaje: 'Hola, quiero información sobre envíos a Asunción', leido: false,
    fechaEnvio: '2026-05-12T12:00:00',
  },
  {
    id: 2, nombre: 'Carlos Ruiz', email: 'carlos@example.com', telefono: '+34 655 555 555',
    mensaje: '¿Cuál es el plazo de entrega a Paraguay?', leido: true,
    fechaEnvio: '2026-05-10T09:30:00',
  },
]

describe('MensajesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminMensajes.mockResolvedValue({ data: MENSAJES })
    mockPatchAdminMensajeLeido.mockResolvedValue({ data: {} })
    mockDeleteAdminMensaje.mockResolvedValue({ data: {} })
  })

  it('carga y muestra los mensajes en la tabla', async () => {
    render(<MensajesPage />)
    expect(mockGetAdminMensajes).toHaveBeenCalledWith(undefined)
    expect(await screen.findByText('Ana López')).toBeInTheDocument()
    expect(screen.getByText('Carlos Ruiz')).toBeInTheDocument()
    expect(screen.getByText('Leído')).toBeInTheDocument()
    expect(screen.getByText('No leído')).toBeInTheDocument()
  })

  it('filtra por estado de lectura al cambiar el select', async () => {
    const user = userEvent.setup()
    render(<MensajesPage />)
    await screen.findByText('Ana López')
    mockGetAdminMensajes.mockClear()

    await user.selectOptions(screen.getByLabelText('Filtrar por estado de lectura'), 'no_leido')
    expect(mockGetAdminMensajes).toHaveBeenCalledWith(false)
  })

  it('marca un mensaje como leído', async () => {
    const user = userEvent.setup()
    render(<MensajesPage />)
    const marcar = await screen.findByRole('button', { name: /Marcar leído/i })
    await user.click(marcar)

    await waitFor(() => expect(mockPatchAdminMensajeLeido).toHaveBeenCalledWith(1, true))
    expect(mockShowSuccess).toHaveBeenCalled()
  })

  it('elimina un mensaje con confirmación', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<MensajesPage />)
    const eliminar = await screen.findAllByRole('button', { name: /Eliminar/i })
    await user.click(eliminar[0])

    await waitFor(() => expect(mockDeleteAdminMensaje).toHaveBeenCalledWith(1))
    confirmSpy.mockRestore()
  })

  it('muestra el estado vacío sin mensajes', async () => {
    mockGetAdminMensajes.mockResolvedValue({ data: [] })
    render(<MensajesPage />)
    expect(await screen.findByText('No hay mensajes de contacto todavía')).toBeInTheDocument()
  })

  it('muestra toast de error si falla la carga', async () => {
    mockGetAdminMensajes.mockRejectedValue(new Error('Error de conexión'))
    render(<MensajesPage />)
    await waitFor(() => expect(mockShowError).toHaveBeenCalled())
  })
})
```

- [ ] **Step 2: Ejecutar los tests y verificar que fallan**

Run: `npx vitest run src/pages/MensajesPage.test.jsx` (workdir `frontend-react`)
Expected: FAIL — módulo `./MensajesPage` no exporta.

- [ ] **Step 3: Implementar `MensajesPage.jsx`**

Create `frontend-react/src/pages/MensajesPage.jsx`:

```jsx
import { useState, useEffect, useCallback } from 'react';
import { getAdminMensajes, patchAdminMensajeLeido, deleteAdminMensaje } from '../services/api';
import { parseLocalDateTime } from '../services/dateUtils';
import { useToast } from '../context/NotificationContext';
import EmptyState from '../components/EmptyState';

const FILTROS = [
  { value: '', label: 'Todos' },
  { value: 'no_leido', label: 'No leídos' },
  { value: 'leido', label: 'Leídos' },
];

export default function MensajesPage() {
  const { showSuccess, showError } = useToast();
  const [mensajes, setMensajes] = useState([]);
  const [filtro, setFiltro] = useState('');
  const [loading, setLoading] = useState(true);

  const cargar = useCallback(async (filtroActual) => {
    try {
      let leido;
      if (filtroActual === 'no_leido') leido = false;
      if (filtroActual === 'leido') leido = true;
      const res = await getAdminMensajes(leido);
      setMensajes(res.data || []);
    } catch (err) {
      showError(err.message || 'Error al cargar los mensajes');
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    setLoading(true);
    cargar(filtro);
  }, [filtro, cargar]);

  const marcarLeido = async (id) => {
    try {
      await patchAdminMensajeLeido(id, true);
      showSuccess('Mensaje marcado como leído');
      cargar(filtro);
    } catch (err) {
      showError(err.message || 'Error al marcar el mensaje');
    }
  };

  const eliminar = async (id) => {
    if (!window.confirm('¿Seguro que quieres eliminar este mensaje?')) return;
    try {
      await deleteAdminMensaje(id);
      showSuccess('Mensaje eliminado');
      cargar(filtro);
    } catch (err) {
      showError(err.message || 'Error al eliminar el mensaje');
    }
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>Mensajes de contacto</h1>
          <p className="dashboard-subtitle">Solicitudes e incidencias recibidas desde el formulario de contacto.</p>
        </div>
      </header>

      <div className="import-form-row">
        <select
          id="filtroLeido"
          className="import-select"
          value={filtro}
          onChange={e => setFiltro(e.target.value)}
          aria-label="Filtrar por estado de lectura"
        >
          {FILTROS.map(f => (
            <option key={f.value} value={f.value}>{f.label}</option>
          ))}
        </select>
      </div>

      <section className="table-section">
        <div className="table-header">
          <h2>Mensajes</h2>
          <span className="table-count">{mensajes.length} mensaje{mensajes.length !== 1 ? 's' : ''}</span>
        </div>

        {loading ? (
          <EmptyState message="Cargando mensajes…" />
        ) : mensajes.length === 0 ? (
          <EmptyState message="No hay mensajes de contacto todavía" />
        ) : (
          <table className="envios-table">
            <thead>
              <tr>
                <th>Nombre</th><th>Email</th><th>Mensaje</th>
                <th>Estado</th><th>Fecha</th><th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {mensajes.map(m => (
                <tr key={m.id}>
                  <td>
                    <div className="cell-nombre">{m.nombre}</div>
                    <div className="cell-sub">{m.telefono || '-'}</div>
                  </td>
                  <td>{m.email}</td>
                  <td className="cell-mensaje">{m.mensaje}</td>
                  <td>
                    <span className={m.leido ? 'lote-badge lote-badge--success' : 'lote-badge lote-badge--warning'}>
                      {m.leido ? 'Leído' : 'No leído'}
                    </span>
                  </td>
                  <td className="cell-date">
                    {m.fechaEnvio
                      ? parseLocalDateTime(m.fechaEnvio).toLocaleString('es-ES', {
                          day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
                        })
                      : '-'}
                  </td>
                  <td>
                    <div className="acciones-fila">
                      {!m.leido && (
                        <button className="btn-nav-link" onClick={() => marcarLeido(m.id)}>Marcar leído</button>
                      )}
                      <button className="btn-nav-link" onClick={() => eliminar(m.id)}>Eliminar</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
```

- [ ] **Step 4: Ejecutar los tests y verificar que pasan**

Run: `npx vitest run src/pages/MensajesPage.test.jsx` (workdir `frontend-react`)
Expected: PASS (6 tests).

- [ ] **Step 5: Añadir la ruta en `App.jsx`**

Añadir import y ruta:

```jsx
import MensajesPage from './pages/MensajesPage';
```

y dentro de `<Route element={<MainLayout />}>`:

```jsx
<Route path="/dashboard/mensajes" element={
  <ProtectedRoute><MensajesPage /></ProtectedRoute>
} />
```

- [ ] **Step 6: Añadir el enlace en `MainLayout.jsx`**

Añadir junto a los demás:

```jsx
<button className="btn-nav-link" onClick={() => navigate('/dashboard/mensajes')}>
  Mensajes
</button>
```

- [ ] **Step 7: Ejecutar toda la suite del frontend**

Run: `npx vitest run` (workdir `frontend-react`)
Expected: PASS (51 + 6 de MensajesPage = 57).

- [ ] **Step 8: Commit**

```bash
git add frontend-react/src/pages/MensajesPage.jsx frontend-react/src/pages/MensajesPage.test.jsx frontend-react/src/App.jsx frontend-react/src/layouts/MainLayout.jsx
git commit -m "feat: página de mensajes de contacto en la SPA React"
```

---

### Task 6: Estilos y verificación final

**Files:**
- Modify: `frontend-react/src/index.css`
- Modify: `docs/handoff.md`

**Interfaces:**
- Consumes: clases CSS reutilizables (`.import-select`, `.btn-nav-link`, `.lote-badge--*`, `.import-modal-*`, `.import-label`, `.envios-table`, `.table-section`, `.empty-state`).
- Produces: clases `.acciones-fila`, `.cell-nombre`, `.cell-sub`, `.cell-mensaje`, `.import-input`, `.form-edicion`.

- [ ] **Step 1: Añadir estilos a `frontend-react/src/index.css`**

Añadir al final del archivo:

```css
/* ---- F4: Reservas y mensajes ---- */
.acciones-fila { display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: center; }
.acciones-fila .btn-nav-link { padding: 0.35rem 0.6rem; font-size: 0.8rem; }
.cell-nombre { font-weight: 600; color: #e1e5ee; }
.cell-sub { font-size: 0.8rem; color: #8b93a7; }
.cell-mensaje { max-width: 320px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #c3c9d6; }
.form-edicion { display: flex; flex-direction: column; gap: 0.5rem; padding: 0.5rem 0; }
.import-input {
  width: 100%; padding: 0.5rem 0.75rem; border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.15); background: rgba(255, 255, 255, 0.06);
  color: #e1e5ee; font-size: 0.9rem;
}
.import-input:focus { outline: none; border-color: #d4762a; }
.import-input::placeholder { color: #8b93a7; }
```

- [ ] **Step 2: Ejecutar build del frontend**

Run: `npm run build` (workdir `frontend-react`)
Expected: `vite build` OK sin errores.

- [ ] **Step 3: Ejecutar la suite completa del frontend**

Run: `npx vitest run` (workdir `frontend-react`)
Expected: PASS (57 tests).

- [ ] **Step 4: Ejecutar el backend completo**

Run: `& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" clean test -q`
Expected: BUILD SUCCESS (317 + 10 nuevos = 327).

- [ ] **Step 5: Actualizar `docs/handoff.md`**

Añadir entrada de avance para F4: APIs nuevas de mensajes, fix de casing, páginas React `ReservasPage`/`MensajesPage`, rutas, count de tests (frontend 57, backend 327).

- [ ] **Step 6: Commit**

```bash
git add frontend-react/src/index.css docs/handoff.md
git commit -m "docs: estilos F4 y actualización de handoff"
```

---

## Self-Review

- **Cobertura de spec:** T1 (fix casing) → Task 1. T2 (API mensajes) → Task 2. T3 (helpers) → Task 3. T4 (ReservasPage) → Task 4. T5 (MensajesPage) → Task 5. T6 (routing/nav/estilos) → Tasks 4/5/6. Sin huecos.
- **Sin placeholders:** todos los pasos incluyen código y comandos con resultado esperado.
- **Consistencia de tipos:** `getAdminReservas(estado)` con `estado` string/undefined en Tasks 3-4; `getAdminMensajes(leido)` con `undefined`/`boolean` en Tasks 3-5; `patchAdminReservaEstado(id, 'aprobada')` consistente en Task 4; endpoints `/admin/reservas` y `/admin/mensajes` coinciden con el prefijo `/api/v1` de la instancia axios y con los controllers backend.
