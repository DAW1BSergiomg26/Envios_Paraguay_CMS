# Comandos Exactos — Primer Deploy Real

Todos los comandos necesarios para el primer despliegue real de MonteAstur en VPS, agrupados por bloque.

> **IMPORTANTE:** Reemplazar `<IP_DEL_VPS>` y `<DOMINIO>` antes de ejecutar.

---

## A) Local — Preparación

```bash
# 1. Ver que estás en la rama correcta
git branch
# → develop (o feature/...)

# 2. Últimos cambios
git pull origin develop

# 3. Generar clave SSH dedicada para GitHub Actions
ssh-keygen -t ed25519 -f ~/.ssh/monteastur_deploy_ed25519 -N ""

# 4. Ver clave pública (la necesitas para el VPS)
cat ~/.ssh/monteastur_deploy_ed25519.pub
```

---

## B) VPS — Configuración inicial (como root)

```bash
# 1. SSH al VPS como root
ssh root@<IP_DEL_VPS>

# 2. Actualizar sistema
apt update && apt upgrade -y

# 3. Crear usuario deploy
adduser deploy
usermod -aG sudo deploy

# 4. Configurar SSH key para deploy
mkdir -p ~deploy/.ssh
echo "<CLAVE_PUBLICA_DE_LA_PASO_A4>" >> ~deploy/.ssh/authorized_keys
chmod 700 ~deploy/.ssh
chmod 600 ~deploy/.ssh/authorized_keys
chown -R deploy:deploy ~deploy/.ssh

# 5. Hardening SSH
sed -i 's/^PermitRootLogin yes/PermitRootLogin no/' /etc/ssh/sshd_config
sed -i 's/^#PasswordAuthentication yes/PasswordAuthentication no/' /etc/ssh/sshd_config
sed -i 's/^PasswordAuthentication yes/PasswordAuthentication no/' /etc/ssh/sshd_config
echo "AllowUsers deploy" >> /etc/ssh/sshd_config
systemctl restart sshd

# 6. Salir y probar conexión
exit

# 7. Probar (desde local)
ssh deploy@<IP_DEL_VPS>
```

---

## C) VPS — Bootstrap + Deploy (como deploy)

```bash
# 1. Copiar script bootstrap desde local
# (desde otra terminal local)
scp scripts/vps-bootstrap.sh deploy@<IP_DEL_VPS>:~/

# 2. Ejecutar bootstrap (ya como deploy)
chmod +x ~/vps-bootstrap.sh
sudo ~/vps-bootstrap.sh

# 3. Verificar instalaciones
docker --version
docker compose version
sudo ufw status verbose
sudo fail2ban-client status

# 4. Clonar repositorio
cd /opt
sudo git clone https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS.git monteastur
sudo chown -R deploy:deploy monteastur
cd monteastur

# 5. Copiar .env y editarlo
cp .env.production.example .env
nano .env
# → CAMBIAR todos los CHANGE_ME
# → DB_DDL_AUTO=update (primera vez)

# 6. Proteger .env
chmod 600 .env

# 7. Verificar sintaxis docker compose
docker compose config

# 8. Primer build y up
docker compose up -d --build

# 9. Verificar containers
docker compose ps
# → 6/6 UP

# 10. Healthcheck
curl http://localhost:8090/actuator/health

# 11. Ver logs
docker compose logs --tail=20 app
docker compose logs --tail=20 mysql
```

---

## D) GitHub Secrets — Configurar

```bash
# Desde local, obtener clave privada
cat ~/.ssh/monteastur_deploy_ed25519
```

En GitHub:
1. Ir al repositorio → Settings → Secrets and variables → Actions
2. Añadir 4 secrets:
   - `VPS_HOST` = `<IP_DEL_VPS>`
   - `VPS_USERNAME` = `deploy`
   - `VPS_SSH_KEY` = contenido de `monteastur_deploy_ed25519` (texto completo)
   - `VPS_PORT` = `22`

