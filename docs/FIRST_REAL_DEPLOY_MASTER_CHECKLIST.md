# Monteastur Envios — Master Checklist Primer Deploy Real

> **Versión:** 1.0 | **Fecha:** 2026-05-25
> **Duración total estimada:** ~4h (sin contar verificación Hetzner ~24h ni propagación DNS)
> **Coste mensual estimado:** ~€5.33/mes (VPS + dominio)
> **Documentos consolidados:** VPS_REAL_EXECUTION_GUIDE, FIRST_REAL_DEPLOY_COMMANDS, HETZNER_VPS_PURCHASE_GUIDE, DOMAIN_PURCHASE_GUIDE, GITHUB_SECRETS_SSH_SETUP, FINAL_PRODUCTION_DEPLOY_CHECKLIST, SMOKE_TESTS_PRODUCTION

---

## Instrucciones

1. Seguir las fases en orden estricto (A → P)
2. Marcar cada checkbox al completarlo
3. Si una fase falla, NO pasar a la siguiente sin resolver
4. Tiempos estimados son orientativos
5. Reemplazar `<IP_DEL_VPS>` y `<DOMINIO>` por valores reales

---

## Fase A — Compra VPS (Hetzner) ~30 min + ~24h verificación

### A.1 Crear cuenta Hetzner

