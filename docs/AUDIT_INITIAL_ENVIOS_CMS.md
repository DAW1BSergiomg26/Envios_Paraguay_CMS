# AUDIT_INITIAL_ENVIOS_CMS

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/auditoria-inicial-envios-cms
Tipo: auditoria inicial sin cambios de codigo
Mission: MISSION_ENVIOS_PARAGUAY_CMS
Team: TEAM_PRODUCT_EVOLUTION_DAW
Academy: ACADEMY_PROJECT_AUDIT
Roadmap: PROJECT_ROADMAP_REAL_APP
```

---

## Proposito

Este documento recoge la primera auditoria tecnica y de producto de `Envios_Paraguay_CMS`.

La auditoria no modifica codigo. Su objetivo es entender el proyecto, detectar riesgos, ordenar prioridades y preparar un roadmap profesional de mejora.

---

## Resumen ejecutivo

`Envios_Paraguay_CMS` no es una web simple. Es una aplicacion full stack avanzada con backend Spring Boot, frontend React, capa CMS, tracking, despliegue con Docker/Nginx, CI/CD, monitorizacion y documentacion de produccion.

El proyecto tiene mucho potencial, pero necesita crecer con metodo: auditoria, fases pequenas, seguridad, pruebas, Git profesional y roadmap de producto.

---

## Arquitectura detectada

### Backend

```text
Spring Boot 3.3.5
Java 17
Thymeleaf
Spring Data JPA
MySQL
Spring Security
Spring Mail
Spring Actuator
Micrometer Prometheus
```

### Frontend

```text
React
Vite
React Router
Axios
Recharts
PWA
XLSX
Vitest
Playwright
```

### Infraestructura

```text
Docker
Docker Compose
Nginx
GitHub Actions
Monitoring
Logs
Uploads
Documentacion VPS / produccion / demo
```

---

## Estructura principal observada

```text
.github/
backup/
docs/
frontend-react/
logs/
monitoring/
nginx/
scripts/
src/
target/
uploads/
Dockerfile
docker-compose.yml
pom.xml
README.md
```

---

## Backend observado

Raiz Java detectada:

```text
src/main/java/com/monteastur
```

Recursos detectados:

```text
src/main/resources/templates
src/main/resources/static
src/main/resources/data
src/main/resources/application.properties
src/main/resources/application-prod.properties
src/main/resources/logback-spring.xml
```

Templates principales detectados:

```text
cliente/
cms/
en/
fragments/
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

Static detectado:

```text
css/
img/
js/
react-dashboard/
uploads/
```

---

## Frontend React observado

Paginas principales:

```text
AdminDashboard.jsx
LoginPage.jsx
ProtectedRoute.jsx
ShipmentDetailPage.jsx
```

Componentes destacados:

```text
ActiveFilters
ActivityChart
AnalyticsKPIs
AnalyticsSection
DateRangeFilter
EmptyState
EvidenciasGrid
ExportButtons
InstallPWAButton
MultiStatusFilter
OfflineBanner
Pagination
PushNotificationButton
RefreshIndicator
SearchBar
ShipmentStatusChart
StatsCard
StatusBadge
StatusFilter
Timeline
ToastContainer
UpdateEstadoPanel
```

Servicios y hooks:

```text
api.js
dateUtils.js
exportUtils.js
offlineCache.js
offlineQueue.js
useOfflineSync.js
useOnlineStatus.js
usePolling.js
usePushNotifications.js
usePWAInstall.js
```

Testing frontend:

```text
Vitest
Testing Library
Playwright E2E
```

E2E detectado:

```text
dashboard.spec.js
debug.spec.js
home.spec.js
login.spec.js
tracking.spec.js
```

---

## Seguridad y configuracion

### Buenas senales

```text
.env esta ignorado por Git.
logs/ esta ignorado por Git.
uploads/ esta ignorado por Git.
target/ esta ignorado por Git.
node_modules y dist estan ignorados en frontend-react/.gitignore.
application-prod.properties fuerza variables de entorno para credenciales.
```

