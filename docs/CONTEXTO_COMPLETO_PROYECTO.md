# CONTEXTO COMPLETO DEL PROYECTO — MONTEASTUR ENVIOS

> Documento de contexto integral para que un modelo de IA (Gemini / Google AI Studio) entre en contexto total del proyecto `Envios_Paraguay_CMS`. Generado el 2026-08-07. Última revisión del código: HEAD `3713cbf`.

---

## 1. Descripción General

**MONTEASTUR ENVIOS** es una plataforma logística profesional full-stack para la gestión de **envíos internacionales España (Asturias) ↔ Paraguay**. Combina:

- **Web pública** (server-side Thymeleaf) con tracking de envíos en tiempo real por código único.
- **Panel de administración** (CMS) para gestión de envíos, estados, evidencias, reservas, contactos, textos legales, importación masiva CSV y generación de documentos PDF.
- **Panel de cliente** seguro con sesión propia (login por email/contraseña BCrypt).
- **Dashboard React SPA** (analytics, gráficos, PWA instalable, push notifications, modo offline).
- **API REST `/api/v1/`** para tracking público, cliente y administración.

La identidad visual es el **sistema de diseño "Asturias-Paraguay"**: verde bosque profundo de Asturias (`#0D2319`, `#153C2D`, `#1B4D3B`) + acento cálido de Paraguay (`#E67E22`), con theme switcher oscuro/claro. El acento corporativo histórico del proyecto es `#d4762a`.

---

## 2. Stack Tecnológico Real

### Backend
| Tecnología | Versión / Detalle |
| --- | --- |
| **Java** | Compilado para **Java 17** bytecode (`<java.version>17</java.version>` en `pom.xml`). Build en contenedor `maven:3.9-eclipse-temurin-25` (la imagen ejecuta el código en JDK 25). |
| **Spring Boot** | 3.5.16 (parent en `pom.xml`; el README publicitaba 3.3.5 — la fuente de verdad es el `pom.xml`). |
| **Spring Security** | Sesión + CSRF, RBAC con roles desde BD (`users`/`roles`/`user_roles`), BCrypt, HTTP Basic para `/api/v1/deliveries/**`. |
| **Spring Data JPA + Hibernate** | Persistencia, `open-in-view=false`, `default_batch_fetch_size=20`. |
| **Flyway** | Migraciones `V1`–`V10` en `src/main/resources/db/migration/`. |
| **MySQL 8** | Base de datos relacional (perfil prod vía `SPRING_DATASOURCE_URL`/`DATABASE_URL`, pensado para Render/MySQL 8 y TiDB Cloud). |
| **Redis 7** | Sesiones distribuidas (`spring-session-data-redis`, namespace `monteastur:session`) + caches (`envios.tracking`, `envios.tracking.pagina`, `envios.cliente.dashboard`, `envios.dashboard`). Pool Lettuce con `commons-pool2`. |
| **Spring Mail** | JavaMailSender (SMTP), Mailpit en desarrollo, timeouts 5s/10s/10s. |
| **Springdoc OpenAPI** | Swagger UI en `/api/v1/swagger-ui.html` (try-it-out deshabilitado). |
| **OpenCSV 5.9** | Parsing CSV de importación masiva. |
| **ZXing 3.5.3** | Códigos Code128 y QR. |
| **OpenPDF 1.3.40** | Generación de PDFs (etiquetas térmicas y manifiestos). |
| **Spring Boot Actuator + Micrometer** | `/actuator/health`, `/actuator/info`, `/actuator/prometheus`, probes liveness/readiness en prod. |
| **Logback** | Logs `logs/monteastur.log` + `logs/monteastur-error.log`, rotación diaria, retención 30 días. |
| **HikariCP** | Pool `EnviosHikariPool` (dev) / `EnviosProdPool` (prod), max=25/min=5, leak detection. |

### Frontend
| Tecnología | Detalle |
| --- | --- |
| **Thymeleaf** | Motor de plantillas MVC (todas las páginas públicas y paneles CMS/cliente). |
| **React 19 + Vite 8** | Dashboard SPA en `frontend-react/` (compilado a `src/main/resources/static/react-dashboard/`). |
| **React Router 7** | Rutas del SPA (`/dashboard`, `/dashboard/envio/:codigo`). |
| **Recharts 3** | Gráficos de analytics. |
| **Axios** | Cliente HTTP del SPA (usa la sesión Spring Security, sin JWT). |
| **vite-plugin-pwa** | PWA instalable, service worker (Workbox), precache ~12 entradas. |
| **xlsx** | Exportación de datos. |
| **Vitest + Testing Library** | 15 tests unitarios del SPA. |
| **Playwright** | 9 tests E2E. |
| **Bootstrap 5** | Base UI histórica (las vistas actuales usan sobre todo el `design-system.css` propio). |

### DevOps / Infraestructura
| Tecnología | Detalle |
| --- | --- |
| **Docker + Docker Compose** | Servicios: `db` (MySQL 8), `app`, `nginx`, `certbot`, `prometheus`, `grafana`, `uptime-kuma`, `redis`, `mailpit`. Volúmenes: `mysql_data`, `uploads_data`, `logs_data`, `certbot_www`. |
| **Nginx** | Reverse proxy, SSL termination, security headers, gzip, límite subida 10MB. |
| **Prometheus + Grafana + Uptime Kuma** | Monitoring auto-hospedado. |
| **GitHub Actions** | CI (`ci.yml`) + CD (`deploy.yml`, `deploy-prod.yml`). |
| **Render.com** | Blueprint `render.yaml` (Web Service docker free, MySQL 8 / Redis Upstash externo). |
| **Let's Encrypt / Certbot** | Certificados SSL. |

---

## 3. Arquitectura

Modelo **híbrido MVC + REST API + SPA** con capas separadas:

1. **Controllers** → vistas Thymeleaf o JSON (`controller/`, `controller/web/`, `controller/api/`).
2. **Services** → lógica de negocio y transacciones (`service/`, `service/batch/`, `service/analytics/`, `service/pdf/`, `service/web/`).
3. **Repositories** → Spring Data JPA.
4. **Model** → entidades JPA en Java puro (sin Lombok).
5. **Eventos de dominio + listeners asíncronos** → `event/EstadoEnvioActualizadoEvent`, `listener/NotificacionEventListener`, `listener/WebhookEventListener`.

Flujo de datos:

```
Navegador → Nginx (:80/:443) → Spring Boot (Tomcat, :8080)
                                    ├─ Thymeleaf MVC (plantillas)
                                    ├─ React SPA (/react-dashboard)
                                    ├─ REST API (/api/v1)
                                    └─ Actuator (/actuator/health)
Spring Boot → MySQL 8 (Flyway V1-V10) + Redis 7 (sesiones/cache) + Uploads
```

**Decisiones clave:**
- **Sin JWT.** El SPA React reutiliza la cookie `JSESSIONID` (HttpOnly, Secure en prod) de Spring Security. CSRF **desactivado para `/api/**`**, **activo para formularios Thymeleaf**. Documentado en `SecurityConfig.java`.
- **`@TransactionalEventListener(AFTER_COMMIT)`** para eventos de dominio; los listeners `@Async` ejecutan bajo `Propagation.REQUIRES_NEW` en pools dedicados (`notifTaskExecutor`, `webhookTaskExecutor`, `batchTaskExecutor`).
- **`open-in-view=false`** — cualquier acceso LAZY fuera de transacción requiere `@EntityGraph` o consulta dedicada.
- **Firma digital HMAC-SHA256** para webhooks outbound (hex lowercase con `HexFormat`, header `X-Signature-256`).
- **Clients HTTP robustos** con `RestClient` y timeouts estrictos (webhooks: 2s connect / 5s read).
- **`BootstrapPropertyEnvironmentPostProcessor`**: normaliza la URL de BD anteponiendo `jdbc:` si viene sin prefijo (`mysql://...` → `jdbc:mysql://...`). Lee `SPRING_DATASOURCE_URL` (preferida) o `DATABASE_URL` (fallback) y la publica como primer property source.
- **`MonteasturApplication.validateEnvironment()`**: en perfil `prod` aborta el arranque con `IllegalStateException` si faltan `DB_USERNAME`/`DB_PASSWORD`.

---

## 4. Estructura del Proyecto

