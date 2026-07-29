# VPS Deploy Day Runbook — MonteAstur Envios

## Regla #1: No improvisar

Cada paso está documentado. Si algo no sale como esperas, **PARA**, lee el risk register, decide si continuar o hacer rollback.

## Regla #2: Preparar antes de empezar

- [ ] Café/té preparado
- [ ] Sin distracciones (2h estimadas)
- [ ] Terminal SSH abierta pero sin conectar
- [ ] GitHub abierto en el repo
- [ ] Hetzner dashboard abierto (o similar)
- [ ] Cloudflare dashboard abierto (o DNS provider)
- [ ] Gestor de contraseñas abierto
- [ ] Teléfono con 2FA listo

## Regla #3: Una terminal, un foco

No abras 10 terminales. Usa una sesión SSH y ve paso a paso.

---

## Ejecución

### FASE 0: Preparación (30 min)

```bash
# 0.1 — Comprar VPS en Hetzner
#   - Plan: CX22
#   - Imagen: Ubuntu 24.04 LTS
#   - SSH key: Añadir tu clave pública
#   - Anotar: IP del VPS (ej: 203.0.113.10)

# 0.2 — Comprar dominio en Cloudflare
#   - Dominio: monteastur.com
#   - Coste: ~€9.15/año
#   - Activar WHOIS privacy

# 0.3 — Configurar DNS
#   - Añadir registro A: @ → IP_DEL_VPS
#   - Añadir registro A: www → IP_DEL_VPS
#   - DNS Only (gris) hasta que SSL esté listo
#   - Anotar nameservers si aplica

# 0.4 — Generar contraseñas
openssl rand -base64 32   # ADMIN_PASSWORD
openssl rand -base64 32   # MYSQL_ROOT_PASSWORD
openssl rand -base64 32   # MYSQL_PASSWORD / DB_PASSWORD
openssl rand -base64 32   # GRAFANA_ADMIN_PASSWORD

# 0.5 — Guardar en gestor de contraseñas
#   - Bitwarden / 1Password / KeePass
```

> **Checkpoint FASE 0**: ¿VPS creado? ¿Dominio comprado? ¿DNS configurado? ¿Passwords guardadas? ✅

---

### FASE 1: VPS Bootstrap (20 min)

```bash
# 1.1 — SSH como root
ssh root@<IP_DEL_VPS>

# 1.2 — Crear usuario deploy
adduser deploy
# (rellenar: contraseña segura, nombre completo opcional)
usermod -aG sudo deploy

# 1.3 — Copiar clave SSH pública
su - deploy
mkdir -p ~/.ssh && chmod 700 ~/.ssh
nano ~/.ssh/authorized_keys
# Pegar clave pública (NO la privada)
chmod 600 ~/.ssh/authorized_keys

# Probar SSH con usuario deploy
# (desde otra terminal)
ssh deploy@<IP_DEL_VPS>
# Si funciona, cerrar sesión root

# 1.4 — Ejecutar bootstrap
# Como deploy con sudo:
sudo ./scripts/vps-bootstrap.sh
```

> **Checkpoint FASE 1**: ¿SSH funciona con usuario deploy? ¿Bootstrap terminado sin errores? ✅

---

### FASE 2: Docker (30 min)

```bash
# 2.1 — Clonar repositorio
ssh deploy@<IP_DEL_VPS>
cd /opt/monteastur
git clone https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS.git .
git checkout v20.0-pre-deploy

# 2.2 — Crear .env con valores generados en Fase 0.4
cp .env.production.example .env
nano .env
# Editar TODOS los CHANGE_ME con las contraseñas generadas
# IMPORTANTE: DB_DDL_AUTO=update (SOLO primer arranque)

# 2.3 — Primer arranque
docker compose up -d --build

# 2.4 — Esperar y verificar
sleep 60
curl -f http://localhost/actuator/health
# Debe responder: {"status":"UP"}

# 2.5 — Verificar containers
docker compose ps
# Todos deben mostrar "Up" o "Up (healthy)"

# 2.6 — Cambiar a validate
nano .env
# DB_DDL_AUTO=validate

# 2.7 — Recargar sin rebuild
docker compose up -d
```

> **Checkpoint FASE 2**: ¿Healthcheck UP? ¿6/6 containers UP? ✅

---

### FASE 3: SSL (15 min)

```bash
# 3.1 — Verificar DNS propagado
dig +short monteastur.com
# Debe mostrar la IP del VPS

# 3.2 — Obtener certificado
docker compose --profile certbot run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d monteastur.com -d www.monteastur.com \
  --email admin@monteastur.com \
  --agree-tos --no-eff-email

# 3.3 — Copiar certificados (como root o con sudo)
sudo cp /etc/letsencrypt/live/monteastur.com/fullchain.pem nginx/ssl/
sudo cp /etc/letsencrypt/live/monteastur.com/privkey.pem nginx/ssl/
sudo chown -R deploy:deploy nginx/ssl/

# 3.4 — Activar HTTPS
cp nginx/examples/production-example.conf nginx/conf.d/monteastur-prod.conf
nano nginx/conf.d/monteastur-prod.conf
# Verificar server_name: monteastur.com www.monteastur.com
# Verificar rutas SSL correctas

# 3.5 — Recargar nginx
docker compose restart nginx

# Verificar HTTPS
curl -I https://monteastur.com
# Debe responder 200 OK con HSTS header

# Configurar renovación automática
crontab -e
# Añadir:
0 3 * * * cd /opt/monteastur && docker compose --profile certbot run --rm certbot renew && docker compose restart nginx
```

