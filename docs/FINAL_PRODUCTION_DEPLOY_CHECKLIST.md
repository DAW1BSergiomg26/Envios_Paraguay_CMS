# Monteastur Envios — Checklist Final Despliegue Producción

> **Versión:** 1.0 | **Fecha:** 2026-05-24
> **Estado:** 📋 LISTA PARA EJECUTAR
> **Duración estimada:** ~2h (sin incluir verificación Hetzner ni propagación DNS)

---

## Cómo usar esta checklist

1. Imprimir o abrir en dispositivo aparte
2. Marcar cada item al completarlo
3. No saltar pasos
4. Si algo falla, ejecutar plan de contingencia (sección F)
5. Tiempo estimado por fase indicado en cada sección

---

## A) PRE-DEPLOY (~30 min)

### A.1 VPS

- [ ] **VPS contratado** (Hetzner CX22 recomendado)
- [ ] **Ubuntu 22.04 LTS** instalado
- [ ] **IP del VPS anotada**: `___________`
- [ ] **Root SSH key** configurada durante creación
- [ ] **Conexión root verificada**: `ssh root@<IP>`
- [ ] **apt update/upgrade** ejecutado

### A.2 Usuario deploy

- [ ] Usuario creado: `adduser deploy`
- [ ] Grupos asignados: `usermod -aG docker,sudo deploy`
- [ ] SSH key de root copiada a deploy
- [ ] Conexión como deploy verificada: `ssh deploy@<IP>`

### A.3 Bootstrap

- [ ] `sudo ./scripts/vps-bootstrap.sh` ejecutado sin errores
- [ ] Docker Engine instalado: `docker --version`
- [ ] Docker Compose instalado: `docker compose version`
- [ ] UFW activo: `sudo ufw status`
- [ ] Puertos 22, 80, 443 abiertos

### A.4 Seguridad básica

- [ ] SSH: `PermitRootLogin no` configurado
- [ ] SSH: `PasswordAuthentication no` configurado
- [ ] SSH: `AllowUsers deploy` configurado
- [ ] SSH: `systemctl restart sshd` ejecutado
- [ ] fail2ban instalado y activo
- [ ] unattended-upgrades configurado

### A.5 Dominio

- [ ] Dominio comprado (ej: monteastur.com)
- [ ] Registro A creado: `@ → <IP_VPS>` (TTL 300)
- [ ] Registro A creado: `www → <IP_VPS>` (TTL 300)
- [ ] DNS propagación verificada: `dig +short monteastur.com`

### A.6 GitHub Secrets

- [ ] `VPS_HOST` configurado con IP del VPS
- [ ] `VPS_USER` configurado: `deploy`
- [ ] `VPS_SSH_KEY` configurado con clave privada
- [ ] `VPS_PORT` configurado: `22`
- [ ] Conexión SSH verificada: `VPS_HOST=<IP> ./scripts/check-ssh-connection.sh`

### A.7 Repositorio

- [ ] Repo clonado: `git clone <repo> /opt/monteastur`
- [ ] `.env.example` copiado a `.env`
- [ ] `.env` configurado con credenciales seguras:

| Variable | Generado | Verificado |
|----------|----------|------------|
| `MYSQL_ROOT_PASSWORD` | `openssl rand -base64 32` | ☐ |
| `MYSQL_PASSWORD` | `openssl rand -base64 32` | ☐ |
| `DB_PASSWORD` | `openssl rand -base64 32` | ☐ |
| `ADMIN_PASSWORD` | `openssl rand -base64 32` | ☐ |
| `GRAFANA_ADMIN_PASSWORD` | `openssl rand -base64 16` | ☐ |
| `SPRING_PROFILES_ACTIVE=prod` | Configurado | ☐ |
| `TZ=America/Asuncion` | Configurado | ☐ |

---

## B) PRE-SSL (~15 min)

