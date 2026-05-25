# Release v20 — Pre-deploy Ready

## Resumen técnico

Release de estabilización y hardening pre-deploy del proyecto **Monteastur Envios**,
plataforma logística premium España ↔ Paraguay.

---

## Features incluidas (hasta v20)

### Fase 18 — Estabilización local y demo data
- **Login local reparado**: redirect `/admin/login` → `/login`, `.env` con credenciales locales
- **Proxy redirect port**: nginx conserva puerto 8090 vía `proxy_redirect` + `$http_host`
- **Demo data expandido**: 4 envíos con eventos timeline, 4 mensajes contacto, 4 reservas, 4 imágenes SVG, 2 textos legales
- **Galería demo**: 4 SVGs (`oficinas-centrales.svg`, `flota-reparto.svg`, `almacen-logistico.svg`, `puerto-gijon.svg`)

### Fase 19 — Refactor naming + E2E + CI
- **Renombrado branding**: `CasaRural` → `Monteastur`, paquete `com.grupb2.casarural` → `com.monteastur.envios`
- **Documentación pre-producción**: auditoría, issues conocidos, comandos locales
- **E2E estabilizado**: Playwright config con screenshots/traces/video on failure, scripts locales `.ps1`/`.sh`
- **CI E2E job**: manual (`workflow_dispatch`) con Docker Compose real

### Fase 20 — Hardening final pre-deploy
- **Seguridad Spring**: revisado y configurado correctamente (admin protegido, CSRF híbrido, session fixation)
- **Config producción**: multipart limits 10MB, `app.demo-data=false`, `DB_DDL_AUTO=validate`
- **Nginx**: local vs producción separados, security headers, proxy headers
- **.env.production.example**: todas las vars requeridas con `CHANGE_ME`
- **Docs hardening**: `HARDENING_FINAL_REPORT.md`, `DEPLOY_REAL_READY_CHECKLIST.md`
- **Scripts predeploy**: `predeploy-check.ps1` + `.sh`
- **Release v20 docs**: este documento + `VPS_REAL_NEXT_ACTIONS.md`

---

## Estado Docker

| Servicio          | Imagen                    | Puerto    | Health | Restart         |
|-------------------|---------------------------|-----------|--------|-----------------|
| `monteastur-app`  | `monteastur-app` (build local) | 8080 | ✅ healthy | unless-stopped |
| `monteastur-nginx` | `nginx:alpine`           | 80/443    | ✅      | unless-stopped |
| `monteastur-mysql` | `mysql:8.0`              | — (int.)  | ✅ healthy | unless-stopped |
| `monteastur-prometheus` | `prom/prometheus:v3.2.1` | 9090 | ✅ | unless-stopped |
| `monteastur-grafana` | `grafana/grafana:11.5.2` | 3000 | ✅ | unless-stopped |
| `monteastur-uptime-kuma` | `louislam/uptime-kuma:1` | 3001 | ✅ healthy | unless-stopped |

### Build
- Multi-stage Docker: Node 20 → Maven 3.9 → Eclipse Temurin 17 JRE
- Non-root user `appuser`
- OCI labels configurados

---

## Estado Tests

### Backend (Maven + Spring Boot)
| Suite | Tests | Estado |
|-------|-------|--------|
| `SecurityConfigTest` | 3 | ✅ |
| `PushSubscriptionControllerTest` | 3 | ✅ |
| `TrackingApiControllerTest` | 3 | ✅ |
| `ReservaServiceTest` | 2 | ✅ |
| **Total** | **11** | ✅ |

### Frontend (Vitest)
| Suite | Tests | Estado |
|-------|-------|--------|
| `StatsCard.test.jsx` | 2 | ✅ |
| `EmptyState.test.jsx` | 2 | ✅ |
| `StatusBadge.test.jsx` | 3 | ✅ |
| `SearchBar.test.jsx` | 4 | ✅ |
| `LoginPage.test.jsx` | 4 | ✅ |
| **Total** | **15** | ✅ |

### E2E (Playwright)
| Suite | Tests | Estado |
|-------|-------|--------|
| `home.spec.js` | 2 | ✅ |
| `login.spec.js` | 3 | ✅ |
| `tracking.spec.js` | 2 | ✅ |
| `dashboard.spec.js` | 2 | ✅ |
| `debug.spec.js` | 1 | ✅ |
| **Total** | **10** | ✅ |

### Build
| Frontend | Estado |
|----------|--------|
| Vite build | ✅ (~600-900ms) |
| PWA service worker | ✅ (12 entries, 980KB precache) |

---

## Estado Hardening

| Área | Estado | Notas |
|------|--------|-------|
| Spring Security | ✅ Revisado | Admin protegido, CSRF híbrido, session fixation |
| Config producción | ✅ Completada | multipart limits, demo data off, validate DDL |
| Nginx seguridad | ✅ Revisado | Security headers, proxy_hide_header, local vs prod |
| Docker seguridad | ✅ Revisado | Non-root user, healthchecks, restart policies, mem limits |
| Plantillas .env | ✅ Completadas | `CHANGE_ME` en todas las credenciales, vars completas |
| Rate limiting | ❌ No implementado | No crítico pre-deploy; añadir si hay ataques |
| CORS | ❌ No configurado | No necesario con frontend same-domain |

