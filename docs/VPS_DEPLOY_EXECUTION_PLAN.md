# VPS Deploy Execution Plan — MonteAstur Envios

## Release base

| Elemento | Valor |
|----------|-------|
| Tag Git | `v20.0-pre-deploy` |
| Rama | `main` |
| Commit | Último del merge de `develop` a `main` |
| Estado tests | 25/25 (15 unit + 10 E2E), 11 backend, build OK |

---

## Proveedor recomendado

| Proveedor | Plan | vCPU | RAM | SSD | Precio/mes | Por qué |
|-----------|------|------|-----|-----|------------|---------|
| **Hetzner** | CX22 | 2 | 4 GB | 40 GB | ~€4.50 | Mejor relación calidad/precio, datacenter Europa |
| Contabo | Cloud S | 4 | 8 GB | 200 GB | ~€6.99 | Alternativa si se necesita más almacenamiento |

> Decisión: **Hetzner CX22** — suficiente para Spring Boot + MySQL + Nginx + monitoring.

### Imagen recomendada

- Ubuntu 24.04 LTS
- Sin panel (solo SSH)
- Clave SSH en lugar de contraseña

---

## Dominio recomendado

| Elemento | Valor sugerido | Alternativa |
|----------|----------------|-------------|
| Dominio | `monteastur.com` | `monteastur.es`, `enviosmonteastur.com` |
| Proveedor | Cloudflare Registrar | Namecheap, GoDaddy |
| Coste | ~€9.15/año | ~€10-12/año |
| WHOIS privado | ✅ Incluido en Cloudflare | Puede ser extra en otros |

---

## DNS recomendado

| Registro | Tipo | Valor | TTL |
|----------|------|-------|-----|
| `@` | A | `IP_DEL_VPS` | 300 (luego 3600) |
| `www` | CNAME | `monteastur.com` | 300 (luego 3600) |
| `grafana` | A | `IP_DEL_VPS` | 3600 |
| `uptime` | A | `IP_DEL_VPS` | 3600 |

> Si usas Cloudflare: poner DNS Only (gris) hasta que SSL esté configurado, luego cambiar a Proxied (naranja).

---

## Variables reales necesarias

Antes del deploy, generar y documentar (en gestor de contraseñas, NO en Git):

| Variable | Generación | Longitud recomendada |
|----------|-----------|---------------------|
| `ADMIN_USERNAME` | Manual | 8-16 caracteres |
| `ADMIN_PASSWORD` | `openssl rand -base64 32` | 44 caracteres |
| `MYSQL_ROOT_PASSWORD` | `openssl rand -base64 32` | 44 caracteres |
| `MYSQL_USER` | Manual | `casarural_app` |
| `MYSQL_PASSWORD` | `openssl rand -base64 32` | 44 caracteres |
| `DB_USERNAME` | = `MYSQL_USER` | - |
| `DB_PASSWORD` | = `MYSQL_PASSWORD` | - |
| `GRAFANA_ADMIN_USER` | `admin` | 5 caracteres |
| `GRAFANA_ADMIN_PASSWORD` | `openssl rand -base64 32` | 44 caracteres |
| `SSL_EMAIL` | Email real del admin | - |

---

## Secretos GitHub necesarios

| Secret | Valor | Dónde obtenerlo |
|--------|-------|-----------------|
| `VPS_HOST` | IP del VPS | Hetzner dashboard |
| `VPS_USER` | `deploy` | Creado en bootstrap |
| `VPS_SSH_KEY` | Clave privada OpenSSH | `ssh-keygen -t ed25519` |
| `VPS_PORT` | `22` | Puerto SSH estándar |

---

## Orden de ejecución real

```
FASE 0: PREPARACIÓN (30 min)
  ├── 0.1 Comprar VPS Hetzner CX22
  ├── 0.2 Comprar dominio Cloudflare
  ├── 0.3 Configurar DNS (registro A)
  ├── 0.4 Generar contraseñas con openssl
  └── 0.5 Guardar todo en gestor de contraseñas

FASE 1: VPS BOOTSTRAP (20 min)
  ├── 1.1 SSH como root con IP temporal
  ├── 1.2 Crear usuario deploy
  ├── 1.3 Copiar clave SSH pública
  └── 1.4 Ejecutar scripts/vps-bootstrap.sh

FASE 2: DOCKER (30 min)
  ├── 2.1 git clone en /opt/monteastur
  ├── 2.2 Crear .env con valores generados
  ├── 2.3 DB_DDL_AUTO=update (primer arranque)
  ├── 2.4 docker compose up -d --build
  ├── 2.5 Esperar healthcheck + verificar
  ├── 2.6 Cambiar DB_DDL_AUTO=validate
  └── 2.7 docker compose up -d (recargar)

FASE 3: SSL (15 min)
  ├── 3.1 Verificar DNS propagado
  ├── 3.2 Obtener certificado Let's Encrypt
  ├── 3.3 Copiar certificados a nginx/ssl/
  ├── 3.4 Activar HTTPS (production-example.conf)
  └── 3.5 Verificar HTTPS funciona

FASE 4: GITHUB ACTIONS (10 min)
  ├── 4.1 Configurar GitHub Secrets
  ├── 4.2 Ejecutar workflow manual deploy-prod
  └── 4.3 Verificar que pasa

FASE 5: SMOKE TESTS (15 min)
  ├── 5.1 Ejecutar production-smoke-test.sh
  ├── 5.2 Verificar healthcheck
  ├── 5.3 Verificar login admin
  ├── 5.4 Verificar tracking público
  └── 5.5 Verificar monitoring

FASE 6: HARDENING (15 min)
  ├── 6.1 Configurar fail2ban
  ├── 6.2 Verificar UFW (22, 80, 443)
  ├── 6.3 Configurar crontab backups
  └── 6.4 Verificar logs sin errores

FASE 7: MONITORING 24H
  ├── 7.1 Prometheus targets UP
  ├── 7.2 Grafana dashboard sin gaps
  ├── 7.3 Uptime Kuma al 100%
  └── 7.4 Sin errores en logs
```

