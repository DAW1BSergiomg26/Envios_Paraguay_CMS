# Bloque 2: Controller Refactor + Exception Handling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor `PublicController` to eliminate ES/EN duplication via multi-route locale-based routing, and add centralized exception handling with custom exceptions and a unified `@ControllerAdvice`.

**Architecture:** Three custom `RuntimeException` classes (ResourceNotFoundException → 404, BadRequestException → 400, ConflictException → 409) feed into a single `GlobalExceptionHandler` that detects REST vs MVC by URL prefix. `PublicController` uses `@GetMapping({"/path", "/en/path"})` with private helpers to resolve template prefix and locale-sensitive data, eliminating 11 duplicate EN methods.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Spring MVC, Thymeleaf

## Global Constraints

- All new exception classes go in `com.monteastur.envios.exception` package
- `GlobalExceptionHandler` replaces `TrackingApiExceptionHandler` (delete the old file)
- `@ControllerAdvice` checks URL prefix `/api/` for REST vs MVC distinction
- `PublicController` uses `request.getRequestURI()` for locale detection, NOT `LocaleContextHolder`
- Nested static classes `MesCalendario`/`DiaCalendario` stay in `PublicController`
- Error templates follow existing luxury design system (glassmorphism, CSS variables)

---

### Task 1: Exception Hierarchy (3 classes)

**Files:**
- Create: `src/main/java/com/monteastur/envios/exception/ResourceNotFoundException.java`
- Create: `src/main/java/com/monteastur/envios/exception/BadRequestException.java`
- Create: `src/main/java/com/monteastur/envios/exception/ConflictException.java`

**Interfaces:**
- Produces: `ResourceNotFoundException(String message)` — extends `RuntimeException`, constructor `(String message)` and `(String message, Throwable cause)`
- Produces: `BadRequestException(String message)` — same
- Produces: `ConflictException(String message)` — same

- [ ] **Step 1: Create `ResourceNotFoundException.java`**

```java
package com.monteastur.envios.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 2: Create `BadRequestException.java`**

```java
package com.monteastur.envios.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 3: Create `ConflictException.java`**

```java
package com.monteastur.envios.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/monteastur/envios/exception/
git commit -m "feat: add custom exception hierarchy (ResourceNotFoundException, BadRequestException, ConflictException)"
```

---

### Task 2: GlobalExceptionHandler + Error Templates

**Files:**
- Create: `src/main/java/com/monteastur/envios/controller/GlobalExceptionHandler.java`
- Create: `src/main/resources/templates/error.html`
- Create: `src/main/resources/templates/en/error.html`
- Delete: `src/main/java/com/monteastur/envios/controller/api/TrackingApiExceptionHandler.java`
- Modify: none

**Interfaces:**
- Consumes: `ResourceNotFoundException`, `BadRequestException`, `ConflictException`, `ErrorDto`
- Produces: REST: `ResponseEntity<ErrorDto>`, MVC: `String` view name with model attributes

- [ ] **Step 1: Create `GlobalExceptionHandler.java`**

```java
package com.monteastur.envios.controller;

import com.monteastur.envios.dto.api.ErrorDto;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request, Model model) {
        if (isRestRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorDto(Instant.now().toString(), 404, ex.getMessage()));
        }
        return mvcError(request, model, HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public Object handleBadRequest(BadRequestException ex, HttpServletRequest request, Model model) {
        if (isRestRequest(request)) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, ex.getMessage()));
        }
        return mvcError(request, model, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public Object handleConflict(ConflictException ex, HttpServletRequest request, Model model) {
        if (isRestRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorDto(Instant.now().toString(), 409, ex.getMessage()));
        }
        return mvcError(request, model, HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(DateTimeParseException.class)
    public Object handleDateTimeParse(DateTimeParseException ex, HttpServletRequest request, Model model) {
        if (isRestRequest(request)) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, "Formato de fecha inválido. Use YYYY-MM-DD."));
        }
        return mvcError(request, model, HttpStatus.BAD_REQUEST, "Bad Request", "Formato de fecha inválido.");
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex, HttpServletRequest request, Model model) {
        if (isRestRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDto(Instant.now().toString(), 500, "Error interno del servidor"));
        }
        return mvcError(request, model, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Ha ocurrido un error inesperado. Por favor, inténtelo de nuevo más tarde.");
    }

    private boolean isRestRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }

    private String mvcError(HttpServletRequest request, Model model, HttpStatus status, String error, String message) {
        model.addAttribute("status", status.value());
        model.addAttribute("error", error);
        model.addAttribute("message", message);
        model.addAttribute("timestamp", Instant.now().toString());
        boolean english = request.getRequestURI().startsWith("/en/") || request.getRequestURI().equals("/en");
        return english ? "en/error" : "error";
    }
}
```