Ver `docs/HARDENING_FINAL_REPORT.md` para informe completo.

---

## Estado Documentación VPS

| Documento | Propósito | Estado |
|-----------|-----------|--------|
| `docs/PRODUCTION_VPS_RUNBOOK.md` | Runbook completo | ✅ |
| `docs/VPS_HARDENING_CHECKLIST.md` | Hardening VPS | ✅ |
| `docs/FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md` | Checklist 16 fases | ✅ |
| `docs/FIRST_VPS_DEPLOY_CHECKLIST.md` | Checklist inicial | ✅ |
| `docs/VPS_REAL_NEXT_ACTIONS.md` | Pasos concretos para VPS | ✅ (nuevo) |
| `docs/RELEASE_V20_READY.md` | Este documento | ✅ (nuevo) |
| `scripts/deploy-prod.sh` | Deploy automático | ✅ |
| `scripts/rollback-prod.sh` | Rollback | ✅ |
| `scripts/vps-bootstrap.sh` | Bootstrap VPS | ✅ |
| `scripts/production-smoke-test.sh` | Smoke tests post-deploy | ✅ |
| `scripts/predeploy-check.ps1` | Pre-deploy check local | ✅ (nuevo) |
| `scripts/predeploy-check.sh` | Pre-deploy check local | ✅ (nuevo) |

---

## Credenciales temporales locales

| Rol | URL | Usuario | Contraseña |
|-----|-----|---------|------------|
| Admin (Spring Security) | `http://localhost:8090/login` | `admin` | `admin123` |
| Cliente (custom session) | `http://localhost:8090/cliente/login` | email: `cliente@monteastur.com` | `demo2026` |
| React SPA | `http://localhost:8090/login-react` | `admin` | `admin123` |
| Grafana | `http://localhost:3001` | `admin` | `admin123` |

> **IMPORTANTE:** Estas credenciales son SOLO para desarrollo local.
> En producción, generar contraseñas seguras con `openssl rand -base64 32`.

---

## Advertencias producción

1. **Cambiar contraseñas**: `admin123`, `demo2026`, `changeme_*` son para desarrollo. Generar nuevas.
2. **`DB_DDL_AUTO`**: Usar `update` solo en primer arranque. Cambiar a `validate` después.
3. **`APP_DEMO_DATA`**: Asegurar `false` en producción (`.env.production.example` ya lo tiene).
4. **HTTPS**: No exponer HTTP en producción sin SSL. El redirect 301 está documentado.
5. **Backups**: Configurar crontab para backups automáticos post-deploy.
6. **Monitoring**: Verificar Prometheus + Grafana + Uptime Kuma accesibles.
7. **GitHub Secrets**: Configurar `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY` antes del deploy automático.
8. **fail2ban**: Configurar en VPS para proteger SSH y servicios expuestos.
9. **UFW**: Solo puertos 22, 80, 443 abiertos.
10. **Rollback**: Tener identificado el tag estable para rollback rápido.

---

## Checklist final antes de VPS

- [x] Tests backend: 11/11
- [x] Tests frontend unit: 15/15
- [x] Tests E2E: 10/10
- [x] Frontend build: OK + PWA
- [x] Docker compose: todos los servicios UP
- [x] Docker build multi-stage: OK
- [x] Docker non-root user: OK
- [x] Healthcheck: `{"status":"UP"}`
- [x] Nginx security headers: configurados
- [x] Nginx local vs prod: separados
- [x] Demo data off en prod: configurado
- [x] .env.production.example: completo + CHANGE_ME
- [x] Docs hardening: `HARDENING_FINAL_REPORT.md`
- [x] Docs deploy: `DEPLOY_REAL_READY_CHECKLIST.md`
- [x] Docs release: `RELEASE_V20_READY.md`
- [x] Docs VPS: `VPS_REAL_NEXT_ACTIONS.md`
- [x] Scripts predeploy: `.ps1` + `.sh`
- [x] Scripts deploy prod: `deploy-prod.sh`
- [x] Scripts rollback: `rollback-prod.sh`
- [x] Scripts smoke tests: `production-smoke-test.sh`
- [x] CI/CD: GitHub Actions configurado
- [x] E2E CI: manual trigger disponible

---

## Tags sugeridos

```bash
git tag -a v20.0-pre-deploy -m "v20.0 — Pre-deploy ready"
git tag -a v20.0 -m "v20.0 — Primer deploy real"
```

## Comandos para merge y tag

```bash
# Merge a develop
git checkout develop
git merge --no-ff feature/fase-20-hardening-deploy-real -m "merge: integrar fase-20-hardening-deploy-real a develop"
git push origin develop

# Tag release
git tag -a v20.0-pre-deploy -m "v20.0 — Pre-deploy: hardening + E2E + CI + docs"
git push origin v20.0-pre-deploy

# Merge a main (para deploy)
git checkout main
git merge --no-ff develop -m "release: v20.0 pre-deploy"
git push origin main

# Tag estable en main
git tag -a v20.0 -m "v20.0 — Primer deploy real"
git push origin v20.0
```