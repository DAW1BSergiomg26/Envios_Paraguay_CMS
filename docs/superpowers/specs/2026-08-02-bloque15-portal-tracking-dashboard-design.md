# Bloque 15 — Portal Público de Rastreo & Dashboard Interactivo de Clientes (UI/UX)

**Fecha:** 2026-08-02
**Estado:** Aprobado (Enfoque A — Tailwind via CDN con config de marca).
**Proyecto:** Envios_Paraguay_CMS (Spring Boot 3.3.5 + Thymeleaf + Redis + MySQL 8 + Flyway).

---

## 1. Contexto y motivación

El proyecto ya dispone de rastreo público (`GET/POST /tracking`, `templates/tracking.html`) y de un panel de cliente (`/cliente/panel`, `templates/cliente/panel.html`) con autenticación por sesión manual (`clienteId` en `HttpSession`). Ambos usan un sistema visual propio "premium/luxury" (`static/css/*` cargados desde `fragments/header.html`). También existe la API REST cacheada `GET /api/v1/tracking/{codigo}` (`@Cacheable("envios.tracking")`) y el motor de PDF (Bloque 12) y POD (Bloque 13).

El **Bloque 15** moderniza estas vistas con **Tailwind CSS vía CDN**, añade una **ruta pública `GET /tracking/{codigo}`** (timeline interactivo + datos del POD si está `ENTREGADO`), un **dashboard de cliente con métricas** (peso acumulado vía `PesoUtil`) y **descarga de etiqueta PDF** para envíos propios. Las consultas públicas y del dashboard se **cachean en Redis** con DTOs planos (nunca entidades JPA).

**Decisión de alcance (aprobada):** modernizar las vistas existentes manteniendo las mismas URLs funcionales, **sin** romper el resto del sitio, **sin** migrar la autenticación a Spring Security y **sin** introducir build step de Node.

---

## 2. Reglas inquebrantables del proyecto (aplicadas)

1. **Prohibido Lombok:** todos los DTOs de vista (`PublicTrackingView`, `EventoView`, `EvidenciaView`, `EntregaView`, `ClientDashboardView`, `EnvioResumenView`) en **Java puro**: atributos privados, constructor vacío, constructor parametrizado y getters/setters explícitos.
2. **Inyección por constructor `private final`** en controladores y servicios nuevos.
3. **Nomenclatura:** clases/métodos/variables en inglés; Javadoc y textos de usuario en español; sin comentarios redundantes.
4. **Cero excepciones silenciadas** en flujos críticos.
5. **TDD:** tests unitarios (`Mockito`), de capa web (`@WebMvcTest`) y de integración (`@SpringBootTest`) antes de la verificación final `./mvnw clean test` (BUILD SUCCESS).
6. **Sin migración Flyway** en este bloque (no cambia el esquema).

---

## 3. Componentes nuevos/modificados

### 3.1 `TrackingWebController` (NUEVO) — `src/main/java/com/monteastur/envios/controller/web/TrackingWebController.java`

| Ruta | Método | Comportamiento |
|---|---|---|
| `GET /tracking`, `GET /en/tracking` | `formulario` | Renderiza `tracking-search` con `buscado=false`. |
| `POST /tracking`, `POST /en/tracking` | `buscar` | Normaliza `codigo.trim().toUpperCase()`. Si `PublicTrackingService.cargarPagina(codigo)` es `null` → re-render de `tracking-search` con `error` y `codigo`. Si existe → **PRG**: `302` a `redirect:/tracking/{codigo}`. |
| `GET /tracking/{codigo}`, `GET /en/tracking/{codigo}` | `resultado` | Llama al servicio cacheado. `null` → lanza `TrackingNoEncontradoException` (→ 404 personalizado). Éxito → `tracking-result` con `view` en el model. |