- [ ] **Step 2: Create `error.html`**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${status} + ' - ' + ${error}">Error</title>
    <link rel="stylesheet" href="/css/luxury-core.css">
    <style>
        body {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            background: var(--bg-gradient);
            font-family: 'Inter', system-ui, sans-serif;
            margin: 0;
            padding: 2rem;
        }
        .error-card {
            background: var(--glass-bg);
            backdrop-filter: blur(var(--glass-blur));
            -webkit-backdrop-filter: blur(var(--glass-blur));
            border: 1px solid var(--glass-border);
            border-radius: var(--radius-xl);
            padding: 4rem 3rem;
            max-width: 480px;
            width: 100%;
            text-align: center;
            box-shadow: var(--shadow-elevated);
        }
        .error-code {
            font-size: 5rem;
            font-weight: 700;
            color: var(--color-accent);
            line-height: 1;
            margin-bottom: 0.5rem;
        }
        .error-title {
            font-size: 1.5rem;
            font-weight: 600;
            color: var(--color-text);
            margin-bottom: 1rem;
        }
        .error-message {
            color: var(--color-muted);
            margin-bottom: 2rem;
            line-height: 1.5;
        }
        .btn-luxury {
            display: inline-block;
            padding: 0.75rem 2rem;
            background: var(--color-accent);
            color: white;
            border-radius: var(--radius-full);
            text-decoration: none;
            font-weight: 600;
            transition: all 0.3s ease;
        }
        .btn-luxury:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(212, 175, 55, 0.3);
        }
    </style>
</head>
<body>
    <div class="error-card">
        <div class="error-code" th:text="${status}">404</div>
        <div class="error-title" th:text="${error}">Not Found</div>
        <div class="error-message" th:text="${message}">Page not found</div>
        <a href="/" class="btn-luxury">Volver al inicio</a>
    </div>
</body>
</html>
```

- [ ] **Step 3: Create `en/error.html`**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${status} + ' - ' + ${error}">Error</title>
    <link rel="stylesheet" href="/css/luxury-core.css">
    <style>
        body {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            background: var(--bg-gradient);
            font-family: 'Inter', system-ui, sans-serif;
            margin: 0;
            padding: 2rem;
        }
        .error-card {
            background: var(--glass-bg);
            backdrop-filter: blur(var(--glass-blur));
            -webkit-backdrop-filter: blur(var(--glass-blur));
            border: 1px solid var(--glass-border);
            border-radius: var(--radius-xl);
            padding: 4rem 3rem;
            max-width: 480px;
            width: 100%;
            text-align: center;
            box-shadow: var(--shadow-elevated);
        }
        .error-code {
            font-size: 5rem;
            font-weight: 700;
            color: var(--color-accent);
            line-height: 1;
            margin-bottom: 0.5rem;
        }
        .error-title {
            font-size: 1.5rem;
            font-weight: 600;
            color: var(--color-text);
            margin-bottom: 1rem;
        }
        .error-message {
            color: var(--color-muted);
            margin-bottom: 2rem;
            line-height: 1.5;
        }
        .btn-luxury {
            display: inline-block;
            padding: 0.75rem 2rem;
            background: var(--color-accent);
            color: white;
            border-radius: var(--radius-full);
            text-decoration: none;
            font-weight: 600;
            transition: all 0.3s ease;
        }
        .btn-luxury:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(212, 175, 55, 0.3);
        }
    </style>
</head>
<body>
    <div class="error-card">
        <div class="error-code" th:text="${status}">404</div>
        <div class="error-title" th:text="${error}">Not Found</div>
        <div class="error-message" th:text="${message}">Page not found</div>
        <a href="/en" class="btn-luxury">Back to home</a>
    </div>
</body>
</html>
```

- [ ] **Step 4: Delete `TrackingApiExceptionHandler.java`**

