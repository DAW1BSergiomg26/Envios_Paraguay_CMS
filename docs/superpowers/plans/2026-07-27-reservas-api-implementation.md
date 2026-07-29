# Reservas API REST — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar API REST completa para gestión de reservas (admin CRUD + booking público con validación de disponibilidad).

**Architecture:** Dos controllers separados siguiendo el patrón existente: `ReservaApiController` (admin autenticado en `/api/v1/admin/reservas`) y `ReservaPublicApiController` (público anónimo en `/api/v1/reservas`). Service layer con lógica de validación de disponibilidad y transiciones de estado. DTOs manuales con getters/setters (sin Lombok).

**Tech Stack:** Spring Boot 3.3.5, Spring Data JPA, Spring Security, Jakarta Validation, Java 17

## Global Constraints

- Java 17, Spring Boot 3.3.5
- DTOs: getters/setters manuales (sin Lombok, sin records) — patrón existente
- Paquete base: `com.monteastur.envios`
- Sin cambios en `SecurityConfig` (rutas `/api/v1/admin/**` ya protegidas)
- Sin cambios en modelo `Reserva.java` (schema existente)
- Errores: reutilizar `ErrorDto` existente
- Validación: Jakarta `@NotBlank`, `@NotNull`, `@Email`, `@Min`

---

## File Structure

| Archivo | Responsabilidad |
|---------|----------------|
| `repository/ReservaRepository.java` | **Modificar** — añadir query de overlap |
| `service/ReservaService.java` | **Modificar** — añadir lógica CRUD admin + booking público |
| `dto/api/ReservaAdminDto.java` | **Crear** — DTO de respuesta para admin |
| `dto/api/CrearReservaPublicRequest.java` | **Crear** — Request DTO para booking público |
| `controller/api/ReservaApiController.java` | **Crear** — CRUD admin endpoints |
| `controller/api/ReservaPublicApiController.java` | **Crear** — POST público booking |

---

### Task 1: Repository — Query de disponibilidad

**Files:**
- Modify: `src/main/java/com/monteastur/envios/repository/ReservaRepository.java`

**Interfaces:**
- Produces: `existsOverlap(LocalDate fechaEntrada, LocalDate fechaSalida, Long excludeId)` y `existsOverlap(LocalDate fechaEntrada, LocalDate fechaSalida)` — usados por `ReservaService`

- [ ] **Step 1: Añadir queries de overlap al repositorio**

Abrir `ReservaRepository.java` y añadir los siguientes métodos después de `findOcupadasEnRango`:

```java
@Query("SELECT COUNT(r) > 0 FROM Reserva r WHERE r.estado IN ('pendiente', 'aprobada', 'confirmada') AND r.fechaEntrada < :fechaSalida AND r.fechaSalida > :fechaEntrada")
boolean existsOverlap(@Param("fechaEntrada") LocalDate fechaEntrada, @Param("fechaSalida") LocalDate fechaSalida);

@Query("SELECT COUNT(r) > 0 FROM Reserva r WHERE r.estado IN ('pendiente', 'aprobada', 'confirmada') AND r.fechaEntrada < :fechaSalida AND r.fechaSalida > :fechaEntrada AND r.id != :excludeId")
boolean existsOverlapExcluding(@Param("fechaEntrada") LocalDate fechaEntrada, @Param("fechaSalida") LocalDate fechaSalida, @Param("excludeId") Long excludeId);
```

- [ ] **Step 2: Verificar compilación**

