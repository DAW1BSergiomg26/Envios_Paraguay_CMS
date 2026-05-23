# Monteastur Envios — Guía de Despliegue en VPS

## Requisitos del VPS

| Recurso       | Mínimo       | Recomendado |
|---------------|-------------|-------------|
| CPU           | 1 vCPU      | 2 vCPU      |
| RAM           | 1 GB        | 2 GB        |
| Disco         | 10 GB       | 20 GB SSD   |
| Sistema       | Ubuntu 22.04| Ubuntu 22.04|
| Docker        | 24+         | 27+         |
| Swap          | 1 GB        | 2 GB        |

## Paso 1: Conectar al VPS

```bash
ssh root@<ip-del-vps>
```

## Paso 2: Actualizar sistema

```bash
apt update && apt upgrade -y
apt install -y ufw git curl
```

## Paso 3: Instalar Docker

```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
usermod -aG docker $USER
newgrp docker
```

Verificar instalaci\u00f3n:
```bash
docker --version
docker compose version
```

## Paso 4: Configurar firewall (UFW)

```bash
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp        # SSH
ufw allow 80/tcp        # HTTP
ufw allow 443/tcp       # HTTPS
ufw --force enable
ufw status verbose
```

## Paso 5: Clonar el repositorio

```bash
cd /opt
git clone <url-del-repositorio> monteastur
cd monteastur
```

## Paso 6: Configurar variables de entorno

```bash
cp .env.example .env
nano .env
```

**Valores obligatorios a cambiar:**
- `MYSQL_ROOT_PASSWORD` — contrase\u00f1a root de MySQL
- `MYSQL_PASSWORD` — contrase\u00f1a del usuario app
- `ADMIN_PASSWORD` — contrase\u00f1a del panel admin
- `DB_DDL_AUTO=validate`

**Valores recomendados:**
```env
PORT=8080
NGINX_PORT=80
DB_DDL_AUTO=validate
JPA_SHOW_SQL=false
THYMELEAF_CACHE=true
```

## Paso 7: Construir y levantar

```bash
docker compose build
docker compose up -d
```

## Paso 8: Verificar estado

```bash
docker ps
curl http://localhost/actuator/health
```

Debe responder: `{"status":"UP"}`

## Paso 9: Verificar logs

```bash
# App
docker logs monteastur-app -f --tail 50

# MySQL
docker logs monteastur-mysql -f --tail 20

# Nginx
docker logs monteastur-nginx -f --tail 20
```

## Actualizar deploy (nueva versi\u00f3n)

```bash
cd /opt/monteastur
git pull origin main
docker compose build app
docker compose up -d --force-recreate app
```

Si hubo cambios en BD (solo primer deploy con esquema nuevo):
```bash
# Temporalmente cambiar en .env: DB_DDL_AUTO=update
docker compose up -d --force-recreate app
# Volver a: DB_DDL_AUTO=validate
docker compose restart app
```

## Reinicio completo

```bash
# Detener todo
docker compose down

# Iniciar todo (con build)
docker compose up -d --build

# Solo reiniciar un servicio
docker compose restart app
```

## Troubleshooting r\u00e1pido

| Problema | Comando |
|----------|---------|
| App no arranca | `docker logs monteastur-app --tail 30` |
| MySQL no conecta | `docker logs monteastur-mysql` |
| Error 502 Bad Gateway | Nginx no llega a app — verificar `docker ps` |
| Error 503 | App iniciando — esperar 40s (start_period) |
| Puerto 80 ocupado | Cambiar `NGINX_PORT` en `.env` |
| Permisos uploads | `docker exec monteastur-app ls -la /app/uploads` |