> **Checkpoint FASE 3**: ¿HTTPS funciona? ¿Certificado válido? ¿Renovación automática configurada? ✅

---

### FASE 4: GitHub Actions (10 min)

```bash
# 4.1 — Configurar GitHub Secrets
# Ir a: GitHub → Settings → Secrets and variables → Actions
# Añadir:
#   VPS_HOST: <IP_DEL_VPS>
#   VPS_USER: deploy
#   VPS_SSH_KEY: <contenido de ~/.ssh/github-actions-monteastur>
#   VPS_PORT: 22

# 4.2 — Ejecutar workflow manual
# GitHub → Actions → Deploy Production → Run workflow
# Branch: main
# Confirmación: deploy

# 4.3 — Verificar
# Esperar a que el workflow termine en verde
# Verificar healthcheck post-deploy
```

> **Checkpoint FASE 4**: ¿GitHub Secrets configurados? ¿Workflow pasa? ✅

---

### FASE 5: Smoke Tests (15 min)

```bash
# 5.1 — Ejecutar smoke tests oficiales
BASE_URL=https://monteastur.com ./scripts/production-smoke-test.sh

# 5.2 — Verificar manualmente
curl -f https://monteastur.com/actuator/health
curl -I https://monteastur.com

# 5.3 — Login admin
# Abrir https://monteastur.com/login
# Probar credenciales admin

# 5.4 — Login cliente
# Abrir https://monteastur.com/cliente/login
# (solo si hay datos demo; si no, verificar formulario)

# 5.5 — Verificar monitoring
curl -f http://localhost:9090/targets   # Prometheus
curl -f http://monteastur.com:3000      # Grafana
curl -f http://monteastur.com:3001      # Uptime Kuma
```

> **Checkpoint FASE 5**: ¿Smoke tests pasan? ¿Login funciona? ✅

---

### FASE 6: Hardening (15 min)

```bash
# 6.1 — Configurar fail2ban
sudo apt install fail2ban -y
sudo cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# 6.2 — Verificar UFW
sudo ufw status verbose
# Debe mostrar: 22/tcp, 80/tcp, 443/tcp

# 6.3 — Configurar crontab backups
crontab -e
# Añadir:
0 3 * * * /opt/monteastur/scripts/backup-db.sh
0 4 * * * /opt/monteastur/scripts/backup-uploads.sh
0 5 * * * find /opt/monteastur/backup -name "*.sql.gz" -mtime +30 -delete

# 6.4 — Verificar logs sin errores
docker compose logs app --tail 30
# No debe haber errores WARN+ (excepto arranque normal)
```

> **Checkpoint FASE 6**: ¿fail2ban activo? ¿UFW correcto? ¿Backups programados? ✅

---

### FASE 7: Monitoring 24h

```bash
# Abrir Grafana: https://monteastur.com:3000
# Verificar dashboard Monteastur Envios
# Todos los paneles deben mostrar datos

# Abrir Uptime Kuma: https://monteastur.com:3001
# Configurar monitor para https://monteastur.com

# Abrir Prometheus: https://monteastur.com:9090/targets
# Todos los targets UP

# Dejar monitoreando...
```

> **Checkpoint FASE 7**: ¿Grafana muestra datos? ¿Uptime Kuma monitorizando? ✅

---

## Cuándo parar

| Situación | Acción |
|-----------|--------|
| VPS no arranca en Hetzner | Parar. Contactar soporte Hetzner. |
| SSH no conecta tras 5 intentos | Parar. Verificar IP, firewall, SSH key. |
| Bootstrap falla | Parar. Leer error, buscar en docs. |
| Docker build falla (>10 min) | Parar. Verificar Dockerfile, disk space. |
| Healthcheck no UP tras 3 min | Parar. Verificar logs de app y db. |
| SSL certbot falla | Parar. Verificar DNS propagado y puerto 80 abierto. |
| Smoke tests fallan | Parar. NO hacer deploy como exitoso. |

---

## Cuándo hacer rollback

Ver `docs/VPS_DEPLOY_EXECUTION_PLAN.md` — sección "Rollback plan".

Resumen:

```bash
cd /opt/monteastur && ./scripts/rollback-prod.sh v20.0-pre-deploy
```

Si el rollback no funciona:

```bash
git checkout v20.0-pre-deploy
docker compose up -d --build
```

---

## Post-deploy (primeras 24h)

- [ ] Revisar Grafana dashboard cada 4h
- [ ] Revisar Uptime Kuma status
- [ ] Verificar que backups nocturnos se ejecutan
- [ ] No hacer cambios en producción hasta pasadas 24h
- [ ] Documentar cualquier incidencia en docs/DEPLOY_LOG.md

---

## Si todo va bien

```bash
# Cerrar sesión SSH
exit

# Celebrar 🎉
# Pero recordar: la operativa real comienza ahora
```