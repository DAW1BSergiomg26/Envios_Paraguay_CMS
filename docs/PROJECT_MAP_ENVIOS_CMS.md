# PROJECT_MAP_ENVIOS_CMS

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/auditoria-inicial-envios-cms
Tipo: mapa tecnico sin cambios de codigo
Documento previo: docs/AUDIT_INITIAL_ENVIOS_CMS.md
```

---

## Proposito

Este documento funciona como mapa tecnico inicial de `Envios_Paraguay_CMS`.

Su objetivo es dejar claro como esta organizado el proyecto antes de tocar codigo, refactorizar, cambiar configuracion o mejorar UX/UI.

---

## Mapa general

El proyecto combina varias capas:

```text
Backend Spring Boot
Templates Thymeleaf
CMS administrativo
Zona cliente
API REST
Frontend React/Vite
Dashboard React compilado dentro de static
Docker / Nginx
GitHub Actions
Monitoring
Documentacion de despliegue
```

---

## Backend Java

Raiz principal:

```text
src/main/java/com/monteastur/envios
```

Archivo principal:

```text
MonteasturApplication.java
```

Paquetes detectados:

```text
config/
controller/
controller/api/
dto/api/
model/
repository/
service/
```

---

## Configuracion backend

Archivos detectados:

```text
DataInitializer.java
ReactConfig.java
SecurityConfig.java
SpaForwardController.java
WebMvcConfig.java
```

Lectura inicial:

```text
DataInitializer → carga o prepara datos iniciales.
ReactConfig → configuracion relacionada con React/dashboard.
SecurityConfig → seguridad Spring Security.
SpaForwardController → soporte de SPA/rutas frontend.
WebMvcConfig → configuracion MVC.
```

Riesgo a revisar:

```text
Confirmar como conviven Thymeleaf, rutas MVC, API REST y React SPA.
```

---

## Controllers MVC

Archivos detectados:

```text
AdminController.java
ClienteController.java
LoginController.java
PublicController.java
```

Lectura inicial:

```text
AdminController → CMS o administracion Thymeleaf.
ClienteController → zona cliente.
LoginController → autenticacion/login.
PublicController → paginas publicas.
```

---

## Controllers API

Archivos detectados:

```text
AdminApiController.java
ClienteApiController.java
PushSubscriptionController.java
TrackingApiController.java
TrackingApiExceptionHandler.java
```

Lectura inicial:

```text
AdminApiController → API para dashboard/admin.
ClienteApiController → API para cliente.
PushSubscriptionController → notificaciones push/PWA.
TrackingApiController → tracking de envios.
TrackingApiExceptionHandler → manejo de errores API tracking.
```

---

## DTOs API

Archivos detectados:

```text
ActualizarEstadoRequest.java
AdminEnvioResumenDto.java
ClienteEnvioResumenDto.java
ErrorDto.java
EventoDto.java
EvidenciaDto.java
PushSubscriptionRequest.java
TrackingDto.java
```

Lectura inicial:

```text
Hay separacion entre entidades y objetos de respuesta/peticion.
Esto es positivo para API REST y frontend React.
```

---

## Modelos / Entidades

Archivos detectados:

```text
Cliente.java
EnvioTracking.java
EventoTracking.java
EvidenciaEnvio.java
Imagen.java
MensajeContacto.java
Reserva.java
TextoLegal.java
```

Lectura inicial:

```text
El dominio mezcla dos mundos:
1. Web/CMS original: Imagen, MensajeContacto, Reserva, TextoLegal.
2. Logistica/tracking: Cliente, EnvioTracking, EventoTracking, EvidenciaEnvio.
```

Riesgo:

```text
Puede haber herencia funcional del proyecto Casa Rural mezclada con Monteastur Envios.
Conviene revisar naming y responsabilidad por fases, no de golpe.
```

---

## Repositories

Archivos detectados:

```text
ClienteRepository.java
EnvioTrackingRepository.java
EventoTrackingRepository.java
EvidenciaEnvioRepository.java
ImagenRepository.java
MensajeContactoRepository.java
ReservaRepository.java
TextoLegalRepository.java
```

Lectura inicial:

```text
Persistencia JPA separada por entidad.
```

---

## Services

Archivos detectados:

```text
ClienteService.java
EmailService.java
EnvioTrackingService.java
EventoTrackingService.java
EvidenciaEnvioService.java
ReservaService.java
```

Lectura inicial:

```text
Hay capa de servicio para logica principal.
Falta revisar si los controllers delegan correctamente o si hay logica mezclada en controllers.
```

---

## Templates Thymeleaf

Raiz:

```text
src/main/resources/templates
```

Paginas publicas:

```text
aviso-legal.html
contacto.html
entorno.html
home.html
lacasa.html
login.html
operaciones.html
politica-cookies.html
reservas.html
tracking.html
```

Zona cliente:

```text
cliente/login.html
cliente/panel.html
```

CMS:

```text
cms/contactos.html
cms/dashboard.html
cms/imagenes.html
cms/reservas.html
cms/textos.html
cms/tracking-form.html
cms/tracking.html
```

Version inglesa:

```text
en/aviso-legal.html
en/casa.html
en/contacto.html
en/home.html
en/operaciones.html
en/politica-cookies.html
en/reservas.html
en/tracking.html
```

Fragments:

```text
fragments/admin-sidebar.html
fragments/footer.html
fragments/footer-en.html
fragments/header.html
fragments/header-en.html
```

Lectura inicial:

```text
Hay una web publica completa, una zona cliente, un CMS, version inglesa y fragments reutilizables.
```

---

## Static assets

Raiz:

```text
src/main/resources/static
```

CSS principal:

```text
css/style.css
css/admin.css
```

CSS admin modular:

```text
css/admin/admin-base.css
css/admin/admin-client-panel.css
css/admin/admin-components.css
css/admin/admin-dashboard.css
css/admin/admin-evidencias.css
css/admin/admin-login.css
css/admin/admin-responsive.css
css/admin/admin-sidebar.css
css/admin/admin-theme.css
css/admin/admin-tracking.css
```

JS:

```text
js/app.js
```

Imagenes:

```text
img/demo-gallery/
img/media/
img/monteastur/
```

Dashboard React compilado:

```text
react-dashboard/index.html
react-dashboard/assets/*.css
react-dashboard/assets/*.js
```

Uploads detectado dentro de static:

```text
static/uploads/*.webp
```

Riesgo:

```text
Revisar si static/uploads contiene archivos generados que deberian vivir fuera de resources/static en produccion.
```

---

## Frontend React fuente

Raiz:

```text
frontend-react/src
```

Archivos principales:

```text
App.jsx
main.jsx
index.css
```

Paginas:

```text
AdminDashboard.jsx
LoginPage.jsx
ProtectedRoute.jsx
ShipmentDetailPage.jsx
```

Layouts:

```text
MainLayout.jsx
```

Context:

```text
AuthContext.jsx
NotificationContext.jsx
```

Servicios:

```text
api.js
dateUtils.js
exportUtils.js
offlineCache.js
offlineQueue.js
```

Hooks:

```text
useOfflineSync.js
useOnlineStatus.js
usePolling.js
usePushNotifications.js
usePWAInstall.js
```

Componentes destacados:

```text
AnalyticsKPIs
ActivityChart
EvidenciasGrid
ExportButtons
InstallPWAButton
OfflineBanner
PushNotificationButton
ShipmentStatusChart
StatsCard
StatusBadge
Timeline
ToastContainer
UpdateEstadoPanel
```

Testing unitario detectado:

```text
EmptyState.test.jsx
SearchBar.test.jsx
StatsCard.test.jsx
StatusBadge.test.jsx
LoginPage.test.jsx
```

---

## E2E / Playwright

Ruta:

```text
frontend-react/e2e
```

Specs detectadas:

```text
dashboard.spec.js
debug.spec.js
home.spec.js
login.spec.js
tracking.spec.js
```

Lectura inicial:

```text
Existe cobertura E2E para flujos importantes: home, login, tracking y dashboard.
```

---

## CI/CD

Workflows detectados:

```text
.github/workflows/ci.yml
.github/workflows/deploy.yml
.github/workflows/deploy-prod.yml
```

Lectura inicial:

```text
Hay separacion entre integracion continua y despliegue.
```

Pendiente:

```text
Revisar jobs, secretos requeridos, ramas disparadoras y artefactos.
```

---

## Riesgos arquitectonicos iniciales

### P1 — Convivencia Thymeleaf + React

```text
Hay Thymeleaf, CMS, cliente, API REST y React dashboard compilado.
Se debe documentar que interfaz manda en cada ruta.
```

### P1 — Uploads dentro de static

```text
Existe static/uploads con al menos un archivo webp.
Conviene revisar si es demo, recurso fijo o subida generada.
```

### P1 — Seguridad de admin y perfiles

```text
Revisar SecurityConfig, application.properties y application-prod.properties antes de despliegue real.
```

### P2 — Naming heredado

```text
Existen elementos de Casa Rural y Monteastur Envios conviviendo.
No romper ahora; planificar limpieza por fases.
```

### P2 — React build versionado en static

```text
static/react-dashboard contiene build compilado.
Conviene definir proceso oficial: fuente React → build → copia a static.
```

---

## Proxima accion recomendada

Crear un tercer documento:

```text
docs/ROUTE_AND_FLOW_MAP_ENVIOS_CMS.md
```

Objetivo:

```text
Mapear rutas publicas, rutas admin, rutas cliente, rutas API y rutas React.
```

Motivo:

```text
Antes de tocar seguridad, UX o deploy, hay que saber como navega la app y que endpoint alimenta cada vista.
```

---

## Decision actual

```text
Estado: mapa tecnico inicial creado
Riesgo general: medio controlado
Siguiente paso: mapear rutas y flujos
```

---

## Frase guia

Ya no estamos mirando archivos sueltos.

Estamos viendo el esqueleto completo de un producto real.