`git rm src/main/java/com/monteastur/envios/controller/api/TrackingApiExceptionHandler.java`

- [ ] **Step 5: Verify build**

```bash
mvn clean compile -q
```
Expected: BUILD SUCCESS (no errors, no warnings)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/monteastur/envios/controller/GlobalExceptionHandler.java src/main/resources/templates/error.html src/main/resources/templates/en/error.html
git rm src/main/java/com/monteastur/envios/controller/api/TrackingApiExceptionHandler.java
git commit -m "feat: add GlobalExceptionHandler with REST/MVC routing, error templates; remove old TrackingApiExceptionHandler"
```

---

### Task 3: PublicController Refactor

**Files:**
- Modify: `src/main/java/com/monteastur/envios/controller/PublicController.java`

**Interfaces:**
- Consumes: same repositories and services as before (no change)
- Produces: same template views and model attributes as before

**Key changes:**
- 22 endpoints → 11 endpoints (all EN methods removed)
- Helper `template(view, request)` resolves `"en/" + view` when URI starts with `/en/`
- Helper `monthNames(request)` returns `MESES_ES` or `MESES_EN` based on URI
- `request.getSession()` only called for `/reservas` and `/en/reservas` (kept in the unified method)
- All mapping annotations use `{"/...", "/en/..."}` multi-route syntax
- Note: `/casa` and `/lacasa` both map to Spanish, `/en/casa` maps to English

- [ ] **Step 1: Rewrite `PublicController.java`**

Read the current file first, then replace entirely:

```java
package com.monteastur.envios.controller;

import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.Imagen;
import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.model.TextoLegal;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.repository.MensajeContactoRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.TextoLegalRepository;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.EventoTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.*;

@Controller
public class PublicController {

