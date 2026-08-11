# F7 — UI de gestión de Webhooks y Notificaciones en la SPA React (diseño)

**Fecha:** 2026-08-12
**Estado:** Diseño aprobado (Enfoque A) — pendiente de plan de implementación.
**Fase del roadmap:** F7 (Externo: notificaciones + webhooks), sobre la migración CMS→SPA completada en F6.

## 1. Contexto y problema

El backend ya dispone de la infraestructura de integraciones externas, construida en fases previas
(plan `2026-07-31-notificaciones-automaticas-implementation.md` y plan
`2026-08-02-bloque10-webhooks-outbound-hmac-plan.md`):

- **Webhooks outbound con firma HMAC-SHA256:** `WebhookConfig` + `WebhookConfigRepository`,
  `WebhookDispatchService` (RestClient con timeouts, cabecera `X-Signature-256`), `WebhookPayloadBuilder`,
  `WebhookSignature`, `WebhookEventListener` (`@Async` + `@TransactionalEventListener(AFTER_COMMIT)`),
  `WebhookLog` + `WebhookLogRepository` (auditoría de cada despacho: status HTTP, exitoso, error, payload).
- **Notificaciones por email:** `Notificacion` + `NotificacionRepository` (estados `ENVIADO`,
  `FALLIDO`, `OMITIDO_SIN_DESTINATARIO`), `NotificacionEventListener` (email de cambio de estado),
  `EmailService.enviarCorreoSimple(para, asunto, texto)`.
- **API admin existente:** `WebhookConfigController` (`/api/v1/admin/webhooks`) con
  `GET` (listar, filtro por `clienteId`), `POST` (crear), `DELETE /{id}`. Nunca expone `secret_token`.

**Carencia:** la SPA React (`frontend-react`) no gestiona estas integraciones. No hay página de
webhooks ni de notificaciones, no hay rutas `/dashboard/webhooks` ni `/dashboard/notificaciones`,
ni enlaces en el nav, ni botón de reintento de notificaciones fallidas.

**Objetivo de F7:** exponer la gestión y auditoría de webhooks y notificaciones en la SPA,
siguiendo las convenciones ya establecidas en F4/F5 (controllers API dedicados por recurso,
DTOs con `from()`, `@PreAuthorize("hasRole('ROLE_ADMIN')")`, páginas + tests frontend con vitest).

## 2. Decisiones de diseño (aprobadas en brainstorming)

- **Enfoque A — controllers API dedicados por recurso**, cohesionados por recurso (convención F4/F5):
  `NotificacionApiController` nuevo; `WebhookConfigController` existente se **extiende** (no se reemplaza).
- **Notificaciones:** solo lectura + reintento manual de fallidas (sin gestión de plantillas en F7).
- **Webhooks:** CRUD completo (listar/crear/editar/eliminar) + historial de despachos (`WebhookLog`)
  colapsable por webhook en la SPA.
- **Reintento:** síncrono — `POST /api/v1/admin/notificaciones/{id}/reintentar` reenvía el mismo
  asunto/cuerpo al mismo destinatario y actualiza el registro a `ENVIADO` (o deja `FALLIDO` con nuevo
  `errorMensaje`). Uso exclusivo admin.
- **Fuera de alcance F7:** gestión de plantillas de email, suscripción por tipo de evento por webhook,
  reintentos con backoff en el dispatcher, botón de prueba de conexión (ping) de webhook, gestión de
  notificaciones push del navegador (ya existe `PushNotificationButton` como feature independiente).

## 3. Arquitectura de la solución

### 3.1 Backend (TDD estricto, Java puro sin Lombok, inyección por constructor)

Nuevos DTOs (en `com.monteastur.envios.dto.api`):

1. `NotificacionDto` — id, envioId, destinatario, asunto, mensaje, estado, errorMensaje, fechaCreacion.
   `static from(Notificacion)`. Se reutiliza como respuesta de `POST /{id}/reintentar` (no se crea DTO
   específico de respuesta).
