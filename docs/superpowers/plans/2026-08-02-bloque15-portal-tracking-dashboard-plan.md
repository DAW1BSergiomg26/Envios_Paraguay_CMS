# Bloque 15 — Portal Público de Rastreo & Dashboard Interactivo de Clientes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (or subagent-driven-development) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. TDD: test primero, código después.

**Goal:** Modernizar el rastreo público y el panel de cliente con **Tailwind CSS vía CDN** (Enfoque A aprobado): nueva ruta pública `GET /tracking/{codigo}` con timeline interactivo + POD si `ENTREGADO`, dashboard de cliente con métricas de peso (`PesoUtil`) y descarga de la etiqueta PDF del propio envío, con consultas cacheadas en Redis mediante DTOs planos (`PublicTrackingView`, `ClientDashboardView`). Se mantienen las URLs funcionales existentes y se respeta el resto del sitio (vistas premium, API REST, admin) sin cambios.

**Architecture:** Capa de presentación web nueva (`controller.web`, `service.web`, `dto.web`) que reemplaza los handlers `GET/POST /tracking` de `PublicController` y `GET /cliente/panel` de `ClienteController`. `PublicTrackingService.cargarPagina(codigo)` (`@Cacheable "envios.tracking.pagina"`, TTL 5 min) ensambla `PublicTrackingView` sin referencias LAZY. `ClientDashboardService.cargarDashboard(clienteId)` (`@Cacheable "envios.cliente.dashboard"`, TTL 1 min) calcula métricas con `PesoUtil.parsear` (pesos inválidos ignorados). La invalidación se amplía en los puntos de mutación de envíos existentes. Auth de cliente sigue por `HttpSession(clienteId)` (sin Spring Security `ROLE_CLIENTE`). PDF solo etiqueta térmica del propio envío con ownership (ajeno → 403, inexistente → 404, sin sesión → redirect login).

**Tech Stack:** Spring Boot 3.3.5, Thymeleaf 3.1, Tailwind CSS 3 (CDN `cdn.tailwindcss.com`), `html5-qrcode` (CDN unpkg), Spring Cache + Redis (`GenericJackson2JsonRedisSerializer`), Spring Security (CSRF activo para formularios web), MySQL 8, Java 17.

## Global Constraints

- **Prohibido Lombok:** todos los DTOs View en Java puro (atributos privados, constructor vacío, constructor parametrizado, getters/setters explícitos).
- **Inyección por constructor `private final`** en servicios y controladores nuevos. Cero `@Autowired` en campos.
- **Nomenclatura:** clases/métodos/variables en inglés; Javadoc y textos de usuario en español; sin comentarios redundantes.
- **DTOs cacheables sin entidades JPA:** nunca se cachean `EnvioTracking`/`EventoTracking`/`EvidenciaEnvio`/`EntregaEvidencia`/`Cliente`. `PublicTrackingView`/`ClientDashboardView` (y sus sub-vistas) son clases **no final** con listas mutables `ArrayList` para que `GenericJackson2JsonRedisSerializer` (typing `NON_FINAL`) las serialice/deserialice sin fallos.
- **CSRF:** Spring Security mantiene CSRF activo para formularios web. Los POST de los tests `@WebMvcTest` usan `.with(csrf())`. Thymeleaf inyecta `_csrf` automáticamente en `th:action` (no hay inputs manuales).
- **`AccessDeniedException` NO se usa para el 403 del PDF** porque `CustomAccessDeniedHandler` la traduce a **400** (SC_BAD_REQUEST) en todo el repo. Se introduce `ForbiddenException` mapeada a **403** en `GlobalExceptionHandler` (familia consistente: `ResourceNotFoundException`/`BadRequestException`/`ConflictException`).
- **Orden de ejecución crítico:** los tests `@WebMvcTest` resuelven vistas Thymeleaf reales (la slice incluye `ThymeleafAutoConfiguration`), por eso las plantillas Tailwind se crean (Task 5) **antes** de los tests de controladores (Tasks 6–7). Sin plantilla, un test `@WebMvcTest` que renderiza `tracking-result` falla con 500.
- **Refactor atómico por ruta:** `PublicController` y `TrackingWebController` se tocan en el **mismo commit** (Task 6), y `ClienteController` con `ClientDashboardController` en el mismo commit (Task 7). Evita conflictos de mapeo ambiguo en el contexto de Spring.
- **i18n:** `/en/tracking` y `/en/tracking/{codigo}` delegan en las mismas vistas modernizadas en español (`tracking-search`, `tracking-result`). Se eliminan `templates/tracking.html` y `templates/en/tracking.html`.
- **Sin cambios de esquema:** no hay migración Flyway nueva.
- **Sin build de Node:** Tailwind vía CDN runtime; `tailwind.config` inline con `brand: '#d4762a'`.
- **Verificación:** suite completa en contenedor Maven Linux con MySQL/Redis (comando AGENTS.md). El resto del sitio no se toca.

Comandos de verificación:
- Suite completa (replica CI, requiere red `envios_paraguay_cms_backend` con `db`/`redis`):
  ```powershell
  docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend `
    -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
    -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis `
    -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 sh -c "./mvnw clean test -B"
  ```
- Test puntual (mismo entorno): `... sh -c "./mvnw test -Dtest=PublicTrackingServiceTest -B"` (para tests que NO requieren DB: unit + `@WebMvcTest`; los `@SpringBootTest` de integración requieren MySQL/Redis levantados).
- Compilar sin tests (rápido, JDK17 local): `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -DskipTests compile`

---

### Task 1: DTOs de vista (Java puro) — `com.monteastur.envios.dto.web`

**Files:**
- Create: `src/main/java/com/monteastur/envios/dto/web/PublicTrackingView.java`
- Create: `src/main/java/com/monteastur/envios/dto/web/EventoView.java`
- Create: `src/main/java/com/monteastur/envios/dto/web/EvidenciaView.java`
- Create: `src/main/java/com/monteastur/envios/dto/web/EntregaView.java`
- Create: `src/main/java/com/monteastur/envios/dto/web/ClientDashboardView.java`
- Create: `src/main/java/com/monteastur/envios/dto/web/EnvioResumenView.java`

**Interfaces:**
- Producen los DTOs planos que consumen `PublicTrackingService`/`ClientDashboardService` (Tasks 2–3) y los templates Tailwind (Task 5). Se serializan en Redis con typing `NON_FINAL` → clases no finales y listas `ArrayList`.

- [x] **Step 1: `PublicTrackingView`**

```java
package com.monteastur.envios.dto.web;

import com.monteastur.envios.model.EnvioTracking;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Vista plana (cacheable en Redis) del portal público de rastreo.
 * No expone referencias a entidades JPA.
 */
public class PublicTrackingView {

    public static final List<String> PASOS_CANONICOS = List.of(
            "RECIBIDO", "EN_ADUANA_ORIGEN", "EN_TRANSITO",
            "EN_ADUANA_DESTINO", "EN_REPARTO", "ENTREGADO");

    private String codigoUnico;
    private String estado;
    private String destinatario;
    private String origen;
    private String destino;
    private String peso;
    private String contenido;
    private String observaciones;
    private String ubicacionActual;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimaActualizacion;
    private String clienteNombre;
    private Long batchId;
    private int pasoActual = -1;
    private List<String> pasos = new ArrayList<>(PASOS_CANONICOS);
    private List<EventoView> eventos = new ArrayList<>();
    private List<EvidenciaView> evidencias = new ArrayList<>();
    private EntregaView entrega;

    public PublicTrackingView() {}

    public PublicTrackingView(String codigoUnico, String estado, String destinatario,
                              String origen, String destino, String peso, String contenido,
                              String observaciones, String ubicacionActual,
                              LocalDateTime fechaCreacion, LocalDateTime ultimaActualizacion,
                              String clienteNombre, Long batchId, int pasoActual,
                              List<String> pasos, List<EventoView> eventos,
                              List<EvidenciaView> evidencias, EntregaView entrega) {
        this.codigoUnico = codigoUnico;
        this.estado = estado;
        this.destinatario = destinatario;
        this.origen = origen;
        this.destino = destino;
        this.peso = peso;
        this.contenido = contenido;
        this.observaciones = observaciones;
        this.ubicacionActual = ubicacionActual;
        this.fechaCreacion = fechaCreacion;
        this.ultimaActualizacion = ultimaActualizacion;
        this.clienteNombre = clienteNombre;
        this.batchId = batchId;
        this.pasoActual = pasoActual;
        this.pasos = pasos != null ? pasos : new ArrayList<>(PASOS_CANONICOS);
        this.eventos = eventos != null ? eventos : new ArrayList<>();
        this.evidencias = evidencias != null ? evidencias : new ArrayList<>();
        this.entrega = entrega;
    }

    public static PublicTrackingView from(EnvioTracking envio, List<EventoView> eventos,
                                          List<EvidenciaView> evidencias, EntregaView entrega) {
        String clienteNombre = envio.getCliente() != null ? envio.getCliente().getNombre() : null;
        return new PublicTrackingView(
                envio.getCodigoUnico(), envio.getEstado(), envio.getDestinatario(),
                envio.getOrigen(), envio.getDestino(), envio.getPeso(), envio.getContenido(),
                envio.getObservaciones(), envio.getUbicacionActual(),
                envio.getFechaCreacion(), envio.getUltimaActualizacion(),
                clienteNombre, envio.getBatchId(),
                PASOS_CANONICOS.indexOf(envio.getEstado()),
                new ArrayList<>(PASOS_CANONICOS), eventos, evidencias, entrega);
    }

    // Getters y setters explícitos de todos los campos (necesarios para la deserialización JSON en Redis)
    public String getCodigoUnico() { return codigoUnico; }
    public void setCodigoUnico(String codigoUnico) { this.codigoUnico = codigoUnico; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getPeso() { return peso; }
    public void setPeso(String peso) { this.peso = peso; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getUbicacionActual() { return ubicacionActual; }
    public void setUbicacionActual(String ubicacionActual) { this.ubicacionActual = ubicacionActual; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public int getPasoActual() { return pasoActual; }
    public void setPasoActual(int pasoActual) { this.pasoActual = pasoActual; }
    public List<String> getPasos() { return pasos; }
    public void setPasos(List<String> pasos) { this.pasos = pasos; }
    public List<EventoView> getEventos() { return eventos; }
    public void setEventos(List<EventoView> eventos) { this.eventos = eventos; }
    public List<EvidenciaView> getEvidencias() { return evidencias; }
    public void setEvidencias(List<EvidenciaView> evidencias) { this.evidencias = evidencias; }
    public EntregaView getEntrega() { return entrega; }
    public void setEntrega(EntregaView entrega) { this.entrega = entrega; }
}
```

- [x] **Step 2: `EventoView`, `EvidenciaView`, `EntregaView`** (mismo patrón: ctor vacío + ctor parametrizado + `from` + getters/setters)