---

## E) DNS — Configurar

En el panel de tu proveedor DNS:

| Tipo | Nombre | Valor | TTL |
|------|--------|-------|-----|
| A | `@` | `<IP_DEL_VPS>` | 300 |
| A | `www` | `<IP_DEL_VPS>` | 300 |
| A | `api` | `<IP_DEL_VPS>` | 300 |
| A | `monitor` | `<IP_DEL_VPS>` | 300 |

**Esperar propagación** (~5-30 min):

```bash
# Verificar
ping <DOMINIO>
curl -I http://<DOMINIO>
```

---

## F) HTTPS — Let's Encrypt

```bash
# 1. Asegurar que el dominio apunta al VPS (paso E)
# 2. Puerto 80 libre (detener temporalmente nginx si es necesario)
ssh deploy@<IP_DEL_VPS>
sudo systemctl stop nginx  # si existe

# 3. Obtener certificado
sudo certbot certonly --standalone \
  -d <DOMINIO> \
  -d www.<DOMINIO> \
  --email admin@<DOMINIO> \
  --agree-tos --non-interaction

# 4. Verificar certificados
sudo certbot certificates

# 5. Añadir renovación automática
sudo crontab -e
# Añadir línea:
0 3 * * * certbot renew --quiet && docker compose -f /opt/monteastur/docker-compose.yml exec nginx nginx -s reload

# 6. Verificar HTTPS
curl -I https://<DOMINIO>
# → HTTP/2 200
# → strict-transport-security
```

---

## G) Smoke Tests — Verificar

```bash
# 1. Healthcheck API
curl -f https://api.<DOMINIO>/actuator/health
# → {"status":"UP"}

# 2. Home page
curl -I https://<DOMINIO>
# → HTTP/2 200

# 3. Login admin
curl -f -c cookies.txt -b cookies.txt \
  -X POST https://<DOMINIO>/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=<ADMIN_PASSWORD>"
# → HTTP 302 redirect a /admin/dashboard

# 4. Tracking público
curl -f "https://<DOMINIO>/api/public/tracking/TEST123"
# → JSON con datos de tracking

# 5. Monitoring
curl -f https://monitor.<DOMINIO>/api/health
# → OK

# 6. SSL
# Abrir https://www.ssllabs.com/ssltest/analyze.html?d=<DOMINIO>
```

Smoke tests completos con prioridades: [`SMOKE_TESTS_PRODUCTION.md`](SMOKE_TESTS_PRODUCTION.md)

---

## H) Rollback — Si algo falla

```bash
# Opción 1: Script de rollback
cd /opt/monteastur
./scripts/rollback-prod.sh v14.0-e2e-ready

# Opción 2: Manual
cd /opt/monteastur
git checkout <tag-anterior>
docker compose down
docker compose up -d --build
docker compose ps
curl http://localhost:8090/actuator/health

# Opción 3: Restaurar backup MySQL
docker compose exec -T mysql mysql -uroot -p<ROOT_PASSWORD> casarural < backup.sql

# Opción 4: Full reset
docker compose down -v  # OJO: borra volúmenes (datos incluidos)
docker compose up -d --build
```

---

## Resumen de comandos útiles

```bash
# Estado
docker compose ps                    # Estado containers
docker compose logs --tail=30 app    # Últimos logs
curl localhost:8090/actuator/health  # Healthcheck
df -h                                # Disco
free -h                              # RAM
htop                                 # CPU

# Docker
docker compose up -d --build         # Rebuild y up
docker compose down                  # Parar
docker compose pull                  # Actualizar imágenes
docker system prune -f               # Limpiar (cuidado)

# SSL
sudo certbot certificates            # Estado certificados
sudo certbot renew --dry-run         # Probar renovación

# Logs
journalctl -u sshd -n 20             # Logs SSH
sudo fail2ban-client status sshd     # Estado fail2ban
sudo ufw status verbose              # Estado firewall
```