3. `WebhookLogDto` — id, webhookId, envioId, responseStatus, exitoso, errorMensaje, fechaCreacion
   (payload **no** se incluye en el listado; el detalle/ampliación de payload queda fuera de F7).
4. `ActualizarWebhookRequest` — url, secretToken, activo (campos opcionales; validación de esquema http/https).

Nuevo controller `NotificacionApiController`:

- `GET /api/v1/admin/notificaciones` → `List<NotificacionDto>`, filtro opcional por `estado`
  (`@RequestParam(required = false) String estado`, validado contra el enum, `400` si es inválido).
  Orden por `fechaCreacion` desc.
- `GET /api/v1/admin/notificaciones/{id}` → `NotificacionDto` (o `404`).
- `POST /api/v1/admin/notificaciones/{id}/reintentar`:
  1. Busca la notificación por id; `404` si no existe.
  2. Si `estado != FALLIDO` → `409 Conflict` (solo se reintentan fallidas).
  3. Si falta `destinatario` → `400 Bad Request`.
  4. Reenvía con `EmailService.enviarCorreoSimple(destinatario, asunto, mensaje)`.
  5. Éxito → actualiza el registro a `ENVIADO`, `errorMensaje = null`, guarda y devuelve `NotificacionDto`.
  6. Falla de email → actualiza `errorMensaje` con el nuevo error (mantiene `FALLIDO`), guarda y
     devuelve `500` con `ErrorDto` (el detalle del fallo queda auditado en la entidad).
  - `@PreAuthorize("hasRole('ROLE_ADMIN')")`, Swagger `@Tag("Admin Notificaciones")`.

Extensión de `WebhookConfigController` (se añaden métodos, sin tocar los existentes):

- `PUT /api/v1/admin/webhooks/{id}` → `ActualizarWebhookRequest`; `404` si no existe; valida URL
  (esquema http/https) si se envía; `secretToken` en blanco se ignora (no se borra el secreto);
  `activo` opcional; devuelve `WebhookConfigDto` (sin secret). Se reutiliza `validarUrl(String)` existente.
- `GET /api/v1/admin/webhooks/{id}/logs` → `List<WebhookLogDto>` ordenado por `fechaCreacion` desc,
  filtro opcional por `exitoso` (`@RequestParam(required = false) Boolean exitoso`); `404` si el
  webhook no existe. Usa `WebhookLogRepository.findByWebhookIdOrderByFechaCreacionDesc`.

### 3.2 Frontend (React, patrones F4/F5: páginas + tests vitest)

Nuevas páginas en `frontend-react/src/pages`:

1. `WebhooksPage.jsx`
   - Listado de configs (tarjetas o tabla) consumiendo `GET /api/v1/admin/webhooks`.
   - Alta: formulario (clienteId, url, secretToken, activo) → `POST`.
   - Edición inline: formulario → `PUT /{id}` (url, secretToken opcional, activo).
   - Eliminación con confirm → `DELETE /{id}`.
   - Por cada webhook, historial colapsable de despachos → `GET /{id}/logs`, con estado visual
     (exitoso/failure), HTTP status, fecha; filtro por exitoso.
   - El `secret_token` **nunca** se muestra (solo se escribe al crear/editar).
2. `NotificacionesPage.jsx`
   - Listado `GET /api/v1/admin/notificaciones` con filtro por estado (ENVIADO/FALLIDO/OMITIDO_SIN_DESTINATARIO).
   - Detalle en fila expandible (destinatario, asunto, mensaje, error, fecha).
   - Botón "Reintentar" en notificaciones `FALLIDO` → `POST /{id}/reintentar`, refresca la lista y
     muestra toast de resultado.
3. `api.js` — nuevos helpers `listarWebhooks`, `crearWebhook`, `actualizarWebhook`, `eliminarWebhook`,
   `listarWebhookLogs`, `listarNotificaciones`, `reintentarNotificacion` (patrón de los helpers existentes
   para `/api/v1/admin/*`, reutilizando el manejo de credenciales/sesión actual).