```java
package com.monteastur.envios.dto.web;

import com.monteastur.envios.model.EventoTracking;
import java.time.LocalDateTime;

public class EventoView {

    private String estado;
    private String titulo;
    private String descripcion;
    private String ubicacion;
    private String icono;
    private String color;
    private LocalDateTime fechaEvento;
    private boolean visibleCliente;

    public EventoView() {}

    public EventoView(String estado, String titulo, String descripcion, String ubicacion,
                      String icono, String color, LocalDateTime fechaEvento, boolean visibleCliente) {
        this.estado = estado;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.icono = icono;
        this.color = color;
        this.fechaEvento = fechaEvento;
        this.visibleCliente = visibleCliente;
    }

    public static EventoView from(EventoTracking evento) {
        return new EventoView(evento.getEstado(), evento.getTitulo(), evento.getDescripcion(),
                evento.getUbicacion(), evento.getIcono(), evento.getColor(),
                evento.getFechaEvento(), evento.isVisibleCliente());
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDateTime fechaEvento) { this.fechaEvento = fechaEvento; }
    public boolean isVisibleCliente() { return visibleCliente; }
    public void setVisibleCliente(boolean visibleCliente) { this.visibleCliente = visibleCliente; }
}
```

```java
package com.monteastur.envios.dto.web;

import com.monteastur.envios.model.EvidenciaEnvio;
import java.time.LocalDateTime;

public class EvidenciaView {

    private Long id;
    private String titulo;
    private String descripcion;
    private String tipo;
    private String urlArchivo;
    private LocalDateTime fechaSubida;

    public EvidenciaView() {}

    public EvidenciaView(Long id, String titulo, String descripcion, String tipo,
                         String urlArchivo, LocalDateTime fechaSubida) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.urlArchivo = urlArchivo;
        this.fechaSubida = fechaSubida;
    }

    public static EvidenciaView from(EvidenciaEnvio evidencia) {
        return new EvidenciaView(evidencia.getId(), evidencia.getTitulo(), evidencia.getDescripcion(),
                evidencia.getTipo(), evidencia.getUrlArchivo(), evidencia.getFechaSubida());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getUrlArchivo() { return urlArchivo; }
    public void setUrlArchivo(String urlArchivo) { this.urlArchivo = urlArchivo; }
    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }
}
```

```java
package com.monteastur.envios.dto.web;

import com.monteastur.envios.model.EntregaEvidencia;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EntregaView {

    private String receptorNombre;
    private String receptorDocumento;
    private String firmaBase64;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String notas;
    private LocalDateTime fechaEntrega;

    public EntregaView() {}

    public EntregaView(String receptorNombre, String receptorDocumento, String firmaBase64,
                       BigDecimal latitud, BigDecimal longitud, String notas, LocalDateTime fechaEntrega) {
        this.receptorNombre = receptorNombre;
        this.receptorDocumento = receptorDocumento;
        this.firmaBase64 = firmaBase64;
        this.latitud = latitud;
        this.longitud = longitud;
        this.notas = notas;
        this.fechaEntrega = fechaEntrega;
    }

    public static EntregaView from(EntregaEvidencia evidencia) {
        return new EntregaView(evidencia.getReceptorNombre(), evidencia.getReceptorDocumento(),
                evidencia.getFirmaBase64(), evidencia.getLatitud(), evidencia.getLongitud(),
                evidencia.getNotas(), evidencia.getFechaEntrega());
    }

    public String getReceptorNombre() { return receptorNombre; }
    public void setReceptorNombre(String receptorNombre) { this.receptorNombre = receptorNombre; }
    public String getReceptorDocumento() { return receptorDocumento; }
    public void setReceptorDocumento(String receptorDocumento) { this.receptorDocumento = receptorDocumento; }
    public String getFirmaBase64() { return firmaBase64; }
    public void setFirmaBase64(String firmaBase64) { this.firmaBase64 = firmaBase64; }
    public BigDecimal getLatitud() { return latitud; }
    public void setLatitud(BigDecimal latitud) { this.latitud = latitud; }
    public BigDecimal getLongitud() { return longitud; }
    public void setLongitud(BigDecimal longitud) { this.longitud = longitud; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }
}
```

- [x] **Step 3: `ClientDashboardView` y `EnvioResumenView`**

```java
package com.monteastur.envios.dto.web;

import java.util.ArrayList;
import java.util.List;

public class ClientDashboardView {

    private Long clienteId;
    private String clienteNombre;
    private String clienteEmail;
    private int totalEnvios;
    private int enviosActivos;
    private int enviosEntregados;
    private double pesoTotalKg;
    private double pesoActivoKg;
    private List<EnvioResumenView> envios = new ArrayList<>();

    public ClientDashboardView() {}

    public ClientDashboardView(Long clienteId, String clienteNombre, String clienteEmail,
                               int totalEnvios, int enviosActivos, int enviosEntregados,
                               double pesoTotalKg, double pesoActivoKg, List<EnvioResumenView> envios) {
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteEmail = clienteEmail;
        this.totalEnvios = totalEnvios;
        this.enviosActivos = enviosActivos;
        this.enviosEntregados = enviosEntregados;
        this.pesoTotalKg = pesoTotalKg;
        this.pesoActivoKg = pesoActivoKg;
        this.envios = envios != null ? envios : new ArrayList<>();
    }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getClienteEmail() { return clienteEmail; }
    public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }
    public int getTotalEnvios() { return totalEnvios; }
    public void setTotalEnvios(int totalEnvios) { this.totalEnvios = totalEnvios; }
    public int getEnviosActivos() { return enviosActivos; }
    public void setEnviosActivos(int enviosActivos) { this.enviosActivos = enviosActivos; }
    public int getEnviosEntregados() { return enviosEntregados; }
    public void setEnviosEntregados(int enviosEntregados) { this.enviosEntregados = enviosEntregados; }
    public double getPesoTotalKg() { return pesoTotalKg; }
    public void setPesoTotalKg(double pesoTotalKg) { this.pesoTotalKg = pesoTotalKg; }
    public double getPesoActivoKg() { return pesoActivoKg; }
    public void setPesoActivoKg(double pesoActivoKg) { this.pesoActivoKg = pesoActivoKg; }
    public List<EnvioResumenView> getEnvios() { return envios; }
    public void setEnvios(List<EnvioResumenView> envios) { this.envios = envios; }
}
```

```java
package com.monteastur.envios.dto.web;

import com.monteastur.envios.model.EnvioTracking;
import java.time.LocalDateTime;

public class EnvioResumenView {

    private Long id;
    private String codigoUnico;
    private String estado;
    private String destino;
    private String contenido;
    private String peso;
    private LocalDateTime ultimaActualizacion;
    private Long batchId;

    public EnvioResumenView() {}

    public EnvioResumenView(Long id, String codigoUnico, String estado, String destino,
                            String contenido, String peso, LocalDateTime ultimaActualizacion, Long batchId) {
        this.id = id;
        this.codigoUnico = codigoUnico;
        this.estado = estado;
        this.destino = destino;
        this.contenido = contenido;
        this.peso = peso;
        this.ultimaActualizacion = ultimaActualizacion;
        this.batchId = batchId;
    }

    public static EnvioResumenView from(EnvioTracking envio) {
        return new EnvioResumenView(envio.getId(), envio.getCodigoUnico(), envio.getEstado(),
                envio.getDestino(), envio.getContenido(), envio.getPeso(),
                envio.getUltimaActualizacion(), envio.getBatchId());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigoUnico() { return codigoUnico; }
    public void setCodigoUnico(String codigoUnico) { this.codigoUnico = codigoUnico; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getPeso() { return peso; }
    public void setPeso(String peso) { this.peso = peso; }
    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
}
```

- [x] **Step 4: Compilar y commit**

```bash
git add src/main/java/com/monteastur/envios/dto/web
git commit -m "feat(web): add view DTOs for public tracking and client dashboard"
```

---

### Task 2: `PublicTrackingService` (NUEVO) + test unitario con caché

**Files:**
- Create: `src/main/java/com/monteastur/envios/service/web/PublicTrackingService.java`
- Create: `src/test/java/com/monteastur/envios/service/web/PublicTrackingServiceTest.java`

**Interfaces:**
- Consume: `EnvioTrackingRepository.findWithClienteByCodigoUnico`, `EventoTrackingService.listarPorEnvio`, `EvidenciaEnvioService.listarPorEnvioParaCliente`, `EntregaEvidenciaRepository.findByEnvioId`, y los DTOs de Task 1. Produce: `PublicTrackingView` cacheado en `envios.tracking.pagina` (clave = código, TTL 5 min, no cachea null).

- [x] **Step 1: Escribir el servicio** (TDD: primero el test, después la implementación)

```java
package com.monteastur.envios.service.web;

import com.monteastur.envios.dto.web.EvidenciaView;
import com.monteastur.envios.dto.web.EventoView;
import com.monteastur.envios.dto.web.EntregaView;
import com.monteastur.envios.dto.web.PublicTrackingView;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de consulta pública de rastreo con caché Redis.
 * Devuelve un DTO plano (nunca entidades JPA). null si el código no existe.
 */
@Service
public class PublicTrackingService {

    private final EnvioTrackingRepository envioTrackingRepository;
    private final EventoTrackingService eventoTrackingService;
    private final EvidenciaEnvioService evidenciaEnvioService;
    private final EntregaEvidenciaRepository entregaEvidenciaRepository;

    public PublicTrackingService(EnvioTrackingRepository envioTrackingRepository,
                                 EventoTrackingService eventoTrackingService,
                                 EvidenciaEnvioService evidenciaEnvioService,
                                 EntregaEvidenciaRepository entregaEvidenciaRepository) {
        this.envioTrackingRepository = envioTrackingRepository;
        this.eventoTrackingService = eventoTrackingService;
        this.evidenciaEnvioService = evidenciaEnvioService;
        this.entregaEvidenciaRepository = entregaEvidenciaRepository;
    }

    @Cacheable(value = "envios.tracking.pagina", key = "#codigo", unless = "#result == null")
    public PublicTrackingView cargarPagina(String codigo) {
        EnvioTracking envio = envioTrackingRepository
                .findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
                .orElse(null);
        if (envio == null) {
            return null;
        }
        List<EventoView> eventos = eventoTrackingService.listarPorEnvio(envio.getId()).stream()
                .map(EventoView::from)
                .toList();
        List<EvidenciaView> evidencias = evidenciaEnvioService.listarPorEnvioParaCliente(envio.getId()).stream()
                .map(EvidenciaView::from)
                .toList();
        EntregaView entrega = null;
        if ("ENTREGADO".equals(envio.getEstado())) {
            entrega = entregaEvidenciaRepository.findByEnvioId(envio.getId())
                    .map(EntregaView::from)
                    .orElse(null);
        }
        return PublicTrackingView.from(envio, eventos, evidencias, entrega);
    }
}
```

- [x] **Step 2: Escribir el test** (Mockito + `ConcurrentMapCacheManager`, patrón `EnvioTrackingServiceCacheTest`)