- Se **eliminan** los handlers `GET/POST /tracking` de `PublicController` (se trasladan aquí). `PublicController` conserva el resto de rutas públicas y su helper `template()`.
- **Decisión i18n:** `/en/*` se mantienen funcionales y delegan en las mismas vistas modernizadas (el layout moderno aplica a ambos idiomas; el texto de las páginas modernizadas es ES). Se eliminan `templates/tracking.html` y `templates/en/tracking.html` (supersedidas).

### 3.2 `ClientDashboardController` (NUEVO) — `src/main/java/com/monteastur/envios/controller/web/ClientDashboardController.java`

| Ruta | Método | Comportamiento |
|---|---|---|
| `GET /cliente/panel` | `panel` | Protegido por sesión: sin `clienteId` → `redirect:/cliente/login`. Cliente inexistente → invalida sesión y redirige a login. Éxito → `cliente/panel` con `ClientDashboardView` en el model. |
| `GET /cliente/panel/envio/{codigo}/etiqueta` | `descargarEtiqueta` | Sesión obligatoria. El envío debe pertenecer al cliente autenticado (si no → **403**; si no existe → **404**). Devuelve el PDF de etiqueta térmica (`application/pdf`, `Content-Disposition: attachment; filename=etiqueta-{codigo}.pdf`). Reutiliza `DocumentoPdfService.generarEtiqueta(codigo, "cliente:"+email)`. |

- El handler `/cliente/panel` se **elimina de `ClienteController`** (que conserva `/cliente/login`, `/cliente/logout`). La auth sigue siendo por sesión manual (mismo `HttpSession`).
- El dashboard se sirve desde `templates/cliente/panel.html` (fichero existente, **modernizado**; se mantiene la ruta y el nombre de vista).

### 3.3 `PublicTrackingService` (NUEVO) — `src/main/java/com/monteastur/envios/service/web/PublicTrackingService.java`

```java
@Cacheable(value = "envios.tracking.pagina", key = "#codigo", unless = "#result == null")
public PublicTrackingView cargarPagina(String codigo)
```

- `codigo = codigo.trim().toUpperCase()`.
- `envioTrackingRepository.findWithClienteByCodigoUnico(codigo)` → si vacío devuelve `null`.
- Eventos: `eventoTrackingService.listarPorEnvio(envio.getId())`.
- Evidencias visibles al cliente: `evidenciaEnvioService.listarPorEnvioParaCliente(envio.getId())`.
- POD: si `estado.equals("ENTREGADO")` → `entregaEvidenciaRepository.findByEnvioId(envio.getId())` (nullable).
- Ensambla `PublicTrackingView` **sin referencias LAZY** (solo datos planos).

### 3.4 `ClientDashboardService` (NUEVO) — `src/main/java/com/monteastur/envios/service/web/ClientDashboardService.java`

```java
@Cacheable(value = "envios.cliente.dashboard", key = "#clienteId")
public ClientDashboardView cargarDashboard(Long clienteId)
```

- `envioTrackingRepository.findByClienteIdOrderByUltimaActualizacionDesc(clienteId)`.
- `clienteRepository.findById(clienteId)` para nombre/email.
- Métricas: `totalEnvios`, `enviosActivos` (estado != `ENTREGADO`), `enviosEntregados`, `pesoTotalKg`, `pesoActivoKg`. Los pesos se suman con `PesoUtil.parsear(String)` → `OptionalDouble` (inválidos se ignoran).
- **Simplificación deliberada:** el dashboard NO carga timeline/evidencias por envío (evita N+1); cada fila enlaza a `GET /tracking/{codigo}` para el detalle completo.

### 3.5 DTOs de vista (Java puro, sin Lombok)