```
Envios_Paraguay_CMS/
├── pom.xml                        # Spring Boot 3.5.16, Java 17, deps (opencsv, zxing, openpdf, springdoc)
├── README.md                      # Documentación principal del proyecto
├── docker-compose.yml             # Stack completo (db, app, nginx, certbot, prometheus, grafana, uptime-kuma, redis, mailpit)
├── Dockerfile                     # Build multi-etapa + runtime eclipse-temurin:25-jre, HEALTHCHECK con curl
├── render.yaml                    # Blueprint Render.com (Web Service docker, free tier)
├── nginx/                         # conf.d/local.conf, monteastur.conf, examples/production-example.conf, ssl/
├── scripts/                       # deploy, rollback, backup, restore, smoke tests, healthchecks (.sh y .ps1)
├── docs/                          # 60+ documentos de guías, auditorías, runbooks (ver §11)
├── backup/                        # Backups de BD (db/) y uploads (uploads/)
├── logs/                          # Logs de ejecución (NO en Git)
├── uploads/                       # Archivos subidos (NO en Git)
├── frontend-react/                # SPA React (source)
│   ├── src/components/            #   StatsCard, StatusBadge, SearchBar, EmptyState, Timeline, AnalyticsKPIs, etc.
│   ├── src/pages/                 #   AdminDashboard, LoginPage, ProtectedRoute, ShipmentDetailPage
│   ├── src/context/               #   AuthContext, NotificationContext
│   ├── src/hooks/                 #   usePushNotifications, useOfflineSync, useOnlineStatus, usePolling, usePWAInstall
│   ├── src/services/              #   api.js, offlineCache.js, offlineQueue.js, exportUtils.js, dateUtils.js
│   └── e2e/ tests/                #   Playwright specs
├── src/main/java/com/monteastur/envios/
│   ├── MonteasturApplication.java
│   ├── config/                    # SecurityConfig, WebMvcConfig, ReactConfig, AsyncConfig, RedisConfig, SessionConfig,
│   │                              #   DataInitializer, DefaultUsersInitializer, OpenApiConfig, RBACAccessLogger,
│   │                              #   RBACAuditorConfig, CacheAuditErrorHandler, WebhookHttpConfig, BatchImportHttpConfig,
│   │                              #   BootstrapPropertyEnvironmentPostProcessor, BootstrapPropertyNormalizer,
│   │                              #   SpaForwardController
│   ├── controller/                # AdminController, PublicController, ClienteController, LoginController, GlobalExceptionHandler
│   ├── controller/web/            # TrackingWebController, ClientDashboardController
│   ├── controller/api/            # AdminApiController, AnalyticsRestController, ClienteApiController, TrackingApiController,
│   │                              #   ReservaApiController, ReservaPublicApiController, WebhookConfigController,
│   │                              #   BatchImportController, DocumentosController, EntregaEvidenciaController,
│   │                              #   PushSubscriptionController
│   ├── service/                   # EnvioTrackingService, EventoTrackingService, EvidenciaEnvioService, ClienteService,
│   │                              #   ReservaService, EmailService, EntregaValidator, EntregaEvidenciaService,
│   │                              #   DocumentoPdfService, CsvBatchImportService, WebhookDispatchService,
│   │                              #   WebhookSignature, WebhookPayloadBuilder
│   ├── service/web/               # PublicTrackingService, ClientDashboardService
│   ├── service/batch/             # BatchImportPersistenceService, CsvEnvioParser, CsvEnvioRow, CsvLineConsumer, CsvImportLineError
│   ├── service/analytics/         # AnalyticsDashboardService, AnalyticsQueryService
│   ├── service/pdf/               # EtiquetaPdfGenerator, ManifiestoPdfGenerator, BarcodeService, PesoUtil
│   ├── repository/                # 15 repositorios Spring Data JPA
│   ├── model/                     # 19 entidades JPA + enums (BatchImportEstado, TipoDocumento)
│   ├── dto/api/                   # TrackingDto, PublicTrackingDto, AdminEnvioResumenDto, ClienteEnvioResumenDto, EventoDto,
│   │                              #   EvidenciaDto, EntregaEvidenciaDto, ErrorDto, WebhookConfigDto, WebhookConfigRequest,
│   │                              #   BatchImportResponseDto, BatchImportErrorDto, DocumentoGeneradoDto, ReservaAdminDto,
│   │                              #   RegistrarEntregaRequest, CrearReservaPublicRequest, ActualizarReservaRequest,
│   │                              #   ActualizarEstadoRequest, PushSubscriptionRequest
│   ├── dto/web/                   # PublicTrackingView, EventoView, EvidenciaView, EntregaView, ClientDashboardView, EnvioResumenView
│   ├── dto/analytics/             # AnalyticsSummaryDto, KpiDto, EstadoCountDto, RutaDto, TendenciaDto, WebhookPuntoDto
│   ├── event/                     # EstadoEnvioActualizadoEvent
│   ├── listener/                  # NotificacionEventListener, WebhookEventListener
│   ├── exception/                 # TrackingNoEncontradoException, ResourceNotFoundException, ForbiddenException,
│   │                              #   BadRequestException, ConflictException
│   └── security/                  # CustomAccessDeniedHandler
├── src/main/resources/
│   ├── application.properties     # Config dev/default (ver §7)
│   ├── application-prod.properties# Perfil producción (ver §7)
│   ├── db/migration/              # V1__initial_schema.sql … V10__create_analytics_indexes.sql
│   ├── data/schema.sql            # Esquema de referencia (sincronizado con migraciones)
│   ├── logback-spring.xml
│   ├── META-INF/spring.factories
│   ├── templates/                 # Thymeleaf: home, lacasa, entorno, operaciones, reservas, contacto, aviso-legal,
│   │                              #   politica-cookies, error, tracking-search, tracking-result, tracking-404, login,
│   │                              #   en/ (versiones inglesas), cms/ (dashboard, tracking, tracking-form, reservas,
│   │                              #   imagenes, contactos, textos, imports, documentos), cliente/ (login, panel),
│   │                              #   fragments/ (header, header-en, footer, footer-en, admin-sidebar)
│   └── static/
│       ├── css/design-system.css  # ÚNICA hoja de estilos (tokens duales dark/light, ensamblado)
│       ├── js/                    # app.js, analytics.js, theme-toggle.js, vendor/ (lucide, chart.umd)
│       ├── img/                   # monteastur/ (branding, hero, operaciones), media/, demo-gallery/
│       ├── react-dashboard/       # SPA compilado (index.html + assets/)
│       └── uploads/               # Uploads de ejemplo
└── .github/workflows/             # ci.yml, deploy.yml, deploy-prod.yml
```

> ⚠️ **Regla del CSS**: `design-system.css` es la única hoja de estilos del proyecto (28 hojas legacy fueron eliminadas). Si se reconstruye, **nunca** ensamblar dos veces sobre el mismo archivo; reconstruir desde las 6 fuentes de `%TEMP%\opencode` (documentado en `docs/handoff.md`). No existe configuración Tailwind (verificado).

---

## 5. Modelo de Datos (Migraciones Flyway V1–V10)

| Migración | Contenido |
| --- | --- |
| **V1** | `clientes`, `envios_tracking`, `eventos_tracking`, `evidencias_envio`, `reservas`, `mensajes_contacto`, `imagenes`, `textos_legales` + índices estratégicos. |
| **V2** | Tablas RBAC: `users`, `roles`, `user_roles`. |
| **V3** | `notificaciones` (módulo de notificaciones automáticas por email). |
| **V4** | `webhooks_config` y `webhook_logs` (FKs `ON DELETE CASCADE`, `BOOLEAN`). |
| **V5** | `batch_imports` y `batch_import_errors` (`cliente_id` NULL `ON DELETE SET NULL`, errores `ON DELETE CASCADE`, `utf8mb4`). |
| **V6** | `documentos_generados` (FKs `ON DELETE CASCADE` / `ON DELETE SET NULL`). |
| **V7** | Columna `batch_id` en `envios_tracking` (FK `ON DELETE SET NULL` + índice). |
| **V8** | `entregas_evidencia` (POD): `envio_id UNIQUE` FK `ON DELETE CASCADE`, firma `LONGTEXT`, `latitud DECIMAL(10,8)`, `longitud DECIMAL(11,8)`, `fecha_entrega DEFAULT CURRENT_TIMESTAMP`. |
| **V9** | Columna `leido BOOLEAN NOT NULL DEFAULT FALSE` en `mensajes_contacto` (fix `Schema-validation: missing column [leido]`). |
| **V10** | Índices de las agregaciones SQL del BI dashboard (analytics). |