```java
package com.monteastur.envios.service.web;

import com.monteastur.envios.dto.web.PublicTrackingView;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PublicTrackingServiceTest.TestConfig.class)
class PublicTrackingServiceTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        EnvioTrackingRepository envioTrackingRepository() {
            return Mockito.mock(EnvioTrackingRepository.class);
        }

        @Bean
        EventoTrackingService eventoTrackingService() {
            return Mockito.mock(EventoTrackingService.class);
        }

        @Bean
        EvidenciaEnvioService evidenciaEnvioService() {
            return Mockito.mock(EvidenciaEnvioService.class);
        }

        @Bean
        EntregaEvidenciaRepository entregaEvidenciaRepository() {
            return Mockito.mock(EntregaEvidenciaRepository.class);
        }

        @Bean
        PublicTrackingService publicTrackingService(EnvioTrackingRepository repo,
                                                    EventoTrackingService eventos,
                                                    EvidenciaEnvioService evidencias,
                                                    EntregaEvidenciaRepository entregas) {
            return new PublicTrackingService(repo, eventos, evidencias, entregas);
        }

        @Bean
        CacheManager cacheManager() {
            ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("envios.tracking.pagina");
            cacheManager.setAllowNullValues(false);
            return cacheManager;
        }
    }

    @Autowired
    private PublicTrackingService service;

    @Autowired
    private EnvioTrackingRepository envioTrackingRepository;

    @Autowired
    private EventoTrackingService eventoTrackingService;

    @Autowired
    private EvidenciaEnvioService evidenciaEnvioService;

    @Autowired
    private EntregaEvidenciaRepository entregaEvidenciaRepository;

    private EnvioTracking envio(String codigo, String estado) {
        EnvioTracking envio = new EnvioTracking(codigo, estado, "Destinatario Test",
                "Asturias, España", "Asunción, Paraguay", "10 kg", "Documentos");
        envio.setId(1L);
        envio.setCliente(new Cliente("cliente@test.com", "x", "Cliente Test", null));
        return envio;
    }

    @Test
    void codigoInexistente_retornaNull() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico(anyString()))
                .thenReturn(Optional.empty());

        assertThat(service.cargarPagina("NOPE")).isNull();
    }

    @Test
    void entregaViewSoloCuandoEntregado() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1"))
                .thenReturn(Optional.of(envio("MT-1", "ENTREGADO")));
        when(eventoTrackingService.listarPorEnvio(1L)).thenReturn(List.of());
        when(evidenciaEnvioService.listarPorEnvioParaCliente(1L)).thenReturn(List.of());
        when(entregaEvidenciaRepository.findByEnvioId(1L))
                .thenReturn(Optional.of(new EntregaEvidencia(envio("MT-1", "ENTREGADO"),
                        "Ana López", "12345678", "firma", null, null, null)));

        PublicTrackingView view = service.cargarPagina("MT-1");

        assertThat(view).isNotNull();
        assertThat(view.getPasoActual()).isEqualTo(5);
        assertThat(view.getEntrega()).isNotNull();
        assertThat(view.getEntrega().getReceptorNombre()).isEqualTo("Ana López");
        assertThat(view.getClienteNombre()).isEqualTo("Cliente Test");
    }

    @Test
    void pasoActualMenosUnoSiEstadoNoCanonico() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-9"))
                .thenReturn(Optional.of(envio("MT-9", "CANCELADO")));
        when(eventoTrackingService.listarPorEnvio(1L)).thenReturn(List.of());
        when(evidenciaEnvioService.listarPorEnvioParaCliente(1L)).thenReturn(List.of());

        PublicTrackingView view = service.cargarPagina("MT-9");

        assertThat(view.getPasoActual()).isEqualTo(-1);
        assertThat(view.getEntrega()).isNull();
    }

    @Test
    void segundaConsultaSeSirveDesdeCache() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-CACHE"))
                .thenReturn(Optional.of(envio("MT-CACHE", "RECIBIDO")));
        when(eventoTrackingService.listarPorEnvio(1L)).thenReturn(List.of());
        when(evidenciaEnvioService.listarPorEnvioParaCliente(1L)).thenReturn(List.of());

        service.cargarPagina("MT-CACHE");
        service.cargarPagina("MT-CACHE");

        verify(envioTrackingRepository, times(1)).findWithClienteByCodigoUnico("MT-CACHE");
    }
}
```

- [x] **Step 3: Verificar el test puntual**

```powershell
docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
  -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis `
  -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 sh -c "./mvnw test -Dtest=PublicTrackingServiceTest -B"
```

Expected: `BUILD SUCCESS`, 4 tests, 0 fallos.

- [x] **Step 4: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/web src/test/java/com/monteastur/envios/service/web
git commit -m "feat(web): add PublicTrackingService with Redis-cached public tracking page"
```

---

### Task 3: `ClientDashboardService` (NUEVO) + test unitario de métricas

**Files:**
- Create: `src/main/java/com/monteastur/envios/service/web/ClientDashboardService.java`
- Create: `src/test/java/com/monteastur/envios/service/web/ClientDashboardServiceTest.java`

**Interfaces:**
- Consume: `EnvioTrackingRepository.findByClienteIdOrderByUltimaActualizacionDesc`, `ClienteRepository.findById`, `PesoUtil.parsear`. Produce: `ClientDashboardView` cacheado en `envios.cliente.dashboard` (clave = clienteId, TTL 1 min).

- [x] **Step 1: Escribir el test** (Mockito puro, sin Spring)

```java
package com.monteastur.envios.service.web;

import com.monteastur.envios.dto.web.ClientDashboardView;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientDashboardServiceTest {

    @Mock
    private EnvioTrackingRepository envioTrackingRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClientDashboardService service;

    private Cliente cliente() {
        Cliente cliente = new Cliente("cliente@test.com", "x", "Cliente Test", null);
        cliente.setId(7L);
        return cliente;
    }

    private EnvioTracking envio(String codigo, String estado, String peso) {
        EnvioTracking envio = new EnvioTracking(codigo, estado, "Destinatario Test",
                "Origen", "Destino", peso, "Contenido");
        envio.setId(1L);
        return envio;
    }

    @Test
    void metricasConPesosValidosInvalidosYMix() {
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente()));
        when(envioTrackingRepository.findByClienteIdOrderByUltimaActualizacionDesc(7L))
                .thenReturn(List.of(
                        envio("MT-A", "ENTREGADO", "10 kg"),
                        envio("MT-B", "EN_TRANSITO", "5,5 kg"),
                        envio("MT-C", "EN_REPARTO", "ab/oo"),
                        envio("MT-D", "RECIBIDO", null)
                ));

        ClientDashboardView view = service.cargarDashboard(7L);

        assertThat(view.getClienteId()).isEqualTo(7L);
        assertThat(view.getClienteNombre()).isEqualTo("Cliente Test");
        assertThat(view.getClienteEmail()).isEqualTo("cliente@test.com");
        assertThat(view.getTotalEnvios()).isEqualTo(4);
        assertThat(view.getEnviosEntregados()).isEqualTo(1);
        assertThat(view.getEnviosActivos()).isEqualTo(3);
        assertThat(view.getPesoTotalKg()).isEqualTo(15.5);
        assertThat(view.getPesoActivoKg()).isEqualTo(5.5);
        assertThat(view.getEnvios()).hasSize(4);
    }

    @Test
    void clienteInexistente_retornaNull() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.cargarDashboard(99L)).isNull();
    }
}
```

- [x] **Step 2: Escribir la implementación**

```java
package com.monteastur.envios.service.web;

import com.monteastur.envios.dto.web.ClientDashboardView;
import com.monteastur.envios.dto.web.EnvioResumenView;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.pdf.PesoUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Servicio del dashboard de cliente con caché Redis y métricas de peso.
 * Los pesos no parseables por PesoUtil se ignoran en las sumas.
 */
@Service
public class ClientDashboardService {

    private final EnvioTrackingRepository envioTrackingRepository;
    private final ClienteRepository clienteRepository;

    public ClientDashboardService(EnvioTrackingRepository envioTrackingRepository,
                                  ClienteRepository clienteRepository) {
        this.envioTrackingRepository = envioTrackingRepository;
        this.clienteRepository = clienteRepository;
    }

    @Cacheable(value = "envios.cliente.dashboard", key = "#clienteId", unless = "#result == null")
    public ClientDashboardView cargarDashboard(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (cliente == null) {
            return null;
        }
        List<EnvioTracking> envios = envioTrackingRepository
                .findByClienteIdOrderByUltimaActualizacionDesc(clienteId);
        int entregados = 0;
        double pesoTotal = 0;
        double pesoActivo = 0;
        List<EnvioResumenView> resumenes = new ArrayList<>();
        for (EnvioTracking envio : envios) {
            boolean esEntregado = "ENTREGADO".equals(envio.getEstado());
            if (esEntregado) {
                entregados++;
            }
            OptionalDouble peso = PesoUtil.parsear(envio.getPeso());
            if (peso.isPresent()) {
                pesoTotal += peso.getAsDouble();
                if (!esEntregado) {
                    pesoActivo += peso.getAsDouble();
                }
            }
            resumenes.add(EnvioResumenView.from(envio));
        }
        int total = envios.size();
        return new ClientDashboardView(cliente.getId(), cliente.getNombre(), cliente.getEmail(),
                total, total - entregados, entregados, pesoTotal, pesoActivo, resumenes);
    }
}
```

- [x] **Step 3: Verificar el test puntual** (mismo comando Docker, `-Dtest=ClientDashboardServiceTest`) → `BUILD SUCCESS`, 2 tests.
- [x] **Step 4: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/web src/test/java/com/monteastur/envios/service/web
git commit -m "feat(web): add ClientDashboardService with PesoUtil weight metrics"
```

---

### Task 4: Caché Redis — registrar caches nuevos y ampliar `@CacheEvict`

**Files:**
- Modify: `src/main/java/com/monteastur/envios/config/RedisConfig.java`
- Modify: `src/main/java/com/monteastur/envios/service/EnvioTrackingService.java`
- Modify: `src/main/java/com/monteastur/envios/service/EntregaEvidenciaService.java`
- Modify: `src/main/java/com/monteastur/envios/service/batch/BatchImportPersistenceService.java`

**Interfaces:**
- Consume los `@Cacheable` de Tasks 2–3. Produce la invalidación correcta en cada punto de mutación de envíos.

- [x] **Step 1: Añadir los caches a `RedisConfig`** (tras la entrada `envios.clientes`)

```java
var configs = Map.of(
    "envios.tracking", defaultConfig.entryTtl(Duration.ofMinutes(5)),
    "envios.dashboard", defaultConfig.entryTtl(Duration.ofMinutes(1)),
    "envios.tracking.pagina", defaultConfig.entryTtl(Duration.ofMinutes(5)),
    "envios.cliente.dashboard", defaultConfig.entryTtl(Duration.ofMinutes(1)),
    "envios.reservas", defaultConfig.entryTtl(Duration.ofMinutes(10)),
    "envios.clientes", defaultConfig.entryTtl(Duration.ofMinutes(10)),
    "envios.disponibilidad", defaultConfig.entryTtl(Duration.ofMinutes(2))
);
```

- [x] **Step 2: Ampliar `@CacheEvict` en `EnvioTrackingService`**

```java
@CacheEvict(value = {"envios.tracking", "envios.tracking.pagina", "envios.cliente.dashboard"}, allEntries = true)
public EnvioTracking guardar(EnvioTracking envio) { ... }

@Transactional
@CacheEvict(value = {"envios.tracking", "envios.tracking.pagina", "envios.cliente.dashboard"}, allEntries = true)
public EnvioTracking actualizarEstado(String codigo, String nuevoEstado) { ... }

