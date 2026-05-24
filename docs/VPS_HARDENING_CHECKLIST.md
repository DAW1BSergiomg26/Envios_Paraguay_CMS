# Monteastur Envios — VPS Hardening Checklist

> **Versión:** 1.0 | **Última actualización:** 2026-05-24
> **Objetivo:** Endurecer VPS Ubuntu antes de producción real.
> **Aplica a:** Ubuntu 22.04 / 24.04 LTS

---

## Índice

1. [Seguridad SSH](#a-seguridad-ssh)
2. [Firewall UFW](#b-firewall-ufw)
3. [fail2ban](#c-fail2ban)
4. [Actualizaciones automáticas](#d-actualizaciones-automáticas)
5. [Docker seguridad](#e-docker-seguridad)
6. [Backups](#f-backups)
7. [Monitoring](#g-monitoring)
8. [SSL / HTTPS](#h-ssl--https)
9. [Checklist final antes de producción](#i-checklist-final-antes-de-producción)

---

## A) Seguridad SSH

### Configurar sshd_config

```bash
sudo nano /etc/ssh/sshd_config
```

Líneas a asegurar:

```
Port 22
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
AllowUsers deploy
ClientAliveInterval 60
ClientAliveCountMax 3
MaxAuthTries 3
MaxSessions 3
```

Aplicar cambios:

```bash
sudo systemctl restart sshd
```

### Verificar configuración

```bash
# Confirmar que root login está deshabilitado
sudo sshd -T | grep permitrootlogin
# → permitrootlogin no

# Confirmar password auth deshabilitado
sudo sshd -T | grep passwordauthentication
# → passwordauthentication no
```

### Checklist SSH

- [ ] `PermitRootLogin no`
- [ ] `PasswordAuthentication no`
- [ ] Claves SSH configuradas para `deploy`
- [ ] `AllowUsers deploy` activo
- [ ] SSH config test pasado: `sudo sshd -t`

---

## B) Firewall UFW

### Reglas base

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp comment 'SSH'
sudo ufw allow 80/tcp comment 'HTTP'
sudo ufw allow 443/tcp comment 'HTTPS'
sudo ufw --force enable
```

### Opcional: restringir monitoring

```bash
# Solo IP de oficina/vpn
sudo ufw allow from <IP_OFICINA> to any port 9090 proto tcp comment 'Prometheus'
sudo ufw allow from <IP_OFICINA> to any port 3000 proto tcp comment 'Grafana'
sudo ufw allow from <IP_OFICINA> to any port 3001 proto tcp comment 'Uptime Kuma'
```

### Verificar

```bash
sudo ufw status verbose
sudo ufw show added
```

### Checklist UFW

- [ ] `default deny incoming`
- [ ] `default allow outgoing`
- [ ] Puerto 22/tcp abierto (SSH)
- [ ] Puerto 80/tcp abierto (HTTP)
- [ ] Puerto 443/tcp abierto (HTTPS)
- [ ] Puertos de monitoring restringidos (opcional)
- [ ] UFW habilitado y activo en cada reboot

---

## C) fail2ban

### Instalación

```bash
sudo apt install -y fail2ban
```

### Configuración básica SSH

```bash
sudo cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local
sudo nano /etc/fail2ban/jail.local
```

Asegurar sección `[sshd]`:

```ini
[sshd]
enabled = true
port = 22
filter = sshd
logpath = /var/log/auth.log
maxretry = 5
bantime = 600
findtime = 600
```

### Activar

```bash
sudo systemctl enable --now fail2ban
sudo fail2ban-client status sshd
```

### Verificar

```bash
# Estado general
sudo fail2ban-client status

# IPs baneadas
sudo fail2ban-client status sshd
```

### Checklist fail2ban

- [ ] fail2ban instalado
- [ ] Jail SSH habilitado
- [ ] `maxretry` ≤ 5
- [ ] `bantime` ≥ 600
- [ ] Service activo: `systemctl status fail2ban`

---

## D) Actualizaciones automáticas

### Instalar unattended-upgrades

```bash
sudo apt install -y unattended-upgrades
sudo dpkg-reconfigure --priority=low unattended-upgrades
```

### Configurar

```bash
sudo nano /etc/apt/apt.conf.d/50unattended-upgrades
```

Asegurar:

```ini
Unattended-Upgrade::Allowed-Origins {
    "${distro_id}:${distro_codename}-security";
    "${distro_id}ESMApps:${distro_codename}-apps-security";
    "${distro_id}ESM:${distro_codename}-infra-security";
};
Unattended-Upgrade::AutoFixInterruptedDpkg "true";
Unattended-Upgrade::AutomaticReboot "false";
Unattended-Upgrade::Remove-Unused-Kernel-Packages "true";
Unattended-Upgrade::Remove-New-Unused-Dependencies "true";
Unattended-Upgrade::Remove-Unused-Dependencies "true";
```

### Verificar

```bash
sudo unattended-upgrades --dry-run --debug
```

### Actualización manual periódica

```bash
# Recomendado ejecutar cada mes
sudo apt update && sudo apt upgrade -y
sudo apt autoremove -y
sudo apt autoclean
```

### Checklist actualizaciones

- [ ] unattended-upgrades instalado
- [ ] Solo updates de seguridad
- [ ] No reboot automático
- [ ] Kernel packages limpios automáticamente

---

## E) Docker seguridad

### Prácticas aplicadas en docker-compose.yml

| Práctica | Estado |
|----------|--------|
| `restart: unless-stopped` en todos los servicios | ✅ |
| Límites de memoria (`mem_limit`) | ✅ |
| Healthchecks en servicios críticos | ✅ (app, db) |
| Redes separadas (backend bridge) | ✅ |
| Puertos mínimos expuestos | ✅ |
| Volúmenes para datos persistentes | ✅ |
| `.env` para secrets | ✅ |
| Imágenes oficiales (mysql, nginx, etc.) | ✅ |

### Mantenimiento periódico

```bash
# Limpiar imágenes, contenedores y volúmenes no usados
docker system prune -af

# Limpiar solo imágenes no usadas (menos agresivo)
docker image prune -af

# Ver consumo de disco por Docker
docker system df

# Ver logs de un contenedor con límite
docker logs --tail 100 monteastur-app

# Rotar logs de Docker (opcional)
# sudo nano /etc/docker/daemon.json
# {
#   "log-driver": "json-file",
#   "log-opts": {
#     "max-size": "10m",
#     "max-file": "3"
#   }
# }
```

### Recomendación: prune automático (cron)

```bash
# Ejecutar cada domingo a las 6 AM
0 6 * * 0 docker image prune -af
```

### Checklist Docker

- [ ] `restart: unless-stopped` en todos los servicios
- [ ] Límites de memoria configurados
- [ ] Healthchecks en app y db
- [ ] Puertos mínimos expuestos (no exponer 9090/3000/3001 si no es necesario)
- [ ] Docker system df periódico
- [ ] Prune automático configurado

---

## F) Backups

### Scripts disponibles

| Script | Descripción | Destino |
|--------|-------------|---------|
| `scripts/backup-db.sh` | Backup MySQL vía docker exec + mysqldump + gzip | `backup/db/` |
| `scripts/backup-uploads.sh` | Backup uploads vía docker cp + tar.gz | `backup/uploads/` |
| `scripts/restore-db.sh` | Restore MySQL desde backup | — |
| `scripts/restore-uploads.sh` | Restore uploads desde backup | — |

### Frecuencia recomendada (crontab deploy)

```cron
# Backup BD a las 3:00 AM
0 3 * * * /opt/monteastur/scripts/backup-db.sh

# Backup uploads a las 4:00 AM
0 4 * * * /opt/monteastur/scripts/backup-uploads.sh

# Backup .env (semanal, lunes)
0 5 * * 1 cp /opt/monteastur/.env /opt/monteastur/backups/.env.$(date +\%Y-\%m-\%d)

# Rotación: eliminar backups > 30 días
0 6 * * * find /opt/monteastur/backup -name "*.sql.gz" -mtime +30 -delete
0 6 * * * find /opt/monteastur/backup -name "*.tar.gz" -mtime +30 -delete
```

### Backup externo (recomendado)

```bash
# SCP a servidor externo o NAS
0 7 * * * rsync -avz /opt/monteastur/backup/ user@backup-server:/backups/monteastur/
```

### Prueba de restore

> **Regla de oro:** Un backup que no se ha probado no es un backup.
> Probar restore al menos una vez al mes.

```bash
# Probar restore de BD
./scripts/restore-db.sh backup/db/2026-05-23_14-00.sql.gz

# Probar restore de uploads
./scripts/restore-uploads.sh backup/uploads/2026-05-23_14-00.tar.gz
```

### Checklist Backups

- [ ] Backup MySQL configurado y probado
- [ ] Backup uploads configurado y probado
- [ ] Backup .env configurado
- [ ] Rotación automática (>30 días)
- [ ] Backup externo configurado (opcional)
- [ ] Restore probado al menos una vez

---

## G) Monitoring

### Healthchecks

```bash
# Endpoint de salud de la app
curl -f http://localhost/actuator/health
# → {"status":"UP"}

# Prometheus targets
curl http://localhost:9090/api/v1/targets

# Docker container status
docker ps
```

### Script de healthcheck

Ver `scripts/server-healthcheck.sh` — reporta:

- Uptime del servidor
- Espacio en disco
- Uso de RAM
- Estado de contenedores Docker
- Healthcheck de la aplicación
- Docker disk usage

```bash
# Ejecutar
./scripts/server-healthcheck.sh
```

### Alertas recomendadas en Grafana

| Alerta | Condición | Severidad |
|--------|-----------|-----------|
| App Down | `up == 0` | 🔴 critical |
| High CPU | `cpu > 90%` | 🟡 warning |
| High Heap | `heap > 90%` | 🟡 warning |
| High 5xx Rate | `5xx > 5/min` | 🔴 critical |
| Disk space | `< 20% free` | 🟡 warning |

### Uptime Kuma

Monitores recomendados:

| Monitor | Tipo | URL |
|---------|------|-----|
| Web App | HTTP | `https://monteastur.com` |
| Health Endpoint | HTTP | `https://monteastur.com/actuator/health` |
| SSL Certificate | SSL | `monteastur.com:443` |

### Checklist Monitoring

- [ ] Healthcheck endpoint responde `{"status":"UP"}`
- [ ] Prometheus targets UP
- [ ] Grafana dashboards cargan
- [ ] Uptime Kuma monitores configurados
- [ ] Alertas de disco, CPU, RAM
- [ ] `scripts/server-healthcheck.sh` funciona

---

## H) SSL / HTTPS

### Certificado Let's Encrypt

```bash
# Obtener certificado
docker compose --profile certbot run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d monteastur.com -d www.monteastur.com \
  --email admin@monteastur.com \
  --agree-tos --no-eff-email

# Copiar a nginx/ssl
cp /etc/letsencrypt/live/monteastur.com/fullchain.pem nginx/ssl/
cp /etc/letsencrypt/live/monteastur.com/privkey.pem nginx/ssl/
```

### Renovación automática (cron)

```cron
0 3 * * * docker compose --profile certbot run --rm certbot renew && docker compose restart nginx
```

### HSTS (opcional, irreversible)

Una vez verificado que HTTPS funciona estable:

```
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
```

> ⚠️ **Precaución:** HSTS con `preload` es irreversible. Asegurar que HTTPS funciona 100% antes de activar.

### Security headers (ya configurados en nginx)

| Header | Estado |
|--------|--------|
| `X-Frame-Options: DENY` | ✅ |
| `X-Content-Type-Options: nosniff` | ✅ |
| `Referrer-Policy: strict-origin-when-cross-origin` | ✅ |
| `Permissions-Policy` | ✅ |
| `Content-Security-Policy` | ✅ |
| `Strict-Transport-Security` | ✅ (descomentar en HTTPS) |

### Checklist SSL

- [ ] Certificado Let's Encrypt válido
- [ ] Renovación automática configurada
- [ ] HTTPS redirige correctamente
- [ ] Security headers presentes
- [ ] HSTS configurado (opcional)
- [ ] SSL Labs test: https://www.ssllabs.com/ssltest/

---

## I) Checklist final antes de producción

### Infraestructura

- [ ] VPS con Ubuntu 22.04/24.04
- [ ] Docker Engine + Docker Compose instalados
- [ ] Usuario `deploy` creado y en grupo `docker`
- [ ] SSH configurado con claves, sin contraseñas, sin root
- [ ] UFW activo con puertos mínimos
- [ ] fail2ban instalado y activo
- [ ] unattended-upgrades configurado
- [ ] Zona horaria configurada: `sudo timedatectl set-timezone America/Asuncion`

### Aplicación

- [ ] Repositorio clonado en `/opt/monteastur`
- [ ] `.env` configurado con credenciales seguras
- [ ] `docker compose up -d` → 6/6 containers UP
- [ ] `curl -f http://localhost/actuator/health` → `{"status":"UP"}`
- [ ] Login admin funciona (Thymeleaf + SPA)
- [ ] Login cliente funciona
- [ ] Tracking público funciona
- [ ] Upload/subida de imágenes funciona
- [ ] Logs se escriben sin errores de permisos
- [ ] PWA instalable
- [ ] Offline mode funciona

### Dominio y SSL

- [ ] DNS apunta al VPS
- [ ] Certificado SSL válido
- [ ] HTTPS funciona
- [ ] HTTP → HTTPS redirect
- [ ] Security headers presentes

### CI/CD

- [ ] GitHub Secrets: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY` configurados
- [ ] Conexión SSH desde GitHub Actions probada
- [ ] Workflow `deploy-prod.yml` ejecutable manualmente
- [ ] Rollback probado: `./scripts/rollback-prod.sh <tag>`

### Backups

- [ ] Backup MySQL funciona
- [ ] Backup uploads funciona
- [ ] Backup .env configurado
- [ ] Rotación automática (>30 días)
- [ ] Restore probado

### Monitoring

- [ ] Prometheus targets UP
- [ ] Grafana accesible con dashboards
- [ ] Uptime Kuma monitores configurados
- [ ] `server-healthcheck.sh` ejecutable
- [ ] Alertas de disco, CPU, RAM

### Testing

- [ ] `mvn test` → BUILD SUCCESS
- [ ] `npm test -- --run` → todos pasan
- [ ] `npm run build` → build exitoso
- [ ] `docker compose config` → válido

---

> Mantenido por: Equipo Monteastur Envios
> Próxima revisión: 2026-06-24