### Entidades JPA (19)
`Cliente`, `EnvioTracking`, `EventoTracking`, `EvidenciaEnvio`, `Reserva`, `MensajeContacto`, `Imagen`, `TextoLegal`, `Notificacion`, `WebhookConfig`, `WebhookLog`, `BatchImport`, `BatchImportError`, `DocumentoGenerado`, `EntregaEvidencia` + enums `BatchImportEstado` (`PENDIENTE/EN_PROCESO/COMPLETADO/COMPLETADO_CON_ERRORES/FALLIDO`) y `TipoDocumento` (`ETIQUETA_TERMICA/ETIQUETAS_LOTE/MANIFIESTO_CARGA`).

**Relaciones principales:**
- `Cliente 1—N EnvioTracking` (FK `cliente_id`)
- `EnvioTracking 1—N EventoTracking` / `1—N EvidenciaEnvio` / `1—0..1 EntregaEvidencia`
- `BatchImport 1—N BatchImportError`; `BatchImport 1—N EnvioTracking` (vía `batch_id`)
- `EnvioTracking 1—0..1 DocumentoGenerado` (referencia)
- RBAC: `users 1—N user_roles N—1 roles`

---

## 6. Rutas y Endpoints

### Web pública (Thymeleaf)
| Ruta | Descripción |
| --- | --- |
| `/`, `/en` | Home. |
| `/casa`, `/lacasa`, `/en/casa` | "La casa" (sección de la empresa). |
| `/entorno`, `/en/entorno` | Entorno. |
| `/operaciones`, `/en/operaciones` | Operaciones. |
| `/reservas`, `/en/reservas` (GET/POST) | Formulario de reservas/solicitudes. |
| `/contacto`, `/en/contacto` (GET/POST) | Formulario de contacto. |
| `/aviso-legal`, `/en/aviso-legal` | Texto legal. |
| `/politica-cookies`, `/en/politica-cookies` | Política de cookies. |
| `/tracking`, `/en/tracking` (GET/POST), `/tracking/{codigo}` | Buscador de tracking (PRG) + resultado con timeline. |
| `/login` | Login Spring Security (admin). |
| `/admin/login` | Alias de login admin. |

### Panel administración (CMS, requiere sesión admin)
| Ruta | Descripción |
| --- | --- |
| `/admin/dashboard` | Dashboard CMS. |
| `/admin/mensajesrecibidos` | Mensajes de contacto. |
| `/admin/reservas` + `POST /reservas/aprobar/{id}`, `/reservas/cancelar/{id}`, `/reservas/eliminar/{id}` | Gestión de reservas. |
| `/admin/imagenes` (GET/POST) + `POST /imagenes/eliminar/{id}` | Galería CMS. |
| `/admin/textos` (GET/POST) | Textos legales. |
| `/admin/tracking` | Listado de envíos. |
| `/admin/tracking/nuevo`, `/admin/tracking/editar/{id}`, `POST /tracking/guardar`, `POST /tracking/eliminar/{id}` | CRUD envíos. |
| `POST /admin/tracking/evidencia/{envioId}`, `POST /tracking/evidencia/toggle/{id}`, `POST /tracking/evidencia/eliminar/{id}` | Evidencias. |
| `/admin/imports` | Importación CSV. |
| `/admin/documentos` | Documentos/PDF generados. |

### Panel cliente
| Ruta | Descripción |
| --- | --- |
| `/cliente/login` (GET/POST), `/cliente/logout` (GET/POST) | Login/logout cliente (sesión propia). |
| `/cliente/panel` | Dashboard del cliente (métricas + tabla de envíos). |
| `/cliente/panel/envio/{codigo}/etiqueta` | Descarga de etiqueta PDF (con ownership 200/403/404). |

### API REST `/api/v1/`
| Endpoint | Seguridad | Descripción |
| --- | --- | --- |
| `GET /api/v1/tracking/{codigo}` | Pública | TrackingDto resumido. |
| `GET /api/v1/reservas/disponibilidad` | Pública | Disponibilidad. |
| `POST /api/v1/reservas` | Pública | Crear reserva pública. |
| `GET /api/v1/cliente/envios`, `GET /api/v1/cliente/envios/{codigo}` | Sesión cliente | Envíos propios (403 si ajeno). |
| `GET /api/v1/cliente/evidencias/{id}/archivo` | Sesión cliente | Archivo de evidencia. |
| `GET /api/v1/admin/envios` (paginado/filtros) | Sesión admin | Listado envíos: `?page&size&estado&codigo&sort`. |
| `GET /api/v1/admin/envios/{codigo}` | Sesión admin | Detalle completo. |
| `PUT /api/v1/admin/envios/{codigo}/estado` | Sesión admin | Cambiar estado (crea evento). |
| `GET /api/v1/admin/analytics/resumen`, `POST /api/v1/admin/analytics/refresh` | Sesión admin | KPIs y tendencias. |
| `GET/POST/DELETE /api/v1/admin/webhooks`, `DELETE /api/v1/admin/webhooks/{id}` | `ROLE_ADMIN` | CRUD webhooks (secretToken nunca expuesto). |
| `POST /api/v1/admin/imports/csv`, `GET /api/v1/admin/imports/{id}`, `GET /api/v1/admin/imports/{id}/errors` | `ROLE_ADMIN` (`@PreAuthorize`) | Importación CSV asíncrona (202 + `batch_id`). |
| `GET /api/v1/admin/documentos/envios/{codigo}/etiqueta` | `ROLE_ADMIN` | Etiqueta PDF inline. |
| `GET /api/v1/admin/documentos/lotes/{batchId}/etiquetas` | `ROLE_ADMIN` | Etiquetas de lote (streaming, tope `app.pdf.max-pages`). |
| `GET /api/v1/admin/documentos/lotes/{batchId}/manifiesto` | `ROLE_ADMIN` | Manifiesto A4. |
| `GET /api/v1/admin/documentos?tipo=` | `ROLE_ADMIN` | Auditoría de documentos (sin campos sensibles). |
| `GET/PUT/PATCH/DELETE /api/v1/admin/reservas...` | Sesión admin | CRUD reservas. |
| `POST /api/v1/deliveries/{codigo}/pod`, `GET /api/v1/deliveries/{codigo}/pod` | `authenticated()` + HTTP Basic + `@PreAuthorize` ADMIN/OPERADOR | Evidencia digital de entrega (POD) con firma y GPS. |
| `POST /api/v1/push/subscribe`, `/unsubscribe`, `/test` | — | Push notifications del SPA. |
| `GET /api/v1/docs`, `/api/v1/swagger-ui.html` | Pública | OpenAPI/Swagger. |