- [ ] Puerto 80 abierto en UFW: `sudo ufw status | grep 80`
- [ ] Nginx funcionando: `curl -I http://localhost:80`
- [ ] Dominio responde HTTP: `curl -I http://monteastur.com`
- [ ] DNS propagado: `dig +short monteastur.com` → IP correcta
- [ ] Certbot perfil activado en docker-compose
- [ ] Location `.well-known/acme-challenge/` en nginx configurado

---

## C) DEPLOY (~45 min)

### Paso 1: Bootstrap final

- [ ] Scripts ejecutables: `chmod +x /opt/monteastur/scripts/*.sh`
- [ ] `.env` revisado y completo

### Paso 2: Docker compose build

- [ ] `docker compose build --no-cache` (primera vez)
- [ ] Sin errores de build
- [ ] Frontend build OK dentro del Dockerfile

### Paso 3: Levantar stack

- [ ] `docker compose up -d` ejecutado
- [ ] `docker ps` → 6/6 containers UP:

| Contenedor | Estado |
|------------|--------|
| `monteastur-app` | ☐ UP |
| `monteastur-nginx` | ☐ UP |
| `monteastur-mysql` | ☐ UP (healthy) |
| `monteastur-prometheus` | ☐ UP |
| `monteastur-grafana` | ☐ UP |
| `monteastur-uptime-kuma` | ☐ UP |

### Paso 4: Verificar healthchecks

- [ ] `curl -f http://localhost/actuator/health` → `{"status":"UP"}`
- [ ] `curl -f http://localhost` → HTML
- [ ] `curl -f http://localhost:9090/targets` → Prometheus targets
- [ ] `curl -f http://localhost:3000` → Grafana login
- [ ] `curl -f http://localhost:3001` → Uptime Kuma

### Paso 5: Activar HTTPS

- [ ] Certificado SSL obtenido: `docker compose --profile certbot run --rm certbot certonly ...`
- [ ] Certificados copiados a `nginx/ssl/`
- [ ] Bloque HTTPS descomentado en nginx config
- [ ] `server_name` actualizado en nginx
- [ ] `docker compose restart nginx` ejecutado
- [ ] `curl -I https://monteastur.com` → HTTP/2 200
- [ ] Security headers presentes: `curl -I https://monteastur.com | grep -i "strict-transport-security"`

### Paso 6: Generar SSH key y secrets

- [ ] `ssh-keygen -t ed25519 -f ~/.ssh/github-actions -N ""`
- [ ] Clave pública añadida a `authorized_keys`
- [ ] Clave privada copiada a GitHub Secret `VPS_SSH_KEY`

### Paso 7: Ejecutar workflow

- [ ] Ir a GitHub → Actions → Deploy Production
- [ ] Branch: `develop`
- [ ] Confirm: `deploy`
- [ ] Job `pre-deploy-check` pasa
- [ ] Job `deploy-production` pasa
- [ ] Job `notify-failure` no se ejecuta (no hay fallo)

### Paso 8: Configurar renovaciones

- [ ] Crontab configurado:

```cron
0 3 * * * /opt/monteastur/scripts/backup-db.sh
0 4 * * * /opt/monteastur/scripts/backup-uploads.sh
0 5 * * * find /opt/monteastur/backup -name "*.sql.gz" -mtime +30 -delete
0 5 * * * find /opt/monteastur/backup -name "*.tar.gz" -mtime +30 -delete
0 3 * * * cd /opt/monteastur && docker compose --profile certbot run --rm certbot renew && docker compose restart nginx
0 6 * * 0 docker image prune -af
```

---

## D) POST-DEPLOY (~20 min)

### D.1 Web pública

- [ ] `curl -I https://monteastur.com` → 200 OK
- [ ] Home page carga visualmente (abrir en navegador)
- [ ] `/seguimiento` funciona
- [ ] `/login` funciona
- [ ] `/react-dashboard` carga sin errores en consola
- [ ] `/admin/dashboard` (Thymeleaf) funciona
- [ ] Sin errores 404/500 en navegación básica