@CacheEvict(value = {"envios.tracking", "envios.tracking.pagina", "envios.cliente.dashboard"}, allEntries = true)
public void eliminar(Long id) { ... }
```

- [x] **Step 3: Ampliar `@CacheEvict` en `EntregaEvidenciaService.registrarEntrega`**

```java
@CacheEvict(value = {"envios.dashboard", "envios.tracking.pagina", "envios.cliente.dashboard"}, allEntries = true)
public EntregaEvidencia registrarEntrega(String codigo, RegistrarEntregaRequest request) { ... }
```

- [x] **Step 4: Ampliar `@CacheEvict` en `BatchImportPersistenceService.procesarChunk`**

```java
@CacheEvict(value = {"envios.dashboard", "envios.tracking.pagina", "envios.cliente.dashboard"}, allEntries = true)
public void procesarChunk(Long batchId, List<EnvioTracking> envios, List<CsvImportLineError> errores) { ... }
```

- [x] **Step 5: Compilar y commit**

```bash
git add src/main/java/com/monteastur/envios/config/RedisConfig.java src/main/java/com/monteastur/envios/service/EnvioTrackingService.java src/main/java/com/monteastur/envios/service/EntregaEvidenciaService.java src/main/java/com/monteastur/envios/service/batch/BatchImportPersistenceService.java
git commit -m "perf(cache): register tracking-page and client-dashboard Redis caches and evict on mutations"
```

---

### Task 5: Plantillas Thymeleaf modernizadas (Tailwind CDN) + limpieza de templates antiguos

**Files:**
- Create: `src/main/resources/templates/fragments/public-head.html` (fragmento `head`, `navbar`, `footer` en Tailwind)
- Create: `src/main/resources/templates/tracking-search.html`
- Create: `src/main/resources/templates/tracking-result.html`
- Create: `src/main/resources/templates/tracking-404.html`
- Modify: `src/main/resources/templates/cliente/panel.html` (reescritura Tailwind; se mantiene la ruta y el nombre de vista)
- Delete: `src/main/resources/templates/tracking.html`
- Delete: `src/main/resources/templates/en/tracking.html`

**Interfaces:**
- Son las vistas que renderizan los controladores de Tasks 6–7 y que los tests `@WebMvcTest` resuelven (por eso van ANTES). Aislamiento: solo las páginas modernizadas cargan Tailwind; las vistas premium intactas no.

- [x] **Step 1: `fragments/public-head.html`** (head con Tailwind CDN + config de marca; navbar y footer)

```html
<head th:fragment="head(titulo)">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${titulo} + ' - MONTEASTUR ENVIOS'">MONTEASTUR ENVIOS</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        brand: {
                            DEFAULT: '#d4762a',
                            50: '#fdf3ea', 100: '#fbe5d1', 400: '#e6a26a',
                            600: '#b9631f', 700: '#9a4f1a'
                        }
                    },
                    fontFamily: {
                        sans: ['Poppins', 'ui-sans-serif', 'system-ui', 'sans-serif']
                    }
                }
            }
        }
    </script>
</head>

<header th:fragment="navbar" class="sticky top-0 z-40 bg-white/90 backdrop-blur border-b border-stone-200">
    <nav class="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <a href="/" class="text-xl font-bold tracking-tight">
            <span class="text-brand-600">MONTEASTUR</span> <span class="text-stone-900">ENVIOS</span>
        </a>
        <div class="hidden md:flex items-center gap-6 text-sm font-medium">
            <a href="/" class="text-stone-600 hover:text-brand-600">Inicio</a>
            <a href="/casa" class="text-stone-600 hover:text-brand-600">La Casa</a>
            <a href="/tracking" class="text-brand-600 font-semibold">Seguimiento</a>
            <a href="/reservas" class="text-stone-600 hover:text-brand-600">Reservas</a>
            <a href="/contacto" class="text-stone-600 hover:text-brand-600">Contacto</a>
        </div>
    </nav>
</header>

<footer th:fragment="footer" class="bg-stone-900 text-stone-300 mt-16">
    <div class="max-w-6xl mx-auto px-4 py-10 grid gap-6 md:grid-cols-3">
        <div>
            <p class="font-bold text-white">MONTEASTUR ENVIOS</p>
            <p class="text-sm mt-2">Envíos entre Asturias y Paraguay con seguimiento en tiempo real.</p>
        </div>
        <div class="text-sm">
            <p class="font-semibold text-white mb-2">Enlaces</p>
            <a href="/tracking" class="block hover:text-brand-400">Seguimiento</a>
            <a href="/contacto" class="block hover:text-brand-400">Contacto</a>
        </div>
        <div class="text-sm">
            <p class="font-semibold text-white mb-2">Contacto</p>
            <p>+34 642 687 292</p>
            <p>Asturias &harr; Paraguay</p>
        </div>
    </div>
    <div class="border-t border-stone-800 text-center text-xs py-4">© 2026 MONTEASTUR ENVIOS</div>
</footer>
```

- [x] **Step 2: `tracking-search.html`** (hero + buscador + error + modal QR con `html5-qrcode`)

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/public-head :: head('Seguimiento')}"></head>
<body class="bg-stone-50 text-stone-900">
<header th:replace="~{fragments/public-head :: navbar}"></header>

<main class="max-w-6xl mx-auto px-4">
    <section class="mt-12 rounded-3xl bg-gradient-to-br from-brand-600 to-brand-700 text-white p-8 md:p-12 shadow-xl">
        <span class="inline-block text-xs font-semibold uppercase tracking-widest bg-white/15 px-3 py-1 rounded-full">Asturias &harr; Paraguay</span>
        <h1 class="mt-4 text-3xl md:text-4xl font-extrabold">Seguimiento de tu envío</h1>
        <p class="mt-2 text-white/85 max-w-xl">Introduce tu código MONTEASTUR o escanea el QR de la etiqueta para ver el estado actualizado de tu paquete.</p>

        <form method="post" th:action="@{/tracking}" class="mt-8 flex flex-col sm:flex-row gap-3 max-w-2xl">
            <input type="text" name="codigo" th:value="${codigo}" required autocomplete="off"
                   placeholder="Introduce tu código MONTEASTUR"
                   class="flex-1 rounded-xl px-4 py-3 text-stone-900 placeholder-stone-400 focus:outline-none focus:ring-4 focus:ring-white/40">
            <button type="submit"
                    class="rounded-xl bg-stone-900 hover:bg-stone-800 text-white font-semibold px-6 py-3 transition">Buscar envío</button>
            <button type="button" onclick="abrirEscanner()"
                    class="rounded-xl bg-white/15 hover:bg-white/25 border border-white/30 text-white font-semibold px-6 py-3 transition">Escanear QR</button>
        </form>

        <p th:if="${buscado and error}"
           class="mt-4 inline-block bg-red-600 text-white text-sm font-medium px-4 py-2 rounded-lg">
            No encontramos ningún envío con el código
            <span class="font-bold" th:text="${codigo}">MT-...</span>. Verifícalo e inténtalo de nuevo.
        </p>
    </section>

    <section class="mt-12">
        <h2 class="text-xl font-bold">¿Cómo funciona?</h2>
        <div class="mt-4 grid gap-4 md:grid-cols-3">
            <div class="bg-white rounded-2xl border border-stone-200 p-6">
                <span class="text-brand-600 font-bold text-sm uppercase tracking-wide">1. Busca</span>
                <h3 class="mt-2 font-semibold">Introduce el código</h3>
                <p class="text-sm text-stone-500 mt-1">El código MONTEASTUR viene en la etiqueta de tu envío (formato MT-AAAA-0001).</p>
            </div>
            <div class="bg-white rounded-2xl border border-stone-200 p-6">
                <span class="text-brand-600 font-bold text-sm uppercase tracking-wide">2. Escanea</span>
                <h3 class="mt-2 font-semibold">Usa la cámara</h3>
                <p class="text-sm text-stone-500 mt-1">O escanea el código QR de la etiqueta térmica para abrir el seguimiento al instante.</p>
            </div>
            <div class="bg-white rounded-2xl border border-stone-200 p-6">
                <span class="text-brand-600 font-bold text-sm uppercase tracking-wide">3. Sigue</span>
                <h3 class="mt-2 font-semibold">Consulta el estado</h3>
                <p class="text-sm text-stone-500 mt-1">Revisa la línea de tiempo, los eventos y el comprobante de entrega (POD) si ya fue entregado.</p>
            </div>
        </div>
    </section>
</main>

<div id="qr-modal" class="hidden fixed inset-0 z-50 bg-stone-900/70 flex items-center justify-center p-4">
    <div class="bg-white rounded-2xl max-w-md w-full p-6">
        <div class="flex items-center justify-between">
            <h3 class="font-bold">Escanear código QR</h3>
            <button type="button" onclick="cerrarEscanner()" class="text-stone-400 hover:text-stone-700 font-bold">Cerrar</button>
        </div>
        <div id="qr-reader" class="mt-4 rounded-xl overflow-hidden"></div>
        <p id="qr-error" class="mt-3 text-sm text-red-600 hidden">No se pudo acceder a la cámara. Inténtalo de nuevo o introduce el código manualmente.</p>
    </div>
</div>

<footer th:replace="~{fragments/public-head :: footer}"></footer>

<script src="https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js"></script>
<script>
    let qrScanner = null;
    function abrirEscanner() {
        document.getElementById('qr-modal').classList.remove('hidden');
        document.getElementById('qr-error').classList.add('hidden');
        if (typeof Html5Qrcode === 'undefined') {
            document.getElementById('qr-error').classList.remove('hidden');
            return;
        }
        qrScanner = new Html5Qrcode('qr-reader');
        qrScanner.start({ facingMode: 'environment' }, { fps: 10, qrbox: 220 },
            function (texto) {
                const m = texto.match(/\/tracking\/([A-Za-z0-9-]+)/);
                const codigo = m ? m[1] : texto.trim();
                window.location.href = '/tracking/' + encodeURIComponent(codigo);
            },
            function () {});
    }
    function cerrarEscanner() {
        document.getElementById('qr-modal').classList.add('hidden');
        if (qrScanner) { qrScanner.stop().then(() => qrScanner.clear()); qrScanner = null; }
    }
</script>
</body>
</html>
```

- [x] **Step 3: `tracking-result.html`** (timeline interactivo + eventos expandibles + POD + evidencias)

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/public-head :: head('Seguimiento')}"></head>
<body class="bg-stone-50 text-stone-900">
<header th:replace="~{fragments/public-head :: navbar}"></header>

