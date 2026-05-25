# VPS Real — Próximos pasos

Pasos concretos para el primer despliegue real en VPS.

> **Pre-requisito**: Haber completado la release v20 y tener el tag `v20.0-pre-deploy` creado.

---

## Paso 1: Comprar VPS

| Proveedor | Plan | vCPU | RAM | SSD | Precio |
|-----------|------|------|-----|-----|--------|
| **Hetzner** | CX22 | 2 | 4 GB | 40 GB | ~€4.50/mes |

```bash
# Crear servidor Ubuntu 24.04 LTS
# Anotar: IP del VPS, contraseña root (temporal)
```

Ver `docs/HETZNER_VPS_PURCHASE_GUIDE.md` para guía completa.

---

## Paso 2: Comprar dominio

| Proveedor | Precio | Notas |
|-----------|--------|-------|
| **Cloudflare Registrar** | ~€9.15/año | WHOIS privado incluido, DNS Anycast gratuito |
| Namecheap | ~€10/año | Alternativa |

```bash
# Elegir dominio: monteastur.com (recomendado)
# Configurar nameservers de Cloudflare si aplica
```

Ver `docs/DOMAIN_PURCHASE_GUIDE.md` para guía completa.

---

## Paso 3: Crear DNS

```bash
# Registro A: dominio → IP del VPS
monteastur.com  A    <VPS_IP>   TTL: 300

# Opcional: www subdomain
www  A  <VPS_IP>  TTL: 300

# Opcional: monitoring subdominios
grafana  A  <VPS_IP>  TTL: 300
uptime   A  <VPS_IP>  TTL: 300
```

Verificar propagación:
```bash
dig +short monteastur.com
dig @8.8.8.8 monteastur.com
```

---

## Paso 4: Crear usuario deploy

```bash
# Acceder como root
ssh root@<VPS_IP>

# Crear usuario
adduser deploy
usermod -aG sudo deploy

# Configurar SSH key
su - deploy
mkdir -p ~/.ssh && chmod 700 ~/.ssh
# Copiar clave pública de tu máquina local
nano ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# Deshabilitar login root por SSH (opcional, post-deploy)
sudo sed -i 's/PermitRootLogin yes/PermitRootLogin prohibit-password/' /etc/ssh/sshd_config
sudo systemctl restart sshd
```

---

## Paso 5: Ejecutar bootstrap

```bash
# Como root o con sudo
sudo ./scripts/vps-bootstrap.sh
```

Esto instala:
- Docker + Docker Compose
- Crea directorios (`/opt/monteastur`)
- Configura UFW (puertos 22, 80, 443)
- fail2ban (opcional, documentado en VPS_HARDENING_CHECKLIST.md)
- Clona el repositorio

---

## Paso 6: Crear .env real

```bash
cd /opt/monteastur
cp .env.production.example .env
nano .env
```

Generar contraseñas seguras:
```bash
openssl rand -base64 32   # Repetir para cada secreto
```

Editar TODOS los `CHANGE_ME`:
- `ADMIN_USERNAME` / `ADMIN_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_USER` / `MYSQL_PASSWORD`
- `DB_USERNAME` / `DB_PASSWORD`
- `GRAFANA_ADMIN_PASSWORD`
- `SSL_EMAIL`

Configurar `DB_DDL_AUTO=update` SOLO para el primer arranque.

---

## Paso 7: Levantar Docker

```bash
cd /opt/monteastur

# Primer arranque (con DDL_AUTO=update)
docker compose up -d --build

# Verificar estado
docker compose ps
# Esperar ~60s y verificar health
curl -f http://localhost/actuator/health
```

Si healthcheck pasa:

```bash
# Cambiar a validate y reiniciar
nano .env   # DB_DDL_AUTO=validate
docker compose up -d
```

---

## Paso 8: Activar SSL (Let's Encrypt)

```bash
# Obtener certificado (requiere DNS propagado)
docker compose --profile certbot run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d monteastur.com -d www.monteastur.com \
  --email admin@monteastur.com \
  --agree-tos --no-eff-email

# Copiar certificados
sudo cp /etc/letsencrypt/live/monteastur.com/fullchain.pem nginx/ssl/
sudo cp /etc/letsencrypt/live/monteastur.com/privkey.pem nginx/ssl/

# Activar HTTPS
cp nginx/examples/production-example.conf nginx/conf.d/monteastur-prod.conf
# Editar server_name si es necesario
nano nginx/conf.d/monteastur-prod.conf

# Recargar nginx
docker compose restart nginx

# Verificar
curl -I https://monteastur.com
```