**`PublicTrackingView`** — `src/main/java/com/monteastur/envios/dto/web/PublicTrackingView.java`
- Envío: `codigoUnico`, `estado`, `destinatario`, `origen`, `destino`, `peso`, `contenido`, `observaciones`, `ubicacionActual`, `fechaCreacion`, `ultimaActualizacion`, `clienteNombre` (nullable), `batchId` (nullable).
- Timeline: `pasoActual` (índice 0–5 del estado canónico; **-1 si el estado no está en la lista canónica** → el template pinta el stepper sin paso resaltado) y `List<String> pasos` (los 6 estados canónicos en orden: `RECIBIDO`, `EN_ADUANA_ORIGEN`, `EN_TRANSITO`, `EN_ADUANA_DESTINO`, `EN_REPARTO`, `ENTREGADO`).
- Colecciones: `List<EventoView> eventos`, `List<EvidenciaView> evidencias`, `EntregaView entrega` (nullable).

**`EventoView`**: `estado`, `titulo`, `descripcion`, `ubicacion`, `icono`, `color`, `fechaEvento`, `visibleCliente`.
**`EvidenciaView`**: `id`, `titulo`, `descripcion`, `tipo`, `urlArchivo`, `fechaSubida`.
**`EntregaView`**: `receptorNombre`, `receptorDocumento`, `firmaBase64`, `latitud`, `longitud`, `notas`, `fechaEntrega`.

**`ClientDashboardView`**: `clienteId`, `clienteNombre`, `clienteEmail`, `totalEnvios`, `enviosActivos`, `enviosEntregados`, `pesoTotalKg`, `pesoActivoKg`, `List<EnvioResumenView> envios`.
**`EnvioResumenView`**: `id`, `codigoUnico`, `estado`, `destino`, `contenido`, `peso`, `ultimaActualizacion`, `batchId`.

### 3.6 Caché (RedisConfig)

Añadir a `RedisConfig` los caches:

```java
"envios.tracking.pagina", defaultConfig.entryTtl(Duration.ofMinutes(5)),
"envios.cliente.dashboard", defaultConfig.entryTtl(Duration.ofMinutes(1))
```

**Invalidación** — ampliar los `@CacheEvict` existentes con los dos nombres nuevos (`allEntries = true`) en cada punto de mutación de envíos:
- `EnvioTrackingService.guardar`, `actualizarEstado`, `eliminar`.
- `EntregaEvidenciaService.registrarEntrega`.
- `BatchImportPersistenceService.procesarChunk`.

### 3.7 Excepción y manejo de 404

- `TrackingNoEncontradoException` (RuntimeException) — `src/main/java/com/monteastur/envios/exception/TrackingNoEncontradoException.java`.
- `@ExceptionHandler(TrackingNoEncontradoException.class)` en `TrackingWebController` → `tracking-404` con `ResponseStatus` HTTP 404.
- La API REST existente mantiene su 404 JSON (sin cambios).

---

## 4. Vistas Thymeleaf (Tailwind CSS via CDN)

### 4.1 Capa visual aislada — `fragments/public-head.html` (NUEVO)

- `<head>` con:
  - `<script src="https://cdn.tailwindcss.com"></script>`.
  - `tailwind.config` inline: `theme.extend.colors.brand: '#d4762a'` (color corporativo) + `fontFamily` corporativa.
  - `html5-qrcode` via CDN (`https://unpkg.com/html5-qrcode`) solo cuando `th:if` lo requiera (página de búsqueda).
- Navbar (enlaces `/`, `/casa`, `/tracking`, `/reservas`, `/contacto`) y footer en Tailwind.
- **Aislamiento:** las páginas modernizadas usan SOLO esta capa. Las vistas "premium" existentes **no** cargan Tailwind (cero conflictos de preflight).

### 4.2 `tracking-search.html` (NUEVO)

- Hero con gradiente corporativo, título, buscador (input + botón), **validación visual** (campo vacío → mensaje).
- Botón **"Escanear QR"**: modal con escáner `html5-qrcode` (cámara); al detectar un código (URL `/tracking/{codigo}` o código plano) navega a `/tracking/{codigo}`.
- Estado de error (`buscado=true && error`) → aviso amigable.
- Sección "¿Cómo funciona?" con tarjetas (1. Busca, 2. Escanea, 3. Sigue tu envío).