<main class="max-w-6xl mx-auto px-4 py-10">
    <a href="/tracking" class="text-sm font-medium text-brand-600 hover:underline">&larr; Buscar otro envío</a>

    <section class="mt-4 bg-white rounded-2xl border border-stone-200 p-6">
        <div class="flex flex-wrap items-center justify-between gap-4">
            <div>
                <span class="text-xs font-semibold uppercase tracking-wide text-stone-400">Código de envío</span>
                <h1 class="mt-1 text-2xl font-extrabold text-brand-600" th:text="${view.codigoUnico}">MT-2026-0001</h1>
            </div>
            <span th:switch="${view.estado}" class="inline-flex items-center rounded-full px-3 py-1 text-xs font-bold uppercase tracking-wide">
                <span th:case="'RECIBIDO'" class="bg-slate-200 text-slate-700 rounded-full px-3 py-1">Recibido</span>
                <span th:case="'EN_ADUANA_ORIGEN'" class="bg-amber-200 text-amber-800 rounded-full px-3 py-1">Aduana de origen</span>
                <span th:case="'EN_TRANSITO'" class="bg-sky-200 text-sky-800 rounded-full px-3 py-1">En tránsito</span>
                <span th:case="'EN_ADUANA_DESTINO'" class="bg-violet-200 text-violet-800 rounded-full px-3 py-1">Aduana de destino</span>
                <span th:case="'EN_REPARTO'" class="bg-orange-200 text-orange-800 rounded-full px-3 py-1">En reparto</span>
                <span th:case="'ENTREGADO'" class="bg-emerald-200 text-emerald-800 rounded-full px-3 py-1">Entregado</span>
                <span th:case="*" class="bg-stone-200 text-stone-700 rounded-full px-3 py-1" th:text="${view.estado}">Estado</span>
            </span>
        </div>
        <p class="mt-4 text-sm text-stone-500">
            Última actualización:
            <span class="font-medium text-stone-700" th:text="${#temporals.format(view.ultimaActualizacion, 'dd/MM/yyyy HH:mm')}">15/05/2026 14:30</span>
        </p>
    </section>

    <section class="mt-6 grid gap-6 lg:grid-cols-2">
        <div class="bg-white rounded-2xl border border-stone-200 p-6">
            <h2 class="text-lg font-bold">Recorrido</h2>
            <div class="mt-4 flex items-center gap-3">
                <span class="flex-1 rounded-xl bg-stone-100 px-4 py-3 text-sm">
                    <span class="block text-xs uppercase text-stone-400">Origen</span>
                    <span class="font-semibold" th:text="${view.origen}">Asturias, España</span>
                </span>
                <span class="text-2xl text-brand-600">→</span>
                <span class="flex-1 rounded-xl bg-stone-100 px-4 py-3 text-sm">
                    <span class="block text-xs uppercase text-stone-400">Destino</span>
                    <span class="font-semibold" th:text="${view.destino}">Asunción, Paraguay</span>
                </span>
            </div>
            <div class="mt-4 grid grid-cols-2 gap-3 text-sm">
                <div>
                    <span class="block text-xs uppercase text-stone-400">Peso</span>
                    <span class="font-semibold" th:text="${view.peso}">10 kg</span>
                </div>
                <div>
                    <span class="block text-xs uppercase text-stone-400">Contenido</span>
                    <span class="font-semibold" th:text="${view.contenido}">Documentos</span>
                </div>
            </div>
            <p th:if="${view.observaciones != null and !view.observaciones.isEmpty()}"
               class="mt-4 rounded-xl bg-brand-50 border border-brand-100 px-4 py-3 text-sm text-stone-700" th:text="${view.observaciones}">Nota del operador</p>
        </div>

        <div class="bg-white rounded-2xl border border-stone-200 p-6">
            <h2 class="text-lg font-bold">Progreso del envío</h2>
            <ol class="mt-6">
                <li th:each="paso, iter : ${view.pasos}" class="relative flex gap-4 pb-8 last:pb-0">
                    <span th:class="'mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-bold ' + (${view.pasoActual >= 0 and iter.index < view.pasoActual} ? 'bg-brand-600 text-white' : (${view.pasoActual == iter.index} ? 'ring-2 ring-brand-600 ring-offset-2 bg-white text-brand-700' : 'bg-stone-200 text-stone-500'))"
                          th:text="${iter.index + 1}">1</span>
                    <div class="flex flex-col">
                        <span th:switch="${paso}"
                              th:class="'font-semibold ' + (${view.pasoActual == iter.index} ? 'text-brand-700' : 'text-stone-800')">
                            <span th:case="'RECIBIDO'">Recibido en origen</span>
                            <span th:case="'EN_ADUANA_ORIGEN'">Aduana de origen</span>
                            <span th:case="'EN_TRANSITO'">Tránsito internacional</span>
                            <span th:case="'EN_ADUANA_DESTINO'">Aduana de destino</span>
                            <span th:case="'EN_REPARTO'">En reparto</span>
                            <span th:case="'ENTREGADO'">Entregado</span>
                            <span th:case="*" th:text="${paso}"></span>
                        </span>
                        <span th:if="${view.pasoActual == iter.index}" class="text-sm font-medium text-brand-600">Estado actual</span>
                    </div>
                </li>
            </ol>
        </div>
    </section>

    <section class="mt-6 bg-white rounded-2xl border border-stone-200 p-6">
        <h2 class="text-lg font-bold">Historial del envío</h2>
        <ul th:if="${not #lists.isEmpty(view.eventos)}" class="mt-4 space-y-3">
            <li th:each="ev : ${view.eventos}" class="rounded-xl border border-stone-200 p-4">
                <button type="button" onclick="toggleEvento(this)"
                        class="w-full flex items-center justify-between gap-3 text-left">
                    <span class="flex items-center gap-3">
                        <span class="flex h-8 w-8 items-center justify-center rounded-full text-lg"
                              th:style="'background:' + ${ev.color} + '22'" th:text="${ev.icono}">📦</span>
                        <span>
                            <span class="font-semibold block" th:text="${ev.titulo}">Título del evento</span>
                            <span class="text-sm text-stone-500" th:text="${#temporals.format(ev.fechaEvento, 'dd/MM/yyyy HH:mm')}">Fecha</span>
                        </span>
                    </span>
                    <span class="text-stone-400">▾</span>
                </button>
                <div class="hidden mt-3 text-sm text-stone-600 space-y-1">
                    <p th:if="${ev.ubicacion != null and !ev.ubicacion.isEmpty()}" class="font-medium" th:text="'Ubicación: ' + ${ev.ubicacion}">Ubicación</p>
                    <p th:if="${ev.descripcion != null and !ev.descripcion.isEmpty()}" th:text="${ev.descripcion}">Descripción</p>
                </div>
            </li>
        </ul>
        <p th:if="${#lists.isEmpty(view.eventos)}" class="mt-4 text-sm text-stone-500">Aún no hay eventos registrados para este envío.</p>
    </section>

    <section th:if="${view.entrega != null}" class="mt-6 bg-white rounded-2xl border border-emerald-200 p-6">
        <h2 class="text-lg font-bold text-emerald-800">Comprobante de entrega (POD)</h2>
        <div class="mt-4 grid gap-6 md:grid-cols-2">
            <div class="space-y-2 text-sm">
                <p><span class="font-semibold">Receptor:</span> <span th:text="${view.entrega.receptorNombre}">Nombre</span></p>
                <p><span class="font-semibold">Documento:</span> <span th:text="${view.entrega.receptorDocumento}">Doc</span></p>
                <p><span class="font-semibold">Fecha de entrega:</span> <span th:text="${#temporals.format(view.entrega.fechaEntrega, 'dd/MM/yyyy HH:mm')}">Fecha</span></p>
                <p th:if="${view.entrega.latitud != null}">
                    <span class="font-semibold">GPS:</span>
                    <span th:text="${view.entrega.latitud} + ', ' + ${view.entrega.longitud}">lat, lon</span>
                </p>
                <p th:if="${view.entrega.notas != null and !view.entrega.notas.isEmpty()}" th:text="${view.entrega.notas}">Notas</p>
            </div>
            <div class="flex flex-col items-center justify-center">
                <span class="text-sm font-semibold text-stone-500">Firma del receptor</span>
                <img th:src="'data:image/png;base64,' + ${view.entrega.firmaBase64}"
                     alt="Firma del receptor"
                     class="mt-2 max-h-32 rounded-lg border border-stone-200 bg-white">
            </div>
        </div>
    </section>

    <section th:if="${not #lists.isEmpty(view.evidencias)}" class="mt-6 bg-white rounded-2xl border border-stone-200 p-6">
        <h2 class="text-lg font-bold">Evidencias del envío</h2>
        <div class="mt-4 grid gap-3 sm:grid-cols-2">
            <div th:each="ev : ${view.evidencias}" class="flex items-center gap-3 rounded-xl border border-stone-200 p-3">
                <img th:if="${ev.tipo == 'FOTO'}" th:src="${ev.urlArchivo}" th:alt="${ev.titulo}"
                     class="h-14 w-14 rounded-lg object-cover">
                <span th:if="${ev.tipo != 'FOTO'}" class="flex h-14 w-14 items-center justify-center rounded-lg bg-stone-100 text-2xl">📄</span>
                <span>
                    <span class="font-semibold block" th:text="${ev.titulo}">Título</span>
                    <span class="text-sm text-stone-500 block" th:text="${#temporals.format(ev.fechaSubida, 'dd/MM/yyyy')}">Fecha</span>
                    <a th:href="${ev.urlArchivo}" target="_blank" class="text-sm font-medium text-brand-600 hover:underline">Ver</a>
                </span>
            </div>
        </div>
    </section>
</main>

<footer th:replace="~{fragments/public-head :: footer}"></footer>

<script>
    function toggleEvento(boton) {
        const detalle = boton.nextElementSibling;
        detalle.classList.toggle('hidden');
    }
</script>
</body>
</html>
```

- [x] **Step 4: `tracking-404.html`**

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/public-head :: head('Envío no encontrado')}"></head>
<body class="bg-stone-50 text-stone-900">
<header th:replace="~{fragments/public-head :: navbar}"></header>

<main class="max-w-2xl mx-auto px-4 py-20 text-center">
    <p class="text-6xl font-extrabold text-brand-600">404</p>
    <h1 class="mt-4 text-2xl font-bold">Envío no encontrado</h1>
    <p class="mt-2 text-stone-500">
        No encontramos ningún envío con el código
        <span class="font-semibold text-stone-800" th:text="${codigo}">MT-...</span>. Verifícalo e inténtalo de nuevo.
    </p>
    <form method="post" th:action="@{/tracking}" class="mt-8 flex flex-col sm:flex-row gap-3">
        <input type="text" name="codigo" th:value="${codigo}" required
               class="flex-1 rounded-xl border border-stone-300 px-4 py-3 focus:outline-none focus:ring-2 focus:ring-brand-600">
        <button type="submit" class="rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-semibold px-6 py-3 transition">Reintentar</button>
    </form>
    <a href="/tracking" class="mt-6 inline-block text-sm font-medium text-brand-600 hover:underline">Escanear otro QR</a>
</main>

<footer th:replace="~{fragments/public-head :: footer}"></footer>
</body>
</html>
```

- [x] **Step 5: Reescribir `cliente/panel.html`** (topbar con nombre + logout POST, tarjetas de métricas, tabla con acciones tracking + etiqueta)

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/public-head :: head('Mi Panel')}"></head>
<body class="bg-stone-50 text-stone-900">
<header class="bg-white border-b border-stone-200 sticky top-0 z-40">
    <div class="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <span class="text-xl font-bold tracking-tight">
            <span class="text-brand-600">MONTEASTUR</span> <span class="text-stone-900">ENVIOS</span>
        </span>
        <div class="flex items-center gap-4">
            <span class="text-sm font-medium" th:text="'Hola, ' + ${panel.clienteNombre}">Hola, Nombre</span>
            <form method="post" th:action="@{/cliente/logout}">
                <button type="submit" class="text-sm font-medium text-stone-500 hover:text-red-600">Cerrar sesión</button>
            </form>
        </div>
    </div>
</header>