Configurar renovación automática:
```bash
crontab -e
# Añadir:
0 3 * * * cd /opt/monteastur && docker compose --profile certbot run --rm certbot renew && docker compose restart nginx
```

---

## Paso 9: Configurar GitHub Secrets

En GitHub → **Settings → Secrets and variables → Actions**:

| Secret | Valor | Ejemplo |
|--------|-------|---------|
| `VPS_HOST` | IP del VPS | `203.0.113.10` |
| `VPS_USER` | Usuario SSH | `deploy` |
| `VPS_SSH_KEY` | Clave privada SSH (multilínea) | `-----BEGIN OPENSSH PRIVATE KEY-----` |
| `VPS_PORT` | Puerto SSH | `22` |

Generar clave SSH dedicada:
```bash
ssh-keygen -t ed25519 -C "github-actions@monteastur" -f ~/.ssh/github-actions-monteastur
ssh-copy-id -i ~/.ssh/github-actions-monteastur.pub deploy@<VPS_IP>
```

Ver `docs/GITHUB_SECRETS_SSH_SETUP.md` para guía completa.

---

## Paso 10: Ejecutar deploy-prod manual

```bash
# Desde local
ssh deploy@<VPS_IP>
cd /opt/monteastur
git checkout main
git pull origin main

# O desde GitHub Actions:
# GitHub → Actions → Deploy Production → Run workflow
```

```bash
# Verificar post-deploy
./scripts/production-smoke-test.sh
```

---

## Paso 11: Smoke tests

Ejecutar smoke tests post-deploy:
```bash
BASE_URL=https://monteastur.com ./scripts/production-smoke-test.sh
```

Verificar manualmente:
- [ ] Healthcheck: `curl -f https://monteastur.com/actuator/health`
- [ ] Home: `curl -I https://monteastur.com` (200 OK, security headers)
- [ ] Login: `https://monteastur.com/login` (formulario visible)
- [ ] Tracking: `https://monteastur.com/tracking` (con demo data o con datos reales)
- [ ] Login React: `https://monteastur.com/login-react` (SPA carga)
- [ ] Prometheus: `https://monteastur.com:9090/targets`
- [ ] Grafana: `https://monteastur.com:3000` (login admin)
- [ ] Uptime Kuma: `https://monteastur.com:3001`

---

## Paso 12: Rollback test

Verificar que el rollback funciona:
```bash
# Identificar tag actual
git tag -l

# Simular rollback
cd /opt/monteastur && ./scripts/rollback-prod.sh v20.0-pre-deploy

# Verificar que el healthcheck sigue pasando
curl -f http://localhost/actuator/health
```

---

## Paso 13: Monitoring 24h

Monitorizar durante las primeras 24 horas:
- [ ] Prometheus targets: todos UP
- [ ] Grafana dashboard: sin gaps en métricas
- [ ] Uptime Kuma: 100% uptime
- [ ] Logs de app: sin errores WARN+
- [ ] Logs de nginx: sin 5xx
- [ ] Logs de MySQL: sin conexiones fallidas
- [ ] Backups automáticos ejecutados correctamente

---

## Post-deploy checklist (primeras 24h)

- [ ] SSL Labs: https://www.ssllabs.com/ssltest/analyze.html?d=monteastur.com
- [ ] Google PageSpeed Insights: rendimiento
- [ ] Lighthouse audit: PWA, accesibilidad
- [ ] fail2ban: verificar logs `sudo fail2ban-client status sshd`
- [ ] UFW: `sudo ufw status verbose`
- [ ] Docker: `docker stats` — memoria/CPU estable
- [ ] Backups: verificar que los archivos existen en `backup/`
- [ ] Rollback: procedimiento claro y documentado
- [ ] GitHub Actions: verificar que los workflows se ejecutan correctamente

---

## Referencias

| Documento | Enlace |
|-----------|--------|
| Runbook producción | `docs/PRODUCTION_VPS_RUNBOOK.md` |
| Hardening VPS | `docs/VPS_HARDENING_CHECKLIST.md` |
| Deploy master checklist | `docs/FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md` |
| GitHub Secrets SSH | `docs/GITHUB_SECRETS_SSH_SETUP.md` |
| DNS + SSL setup | `docs/DOMAIN_DNS_SSL_SETUP.md` |
| Release v20 | `docs/RELEASE_V20_READY.md` |
| Pre-deploy check scripts | `scripts/predeploy-check.ps1` / `.sh` |
| Smoke tests | `scripts/production-smoke-test.sh` |
| Deploy prod | `scripts/deploy-prod.sh` |
| Rollback | `scripts/rollback-prod.sh` |