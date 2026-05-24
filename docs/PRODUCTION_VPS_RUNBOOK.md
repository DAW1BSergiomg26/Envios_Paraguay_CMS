# Monteastur Envios — Runbook de Producción VPS

> **Versión:** 1.0 | **Última actualización:** 2026-05-24
> **Repositorio:** `https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS`
> **Rama producción:** `develop`
> **Branch feature:** `feature/fase-15-vps-produccion`

---

## Índice

1. [Requisitos del VPS](#1-requisitos-del-vps)
2. [Instalación inicial](#2-instalación-inicial)
3. [Configurar usuario deploy](#3-configurar-usuario-deploy)
4. [Configurar SSH](#4-configurar-ssh)
5. [Firewall UFW](#5-firewall-ufw)
6. [Clonar repositorio](#6-clonar-repositorio)
7. [Configurar .env](#7-configurar-env)
8. [Levantar stack Docker](#8-levantar-stack-docker)
9. [Verificar healthchecks](#9-verificar-healthchecks)
10. [Configurar dominio DNS](#10-configurar-dominio-dns)
11. [HTTPS / SSL con Let's Encrypt](#11-https--ssl-con-lets-encrypt)
12. [Configurar monitoring](#12-configurar-monitoring)
13. [Configurar backups](#13-configurar-backups)
14. [Configurar GitHub Secrets para CD](#14-configurar-github-secrets-para-cd)
15. [Rollback manual](#15-rollback-manual)
16. [Troubleshooting](#16-troubleshooting)
17. [Checklist de producción](#17-checklist-de-producción)
18. [Primer deploy real](#18-primer-deploy-real)

---

## 1. Requisitos del VPS

### Especificaciones mínimas

| Recurso | Mínimo | Recomendado |
|---------|--------|-------------|
| CPU | 2 vCPU | 4 vCPU |
| RAM | 4 GB | 8 GB |
| Disco | 40 GB SSD | 80 GB SSD |
| SO | Ubuntu 22.04 LTS | Ubuntu 24.04 LTS |
| Conexión | 100 Mbps | 1 Gbps |

### Software preinstalado (todo vía `vps-bootstrap.sh`)

- Docker Engine (última versión estable)
- Docker Compose plugin (v2.x)
- Git
- curl, wget, unzip
- UFW (Uncomplicated Firewall)

### Puertos requeridos

| Puerto | Servicio | Motivo |
|--------|----------|--------|
| 22 | SSH | Acceso remoto |
| 80 | HTTP (Nginx) | Tráfico web + Let's Encrypt |
| 443 | HTTPS (Nginx) | Tráfico web seguro |
| 9090 | Prometheus | Monitoreo interno (opcional: restringir) |
| 3000 | Grafana | Dashboards (opcional: restringir) |
| 3001 | Uptime Kuma | Uptime monitoring (opcional: restringir) |

---

## 2. Instalación inicial

### Opción A: Script automático

```bash
# Como root
sudo ./scripts/vps-bootstrap.sh
```

### Opción B: Manual paso a paso

```bash
# Actualizar sistema
apt update && apt upgrade -y

# Instalar Docker Engine
apt install -y docker.io

# Instalar Docker Compose plugin
apt install -y docker-compose-v2

# Habilitar Docker
systemctl enable --now docker

# Crear directorios
mkdir -p /opt/monteastur /opt/monteastur/backups /opt/monteastur/logs
```

Verificar instalación:

```bash
docker --version
docker compose version
docker info
```

---

## 3. Configurar usuario deploy

```bash
# Crear usuario
adduser deploy

# Añadir a grupos
usermod -aG docker deploy
usermod -aG sudo deploy

# Verificar
id deploy
```

---

## 4. Configurar SSH

```bash
# Editar configuración SSH
nano /etc/ssh/sshd_config
```

Asegurar estas líneas:

```
Port 22
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
ClientAliveInterval 60
ClientAliveCountMax 3
```

Reiniciar SSH:

```bash
systemctl restart sshd
```

Añadir clave pública del desarrollador:

```bash
su - deploy
mkdir -p ~/.ssh
chmod 700 ~/.ssh
nano ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

---

## 5. Firewall UFW

```bash
# Configurar reglas
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment 'SSH'
ufw allow 80/tcp comment 'HTTP'
ufw allow 443/tcp comment 'HTTPS'

# Opcional: restringir monitoring a IP específica
# ufw allow from <TU_IP> to any port 9090 proto tcp comment 'Prometheus'
# ufw allow from <TU_IP> to any port 3000 proto tcp comment 'Grafana'
# ufw allow from <TU_IP> to any port 3001 proto tcp comment 'Uptime Kuma'

# Habilitar
ufw --force enable
ufw status verbose
```

---

## 6. Clonar repositorio

```bash
su - deploy
cd /opt
git clone https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS.git monteastur
cd monteastur

# (Opcional) Cambiar a rama específica
# git checkout develop
```

### Estructura final

```
/opt/monteastur/
├── docker-compose.yml
├── Dockerfile
├── .env                  # Credenciales (NO subir a Git)
├── nginx/
│   ├── nginx.conf
│   └── conf.d/
│       └── monteastur.conf
├── scripts/
│   ├── vps-bootstrap.sh
│   ├── deploy-prod.sh
│   ├── rollback-prod.sh
│   ├── backup-db.sh
│   ├── backup-uploads.sh
│   ├── restore-db.sh
│   └── restore-uploads.sh
├── monitoring/
│   ├── prometheus/
│   └── grafana/
├── docs/
├── backup/
│   ├── db/
│   └── uploads/
├── logs/
├── frontend-react/
├── src/                  # Código Spring Boot
├── pom.xml
└── README.md
```

---

## 7. Configurar .env

```bash
cp .env.example .env
nano .env
```

**Variables obligatorias a cambiar:**

```bash
MYSQL_ROOT_PASSWORD=<generar_contraseña_segura>
MYSQL_PASSWORD=<generar_contraseña_segura>
DB_PASSWORD=<generar_contraseña_segura>
ADMIN_PASSWORD=<generar_contraseña_segura>
GRAFANA_ADMIN_PASSWORD=<generar_contraseña_segura>
SPRING_PROFILES_ACTIVE=prod
```

**Variables recomendadas:**

```bash
TZ=America/Asuncion
SSL_EMAIL=admin@monteastur.com    # Email para Let's Encrypt
BACKUP_PATH=./backup
```

No cambiar salvo necesidad:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/casarural?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=convertToNull
PORT=8080
NGINX_PORT=80
UPLOAD_DIR=/app/uploads
```

> **IMPORTANTE:** `DB_DDL_AUTO=validate` — la BD debe existir con el schema correcto. Para primera instalación, usar `update` temporalmente y luego revertir.

---

## 8. Levantar stack Docker

### Primera vez

```bash
cd /opt/monteastur
docker compose build
docker compose up -d
```

### Despliegues posteriores

Usar el script:

```bash
./scripts/deploy-prod.sh
```

O manualmente:

```bash
cd /opt/monteastur
git pull
docker compose build
docker compose up -d
docker image prune -f
```

### Verificar contenedores

```bash
docker ps
```

Esperado: 6 contenedores UP (app, nginx, db, prometheus, grafana, uptime-kuma).

---

## 9. Verificar healthchecks

```bash
# Healthcheck de la aplicación
curl -f http://localhost/actuator/health
# → {"status":"UP"}

# Ver nginx responde
curl -f http://localhost/
# → HTML de la página principal

# Info de la app
curl http://localhost/actuator/info

# Verificar logs
docker logs monteastur-app --tail 20

# Verificar base de datos
docker exec monteastur-mysql mysqladmin ping -h localhost
# → mysqld is alive
```

---

## 10. Configurar dominio DNS

### Registrar registros DNS

| Tipo | Nombre | Valor |
|------|--------|-------|
| A | `@` | `<IP_DEL_VPS>` |
| A | `www` | `<IP_DEL_VPS>` |
| AAAA | `@` | `<IPv6_DEL_VPS>` (opcional) |

### Verificar propagación

```bash
dig +short monteastur.com
dig +short www.monteastur.com
nslookup monteastur.com
```

Esperar propagación (5 min a 48h).

---

## 11. HTTPS / SSL con Let's Encrypt

### Actualizar nginx.conf para dominio

Editar `nginx/conf.d/monteastur.conf`:

```nginx
server_name monteastur.com www.monteastur.com;
```

### Obtener certificado

```bash
docker compose --profile certbot run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d monteastur.com -d www.monteastur.com \
  --email admin@monteastur.com \
  --agree-tos --no-eff-email
```

### Copiar certificados

```bash
cp /etc/letsencrypt/live/monteastur.com/fullchain.pem nginx/ssl/
cp /etc/letsencrypt/live/monteastur.com/privkey.pem nginx/ssl/
```

### Descomentar bloque HTTPS

Editar `nginx/conf.d/monteastur.conf` y descomentar el bloque `server` de HTTPS (líneas 39-73).

```bash
docker compose restart nginx
```

### Verificar HTTPS

```bash
curl -I https://monteastur.com
# → 200 OK + security headers
```

### Renovación automática (cron)

```bash
crontab -e
```

Añadir:

```cron
0 3 * * * docker compose --profile certbot run --rm certbot renew && docker compose restart nginx
```

---

## 12. Configurar monitoring

### Prometheus

Acceso: `http://<VPS_IP>:9090`

Verificar targets:
```bash
curl http://localhost:9090/api/v1/targets | jq .
```

### Grafana

Acceso: `http://<VPS_IP>:3000`

Login: `admin` / `<GRAFANA_ADMIN_PASSWORD>` (configurado en `.env`)

Dashboards preconfigurados:
- Monteastur Envios (auto-provisionado)
- Métricas JVM, HTTP, sistema

### Uptime Kuma

Acceso: `http://<VPS_IP>:3001`

Configurar monitores:
1. HTTP Monitor → `https://monteastur.com`
2. HTTP Monitor → `https://monteastur.com/actuator/health`
3. SSL Certificate → `monteastur.com:443`

### Restringir acceso (opcional)

```bash
# Permitir solo IP de oficina
ufw allow from <IP_OFICINA> to any port 9090 proto tcp
ufw allow from <IP_OFICINA> to any port 3000 proto tcp
ufw allow from <IP_OFICINA> to any port 3001 proto tcp

# O por VPN
ufw allow from 10.8.0.0/24 to any port 9090 proto tcp
```

---

## 13. Configurar backups

### Backup diario automático (crontab usuario deploy)

```bash
crontab -e
```

```cron
# Backup BD a las 3:00 AM
0 3 * * * /opt/monteastur/scripts/backup-db.sh

# Backup uploads a las 4:00 AM
0 4 * * * /opt/monteastur/scripts/backup-uploads.sh

# Rotación: eliminar backups > 30 días
0 5 * * * find /opt/monteastur/backup -name "*.sql.gz" -mtime +30 -delete
0 5 * * * find /opt/monteastur/backup -name "*.tar.gz" -mtime +30 -delete
```

### Backup externo (recomendado)

```bash
# SCP a servidor externo
0 6 * * * scp /opt/monteastur/backup/db/$(date +\%Y-\%m-\%d)*.sql.gz backup@remoto:/backups/monteastur/

# O AWS S3
# 0 6 * * * aws s3 sync /opt/monteastur/backup/ s3://monteastur-backups/ --delete
```

### Restore

```bash
# Base de datos
./scripts/restore-db.sh backup/db/2026-05-23_14-00.sql.gz

# Uploads (crea backup previo automático)
./scripts/restore-uploads.sh backup/uploads/2026-05-23_14-00.tar.gz
```

---

## 14. Configurar GitHub Secrets para CD

### Workflow disponible

El proyecto incluye el workflow [`deploy-prod.yml`](../.github/workflows/deploy-prod.yml) para desplegar a producción manualmente desde GitHub Actions.

**Características:**
- Solo ejecutable desde branch `develop`
- Requiere confirmación explícita escribiendo "deploy"
- Pre-validación: `docker compose config` + `mvn test` + `npm test` + `npm run build`
- Deploy vía SSH usando `appleboy/ssh-action`
- Timeout de 15min (validación) + 20min (deploy)
- Notificación de fallo con instrucciones de rollback

### Secrets requeridos

Ir a: **GitHub → Settings → Secrets and variables → Actions**

| Secret | Valor | Descripción |
|--------|-------|-------------|
| `VPS_HOST` | `monteastur.com` o IP | IP/dominio del VPS |
| `VPS_USER` | `deploy` | Usuario SSH en el VPS |
| `VPS_SSH_KEY` | `-----BEGIN OPENSSH PRIVATE KEY-----\n...` | Clave privada SSH (formato PEM/OpenSSH) |
| `VPS_PORT` | `22` | Puerto SSH (opcional, default 22) |

### Generar clave SSH para GitHub Actions

```bash
# 1. Conectar al VPS como usuario deploy
ssh deploy@<VPS_IP>

# 2. Generar clave SSH dedicada para GitHub Actions
ssh-keygen -t ed25519 -f ~/.ssh/github-actions -N ""

# 3. Autorizar la clave pública
cat ~/.ssh/github-actions.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# 4. Mostrar la clave privada (copiar al portapapeles)
cat ~/.ssh/github-actions
```

> La salida del paso 4 es el valor para `VPS_SSH_KEY`. Incluye las líneas `-----BEGIN OPENSSH PRIVATE KEY-----` y `-----END OPENSSH PRIVATE KEY-----`.

### Añadir secrets a GitHub

1. Ir a **GitHub → Repositorio → Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Añadir cada secret:

| Secret | Valor |
|--------|-------|
| `VPS_HOST` | IP pública del VPS (ej: `203.0.113.10`) |
| `VPS_USER` | `deploy` |
| `VPS_SSH_KEY` | Contenido completo de `~/.ssh/github-actions` (desde `-----BEGIN` hasta `-----END`) |
| `VPS_PORT` | `22` (dejar por defecto) |

### Probar conexión SSH localmente (antes del deploy)

```bash
# Verificar que la clave funciona
ssh -i ~/.ssh/github-actions deploy@<VPS_IP>

# Verificar permisos del directorio
ls -la /opt/monteastur

# Verificar que el script deploy-prod.sh existe
ls -la /opt/monteastur/scripts/deploy-prod.sh

# Verificar .env existe
test -f /opt/monteastur/.env && echo "OK" || echo "MISSING"
```

### Ejecutar workflow manualmente

1. Ir a **GitHub → Actions → Deploy Production**
2. Click **Run workflow**
3. Seleccionar branch: `develop`
4. Confirmar escribiendo `deploy` en el campo
5. Click **Run workflow**

El workflow ejecutará:
1. **pre-deploy-check**: valida Docker Compose, corre tests backend y frontend, build frontend
2. **deploy-production** (si validación pasó): SSH al VPS, git pull, `./scripts/deploy-prod.sh`
3. **notify-failure** (si falló): muestra en logs las instrucciones de rollback

---

## 15. Rollback manual

### Usando script

```bash
./scripts/rollback-prod.sh v14.0-e2e-ready
```

### Manual

```bash
cd /opt/monteastur
git log --oneline -10
git checkout <commit_hash>
docker compose up -d --build
```

### Rollback de base de datos

```bash
# 1. Restaurar BD desde backup
./scripts/restore-db.sh backup/db/2026-05-23_14-00.sql.gz

# 2. Restaurar uploads
./scripts/restore-uploads.sh backup/uploads/2026-05-23_14-00.tar.gz

# 3. Reconstruir y reiniciar
docker compose up -d --build
```

---

## 16. Troubleshooting

| Problema | Causa | Solución |
|----------|-------|----------|
| App no arranca | Schema BD no existe | `DB_DDL_AUTO=update` temporal, luego revertir a `validate` |
| 502 Bad Gateway | Nginx no llega a app | `docker ps` → app debe estar `Up`; `docker logs app` |
| 403 API | Sesión no válida o CSRF | Login vía SPA; CSRF deshabilitado para `/api/**` |
| Uploads no visibles | Ruta/permisos incorrectos | `docker exec monteastur-app ls -la /app/uploads` |
| Puerto 80 ocupado | Apache/nginx del sistema | `lsof -i :80`; cambiar `NGINX_PORT` o detener servicio |
| Certificado SSL caducado | No se renovó automáticamente | `docker compose --profile certbot run --rm certbot renew` |
| Disco lleno | Logs/imágenes/basura Docker | `docker system prune -af`; revisar `logs/`; `du -sh /var/lib/docker` |
| Container restart loop | Error en Spring Boot | `docker logs monteastur-app --tail 50` para diagnosticar |
| MySQL no arranca | Permisos/volumen corrupto | `docker logs monteastur-mysql`; `docker compose down && docker compose up -d` |
| Prometheus no recibe datos | Target caído | `curl http://localhost:9090/targets`; verificar `app:8080` UP |
| Grafana no carga dashboards | Volumen con datos previos | `docker compose down -v grafana && docker compose up -d` (borra BD interna) |
| SSH: Permission denied | Clave pública no autorizada | Verificar `cat ~/.ssh/id_ed25519.pub >> ~/.ssh/authorized_keys` y `chmod 600 ~/.ssh/authorized_keys` |
| SSH: Connection refused | Puerto 22 bloqueado | `ufw status` verificar regla; `systemctl status sshd` |
| SSH: Host key changed | VPS reinstalado | `ssh-keygen -R <VPS_IP>` para limpiar clave anterior |
| Deploy: Script not found | Repositorio no clonado | `ls /opt/monteastur/scripts/` debe mostrar `deploy-prod.sh`; si no, clonar repo |
| Deploy: .env missing | .env no creado | `cp .env.example .env && nano .env` con credenciales de producción |
| Deploy: git pull conflict | Cambios locales sin commit | `git stash` o `git reset --hard origin/develop` (cuidado: pierde cambios locales) |

---

## 17. Checklist de producción

### Pre-despliegue

- [ ] `mvn clean package -DskipTests` → BUILD SUCCESS
- [ ] `npm test -- --run` → todos pasan
- [ ] `npm run build` → build exitoso
- [ ] `docker compose config` → válido
- [ ] `docker compose build` → sin errores
- [ ] `.env.example` actualizado con todas las variables
- [ ] Scripts con `chmod +x`
- [ ] `.github/workflows/deploy-prod.yml` sintaxis YAML válida
- [ ] GitHub Secrets configurados: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`
- [ ] Conexión SSH manual probada: `ssh deploy@<VPS_IP>`

### Post-despliegue

- [ ] `docker ps` → 6/6 containers UP
- [ ] `curl -f http://localhost/actuator/health` → `{"status":"UP"}`
- [ ] Login admin funciona (Thymeleaf y SPA)
- [ ] Login cliente funciona
- [ ] Tracking público funciona
- [ ] Upload/subida de imágenes funciona
- [ ] Security headers presentes (`curl -I`)
- [ ] HTTPS funciona (certificado válido)
- [ ] HTTP → HTTPS redirect
- [ ] Prometheus targets UP
- [ ] Grafana accesible, dashboards cargan
- [ ] Uptime Kuma monitores configurados
- [ ] Backups programados (crontab)
- [ ] Logs se escriben sin errores de permisos
- [ ] PWA instalable
- [ ] Notificaciones push funcionan
- [ ] Offline mode funciona

### Seguridad

- [ ] `PermitRootLogin no`
- [ ] `PasswordAuthentication no`
- [ ] UFW activo con reglas mínimas
- [ ] fail2ban instalado y activo
- [ ] unattended-upgrades configurado
- [ ] Clave SSH para CD configurada
- [ ] `.env` no está en Git (`.gitignore`)
- [ ] Contraseñas producidas con generador seguro
- [ ] CSRF habilitado para formularios, deshabilitado para `/api/**`
- [ ] Cookie de sesión HttpOnly + Secure
- [ ] HSTS habilitado (HTTPS)
- [ ] CSP configurada
- [ ] `server-healthcheck.sh` ejecutable y funcional

> Para hardening detallado ver [`VPS_HARDENING_CHECKLIST.md`](VPS_HARDENING_CHECKLIST.md).

---

## 18. Primer deploy real

### Tiempos estimados

| Paso | Duración |
|------|----------|
| Contratar VPS (Hetzner CX22) | ~10 min |
| Bootstrap + clonar repo | ~5 min |
| Crear usuario deploy + SSH | ~5 min |
| Configurar .env | ~5 min |
| Generar SSH key GitHub Actions | ~2 min |
| Primer docker compose up | ~10 min |
| DNS + HTTPS | ~10 min + propagación |
| GitHub Secrets | ~5 min |
| Workflow manual | ~5 min |
| Validaciones finales | ~5 min |
| **Total** | **~45 min efectivos** |

### Riesgos comunes del primer deploy

| Riesgo | Probabilidad | Mitigación |
|--------|-------------|------------|
| DNS no propagado | Alta (esperar 5-15 min) | Usar IP directamente para healthchecks |
| Certificado SSL falla por DNS | Alta | Verificar `dig` antes de certbot |
| Puerto 80 ocupado | Media | `lsof -i :80`; detener Apache si existe |
| `.env` con credenciales débiles | Media | Usar generador: `openssl rand -base64 32` |
| Docker build lento (1ª vez) | Alta | Build previo local; usar caché |
| SSH key formato incorrecto | Media | Asegurar incluir `-----BEGIN` y `-----END` |
| Contenedor mysql no healthy | Media | Esperar 30s; verificar `docker logs` |
| Grafana login falla | Baja | Default `admin` / `admin123` (cambiar en .env) |

### Errores típicos DNS/SSL

```bash
# Error: Certbot "DNS problem: NXDOMAIN"
# → Los registros DNS no están configurados o no se propagaron
dig monteastur.com  # Debe devolver IP del VPS

# Error: Certbot "Connection refused" en puerto 80
# → Nginx no está escuchando en 80 o UFW lo bloquea
curl -I http://monteastur.com  # Debe responder
sudo ufw status                # 80/tcp debe estar ALLOW

# Error: "SSL: CERTIFICATE_VERIFY_FAILED"
# → Certificado no válido o caducado
echo | openssl s_client -connect monteastur.com:443

# Error: "502 Bad Gateway" después de SSL
# → Nginx configurado con HTTPS pero proxy_pass no funciona
docker ps  # monteastur-app debe estar UP
```

### Coste estimado del primer mes

| Concepto | Coste |
|----------|-------|
| VPS Hetzner CX22 | €4.50 |
| Dominio (prorrateado ~€10/año) | €0.83 |
| SSL / Monitoring / Uptime | €0 |
| **Total** | **~€5.33** |

Ver [`docs/FIRST_VPS_DEPLOY_CHECKLIST.md`](FIRST_VPS_DEPLOY_CHECKLIST.md) para guía paso a paso completa.

---

> **Documentos relacionados:**
> - [`VPS_HARDENING_CHECKLIST.md`](VPS_HARDENING_CHECKLIST.md) — Checklist completo de hardening (SSH, UFW, fail2ban, updates, Docker, backups, monitoring, SSL)
> - [`FIRST_VPS_DEPLOY_CHECKLIST.md`](FIRST_VPS_DEPLOY_CHECKLIST.md) — Checklist paso a paso para el primer despliegue real
> - [`scripts/server-healthcheck.sh`](../scripts/server-healthcheck.sh) — Script de healthcheck rápido
>
> **Mantenido por:** Equipo Monteastur Envios
> **Próxima revisión:** 2026-06-24