### 4.3 `tracking-result.html` (NUEVO)

- Header con código, badge de estado (mapa `th:switch` de los 6 estados String → colores semánticos Tailwind: `RECIBIDO` slate, `EN_ADUANA_ORIGEN` amber, `EN_TRANSITO` sky, `EN_ADUANA_DESTINO` violet, `EN_REPARTO` orange, `ENTREGADO` emerald) y fecha de última actualización.
- Tarjetas: origen → destino (flecha), peso + contenido + observaciones.
- **Timeline stepper vertical interactivo**: 6 pasos canónicos (`th:each` sobre `view.pasos`), cada paso con icono y label; pasos completados en verde, paso actual resaltado (anillo/pulso `brand`), futuros atenuados. Transiciones fluidas.
- **Lista de eventos expandible** (`th:each` sobre `view.eventos`, orden desc): icono + color del evento, título, ubicación, fecha formateada y descripción (toggle con JS vanilla).
- **Tarjeta POD** (si `view.entrega != null`): firma PNG (`<img src="data:image/png;base64,{firmaBase64}">`), receptor + documento, fecha de entrega, coordenadas GPS.
- Evidencias adjuntas (`view.evidencias`): miniaturas/enlaces.
- Enlace "Buscar otro envío" → `/tracking`.

### 4.4 `cliente/panel.html` (MODIFICADO — modernizado)

- Topbar con nombre del cliente + logout (`POST /cliente/logout`).
- Tarjetas de métricas: **Total**, **Envíos activos**, **Entregados**, **Peso total (kg)**, **Peso activo (kg)** (formateo con `#numbers.formatDecimal`).
- Tabla de envíos recientes: código, destino, contenido, peso, estado (badge semántico), última actualización; acciones **"Ver tracking"** → `/tracking/{codigo}` y **"Descargar etiqueta"** → `GET /cliente/panel/envio/{codigo}/etiqueta`.

### 4.5 `tracking-404.html` (NUEVO)

- Mensaje "Envío no encontrado", HTTP 404, con buscador integrado para reintentar y enlace al QR.

### 4.6 Limpieza

- Se eliminan: `templates/tracking.html`, `templates/en/tracking.html` (supersedidas por las nuevas). El resto de vistas premium quedan intactas.

---

## 5. Flujo de datos

```
GET /tracking/{codigo}
  → TrackingWebController.resultado
    → PublicTrackingService.cargarPagina(codigo)  [@Cacheable "envios.tracking.pagina", Redis]
        → EnvioTrackingRepository.findWithClienteByCodigoUnico
        → EventoTrackingService.listarPorEnvio
        → EvidenciaEnvioService.listarPorEnvioParaCliente
        → (si ENTREGADO) EntregaEvidenciaRepository.findByEnvioId
    → PublicTrackingView (DTO plano cacheable)
    → tracking-result.html

GET /cliente/panel  (sesión)
  → ClientDashboardController.panel
    → ClientDashboardService.cargarDashboard(clienteId)  [@Cacheable "envios.cliente.dashboard", Redis]
        → EnvioTrackingRepository.findByClienteIdOrderByUltimaActualizacionDesc
        → métricas (PesoUtil.parsear)
    → ClientDashboardView → cliente/panel.html

GET /cliente/panel/envio/{codigo}/etiqueta  (sesión)
  → ownership check (403/404)
  → DocumentoPdfService.generarEtiqueta(codigo, "cliente:"+email) → byte[]
```

---

## 6. Manejo de errores

- Código inexistente en `GET /tracking/{codigo}` → `TrackingNoEncontradoException` → **404** con `tracking-404.html`.
- Código inexistente en `POST /tracking` → re-render de búsqueda con error visual (200) — UX de formulario, no rompe el PRG.
- Dashboard/PDF sin sesión → `redirect:/cliente/login`.
- PDF de envío ajeno → **403**; envío inexistente → **404**.
- La API REST conserva sus respuestas actuales (200/404/401/403).