### SPA React
`/react-dashboard` → dashboard; `/react-dashboard/dashboard/envio/:codigo` → detalle de envío. También se sirven stubs en `/login-react` y `/dashboard` (SpaForwardController).

### Actuator
`/actuator/health`, `/actuator/info`, `/actuator/prometheus` (en prod además `/actuator/health/liveness`, `/actuator/health/readiness`).

---

## 7. Configuración y Variables de Entorno

### Variables principales (dev `application.properties`)
| Variable | Default dev | Nota prod |
| --- | --- | --- |
| `PORT` | 8080 | Render asigna dinámico. |
| `SPRING_DATASOURCE_URL` / `DATABASE_URL` | `jdbc:mysql://localhost:3306/envios_paraguay_cms?...` | **Obligatoria en prod** (fail-fast). El conversor añade `jdbc:` si falta. |
| `DB_USERNAME` / `DB_PASSWORD` | `root`/`root` (dev) | **Obligatorias en prod**, sin fallback. |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin`/`admin123` (dev) | **Obligatorias en prod**. |
| `UPLOAD_DIR` | `./uploads` | `/app/uploads` (volumen) / `/tmp/uploads` (Render). |
| `LOG_DIR` | `./logs` | — |
| `REDIS_HOST` / `REDIS_PORT` | `localhost`/`6379` | En prod `redis` (Docker) o Upstash externo (Render). |
| `THYMELEAF_CACHE` | `false` | `true` en prod. |
| `JPA_SHOW_SQL` | `true` | `false` en prod. |
| `APP_DEMO_DATA` | `true` | `false` en prod (carga demo al arrancar). |
| `APP_NOTIFICATION_MAIL_ENABLED` | `true` | `false` en Render. |
| `MANAGEMENT_HEALTH_MAIL_ENABLED` | (default true) | **`false` en Render** (crítico: el `MailHealthIndicator` contra `localhost:1025` devolvería `DOWN` → 503 → Render marca el servicio unhealthy). |
| `APP_TRACKING_BASE_URL` | `http://localhost:8080/tracking` | URL pública de tracking (webhooks y PDFs). |
| `APP_WEBHOOK_ENABLED` | `true` | Kill-switch webhooks. |
| `APP_BATCH_ENABLED` | `true` | Kill-switch batch import. |
| `APP_PDF_ENABLED` | `true` | Kill-switch PDF. |
| `JAVA_OPTS` | — | Render free tier: `-Xms256m -Xmx384m -XX:+UseG1GC -XX:+UseStringDeduplication`. |

### HikariCP (dev y prod)
`maximum-pool-size=25`, `minimum-idle=5`, `connection-timeout=20000`, `leak-detection-threshold` 60s (dev) / 30s (prod), pool-name específico por perfil.

### Redis/Session
Pool Lettuce `max-active=30`, `max-idle=15`, `min-idle=5`, `max-wait=2000ms`, timeout 3000ms; `spring.session.redis.namespace=monteastur:session`, save-mode `on-set-attribute`, flush `on-save`, timeout sesión 30 min.

### Flyway
`enabled=true`, locations `classpath:db/migration`, `baseline-on-migrate=true`. `spring.sql.init.mode=never`. `ddl-auto=validate` (dev y prod).

### Props custom por módulo
`app.notification.*`, `app.webhook.*` (enabled, connect-timeout 2000, read-timeout 5000, executor core/max/queue), `app.batch.*` (chunk-size 100, tmp-dir, max-file-size 5MB, max-line-length 10000, max-rows 200000, executor), `app.pdf.*` (max-pages 5000, qr.size 200, barcode 500×120).

---

## 8. Seguridad

- **Admin**: Spring Security con `JdbcUserDetailsManager` contra las tablas `users`/`user_roles`/`roles` (BCrypt). Credenciales desde variables de entorno, sin defaults en prod.
- **Clientes**: sesión propia en `HttpSession` (login por email/password BCrypt), no confundir con la sesión Spring Security.
- **Protección de rutas** (`SecurityConfig`):
  - `authenticated()`: `/admin/**`, `/api/v1/admin/**`, `/api/v1/deliveries/**`.
  - `permitAll()`: OpenAPI/Swagger, tracking, resto del sitio.
  - CSRF ignorado solo en `/api/**`.
  - HTTP Basic activado (para los agentes de reparto en POD).
  - `CustomAccessDeniedHandler` devuelve 400/403 JSON correctamente (con `Accept: text/html` los tests de redirect se mantienen).
- **RBAC**: roles desde BD (`ROLE_ADMIN`, `ROLE_OPERADOR`, etc.) con `@EnableMethodSecurity` y `@PreAuthorize`.
- **DTOs**: los campos sensibles (`secretToken` de webhooks) nunca se exponen en respuestas.
- **Cookies**: `HttpOnly`, `SameSite=Lax`, `Secure` automático en prod vía `forward-headers-strategy=framework` cuando `X-Forwarded-Proto=https`.
- **Headers**: X-Frame-Options DENY, Referrer-Policy STRICT_ORIGIN_WHEN_CROSS_ORIGIN; Nginx añade HSTS, CSP, Permissions-Policy.
- **Validación de entorno**: en prod se aborta el arranque si faltan credenciales de BD/admin.
- **Zero excepciones silenciadas**: los listeners asíncronos auditan fallos (tablas `webhook_logs`, `batch_import_errors`) sin romper el flujo principal.

