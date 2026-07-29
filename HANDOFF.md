# HANDOFF — Envios_Paraguay_CMS (Monteastur Envios)

**Última actualización:** 2026-07-29
**Rama activa:** `feature/seguimiento-premium`
**Commits totales:** ~260

---

## Estado del Sistema

| Componente | Estado | Puerto |
|------------|--------|--------|
| App (Spring Boot) | ✅ Running | `:8080` |
| MySQL 8.0 | ✅ Healthy | `:3306` (interno) |
| Nginx (proxy reverso) | ✅ Running | `:8090` |
| Prometheus | ✅ Running | `:9090` (localhost) |
| Grafana | ✅ Running | `:3001` (localhost) |
| Uptime Kuma | ✅ Running | `:3002` (localhost) |

---

## Arquitectura

```
Usuario → Nginx (:8090) → Spring Boot (:8080) → MySQL (:3306)
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
               Prometheus     Grafana      Uptime Kuma
```

- **Stack:** Spring Boot 3.3.5 / Java 17 / Maven + Thymeleaf templates
- **DB:** MySQL 8.0 (Docker), esquema `envios_paraguay_cms`
- **Proxy:** Nginx Alpine (local: `local.conf` sin CSP; prod: `monteastur.conf` con CSP)
- **Monitorización:** Prometheus + Grafana + Uptime Kuma (todos en localhost)
- **CDN:** Lucide icons vía `unpkg.com/lucide@latest` con `defer`
- **Despliegue:** Docker multi-stage (compilación Maven → JRE 17 mínimo)

---

## Diseño Visual Aplicado (Luxury Transformation)

### Sistema de diseño: `static/css/luxury-core.css`

- **Design tokens:** CSS custom properties (`--color-accent`, `--glass-bg`, etc.)
- **Glassmorphism:** `.glass-card` con `backdrop-filter: blur()` y bordes translúcidos
- **Tipografía lujo:** `.luxury-heading-xl`, `.luxury-heading`, `.luxury-eyebrow` con tracking, letter-spacing y opacidades
- **Botones premium:** `.btn-luxury` (primary), `.btn-luxury-secondary`, `.btn-luxury-ghost`
- **Efectos:** `.card-hover` (elevación sutil), `.focus-glow` (glow en inputs), `.status-dot` (indicadores)
- **Iconos:** `.luxury-icon` / `.luxury-icon-sm` / `.luxury-icon-lg` / `.luxury-icon-light` con color heredado y transiciones

### Iconos Lucide — Corrección de carga en Docker

Se corrigieron dos bugs que impedían el renderizado de iconos en el entorno Docker:

1. **`defer` en script inline** (ignorado por HTML spec): se eliminó el atributo `defer` de `<script defer>` en ambos footers
2. **Script fuera del fragmento Thymeleaf**: el `<script>` de inicialización `lucide.createIcons()` se movió DENTRO del `<footer>` para que `th:replace` lo incluya en todas las páginas

Flujo de carga actual:
```
<head> → <script src="lucide" defer>  (paralelo, ejecuta tras parsear HTML)
<body> → <footer> → <script>lucide.createIcons()</script>  (tras DOMContentLoaded)
```

### Templates transformados (ES + EN)

Todos los templates en `/templates/` y `/templates/en/` usan exclusivamente `data-lucide` attributes (sin emojis):

