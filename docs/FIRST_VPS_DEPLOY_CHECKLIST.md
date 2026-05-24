# Monteastur Envios — Checklist Primer Despliegue VPS

> **Versión:** 1.0 | **Última actualización:** 2026-05-24
> **Objetivo:** Checklist completa para el primer despliegue en producción real.
> **Estado:** PREPARADO — Pendiente de ejecución.

---

## Índice

- [A) Selección proveedor VPS](#a-selección-proveedor-vps)
- [B) Requisitos mínimos reales](#b-requisitos-mínimos-reales)
- [C) Dominio](#c-dominio)
- [D) HTTPS / SSL](#d-https--ssl)
- [E) GitHub Secrets checklist](#e-github-secrets-checklist)
- [F) Primer deploy seguro (paso a paso)](#f-primer-deploy-seguro-paso-a-paso)
- [G) Validaciones post-deploy](#g-validaciones-post-deploy)
- [H) Rollback real](#h-rollback-real)
- [I) Coste estimado mensual](#i-coste-estimado-mensual)
- [J) Checklist ejecución](#j-checklist-ejecución)

---

## A) Selección proveedor VPS

### Comparativa básica (junio 2026)

| Proveedor | vCPU | RAM | SSD | Tráfico | Precio/mes | IP fija | Notas |
|-----------|------|-----|-----|---------|------------|---------|-------|
| **Hetzner** CX22 | 2 | 4 GB | 40 GB | 20 TB | ~€4.50 | Sí | Mejor relación calidad/precio |
| **Hetzner** CX32 | 4 | 8 GB | 80 GB | 20 TB | ~€8.50 | Sí | Recomendado si hay presupuesto |
| **Contabo** Cloud S | 4 | 8 GB | 200 GB | 32 TB | ~€6.99 | Sí | Mucho disco, rendimiento medio |
| **Contabo** Cloud M | 6 | 16 GB | 400 GB | 32 TB | ~€10.49 | Sí | Para escalar con monitoring pesado |
| **DigitalOcean** Basic | 2 | 4 GB | 80 GB | 4 TB | ~$24 | Sí | Ecosistema, precio alto |
| **OVH** Eco | 2 | 4 GB | 40 GB | ilimitado | ~€5.50 | Sí | Bueno si ya están en OVH |
| **Hostinger** VPS KVM 2 | 2 | 4 GB | 50 GB | 4 TB | ~€5.99 | Sí | Panel hPanel incluido |

### Proveedores no recomendados para este proyecto
- **AWS / GCP / Azure**: Costes impredecibles, configuración compleja para un proyecto de este tamaño.
- **Ionos / Strato**: Rendimiento inconsistente, soporte limitado.
- **Vultr**: Buena opción pero más caro que Hetzner sin ventajas claras.

### Recomendación final: **Hetzner CX22** (~€4.50/mes)

**Motivos:**
- Precio imbatible: ~€4.50/mes por 4 GB RAM + 2 vCPU + 40 GB SSD
- IP fija incluida
- Tráfico 20 TB/mes (más que suficiente)
- Data center en Europa (Finlandia/Alemania)
- API para automatización
- Buen soporte y comunidad
- Panel de control simple (Robot) o opcional CPanel

**Alternativa económica:** Contabo Cloud S (~€6.99/mes) si se necesita más disco.

### Requisitos mínimos reales

| Recurso | Mínimo | Recomendado |
|---------|--------|-------------|
| vCPU | 2 | 2 (Hetzner CX22 cumple) |
| RAM | 4 GB | 4 GB |
| SSD | 40 GB | 50 GB |
| SO | Ubuntu 22.04 LTS | Ubuntu 22.04 LTS |
| Docker | Sí | Sí |

### Proceso de contratación (Hetzner)

1. Ir a https://www.hetzner.com/cloud
2. Crear cuenta (email + datos básicos)
3. Verificar identidad (documento + ~24h)
4. Seleccionar **CX22** (2 vCPU, 4 GB RAM, 40 GB SSD)
5. SO: **Ubuntu 22.04 LTS**
6. Añadir **IP pública** (incluida)
7. Añadir **SSH Key** (generar local y pegar clave pública)
8. Finalizar pedido (~5 min tras aprobación)

---

## B) Dominio

### Configuración DNS

| Tipo | Nombre | Valor | TTL |
|------|--------|-------|-----|
| A | `@` | `<IP_DEL_VPS>` | 300 (5 min) |
| A | `www` | `<IP_DEL_VPS>` | 300 (5 min) |

**TTL recomendado:** 300s (5 min) durante el setup. Subir a 3600s (1h) tras verificar.

### Opcional: subdominios

| Subdominio | Tipo | Valor | Puerto | Servicio |
|------------|------|-------|--------|----------|
| `app.monteastur.com` | CNAME → `monteastur.com` o A directa | Misma IP | 80/443 | App |
| `grafana.monteastur.com` | A | Misma IP | 3000 | Grafana (requiere nginx extra) |
| `uptime.monteastur.com` | A | Misma IP | 3001 | Uptime Kuma (requiere nginx extra) |

> **Nota:** Los subdominios para Grafana/Uptime Kuma requieren configurar bloques `server` adicionales en nginx para hacer reverse proxy con SSL. Opcional para el primer deploy.

### Verificar propagación

```bash
# Instalar dig si no está
sudo apt install -y dnsutils

# Verificar registros
dig +short monteastur.com
dig +short www.monteastur.com
nslookup monteastur.com
```

Tiempo de propagación: 5 min a 48h. Con TTL=300, suele ser <15 min.

---

## C) HTTPS / SSL

### Proceso Let's Encrypt

```bash
# 1. Levantar stack (sin SSL todavía)
docker compose up -d

# 2. Verificar que nginx responde en HTTP con el dominio
curl -I http://monteastur.com

# 3. Obtener certificado
docker compose --profile certbot run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d monteastur.com -d www.monteastur.com \
  --email admin@monteastur.com \
  --agree-tos --no-eff-email

# 4. Verificar certificado descargado
ls -la /etc/letsencrypt/live/monteastur.com/
# → fullchain.pem, privkey.pem

# 5. Copiar a nginx/ssl
cp /etc/letsencrypt/live/monteastur.com/fullchain.pem nginx/ssl/
cp /etc/letsencrypt/live/monteastur.com/privkey.pem nginx/ssl/

# 6. Descomentar bloque HTTPS en nginx/conf.d/monteastur.conf
#    (líneas 39-73, cambiar server_name)

# 7. Recargar nginx
docker compose restart nginx
```

### Verificar SSL

```bash
# HTTPS responde correctamente
curl -I https://monteastur.com
# → HTTP/2 200 + security headers

# Certificado válido
echo | openssl s_client -connect monteastur.com:443 -servername monteastur.com 2>/dev/null | openssl x509 -noout -dates

# Prueba SSL Labs (externo)
# https://www.ssllabs.com/ssltest/analyze.html?d=monteastur.com
```

### Renovación automática

```cron
0 3 * * * cd /opt/monteastur && docker compose --profile certbot run --rm certbot renew && docker compose restart nginx
```

---

## D) Requisitos previos

Antes del primer deploy, asegurar:

### D.1) GitHub

- [ ] Repositorio clonado localmente
- [ ] Branch `develop` actualizada
- [ ] Últimos cambios pusheados:
  - [ ] `feature/fase-15-vps-produccion` merged a `develop`
  - [ ] CI pasando en `develop`
- [ ] Tags creados (al menos v14.0-e2e-ready)

### D.2) GitHub Secrets

- [ ] `VPS_HOST` = IP del VPS (la asigna Hetzner)
- [ ] `VPS_USER` = `deploy` (crear en bootstrap)
- [ ] `VPS_SSH_KEY` = clave privada generada en VPS
- [ ] `VPS_PORT` = `22` (dejar default)

### D.3) Local (tu máquina)

- [ ] SSH key generada: `ssh-keygen -t ed25519`
- [ ] `mvn test` → BUILD SUCCESS
- [ ] `npm test -- --run` → todos pasan
- [ ] `npm run build` → build exitoso
- [ ] `docker compose config` → válido

---

## E) GitHub Secrets checklist

| Secret | Estado | Notas |
|--------|--------|-------|
| `VPS_HOST` | ⬜ Pendiente | IP del VPS tras contratación |
| `VPS_USER` | ⬜ Pendiente | `deploy` (crear en paso 2) |
| `VPS_SSH_KEY` | ⬜ Pendiente | Generar en VPS, copiar privada |
| `VPS_PORT` | ⬜ Pendiente | `22` por defecto |

### Cómo generar la SSH key para GitHub Actions

```bash
# 1. Conectar al VPS como usuario deploy
ssh deploy@<VPS_IP>

# 2. Generar clave
ssh-keygen -t ed25519 -f ~/.ssh/github-actions -N ""

# 3. Autorizar
cat ~/.ssh/github-actions.pub >> ~/.ssh/authorized_keys

# 4. Mostrar privada (copiar a GitHub Secrets)
cat ~/.ssh/github-actions
```

### Cómo añadir secrets a GitHub

1. Ir a **GitHub → Repositorio → Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Añadir cada uno:

| Secret | Valor |
|--------|-------|
| `VPS_HOST` | `203.0.113.10` (IP real del VPS) |
| `VPS_USER` | `deploy` |
| `VPS_SSH_KEY` | Pegar contenido completo de `~/.ssh/github-actions` (incluyendo `-----BEGIN` y `-----END`) |
| `VPS_PORT` | `22` |

---

## F) Primer deploy seguro (paso a paso)

**Tiempo estimado total:** ~45 min (sin incluir propagación DNS)

### Paso 1: Contratar VPS (~10 min)

```bash
# Contratar Hetzner CX22 con Ubuntu 22.04
# Anotar IP que asignan (ej: 203.0.113.10)
```

### Paso 2: Conectar + Bootstrap (~5 min)

```bash
# Conectar como root (usando clave SSH que configuraste en Hetzner)
ssh root@203.0.113.10

# Descargar y ejecutar bootstrap
apt update && apt install -y git curl
git clone https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS.git /opt/monteastur
cd /opt/monteastur
sudo ./scripts/vps-bootstrap.sh

# Salir de root session
exit
```

### Paso 3: Crear usuario deploy y configurar SSH (~5 min)

```bash
# Volver a conectar como root
ssh root@203.0.113.10

# Crear usuario deploy
adduser deploy
usermod -aG docker deploy
usermod -aG sudo deploy

# Configurar SSH
nano /etc/ssh/sshd_config
# Asegurar:
#   PermitRootLogin no
#   PasswordAuthentication no
#   PubkeyAuthentication yes
#   AllowUsers deploy

# Copiar clave pública de root a deploy
cp -r ~/.ssh /home/deploy/
chown -R deploy:deploy /home/deploy/.ssh
chmod 700 /home/deploy/.ssh
chmod 600 /home/deploy/.ssh/authorized_keys

# Reiniciar SSH
systemctl restart sshd

# Probar conexión como deploy (desde otra terminal)
ssh deploy@203.0.113.10

# Si funciona, cerrar sesión root
exit
```

### Paso 4: Configurar deploy + clonar repo (~5 min)

```bash
# Ya como deploy en /opt/monteastur (clonado en paso 2)
cd /opt/monteastur

# Configurar .env
cp .env.example .env
nano .env
# Cambiar:
#   MYSQL_ROOT_PASSWORD=<generar>
#   MYSQL_PASSWORD=<generar>
#   DB_PASSWORD=<generar>
#   ADMIN_PASSWORD=<generar>
#   GRAFANA_ADMIN_PASSWORD=<generar>
#   SPRING_PROFILES_ACTIVE=prod
#   TZ=America/Asuncion
```

### Paso 5: Generar SSH key para GitHub Actions (~2 min)

```bash
ssh-keygen -t ed25519 -f ~/.ssh/github-actions -N ""
cat ~/.ssh/github-actions.pub >> ~/.ssh/authorized_keys
cat ~/.ssh/github-actions
# → Copiar esta salida, será VPS_SSH_KEY en GitHub Secrets
```

### Paso 6: Primer docker compose (~10 min)

```bash
cd /opt/monteastur
docker compose build
docker compose up -d

# Verificar contenedores
docker ps
# → 6/6 containers UP (app, nginx, db, prometheus, grafana, uptime-kuma)

# Verificar healthcheck
curl -f http://localhost/actuator/health
# → {"status":"UP"}
```

### Paso 7: Configurar DNS + HTTPS (~10 min + propagación)

```bash
# 1. Ir al panel de dominio y crear registros A
# 2. Esperar propagación (verificar con dig)
# 3. Obtener certificado SSL (ver sección HTTPS)
# 4. Descomentar HTTPS en nginx
# 5. Recargar nginx
```

### Paso 8: Configurar GitHub Secrets (~5 min)

```bash
# En GitHub:
# Settings → Secrets and variables → Actions
# Añadir:
#   VPS_HOST = 203.0.113.10
#   VPS_USER = deploy
#   VPS_SSH_KEY = (lo copiado en paso 5)
#   VPS_PORT = 22
```

### Paso 9: Ejecutar workflow manual (~5 min)

```bash
# En GitHub:
# Actions → Deploy Production → Run workflow
# Branch: develop
# Confirm: "deploy"
```

### Paso 10: Validaciones finales (~5 min)

```bash
# Ver todos los servicios OK
./scripts/server-healthcheck.sh

# Ver HTTPS
curl -I https://monteastur.com

# Ver monitoring
curl http://localhost:9090/targets
```

---

## G) Validaciones post-deploy

### Web

- [ ] `curl -I https://monteastur.com` → HTTP/2 200
- [ ] Home page carga sin errores
- [ ] `/seguimiento` funciona
- [ ] `/login` funciona
- [ ] `/react-dashboard` carga sin errores
- [ ] `/admin/dashboard` (Thymeleaf) funciona

### Actuator

- [ ] `curl -f https://monteastur.com/actuator/health` → `{"status":"UP"}`
- [ ] `curl https://monteastur.com/actuator/info` → JSON con info app

### Monitoring

- [ ] Prometheus accesible: `http://<VPS_IP>:9090`
- [ ] Prometheus targets: `up{application="monteastur-envios"}`
- [ ] Grafana accesible: `http://<VPS_IP>:3000`
- [ ] Grafana dashboards cargan (Monteastur Envios)
- [ ] Uptime Kuma accesible: `http://<VPS_IP>:3001`
- [ ] Uptime Kuma monitores configurados

### SSL

- [ ] HTTPS redirige correctamente
- [ ] Certificado válido (no expirado)
- [ ] Security headers presentes: `curl -I https://monteastur.com`
- [ ] HSTS configurado (opcional)
- [ ] SSL Labs test: grade A o superior

### Docker

- [ ] `docker ps` → 6/6 containers UP
- [ ] `docker logs monteastur-app --tail 20` sin errores
- [ ] `docker system df` sin uso excesivo

### Logs

- [ ] Logs de app se escriben sin errores de permisos
- [ ] Logs de nginx accesibles

### Backups

- [ ] `./scripts/backup-db.sh` funciona
- [ ] `./scripts/backup-uploads.sh` funciona
- [ ] Crontab configurado

---

## H) Rollback real

### Probar rollback funcional

```bash
# 1. Ejecutar healthcheck actual
./scripts/server-healthcheck.sh

# 2. Ejecutar rollback a tag conocido
./scripts/rollback-prod.sh v14.0-e2e-ready

# 3. Verificar que la app sigue funcionando
./scripts/server-healthcheck.sh

# 4. Volver a develop
git checkout develop
docker compose up -d --build
```

### Rollback de base de datos

```bash
# Si el deploy rompió datos:
./scripts/restore-db.sh backup/db/<backup_anterior>.sql.gz
./scripts/restore-uploads.sh backup/uploads/<backup_anterior>.tar.gz
docker compose up -d --build
```

### Criterios para hacer rollback

Si después del deploy ocurre **al menos uno** de estos:

- 🔴 Healthcheck no responde después de 3 intentos
- 🔴 Login admin no funciona
- 🔴 Login cliente no funciona
- 🔴 Tracking público no funciona
- 🟡 Upload/subida de imágenes no funciona
- 🟡 Página principal con errores visibles
- 🟡 Logs con errores críticos continuos

---

## I) Coste estimado mensual

| Concepto | Proveedor | Coste/mes | Notas |
|----------|-----------|-----------|-------|
| VPS CX22 | Hetzner | ~€4.50 | 2 vCPU, 4 GB RAM, 40 GB SSD |
| Dominio `.com` | Namecheap / Cloudflare | ~€10/año (~€0.83/mes) | Renovación anual |
| SSL Let's Encrypt | Gratuito | €0 | Incluido |
| Monitoring | Prometheus + Grafana | €0 | Auto-hospedado |
| Uptime monitoring | Uptime Kuma | €0 | Auto-hospedado |
| Backup externo (opcional) | S3 o SCP | €0–5 | Opcional, S3 Glacier muy barato |
| **Total mínimo** | | **~€5.33/mes** | |
| **Total con backup externo** | | **~€10/mes** | |
| **Total anual aprox** | | **~€65–120/año** | |

### Desglose primer mes

| Concepto | Coste |
|----------|-------|
| Setup VPS (primer mes) | €4.50 |
| Dominio (prorrateado) | €0.83 |
| **Total primer mes** | **~€5.33** |

---

## J) Checklist ejecución

### Pre-deploy (días antes)

- [ ] Contratar VPS (Hetzner CX22 recomendado)
- [ ] Anotar IP del VPS
- [ ] Configurar DNS (registros A)
- [ ] Tener dominio .com registrado
- [ ] Verificar que `feature/fase-15-vps-produccion` está merged a `develop`
- [ ] CI pasando en `develop`
- [ ] Tags creados (v14.0-e2e-ready)

### Día del deploy

- [ ] Bootstrap VPS (paso 2)
- [ ] Crear usuario deploy (paso 3)
- [ ] Configurar SSH (paso 3)
- [ ] Configurar .env (paso 4)
- [ ] Generar SSH key GitHub Actions (paso 5)
- [ ] Primer docker compose up (paso 6)
- [ ] Configurar DNS + HTTPS (paso 7)
- [ ] Configurar GitHub Secrets (paso 8)
- [ ] Ejecutar workflow manual (paso 9)
- [ ] Validaciones post-deploy (paso 10)

### Post-deploy (primeros días)

- [ ] Monitorear logs diariamente
- [ ] Verificar backups automáticos
- [ ] Probar rollback
- [ ] Configurar alertas Grafana
- [ ] Configurar Uptime Kuma
- [ ] Hacer test SSL Labs
- [ ] Verificar no hay errores en `server-healthcheck.sh`
- [ ] Subir TTL DNS a 3600s tras verificar estabilidad

---

> **Documentos relacionados:**
> - [`PRODUCTION_VPS_RUNBOOK.md`](PRODUCTION_VPS_RUNBOOK.md) — Runbook completo
> - [`VPS_HARDENING_CHECKLIST.md`](VPS_HARDENING_CHECKLIST.md) — Hardening de seguridad
>
> **Mantenido por:** Equipo Monteastur Envios
> **Próxima revisión:** 2026-06-24