---

## 9. Módulos Funcionales Destacados

### 9.1 Tracking en tiempo real
- Código único (`MT-2026-0001`), estados: `RECIBIDO`, `EN_TRANSITO`, `ADUANA`, `EN_REPARTO`, `ENTREGADO` (y variantes).
- Timeline de eventos con `visible_cliente`, evidencias con `visible_cliente`, galería.
- Caché Redis `envios.tracking` + `envios.tracking.pagina` (TTL 5 min). **Bug conocido corregido**: analytics por día usan `hoySegunBaseDeDatos()` para respetar la timezone de la BD (commit `3713cbf`).
- Búsqueda pública con PRG + página 404 personalizada; lector QR `html5-qrcode` en el buscador.

### 9.2 Portal público + Dashboard de cliente (Bloque 15)
- `PublicTrackingService.cargarPagina` (`@Cacheable "envios.tracking.pagina"`, TTL 5 min, `unless result==null`) con timeline + POD (solo si `ENTREGADO`).
- `ClientDashboardService.cargarDashboard(clienteId)` (`@Cacheable "envios.cliente.dashboard"`, TTL 1 min) con métricas (`PesoUtil.parsear`, pesos inválidos ignorados).
- `@CacheEvict` en los 3 caches en todos los puntos de mutación (`EnvioTrackingService.guardar/actualizarEstado/eliminar`, `EntregaEvidenciaService.registrarEntrega`, `BatchImportPersistenceService.procesarChunk`).

### 9.3 Notificaciones automáticas (email)
- `EstadoEnvioActualizadoEvent` publicado al cambiar estado.
- `NotificacionEventListener` (`@Async` + `REQUIRES_NEW` + `AFTER_COMMIT`) envía email al cliente con URL de tracking.
- `EmailService.enviarCorreoSimple` con timeouts; fallos SMTP se loguean sin romper el flujo.
- Tabla `notificaciones` para auditoría. Enviar correos solo si `app.notification.mail.enabled=true` y el cliente tiene email.

### 9.4 Webhooks outbound (Bloque 10)
- Config por tabla `webhooks_config` (URL, secretToken, activo, evento de interés) + auditoría en `webhook_logs`.
- Firma **HMAC-SHA256** sobre el body crudo, hex lowercase, header `X-Signature-256`.
- `WebhookDispatchService`: POST con `RestClient`, timeouts 2s/5s, **sin reintentos**, payload JSON normalizado (incluye `url_seguimiento` + timestamp ISO).
- `WebhookEventListener`: `@Async("webhookTaskExecutor")` + `REQUIRES_NEW` + `AFTER_COMMIT`; traga excepciones para no romper el tracking.

### 9.5 Importación masiva CSV (Bloque 11)
- `POST /api/v1/admin/imports/csv` → 202 + `batch_id`; proceso asíncrono `@Async("batchTaskExecutor")`.
- Formato CSV: cabecera `codigo,estado,destinatario,origen,destino,peso,contenido,observaciones` (OpenCSV, streaming, BOM manejado, filas cortas rellenadas a 8, límite 255 por campo, max 200 000 filas).
- Deduplicación local + `existsByCodigoUnico`; chunks de 100 con `REQUIRES_NEW` + `@CacheEvict`. Errores en `batch_import_errors`.
- **No** publica `EstadoEnvioActualizadoEvent` ni dispara webhooks.
- Estados del lote: `PENDIENTE/EN_PROCESO/COMPLETADO/COMPLETADO_CON_ERRORES/FALLIDO`.

### 9.6 Documentos PDF, etiquetas y códigos (Bloque 12)
- `EtiquetaPdfGenerator`: etiqueta térmica 100×150 mm (283.46×425.2 pt) con Code128 + QR con URL de tracking.
- `ManifiestoPdfGenerator`: A4 (595.28×841.89 pt), tabla 5 columnas, totales de peso (`PesoUtil`), firma de despacho.
- Generación **en memoria** (cero I/O en disco), streaming de lotes al `OutputStream` con tope `app.pdf.max-pages` (default 5000) → 400.
- Auditoría persistida en `documentos_generados` (tipo/referenciaId/nombreArchivo/pesoBytes/usuario).

### 9.7 Evidencia de entrega POD (Bloque 13)
- `POST/GET /api/v1/deliveries/{codigo}/pod` → 201/200; `@PreAuthorize` ADMIN/OPERADOR + HTTP Basic.
- `EntregaValidator`: Base64 + magic bytes PNG (`0x89PNG`), firma máx. 5 MB, rangos GPS válidos.
- `EntregaEvidenciaService.registrarEntrega` (transaccional): valida → persiste → `actualizarEstado(codigo, "ENTREGADO")` → `crearEvento` → `@CacheEvict`. 409 duplicado / 404 inexistente.
- Coordenadas con `BigDecimal` (Hibernate 6 rechaza `scale` en flotantes).

### 9.8 Analytics (dashboard React)
- `AnalyticsQueryService` + `AnalyticsDashboardService`: KPIs, distribución por estado, rutas top, tendencias diarias, actividad de webhooks.
- Agregaciones SQL con índices de V10. Endpoint `POST /api/v1/admin/analytics/refresh` para invalidar caché.
- Gráficos Recharts en el SPA (`ActivityChart`, `ShipmentStatusChart`, `AnalyticsKPIs`, `DateRangeFilter`, `ExportButtons` con xlsx).