| Template | Iconos Lucide |
|----------|---------------|
| home.html | check, globe, file-text, map-pin, message-circle, truck, package, shield-check, users, phone, ship, check-circle |
| casa.html | package, truck, clipboard-check, users, phone, map-pin, ship, check, globe, message-circle, shield, settings |
| tracking.html | search, package, truck, file-text, ship, map-pin, check-circle, phone, loader |
| reservas.html | calendar, clock, truck, package, check, phone, mail, user, clipboard, credit-card |
| contacto.html | phone, mail, map-pin, message-circle, send, clock, check-circle |
| operaciones.html | image, map-pin, ship, truck, package, users, check |
| entorno.html | image, map-pin, ship, truck, package, users |
| login.html | user, lock, eye, eye-off, log-in |
| aviso-legal.html | shield, file-text |
| politica-cookies.html | cookie, info |
| admin/*.html, CMS views | Varios (dashboard, edit, etc.) |

---

## Scripts de Inicio

### Docker Compose (producción local)

```powershell
.\start-all.ps1                    # Build + up + espera health + abre navegador
.\start-all.ps1 -NoBuild           # Salta build, solo up
.\start-all.ps1 -NoBrowser         # Sin abrir navegador
```

Secuencia:
1. `docker compose down` — limpia contenedores previos
2. `docker compose build` — compila JAR y construye imagen multi-stage
3. `docker compose up -d` — levanta servicios en background
4. Espera MySQL healthy (healthcheck `mysqladmin ping`)
5. Espera Spring Boot UP (`/actuator/health` → `"status":"UP"`)
6. Abre `http://localhost:8090` en el navegador

### Local (sin Docker)

```powershell
.\start-app.ps1                    # mvn spring-boot:run
.\start-app.ps1 -NoBrowser         # Sin abrir navegador
.\start-app.ps1 -SkipCompile       # Sin compilar primero
```

Requiere MySQL local con las credenciales de `.env`.

---

## Variables de Entorno (`.env`)

```
PORT=8080
NGINX_PORT=8090
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=envios_paraguay_cms
MYSQL_USER=app_user
MYSQL_PASSWORD=changeme_app
SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/envios_paraguay_cms?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=app_user
DB_PASSWORD=changeme_app
DB_DDL_AUTO=update
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
PROMETHEUS_PORT=9090
GRAFANA_PORT=3001
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin123
UPTIME_KUMA_PORT=3002
APP_DEMO_DATA=true
```

---

## Configuración Nginx

### `local.conf` (Docker local — activo para `localhost`)
- Sin CSP headers
- `proxy_pass http://app:8080`
- `proxy_redirect` para ajustar puerto `:8090`

### `monteastur.conf` (producción — `server_name _`)
- CSP completo: `default-src 'self'; script-src 'self' https://unpkg.com; ...`
- Security headers: X-Frame-Options, HSTS, Referrer-Policy, Permissions-Policy
- Sección HTTPS comentada (pendiente de certificado SSL)

---

## Notas Técnicas

### Base de Datos
- `DatabaseInitializer.java` eliminado — la DB debe existir previamente
- `schema.sql` con `CREATE DATABASE IF NOT EXISTS envios_paraguay_cms`
- `ci.yml` actualizado con `envios_paraguay_cms_ci`
- JPA `ddl-auto: update` para tablas

### Monitorización
- Prometheus scrape metrics desde `/actuator/prometheus`
- Grafana auto-provisionado (`monitoring/grafana/provisioning/`)
- Uptime Kuma monitoriza `http://app:8080/actuator/health`

### CI/CD
- GitHub Actions en `.github/workflows/ci.yml`
- Build multi-stage Docker con frontend React + backend Spring
- Empuja imagen a DockerHub (pendiente configurar secrets)

---

## Comandos Útiles

```powershell
# Ver logs en tiempo real
docker compose logs -f

# Ver logs de un servicio específico
docker compose logs -f app
docker compose logs -f nginx

# Consola MySQL
docker exec -it monteastur-mysql mysql -u root -p envios_paraguay_cms

# Health check manual
curl http://localhost:8080/actuator/health

# Compilar sin tests
mvn clean package -DskipTests
```

---

## Próximos Pasos (Sugeridos)

- [ ] Obtener certificado SSL (Let's Encrypt) y descomentar sección HTTPS en `monteastur.conf`
- [ ] Configurar secrets de DockerHub en GitHub Actions para CI/CD
- [ ] Despliegue en VPS real (Koyeb / Render)
- [ ] E2E tests con Playwright
- [ ] Backup automatizado de BD

---

*Documento mantenido manualmente — refleja el estado del repositorio en `feature/seguimiento-premium` a 2026-07-29.*