Run: `mvn compile -q -f pom.xml`
Expected: BUILD SUCCESS (sin errores de compilación)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/monteastur/envios/repository/ReservaRepository.java
git commit -m "feat(reservas): add overlap queries to ReservaRepository"
```

---

### Task 2: DTOs — ReservaAdminDto y CrearReservaPublicRequest

**Files:**
- Create: `src/main/java/com/monteastur/envios/dto/api/ReservaAdminDto.java`
- Create: `src/main/java/com/monteastur/envios/dto/api/CrearReservaPublicRequest.java`

**Interfaces:**
- Produces: `ReservaAdminDto` (usado por `ReservaApiController`) y `CrearReservaPublicRequest` (usado por `ReservaPublicApiController`)

- [ ] **Step 1: Crear ReservaAdminDto**

Crear fichero `src/main/java/com/monteastur/envios/dto/api/ReservaAdminDto.java`:

```java
package com.monteastur.envios.dto.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReservaAdminDto {
    private Long id;
    private String nombreCliente;
    private String email;
    private String telefono;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private Integer numeroHuespedes;
    private String comentarios;
    private String estado;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public void setFechaEntrada(LocalDate fechaEntrada) { this.fechaEntrada = fechaEntrada; }
    public LocalDate getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDate fechaSalida) { this.fechaSalida = fechaSalida; }
    public Integer getNumeroHuespedes() { return numeroHuespedes; }
    public void setNumeroHuespedes(Integer numeroHuespedes) { this.numeroHuespedes = numeroHuespedes; }
    public String getComentarios() { return comentarios; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: Crear CrearReservaPublicRequest**

Crear fichero `src/main/java/com/monteastur/envios/dto/api/CrearReservaPublicRequest.java`:

```java
package com.monteastur.envios.dto.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CrearReservaPublicRequest {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombreCliente;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es válido")
    private String email;

    private String telefono;

    @NotNull(message = "La fecha de entrada es obligatoria")
    private LocalDate fechaEntrada;

    @NotNull(message = "La fecha de salida es obligatoria")
    private LocalDate fechaSalida;

    @NotNull(message = "El número de huéspedes es obligatorio")
    @Min(value = 1, message = "Debe haber al menos 1 huésped")
    private Integer numeroHuespedes;

    private String comentarios;

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public void setFechaEntrada(LocalDate fechaEntrada) { this.fechaEntrada = fechaEntrada; }
    public LocalDate getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDate fechaSalida) { this.fechaSalida = fechaSalida; }
    public Integer getNumeroHuespedes() { return numeroHuespedes; }
    public void setNumeroHuespedes(Integer numeroHuespedes) { this.numeroHuespedes = numeroHuespedes; }
    public String getComentarios() { return comentarios; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }
}
```

- [ ] **Step 3: Verificar compilación**

Run: `mvn compile -q -f pom.xml`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/monteastur/envios/dto/api/ReservaAdminDto.java src/main/java/com/monteastur/envios/dto/api/CrearReservaPublicRequest.java
git commit -m "feat(reservas): add ReservaAdminDto and CrearReservaPublicRequest DTOs"
```

---

### Task 3: Service — Lógica de negocio completa

**Files:**
- Modify: `src/main/java/com/monteastur/envios/service/ReservaService.java`

**Interfaces:**
- Consumes: `ReservaRepository` (ya existente, con queries del Task 1), `ReservaAdminDto`, `CrearReservaPublicRequest`
- Produces: `actualizar()`, `cambiarEstado()`, `verificarDisponibilidad()`, `crearPublico()` — usados por los controllers del Task 4 y Task 5

- [ ] **Step 1: Reescribir ReservaService.java con lógica completa**

Reemplazar el contenido completo de `src/main/java/com/monteastur/envios/service/ReservaService.java`:

```java
package com.monteastur.envios.service;

import com.monteastur.envios.dto.api.ActualizarReservaRequest;
import com.monteastur.envios.dto.api.CrearReservaPublicRequest;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReservaService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("PENDIENTE", "APROBADA", "CONFIRMADA", "CANCELADA");

    private static final Map<String, Set<String>> TRANSICIONES_PERMITIDAS = Map.of(
        "PENDIENTE", Set.of("APROBADA", "CANCELADA"),
        "APROBADA", Set.of("CONFIRMADA", "CANCELADA"),
        "CONFIRMADA", Set.of("CANCELADA")
    );

    private final ReservaRepository repo;

    public ReservaService(ReservaRepository repo) {
        this.repo = repo;
    }

    public Reserva crear(Reserva reserva) {
        return repo.save(reserva);
    }

    public Optional<Reserva> buscarPorId(Long id) {
        return repo.findById(id);
    }

    public List<Reserva> listarTodas() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    // --- Booking público ---

    @Transactional
    public Reserva crearPublico(CrearReservaPublicRequest request) {
        if (request.getFechaEntrada().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de entrada no puede ser en el pasado");
        }
        if (!request.getFechaSalida().isAfter(request.getFechaEntrada())) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la de entrada");
        }
        if (repo.existsOverlap(request.getFechaEntrada(), request.getFechaSalida())) {
            throw new IllegalStateException("Las fechas seleccionadas no están disponibles");
        }

        Reserva r = new Reserva(
            request.getNombreCliente(),
            request.getEmail(),
            request.getTelefono(),
            request.getFechaEntrada(),
            request.getFechaSalida(),
            request.getNumeroHuespedes(),
            request.getComentarios()
        );
        return repo.save(r);
    }

    // --- Admin CRUD ---

    @Transactional
    public Optional<Reserva> actualizar(Long id, ActualizarReservaRequest request) {
        return repo.findById(id).map(r -> {
            if (request.getNombreCliente() != null) r.setNombreCliente(request.getNombreCliente());
            if (request.getEmail() != null) r.setEmail(request.getEmail());
            if (request.getTelefono() != null) r.setTelefono(request.getTelefono());
            if (request.getNumeroHuespedes() != null) r.setNumeroHuespedes(request.getNumeroHuespedes());
            if (request.getComentarios() != null) r.setComentarios(request.getComentarios());

            boolean fechasCambiadas = (request.getFechaEntrada() != null && !request.getFechaEntrada().equals(r.getFechaEntrada()))
                                   || (request.getFechaSalida() != null && !request.getFechaSalida().equals(r.getFechaSalida()));

            if (request.getFechaEntrada() != null) r.setFechaEntrada(request.getFechaEntrada());
            if (request.getFechaSalida() != null) r.setFechaSalida(request.getFechaSalida());

            if (fechasCambiadas) {
                if (r.getFechaEntrada().isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("La fecha de entrada no puede ser en el pasado");
                }
                if (!r.getFechaSalida().isAfter(r.getFechaEntrada())) {
                    throw new IllegalArgumentException("La fecha de salida debe ser posterior a la de entrada");
                }
                if (repo.existsOverlapExcluding(r.getFechaEntrada(), r.getFechaSalida(), r.getId())) {
                    throw new IllegalStateException("Las fechas seleccionadas no están disponibles");
                }
            }

            return repo.save(r);
        });
    }

    @Transactional
    public Optional<Reserva> cambiarEstado(Long id, String nuevoEstado) {
        String estadoNormalizado = nuevoEstado.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(estadoNormalizado)) {
            throw new IllegalArgumentException("Estado no válido: " + nuevoEstado);
        }

        return repo.findById(id).map(r -> {
            Set<String> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(r.getEstado(), Set.of());
            if (!permitidos.contains(estadoNormalizado)) {
                throw new IllegalStateException(
                    "Transición no permitida: " + r.getEstado() + " → " + estadoNormalizado
                );
            }
            r.setEstado(estadoNormalizado);
            return repo.save(r);
        });
    }

    // --- Disponibilidad ---

    public boolean verificarDisponibilidad(LocalDate fechaEntrada, LocalDate fechaSalida) {
        return !repo.existsOverlap(fechaEntrada, fechaSalida);
    }
}
```

- [ ] **Step 2: Crear ActualizarReservaRequest**

Crear fichero `src/main/java/com/monteastur/envios/dto/api/ActualizarReservaRequest.java`:

```java
package com.monteastur.envios.dto.api;

