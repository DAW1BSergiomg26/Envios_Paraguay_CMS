# Deploy Real Ready Checklist — MonteAstur Envios

Checklist final para el primer despliegue en producción.

> **Estado**: ✅ LISTO para primer deploy real

---

## 1. Tests

- [x] Unit tests (Vitest): **15/15 passed**
- [x] E2E tests (Playwright): **10/10 passed**
- [x] Backend tests (Maven): **11/11 passed**
- [x] Frontend build: **OK** (Vite, PWA generado)
- [x] `test:all` (unit + E2E): **25/25 passed**

---

## 2. Docker

- [x] `docker compose config`: válido
- [x] `docker compose build`: sin errores
- [x] `docker compose up -d`: todos los servicios UP
- [x] Healthcheck: `{"status":"UP"}`
- [x] Restart policies: `unless-stopped` en todos
- [x] Non-root user en contenedor app
- [x] Volúmenes persistentes configurados
- [x] Memory limits configurados
- [x] Solo red interna `backend` (MySQL no expuesto)

---

## 3. Nginx

- [x] `local.conf` funciona en desarrollo (localhost + :8090)
- [x] `monteastur.conf` con security headers activos
- [x] `production-example.conf` documentado para HTTPS
- [x] Gzip compression activa
- [x] `client_max_body_size 10M` configurado
- [x] `proxy_hide_header` para evitar conflictos
- [x] Let's Encrypt challenge endpoint configurado
- [x] Config local y producción separadas

---

## 4. Demo Data

- [x] `application-prod.properties`: `app.demo-data=false`
- [x] `.env.production.example`: `APP_DEMO_DATA=false`
- [x] Solo arranca con `APP_DEMO_DATA=true` si se fuerza explícitamente

---

## 5. Secretos

- [x] `.env` en `.gitignore` (no se sube a Git)
- [x] `.env.production.example` con todos los `CHANGE_ME`
- [ ] `.env` generado en VPS con contraseñas seguras:
  - `openssl rand -base64 32` para cada secreto
  - `ADMIN_PASSWORD`
  - `MYSQL_ROOT_PASSWORD`
  - `MYSQL_PASSWORD` / `DB_PASSWORD`
  - `GRAFANA_ADMIN_PASSWORD`

---

## 6. SSL / HTTPS

- [ ] Dominio comprado y DNS apuntando al VPS
- [ ] Certificado Let's Encrypt obtenido vía certbot Docker
- [ ] `production-example.conf` copiado a `conf.d/` como `monteastur-prod.conf`
- [ ] HTTP → HTTPS redirect funcionando
- [ ] HSTS preload listo (opcional)

---

## 7. Backups

- [ ] Scripts disponibles:
  - `scripts/backup-db.sh` (MySQL → backup/db/)
  - `scripts/backup-uploads.sh` (uploads → backup/uploads/)
  - `scripts/restore-db.sh`
  - `scripts/restore-uploads.sh`
- [ ] Crontab configurado para backups diarios:
  - `0 3 * * * /opt/monteastur/scripts/backup-db.sh`
  - `0 4 * * * /opt/monteastur/scripts/backup-uploads.sh`
- [ ] Retención de 30 días configurada

---

## 8. Rollback

- [ ] `scripts/rollback-prod.sh` listo para usar
- [ ] Tags Git creados para versiones estables
- [ ] Procedimiento documentado en PRODUCTION_VPS_RUNBOOK.md

---

## 9. Monitoring

- [x] Prometheus configurado (recoge métricas cada 15s)
- [x] Grafana provisionado (datasource + dashboard auto-importados)
- [x] Uptime Kuma configurado
- [x] Actuator endpoints: health, info, prometheus

---

## 10. Smoke Tests

- [ ] `scripts/production-smoke-test.sh` ejecutado post-deploy
- [ ] Tests a verificar:
  - `curl -f /actuator/health` → UP
  - Home page carga con security headers
  - Tracking público funciona (`/tracking`)
  - Login admin correcto (`/login`)
  - Login cliente correcto (`/cliente/login`)
  - Dashboard React SPA carga sin errores
  - Upload/subida de imágenes funciona
  - Monitoring (Prometheus, Grafana, Kuma) accesible
  - PWA instalable
  - SSL Labs grade A+
  - Mobile responsive

---

## 11. CI/CD

- [x] GitHub Actions CI configurado (backend + frontend + docker build)
- [x] E2E tests disponibles (manual trigger)
- [x] Deploy workflow (`deploy-prod.yml`) listo con GitHub Secrets
- [x] GitHub Secrets configurados: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`

---

## 12. Documentación

- [x] `docs/PRODUCTION_VPS_RUNBOOK.md` — Runbook completo
- [x] `docs/HARDENING_FINAL_REPORT.md` — Informe de hardening
- [x] `docs/E2E_CI_GUIDE.md` — Guía E2E + CI
- [x] `docs/VPS_HARDENING_CHECKLIST.md` — Hardening VPS
- [x] `docs/FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md` — Checklist 16 fases
- [x] `docs/FIRST_VPS_DEPLOY_CHECKLIST.md` — Checklist deploy inicial
- [x] `.env.production.example` — Plantilla .env producción

---

## Resumen final

| Área | Estado |
|------|--------|
| Tests | ✅ |
| Docker | ✅ |
| Nginx | ✅ |
| Demo data off | ✅ |
| Secretos | ⚠️ Pendiente generar en VPS |
| SSL/HTTPS | ⚠️ Pendiente configurar en VPS |
| Backups | ⚠️ Pendiente configurar cron |
| Rollback | ✅ |
| Monitoring | ✅ |
| Smoke tests | ⚠️ Pendiente ejecutar post-deploy |
| CI/CD | ✅ |
| Documentación | ✅ |

**Decisión: ✅ LISTO para primer deploy real**

Pasos tras el deploy:
1. Ejecutar `scripts/production-smoke-test.sh`
2. Configurar crontab para backups
3. Verificar SSL Labs grade
4. Marcar checklist post-24h en FINAL_PRODUCTION_DEPLOY_CHECKLIST.md