- [ ] Ir a [hetzner.com](https://www.hetzner.com/) → Cloud Console → Register
- [ ] Rellenar registro: nombre, email, contraseña
- [ ] Verificar email (revisar spam si no llega)
- [ ] Subir documentación identidad (DNI/NIE/pasaporte + comprobante domicilio)
- [ ] Esperar verificación (~2-24h hábiles)
- [ ] Añadir método de pago (PayPal recomendado)

### A.2 Crear servidor

- [ ] Ir a [console.hetzner.cloud](https://console.hetzner.cloud/) → New Project
- [ ] Click **Add Server**
- [ ] Configurar:
  - Location: Nuremberg (NBG) o Helsinki (HEL)
  - Image: **Ubuntu 24.04 LTS**
  - Type: **CX22** (2 vCPU, 4 GB RAM, 40 GB SSD)
  - IPv4: Sí
  - IPv6: Opcional
  - SSH Key: Subir clave pública (generar si no existe)
  - Backups: Sí (+20% precio)
  - Firewall: Crear con reglas 22, 80, 443
- [ ] Click **Create & Buy Now**
- [ ] Anotar **IP pública**: `___________`
- [ ] Guardar contraseña temporal (si no usó SSH Key)

### A.3 Verificación

- [ ] `ssh root@<IP_DEL_VPS>` funciona
- [ ] `cat /etc/os-release` → Ubuntu 24.04
- [ ] `apt update && apt upgrade -y` ejecutado
- [ ] Herramientas instaladas: `apt install -y htop curl wget git ufw`

---

## Fase B — Compra Dominio ~15 min

### B.1 Elegir proveedor

- [ ] **Opción recomendada:** Cloudflare Registrar (~€9.15/año)
- [ ] Alternativa: Namecheap (~€10.69/año)

### B.2 Elegir nombre

- [ ] **Recomendado:** `monteastur.com` (10 letras, .com, sin guiones)
- [ ] Alternativas: `monteastur.es`, `monteasturenvios.com`
- [ ] Verificar disponibilidad en el proveedor

### B.3 Comprar

- [ ] Añadir al carrito
- [ ] WHOIS privado: activado (incluido en Cloudflare)
- [ ] Renovación automática: activada
- [ ] Completar pago
- [ ] Confirmar propiedad del dominio (email de verificación)

---

## Fase C — Configurar DNS ~15 min + propagación

### C.1 Crear registros A

| Tipo | Nombre | Valor | TTL |
|------|--------|-------|-----|
| A | `@` | `<IP_DEL_VPS>` | 300 |
| A | `www` | `<IP_DEL_VPS>` | 300 |
| A | `api` | `<IP_DEL_VPS>` | 300 |
| A | `monitor` | `<IP_DEL_VPS>` | 300 |

- [ ] Registro `@` → IP del VPS
- [ ] Registro `www` → IP del VPS
- [ ] Registro `api` → IP del VPS
- [ ] Registro `monitor` → IP del VPS

### C.2 Cloudflare (si aplica)

- [ ] Proxy ON para `@` y `www`
- [ ] Proxy OFF para `api` y `monitor`
- [ ] SSL/TLS: **Full (strict)**
- [ ] Always Use HTTPS: ON

### C.3 Verificar propagación

- [ ] `ping <DOMINIO>` resuelve a IP correcta
- [ ] `curl -I http://<DOMINIO>` responde

---

## Fase D — Entrar por SSH ~10 min

### D.1 Configurar usuario deploy

```bash
# Como root en VPS
adduser deploy
usermod -aG sudo deploy
```

- [ ] Usuario `deploy` creado
- [ ] `deploy` en grupo `sudo`

### D.2 Copiar clave SSH

```bash
# Desde local
ssh-copy-id deploy@<IP_DEL_VPS>
```

- [ ] `ssh deploy@<IP_DEL_VPS>` funciona sin contraseña

### D.3 Hardening SSH

- [ ] `PermitRootLogin no`
- [ ] `PasswordAuthentication no`
- [ ] `AllowUsers deploy`
- [ ] `systemctl restart sshd`
- [ ] Probar conexión como `deploy` en terminal separada ANTES de cerrar root

---

## Fase E — Bootstrap Servidor ~15 min

### E.1 Copiar y ejecutar bootstrap

```bash
# Desde local
scp scripts/vps-bootstrap.sh deploy@<IP_DEL_VPS>:~/

# En VPS como deploy
chmod +x ~/vps-bootstrap.sh
sudo ~/vps-bootstrap.sh
```

- [ ] Script bootstrap ejecutado sin errores
- [ ] `docker --version` muestra versión
- [ ] `docker compose version` muestra versión
- [ ] `sudo ufw status verbose` → puertos 22, 80, 443 abiertos
- [ ] `sudo fail2ban-client status` → jail sshd activo

### E.2 Verificar estructura

- [ ] Directorio `/opt/monteastur/` creado
- [ ] Subdirectorios: `scripts/`, `backup/`, `logs/`, `nginx/`, `monitoring/`

---

## Fase F — Clonar Repo ~5 min

### F.1 Clonar

```bash
# En VPS como deploy
cd /opt
sudo git clone https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS.git monteastur
sudo chown -R deploy:deploy monteastur
cd monteastur
```

- [ ] Repositorio clonado en `/opt/monteastur`
- [ ] Propietario `deploy:deploy`
- [ ] `git branch` → `develop` (o la rama deseada)

### F.2 Hacer scripts ejecutables

- [ ] `chmod +x /opt/monteastur/scripts/*.sh`

---

## Fase G — Crear .env Real ~10 min

### G.1 Copiar plantilla

```bash
cp /opt/monteastur/.env.production.example /opt/monteastur/.env
```

- [ ] `.env` creado desde `.env.production.example`

### G.2 Generar credenciales

| Variable | Comando | Valor generado |
|----------|---------|----------------|
| `MYSQL_ROOT_PASSWORD` | `openssl rand -base64 32` | |
| `MYSQL_PASSWORD` | `openssl rand -base64 32` | |
| `DB_PASSWORD` | `openssl rand -base64 32` | |
| `ADMIN_PASSWORD` | `openssl rand -base64 32` | |
| `GRAFANA_ADMIN_PASSWORD` | `openssl rand -base64 16` | |

### G.3 Configurar variables

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `DB_DDL_AUTO=update` (primera vez; luego `validate`)
- [ ] `TZ=America/Asuncion`
- [ ] `JPA_SHOW_SQL=false`
- [ ] `SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/${MYSQL_DATABASE}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`
- [ ] `chmod 600 .env` (proteger credenciales)

### G.4 Verificar

- [ ] `docker compose config` → sin errores de sintaxis

---

## Fase H — Primer Docker Compose Up ~10 min

### H.1 Build y up

```bash
cd /opt/monteastur
docker compose up -d --build
```

- [ ] Build completo sin errores
- [ ] Todos los contenedores UP:

| Contenedor | Estado esperado |
|------------|----------------|
| `monteastur-app` | ✅ UP |
| `monteastur-mysql` | ✅ UP (healthy) |
| `monteastur-nginx` | ✅ UP |
| `monteastur-prometheus` | ✅ UP |
| `monteastur-grafana` | ✅ UP |
| `monteastur-uptime-kuma` | ✅ UP |

### H.2 Verificar healthchecks

```bash
curl -f http://localhost/actuator/health
curl -f http://localhost
curl -f http://localhost:9090/targets
curl -f http://localhost:3000
curl -f http://localhost:3001
```

- [ ] `/actuator/health` → `{"status":"UP"}`
- [ ] `/` → HTML 200
- [ ] Prometheus targets → UP
- [ ] Grafana login → 200
- [ ] Uptime Kuma → 200

### H.3 Cambiar a validate

- [ ] Editar `.env`: `DB_DDL_AUTO=validate`
- [ ] `docker compose up -d --build`
- [ ] Healthcheck sigue OK

---

## Fase I — Crear Certificados HTTPS ~10 min + propagación

### I.1 Obtener certificado

```bash
# Asegurar DNS propagado (paso C)
sudo certbot certonly --standalone \
  -d <DOMINIO> -d www.<DOMINIO> \
  --email admin@<DOMINIO> \
  --agree-tos --non-interactive
```

- [ ] Certificado obtenido sin errores
- [ ] `sudo certbot certificates` muestra dominio y fechas

### I.2 Copiar certificados a nginx (si aplica ruta manual)

- [ ] `fullchain.pem` disponible
- [ ] `privkey.pem` disponible

### I.3 Configurar renovación automática

```bash
sudo crontab -e
# Añadir:
0 3 * * * certbot renew --quiet && docker compose -f /opt/monteastur/docker-compose.yml exec nginx nginx -s reload
```

- [ ] Crontab configurado
- [ ] `sudo certbot renew --dry-run` → OK

### I.4 Verificar HTTPS

- [ ] `curl -I https://<DOMINIO>` → HTTP/2 200
- [ ] `strict-transport-security` header presente
- [ ] Abrir `https://<DOMINIO>` en navegador → carga correcta

---

## Fase J — Configurar GitHub Secrets ~10 min

### J.1 Generar clave SSH dedicada

```bash
# Desde local
ssh-keygen -t ed25519 -f ~/.ssh/monteastur_deploy_ed25519 -N ""
```

- [ ] Clave generada: `~/.ssh/monteastur_deploy_ed25519` (privada) + `.pub` (pública)

### J.2 Copiar clave pública al VPS

```bash
ssh-copy-id -i ~/.ssh/monteastur_deploy_ed25519.pub deploy@<IP_DEL_VPS>
```

- [ ] `ssh -i ~/.ssh/monteastur_deploy_ed25519 deploy@<IP_DEL_VPS> "echo OK"` → OK

### J.3 Añadir secrets en GitHub

Ir a: **GitHub → Repositorio → Settings → Secrets and variables → Actions → New repository secret**

- [ ] `VPS_HOST` = `<IP_DEL_VPS>` o `<DOMINIO>`
- [ ] `VPS_USER` = `deploy`
- [ ] `VPS_SSH_KEY` = contenido completo de `monteastur_deploy_ed25519` (incluyendo BEGIN/END)
- [ ] `VPS_PORT` = `22`

### J.4 Verificar conexión

```bash
VPS_HOST=<IP_DEL_VPS> ./scripts/check-ssh-connection.sh
```

- [ ] Script muestra `[OK] Conexión SSH exitosa`
- [ ] `[OK] Docker accesible`

---

## Fase K — Ejecutar Deploy-Prod Manual ~10 min

### K.1 Desde GitHub Actions

- [ ] Ir a **GitHub → Actions → Deploy Production**
- [ ] Click **Run workflow**
- [ ] Branch: `develop`
- [ ] Confirmación: `deploy`
- [ ] Job `pre-deploy-check` pasa
- [ ] Job `deploy-production` pasa
- [ ] Job `notify-failure` no se ejecuta

### K.2 Verificación post-workflow

- [ ] `docker ps` → 6/6 containers UP
- [ ] `curl -f http://localhost/actuator/health` → `{"status":"UP"}`

### K.3 Configurar crontab completo

```bash
crontab -e
```

- [ ] Backup BD: `0 3 * * * /opt/monteastur/scripts/backup-db.sh`
- [ ] Backup uploads: `0 4 * * * /opt/monteastur/scripts/backup-uploads.sh`
- [ ] Limpieza backups antiguos: `0 5 * * * find /opt/monteastur/backup -name "*.sql.gz" -mtime +30 -delete`
- [ ] Limpieza backups uploads: `0 5 * * * find /opt/monteastur/backup -name "*.tar.gz" -mtime +30 -delete`
- [ ] Renovación SSL: `0 3 * * * cd /opt/monteastur && docker compose --profile certbot run --rm certbot renew && docker compose restart nginx`
- [ ] Limpieza Docker: `0 6 * * 0 docker image prune -af`

---

## Fase L — Ejecutar Smoke Tests ~15 min

### L.1 Automáticos

```bash
# Desde local o VPS
BASE_URL=https://<DOMINIO> ./scripts/production-smoke-test.sh
```

- [ ] Healthcheck endpoint → `{"status":"UP"}`
- [ ] Home page carga → 200 OK
- [ ] Login admin funciona
- [ ] Tracking público funciona

### L.2 Manuales (navegador)

- [ ] Login cliente funciona
- [ ] Dashboard React SPA carga sin errores
- [ ] Upload / subida de imágenes funciona
- [ ] Monitoring accesible (Prometheus, Grafana, Uptime Kuma)
- [ ] PWA instalable
- [ ] SSL Labs Grade A+
- [ ] Mobile responsive

### L.3 Criterio de aceptación

- [ ] **Todos los tests 🔴 pasan** (si no, deploy NO es exitoso)

---

## Fase M — Verificar Monitoring ~10 min

### M.1 Prometheus

- [ ] `curl http://localhost:9090/targets` → app `UP`
- [ ] `curl http://localhost:9090/api/v1/query?query=up` → datos

### M.2 Grafana

- [ ] Accesible en `http://<IP>:3000` o `https://monitor.<DOMINIO>`
- [ ] Login funciona (admin / password del .env)
- [ ] Dashboard "Monteastur Envios" cargado
- [ ] Paneles con datos (no "No data")

### M.3 Uptime Kuma

- [ ] Accesible en `http://<IP>:3001`
- [ ] Monitor HTTP configurado para `https://<DOMINIO>`
- [ ] Monitor SSL configurado

### M.4 Script healthcheck

- [ ] `./scripts/server-healthcheck.sh` → All checks passed

---

## Fase N — Probar Backup ~10 min

### N.1 Backup base de datos

- [ ] `./scripts/backup-db.sh` → archivo .sql.gz en `backup/db/`
- [ ] Archivo con peso > 0 bytes

### N.2 Backup uploads

- [ ] `./scripts/backup-uploads.sh` → archivo .tar.gz en `backup/uploads/`
- [ ] Archivo con peso > 0 bytes

### N.3 Verificar restore (opcional en primer deploy)

- [ ] `./scripts/restore-db.sh backup/db/<archivo>.sql.gz` → OK
- [ ] App sigue funcionando tras restore

---

## Fase O — Probar Rollback ~5 min

### O.1 Ejecutar rollback

- [ ] `./scripts/rollback-prod.sh v14.0-e2e-ready` → sin errores

### O.2 Verificar recuperación

- [ ] `docker ps` → 6/6 containers UP
- [ ] `curl -f http://localhost/actuator/health` → `{"status":"UP"}`
- [ ] `curl -I https://<DOMINIO>` → 200 OK

### O.3 Restaurar develop

- [ ] `git checkout develop && docker compose up -d --build`
- [ ] Healthcheck OK

---

## Fase P — Checklist 24h Post-Deploy

### P.1 Sistema

- [ ] CPU normal: `uptime` (load < 2.0)
- [ ] RAM disponible: `free -h` (uso < 80%)
- [ ] Disco: `df -h /` (uso < 80%)
- [ ] Docker: `docker ps` → 6/6 UP

### P.2 Aplicación

- [ ] `curl -f localhost/actuator/health` → UP
- [ ] Logs sin errores: `docker logs monteastur-app --since 24h | grep -i error | wc -l`
- [ ] Logs nginx sin 5xx: `docker logs monteastur-nginx --since 24h | grep ' 5' | wc -l`

### P.3 SSL

- [ ] Certificado válido (no expirado)
- [ ] Renovación automática: `sudo certbot renew --dry-run`

### P.4 Backups

- [ ] Backup BD ejecutado (archivo en `backup/db/` con fecha de hoy)
- [ ] Backup uploads ejecutado (archivo en `backup/uploads/` con fecha de hoy)

### P.5 Monitoring

- [ ] Prometheus targets UP
- [ ] Grafana dashboards con datos
- [ ] Uptime Kuma sin alertas
- [ ] `./scripts/server-healthcheck.sh` → All checks passed

### P.6 Usuarios

- [ ] Login admin funciona
- [ ] Login cliente funciona
- [ ] Tracking público funciona
- [ ] Sin reportes de error del equipo

---

## Plan de Contingencia

| Síntoma | Acción |
|---------|--------|
| Healthcheck no responde | `docker logs monteastur-app --tail 50` |
| Nginx 502 | `docker ps` verificar app; `docker logs app` |
| MySQL no conecta | `docker logs monteastur-mysql` |
| Build falla | Revisar `docker compose build` output |
| Certbot NXDOMAIN | DNS no propagado → esperar y reintentar |
| Certbot connection refused | Puerto 80 bloqueado → verificar UFW |
| Disco lleno | `docker system prune -af` |
| **Rollback inmediato** | `./scripts/rollback-prod.sh v14.0-e2e-ready` |

---

## Resumen de costes

| Concepto | Coste |
|----------|-------|
| VPS Hetzner CX22 | ~€4.50/mes |
| Backup VPS (+20%) | ~€0.90/mes |
| Dominio .com (Cloudflare) | ~€0.76/mes (~€9.15/año) |
| SSL / Monitoring / CI/CD | €0 |
| **Total mensual** | **~€5.33/mes** |
| **Total primer año** | **~€69** |

---

> **Documentos relacionados:**
> - [`REAL_DEPLOY_DECISION_LOG.md`](REAL_DEPLOY_DECISION_LOG.md) — Decisiones técnicas y justificaciones
> - [`scripts/production-smoke-test.sh`](../scripts/production-smoke-test.sh) — Smoke tests automatizados
> - [`scripts/production-post-deploy-check.sh`](../scripts/production-post-deploy-check.sh) — Verificación post-deploy
> - [`PRODUCTION_VPS_RUNBOOK.md`](PRODUCTION_VPS_RUNBOOK.md) — Runbook de operaciones diarias
> - [`VPS_HARDENING_CHECKLIST.md`](VPS_HARDENING_CHECKLIST.md) — Hardening de seguridad
>
> **Mantenido por:** Equipo Monteastur Envios
> **Próxima revisión:** 2026-06-25