    private static final String[] MESES_ES =
        {"Enero","Febrero","Marzo","Abril","Mayo","Junio",
         "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
    private static final String[] MESES_EN =
        {"January","February","March","April","May","June",
         "July","August","September","October","November","December"};

    private final MensajeContactoRepository mensajeRepo;
    private final ReservaRepository reservaRepo;
    private final ImagenRepository imagenRepo;
    private final TextoLegalRepository textoRepo;
    private final EnvioTrackingRepository trackingRepo;
    private final EmailService emailService;
    private final EventoTrackingService eventoTrackingService;

    public PublicController(MensajeContactoRepository mensajeRepo, ReservaRepository reservaRepo,
                            ImagenRepository imagenRepo, TextoLegalRepository textoRepo,
                            EnvioTrackingRepository trackingRepo, EmailService emailService,
                            EventoTrackingService eventoTrackingService) {
        this.mensajeRepo = mensajeRepo;
        this.reservaRepo = reservaRepo;
        this.imagenRepo = imagenRepo;
        this.textoRepo = textoRepo;
        this.trackingRepo = trackingRepo;
        this.emailService = emailService;
        this.eventoTrackingService = eventoTrackingService;
    }

    @GetMapping({"/", "/en"})
    public String index(HttpServletRequest request) {
        return template("home", request);
    }

    @GetMapping({"/casa", "/lacasa", "/en/casa"})
    public String laCasa(Model model, HttpServletRequest request) {
        model.addAttribute("imagenes", imagenRepo.findAllByOrderByOrdenAsc());
        return template("lacasa", request);
    }

    @GetMapping({"/entorno", "/en/entorno"})
    public String entorno(HttpServletRequest request) {
        return template("entorno", request);
    }

    @GetMapping({"/reservas", "/en/reservas"})
    public String reservas(Model model, HttpServletRequest request) {
        request.getSession();
        model.addAttribute("reservaEnviada", false);
        model.addAttribute("calendarios", generarCalendarios(occupiedDates(), monthNames(request)));
        return template("reservas", request);
    }

    @PostMapping({"/reservas", "/en/reservas"})
    public String enviarReserva(@RequestParam String nombreCliente,
                                 @RequestParam String email,
                                 @RequestParam(required = false) String telefono,
                                 @RequestParam String fechaEntrada,
                                 @RequestParam String fechaSalida,
                                 @RequestParam Integer numeroHuespedes,
                                 @RequestParam(required = false) String comentarios,
                                 Model model, HttpServletRequest request) {
        Reserva res = new Reserva(nombreCliente, email, telefono,
                                  LocalDate.parse(fechaEntrada), LocalDate.parse(fechaSalida),
                                  numeroHuespedes, comentarios);
        reservaRepo.save(res);
        emailService.notificarReserva(res);
        model.addAttribute("reservaEnviada", true);
        return template("reservas", request);
    }

    @GetMapping({"/contacto", "/en/contacto"})
    public String contacto(Model model, HttpServletRequest request) {
        model.addAttribute("mensajeEnviado", false);
        return template("contacto", request);
    }

    @PostMapping({"/contacto", "/en/contacto"})
    public String enviarContacto(@RequestParam String nombre,
                                  @RequestParam String email,
                                  @RequestParam(required = false) String telefono,
                                  @RequestParam String mensaje,
                                  Model model, HttpServletRequest request) {
        MensajeContacto msg = new MensajeContacto(nombre, email, telefono, mensaje);
        mensajeRepo.save(msg);
        emailService.notificarContacto(nombre, email, mensaje);
        model.addAttribute("mensajeEnviado", true);
        return template("contacto", request);
    }

    @GetMapping({"/operaciones", "/en/operaciones"})
    public String operaciones(HttpServletRequest request) {
        return template("operaciones", request);
    }

    @GetMapping({"/aviso-legal", "/en/aviso-legal"})
    public String avisoLegal(Model model, HttpServletRequest request) {
        model.addAttribute("texto", textoRepo.findBySlug("aviso-legal").orElse(null));
        return template("aviso-legal", request);
    }

    @GetMapping({"/politica-cookies", "/en/politica-cookies"})
    public String politicaCookies(Model model, HttpServletRequest request) {
        model.addAttribute("texto", textoRepo.findBySlug("politica-cookies").orElse(null));
        return template("politica-cookies", request);
    }

    @GetMapping({"/tracking", "/en/tracking"})
    public String tracking(Model model, HttpServletRequest request) {
        model.addAttribute("envio", null);
        model.addAttribute("buscado", false);
        return template("tracking", request);
    }

    @PostMapping({"/tracking", "/en/tracking"})
    public String buscarTracking(@RequestParam String codigo, Model model, HttpServletRequest request) {
        var optEnvio = trackingRepo.findByCodigoUnico(codigo.trim().toUpperCase());
        model.addAttribute("envio", optEnvio.orElse(null));
        model.addAttribute("buscado", true);
        optEnvio.ifPresent(e -> model.addAttribute("eventos", eventoTrackingService.listarPorEnvio(e.getId())));
        return template("tracking", request);
    }

    // -----------------------------------------------------------
    //  CALENDARIO DE DISPONIBILIDAD
    // -----------------------------------------------------------

    public static class MesCalendario {
        private final String nombre;
        private final int year;
        private final List<List<DiaCalendario>> semanas;
        public MesCalendario(String nombre, int year, List<List<DiaCalendario>> semanas) {
            this.nombre = nombre; this.year = year; this.semanas = semanas;
        }
        public String getNombre() { return nombre; }
        public int getYear() { return year; }
        public List<List<DiaCalendario>> getSemanas() { return semanas; }
    }

    public static class DiaCalendario {
        private final int numero;
        private final boolean ocupado;
        private final boolean pasado;
        private final boolean relleno;
        public DiaCalendario(int numero, boolean ocupado, boolean pasado, boolean relleno) {
            this.numero = numero; this.ocupado = ocupado; this.pasado = pasado; this.relleno = relleno;
        }
        public int getNumero() { return numero; }
        public boolean isOcupado() { return ocupado; }
        public boolean isPasado() { return pasado; }
        public boolean isRelleno() { return relleno; }
    }

    private String template(String view, HttpServletRequest request) {
        return isEnglish(request) ? "en/" + view : view;
    }

    private boolean isEnglish(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/en/") || uri.equals("/en");
    }

    private String[] monthNames(HttpServletRequest request) {
        return isEnglish(request) ? MESES_EN : MESES_ES;
    }

    private Set<LocalDate> occupiedDates() {
        LocalDate today = LocalDate.now();
        LocalDate fin = today.plusMonths(3).withDayOfMonth(1).plusMonths(1).minusDays(1);
        List<Reserva> ocupadas = reservaRepo.findOcupadasEnRango(today, fin);
        Set<LocalDate> set = new HashSet<>();
        for (Reserva r : ocupadas) {
            LocalDate d = r.getFechaEntrada();
            while (d.isBefore(r.getFechaSalida())) {
                if (!d.isBefore(today)) set.add(d);
                d = d.plusDays(1);
            }
        }
        return set;
    }

    private List<MesCalendario> generarCalendarios(Set<LocalDate> occupied, String[] meses) {
        List<MesCalendario> calendarios = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate inicio = today.withDayOfMonth(1);
        for (int i = 0; i < 3; i++) {
            LocalDate mes = inicio.plusMonths(i);
            int year = mes.getYear();
            int month = mes.getMonthValue();
            int diasEnMes = mes.lengthOfMonth();
            List<List<DiaCalendario>> semanas = new ArrayList<>();
            List<DiaCalendario> semana = new ArrayList<>();
            LocalDate first = LocalDate.of(year, month, 1);
            int padding = first.getDayOfWeek().getValue() - 1;
            for (int p = 0; p < padding; p++)
                semana.add(new DiaCalendario(0, false, false, true));
            for (int d = 1; d <= diasEnMes; d++) {
                LocalDate fecha = LocalDate.of(year, month, d);
                boolean ocupado = occupied.contains(fecha);
                boolean pasado = fecha.isBefore(today);
                semana.add(new DiaCalendario(d, ocupado, pasado, false));
                if (semana.size() == 7) {
                    semanas.add(semana);
                    semana = new ArrayList<>();
                }
            }
            if (!semana.isEmpty()) {
                while (semana.size() < 7)
                    semana.add(new DiaCalendario(0, false, false, true));
                semanas.add(semana);
            }
            calendarios.add(new MesCalendario(meses[month - 1], year, semanas));
        }
        return calendarios;
    }
}
```

- [ ] **Step 2: Verify build**

```bash
mvn clean compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/monteastur/envios/controller/PublicController.java
git commit -m "refactor: deduplicate ES/EN in PublicController using multi-route locale-based routing"
```

---

### Task 4: API Controller Cleanup

**Files:**
- Modify: `src/main/java/com/monteastur/envios/controller/api/ReservaApiController.java`
- Modify: `src/main/java/com/monteastur/envios/controller/api/ReservaPublicApiController.java`
- Modify: `src/main/java/com/monteastur/envios/controller/api/AdminApiController.java`
- Modify: `src/main/java/com/monteastur/envios/controller/api/ClienteApiController.java`

**Interfaces:**
- Consumes: `ResourceNotFoundException`, `BadRequestException`, `ConflictException`, `GlobalExceptionHandler`
- Produces: Same REST endpoints, same response shapes — no behavioral change, only cleanup

**Key changes:**
- Replace `Optional.isPresent()` + inline 404 ErrorDto with `orElseThrow(() -> new ResourceNotFoundException(...))`
- Remove try/catch for `IllegalArgumentException`/`IllegalStateException` — let `GlobalExceptionHandler` catch custom exceptions
- Services are not changed — they still throw `IllegalArgumentException`/`IllegalStateException`. We could wrap them, but the minimal change is to let the handler catch these and map to custom exceptions. Actually no — the services throw standard Java exceptions, not our custom ones. The handler only handles custom exceptions. So we need to:
  - Option A: Update services to throw custom exceptions (invasive)
  - Option B: Add handlers for `IllegalArgumentException` → 400, `IllegalStateException` → 409 in `GlobalExceptionHandler`
  
  Option B is simpler and doesn't touch service code. Let's add those handlers.

  Wait, actually the user said "limpiar los try/catch repetitivos en los controladores API (ReservaApiController)". They want the try/catch gone. The simplest path is to add `IllegalArgumentException` and `IllegalStateException` handlers to `GlobalExceptionHandler` that map them to 400 and 409 respectively. Then the controllers can just let exceptions propagate.

  Actually, I should also update `GlobalExceptionHandler` to handle these. Let me add those handlers.

- [ ] **Step 1: Update `GlobalExceptionHandler.java`** — add handlers for standard Java exceptions

Add these methods after the `handleGeneric` method:

```java
@ExceptionHandler(IllegalArgumentException.class)
public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
    if (isRestRequest(request)) {
        return ResponseEntity.badRequest()
            .body(new ErrorDto(Instant.now().toString(), 400, ex.getMessage()));
    }
    return mvcError(request, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
}

@ExceptionHandler(IllegalStateException.class)
public Object handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
    if (isRestRequest(request)) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorDto(Instant.now().toString(), 409, ex.getMessage()));
    }
    return mvcError(request, HttpStatus.CONFLICT, "Conflict", ex.getMessage());
}
```

- [ ] **Step 2: Clean up `ReservaApiController.java`**

Read the current file. Replace inline try/catch and Optional checks with `orElseThrow`:

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.*;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.service.ReservaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<ReservaAdminDto> detalle(@PathVariable Long id) {
        Reserva reserva = reservaService.buscarPorId(id)
            .orElseThrow(() -> new com.monteastur.envios.exception.ResourceNotFoundException("Reserva no encontrada: " + id));
        return ResponseEntity.ok(toDto(reserva));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservaAdminDto> actualizar(@PathVariable Long id,
                                                       @RequestBody ActualizarReservaRequest request) {
        Reserva reserva = reservaService.actualizar(id, request)
            .orElseThrow(() -> new com.monteastur.envios.exception.ResourceNotFoundException("Reserva no encontrada: " + id));
        return ResponseEntity.ok(toDto(reserva));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReservaAdminDto> cambiarEstado(@PathVariable Long id,
                                                          @RequestBody ActualizarEstadoRequest request) {
        Reserva reserva = reservaService.cambiarEstado(id, request.getEstado())
            .orElseThrow(() -> new com.monteastur.envios.exception.ResourceNotFoundException("Reserva no encontrada: " + id));
        return ResponseEntity.ok(toDto(reserva));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        Reserva reserva = reservaService.buscarPorId(id)
            .orElseThrow(() -> new com.monteastur.envios.exception.ResourceNotFoundException("Reserva no encontrada: " + id));
        reservaService.eliminar(id);
        return ResponseEntity.noContent().build();
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

- [ ] **Step 3: Clean up `ReservaPublicApiController.java`**

Remove try/catch, let exceptions propagate:

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.CrearReservaPublicRequest;
import com.monteastur.envios.dto.api.ReservaAdminDto;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaPublicApiController {

    private final ReservaService reservaService;

    public ReservaPublicApiController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<ReservaAdminDto> crear(@Valid @RequestBody CrearReservaPublicRequest request) {
        Reserva reserva = reservaService.crearPublico(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(reserva));
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidad(
            @RequestParam String fechaEntrada,
            @RequestParam String fechaSalida) {
        LocalDate inicio = LocalDate.parse(fechaEntrada);
        LocalDate fin = LocalDate.parse(fechaSalida);
        boolean disponible = reservaService.verificarDisponibilidad(inicio, fin);
        return ResponseEntity.ok(Map.of(
            "disponible", disponible,
            "fechaEntrada", fechaEntrada,
            "fechaSalida", fechaSalida
        ));
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

- [ ] **Step 4: Clean up `AdminApiController.java`**

Replace inline ErrorDto for 404 with `orElseThrow`. The `listarEnvios` method stays as-is (no error handling needed). The `detalleEnvio` and `actualizarEstado` methods:

```java
@GetMapping("/envios/{codigo}")
public ResponseEntity<TrackingDto> detalleEnvio(@PathVariable String codigo) {
    EnvioTracking envio = trackingRepo.findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
        .orElseThrow(() -> new com.monteastur.envios.exception.ResourceNotFoundException("Tracking no encontrado: " + codigo));
    return ResponseEntity.ok(toTrackingDto(envio));
}

