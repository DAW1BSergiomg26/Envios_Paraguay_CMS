# Production Secrets Template — MonteAstur Envios

> **IMPORTANTE**: Este documento es una plantilla. NO contiene valores reales.
> Los valores reales deben generarse con `openssl rand -base64 32` y guardarse en un gestor de contraseñas.
> NUNCA subir este archivo con valores reales a Git.

---

## Secretos de aplicación (`.env` en el VPS)

Estos secretos van en el archivo `/opt/monteastur/.env` del VPS.

| Variable | Valor (placeholder) | Generado | Guardado en |
|----------|---------------------|----------|-------------|
| `ADMIN_USERNAME` | `CHANGE_ME_ADMIN` | Manual | Gestor contraseñas |
| `ADMIN_PASSWORD` | `CHANGE_ME_ADMIN_PASSWORD` | `openssl rand -base64 32` | Gestor contraseñas |
| `MYSQL_ROOT_PASSWORD` | `CHANGE_ME_ROOT_PASSWORD` | `openssl rand -base64 32` | Gestor contraseñas |
| `MYSQL_USER` | `casarural_app` | Manual | Gestor contraseñas |
| `MYSQL_PASSWORD` | `CHANGE_ME_DB_PASSWORD` | `openssl rand -base64 32` | Gestor contraseñas |
| `DB_USERNAME` | = `MYSQL_USER` | — | — |
| `DB_PASSWORD` | = `MYSQL_PASSWORD` | — | — |
| `GRAFANA_ADMIN_USER` | `admin` | Manual | Gestor contraseñas |
| `GRAFANA_ADMIN_PASSWORD` | `CHANGE_ME_GRAFANA_PASSWORD` | `openssl rand -base64 32` | Gestor contraseñas |
| `SSL_EMAIL` | `admin@monteastur.com` | Manual | — |

### Comandos para generar

```bash
# Generar todos los secretos de una vez
echo "ADMIN_PASSWORD=$(openssl rand -base64 32)"
echo "MYSQL_ROOT_PASSWORD=$(openssl rand -base64 32)"
echo "MYSQL_PASSWORD=$(openssl rand -base64 32)"
echo "GRAFANA_ADMIN_PASSWORD=$(openssl rand -base64 32)"
```

---

## Secretos de GitHub Actions

Estos secretos van en GitHub → **Settings → Secrets and variables → Actions**.

| Secret | Valor (placeholder) | Dónde obtenerlo |
|--------|---------------------|-----------------|
| `VPS_HOST` | `203.0.113.10` | Dashboard del proveedor VPS |
| `VPS_USER` | `deploy` | Creado durante bootstrap |
| `VPS_SSH_KEY` | `-----BEGIN OPENSSH PRIVATE KEY-----\n...` | `cat ~/.ssh/github-actions-monteastur` |
| `VPS_PORT` | `22` | Puerto SSH estándar |

### Generar clave SSH para GitHub Actions

```bash
ssh-keygen -t ed25519 -C "github-actions@monteastur" -f ~/.ssh/github-actions-monteastur

# Mostrar clave pública (para añadir al VPS)
cat ~/.ssh/github-actions-monteastur.pub

# Mostrar clave privada (para GitHub Secrets)
cat ~/.ssh/github-actions-monteastur

# Copiar al VPS
ssh-copy-id -i ~/.ssh/github-actions-monteastur.pub deploy@<VPS_IP>
```

---

## Secretos que NO deben estar en ningún archivo

| Secreto | Exposición | Riesgo |
|---------|-----------|--------|
| Contraseñas de producción | En logs, dump de BD, capturas | Crítico |
| Clave privada SSH | En repos, emails, chats | Crítico |
| API keys de terceros | En frontend, logs | Alto |
| Certificados SSL privados | En repos públicos | Alto |

### Buenas prácticas

1. **Rotación**: Cambiar contraseñas cada 90 días
2. **Mínimo privilegio**: Cada servicio solo tiene acceso a lo que necesita
3. **No compartir**: Cada desarrollador tiene su propia clave SSH
4. **No loggear**: `logging.level.com.monteastur.envios=INFO`, no DEBUG
5. **No hardcodear**: Siempre usar variables de entorno
6. **No commitear**: `.env` en `.gitignore`, verificar antes de push

---

## Verificación post-deploy

```bash
# Verificar que ningún secreto está en el repositorio
cd /opt/monteastur
git grep -i "password\|secret\|CHANGE_ME" -- :!.env :!.gitignore

# Verificar que .env no es world-readable
ls -la .env   # Debe ser -rw------- (600) o similar

# Verificar que logs no contienen credenciales
grep -i "password\|credentials" logs/*.log 2>/dev/null || echo "No secrets in logs (OK)"
```

---

## Plantilla .env final (producción)

```bash
# =============================================================================
# MonteAstur Envios — .env de producción
# GENERAR CON: openssl rand -base64 32 para cada CHANGE_ME
# =============================================================================

SPRING_PROFILES_ACTIVE=prod

# Primera vez: DB_DDL_AUTO=update, luego validate
DB_DDL_AUTO=validate
APP_DEMO_DATA=false

ADMIN_USERNAME=CHANGE_ME
ADMIN_PASSWORD=CHANGE_ME

PORT=8080

MYSQL_ROOT_PASSWORD=CHANGE_ME
MYSQL_DATABASE=casarural
MYSQL_USER=casarural_app
MYSQL_PASSWORD=CHANGE_ME

SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/casarural?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=convertToNull
DB_USERNAME=casarural_app
DB_PASSWORD=CHANGE_ME

NGINX_PORT=80

SSL_EMAIL=admin@monteastur.com

GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=CHANGE_ME
PROMETHEUS_PORT=9090
GRAFANA_PORT=3000
UPTIME_KUMA_PORT=3001

TZ=America/Asuncion

UPLOAD_DIR=/app/uploads
LOG_DIR=/app/logs
BACKUP_PATH=./backup
```