### D.2 Autenticación

- [ ] Login admin (Thymeleaf) funciona
- [ ] Login admin (SPA React) funciona
- [ ] Login cliente funciona
- [ ] Dashboard admin carga con datos
- [ ] Panel cliente carga con datos
- [ ] Logout funciona

### D.3 API

- [ ] `curl -f https://monteastur.com/actuator/health` → `{"status":"UP"}`
- [ ] `curl https://monteastur.com/actuator/info` → JSON
- [ ] Tracking público: `curl https://monteastur.com/api/v1/tracking/MT-2026-0001`
- [ ] Upload/subida de imágenes funciona

### D.4 Monitoring

- [ ] Prometheus: `curl http://localhost:9090/targets` → app UP
- [ ] Grafana accesible: `http://<IP>:3000`
- [ ] Grafana dashboards cargan (Monteastur Envios)
- [ ] Uptime Kuma accesible: `http://<IP>:3001`
- [ ] Monitor HTTP configurado en Uptime Kuma
- [ ] Monitor SSL configurado en Uptime Kuma

### D.5 SSL

- [ ] Certificado Let's Encrypt válido
- [ ] HTTPS redirige correctamente (HTTP → HTTPS)
- [ ] `curl -I https://monteastur.com` muestra security headers
- [ ] HSTS header presente (si configurado)
- [ ] SSL Labs test: https://www.ssllabs.com/ssltest/

### D.6 Backups

- [ ] `./scripts/backup-db.sh` funciona
- [ ] `./scripts/backup-uploads.sh` funciona
- [ ] Backup `.env` configurado en crontab
- [ ] Backup creado en `backup/db/` y `backup/uploads/`

### D.7 Logs

- [ ] `docker logs monteastur-app --tail 20` sin errores
- [ ] `docker logs monteastur-nginx --tail 20` sin errores
- [ ] Logs de app se escriben sin errores de permisos

### D.8 Rollback

- [ ] `./scripts/rollback-prod.sh v14.0-e2e-ready` funciona
- [ ] App se recupera después del rollback
- [ ] `git checkout develop && docker compose up -d --build` restaura

---

## E) SMOKE TESTS (~15 min)

Ejecutar según [`docs/SMOKE_TESTS_PRODUCTION.md`](SMOKE_TESTS_PRODUCTION.md).

| Test | Prioridad | Resultado |
|------|-----------|-----------|
| Healthcheck endpoint | 🔴 Alta | ☐ |
| Home page carga | 🔴 Alta | ☐ |
| Login admin funciona | 🔴 Alta | ☐ |
| Login cliente funciona | 🔴 Alta | ☐ |
| Tracking público funciona | 🔴 Alta | ☐ |
| Dashboard admin carga | 🟡 Media | ☐ |
| Dashboard React SPA carga | 🟡 Media | ☐ |
| Upload/subida funciona | 🟡 Media | ☐ |
| PWA instalable | 🟢 Baja | ☐ |
| Offline mode funciona | 🟢 Baja | ☐ |
| SSL Labs grade A+ | 🟢 Baja | ☐ |

---

## F) PLAN DE CONTINGENCIA

### Si el deploy falla

| Síntoma | Acción |
|---------|--------|
| Healthcheck no responde | `docker logs monteastur-app --tail 50` para diagnosticar |
| Nginx 502 Bad Gateway | `docker ps` verificar app UP; `docker logs app` |
| MySQL no conecta | Verificar `docker logs monteastur-mysql` |
| Build falla | Revisar `docker compose build` output; verificar Dockerfile |
| Rollback inmediato: | `./scripts/rollback-prod.sh v14.0-e2e-ready` |

### Si SSL falla

