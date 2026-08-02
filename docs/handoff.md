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
8. **Sprint de Optimización y Resiliencia** (post k6, 2026-07-31):
   - `commons-pool2` añadido al pom para activar el pool de conexiones Lettuce (sin él, las props de pool se ignoraban): commit de Task 1 (`bd56610`).
   - Tuning de pools: HikariCP max=25/min=5/connection-timeout=20000 (base y prod reconciliado, conservando hardening); pool Lettuce max-active=30/max-idle=15/min-idle=5/max-wait=2000ms; `spring.data.redis.timeout=3000ms`; save/flush mode explícitos manteniendo namespace `monteastur:session`: commit de Task 2 (`b44ca79`).
   - Nuevo test de integración `EnvioTrackingCacheIntegrationTest` (populate/evict/TTL de `envios.tracking` + verificación del pool Lettuce vía `LettuceConnectionFactory.getClientConfiguration()`): commit de Task 3 (`2d21e78`). Corrección sobre el plan: Spring Boot 3.3.5 no registra un bean `GenericObjectPoolConfig`; el assert usa la client configuration del factory (4/4 tests OK).
   - Corregido el `REPORT.md` de k6 (afirmación obsoleta: tracking ya usaba caché desde `4407c07`): commit de Task 4 (`f77a9bc`).
   - Verificado: `mvn clean test` en verde (63 tests).

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
- **HEAD:** `fa3fc76` (consolidación Bloque 11). **Working tree 100% limpio** — sin cambios pendientes. Bloque 11 y Bloque 12 completados y commiteados.
- **Migraciones Flyway aplicadas:** V1–V7 (V7 añade `batch_id` a `envios_tracking` con FK `ON DELETE SET NULL` para el vínculo envíos↔lote del Bloque 12).
- **Suite completa:** **159 tests** en verde (`BUILD SUCCESS` verificado en contenedor Docker con MySQL/Redis).
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