### Riesgos detectados

#### P0/P1 — Defaults sensibles de desarrollo

En `application.properties` existen valores por defecto para credenciales admin de desarrollo.

Riesgo:

```text
Si la app arranca en entorno publico sin perfil prod o sin variables de entorno, podria usar valores inseguros.
```

Decision:

```text
No modificar todavia. Registrar para hardening posterior.
```

---

#### P1 — Riesgo de arrancar produccion sin perfil prod

`application.properties` usa valores comodos para desarrollo. `application-prod.properties` es mas seguro.

Riesgo:

```text
Un despliegue mal configurado podria arrancar sin SPRING_PROFILES_ACTIVE=prod.
```

Decision:

```text
Priorizar checklist de arranque seguro antes de produccion real.
```

---

#### P1 — Doble interfaz Thymeleaf + React

El proyecto usa templates Thymeleaf y frontend React separado, ademas de `static/react-dashboard`.

Riesgo:

```text
Puede haber duplicidad, confusion de rutas o dudas sobre que interfaz manda en produccion.
```

Decision:

```text
Mapear flujo publico, CMS, tracking y dashboard antes de refactorizar.
```

---

#### P2 — Incoherencia documental de paquete

`pom.xml` declara `groupId` como `com.grupb2`, pero el codigo observado vive bajo `com.monteastur`.

Riesgo:

```text
No rompe necesariamente, pero genera confusion documental y de identidad tecnica.
```

Decision:

```text
Anotar para fase de limpieza/naming, no tocar ahora.
```

---

## Roadmap inicial recomendado

### Fase 0 — Auditoria y mapa

```text
Crear mapa de backend, frontend, rutas, workflows, Docker y documentacion.
No modificar codigo.
```

### Fase 1 — Seguridad y entorno

```text
Revisar perfiles Spring.
Revisar .env.example y .env.production.example.
Revisar defaults inseguros.
Revisar GitHub Secrets.
Revisar docker-compose.yml.
```

### Fase 2 — Testing de salud

```text
mvn test
frontend-react npm test
frontend-react npm run build
frontend-react npm run test:e2e si entorno disponible
```

### Fase 3 — Claridad de arquitectura

```text
Definir que hace Thymeleaf.
Definir que hace React.
Definir como se sirve react-dashboard.
Definir rutas principales.
```

### Fase 4 — UX/UI y producto

```text
Revisar home, tracking, login, dashboard, operaciones y reservas.
Priorizar mejoras visibles sin romper flujos.
```

### Fase 5 — Preproduccion y despliegue

```text
Consolidar runbooks.
Reducir duplicidad documental.
Validar demo gratis / preventa.
Preparar release estable.
```

---

## Primer cambio recomendado

Crear un documento de mapa tecnico del proyecto:

```text
docs/PROJECT_MAP_ENVIOS_CMS.md
```

Debe incluir:

```text
backend packages
controllers
services
repositories
models
security
frontend pages
frontend services
rutas principales
workflows CI/CD
Docker/Nginx
```

Motivo:

```text
Antes de tocar codigo, necesitamos un mapa del territorio.
```

---

## Reglas para siguientes fases

```text
No trabajar directo en develop/main.
Usar ramas feature pequenas.
No mezclar hardening, UX, testing y deploy en una sola rama.
No commitear .env, logs, uploads, target, node_modules ni dist.
No exponer secretos en chats, docs o commits.
Probar antes de merge.
Sugerir commit, no hacer push sin confirmacion humana cuando se trabaje localmente.
```

---

## Decision actual

```text
Estado: auditoria inicial creada
Riesgo general: medio controlado
Siguiente paso: crear mapa tecnico del proyecto
```

---

## Frase guia

Envios Paraguay CMS ya tiene cuerpo de producto.

Ahora necesita mapa, orden y pulso profesional para crecer sin romperse.
