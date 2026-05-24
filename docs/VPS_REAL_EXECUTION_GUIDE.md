# Guía de Ejecución Real — VPS Producción

Documento operativo para la compra, configuración y primer despliegue real de MonteAstur en un VPS Ubuntu.

---

## 1. Comprar VPS (Hetzner CX22)

| Concepto | Valor |
|----------|-------|
| Proveedor | [Hetzner Cloud](https://www.hetzner.com/cloud) |
| Plan | CX22 (2 vCPU, 4 GB RAM, 40 GB SSD) |
| Precio | ~4.50 €/mes |
| OS | Ubuntu 24.04 LTS |
| Ubicación | Nuremberg (preferida) o Helsinki |
| Datacenter | EU-NUE1 o EU-HEL1 |

### Pasos compra

1. Crear cuenta en hetzner.com — verificar email + teléfono (~24h)
2. Añadir método de pago (tarjeta o PayPal)
3. Ir a **Cloud Console** → **Projects** → **New Project**
4. **Add Server** → CX22 → Ubuntu 24.04 → Seleccionar datacenter
5. Opcional: añadir **Firewall** (permitir solo 22, 80, 443)
6. Anotar **IP pública** y **contraseña root** (se muestra una vez)
7. **Importante:** guardar en gestor de contraseñas

---

## 2. Primer acceso como root

```bash
# Desde tu máquina local
ssh root@<IP_DEL_VPS>

# Verificar versión
cat /etc/os-release

# Actualizar sistema
apt update && apt upgrade -y

# Cambiar contraseña root
passwd
```

---

## 3. Crear usuario deploy

```bash
# Añadir usuario
adduser deploy

# Añadir a sudo y docker groups
usermod -aG sudo deploy
groups deploy

# Copiar clave pública (desde local)
# En tu local:
ssh-copy-id deploy@<IP_DEL_VPS>
# O manualmente:
mkdir -p ~deploy/.ssh
chmod 700 ~deploy/.ssh
nano ~deploy/.ssh/authorized_keys  # pegar clave pública
chmod 600 ~deploy/.ssh/authorized_keys
chown -R deploy:deploy ~deploy/.ssh
```

---

## 4. Hardening SSH

```bash
# Editar /etc/ssh/sshd_config
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
AllowUsers deploy

# Reiniciar SSH
systemctl restart sshd
```

**Probar conexión en otra terminal antes de cerrar:**

```bash
ssh deploy@<IP_DEL_VPS>
```

---

## 5. Ejecutar bootstrap VPS

```bash
# Copiar script al VPS
scp scripts/vps-bootstrap.sh deploy@<IP_DEL_VPS>:~/

# Ejecutar
ssh deploy@<IP_DEL_VPS>
chmod +x ~/vps-bootstrap.sh
sudo ./vps-bootstrap.sh
```

El script instala: Docker, Docker Compose, UFW, fail2ban, crea `/opt/monteastur/` y estructura de directorios.

### Verificar

```bash
docker --version
docker compose version
sudo ufw status verbose
sudo fail2ban-client status
```

---

## 6. GitHub Secrets

En **GitHub → Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Valor |
|--------|-------|
| `VPS_HOST` | IP pública del VPS |
| `VPS_USERNAME` | `deploy` |
| `VPS_SSH_KEY` | Clave privada (cat ~/.ssh/monteastur_deploy_ed25519) |
| `VPS_PORT` | `22` |

### Generar clave SSH dedicada (local)

```bash
ssh-keygen -t ed25519 -f ~/.ssh/monteastur_deploy_ed25519 -N ""
cat ~/.ssh/monteastur_deploy_ed25519.pub  # copiar al VPS
```

---

## 7. Dominio y DNS

Ver [`DOMAIN_DNS_SSL_SETUP.md`](DOMAIN_DNS_SSL_SETUP.md).

### Resumen

| Tipo | Nombre | Valor | TTL |
|------|--------|-------|-----|
| A | `@` | IP del VPS | 300 |
| A | `www` | IP del VPS | 300 |
| A | `api` | IP del VPS | 300 |
| A | `monitor` | IP del VPS | 300 |

Esperar propagación (~5-30 min con TTL 300).

---

## 8. Configurar .env en VPS

```bash
# Copiar plantilla desde local
scp .env.production.example deploy@<IP_DEL_VPS>:/opt/monteastur/.env

# Editar en VPS
ssh deploy@<IP_DEL_VPS>
nano /opt/monteastur/.env
```

Ver [`PRODUCTION_ENV_GUIDE.md`](PRODUCTION_ENV_GUIDE.md) para guía detallada.

---

## 9. Copiar archivos al VPS

```bash
# Clone del repo
ssh deploy@<IP_DEL_VPS>
git clone https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS.git /tmp/monteastur
sudo cp -r /tmp/monteastur/* /opt/monteastur/
sudo chown -R deploy:deploy /opt/monteastur/
rm -rf /tmp/monteastur
```

O usando rsync:

```bash
# Desde local
rsync -avz --exclude '.git' --exclude 'node_modules' --exclude '.env' --exclude '.env.local' . deploy@<IP_DEL_VPS>:/opt/monteastur/
```

---

## 10. Primer docker compose up

```bash
cd /opt/monteastur

# Primera vez con DDL_AUTO=update
# Asegurar en .env: DB_DDL_AUTO=update

docker compose up -d --build
docker compose ps
```

### Verificar 6/6 containers

```
NAME                        STATUS
monteastur-app              Up
monteastur-mysql            Up (healthy)
monteastur-nginx            Up
monteastur-prometheus       Up
monteastur-grafana          Up
monteastur-uptime-kuma      Up
```

### Healthcheck

```bash
curl http://localhost:8090/actuator/health
# → {"status":"UP"}
```

### Cambiar a validate

```bash
# Editar .env: DB_DDL_AUTO=validate
docker compose up -d --build
```

---

## 11. HTTPS con Let's Encrypt

```bash
# Certbot standalone (puerto 80 libre)
sudo certbot certonly --standalone -d monteastur.com -d www.monteastur.com --email admin@monteastur.com --agree-tos --non-interactive

# Renovación automática (crontab)
sudo crontab -e
# Añadir:
0 3 * * * certbot renew --quiet && docker compose -f /opt/monteastur/docker-compose.yml exec nginx nginx -s reload
```

### Verificar SSL

```bash
curl -I https://monteastur.com
# → HTTP/2 200
# → strict-transport-security: max-age=31536000
```

---

## 12. Primer deploy con GitHub Actions

```bash
# En local: hacer commit a develop
git add .
git commit -m "chore: preparar produccion"
git push origin develop

# Ir a GitHub → Actions → Deploy to Production
# Click "Run workflow" → branch: develop
```

Esperar ~3-5 min. Verificar en Actions output.

---

## 13. Verificación final

| Test | Comando | Esperado |
|------|---------|----------|
| Web | `curl -I https://monteastur.com` | HTTP/2 200 |
| API | `curl https://api.monteastur.com/actuator/health` | `{"status":"UP"}` |
| Auth | Login admin en web | Dashboard funcional |
| Tracking | GET `/api/public/tracking/CODIGO` | Datos de tracking |
| Monitoring | `https://monitor.monteastur.com` | Grafana/Prometheus |
| PWA | Lighthouse → Installable | Sí instalable |

Smoke tests completos: [`SMOKE_TESTS_PRODUCTION.md`](SMOKE_TESTS_PRODUCTION.md)

---

## 14. Rollback real

```bash
# Si algo falla grave
cd /opt/monteastur
./scripts/rollback-prod.sh v14.0-e2e-ready

# O manual
git checkout <tag-anterior>
docker compose up -d --build
docker compose ps
curl http://localhost:8090/actuator/health
```

---

## 15. Checklist 24h post-deploy

- [ ] Monitoring activo (Prometheus recogiendo métricas)
- [ ] Grafana dashboards visibles
- [ ] Uptime Kuma monitorizando endpoints
- [ ] Logs sin errores (`docker compose logs --tail=50`)
- [ ] SSL válido (no expirado)
- [ ] Backups funcionando
- [ ] Usuarios reportan sin incidencias
- [ ] CPU/RAM/disk estables

---

## Referencias

- [`FIRST_REAL_DEPLOY_COMMANDS.md`](FIRST_REAL_DEPLOY_COMMANDS.md) — Comandos exactos por bloque
- [`PRODUCTION_ENV_GUIDE.md`](PRODUCTION_ENV_GUIDE.md) — Guía de variables de entorno
- [`PRODUCTION_VPS_RUNBOOK.md`](PRODUCTION_VPS_RUNBOOK.md) — Runbook completo
- [`FINAL_PRODUCTION_DEPLOY_CHECKLIST.md`](FINAL_PRODUCTION_DEPLOY_CHECKLIST.md) — Checklist final 7 fases
- [`SMOKE_TESTS_PRODUCTION.md`](SMOKE_TESTS_PRODUCTION.md) — 11 tests post-deploy
- [`GITHUB_SECRETS_SSH_SETUP.md`](GITHUB_SECRETS_SSH_SETUP.md) — SSH y secrets