### 9.9 PWA / Push / Offline
- PWA instalable (manifest + service worker Workbox, precache ~12 entradas, offline fallback).
- Push: `POST /api/v1/push/subscribe|unsubscribe|test`, hook `usePushNotifications.js`, manejadores `push`/`notificationclick` en el SW.
- Offline: `OfflineBanner`, cache de datos en localStorage, cola offline con deduplicación por código+estado, toast de sync.

---

## 10. Testing, CI/CD y Despliegue

### Testing (estado actual)
| Nivel | Detalle |
| --- | --- |
| **Backend** | **276 tests, 0 fallos, BUILD SUCCESS** (JUnit 5 + Mockito + `@WebMvcTest` + `@SpringBootTest` + AssertJ + Awaitility). |
| **Frontend React** | 15 tests (Vitest + Testing Library). |
| **E2E Playwright** | 9 tests (`home`, `login`, `dashboard`, `tracking`). |
| **CI en la nube** | Run `31107460877` para `55eeb22`: ambos jobs verdes. Verificado para HEAD también en runs posteriores. |

Comando local (JDK 17 + Maven): `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd clean test`
Suite en contenedor (MySQL + Redis vía docker network): ver AGENTS.md.

### CI/CD
- **`ci.yml`**: `permissions: contents: read`, `concurrency` por `github.ref` (`cancel-in-progress: true`). Job `test` (MySQL 8 `envios_paraguay_cms_test` + Redis 7-alpine con healthchecks, `setup-java@v4` Temurin 17, `./mvnw clean test -B` con `SPRING_PROFILES_ACTIVE=test`, upload Surefire). Job `docker-build` (needs test, push a `main`/`develop`): buildx, tag `envios-paraguay-cms:latest`, smoke test de arranque en frío (contenedor `--network host`, envs prod, loop 30×5s hasta `/actuator/health` → 200 "UP", servicios efímeros MySQL+Redis+Mailpit).
- **`deploy.yml`**: CD automático a VPS en push a `develop` (pre-deploy-check → SSH → git pull → compose up -d --build → image prune). Secrets: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, `VPS_PORT`.
- **`deploy-prod.yml`**: deploy manual a producción (branch `develop`, escribir "deploy").
- **Render.com**: `render.yaml` — Web Service docker free, branch `main`, healthcheck `/actuator/health`, MySQL 8 externo vía `SPRING_DATASOURCE_URL`, Redis Upstash (`REDIS_HOST`/`REDIS_PORT`), mail off (`APP_NOTIFICATION_MAIL_ENABLED=false` + **`MANAGEMENT_HEALTH_MAIL_ENABLED=false`**), `JAVA_OPTS` free tier, `UPLOAD_DIR=/tmp/uploads`.

### Dockerfile (producción)
- Etapa runtime `eclipse-temurin:25-jre` con `curl` instalado (la imagen base no trae `wget`).
- ENTRYPOINT en forma shell inyectando `$JAVA_OPTS`.
- HEALTHCHECK interno: `curl -fsS http://localhost:${PORT:-8080}/actuator/health`.

### VPS / Dominio / SSL
- Scripts en `scripts/`: `vps-bootstrap.sh`, `deploy-prod.sh`, `rollback-prod.sh <tag>`, `backup-db.sh`, `backup-uploads.sh`, `restore-db.sh`, `restore-uploads.sh`, `server-healthcheck.sh`, `check-ssh-connection.sh`.
- Nginx con Let's Encrypt/Certbot; guías en `docs/`.

---

## 11. Documentación en `docs/` (índice)

60+ documentos. Agrupación:

- **Auditoría y QA**: `PREPRODUCTION_AUDIT_REPORT.md`, `AUDIT_INITIAL_ENVIOS_CMS.md`, `AUDIT_PHASE_0_CLOSURE.md`, `KNOWN_ISSUES_PREPROD.md`, `SENSITIVE_ENDPOINTS_AUDIT.md`, `QA_REAL_EXECUTION_LOG.md`, `QA_BROWSER_CHECKLIST.md`, `QA_E2E_NIVEL_DIOS.md`, `QA_ENDPOINTS_SENSIBLES_PLAN.md`, `EVIDENCE_UPLOADS_AUDIT.md`, `PUBLIC_TRACKING_REVIEW.md`, `MONITORING_ACCESS_REVIEW.md`, `POST_DEPLOY_CHECKS_REVIEW.md`, `OPERATIONAL_SCRIPTS_AUDIT.md`, `OPERATIONAL_QUALITY_PHASE_2_CLOSURE.md`, `PRODUCT_ROBUSTNESS_PHASE_3_CLOSURE.md`.
- **Hardening y seguridad**: `HARDENING_FINAL_REPORT.md`, `HARDENING_PHASE_1_CLOSURE.md`, `HARDENING_BACKLOG_ENVIOS_CMS.md`, `VPS_HARDENING_CHECKLIST.md`.
- **Deploy / VPS / Render**: `FIRST_VPS_DEPLOY_CHECKLIST.md`, `VPS_DEPLOY_GUIDE.md`, `VPS_DEPLOY_DAY_RUNBOOK.md`, `VPS_DEPLOY_EXECUTION_PLAN.md`, `VPS_REAL_EXECUTION_GUIDE.md`, `VPS_REAL_NEXT_ACTIONS.md`, `DEPLOY_REAL_READY_CHECKLIST.md`, `FINAL_PRODUCTION_DEPLOY_CHECKLIST.md`, `FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md`, `FIRST_REAL_DEPLOY_COMMANDS.md`, `LIVE_DEPLOY_PLAN.md`, `REAL_DEPLOY_DECISION_LOG.md`, `FIRST_DEPLOY_RISK_REGISTER.md`, `REAL_DEPLOY_TIMELINE.md`, `PRODUCTION_VPS_RUNBOOK.md`, `PRODUCTION_ENV_GUIDE.md`, `PRODUCTION_SECRETS_TEMPLATE.md`, `PROJECT_FREEZE_V20.md`, `RELEASE_V20_READY.md`.
- **Backup/Monitoring**: `BACKUP_RECOVERY.md`, `BACKUP_RESTORE_REVIEW.md`, `BACKUP_RETENTION_POLICY.md`, `UPTIME_MONITORING.md`, `HTTPS_SETUP.md`.
- **Dominio/DNS/Proveedores**: `DOMAIN_DNS_SSL_SETUP.md`, `DOMAIN_PURCHASE_GUIDE.md`, `HETZNER_VPS_PURCHASE_GUIDE.md`, `GITHUB_SECRETS_SSH_SETUP.md`.
- **Demo/Ventas**: `FREE_DEMO_DEPLOY_OPTIONS.md`, `RECOMMENDED_FREE_DEMO_PLAN.md`, `CLOUDFLARE_TUNNEL_DEMO_GUIDE.md`, `DEMO_SALES_PRESENTATION_SCRIPT.md`, `CONTROLLED_EVIDENCE_DOWNLOAD_PLAN.md`.
- **E2E/CI/Testing**: `E2E_CI_GUIDE.md`, `SMOKE_TESTS_PRODUCTION.md`, `TESTING_STRATEGY.md`, `LOCAL_DEV_COMMANDS.md`, `ROUTE_AND_FLOW_MAP_ENVIOS_CMS.md`, `PROJECT_MAP_ENVIOS_CMS.md`.
- **Estado/avance**: `handoff.md`, `HANDOFF_Envios_Paraguay_CMS.md`, `CONTEXTO_COMPLETO_PROYECTO.md` (este documento).
- **Specs/planes superpowers**: `docs/superpowers/specs/` y `docs/superpowers/plans/` (por bloque: 10 webhooks, 11 batch CSV, 12 PDF/etiquetas, 13 POD, 14 CI/CD, 15 portal tracking, theme-switcher, unificación visual).

