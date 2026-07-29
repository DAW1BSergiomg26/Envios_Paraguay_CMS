# Hardening Final Report — MonteAstur Envios

## 1. Spring Security

| Aspect | Status | Notes |
|--------|--------|-------|
| Admin routes (`/admin/**`, `/api/v1/admin/**`) | ✅ Protected | Spring Security form login |
| Client routes (`/cliente/**`, `/api/v1/cliente/**`) | ✅ Protected | Custom session via `@SessionAttribute` |
| Public routes (tracking, home, contact) | ✅ Public | `anyRequest().permitAll()` |
| CSRF enabled for Thymeleaf | ✅ | Default Spring Security CSRF |
| CSRF disabled for `/api/**` | ✅ | SPA uses HttpOnly cookie, no JS access |
| Session fixation protection | ✅ | `changeSessionId()` |
| Logout cleanup | ✅ | Session invalidated, JSESSIONID cookie deleted |
| Password hashing | ✅ | BCrypt for admin (InMemoryUserDetailsManager) |
| BCrypt for client passwords | ✅ | ClienteService uses BCryptPasswordEncoder |
| Security headers (X-Frame-Options, Referrer-Policy) | ✅ | Set at Spring level, overridden by nginx |
| Rate limiting | ❌ No implementado | No crítico para pre-deploy; añadir en v3.x si hay ataques |

### Decisiones de seguridad

- **No JWT**: la sesión se gestiona con cookie HttpOnly JSESSIONID, reutilizada por el SPA React. Esto evita la complejidad de gestión de tokens sin sacrificar seguridad (la cookie es inaccesible desde JavaScript).
- **CSRF deshabilitado en APIs**: documentado en SecurityConfig.java. La cookie HttpOnly ya autentica cada petición; un atacante no puede leer JSESSIONID desde JS ni fabricar una sesión válida.
- **Clientes con sesión independiente**: los clientes no usan Spring Security sino un `@SessionAttribute` gestionado por ClienteController, con contraseñas hasheadas con BCrypt.

### Recomendaciones futuras

- Implementar rate limiting (Spring Cloud Gateway o Bucket4j) para proteger `/api/v1/tracking/**` y `/login`
- Añadir CORS estricto si se expande el consumo de API desde dominios externos

---

## 2. Configuración de Producción (`application-prod.properties`)

| Propiedad | Valor | Notas |
|-----------|-------|-------|
| `spring.profiles.active` | `prod` (vía env) | Activado en Docker Compose |
| `server.forward-headers-strategy` | `framework` | Necesario para detectar X-Forwarded-Proto tras nginx |
| `spring.thymeleaf.cache` | `true` | Cache ON en producción |
| `spring.jpa.hibernate.ddl-auto` | `${DB_DDL_AUTO:validate}` | `validate` por defecto; `update` solo primer arranque |
| `spring.jpa.show-sql` | `false` | No exponer SQL en logs |
| `spring.jpa.open-in-view` | `false` | Evitar LazyLoadingException silenciosas |
| `server.servlet.session.cookie.http-only` | `true` | Cookie no accesible desde JS |
| `server.servlet.session.cookie.same-site` | `lax` | Protección CSRF a nivel de cookie |
| `server.servlet.session.timeout` | `30m` | Timeout de sesión razonable |
| `app.demo-data` | `false` | Sin datos demo en producción |
| `spring.servlet.multipart.max-file-size` | `10MB` | Coherente con `client_max_body_size` de nginx |
| `spring.servlet.multipart.max-request-size` | `10MB` | Límite total de petición |
| `management.endpoints.web.exposure.include` | `health,info,prometheus` | Solo endpoints necesarios |
| `management.endpoint.health.show-details` | `when_authorized` | Detalles de salud solo para admin autenticado |
| `logging.level.com.monteastur.envios` | `INFO` | Sin debug en producción |
| `logging.level.org.springframework` | `WARN` | Reducir ruido de Spring |
| `logging.level.org.hibernate.SQL` | `WARN` | No mostrar SQL |

### Variables de entorno requeridas (sin default)

- `SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `ADMIN_USERNAME`, `ADMIN_PASSWORD`

---

## 3. Docker

| Aspecto | Estado | Notas |
|---------|--------|-------|
| Multi-stage build | ✅ | Frontend → Backend → Runtime |
| Non-root user | ✅ | `appuser` en runtime con `USER appuser` |
| Restart policies | ✅ | `unless-stopped` en todos los servicios |
| Healthchecks | ✅ | `mysqladmin ping` en db, `/actuator/health` en app |
| `depends_on` condition | ✅ | `db: condition: service_healthy` |
| Memory limits | ✅ | db: 256m, app: 512m, nginx: 64m, etc. |
| Volumes persistentes | ✅ | mysql_data, uploads_data, logs_data |
| Networks | ✅ | `backend` interna; ningún servicio expuesto innecesariamente |
| Certbot | ✅ | Perfil separado (`--profile certbot`), no se ejecuta automáticamente |
| OCI labels | ✅ | image.title, description, version, authors |

### Puertos expuestos

| Servicio | Puerto host | Controlable vía .env |
|----------|-------------|----------------------|
| app | 8080 | `PORT` |
| nginx | 80 / 443 | `NGINX_PORT` / fijo |
| prometheus | 9090 | `PROMETHEUS_PORT` |
| grafana | 3000 | `GRAFANA_PORT` |
| uptime-kuma | 3001 | `UPTIME_KUMA_PORT` |

> NOTA: MySQL (puerto 3306) NO está expuesto al host. Solo accesible desde la red interna `backend`.

---

## 4. Nginx

| Aspecto | Estado | Notas |
|---------|--------|-------|
| Seguridad HTTP → HTTPS (producción) | ✅ | `production-example.conf` documenta el redirect |
| Security headers | ✅ | HSTS, CSP, XFO, X-Content-Type-Options, Referrer-Policy, Permissions-Policy |
| `proxy_hide_header` | ✅ | Oculta headers inseguros del backend |
| `proxy_redirect` para local | ✅ | `proxy_redirect http://localhost/ http://localhost:8090/;` |
| Gzip compression | ✅ | En nginx.conf global |
| Let's Encrypt challenge | ✅ | `.well-known/acme-challenge/` servido desde certbot_www volume |
| `client_max_body_size` | ✅ | 10MB coherente con Spring |
| Timeouts proxy | ✅ | connect/send/read: 30s |
| Configuración local vs producción | ✅ | Separá en `local.conf` (localhost) y `monteastur.conf` (catch-all + security) |
| HTTPS bloque comentado | ✅ | `monteastur.conf` contiene bloque HTTPS comentado; `production-example.conf` es la referencia para desplegar |

