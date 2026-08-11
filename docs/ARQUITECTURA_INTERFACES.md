# Arquitectura de Interfaces — Envios_Paraguay_CMS

**Estado:** Vigente (actualizado el 2026-08-12, hito F7).
**Referencias:** `docs/superpowers/specs/2026-08-10-arquitectura-interfaces-thymeleaf-react-design.md`,
`docs/HARDENING_BACKLOG_ENVIOS_CMS.md` (P2.2, cerrado), `docs/README_DOCS.md` (índice de documentación).

Documenta la clasificación de las interfaces de la aplicación y el resultado de la migración
del CMS Thymeleaf a la SPA React.

---

## 0. Diagrama de arquitectura híbrida (oficial)

Vista de alto nivel: una sola aplicación Spring Boot sirve la SPA React y el SSR Thymeleaf,
compartiendo sesión Spring Security (`JSESSIONID`), con MySQL como base de datos relacional y
Redis para sesiones distribuidas y caché.

```mermaid
flowchart LR
    subgraph Browser
        SPA["SPA React dashboard<br/>(panel admin oficial)"]
        WEB["Web Thymeleaf SSR<br/>, casa, contacto, reservas,<br/>tracking, cliente (público)"]
        API["Cliente externo API v1"]
    end

    subgraph SpringBoot["Spring Boot 3.3.5 (app)"]
        SEC["Spring Security<br/>form login + roles<br/>ADMIN / CLIENTE"]
        CTRL["Controllers<br/>Web + REST API v1"]
        SVC["Servicios de dominio<br/>@Transactional + eventos AFTER_COMMIT"]
        SPAF["Frontend estático React<br/>react-dashboard (302 tras login)"]
        SPAF --> CTRL
    end

    subgraph Data
        MYSQL[("MySQL 8<br/>Flyway migraciones")]
        REDIS[("Redis<br/>sesiones + caché envios.tracking")]
    end

    SPA -- "JSON admin APIs" --> CTRL
    WEB -- "form login (SSR)" --> SEC
    API -- "HTTP Basic / JSON" --> SEC
    SEC --> CTRL
    CTRL --> SVC
    SVC --> MYSQL
    SVC --> REDIS
    SEC -- "302 react-dashboard" --> SPA
```

**Notas de lectura:**

- El navegador entra por la SPA (oficial) o por la web pública (SSR); el login resuelve la sesión
  en Spring Security y redirige la SPA a `/react-dashboard/`.
- El CMS Thymeleaf fue retirado en F6: `/admin/**` y `/admin` redirigen a `/dashboard` y los
  templates `cms/*.html` y el fragmento `admin-sidebar.html` fueron eliminados del proyecto.
- Las APIs `/api/v1/**` sirven a la SPA, al portal y a clientes externos bajo el mismo `SecurityConfig`.

---

## 1. Matriz de interfaces (oficial / legacy / complementaria)

| Interfaz | Estatus | Justificación |
|---|---|---|
| SPA React `/dashboard` | **Oficial** | Panel admin moderno: envíos, filtros, analytics, PWA/offline. Sesión Spring compartida. |
| Web pública `/`, `/casa`, `/contacto`, `/reservas`, `/operaciones`, legales | **Oficial** | Cara pública del negocio; Thymeleaf es el stack correcto para SSR/SEO. |
| Portal tracking `/tracking` | **Oficial** | Rastreo público en línea con la marca; Thymeleaf moderno. |
| Zona cliente `/cliente` | **Oficial** | Login propio por `clienteId`; panel con dashboard cacheado y etiqueta PDF. |
| Login `/login` | **Complementaria** | Soporta ambos flujos (form clásico y login React) mediante la misma sesión. |
| CMS `/admin/**` | **Retirado (F6)** | Redirect total a `/dashboard`; templates `cms/*.html` y `AdminController` eliminados. |
| APIs `/api/v1/**` | **Oficial** | Contrato de datos para SPA, portal y clientes. |

## 2. Autenticación compartida

No hay JWT. La SPA React comparte la sesión **Spring Security** vía cookie `JSESSIONID`.

- `GET /login` con sesión admin válida → `302 /react-dashboard/`.
- `GET /login` con sesión de cliente o anónimo → template `login.html`.
- `POST /login` correcto → `302 /react-dashboard/` (`defaultSuccessUrl`).
- `GET /admin` o `/admin/**` con sesión admin → `302 /dashboard` (retirado en F6); anónimo → `302 /login`.

## 3. Migración CMS → SPA (fases F1–F7, completada)

| Fase | Contenido | Equivalente React | Estado |
|---|---|---|---|
| F1 | Envíos, tracking, detalle, analytics, export | `AdminDashboard`, `ShipmentDetailPage`, APIs `/api/v1/admin/*` | **Completa** |
| F2 | Importación batch (CSV) y su monitorización | `ImportBatchPage` sobre `BatchImportController` | **Completa** |
| F3 | Documentos / evidencias | `DocumentosPage`, `EvidenciasGrid` sobre `DocumentosController`/`EntregaEvidenciaController` | **Completa** |
| F4 | Reservas y contactos (mensajes) | `ReservasPage`, `MensajesPage` sobre `ReservaApiController`/`MensajeContactoApiController` | **Completa** |
| F5 | Imágenes (galería) y textos legales | `AdminImagesPage`, `AdminLegalTextsPage` sobre `ImagenApiController`/`TextoLegalApiController` | **Completa** |
| F6 | Deprecación: `/admin/**` → redirect a `/dashboard` + borrado de `AdminController`, templates `cms/*.html` y `admin-sidebar.html` | CRUD envíos en `EnvioFormPage` + acciones en `AdminDashboard`/`ShipmentDetailPage` | **Completa** |
| F7 | Webhooks y notificaciones de estado | `WebhooksPage` (CRUD + historial de despachos), `NotificacionesPage` (filtro por estado, detalle expandible, reintento) sobre `WebhookConfigController` extendido y `NotificacionApiController` | **Completa** |

## 4. Estado final tras la migración

- Toda la gestión admin vive en la SPA React (`/dashboard/**`), servida por REST `/api/v1/admin/*`
  y las APIs de envíos/evidencias (`/api/v1/admin/envios/**`, `/api/v1/admin/entregas/**`).
- El CMS Thymeleaf fue retirado por completo: `AdminController` y los templates `cms/*.html`
  (con su fragmento `admin-sidebar.html`) se eliminaron; `/admin` y `/admin/**` redirigen a `/dashboard`.
- La web pública (`/`, `/casa`, `/contacto`, `/reservas`, `/operaciones`, legales), el portal de
  tracking y la zona cliente se mantienen en Thymeleaf SSR, que es el stack correcto para SSR/SEO.

## 5. Observaciones de pulido visual (fuera de P2.2)

- Hallazgo **H8** (commit `e72def6`): `design-system.css` no contiene `.sidebar`, `.nav-links`,
  `.main-content`, `.btn-logout` ni `.logout-form`. Regresión previa documentada en el backlog;
  pendiente para un futuro bloque de pulido visual.