import java.time.LocalDate;

public class ActualizarReservaRequest {
    private String nombreCliente;
    private String email;
    private String telefono;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private Integer numeroHuespedes;
    private String comentarios;

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public void setFechaEntrada(LocalDate fechaEntrada) { this.fechaEntrada = fechaEntrada; }
    public LocalDate getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDate fechaSalida) { this.fechaSalida = fechaSalida; }
    public Integer getNumeroHuespedes() { return numeroHuespedes; }
    public void setNumeroHuespedes(Integer numeroHuespedes) { this.numeroHuespedes = numeroHuespedes; }
    public String getComentarios() { return comentarios; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }
}
```

- [ ] **Step 3: Verificar compilación**

Run: `mvn compile -q -f pom.xml`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/ReservaService.java src/main/java/com/monteastur/envios/dto/api/ActualizarReservaRequest.java
git commit -m "feat(reservas): implement ReservaService business logic with availability and state transitions"
```

---

### Task 4: Admin Controller — CRUD endpoints

**Files:**
- Create: `src/main/java/com/monteastur/envios/controller/api/ReservaApiController.java`

**Interfaces:**
- Consumes: `ReservaService` (Task 3), `ReservaAdminDto` (Task 2), `ActualizarReservaRequest`, `ActualizarEstadoRequest` (ya existente), `ErrorDto` (ya existente)