| Síntoma | Acción |
|---------|--------|
| Certbot NXDOMAIN | DNS no propagado → esperar y reintentar |
| Certbot connection refused | Puerto 80 bloqueado → verificar UFW y nginx |
| Certificado no válido | Verificar fechas: `openssl x509 -noout -dates` |
| HTTPS no responde | Verificar bloque SSL descomentado en nginx |
| Fallback: | Usar HTTP mientras se soluciona SSL |

### Si DNS falla

| Síntoma | Acción |
|---------|--------|
| No resuelve | Verificar registro A en panel DNS |
| Resuelve IP antigua | Reducir TTL a 60, esperar propagación |
| Timeout | Verificar dominio no caducado |
| Fallback: | Acceder por IP directamente: `http://<IP>` |
| Trabajar con IP: | Healthchecks, pruebas, monitorización local |

### Si Docker falla

| Síntoma | Acción |
|---------|--------|
| Container no arranca | `docker logs <container>` para diagnóstico |
| Docker daemon caído | `sudo systemctl restart docker` |
| Disco lleno | `docker system prune -af`; `du -sh /var/lib/docker` |
| MySQL corrupto | `docker compose down -v` (pierde datos) y restore desde backup |
| Fallback: | `docker compose restart <service>` |

### Rollback de emergencia

```bash
# 1. Rollback de aplicación
cd /opt/monteastur && ./scripts/rollback-prod.sh v14.0-e2e-ready

# 2. Si no funciona, rollback manual
cd /opt/monteastur
git checkout <commit_hash_estable>
docker compose up -d --build

# 3. Rollback de base de datos (si hay corrupción)
./scripts/restore-db.sh backup/db/<backup_reciente>.sql.gz
docker compose up -d --build

# 4. Verificar recuperación
./scripts/server-healthcheck.sh
```

---

## G) CHECKLIST 24H POST-DEPLOY

Después de 24h del deploy, verificar:

### Sistema

- [ ] CPU normal: `uptime` (load < 2.0)
- [ ] RAM disponible: `free -h` (uso < 80%)
- [ ] Disco: `df -h /` (uso < 80%)
- [ ] Docker: `docker ps` → 6/6 UP
- [ ] Uptime del VPS: `uptime -p`

### Aplicación

- [ ] Healthcheck: `curl -f localhost/actuator/health` → UP
- [ ] Logs sin errores: `docker logs monteastur-app --since 24h | grep -i error | wc -l`
- [ ] Logs nginx sin 5xx: `docker logs monteastur-nginx --since 24h | grep ' 5' | wc -l`

### SSL

- [ ] Certificado válido: `echo | openssl s_client -connect localhost:443 2>/dev/null | openssl x509 -noout -dates`
- [ ] Renovación automática configurada en crontab

### Backups

- [ ] Backup BD ejecutado (archivo en `backup/db/` con fecha de hoy)
- [ ] Backup uploads ejecutado (archivo en `backup/uploads/` con fecha de hoy)

### Monitoring

- [ ] Prometheus targets UP
- [ ] Grafana dashboards con datos
- [ ] Uptime Kuma sin alertas
- [ ] `./scripts/server-healthcheck.sh` → All checks passed

### Usuarios

- [ ] Login admin funciona
- [ ] Login cliente funciona
- [ ] Tracking público funciona
- [ ] Sin reportes de error del equipo

---

> **Documentos relacionados:**
> - [`SMOKE_TESTS_PRODUCTION.md`](SMOKE_TESTS_PRODUCTION.md) — Smoke tests detallados
> - [`LIVE_DEPLOY_PLAN.md`](LIVE_DEPLOY_PLAN.md) — Plan de 15 pasos
> - [`VPS_HARDENING_CHECKLIST.md`](VPS_HARDENING_CHECKLIST.md) — Hardening de seguridad
> - [`PRODUCTION_VPS_RUNBOOK.md`](PRODUCTION_VPS_RUNBOOK.md) — Runbook completo
>
> **Mantenido por:** Equipo Monteastur Envios
> **Próxima revisión:** 2026-06-24