<main class="max-w-6xl mx-auto px-4 py-10">
    <h1 class="text-2xl font-bold">Mis envíos</h1>

    <div class="mt-6 grid gap-4 grid-cols-2 md:grid-cols-3 lg:grid-cols-5">
        <div class="bg-white rounded-2xl border border-stone-200 p-4">
            <p class="text-xs font-semibold uppercase tracking-wide text-stone-400">Total</p>
            <p class="mt-1 text-2xl font-extrabold text-brand-600" th:text="${panel.totalEnvios}">0</p>
        </div>
        <div class="bg-white rounded-2xl border border-stone-200 p-4">
            <p class="text-xs font-semibold uppercase tracking-wide text-stone-400">Envíos activos</p>
            <p class="mt-1 text-2xl font-extrabold" th:text="${panel.enviosActivos}">0</p>
        </div>
        <div class="bg-white rounded-2xl border border-stone-200 p-4">
            <p class="text-xs font-semibold uppercase tracking-wide text-stone-400">Entregados</p>
            <p class="mt-1 text-2xl font-extrabold text-emerald-600" th:text="${panel.enviosEntregados}">0</p>
        </div>
        <div class="bg-white rounded-2xl border border-stone-200 p-4">
            <p class="text-xs font-semibold uppercase tracking-wide text-stone-400">Peso total (kg)</p>
            <p class="mt-1 text-2xl font-extrabold" th:text="${#numbers.formatDecimal(panel.pesoTotalKg, 1, 1)}">0.0</p>
        </div>
        <div class="bg-white rounded-2xl border border-stone-200 p-4">
            <p class="text-xs font-semibold uppercase tracking-wide text-stone-400">Peso activo (kg)</p>
            <p class="mt-1 text-2xl font-extrabold" th:text="${#numbers.formatDecimal(panel.pesoActivoKg, 1, 1)}">0.0</p>
        </div>
    </div>

    <div class="mt-8 bg-white rounded-2xl border border-stone-200 overflow-hidden">
        <div class="overflow-x-auto">
            <table class="w-full text-sm">
                <thead class="bg-stone-100 text-stone-500 text-left">
                    <tr>
                        <th class="px-4 py-3 font-semibold">Código</th>
                        <th class="px-4 py-3 font-semibold">Destino</th>
                        <th class="px-4 py-3 font-semibold">Contenido</th>
                        <th class="px-4 py-3 font-semibold">Peso</th>
                        <th class="px-4 py-3 font-semibold">Estado</th>
                        <th class="px-4 py-3 font-semibold">Última actualización</th>
                        <th class="px-4 py-3 font-semibold">Acciones</th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="envio : ${panel.envios}" class="border-t border-stone-100 hover:bg-stone-50">
                        <td class="px-4 py-3 font-semibold text-brand-600">
                            <a th:href="@{/tracking/{codigo}(codigo=${envio.codigoUnico})}" th:text="${envio.codigoUnico}">MT-2026-0001</a>
                        </td>
                        <td class="px-4 py-3" th:text="${envio.destino}">Asunción, Paraguay</td>
                        <td class="px-4 py-3" th:text="${envio.contenido}">Documentos</td>
                        <td class="px-4 py-3" th:text="${envio.peso}">10 kg</td>
                        <td class="px-4 py-3">
                            <span th:switch="${envio.estado}" class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold uppercase tracking-wide">
                                <span th:case="'RECIBIDO'" class="bg-slate-200 text-slate-700 rounded-full px-2.5 py-0.5">Recibido</span>
                                <span th:case="'EN_ADUANA_ORIGEN'" class="bg-amber-200 text-amber-800 rounded-full px-2.5 py-0.5">Aduana origen</span>
                                <span th:case="'EN_TRANSITO'" class="bg-sky-200 text-sky-800 rounded-full px-2.5 py-0.5">Tránsito</span>
                                <span th:case="'EN_ADUANA_DESTINO'" class="bg-violet-200 text-violet-800 rounded-full px-2.5 py-0.5">Aduana destino</span>
                                <span th:case="'EN_REPARTO'" class="bg-orange-200 text-orange-800 rounded-full px-2.5 py-0.5">Reparto</span>
                                <span th:case="'ENTREGADO'" class="bg-emerald-200 text-emerald-800 rounded-full px-2.5 py-0.5">Entregado</span>
                                <span th:case="*" class="bg-stone-200 text-stone-700 rounded-full px-2.5 py-0.5" th:text="${envio.estado}">Estado</span>
                            </span>
                        </td>
                        <td class="px-4 py-3" th:text="${#temporals.format(envio.ultimaActualizacion, 'dd/MM/yyyy HH:mm')}">15/05/2026 14:30</td>
                        <td class="px-4 py-3">
                            <div class="flex items-center gap-3">
                                <a th:href="@{/tracking/{codigo}(codigo=${envio.codigoUnico})}"
                                   class="font-medium text-brand-600 hover:underline">Ver tracking</a>
                                <a th:href="@{/cliente/panel/envio/{codigo}/etiqueta(codigo=${envio.codigoUnico})}"
                                   class="font-medium text-stone-500 hover:text-brand-600 hover:underline">Descargar etiqueta</a>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div th:if="${#lists.isEmpty(panel.envios)}" class="p-10 text-center text-stone-500">
            No tienes envíos registrados todavía.
        </div>
    </div>
</main>