### Tiempo total estimado

| Fase | Tiempo | Depende de |
|------|--------|------------|
| Fase 0 (preparación) | 30 min | Nada |
| Fase 1 (bootstrap) | 20 min | VPS activo |
| Fase 2 (Docker) | 30 min | Fase 1 |
| Fase 3 (SSL) | 15 min | DNS propagado (~5-30 min) |
| Fase 4 (GitHub) | 10 min | Fase 2 |
| Fase 5 (smoke) | 15 min | Fase 2+3 |
| Fase 6 (hardening) | 15 min | Fase 2 |
| **Total** | **~2h 15min** | - |

> **Nota**: La propagación DNS puede tardar más. Si es así, SSL se retrasa, pero el resto puede continuar.

---

## Checklist antes de tocar VPS

- [ ] Release v20.0-pre-deploy creada y taggeada en GitHub
- [ ] Tests pasan en local (25/25 + 11 backend)
- [ ] Docker compose build funciona en local
- [ ] Todos los contenedores UP en local
- [ ] Healthcheck UP en local
- [ ] Todas las URLs 200 OK en local
- [ ] `.env.production.example` completo y actualizado
- [ ] Script `vps-bootstrap.sh` listo
- [ ] Script `deploy-prod.sh` listo
- [ ] Script `rollback-prod.sh` listo
- [ ] Script `production-smoke-test.sh` listo
- [ ] Docs de hardening y deploy actualizados

---

## Checklist después del deploy

- [ ] `curl -f https://monteastur.com/actuator/health` → UP
- [ ] `curl -I https://monteastur.com` → 200 + security headers
- [ ] Login admin funciona (https://monteastur.com/login)
- [ ] Login cliente funciona (https://monteastur.com/cliente/login)
- [ ] Tracking público funciona (https://monteastur.com/tracking)
- [ ] Dashboard React SPA carga sin errores
- [ ] Prometheus targets UP
- [ ] Grafana accesible con credenciales
- [ ] Uptime Kuma monitorizando
- [ ] Backups programados en crontab
- [ ] SSL Labs grade ≥ A
- [ ] fail2ban activo
- [ ] UFW solo puertos 22, 80, 443

---

## Rollback plan

### Si el deploy falla durante la ejecución

```bash
# 1. Identificar el fallo
docker compose logs app --tail 50

# 2. Si es configurable (.env, DDL), corregir y reiniciar
nano .env
docker compose up -d

# 3. Si no se puede corregir, restaurar tag anterior
git checkout v20.0-pre-deploy
docker compose up -d --build

# 4. Verificar healthcheck
curl -f http://localhost/actuator/health
```

### Si el deploy parece OK pero algo va mal en 24h

```bash
# Rollback al tag estable
cd /opt/monteastur && ./scripts/rollback-prod.sh v20.0-pre-deploy

# Verificar
curl -f http://localhost/actuator/health
docker compose ps
```

### Decisión de rollback

| Síntoma | Decisión |
|---------|----------|
| Healthcheck UP → DOWN | Rollback inmediato |
| Login no funciona | Rollback inmediato |
| API no responde | Rollback inmediato |
| SSL no funciona | Rollback inmediato |
| Monitoring caído pero app OK | No rollback, investigar |
| Rendimiento lento | No rollback, escalar primero |

---

## Monitorización 24h

| Qué monitorizar | Frecuencia | Quién |
|-----------------|------------|-------|
| Uptime Kuma dashboard | Automático | Dev team |
| Grafana: app status, CPU, RAM | Automático | Dev team |
| Prometheus targets | Cada 15s | Automático |
| Logs app (errores WARN+) | Cada hora | Dev team |
| Logs nginx (5xx) | Cada hora | Dev team |
| Logs MySQL (conexiones) | Cada hora | Dev team |
| Backups automáticos | Diario | Automático |
| SSL expiry | Mensual | Automático (certbot) |

### Alertas configurables

| Evento | Canal | Prioridad |
|--------|-------|-----------|
| Healthcheck DOWN | Email + Slack | 🔴 Crítica |
| 5xx rate > 1% | Email | 🟡 Media |
| Disco > 80% | Email | 🟡 Media |
| SSL expires < 30 días | Email | 🟡 Media |
| Backups fallan | Email | 🟡 Media |

---

## Documentos relacionados

| Documento | Enlace |
|-----------|--------|
| Day Runbook | `docs/VPS_DEPLOY_DAY_RUNBOOK.md` |
| Secrets Template | `docs/PRODUCTION_SECRETS_TEMPLATE.md` |
| Risk Register | `docs/FIRST_DEPLOY_RISK_REGISTER.md` |
| VPS Next Actions | `docs/VPS_REAL_NEXT_ACTIONS.md` |
| Hardening Report | `docs/HARDENING_FINAL_REPORT.md` |
| Deploy Checklist | `docs/DEPLOY_REAL_READY_CHECKLIST.md` |
| Release Notes | `docs/RELEASE_V20_READY.md` |