@PutMapping("/envios/{codigo}/estado")
public ResponseEntity<TrackingDto> actualizarEstado(@PathVariable String codigo,
                                                     @RequestBody ActualizarEstadoRequest request) {
    EnvioTracking envio = trackingRepo.findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
        .orElseThrow(() -> new com.monteastur.envios.exception.ResourceNotFoundException("Tracking no encontrado: " + codigo));
    String estadoAnterior = envio.getEstado();
    envio.setEstado(request.getEstado());
    envio.setUltimaActualizacion(LocalDateTime.now());
    trackingRepo.save(envio);
    eventoTrackingService.crearEvento(envio, estadoAnterior);
    return ResponseEntity.ok(toTrackingDto(envio));
}
```

Add a private `toTrackingDto(EnvioTracking)` helper to avoid duplicating the mapping logic:

```java
private TrackingDto toTrackingDto(EnvioTracking envio) {
    TrackingDto dto = new TrackingDto();
    dto.setCodigoUnico(envio.getCodigoUnico());
    dto.setEstado(envio.getEstado());
    dto.setDestinatario(envio.getDestinatario());
    dto.setOrigen(envio.getOrigen());
    dto.setDestino(envio.getDestino());
    dto.setPeso(envio.getPeso());
    dto.setContenido(envio.getContenido());
    dto.setUltimaActualizacion(envio.getUltimaActualizacion());
    if (envio.getCliente() != null) {
        dto.setClienteNombre(envio.getCliente().getNombre());
        dto.setClienteEmail(envio.getCliente().getEmail());
    }
    dto.setEventos(eventoTrackingService.listarPorEnvio(envio.getId()).stream().map(ev -> {
        EventoDto evDto = new EventoDto();
        evDto.setFecha(ev.getFechaEvento());
        evDto.setDescripcion(ev.getDescripcion());
        evDto.setTipo(ev.getEstado());
        return evDto;
    }).collect(Collectors.toList()));
    dto.setEvidencias(evidenciaService.listarPorEnvio(envio.getId()).stream().map(ev -> {
        EvidenciaDto evDto = new EvidenciaDto();
        evDto.setTitulo(ev.getTitulo());
        evDto.setDescripcion(ev.getDescripcion());
        evDto.setTipo(ev.getTipo());
        evDto.setUrlArchivo(ev.getUrlArchivo());
        evDto.setVisibleCliente(ev.getVisibleCliente());
        return evDto;
    }).collect(Collectors.toList()));
    return dto;
}
```

Note: The `TrackingDto` and `EventoDto`/`EvidenciaDto` imports are already present. No new imports needed. Remove unused `Instant` import.

- [ ] **Step 5: Clean up `ClienteApiController.java`**

The 403 auth checks stay as-is (they're not covered by our exception hierarchy). Only clean up the 404 Optional checks with `orElseThrow`. The inline mapping logic stays unchanged (ClienteApiController uses different evidence URLs than AdminApiController).

In `detalleEnvio`, replace:
```java
var opt = trackingRepo.findWithClienteByCodigoUnico(codigo.trim().toUpperCase());
if (opt.isEmpty()) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorDto(Instant.now().toString(), 404, "Tracking no encontrado"));
}
EnvioTracking envio = opt.get();
```
with:
```java
EnvioTracking envio = trackingRepo.findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
    .orElseThrow(() -> new com.monteastur.envios.exception.ResourceNotFoundException("Tracking no encontrado: " + codigo));