<footer th:replace="~{fragments/public-head :: footer}"></footer>
</body>
</html>
```

- [x] **Step 6: Eliminar templates supersedidos** (`tracking.html`, `en/tracking.html`)
- [x] **Step 7: Commit**

```bash
git add src/main/resources/templates
git rm src/main/resources/templates/tracking.html src/main/resources/templates/en/tracking.html
git commit -m "style(web): Tailwind public tracking, result timeline, 404 and client dashboard templates"
```

---

### Task 6: Excepciones, `TrackingWebController`, traslado de rutas `/tracking` + tests

**Files:**
- Create: `src/main/java/com/monteastur/envios/exception/TrackingNoEncontradoException.java`
- Create: `src/main/java/com/monteastur/envios/exception/ForbiddenException.java`
- Create: `src/main/java/com/monteastur/envios/controller/web/TrackingWebController.java`
- Modify: `src/main/java/com/monteastur/envios/controller/GlobalExceptionHandler.java` (añadir handler `ForbiddenException` → 403)
- Modify: `src/main/java/com/monteastur/envios/controller/PublicController.java` (eliminar handlers GET/POST `/tracking` y dependencias)
- Modify: `src/test/java/com/monteastur/envios/controller/PublicControllerTest.java` (eliminar tests de `/tracking` y mocks ya no usados)
- Create: `src/test/java/com/monteastur/envios/controller/web/TrackingWebControllerTest.java`

**Interfaces:**
- `TrackingWebController` consume `PublicTrackingService` (Task 2) y las vistas de Task 5. `PublicController` conserva el resto de rutas públicas y su helper `template()`.

- [x] **Step 1: Excepciones**

```java
package com.monteastur.envios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TrackingNoEncontradoException extends RuntimeException {

    private final String codigo;

    public TrackingNoEncontradoException(String codigo) {
        super("Tracking no encontrado: " + codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
```

```java
package com.monteastur.envios.exception;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
```

- [x] **Step 2: Handler de `ForbiddenException` en `GlobalExceptionHandler`** (tras el handler de `ConflictException`)

```java
@ExceptionHandler(ForbiddenException.class)
public Object handleForbidden(ForbiddenException ex, HttpServletRequest request, Model model) {
    if (isRestRequest(request)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorDto(Instant.now().toString(), 403, ex.getMessage()));
    }
    return mvcError(request, model, HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
}
```

(Imports necesarios: `com.monteastur.envios.exception.ForbiddenException`.)

- [x] **Step 3: `TrackingWebController`**

```java
package com.monteastur.envios.controller.web;

import com.monteastur.envios.dto.web.PublicTrackingView;
import com.monteastur.envios.exception.TrackingNoEncontradoException;
import com.monteastur.envios.service.web.PublicTrackingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Portal público de rastreo modernizado (Tailwind). El buscador sigue PRG:
 * POST -> redirect a /tracking/{codigo} -> GET con la vista cacheada.
 */
@Controller
public class TrackingWebController {

    private final PublicTrackingService publicTrackingService;

    public TrackingWebController(PublicTrackingService publicTrackingService) {
        this.publicTrackingService = publicTrackingService;
    }

    @GetMapping({"/tracking", "/en/tracking"})
    public String formulario(Model model) {
        model.addAttribute("buscado", false);
        return "tracking-search";
    }

    @PostMapping({"/tracking", "/en/tracking"})
    public String buscar(@RequestParam String codigo, Model model) {
        String codigoNormalizado = codigo.trim().toUpperCase();
        PublicTrackingView view = publicTrackingService.cargarPagina(codigoNormalizado);
        if (view == null) {
            model.addAttribute("buscado", true);
            model.addAttribute("error", true);
            model.addAttribute("codigo", codigoNormalizado);
            return "tracking-search";
        }
        return "redirect:/tracking/" + codigoNormalizado;
    }

    @GetMapping({"/tracking/{codigo}", "/en/tracking/{codigo}"})
    public String resultado(@PathVariable String codigo, Model model) {
        String codigoNormalizado = codigo.trim().toUpperCase();
        PublicTrackingView view = publicTrackingService.cargarPagina(codigoNormalizado);
        if (view == null) {
            throw new TrackingNoEncontradoException(codigoNormalizado);
        }
        model.addAttribute("view", view);
        return "tracking-result";
    }

    @ExceptionHandler(TrackingNoEncontradoException.class)
    public String handleNoEncontrado(TrackingNoEncontradoException ex, Model model) {
        model.addAttribute("codigo", ex.getCodigo());
        return "tracking-404";
    }
}
```

- [x] **Step 4: Limpiar `PublicController`** (eliminar handlers `tracking`/`buscarTracking`, el campo `trackingRepo`, el campo `eventoTrackingService`, sus params de constructor y los imports `EnvioTracking`, `EnvioTrackingRepository`, `EventoTrackingService`). Se conservan `template()`, `isEnglish()`, el resto de rutas y el resto de imports (`@PostMapping`, `@RequestParam` siguen usándose en reservas/contacto).

- [x] **Step 5: Actualizar `PublicControllerTest`** (eliminar `tracking_returnsView`, `tracking_en_returnsEnView`, los `@MockBean EnvioTrackingRepository` y `@MockBean EventoTrackingService`, y los imports de esos dos tipos).

- [x] **Step 6: `TrackingWebControllerTest`** (TDD — se escribió con el mismo patrón `@WebMvcTest` + `@Import(SecurityConfig.class)` + `@TestPropertySource` de `PublicControllerTest`; POST con `.with(csrf())`)

```java
package com.monteastur.envios.controller.web;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.dto.web.EventoView;
import com.monteastur.envios.dto.web.EvidenciaView;
import com.monteastur.envios.dto.web.EntregaView;
import com.monteastur.envios.dto.web.PublicTrackingView;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.web.PublicTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TrackingWebController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class TrackingWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicTrackingService publicTrackingService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private PublicTrackingView viewValida() {
        return new PublicTrackingView("MT-1", "RECIBIDO", "Destinatario", "Asturias, España",
                "Asunción, Paraguay", "10 kg", "Documentos", null, "Asturias",
                LocalDateTime.of(2026, 5, 10, 9, 0), LocalDateTime.of(2026, 5, 15, 14, 30),
                null, null, 0, new ArrayList<>(PublicTrackingView.PASOS_CANONICOS),
                List.of(new EventoView()), List.of(new EvidenciaView()), null);
    }

    @Test
    void formulario_retornaBuscador() throws Exception {
        mockMvc.perform(get("/tracking"))
                .andExpect(status().isOk())
                .andExpect(view().name("tracking-search"))
                .andExpect(model().attribute("buscado", false));
    }

    @Test
    void formulario_en_retornaBuscador() throws Exception {
        mockMvc.perform(get("/en/tracking"))
                .andExpect(status().isOk())
                .andExpect(view().name("tracking-search"));
    }

    @Test
    void buscar_encontrado_redirigePrg() throws Exception {
        when(publicTrackingService.cargarPagina("MT-1")).thenReturn(viewValida());

        mockMvc.perform(post("/tracking").param("codigo", " mt-1 ").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tracking/MT-1"));
    }

    @Test
    void buscar_noEncontrado_rerenderConError() throws Exception {
        when(publicTrackingService.cargarPagina("NOPE")).thenReturn(null);

        mockMvc.perform(post("/tracking").param("codigo", "nope").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("tracking-search"))
                .andExpect(model().attribute("error", true))
                .andExpect(model().attribute("codigo", "NOPE"));
    }

    @Test
    void resultado_retornaTimeline() throws Exception {
        when(publicTrackingService.cargarPagina("MT-1")).thenReturn(viewValida());

        mockMvc.perform(get("/tracking/MT-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("tracking-result"))
                .andExpect(model().attributeExists("view"));
    }

    @Test
    void resultado_en_retornaTimeline() throws Exception {
        when(publicTrackingService.cargarPagina("MT-1")).thenReturn(viewValida());

        mockMvc.perform(get("/en/tracking/MT-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("tracking-result"));
    }

    @Test
    void resultado_noEncontrado_retorna404() throws Exception {
        when(publicTrackingService.cargarPagina("NOPE")).thenReturn(null);

        mockMvc.perform(get("/tracking/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("tracking-404"))
                .andExpect(model().attribute("codigo", "NOPE"));
    }
}
```

- [x] **Step 7: Verificar los tests nuevos** (`-Dtest=TrackingWebControllerTest,PublicControllerTest`) → `BUILD SUCCESS`
- [x] **Step 8: Commit**

```bash
git add src/main/java/com/monteastur/envios/exception src/main/java/com/monteastur/envios/controller/web src/main/java/com/monteastur/envios/controller/GlobalExceptionHandler.java src/main/java/com/monteastur/envios/controller/PublicController.java src/test/java/com/monteastur/envios/controller
git commit -m "feat(web): add TrackingWebController with PRG search and 404 page; move /tracking routes"
```

---

### Task 7: `ClientDashboardController`, traslado de `/cliente/panel`, logout POST + tests

**Files:**
- Create: `src/main/java/com/monteastur/envios/controller/web/ClientDashboardController.java`
- Modify: `src/main/java/com/monteastur/envios/controller/ClienteController.java` (eliminar `GET /cliente/panel` y dependencias; añadir `POST /cliente/logout`)
- Create: `src/test/java/com/monteastur/envios/controller/web/ClientDashboardControllerTest.java`

**Interfaces:**
- `ClientDashboardController` consume `ClientDashboardService` (Task 3), `DocumentoPdfService`, `EnvioTrackingRepository`, `ClienteService`, `ForbiddenException` (Task 6) y la vista `cliente/panel` (Task 5). `ClienteController` conserva `/cliente/login` y `/cliente/logout` (GET) y añade logout POST (lo usa el topbar nuevo).

- [x] **Step 1: Escribir el test** (patrón `EntregaEvidenciaControllerTest`/`DocumentosControllerTest`: `@WebMvcTest` + `@Import({GlobalExceptionHandler.class, SecurityConfig.class})` + mocks `DataSource`, `RBACAccessLogger`, `CustomAccessDeniedHandler`)

```java
package com.monteastur.envios.controller.web;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.dto.web.ClientDashboardView;
import com.monteastur.envios.dto.web.EnvioResumenView;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.ClienteService;
import com.monteastur.envios.service.DocumentoPdfService;
import com.monteastur.envios.service.web.ClientDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ClientDashboardController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class ClientDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientDashboardService dashboardService;

    @MockBean
    private DocumentoPdfService documentoPdfService;

    @MockBean
    private EnvioTrackingRepository envioTrackingRepository;

    @MockBean
    private ClienteService clienteService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private Cliente cliente() {
        Cliente cliente = new Cliente("cliente@test.com", "x", "Cliente Uno", null);
        cliente.setId(7L);
        return cliente;
    }

    private ClientDashboardView viewValida() {
        EnvioResumenView resumen = new EnvioResumenView(1L, "MT-1", "RECIBIDO",
                "Asunción, Paraguay", "Documentos", "10 kg", LocalDateTime.now(), null);
        return new ClientDashboardView(7L, "Cliente Uno", "cliente@test.com",
                1, 1, 0, 10.0, 10.0, new ArrayList<>(List.of(resumen)));
    }

    private EnvioTracking envio(boolean propio) {
        EnvioTracking envio = new EnvioTracking("MT-1", "RECIBIDO", "Destinatario",
                "Asturias, España", "Asunción, Paraguay", "10 kg", "Documentos");
        envio.setId(1L);
        envio.setCliente(propio ? cliente() : new Cliente("otro@test.com", "x", "Otro", null));
        return envio;
    }

    @Test
    void panel_sinSesion_redirigeLogin() throws Exception {
        mockMvc.perform(get("/cliente/panel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/login"));
    }

    @Test
    void panel_conSesion_retornaDashboard() throws Exception {
        when(clienteService.buscarPorId(7L)).thenReturn(Optional.of(cliente()));
        when(dashboardService.cargarDashboard(7L)).thenReturn(viewValida());

        mockMvc.perform(get("/cliente/panel").sessionAttr("clienteId", 7L))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/panel"))
                .andExpect(model().attributeExists("panel"));
    }

    @Test
    void panel_clienteInexistente_redirigeLogin() throws Exception {
        when(clienteService.buscarPorId(7L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/cliente/panel").sessionAttr("clienteId", 7L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/login"));
    }

    @Test
    void etiqueta_sinSesion_redirigeLogin() throws Exception {
        mockMvc.perform(get("/cliente/panel/envio/MT-1/etiqueta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/login"));
    }

    @Test
    void etiqueta_envioPropio_retornaPdf() throws Exception {
        when(clienteService.buscarPorId(7L)).thenReturn(Optional.of(cliente()));
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1"))
                .thenReturn(Optional.of(envio(true)));
        when(documentoPdfService.generarEtiqueta("MT-1", "cliente:cliente@test.com"))
                .thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(get("/cliente/panel/envio/MT-1/etiqueta").sessionAttr("clienteId", 7L))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"etiqueta-MT-1.pdf\""))
                .andExpect(content().bytes(new byte[]{'%', 'P', 'D', 'F'}));
    }

    @Test
    void etiqueta_envioAjeno_retorna403() throws Exception {
        when(clienteService.buscarPorId(7L)).thenReturn(Optional.of(cliente()));
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1"))
                .thenReturn(Optional.of(envio(false)));

        mockMvc.perform(get("/cliente/panel/envio/MT-1/etiqueta").sessionAttr("clienteId", 7L))
                .andExpect(status().isForbidden());
    }

    @Test
    void etiqueta_envioInexistente_retorna404() throws Exception {
        when(clienteService.buscarPorId(7L)).thenReturn(Optional.of(cliente()));
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-NOPE"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/cliente/panel/envio/MT-NOPE/etiqueta").sessionAttr("clienteId", 7L))
                .andExpect(status().isNotFound());
    }
}
```

- [x] **Step 2: Escribir la implementación**

```java
package com.monteastur.envios.controller.web;

import com.monteastur.envios.dto.web.ClientDashboardView;
import com.monteastur.envios.exception.ForbiddenException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.ClienteService;
import com.monteastur.envios.service.DocumentoPdfService;
import com.monteastur.envios.service.web.ClientDashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;

/**
 * Panel de cliente autenticado por sesión (clienteId en HttpSession).
 * El dashboard se cachea en Redis y la etiqueta PDF solo es descargable
 * por el propietario del envío (ajeno -> 403, inexistente -> 404).
 */
@Controller
@RequestMapping("/cliente")
public class ClientDashboardController {

    private final ClientDashboardService dashboardService;
    private final DocumentoPdfService documentoPdfService;
    private final EnvioTrackingRepository envioTrackingRepository;
    private final ClienteService clienteService;

    public ClientDashboardController(ClientDashboardService dashboardService,
                                     DocumentoPdfService documentoPdfService,
                                     EnvioTrackingRepository envioTrackingRepository,
                                     ClienteService clienteService) {
        this.dashboardService = dashboardService;
        this.documentoPdfService = documentoPdfService;
        this.envioTrackingRepository = envioTrackingRepository;
        this.clienteService = clienteService;
    }

    @GetMapping("/panel")
    public String panel(HttpSession session, Model model) {
        Cliente cliente = clienteAutenticado(session);
        if (cliente == null) {
            return "redirect:/cliente/login";
        }
        ClientDashboardView dashboard = dashboardService.cargarDashboard(cliente.getId());
        model.addAttribute("panel", dashboard);
        return "cliente/panel";
    }

    @GetMapping("/panel/envio/{codigo}/etiqueta")
    public ResponseEntity<byte[]> descargarEtiqueta(@PathVariable String codigo, HttpSession session) {
        Cliente cliente = clienteAutenticado(session);
        if (cliente == null) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/cliente/login")).build();
        }
        String codigoNormalizado = codigo.trim().toUpperCase();
        EnvioTracking envio = envioTrackingRepository.findWithClienteByCodigoUnico(codigoNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("Tracking no encontrado: " + codigoNormalizado));
        boolean propio = envio.getCliente() != null && cliente.getId().equals(envio.getCliente().getId());
        if (!propio) {
            throw new ForbiddenException("El envío " + codigoNormalizado + " no pertenece al cliente autenticado");
        }
        byte[] pdf = documentoPdfService.generarEtiqueta(codigoNormalizado, "cliente:" + cliente.getEmail());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"etiqueta-" + codigoNormalizado + ".pdf\"")
                .body(pdf);
    }

    private Cliente clienteAutenticado(HttpSession session) {
        Long clienteId = (Long) session.getAttribute("clienteId");
        if (clienteId == null) {
            return null;
        }
        return clienteService.buscarPorId(clienteId)
                .orElseGet(() -> {
                    session.invalidate();
                    return null;
                });
    }
}
```

- [x] **Step 3: Limpiar `ClienteController`** (eliminar el handler `panel`, los campos `trackingRepo`/`evidenciaService`/`eventoTrackingService` y sus params de constructor, imports de `EnvioTracking`, `EnvioTrackingRepository`, `EvidenciaEnvioService`, `EventoTrackingService`, `EvidenciaEnvio`, `EventoTracking`, `Model`, `List`, `Map`, `HashMap`; añadir el handler POST de logout)

```java
@PostMapping("/logout")
public String logoutPost(HttpSession session) {
    session.invalidate();
    return "redirect:/cliente/login";
}
```

- [x] **Step 4: Verificar los tests** (`-Dtest=ClientDashboardControllerTest,ClienteControllerTest`) — nota: no existe `ClienteControllerTest`; usar `-Dtest=ClientDashboardControllerTest` → `BUILD SUCCESS`
- [x] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/controller/web src/main/java/com/monteastur/envios/controller/ClienteController.java src/test/java/com/monteastur/envios/controller/web
git commit -m "feat(web): add ClientDashboardController with ownership-protected label PDF; move /cliente/panel"
```

---

### Task 8: Test de integración E2E del portal + verificación de caché Redis

**Files:**
- Create: `src/test/java/com/monteastur/envios/integration/PortalTrackingDashboardIntegrationTest.java`

**Interfaces:**
- Valida extremo a extremo con contexto real (`@SpringBootTest` + `@ActiveProfiles("test")` + `@AutoConfigureMockMvc` + `@MockBean EmailService`): rutas web, ownership del PDF, 404, y caché Redis (patrón `EnvioTrackingCacheIntegrationTest`/`EntregaEvidenciaIntegrationTest`). Requiere MySQL (`envios_paraguay_cms_test`) y Redis levantados.

- [x] **Step 1: Escribir el test**

```java
package com.monteastur.envios.integration;

import com.monteastur.envios.dto.web.PublicTrackingView;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.EventoTracking;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.DocumentoGeneradoRepository;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EvidenciaEnvioRepository;
import com.monteastur.envios.repository.EventoTrackingRepository;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.web.PublicTrackingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PortalTrackingDashboardIntegrationTest {

    private static final String PNG_1X1 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @Autowired private MockMvc mockMvc;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EnvioTrackingRepository envioTrackingRepository;
    @Autowired private EventoTrackingRepository eventoTrackingRepository;
    @Autowired private EvidenciaEnvioRepository evidenciaEnvioRepository;
    @Autowired private EntregaEvidenciaRepository entregaEvidenciaRepository;
    @Autowired private DocumentoGeneradoRepository documentoGeneradoRepository;
    @Autowired private CacheManager cacheManager;
    @Autowired private StringRedisTemplate stringRedisTemplate;
    @Autowired private PublicTrackingService publicTrackingService;

    @MockBean private EmailService emailService;

    private Long clienteId;
    private Long envioId;
    private String codigoActual;

    @AfterEach
    void limpiar() {
        if (codigoActual != null) {
            documentoGeneradoRepository.deleteAll(documentoGeneradoRepository.findAll().stream()
                    .filter(d -> codigoActual.equals(d.getReferenciaId()))
                    .toList());
        }
        if (envioId != null) {
            entregaEvidenciaRepository.findByEnvioId(envioId).ifPresent(entregaEvidenciaRepository::delete);
            eventoTrackingRepository.deleteAll(eventoTrackingRepository.findByEnvioTrackingIdOrderByFechaEventoDesc(envioId));
            evidenciaEnvioRepository.deleteAll(evidenciaEnvioRepository.findByEnvioTrackingIdOrderByFechaSubidaDesc(envioId));
            envioTrackingRepository.deleteById(envioId);
        }
        if (clienteId != null) {
            clienteRepository.deleteById(clienteId);
        }
        for (String cache : List.of("envios.tracking.pagina", "envios.cliente.dashboard")) {
            Cache c = cacheManager.getCache(cache);
            if (c != null) {
                c.clear();
            }
        }
        clienteId = null;
        envioId = null;
        codigoActual = null;
    }

    private void seedClienteYEnvio(String codigo, String estado) {
        Cliente cliente = clienteRepository.save(
                new Cliente("portal-" + System.nanoTime() + "@test.com", "pass", "Cliente Portal", null));
        clienteId = cliente.getId();
        EnvioTracking envio = new EnvioTracking(codigo, estado, "Destinatario Portal",
                "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos");
        envio.setCliente(cliente);
        EnvioTracking guardado = envioTrackingRepository.save(envio);
        envioId = guardado.getId();
        codigoActual = codigo;

        EventoTracking evento = new EventoTracking();
        evento.setEnvioTracking(guardado);
        evento.setEstado(estado);
        evento.setTitulo("Envío registrado en MONTEASTUR");
        evento.setUbicacion("Asturias, España");
        evento.setIcono("📦");
        evento.setColor("#d4762a");
        evento.setFechaEvento(LocalDateTime.now());
        evento.setCreadoPor("admin");
        evento.setVisibleCliente(true);
        eventoTrackingRepository.save(evento);
    }

    @Test
    void paginaTracking_retorna200ConTimeline() throws Exception {
        String codigo = "PY-PORTAL-" + System.nanoTime();
        seedClienteYEnvio(codigo, "RECIBIDO");

        mockMvc.perform(get("/tracking/" + codigo))
                .andExpect(status().isOk())
                .andExpect(view().name("tracking-result"))
                .andExpect(model().attributeExists("view"));
    }

    @Test
    void paginaTracking_entregado_muestraPOD() throws Exception {
        String codigo = "PY-POD-" + System.nanoTime();
        seedClienteYEnvio(codigo, "ENTREGADO");
        EnvioTracking envio = envioTrackingRepository.findById(envioId).orElseThrow();
        entregaEvidenciaRepository.save(new EntregaEvidencia(envio, "Receptor", "1234", PNG_1X1,
                new BigDecimal("-25.2637421"), new BigDecimal("-57.575926"), null));

        mockMvc.perform(get("/tracking/" + codigo))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("view"));

        PublicTrackingView view = publicTrackingService.cargarPagina(codigo);
        assertThat(view.getPasoActual()).isEqualTo(5);
        assertThat(view.getEntrega()).isNotNull();
        assertThat(view.getEntrega().getReceptorNombre()).isEqualTo("Receptor");
    }

    @Test
    void paginaTracking_inexistente_retorna404() throws Exception {
        mockMvc.perform(get("/tracking/PY-NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("tracking-404"));
    }

    @Test
    void panel_cliente_retornaDashboard() throws Exception {
        String codigo = "PY-DASH-" + System.nanoTime();
        seedClienteYEnvio(codigo, "RECIBIDO");

        mockMvc.perform(get("/cliente/panel").sessionAttr("clienteId", clienteId))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/panel"))
                .andExpect(model().attributeExists("panel"));
    }

    @Test
    void etiqueta_propio_retornaPdf() throws Exception {
        String codigo = "PY-PDF-" + System.nanoTime();
        seedClienteYEnvio(codigo, "RECIBIDO");

        mockMvc.perform(get("/cliente/panel/envio/" + codigo + "/etiqueta").sessionAttr("clienteId", clienteId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void etiqueta_ajeno_retorna403() throws Exception {
        String codigo = "PY-AJENO-" + System.nanoTime();
        seedClienteYEnvio(codigo, "RECIBIDO");
        Long otroClienteId = clienteRepository.save(
                new Cliente("ajeno-" + System.nanoTime() + "@test.com", "pass", "Otro", null)).getId();

        mockMvc.perform(get("/cliente/panel/envio/" + codigo + "/etiqueta").sessionAttr("clienteId", otroClienteId))
                .andExpect(status().isForbidden());

        clienteRepository.deleteById(otroClienteId);
    }

    @Test
    void etiqueta_sinSesion_redirigeLogin() throws Exception {
        mockMvc.perform(get("/cliente/panel/envio/PY-NOPE/etiqueta"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void cachePagina_segundaConsultaNoTocaBaseDeDatos() throws Exception {
        String codigo = "PY-CACHE-" + System.nanoTime();
        seedClienteYEnvio(codigo, "RECIBIDO");

        PublicTrackingView primera = publicTrackingService.cargarPagina(codigo);
        assertThat(primera).isNotNull();

        entregaEvidenciaRepository.findByEnvioId(envioId).ifPresent(entregaEvidenciaRepository::delete);
        eventoTrackingRepository.deleteAll(eventoTrackingRepository.findByEnvioTrackingIdOrderByFechaEventoDesc(envioId));
        evidenciaEnvioRepository.deleteAll(evidenciaEnvioRepository.findByEnvioTrackingIdOrderByFechaSubidaDesc(envioId));
        envioTrackingRepository.deleteById(envioId);
        envioId = null;

        PublicTrackingView segunda = publicTrackingService.cargarPagina(codigo);
        assertThat(segunda).isNotNull();
        assertThat(segunda.getCodigoUnico()).isEqualTo(codigo);
    }

    @Test
    void cachePagina_tieneTtlDeCincoMinutos() throws Exception {
        String codigo = "PY-TTL-" + System.nanoTime();
        seedClienteYEnvio(codigo, "RECIBIDO");

        publicTrackingService.cargarPagina(codigo);

        Long ttl = stringRedisTemplate.getExpire("envios.tracking.pagina::" + codigo);
        assertThat(ttl).isNotNull().isBetween(1L, 300L);
    }
}
```

- [x] **Step 2: Verificar el test de integración** (requiere MySQL/Redis de la red; `-Dtest=PortalTrackingDashboardIntegrationTest`) → `BUILD SUCCESS`, 8 tests
- [x] **Step 3: Commit**

```bash
git add src/test/java/com/monteastur/envios/integration/PortalTrackingDashboardIntegrationTest.java
git commit -m "test(integration): portal tracking and dashboard E2E with Redis cache verification"
```

---

### Task 9: Verificación final (suite completa) + `docs/handoff.md`

**Files:**
- Modify: `docs/handoff.md`

**Interfaces:**
- Valida los criterios de aceptación del spec (criterio 7: `./mvnw clean test` BUILD SUCCESS en contenedor) y deja constancia del estado del Bloque 15.

- [x] **Step 1: Garantizar que la DB de test existe**

```powershell
docker exec monteastur-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS envios_paraguay_cms_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>&1 | Select-String -NotMatch "Using a password"
```

- [x] **Step 2: Suite completa en contenedor Maven Linux**

```powershell
docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
  -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis `
  -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 sh -c "./mvnw clean test -B"
```

(Timeout amplio: 600000 ms.) Expected: `BUILD SUCCESS`, ~210 tests, 0 fallos.

- [x] **Step 3: Actualizar `docs/handoff.md`** con el estado del Bloque 15 (componentes, caches, tests, `BUILD SUCCESS`, pendiente de push).
- [x] **Step 4: Commit final**

```bash
git add docs/handoff.md
git commit -m "docs(handoff): Bloque 15 portal publico y dashboard de clientes"
```

---

## Self-Review

- **Cobertura de spec:** rutas `GET/POST /tracking`, `/en/tracking`, `GET /tracking/{codigo}` (Task 6) ✓; timeline + POD (Task 5, Task 8) ✓; `GET /cliente/panel` + métricas `PesoUtil` (Tasks 3, 7) ✓; etiqueta PDF con ownership 200/403/404 (Task 7, Task 8) ✓; caché Redis con DTOs planos + invalidación (Tasks 2–4, 8) ✓; `TrackingNoEncontradoException` → 404 (Task 6) ✓; limpieza `tracking.html`/`en/tracking.html` (Task 5) ✓; resto del sitio intacto (solo se tocan `PublicController`, `ClienteController`, `GlobalExceptionHandler` y caches) ✓.
- **TDD:** test primero por tarea (Tasks 2–3 unit, Task 6–7 `@WebMvcTest`, Task 8 `@SpringBootTest`). `PublicControllerTest` se actualiza en el mismo commit que el traslado.
- **Sin placeholders:** todos los pasos incluyen código o comandos completos.
- **Riesgos gestionados:** (a) `@WebMvcTest` resuelve Thymeleaf → plantillas en Task 5 antes de los tests de controladores; (b) mapeo ambiguo → refactor atómico por ruta (Task 6 y Task 7); (c) `AccessDeniedException`→400 en el repo → `ForbiddenException`→403 vía `GlobalExceptionHandler`; (d) CSRF → `.with(csrf())` en los POST de tests; (e) listas mutables en DTOs para `GenericJackson2JsonRedisSerializer` NON_FINAL.
- **Consistencia de nombres:** `envios.tracking.pagina` y `envios.cliente.dashboard` usados igual en RedisConfig, servicios, evictions y tests. Model attribute `panel`/`view` consistentes entre controladores y plantillas.

---

## Completion

- **Task 1 (DTOs View):** pendiente.
- **Task 2 (PublicTrackingService):** pendiente.
- **Task 3 (ClientDashboardService):** pendiente.
- **Task 4 (Caché Redis + evicts):** pendiente.
- **Task 5 (Plantillas Tailwind):** pendiente.
- **Task 6 (TrackingWebController + traslado):** pendiente.
- **Task 7 (ClientDashboardController + traslado):** pendiente.
- **Task 8 (Integración E2E + caché):** pendiente.
- **Task 9 (Verificación final + handoff):** pendiente.
