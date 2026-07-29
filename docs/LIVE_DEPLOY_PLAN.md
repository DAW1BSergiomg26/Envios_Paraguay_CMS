# Monteastur Envios — Plan de Deploy Real Online

> **Versión:** 1.0 | **Fecha:** 2026-05-24
> **Estado:** LISTO PARA EJECUTAR
> **Branch:** `feature/fase-16-vps-online` → merge a `develop` → deploy

---

## Índice

1. [Proveedor recomendado](#1-proveedor-recomendado)
2. [Dominio y DNS](#2-dominio-y-dns)
3. [Orden real de despliegue](#3-orden-real-de-despliegue)
4. [Checklist de seguridad](#4-checklist-de-seguridad)
5. [Checklist de validación](#5-checklist-de-validación)
6. [Plan de rollback](#6-plan-de-rollback)
7. [Plan de costes](#7-plan-de-costes)

---

## 1. Proveedor recomendado

### Hetzner CX22

| Recurso | Valor |
|---------|-------|
| Proveedor | **Hetzner** ([cloud.hetzner.com](https://cloud.hetzner.com)) |
| Plan | CX22 |
| vCPU | 2 |
| RAM | 4 GB |
| SSD | 40 GB |
| Tráfico | 20 TB/mes |
| IP fija | Sí (incluida) |
| SO | Ubuntu 22.04 LTS |
| Precio | **~€4.50/mes** |
| Data center | Finlandia o Alemania |

### Alternativas

| Proveedor | Plan | Precio | Diferencia |
|-----------|------|--------|------------|
| Contabo | Cloud S (4 vCPU, 8 GB, 200 GB) | ~€6.99/mes | Más recursos, rendimiento medio |
| DigitalOcean | Basic (2 vCPU, 4 GB, 80 GB) | ~$24/mes | Más caro, menos tráfico |

### Pasos para contratar

1. Ir a https://www.hetzner.com/cloud
2. Crear cuenta (nombre, email, dirección, teléfono)
3. Verificar identidad (subir documento, esperar ~24h)
4. Ir a **Projects → New Project**
5. **Add Server → CX22**
6. Image: **Ubuntu 22.04 LTS**
7. Añadir **SSH Key** (generar con `ssh-keygen -t ed25519`)
8. **Create & Buy** (~5 min)

---

## 2. Dominio y DNS

### Registro A principal

| Tipo | Nombre | Valor | TTL |
|------|--------|-------|-----|
| A | `@` | `<IP_DEL_VPS>` | 300 (setup) → 3600 (estable) |
| A | `www` | `<IP_DEL_VPS>` | 300 (setup) → 3600 (estable) |

### Subdominios opcionales

| Subdominio | Servicio | Puerto | Nginx extra |
|------------|----------|--------|-------------|
| `app.monteastur.com` | App principal | 80/443 | No (ya en server_name) |
| `grafana.monteastur.com` | Grafana | 3000 | Sí (bloque server adicional) |
| `uptime.monteastur.com` | Uptime Kuma | 3001 | Sí (bloque server adicional) |

> **Recomendación:** Para el primer deploy, usar solo el dominio principal. Los subdominios se pueden añadir después.

### Verificación

```bash
# Antes de continuar, asegurar que DNS está propagado
dig +short monteastur.com
# → debe mostrar la IP del VPS
```

---

## 3. Orden real de despliegue

**Tiempo estimado:** ~45 min (sin incluir verificación Hetzner ni propagación DNS)
**Ejecutar en orden estricto. No saltar pasos.**

### Paso 1: Comprar VPS (~10 min + ~24h verificación)

```bash
# 1. Crear cuenta en Hetzner Cloud
# 2. Verificar identidad (documento)
# 3. Contratar CX22 con Ubuntu 22.04
# 4. Anotar IP asignada (ej: 203.0.113.10)
# 5. Añadir SSH key durante la creación
```

> ⚠️ La verificación de identidad en Hetzner puede tardar hasta 24h.
> Hacer este paso con antelación.

### Paso 2: Conectar y crear usuario deploy (~5 min)

```bash
# Conectar como root (usando SSH key de Hetzner)
ssh root@<IP_DEL_VPS>

# Crear usuario deploy
adduser deploy
usermod -aG docker deploy
usermod -aG sudo deploy

# Copiar clave SSH de root a deploy
cp -r ~/.ssh /home/deploy/
chown -R deploy:deploy /home/deploy/.ssh
chmod 700 /home/deploy/.ssh
chmod 600 /home/deploy/.ssh/authorized_keys

# Salir de root
exit
```

### Paso 3: Ejecutar bootstrap (~5 min)

```bash
# Conectar como deploy (primera vez)
ssh deploy@<IP_DEL_VPS>

# Descargar y ejecutar bootstrap
sudo apt update && sudo apt install -y git curl
git clone https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS.git /opt/monteastur
cd /opt/monteastur
sudo ./scripts/vps-bootstrap.sh

# Verificar Docker
docker --version
docker compose version
```

### Paso 4: Clonar repo + .env (~5 min)

```bash
# El repo ya está clonado en /opt/monteastur (paso 3)
cd /opt/monteastur

# Crear .env con credenciales seguras
cp .env.example .env
nano .env
```

**Variables a cambiar obligatoriamente:**

| Variable | Generar con |
|----------|-------------|
| `MYSQL_ROOT_PASSWORD` | `openssl rand -base64 32` |
| `MYSQL_PASSWORD` | `openssl rand -base64 32` |
| `DB_PASSWORD` | `openssl rand -base64 32` |
| `ADMIN_PASSWORD` | `openssl rand -base64 32` |
| `GRAFANA_ADMIN_PASSWORD` | `openssl rand -base64 16` |

**Variables a configurar:**

```bash
SPRING_PROFILES_ACTIVE=prod
TZ=America/Asuncion
SSL_EMAIL=admin@monteastur.com
```

### Paso 5: Primer docker compose (~10 min)

```bash
cd /opt/monteastur
docker compose build
docker compose up -d

# Verificar
docker ps
# → 6/6 containers UP

# Healthcheck
curl -f http://localhost/actuator/health
# → {"status":"UP"}
```

### Paso 6: Configurar DNS (~5 min + propagación)

```bash
# 1. Ir al panel del dominio
# 2. Crear registro A: @ → <IP_DEL_VPS>
# 3. Crear registro A: www → <IP_DEL_VPS>
# 4. TTL: 300

# Verificar propagación
dig +short monteastur.com
# Si no se propaga, continuar con IP para pruebas
```

### Paso 7: Configurar HTTPS (~10 min)

```bash
# Verificar que HTTP funciona con el dominio
curl -I http://monteastur.com

# Obtener certificado
docker compose --profile certbot run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d monteastur.com -d www.monteastur.com \
  --email admin@monteastur.com \
  --agree-tos --no-eff-email

# Copiar certificados
cp /etc/letsencrypt/live/monteastur.com/fullchain.pem nginx/ssl/
cp /etc/letsencrypt/live/monteastur.com/privkey.pem nginx/ssl/

# Editar nginx/conf.d/monteastur.conf
# - Descomentar bloque HTTPS (líneas 39-73)
# - Cambiar server_name a monteastur.com www.monteastur.com

# Recargar nginx
docker compose restart nginx

# Verificar
curl -I https://monteastur.com
# → HTTP/2 200 + security headers
```

### Paso 8: Generar SSH key para GitHub Actions (~2 min)

```bash
ssh-keygen -t ed25519 -f ~/.ssh/github-actions -N ""
cat ~/.ssh/github-actions.pub >> ~/.ssh/authorized_keys
cat ~/.ssh/github-actions
# → Copiar toda la salida (incluyendo BEGIN/END)
```

### Paso 9: Crear GitHub Secrets (~5 min)

Ir a **GitHub → Settings → Secrets and variables → Actions**

| Secret | Valor |
|--------|-------|
| `VPS_HOST` | `<IP_DEL_VPS>` |
| `VPS_USER` | `deploy` |
| `VPS_SSH_KEY` | Clave copiada en paso 8 |
| `VPS_PORT` | `22` |

### Paso 10: Ejecutar workflow manual (~5 min)

```bash
# En GitHub:
# Actions → Deploy Production → Run workflow
# Branch: develop
# Confirm: "deploy"
```

### Paso 11: Verificar monitoring (~5 min)

```bash
# Healthcheck local
./scripts/server-healthcheck.sh

# Prometheus
curl http://localhost:9090/targets

# Grafana
# Abrir http://<IP_VPS>:3000 en navegador
# Login: admin / <GRAFANA_ADMIN_PASSWORD>

# Uptime Kuma
# Abrir http://<IP_VPS>:3001 en navegador
# Configurar monitor: https://monteastur.com
```

### Paso 12: Configurar backups y renovación SSL (~5 min)

```bash
# Configurar crontab
crontab -e
```

```cron
# Backup BD
0 3 * * * /opt/monteastur/scripts/backup-db.sh
# Backup uploads
0 4 * * * /opt/monteastur/scripts/backup-uploads.sh
# Rotación 30 días
0 5 * * * find /opt/monteastur/backup -name "*.sql.gz" -mtime +30 -delete
0 5 * * * find /opt/monteastur/backup -name "*.tar.gz" -mtime +30 -delete
# Renovación SSL
0 3 * * * cd /opt/monteastur && docker compose --profile certbot run --rm certbot renew && docker compose restart nginx
# Prune Docker semanal
0 6 * * 0 docker image prune -af
```

### Paso 13: Asegurar SSH (~2 min)

```bash
sudo nano /etc/ssh/sshd_config
```

Asegurar:

```ini
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
AllowUsers deploy
```

```bash
sudo systemctl restart sshd
```

### Paso 14: fail2ban (~3 min)

```bash
sudo apt install -y fail2ban
sudo cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local
# Verificar sección [sshd] tiene enabled=true
sudo systemctl enable --now fail2ban
```

### Paso 15: unattended-upgrades (~2 min)

```bash
sudo apt install -y unattended-upgrades
sudo dpkg-reconfigure --priority=low unattended-upgrades
```

---

## 4. Checklist de seguridad

### SSH

- [ ] `PermitRootLogin no`
- [ ] `PasswordAuthentication no`
- [ ] `PubkeyAuthentication yes`
- [ ] `AllowUsers deploy`
- [ ] Clave SSH única para GitHub Actions

### Firewall

- [ ] UFW activo: `sudo ufw status`
- [ ] Puertos: 22, 80, 443
- [ ] `default deny incoming`
- [ ] `default allow outgoing`

### fail2ban

- [ ] fail2ban instalado: `sudo fail2ban-client status`
- [ ] Jail SSH activo: `sudo fail2ban-client status sshd`
- [ ] `maxretry ≤ 5`
- [ ] `bantime ≥ 600`

### Actualizaciones

- [ ] unattended-upgrades instalado
- [ ] Solo updates de seguridad
- [ ] No reboot automático

### Docker

- [ ] `restart: unless-stopped` en todos los servicios
- [ ] Límites de memoria configurados
- [ ] Healthchecks activos
- [ ] Prune semanal configurado

### Backups

- [ ] Backup MySQL funciona
- [ ] Backup uploads funciona
- [ ] Backup .env configurado
- [ ] Rotación >30 días
- [ ] Restore probado

### SSL

- [ ] Certificado Let's Encrypt válido
- [ ] Renovación automática configurada
- [ ] Security headers presentes
- [ ] HTTPS redirige correctamente

### Monitoring

- [ ] Prometheus targets UP
- [ ] Grafana dashboards cargan
- [ ] Uptime Kuma monitores OK
- [ ] Alertas de disco/CPU/RAM

---

## 5. Checklist de validación

### Web

- [ ] `curl -I https://monteastur.com` → HTTP/2 200
- [ ] Home page carga sin errores
- [ ] `/seguimiento` funciona
- [ ] `/login` funciona
- [ ] `/react-dashboard` carga sin errores en consola
- [ ] `/admin/dashboard` (Thymeleaf) funciona
- [ ] Login admin correcto (Thymeleaf + SPA)
- [ ] Login cliente correcto

### API

- [ ] `curl -f /actuator/health` → `{"status":"UP"}`
- [ ] `/actuator/info` → JSON
- [ ] Tracking público: `curl /api/v1/tracking/MT-2026-0001`
- [ ] Upload/subida funciona

### Docker

- [ ] `docker ps` → 6/6 containers UP
- [ ] `docker logs monteastur-app --tail 20` sin errores
- [ ] `docker system df` sin uso excesivo

### Monitoring

- [ ] Prometheus: `curl http://localhost:9090/targets` → app UP
- [ ] Grafana: dashboards cargan
- [ ] Uptime Kuma: monitor configurado
- [ ] `./scripts/server-healthcheck.sh` → All checks passed

### Backups

- [ ] `./scripts/backup-db.sh` funciona
- [ ] `./scripts/backup-uploads.sh` funciona
- [ ] Crontab activo: `crontab -l`

---

## 6. Plan de rollback

### Cuándo hacer rollback

Si después del deploy ocurre **cualquiera** de estos síntomas:

| Síntoma | Severidad |
|---------|-----------|
| Healthcheck no responde tras 3 intentos | 🔴 Crítico |
| Login admin no funciona | 🔴 Crítico |
| Login cliente no funciona | 🔴 Crítico |
| Tracking público no funciona | 🔴 Crítico |
| Upload/subida no funciona | 🟡 Alto |
| Home page con errores visibles | 🟡 Alto |
| Logs con errores críticos continuos | 🟡 Alto |

### Rollback de aplicación

```bash
# Opción 1: Script de rollback (recomendado)
cd /opt/monteastur
./scripts/rollback-prod.sh v14.0-e2e-ready

# Opción 2: Rollback manual
git log --oneline -5
git checkout <commit_hash_anterior>
docker compose up -d --build

# Verificar recuperación
./scripts/server-healthcheck.sh
```

### Rollback de base de datos

```bash
# Restaurar backup anterior
./scripts/restore-db.sh backup/db/<backup_anterior>.sql.gz
./scripts/restore-uploads.sh backup/uploads/<backup_anterior>.tar.gz
docker compose up -d --build
```

### Post-rollback

1. Verificar healthcheck: `curl -f http://localhost/actuator/health`
2. Verificar login admin
3. Verificar login cliente
4. Verificar tracking público
5. Notificar al equipo

---

## 7. Plan de costes

### Coste mensual

| Concepto | Proveedor | Coste | Tipo |
|----------|-----------|-------|------|
| VPS CX22 | Hetzner | €4.50/mes | Fijo |
| Dominio .com | Namecheap/Cloudflare | ~€0.83/mes (~€10/año) | Anual |
| SSL | Let's Encrypt | €0 | Gratuito |
| Monitoring | Prometheus + Grafana | €0 | Auto-hospedado |
| Uptime | Uptime Kuma | €0 | Auto-hospedado |
| CI/CD | GitHub Actions | €0 | Gratuito (2000 min/mes) |
| **Total mensual** | | **~€5.33** | |
| **Total anual** | | **~€65** | |

### Costes opcionales

| Concepto | Proveedor | Coste | Recomendado |
|----------|-----------|-------|-------------|
| Backup externo | S3 Glacier / Backblaze B2 | ~€1-3/mes | Sí, tras estabilizar |
| CDN | Cloudflare (Free) | €0 | Sí, tras estabilizar |
| Email SMTP | SendGrid / Mailgun | €0-15/mes | Depende de volúmenes |
| DNS avanzado | Cloudflare | €0 (Free tier) | Sí |
| Monitoring externo | Better Uptime / Checkly | €0-30/mes | Opcional |

### Desglose primer año

| Mes | Coste | Acumulado |
|-----|-------|-----------|
| Mes 1 (setup) | €5.33 | €5.33 |
| Mes 2-12 | €4.50/mes | €54.00 |
| Dominio (año 1) | €10.00 | €10.00 |
| **Total año 1** | | **~€69.33** |
| **Años siguientes** | | **~€54/año** |

---

> **Documentos relacionados:**
> - [`PRODUCTION_VPS_RUNBOOK.md`](PRODUCTION_VPS_RUNBOOK.md) — Runbook de operaciones
> - [`FIRST_VPS_DEPLOY_CHECKLIST.md`](FIRST_VPS_DEPLOY_CHECKLIST.md) — Checklist detallado primer deploy
> - [`VPS_HARDENING_CHECKLIST.md`](VPS_HARDENING_CHECKLIST.md) — Checklist de hardening
>
> **Mantenido por:** Equipo Monteastur Envios
> **Próxima revisión:** 2026-06-24
