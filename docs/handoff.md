# HANDOFF - Envios_Paraguay_CMS

## 📋 Resumen del Proyecto

**Envios_Paraguay_CMS** es una aplicación full-stack desarrollada en **Spring Boot** (Backend) y **Thymeleaf** (Frontend server-side) para gestionar envíos y operaciones logísticas entre Asturias/España y Paraguay: perfil de administración seguro, gestión de envíos, tracking público, notificaciones y control por roles.

---

## 🏗️ Arquitectura y Tecnologías

- **Backend:** Java 17, Spring Boot 3.3.5, Spring Security, Spring Data JPA, Hibernate, Flyway.
- **Base de Datos:** MySQL 8 (perfil de producción apunta a TiDB Cloud y valida el esquema con `ddl-auto=validate`).
- **Caché / Sesiones:** Redis (sesiones distribuidas y caché del tracking público).
- **Servidor Web / Reverse Proxy:** Nginx (caché estático agresivo, cabeceras de seguridad y Let's Encrypt).
- **Email:** JavaMailSender (SMTP) con soporte para Mailpit en desarrollo.
- **Observabilidad:** Prometheus, Grafana y uptime-kuma (definidos en `docker-compose.yml`).
- **Contenedores:** Docker y Docker Compose (`docker-compose.yml`, `start-all.ps1`).

Servicios del compose: `db` (MySQL), `app`, `nginx`, `certbot`, `prometheus`, `grafana`, `uptime-kuma`, `redis`.

---

## 🔒 Mejoras de Hardening y Seguridad Recientes (Sprint Actual)

1. **Endurecimiento de Producción (`application-prod.properties`)** — commit `595818e`:
   - Eliminados los fallbacks inseguros por defecto para las credenciales de base de datos (`DB_USERNAME`, `DB_PASSWORD`) y del administrador (`ADMIN_USERNAME`, `ADMIN_PASSWORD`). Ahora son variables obligatorias sin valor por defecto.
   - Forzado de `spring.jpa.hibernate.ddl-auto=validate` para evitar alteraciones automáticas de esquemas en producción.
2. **Validación de Entorno al Arranque** — commit `d87c7da`:
   - `MonteasturApplication.java` inyecta `Environment` y ejecuta `@PostConstruct validateEnvironment()`: cuando el perfil activo es `prod`, valida que `DB_USERNAME` y `DB_PASSWORD` existan; si falta alguna, registra el error y aborta el arranque lanzando `IllegalStateException`. En desarrollo, la validación se omite y se loguea un aviso informativo.
3. **Optimización Nginx** — commit `efd2bd9`:
   - Caché estático agresivo (`expires 30d;`, `add_header Cache-Control "public, immutable";`) para CSS, JS, fuentes e imágenes en `nginx/conf.d/local.conf` y `nginx/conf.d/monteastur.conf`, antes del bloque `location /`.
4. **Módulo de Notificaciones Automáticas (completado)**:
   - Spec de diseño: commit `841fb2b`.
   - Migración Flyway `V3` (tabla `notificaciones`), entidad `Notificacion` y repositorio: commit `d46a08c`.
   - Evento `EstadoEnvioActualizadoEvent`, `@EnableAsync` y propiedades SMTP/notificaciones: commit `5dc9a4f`.
   - `EmailService.enviarCorreoSimple(...)`: commit `7bc3182`.
   - Listener async `NotificacionEventListener` + unit tests: commit `602548b`.
   - `EnvioTrackingService.actualizarEstado` (evento de dominio) + refactor controller: commit `85a11cf`.
   - Perfil de test + servicio redis en CI: commit `31f4b89`.
   - Test de integración end-to-end (primer `@SpringBootTest`): commit `f823019` (listener con `REQUIRES_NEW`).
   - Mailpit dev + `.env.example`: commits `ac391ac` y `b88dfee`.
   - Timeouts SMTP (hardenig): commit `49c7a3e`.
   - Suite completa 59/59 tests + smoke runtime verificado (email vía Mailpit).
5. **Bloque 10: Módulo de Webhooks Outbound y Firma Digital HMAC-SHA256** (completado, 2026-08-02):
   - Spec de diseño: commit `b5735fb`; plan de implementación: commit `8c55328`.
   - Implementación: commit `6d1d4f7` (Task 1-6).
   - Migración Flyway `V4` (tablas `webhooks_config` y `webhook_logs`, `BOOLEAN` + FKs `ON DELETE CASCADE`), entidades `WebhookConfig`/`WebhookLog` y repositorios (Java puro, sin Lombok).
   - `WebhookSignature` (HMAC-SHA256 en hex lowercase, firma sobre el body bruto, `X-Signature-256`) y `WebhookPayloadBuilder` (JSON normalizado con `url_seguimiento` y timestamp ISO).
   - `WebhookDispatchService`: `POST` vía `RestClient`, timeouts 2s/5s, sin reintentos, auditoría en `webhook_logs` (payload, estado, status, error).
   - `WebhookEventListener`: `@Async` (`webhookTaskExecutor`) + `REQUIRES_NEW` + `AFTER_COMMIT`, traga excepciones para no romper el flujo de tracking.
   - CRUD admin `/api/v1/admin/webhooks` (GET/POST/DELETE) con `secretToken` nunca expuesto en las respuestas.
   - Props `app.webhook.*` en `application.properties` (enabled, timeouts, base-url, executor).
   - Tests: 5 unitarios nuevos + 2 de integración end-to-end (sink HTTP local con `HttpServer` de puerto efímero, casos 200 y 500). Suite completa en verde: **86 tests**.
6. **Bloque 11: Carga Masiva de Envíos por Lotes vía CSV (Batch Ingestion)** (implementado, pendiente de commit, 2026-08-02):
   - Spec de diseño: commit `ad9b0e6` (`docs/superpowers/specs/2026-08-02-bloque11-batch-ingestion-csv-design.md`).
   - Migración Flyway `V5` (tablas `batch_imports` y `batch_import_errors`, `cliente_id` NULL `ON DELETE SET NULL`, errores `ON DELETE CASCADE`, `utf8mb4`), entidades `BatchImport`/`BatchImportEstado`/`BatchImportError` y repositorios (Java puro, sin Lombok).
   - `CsvEnvioParser` (`@Component`, `maxLineLength` vía `app.batch.max-line-length`): parsing streaming con OpenCSV 5.9, cabecera saltada con BOM, columnas `codigo,estado,destinatario,origen,destino,peso,contenido,observaciones`, filas cortas rellenadas a 8, límites >255 por campo, mensajes de error auditables.
   - `CsvBatchImportService`: worker `@Async("batchTaskExecutor")` sin `@Transactional`, deduplicación local + `existsByCodigoUnico`, chunks de 100 con `BatchImportPersistenceService.procesarChunk` (`REQUIRES_NEW` + `@CacheEvict("envios.dashboard")`), `MaxRowsExcedidoException` (200 000 filas), limpieza del temporal en `finally`.
   - API admin `POST /api/v1/admin/imports/csv` (`@PreAuthorize("hasRole('ROLE_ADMIN')")`): 202 + `batch_id`, validaciones 400 (sin fichero, vacío, no `.csv`, >5MB), 404 cliente inexistente, copia síncrona a `app.batch.tmp-dir`; GET de estado y de errores del lote. `GlobalExceptionHandler` con `MissingServletRequestPartException` → 400.
   - Auditoría en `batch_import_errors` (línea, código, mensaje) y estado/progreso en `batch_imports` (`PENDIENTE/EN_PROCESO/COMPLETADO/COMPLETADO_CON_ERRORES/FALLIDO`). La ingesta **no** publica `EstadoEnvioActualizadoEvent` ni dispara webhooks.
   - Tests: 10 parser + 8 worker + 10 controller + 6 integración. Suite completa en verde: **122 tests** (`BUILD SUCCESS` verificado en contenedor Docker).
   - Consolidación final del Bloque 11 (19 ficheros pendientes commiteados en `main`): commit `fa3fc76`.
7. **Bloque 12: Motor de Generación de Documentación PDF, Etiquetas Térmicas (100×150 mm) y Códigos QR/Code128** (completado, 2026-08-02):
   - Spec de diseño: commit `242c099` (`docs/superpowers/specs/2026-08-02-bloque12-pdf-etiquetas-barcodes-design.md`).
   - Dependencias OpenPDF 1.3.40 + ZXing core/javase 3.5.3 y migraciones `V6` (tabla `documentos_generados` con FKs `ON DELETE CASCADE` y `ON DELETE SET NULL`) y `V7` (`batch_id` en `envios_tracking`, FK `ON DELETE SET NULL` + índice, vínculo envíos↔lote rellenado en `BatchImportPersistenceService.procesarChunk`): commit `1649ca8`.
   - Modelo: enum `TipoDocumento` (`ETIQUETA_TERMICA/ETIQUETAS_LOTE/MANIFIESTO_CARGA`), entidad `DocumentoGenerado` con `@PrePersist` (peso/creación) y `DocumentoGeneradoRepository` (histórico, `findByTipoOrderByFechaCreacionDesc`): commit `eda9c4b`.
   - `PesoUtil.parsear(String)` (coma→punto, tolerante a sufijos, inválidos→`OptionalDouble.empty`), `BarcodeService` (ZXing Code128 + QR 250×250 → `BufferedImage` + PNG, modo seguro: `Margin 0/1`, fallback `ISO-8859-1` a `UTF-8`), `EtiquetaPdfGenerator` (283.46×425.2 pt = 100×150 mm, campos del envío + code128 + QR con tracking URL), `ManifiestoPdfGenerator` (A4 595.28×841.89 pt, tabla 5 columnas, totales de peso vía `PesoUtil` con «—» para inválidos, firma de despacho). Todos con `writer.setCompressionLevel(PdfStream.NO_COMPRESSION)` para auditoría de contenido en crudo: commits `4a79349`, `4f11e25`, `932d525`, `39917c6`.
   - `DocumentoPdfService`: generación en memoria (cero I/O en disco), streaming de etiquetas de lote al `OutputStream` con tope `app.pdf.max-pages` (default 5000) → 400 `BadRequestException`, auditoría persistida en `documentos_generados` (tipo/referenciaId/nombreArchivo/pesoBytes/usuario), props `app.pdf.*` (`enabled`, `max-pages`, `tracking.base-url`, `qr.size`, `barcode.width/height`): commit `356d6b6`.
   - API admin `/api/v1/admin/documentos` (`ROLE_ADMIN`): `GET /envios/{codigo}/etiqueta` (inline), `GET /lotes/{batchId}/etiquetas` (attachment, streaming `void` + `HttpServletResponse`, reset del response ante 400/404 para devolver JSON), `GET /lotes/{batchId}/manifiesto` (attachment A4), `GET /documentos?tipo=` (auditoría JSON sin campos sensibles): commit `90608cb`.
   - Tests: 6 PesoUtil + 5 BarcodeService + 2 EtiquetaPdfGenerator + 3 ManifiestoPdfGenerator + 7 DocumentoPdfService + 8 DocumentosController + 4 integración end-to-end. Suite completa en verde: **159 tests** (`BUILD SUCCESS` verificado en contenedor Docker con MySQL/Redis).
8. **Bloque 13: Evidencia Digital de Entrega (POD) con Firma Digital y GPS** (en curso, 2026-08-02):
   - Spec de diseño: commits `9e889d4` y `edb40cc` (`docs/superpowers/specs/2026-08-02-bloque13-pod-evidencia-entrega-design.md`); plan de implementación: commit `ee34dec` (`docs/superpowers/plans/2026-08-02-bloque13-pod-evidencia-entrega.md`).
   - Migración Flyway `V8` (tabla `entregas_evidencia`: `envio_id UNIQUE` con FK `ON DELETE CASCADE`, firma `LONGTEXT`, `latitud DECIMAL(10,8)`, `longitud DECIMAL(11,8)`, `fecha_entrega` con `DEFAULT CURRENT_TIMESTAMP`), entidad `EntregaEvidencia` (LAZY, `@PrePersist`) y `EntregaEvidenciaRepository` (`findByEnvioId`/`existsByEnvioId`): commit `fa187d7`.
   - `EntregaValidator` (Base64 + magic bytes PNG `0x89PNG`, tamaño máx. firma 5 MB, rangos GPS) + DTOs `RegistrarEntregaRequest` y `EntregaEvidenciaDto`: commit `c32c914`.
   - `EntregaEvidenciaService` transaccional (`registrarEntrega` valida → persiste → `actualizarEstado(codigo,"ENTREGADO")` → `crearEvento` → `@CacheEvict` dashboard; 409 duplicado / 404 inexistente; `obtenerEntrega` read-only): commit `0a45138`.
   - `SecurityConfig`: `/api/v1/deliveries/**` con `authenticated()` + HTTP Basic (los tests de redirect de la API admin se ajustaron a `Accept: text/html`; sin él devuelven 401, más correcto para REST): commit `6893747`.
   - `EntregaEvidenciaController` (`POST/GET /api/v1/deliveries/{codigo}/pod`, 201/200, `@PreAuthorize` ADMIN/OPERADOR): commit `fdaed17`. Corrección sobre el plan: `CustomAccessDeniedHandler` ahora escribe la respuesta 400 JSON y `GlobalExceptionHandler` delega el `AccessDeniedException` en él (antes el catch-all lo convertía en 500): commit `fdaed17`. Las coordenadas se mapean con `BigDecimal` en vez de `Double` (Hibernate 6 rechaza `scale` sobre tipos flotantes): commit `7c7ec4d`.
   - Tests: 11 EntregaValidator + 6 EntregaEvidenciaService + 8 EntregaEvidenciaController + 3 CustomAccessDeniedHandler + 3 integración end-to-end (POD → `ENTREGADO` + notificación `OMITIDO_SIN_DESTINATARIO` + evento de timeline; 409 duplicado; 404 sin evidencia).
9. **Sprint de Optimización y Resiliencia** (post k6, 2026-07-31):
   - `commons-pool2` añadido al pom para activar el pool de conexiones Lettuce (sin él, las props de pool se ignoraban): commit de Task 1 (`bd56610`).
   - Tuning de pools: HikariCP max=25/min=5/connection-timeout=20000 (base y prod reconciliado, conservando hardening); pool Lettuce max-active=30/max-idle=15/min-idle=5/max-wait=2000ms; `spring.data.redis.timeout=3000ms`; save/flush mode explícitos manteniendo namespace `monteastur:session`: commit de Task 2 (`b44ca79`).
   - Nuevo test de integración `EnvioTrackingCacheIntegrationTest` (populate/evict/TTL de `envios.tracking` + verificación del pool Lettuce vía `LettuceConnectionFactory.getClientConfiguration()`): commit de Task 3 (`2d21e78`). Corrección sobre el plan: Spring Boot 3.3.5 no registra un bean `GenericObjectPoolConfig`; el assert usa la client configuration del factory (4/4 tests OK).
   - Corregido el `REPORT.md` de k6 (afirmación obsoleta: tracking ya usaba caché desde `4407c07`): commit de Task 4 (`f77a9bc`).
   - Verificado: `mvn clean test` en verde (63 tests).
10. **Bloque 14: Pipeline CI/CD Enterprise (GitHub Actions + Docker & healthchecks)** (completado, 2026-08-02):
    - Spec de diseño: commits `b7421b4` y `57a4361` (`docs/superpowers/specs/2026-08-02-bloque14-cicd-enterprise-design.md`); plan de implementación: commit `e13888d` (`docs/superpowers/plans/2026-08-02-bloque14-ci-cd-enterprise-plan.md`).
    - Maven Wrapper 3.9.9 (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) + `.gitattributes` (LF para `mvnw`, CRLF para `mvnw.cmd`): commit `e76b39d`.
    - Workflow `.github/workflows/ci.yml` con `permissions: contents: read` y `concurrency` por `github.ref` (`cancel-in-progress: true`): job `test` (MySQL 8 `envios_paraguay_cms_test` + Redis 7-alpine con healthchecks, `setup-java@v4` Temurin 17 + cache Maven, `chmod +x mvnw`, `./mvnw clean test -B` con `SPRING_PROFILES_ACTIVE=test`, upload de Surefire con `if: always()`) → job `docker-build` (`needs: test`, solo en `push` a `main`/`develop`): commit `5bea243`.
    - Job `docker-build`: buildx + `docker/build-push-action@v5` (`load: true`, tag `envios-paraguay-cms:latest`) y smoke test de arranque en frío: contenedor con `--network host`, envs de prod (`DB_USERNAME/DB_PASSWORD/ADMIN_USERNAME/ADMIN_PASSWORD/REDIS_HOST/APP_NOTIFICATION_MAIL_ENABLED=false`), loop 30×5 s hasta que `curl /actuator/health` devuelva HTTP 200 con `"UP"` (servicios efímeros `mysql` + `redis` + `mailpit`).
    - **Corrección deliberada al spec (hallada en la verificación local):** el endpoint agregado `/actuator/health` incluye el `MailHealthIndicator`; sin servidor SMTP responde `DOWN` y el smoke falla aunque la app esté sana. Fix: el job `docker-build` añade el servicio `axllent/mailpit` (publica `1025:1025`, healthcheck `wget -q -O /dev/null http://localhost:8025/readyz`), replicando el rol de Mailpit en `docker-compose.yml` de prod. Verificado empíricamente que la imagen incluye `wget` y el healthcheck pasa.
    - Verificación local completa: suite en contenedor Maven Linux con `./mvnw clean test -B` → **BUILD SUCCESS, 190 tests, 0 fallos**; imagen construida; arranque en frío con `/actuator/health` → **`UP` en el intento 2** (contenedores efímeros `smoke-mysql`/`smoke-redis`, puerto 18080, `SPRING_MAIL_HOST=monteastur-mailpit`); Flyway V1–V8 aplicadas con `success=1` en el smoke DB; contenedores efímeros limpiados.
    - **Validado en GitHub Actions (cierre):** push del bloque a `origin/main` y run `30769845155` (commit `9c57351`) → **`conclusion=success`** en ambos jobs: `Test suite (MySQL 8 + Redis 7)` con **217 tests / 0 fallos** (`Tests run: 217, Failures: 0, Errors: 0, Skipped: 0`) y `Docker image build + smoke test` con `/actuator/health` → `{"status":"UP"}` (HTTP 200 en el intento 4, `SMOKE TEST PASSED`), MySQL/Redis/Mailpit efímeros levantados y limpiados por el runner.
11. **Bloque 15: Portal Público de Rastreo & Dashboard Interactivo de Clientes (Tailwind)** (completado, 2026-08-02):
    - Spec de diseño: commits `48b7956`/`1efc0b8` (`docs/superpowers/specs/2026-08-02-bloque15-portal-tracking-dashboard-design.md`); plan de implementación: commit `72d4bc3` (`docs/superpowers/plans/2026-08-02-bloque15-portal-tracking-dashboard-plan.md`).
    - DTOs web Java puro (`PublicTrackingView`, `EventoView`, `EvidenciaView`, `EntregaView`, `ClientDashboardView`, `EnvioResumenView`; listas `ArrayList` para el serializador Redis `NON_FINAL`): commit `0a5421e`.
    - `PublicTrackingService.cargarPagina(codigo)` (`@Cacheable "envios.tracking.pagina"`, TTL 5 min, `unless result==null`) con timeline + POD solo si `ENTREGADO`; `ClientDashboardService.cargarDashboard(clienteId)` (`@Cacheable "envios.cliente.dashboard"`, TTL 1 min) con métricas `PesoUtil.parsear` (pesos inválidos ignorados): commits `6d57098` y `1718989` (6 + 2 unit tests).
    - Caches Redis + `@CacheEvict` ampliados a los tres caches en los puntos de mutación (`EnvioTrackingService.guardar/actualizarEstado/eliminar`, `EntregaEvidenciaService.registrarEntrega`, `BatchImportPersistenceService.procesarChunk`): commit `7fdedbe`.
    - Plantillas Tailwind CDN (fragmentos `public-head`, `tracking-search` con lector QR `html5-qrcode`, `tracking-result` con stepper de 6 pasos + POD con firma, `tracking-404`, `cliente/panel` con logout POST y métricas); eliminados `tracking.html`/`en/tracking.html`: commit `4b09243`.
    - `TrackingWebController` (buscador PRG + 404 personalizado), `ClientDashboardController` (panel + etiqueta PDF con ownership 200/403/404), excepciones `TrackingNoEncontradoException`/`ForbiddenException`, limpieza de `PublicController`/`ClienteController`, logout POST: commits `11849e9` y `1346527`.
    - **Correcciones sobre el plan (verificación empírica):** (a) el `@ResponseStatus` de las excepciones NO aplica cuando hay un `@ExceptionHandler` que las captura → se añadió `@ResponseStatus` a todos los handlers MVC de `GlobalExceptionHandler` y al handler local de `TrackingWebController` (la rama web devuelve ahora 400/403/404/409/500 reales; la rama REST intacta); (b) en `@SpringBootTest` + `@AutoConfigureMockMvc`, Spring Security reemplaza la sesión de MockMvc (los `sessionAttr` no llegan al controller) → el test de integración usa `@AutoConfigureMockMvc(addFilters = false)` (las rutas son `permitAll`; la seguridad ya está cubierta por los `@WebMvcTest`).
    - Test de integración E2E `PortalTrackingDashboardIntegrationTest` (9 tests: rutas web, POD, 404, ownership PDF, caché Redis con TTL 1–300 s): commit `8be0aa9`.
    - Suite completa en contenedor Maven Linux: **BUILD SUCCESS, 217 tests, 0 fallos**.
    - **Validado en GitHub Actions (cierre):** mismo run `30769845155` de CI/CD (ver Bloque 14); el pipeline completo pasó en la nube sobre el estado final de `main` con el Bloque 15 incluido.

12. **Theme Switcher Dark/Light + Pulido Visual + Páginas Admin Imports/Documentos** (completado, 2026-08-03):
    - Spec de diseño: commit `010749b` (`docs/superpowers/specs/2026-08-03-theme-switcher-pulido-visual-spec.md`); plan de implementación: commit `9135ce2` (`docs/superpowers/plans/2026-08-03-theme-switcher-pulido-visual.md`).
    - `design-system.css` con tokens `:root[data-theme="dark"]` (por defecto) y `[data-theme="light"]` (Pristine Quartz), `theme-toggle.js` (toggle localStorage + `prefers-color-scheme` inicial + anti-FOUC inline en los `<head>`), `theme-ui.css`; vínculo en heads públicos y admin (12 templates). `prefers-reduced-motion` respetado.
    - `luxury-core.css`/`style.css`/`admin.css` re-mapeados a los tokens del design-system (glass, superficies, texto sobre acento con `--text-on-accent`, glow) y páginas admin nuevas `imports` y `documentos` con carga CSV asíncrona (polling) y etiquetas/manifiestos PDF: commits `a62fb13..1318e47` (13 commits).
    - **Rebrand aprobado por el usuario (decisión "Rebrand completo verde+naranja")**: sustituye la identidad obsidiana `#09090b` + `#d4762a` por **verde bosque Asturias `#0D2319` + naranja Paraguay `#E67E22`**, sin negros planos en ningún tema. Tokens dark nuevos (`--bg-body-gradient` `linear-gradient(180deg,#0B1E16,#123524,#0E291C)`, `--bg-surface:#153C2D`, `--bg-card:#1B4D3B`, `--bg-card-glass:rgba(21,60,45,.85)`, texto `#F4F7F5/#A3C9B8/#7BA897`, bordes `rgba(163,201,184,.2/.35)`, `--shadow-card`, `--shadow-glow-orange`, radios y fuente `Plus Jakarta Sans`); `body { background: var(--bg-body-gradient) !important; ... }`. Light: acento `#E67E22`/hover `#C65F12`. Barrido de hardcodes en `luxury-core`, `style`, `admin.css`, `admin-theme`, `hero/tracking/operaciones/casa/contacto/reservas-premium`, `admin-{tracking,sidebar,login,evidencias,client-panel}`, `public-head.html` (paleta Tailwind brand + navbar/footer con `var(--surface-header)`/`var(--bg-surface)`), headings inline CMS → `var(--accent-color)`: commit `03d5c34`.
    - **Gate `/casa` (brief):** `<main class="casa-page-main">` con `padding-top:100px` en `lacasa.html` y `en/lacasa.html` (regla en `casa-premium.css`) para no quedar tapado por la cabecera fija.
    - Verificación: greps de identidad vieja en CSS/plantillas **0 restos**; suite completa **233 tests, 0 fallos, BUILD SUCCESS** en contenedor Docker (MySQL/Redis); smoke en `http://127.0.0.1:8081` (contenedor `monteastur-smoke-ui` recreado) con `/actuator/health` → **UP** (db, redis 7.4.10, mail/mailpit, ping, liveness/readiness) y rutas `/`, `/casa` (con `casa-page-main`), `/login` (con `theme-toggle.js` + anti-FOUC), `/tracking` → 200; `/admin/*` → 401 sin login.

---

## 🚀 Guía de Arranque Rápido para Desarrolladores

### 1. Requisitos previos

- Tener instalado **Docker** y **Docker Compose**.
- Java 17 + Maven (solo si se compila localmente; también se puede compilar con el contenedor `maven:3.9-eclipse-temurin-17`).

### 2. Variables de Entorno

Asegúrate de definir las variables de entorno críticas antes de levantar el perfil de producción (`prod`), especialmente:

- `DB_USERNAME`
- `DB_PASSWORD`
- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`

En local, la mayoría están en `.env` (no versionado). El arranque valida su presencia en el perfil `prod`.

### 3. Comprobación y Arranque

- **Validar sintaxis de Nginx:**

  ```powershell
  docker compose run --rm nginx nginx -t
  ```

  Debe devolver `syntax is ok` y `test is successful`.

- **Levantar el stack completo:**

  ```powershell
  docker compose up -d --build
  ```

  o usar el script `start-all.ps1`.

- **Compilar (sin JDK local):**

  ```powershell
  docker run --rm -v "${PWD}:/app" -w /app -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 mvn clean compile -q
  ```

---

## 📌 Estado Git Actual

- **Rama:** `main` (estable).
- **HEAD:** `03d5c34` (`feat(ui): rebrand a verde bosque asturias + naranja paraguay sin negros planos`). Pendiente de push a `origin/main`.
- **Bloque 16 (Theme Switcher + Pulido Visual + Rebrand):** spec `010749b`, plan `9135ce2`, 13 commits de implementación `9135ce2..1318e47` y rebrand `03d5c34`. Suite en verde (**233 tests, BUILD SUCCESS** en contenedor Docker) y smoke en `:8081` con health UP y assets del rebrand servidos.
- **Migraciones Flyway aplicadas:** V1–V8 (V8 crea `entregas_evidencia` con `envio_id UNIQUE`, FK `ON DELETE CASCADE`, firma PNG `LONGTEXT` y coordenadas `DECIMAL(10,8)`/`DECIMAL(11,8)`).
- **Suite completa:** **233 tests** en verde (`BUILD SUCCESS` verificado en contenedor Docker con MySQL/Redis). Smoke test de la imagen en frío: `/actuator/health` → `UP`.
- Flujo de ramas: `main` = estable, `develop` = integración, `feature/*` = mejoras concretas.
- No hacer push ni merge sin confirmación explícita del usuario.

---

## 📝 Reglas de Trabajo

1. No empezar el proyecto desde cero.
2. No cambiar arquitectura sin explicar riesgos.
3. No mezclar demasiadas mejoras en una sola tarea.
4. Antes de modificar archivos: `git status` y `git branch`.
5. Cambios pequeños y revisables.
6. Mantener coherencia español/inglés.
7. Probar antes de sugerir commit.
8. No hacer push ni merge sin confirmación.