```

In `descargarEvidencia`, replace:
```java
var opt = evidenciaService.buscar(id);
if (opt.isEmpty()) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorDto(Instant.now().toString(), 404, "Evidencia no encontrada"));
}
var evidencia = opt.get();
```
with:

```java
var evidencia = evidenciaService.buscar(id)
    .orElseThrow(() -> new com.monteastur.envios.exception.ResourceNotFoundException("Evidencia no encontrada: " + id));
```

The path traversal check and file serving logic stays unchanged. Remove unused imports (`Instant` from methods that no longer use it inline — but `Instant` is still used in 403 ErrorDto constructions, so keep the import).

- [ ] **Step 6: Verify build**

```bash
mvn clean compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 7: Verify runtime**

```bash
mvn clean package -DskipTests -q
```
Expected: BUILD SUCCESS

```bash
docker compose build app
```
Expected: BUILD SUCCESS (verify lucide.min.js is still present)

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/monteastur/envios/controller/api/ReservaApiController.java src/main/java/com/monteastur/envios/controller/api/ReservaPublicApiController.java src/main/java/com/monteastur/envios/controller/api/AdminApiController.java src/main/java/com/monteastur/envios/controller/api/ClienteApiController.java src/main/java/com/monteastur/envios/controller/GlobalExceptionHandler.java
git commit -m "refactor: clean up API controllers — remove inline try/catch and ErrorDto, delegate to GlobalExceptionHandler"
```

---

### Task 5: Push to GitHub

- [ ] **Step 1: Push**

```bash
git push origin feature/seguimiento-premium
```