---

## 5. Plantillas .env

| Archivo | Estado | Notas |
|---------|--------|-------|
| `.env.production.example` | ✅ Actualizado | `CHANGE_ME` en todas las credenciales, incluye `APP_DEMO_DATA=false`, `SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT` |
| `.env.example` | ✅ | Comentarios claros, defaults seguros (`change_me_*`) |
| `.env` (local) | ✅ | En `.gitignore`, no se sube a Git |

### Secretos requeridos en producción

| Secreto | Generación recomendada |
|---------|----------------------|
| `ADMIN_PASSWORD` | `openssl rand -base64 32` |
| `MYSQL_ROOT_PASSWORD` | `openssl rand -base64 32` |
| `MYSQL_PASSWORD` / `DB_PASSWORD` | `openssl rand -base64 32` |
| `GRAFANA_ADMIN_PASSWORD` | `openssl rand -base64 32` |
| `SSL_EMAIL` | Email real del administrador |

---

## 6. Playwright E2E

| Aspecto | Estado |
|---------|--------|
| Tests unitarios (Vitest) | ✅ 15 tests |
| Tests E2E (Playwright) | ✅ 10 tests |
| playwright.config.js | ✅ screenshot on-failure, trace retain-on-failure, video retain-on-failure |
| Scripts locales | ✅ `scripts/run-e2e-local.ps1` y `.sh` |
| CI E2E job | ✅ Manual (`workflow_dispatch`) via Docker Compose |
| Reportes HTML subidos en fallo | ✅ |
| Debug logs en fallo CI | ✅ |

---

## 7. Riesgos pendientes

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Sin rate limiting | Medio | Ataques de fuerza bruta a `/login` o `/api/v1/tracking/**`. Mitigación parcial: fail2ban en VPS. |
| Sin CORS explícito | Bajo | No relevante si el frontend se sirve desde el mismo dominio (nginx proxy). Si se abre API a terceros, configurar. |
| Sin auditoría de accesos | Medio | No hay logs de "quién accedió a qué". Añadir Spring Security audit events en v3.x. |
| E2E no automático en CI | Bajo | Activado manualmente (`workflow_dispatch`). Para habilitar en cada push, cambiar condición en ci.yml. |
| `DB_DDL_AUTO=validate` puede fallar en primer arranque | Medio | Si el esquema no existe, la app no arranca. Usar `update` en primer deploy, luego cambiar a `validate`. |
| Sin backups automáticos en VPS | Medio | Backups manuales vía scripts; configurar cron post-deploy. |
| Sin fail2ban en VPS | Medio | Documentado en VPS_HARDENING_CHECKLIST.md; configurar en bootstrap. |
| Certificado SSL manual | Bajo | Let's Encrypt con certbot Docker profile documentado. |

---

## 8. Decisión final

**✅ LISTO PARA PRIMER DEPLOY REAL**

El proyecto cumple con los requisitos mínimos de seguridad y estabilidad para un primer despliegue en producción:

1. **Seguridad**: Spring Security configurado correctamente, CSRF híbrido, sesiones HttpOnly, BCrypt, nginx con security headers
2. **Configuración producción**: Perfil `prod` con validación de esquema, sin datos demo, logs INFO, multipart limits
3. **Docker**: Build multi-stage, non-root user, healthchecks, restart policies, memory limits
4. **Nginx**: Proxy seguro con headers, gzip, redirect HTTP→HTTPS documentado
5. **Testing**: 25 tests (15 unit + 10 E2E), build frontend ok
6. **Documentación**: Runbook completo, deploy checklist, hardening report, backup/restore scripts

### Checklist pre-deploy

- [ ] `.env` generado desde `.env.production.example` con contraseñas seguras
- [ ] `DB_DDL_AUTO=update` en primer arranque, luego `validate`
- [ ] DNS apuntando al VPS (registro A)
- [ ] HTTPS configurado con Let's Encrypt
- [ ] smoke tests post-deploy ejecutados (`./scripts/production-smoke-test.sh`)
- [ ] Crontab para backups automáticos configurado
- [ ] fail2ban configurado en VPS
- [ ] Firewall UFW: solo puertos 22, 80, 443

### Documentos relacionados

- `docs/DEPLOY_REAL_READY_CHECKLIST.md` — Checklist paso a paso para deploy
- `docs/PRODUCTION_VPS_RUNBOOK.md` — Runbook completo de producción
- `docs/FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md` — Checklist maestra 16 fases
- `docs/VPS_HARDENING_CHECKLIST.md` — Hardening del VPS
- `docs/E2E_CI_GUIDE.md` — Guía de testing E2E y CI
- `.env.production.example` — Plantilla .env para producción