---

## 7. Testing (TDD)

1. **`PublicTrackingServiceTest`** (unit, Mockito): monta la vista con eventos/evidencias; incluye `EntregaView` solo si `ENTREGADO`; devuelve `null` si no existe; cache con `ConcurrentMapCacheManager` (patrón `EnvioTrackingServiceCacheTest`).
2. **`ClientDashboardServiceTest`** (unit, Mockito): métricas con `PesoUtil` (pesos válidos/inválidos/mixtos, conteo activos/entregados).
3. **`TrackingWebControllerTest`** (`@WebMvcTest` + `@Import(SecurityConfig)` + `@TestPropertySource` con `app.admin.*`, mocks de `PublicTrackingService`, `DataSource`, `RBACAccessLogger`, `CustomAccessDeniedHandler`): `GET /tracking` 200; `POST` encontrado → 3xx `redirect:/tracking/{codigo}`; `POST` no encontrado → 200 con error; `GET /tracking/{codigo}` 200 + model; `GET /tracking/NOPE` 404 vista `tracking-404`; variantes `/en/*`.
4. **`ClientDashboardControllerTest`** (`@WebMvcTest`): sin sesión → redirect login; con `MockHttpSession(clienteId)` → 200 + model; cliente inexistente → redirect login.
5. **`ClienteDocumentosControllerTest`** (`@WebMvcTest`): sin sesión → redirect; envío ajeno → 403; propio → 200 `application/pdf`; inexistente → 404.
6. **`PublicControllerTest`** (MODIFICADO): se eliminan los tests de `/tracking` (el mapeo se traslada a `TrackingWebController`).
7. **Integración `@SpringBootTest`** (nuevo, patrón `EntregaEvidenciaIntegrationTest`): seed de cliente + envío + eventos; `GET /tracking/{codigo}` 200; `GET /tracking/NOPE` 404; dashboard con sesión 200 + métricas; etiqueta PDF (propio 200 / ajeno 403); verificación de que la segunda llamada al servicio NO vuelve a tocar el repositorio (caché Redis activa).

Verificación final: `./mvnw clean test` → **BUILD SUCCESS** (suite completa en contenedor Maven Linux con MySQL/Redis, como en Bloques anteriores).

---

## 8. No-goals (YAGNI)

- No se migra la autenticación de clientes a Spring Security ni se activa `ROLE_CLIENTE`.
- No se añaden "facturas" ni manifiestos al dashboard de cliente (solo etiqueta térmica del propio envío).
- No se introduce build step de Node ni compilación estática de Tailwind.
- No se tocan las rutas ni estilos de las vistas premium existentes (fuera de las modernizadas).
- No se añade paginación al dashboard (lista actual de envíos del cliente).
- No hay migración Flyway.

---

## 9. Criterios de aceptación

1. `GET /tracking` muestra el buscador moderno (Tailwind) y `GET /tracking/{codigo}` el timeline interactivo con los datos reales del envío.
2. Código inexistente → 404 personalizado en GET; error visual en POST.
3. Si `ENTREGADO`, la página muestra la tarjeta POD (firma, receptor, fecha, coordenadas).
4. `/cliente/panel` (con sesión) muestra métricas correctas de peso (vía `PesoUtil`) y enlaces de tracking + etiqueta PDF.
5. PDF de etiqueta: 200 para envíos propios, 403 para ajenos, 404 inexistentes, redirect sin sesión.
6. Segunda llamada al servicio no consulta MySQL (caché Redis).
7. `./mvnw clean test` BUILD SUCCESS (suite completa en contenedor Maven Linux con MySQL/Redis).
8. El resto del sitio (vistas premium, API REST, admin) sigue funcionando sin cambios.