4. `App.jsx` — rutas `/dashboard/webhooks` y `/dashboard/notificaciones` dentro de `MainLayout` +
   `ProtectedRoute` (mirror F4/F5).
5. `MainLayout.jsx` — botones de nav "Webhooks" y "Notificaciones".
6. `index.css` — clases F7 reutilizando tokens existentes (`--accent-color` #d4762a, `.acciones-fila`,
   `.upload-form`, `.badge-*`); sin override de marca.

### 3.3 Seguridad

- Todos los endpoints bajo `/api/v1/admin/**` heredan la protección de `SecurityConfig`
  (`hasRole('ROLE_ADMIN')` a nivel de método con `@PreAuthorize`).
- `secret_token` nunca se serializa en `WebhookConfigDto` ni se loguea.
- El payload de `WebhookLog` no se expone en F7 (evita fugas de datos del cliente en el listado).

## 4. Testing

### Backend (TDD, RED → GREEN)

- `NotificacionApiControllerTest` (`@WebMvcTest` + `@MockBean` repos/EmailService, mirror de
  `ImagenApiControllerTest`/`TextoLegalApiControllerTest`):
  - listar sin filtro; listar con `estado` válido; `estado` inválido → 400.
  - detalle existente; detalle inexistente → 404.
  - reintentar `FALLIDO` con email OK → 200, registro `ENVIADO`, `errorMensaje` null.
  - reintentar `ENVIADO` → 409.
  - reintentar inexistente → 404.
  - reintentar sin destinatario → 400.
  - reintentar con fallo de email → 500, registro mantiene `FALLIDO` con nuevo error.
- `WebhookConfigController` (tests añadidos al existente):
  - `PUT` actualiza url/activo y mantiene secret si no se envía.
  - `PUT` con url inválida → 400; `PUT` inexistente → 404.
  - `GET /{id}/logs` devuelve logs ordenados; filtro `exitoso`; webhook inexistente → 404.
- Suite completa `mvn clean test` (excl. integración Docker) → BUILD SUCCESS, 0 failures.

### Frontend (vitest)

- `WebhooksPage.test.jsx`: listado, alta (POST), edición (PUT), eliminación (confirm+DELETE),
  historial colapsable y filtro de logs.
- `NotificacionesPage.test.jsx`: listado con filtro por estado, detalle expandible,
  botón reintentar (POST) y toast.
- `api.test.js` — helpers nuevos (si el patrón actual lo cubre).
- Suite completa `npx vitest run` (workdir `frontend-react`) → 0 failures; `npm run build` → OK.

## 5. Commits atómicos previstos (convención del repo)

1. Backend: DTOs `NotificacionDto`/`WebhookLogDto`/`ActualizarWebhookRequest` + tests de controller.
2. Backend: `NotificacionApiController` (GET listado/detalle + POST reintentar) + tests.
3. Backend: extensión de `WebhookConfigController` (PUT + GET logs) + tests.
4. Frontend: helpers `api.js` + tests.
5. Frontend: `WebhooksPage` + tests + ruta + nav + estilos.
6. Frontend: `NotificacionesPage` + tests + ruta + nav + estilos.
7. Docs: roadmap F7 completa + handoff.

## 6. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| `EmailService.enviarCorreoSimple` lanza `IllegalStateException` si `JavaMailSender` no está configurado (entorno de test) | En `@WebMvcTest` el bean se mockea; el test de fallo de email mockea la excepción y verifica 500 + registro FALLIDO. |
| `@WebMvcTest` importa `SecurityConfig` y exige mocks de `DataSource`/`RBACAccessLogger`/`CustomAccessDeniedHandler` | Mirror exacto de `ImagenApiControllerTest`/`TextoLegalApiControllerTest`. |
| No romper el CRUD existente de webhooks | La extensión añade endpoints; los tests existentes del controller se mantienen verdes. |
| SPA sin datos reales de logs/notificaciones en desarrollo | EmptyState reutilizado (patrón F4/F5) + mocks en tests. |