- [ ] **Step 1: Crear ReservaApiController**

Crear fichero `src/main/java/com/monteastur/envios/controller/api/ReservaApiController.java`:

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.*;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.service.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/reservas")
public class ReservaApiController {

    private final ReservaService reservaService;

    public ReservaApiController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public ResponseEntity<List<ReservaAdminDto>> listar(
            @RequestParam(required = false) String estado) {

        List<Reserva> reservas = reservaService.listarTodas();

        if (estado != null && !estado.isBlank()) {
            String estadoNormalizado = estado.trim().toUpperCase();
            reservas = reservas.stream()
                .filter(r -> r.getEstado().equals(estadoNormalizado))
                .collect(Collectors.toList());
        }

        List<ReservaAdminDto> dtos = reservas.stream()
            .map(this::toDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable Long id) {
        return reservaService.buscarPorId(id)
            .map(r -> ResponseEntity.ok(toDto(r)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorDto(Instant.now().toString(), 404, "Reserva no encontrada")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                         @RequestBody ActualizarReservaRequest request) {
        try {
            return reservaService.actualizar(id, request)
                .map(r -> ResponseEntity.ok(toDto(r)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDto(Instant.now().toString(), 404, "Reserva no encontrada")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorDto(Instant.now().toString(), 409, e.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                            @RequestBody ActualizarEstadoRequest request) {
        try {
            return reservaService.cambiarEstado(id, request.getEstado())
                .map(r -> ResponseEntity.ok(toDto(r)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDto(Instant.now().toString(), 404, "Reserva no encontrada")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        return reservaService.buscarPorId(id)
            .map(r -> {
                reservaService.eliminar(id);
                return ResponseEntity.noContent().build();
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorDto(Instant.now().toString(), 404, "Reserva no encontrada")));
    }

    private ReservaAdminDto toDto(Reserva r) {
        ReservaAdminDto dto = new ReservaAdminDto();
        dto.setId(r.getId());
        dto.setNombreCliente(r.getNombreCliente());
        dto.setEmail(r.getEmail());
        dto.setTelefono(r.getTelefono());
        dto.setFechaEntrada(r.getFechaEntrada());
        dto.setFechaSalida(r.getFechaSalida());
        dto.setNumeroHuespedes(r.getNumeroHuespedes());
        dto.setComentarios(r.getComentarios());
        dto.setEstado(r.getEstado());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}
```

- [ ] **Step 2: Verificar compilación**

Run: `mvn compile -q -f pom.xml`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/monteastur/envios/controller/api/ReservaApiController.java
git commit -m "feat(reservas): add admin CRUD API controller for reservations"
```

---

### Task 5: Public Controller — Booking endpoint

**Files:**
- Create: `src/main/java/com/monteastur/envios/controller/api/ReservaPublicApiController.java`

**Interfaces:**
- Consumes: `ReservaService.crearPublico()` (Task 3), `CrearReservaPublicRequest` (Task 2), `ErrorDto` (ya existente)

- [ ] **Step 1: Crear ReservaPublicApiController**

Crear fichero `src/main/java/com/monteastur/envios/controller/api/ReservaPublicApiController.java`:

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.CrearReservaPublicRequest;
import com.monteastur.envios.dto.api.ErrorDto;
import com.monteastur.envios.dto.api.ReservaAdminDto;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaPublicApiController {

    private final ReservaService reservaService;

    public ReservaPublicApiController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CrearReservaPublicRequest request) {
        try {
            Reserva reserva = reservaService.crearPublico(request);
            ReservaAdminDto dto = toDto(reserva);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorDto(Instant.now().toString(), 409, e.getMessage()));
        }
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidad(
            @RequestParam String fechaEntrada,
            @RequestParam String fechaSalida) {
        try {
            java.time.LocalDate inicio = java.time.LocalDate.parse(fechaEntrada);
            java.time.LocalDate fin = java.time.LocalDate.parse(fechaSalida);
            boolean disponible = reservaService.verificarDisponibilidad(inicio, fin);
            return ResponseEntity.ok(Map.of(
                "disponible", disponible,
                "fechaEntrada", fechaEntrada,
                "fechaSalida", fechaSalida
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Formato de fecha inválido. Use YYYY-MM-DD."));
        }
    }

    private ReservaAdminDto toDto(Reserva r) {
        ReservaAdminDto dto = new ReservaAdminDto();
        dto.setId(r.getId());
        dto.setNombreCliente(r.getNombreCliente());
        dto.setEmail(r.getEmail());
        dto.setTelefono(r.getTelefono());
        dto.setFechaEntrada(r.getFechaEntrada());
        dto.setFechaSalida(r.getFechaSalida());
        dto.setNumeroHuespedes(r.getNumeroHuespedes());
        dto.setComentarios(r.getComentarios());
        dto.setEstado(r.getEstado());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}
```

- [ ] **Step 2: Verificar compilación**

Run: `mvn compile -q -f pom.xml`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/monteastur/envios/controller/api/ReservaPublicApiController.java
git commit -m "feat(reservas): add public booking API with availability validation"
```

---

### Task 6: Verificación end-to-end

**Files:**
- Sin cambios de ficheros

- [ ] **Step 1: Verificar compilación completa**

Run: `mvn compile -f pom.xml`
Expected: BUILD SUCCESS

- [ ] **Step 2: Verificar estructura de archivos creados**

Run: `Get-ChildItem -Recurse src/main/java -Filter "*.java" | Where-Object { $_.Name -match "Reserva" } | Select-Object FullName`
Expected: 6 ficheros — model/Reserva.java, repository/ReservaRepository.java, service/ReservaService.java, dto/api/ReservaAdminDto.java, dto/api/CrearReservaPublicRequest.java, dto/api/ActualizarReservaRequest.java, controller/api/ReservaApiController.java, controller/api/ReservaPublicApiController.java

- [ ] **Step 3: Verificar que el paquete de DTOs incluye todos los ficheros nuevos**

Run: `Get-ChildItem src/main/java/com/monteastur/envios/dto/api/`
Expected: ErrorDto.java, AdminEnvioResumenDto.java, ClienteEnvioResumenDto.java, ActualizarEstadoRequest.java, ActualizarReservaRequest.java, CrearReservaPublicRequest.java, EvidenciaDto.java, EventoDto.java, PublicTrackingDto.java, PushSubscriptionRequest.java, ReservaAdminDto.java, TrackingDto.java

- [ ] **Step 4: Verificar que los controllers API están en el paquete correcto**

Run: `Get-ChildItem src/main/java/com/monteastur/envios/controller/api/`
Expected: AdminApiController.java, ClienteApiController.java, TrackingApiController.java, PushSubscriptionController.java, TrackingApiExceptionHandler.java, ReservaApiController.java, ReservaPublicApiController.java

- [ ] **Step 5: Commit final (si hay archivos sueltos)**

```bash
git status
git add -A
git commit -m "feat(reservas): complete REST API for reservation management" --allow-empty
```
