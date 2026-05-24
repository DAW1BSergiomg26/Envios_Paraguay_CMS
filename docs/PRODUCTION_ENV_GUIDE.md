# Guía de Entorno — Variables de Producción

Cómo configurar el archivo `.env` en el VPS para el despliegue real de MonteAstur.

---

## 1. Copiar plantilla al VPS

```bash
# Desde local
scp .env.production.example deploy@<IP_DEL_VPS>:/opt/monteastur/.env
```

```bash
# Desde el VPS (alternativa)
ssh deploy@<IP_DEL_VPS>
cp /opt/monteastur/.env.production.example /opt/monteastur/.env
```

---

## 2. Editar cada variable

```bash
ssh deploy@<IP_DEL_VPS>
nano /opt/monteastur/.env
```

### Variables a cambiar (marcadas CHANGE_ME)

| Variable | Generar con | Ejemplo |
|----------|-------------|---------|
| `ADMIN_USERNAME` | manual | `admin` |
| `ADMIN_PASSWORD` | `openssl rand -base64 24` | `xK8mPq3R...` |
| `MYSQL_ROOT_PASSWORD` | `openssl rand -base64 32` | `aB3dFgHj...` |
| `MYSQL_PASSWORD` | `openssl rand -base64 24` | `mN5pQsT2...` |
| `SSL_EMAIL` | manual | `admin@tudominio.com` |
| `GRAFANA_ADMIN_PASSWORD` | `openssl rand -base64 20` | `zX7cVbN4...` |

### Variables que pueden quedar por defecto

| Variable | Valor típico | Nota |
|----------|-------------|------|
| `MYSQL_DATABASE` | `casarural` | Solo cambiar si se prefiere otro nombre |
| `MYSQL_USER` | `casarural_user` | Asociado a la DB |
| `SPRING_PROFILES_ACTIVE` | `prod` | No cambiar |
| `TZ` | `Europe/Madrid` | España peninsular |
| `NGINX_PORT` | `80` | Solo HTTP (HTTPS en certbot) |
| `UPLOAD_DIR` | `/app/uploads` | Ruta dentro del container |
| `LOG_DIR` | `/app/logs` | Ruta dentro del container |

---

## 3. Generar contraseñas seguras

```bash
# En tu local o en el VPS

# 32 caracteres (recomendado para root)
openssl rand -base64 32

# 24 caracteres (recomendado para app/admin)
openssl rand -base64 24

# 20 caracteres (mínimo para Grafana)
openssl rand -base64 20
```

**Guardar todas las contraseñas en un gestor** (Bitwarden, 1Password, KeePass).

---

## 4. Qué NO subir a Git

Nunca committear estos archivos:

```
.env
.env.local
.env.production
.env.development
*.key
*.pem
**/secrets/**
```

Confirmar que `.gitignore` los excluye:

```bash
grep -n ".env" .gitignore
# Debe aparecer:
# .env
# .env.*
```

---

## 5. DDL_AUTO: update → validate

### Primera vez (schema vacío)

```env
DB_DDL_AUTO=update
```

```bash
cd /opt/monteastur
docker compose up -d --build
# Hibernate crea las tablas automáticamente
```

### Producción normal (después del primer deploy exitoso)

```env
DB_DDL_AUTO=validate
```

```bash
cd /opt/monteastur
docker compose up -d --build
# Hibernate solo valida que coincide
```

> **Riesgo:** Si cambias entidades Java, `validate` fallará. En ese caso:
> 1. Actualizar schema manualmente (migración SQL)
> 2. O temporalmente `update` con precaución

---

## 6. Validar configuración Docker

```bash
cd /opt/monteastur

# Validar docker-compose.yml sintaxis
docker compose config

# Ver variables del .env
grep -v "^#" .env | grep -v "^$"
```

---

## 7. Variables sensibles y seguridad

| Práctica | ✅ Correcto | ❌ Incorrecto |
|----------|-------------|---------------|
| Almacenamiento | Gestor de contraseñas | Archivo de texto plano |
| Git | Ignorado en `.gitignore` | Committeado |
| Permisos `.env` | `chmod 600 .env` | `chmod 644 .env` |
| Rotación | Cada 90 días o ante breach | Nunca cambiar |
| Compartición | Solo admin del sistema | Equipo completo |

---

## 8. Permisos del archivo .env

```bash
chmod 600 /opt/monteastur/.env
chown deploy:deploy /opt/monteastur/.env

# Verificar
ls -la /opt/monteastur/.env
# -rw------- 1 deploy deploy 1234 May 25 12:00 .env
```

---

## 9. Ejemplo .env completo (producción)

```env
SPRING_PROFILES_ACTIVE=prod
DB_DDL_AUTO=validate
ADMIN_USERNAME=admin
ADMIN_PASSWORD=xK8mPq3R...
MYSQL_ROOT_PASSWORD=aB3dFgHj...
MYSQL_DATABASE=casarural
MYSQL_USER=casarural_user
MYSQL_PASSWORD=mN5pQsT2...
NGINX_PORT=80
SSL_EMAIL=admin@tudominio.com
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=zX7cVbN4...
PROMETHEUS_PORT=9090
GRAFANA_PORT=3000
UPTIME_KUMA_PORT=3001
TZ=Europe/Madrid
UPLOAD_DIR=/app/uploads
LOG_DIR=/app/logs
```

> **Los valores `...` son marcadores. Reemplazar con contraseñas reales generadas con `openssl`.**
