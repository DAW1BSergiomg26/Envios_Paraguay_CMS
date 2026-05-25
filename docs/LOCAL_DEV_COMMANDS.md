# Comandos de Desarrollo Local

**Proyecto:** Monteastur Envios
**Actualizado:** 2026-05-25

---

## Levantar Stack Completo

```powershell
# Desde la raíz del proyecto
docker compose up -d --build
```

Esperar ~30s para que la app esté healthy:
```powershell
# Verificar estado
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

---

## Reconstruir Stack

```powershell
# Sin borrar datos (volúmenes intactos)
docker compose down
docker compose up -d --build

# Borrando BD (fresh start con demo data)
docker compose down -v
docker compose up -d --build
```

---

## Apagar sin Borrar Datos

```powershell
docker compose down
```

Los volúmenes (`mysql_data`, `uploads_data`, `logs_data`) se conservan. Al levantar de nuevo los datos persisten.

---

## Apagar Borrando BD

```powershell
docker compose down -v
```

⚠️ **Advertencia:** Elimina todos los volúmenes (MySQL, Prometheus, Grafana, Uptime Kuma). Al arrancar de nuevo con `APP_DEMO_DATA=true` se repueblan los datos demo automáticamente.

---

## Logs

```powershell
# App Spring Boot
docker compose logs app --tail=50 -f

# Base de datos
docker compose logs db --tail=50

# Nginx
docker compose logs nginx --tail=50

# Todos los servicios
docker compose logs --tail=50
```

---

## URLs Correctas

| Servicio | URL | Puerto |
|----------|-----|--------|
| Web App (vía nginx) | `http://localhost:8090` | 8090 |
| Spring Boot (directo) | `http://localhost:8080` | 8080 |
| Prometheus | `http://localhost:9090` | 9090 |
| Grafana | `http://localhost:3001` | 3001 |
| Uptime Kuma | `http://localhost:3002` | 3002 |

---

## Credenciales Locales

| Rol | URL login | Usuario | Contraseña |
|-----|-----------|---------|------------|
| Admin | `/login` | `admin` | `admin123` |
| Cliente | `/cliente/login` | email: `cliente@monteastur.com` | `demo2026` |
| React SPA | `/login-react` | `admin` | `admin123` |
| Grafana | `http://localhost:3001` | `admin` | `admin123` |

---

## URLs del Frontend

| Ruta | Descripción |
|------|-------------|
| `/` | Home |
| `/login` | Admin login (Spring Security) |
| `/admin/dashboard` | Panel administración |
| `/admin/reservas` | Gestión reservas/envíos |
| `/admin/mensajesrecibidos` | Bandeja de mensajes |
| `/admin/imagenes` | Galería de fotos |
| `/admin/tracking` | Gestión de envíos |
| `/cliente/login` | Login cliente |
| `/cliente/panel` | Panel cliente (envíos propios) |
| `/login-react` | React SPA dashboard |
| `/tracking` | Tracking público |
| `/contacto` | Formulario de contacto |

---

## Troubleshooting Rápido

### App no responde en :8090

```powershell
# Verificar contenedores
docker compose ps

# Ver logs de nginx
docker compose logs nginx --tail=20

# Ver logs de app
docker compose logs app --tail=20
```

### Puerto 80 ocupado (IIS en Windows)

El `.env` ya tiene `NGINX_PORT=8090`. No usar `NGINX_PORT=80` en Windows con IIS activo.

### Login da 403

CSRF requerido. Hacer GET primero para obtener el token, luego POST con `_csrf=<token>`.
O usar un navegador normal.

### Base de datos vacía

```powershell
# Recrear volúmenes y repoblar
docker compose down -v
docker compose up -d --build
```

Verificar `APP_DEMO_DATA=true` en `.env`.

### Error "port is already allocated"

```powershell
# Buscar qué está usando el puerto
netstat -ano | findstr :PORT

# Liberar el puerto si es necesario
# (identificar PID y terminar proceso)
```

### Frontend no actualizado

```powershell
# Reconstruir solo la app (incluye frontend build)
docker compose up -d --build app
```

### Tests fallan localmente

```powershell
# Backend (requiere MySQL en localhost:3306)
mvn test

# Frontend (no requiere stack)
cd frontend-react
npm run test:unit
```