---

## 12. Estado de Git y Flujo de Trabajo

- **Rama estable**: `main` (sincronizada con `origin/main`; HEAD local `3713cbf`).
- Remoto: `https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS.git`.
- Últimos commits (HEAD→) `3713cbf` (fix timezone analytics/webhooks), `a91c04e` (tokens design-system + overhaul header), `6c364ed` (parámetros BD local), `e72def6` (estilos + script + render), `54f9178` (Java 17 estricto pom/Dockerfile), `cf36474` (Upstash Redis TLS env vars), `d2c3b28` (handoff render.yaml), `55eeb22` (render.yaml MySQL 8).
- **Reglas de trabajo** (AGENTS.md):
  - `main` = estable; `develop` = integración; `feature/*` = mejoras.
  - **No push ni merge sin confirmación explícita del usuario.**
  - Cambios pequeños y revisables; no mezclar mejoras no relacionadas.
  - `mvn clean test` (BUILD SUCCESS) antes de dar por terminada una tarea.
  - Java puro sin Lombok; inyección por constructor (`private final`); sin `@Autowired` en campos.
  - Migraciones solo con Flyway (`V{N}__descripcion.sql`), InnoDB, `utf8mb4`.
  - Nomenclatura en inglés para clases/métodos/variables; documentación en español; sin comentarios redundantes.
  - No usar `git add -A`; excluir `start-app.ps1` y `render.yaml - Envios_Paraguay_CMS.txt` de los commits.
  - Estado de avance detallado en `docs/handoff.md`.

---

## 13. Credenciales y URLs de Desarrollo (SOLO local)

| Rol | URL | Usuario | Contraseña |
| --- | --- | --- | --- |
| Admin | `http://localhost:8080/login` | `admin` | `admin123` |
| Cliente demo | `http://localhost:8080/cliente/login` | `cliente@monteastur.com` | `demo2026` |
| React SPA | `http://localhost:8080/login-react` | `admin` | `admin123` |
| Grafana | `http://localhost:3001` | `admin` | `admin123` |
| Mailpit UI | `http://localhost:8025` | — | — |

Datos demo (`APP_DEMO_DATA=true`): cliente María González, 4 envíos `MT-2026-0001..0004` con historial y estados variados, 4 mensajes de contacto, 4 reservas, 4 imágenes SVG demo, textos legales.

> ⚠️ En producción **nunca** usar credenciales por defecto. Generar con `openssl rand -base64 32` y configurarlas vía variables de entorno.

---

## 14. Notas para el Modelo de IA (Gemini / Google AI Studio)

1. **Fuente de verdad = código + `pom.xml`**, no el README (el README aún publicita Spring Boot 3.3.5; el pom usa 3.5.16).
2. **No usar Lombok** ni añadir dependencias sin verificar primero.
3. **No regenerar `design-system.css`** desde cero; es un ensamblado de 6 fuentes (ver `docs/handoff.md` §14).
4. **La columna `leido`** de `mensajes_contacto` es V9 — respetar las migraciones y mantener `data/schema.sql` sincronizado.
5. **El SPA no usa JWT**; autenticación por cookie de sesión. No cambiar este modelo sin justificar riesgos.
6. **Los eventos asíncronos** deben capturar y auditar errores (nunca lanzar excepciones que rompan el flujo transaccional del cliente).
7. **Sensibilidad de DTOs**: nunca exponer `secret_token`, contraseñas ni credenciales de API en respuestas REST/DTOs.
8. **Commits**: no hacer push ni merge sin confirmación explícita del usuario; commits pequeños y revisables.
9. **Verificación**: toda tarea debe terminar con `mvn clean test` → BUILD SUCCESS (y, si toca frontend, `npm test`/`npm run build` en `frontend-react/`).
10. Este documento es un resumen de contexto; para detalles profundos consultar `docs/handoff.md`, el README y las specs en `docs/superpowers/`.
