# MONTEASTUR ENVIOS
Plataforma logística España ↔ Paraguay.

[![CI](https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS/actions/workflows/ci.yml/badge.svg)](https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-%23ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-%236DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-%2361DAFB?logo=react&logoColor=white)](https://react.dev/)
[![Docker](https://img.shields.io/badge/Docker-%232496ED?logo=docker&logoColor=white)](https://docker.com/)

Plataforma web profesional para la gestión de envíos internacionales entre España y Paraguay, con:
- Tracking de envíos en tiempo real
- Panel de administración completo
- Panel de cliente seguro
- Gestión de evidencias y documentos
- Galería CMS para operaciones
- Seguimiento premium con timeline visual

## Tecnologías usadas

- **Java 17** - Lenguaje de programación
- **Spring Boot 3.3.5** - Framework backend
- **Thymeleaf** - Motor de plantillas MVC
- **Spring Security** - Autenticación y autorización
- **MySQL 8** - Base de datos relacional
- **Docker** - Contenerización
- **Docker Compose** - Orquestación de múltiples contenedores
- **Actuator** - Monitoreo y healthchecks
- **Logback** - Sistema de logging profesional
- **Maven 3.9+** - Gestión de dependencias y build
- **CSS modular** - Estilos organizados por funcionalidad
- **React 19** - Dashboard moderno SPA para administración
- **Vite 8** - Bundler y dev server del frontend React
- **Axios** - Cliente HTTP para el SPA React
- **Recharts** - Librería de gráficos para analytics dashboard
- **Nginx** - Reverse proxy, SSL termination, compression
- **Let's Encrypt / Certbot** - Certificados SSL automáticos
- **PWA** - Service Worker, manifest, push notifications, offline mode

## Arquitectura

La aplicación sigue una arquitectura **MVC (Modelo-Vista-Controlador)** claramente separada:

- **Controllers**: Manejan las peticiones HTTP y retornan vistas o datos
- **Services**: Contienen la lógica de negocio y transacciones
- **Repositories**: Interfaz con la capa de persistencia (Spring Data JPA)
- **Templates**: Archivos Thymeleaf en `src/main/resources/templates`
- **Static resources**: CSS, JS e imágenes en `src/main/resources/static`
- **Uploads**: Almacén de archivos subidos (imágenes de tracking, galería, etc.)
- **Logs**: Archivos de registro de la aplicación
- **Configuración externa**: Variables de entorno y archivos `.properties`

### Diagrama de arquitectura

```
Navegador Web (usuario)
       │
       ▼
┌──────────────────────────────────────────────────┐
│                  Nginx (proxy)                    │
│  :80 (HTTP) / :443 (HTTPS)                       │
│  ─ Security headers (HSTS, CSP, XFO)             │
│  ─ Gzip compression                              │
│  ─ Reverse proxy a Spring Boot                   │
└──────────────────────┬───────────────────────────┘
                       │ proxy_pass http://app:8080
                       ▼
┌──────────────────────────────────────────────────┐
│           Spring Boot (Tomcat)                   │
│  ─ Thymeleaf MVC templates                       │
│  ─ React SPA dashboard (/react-dashboard)        │
│  ─ REST API (/api/v1/)                           │
│  ─ Spring Security + Session-based auth          │
│  ─ Health / Actuator                             │
└──────┬──────────────────────────────┬────────────┘
       │                              │
       ▼                              ▼
┌──────────────┐           ┌──────────────────┐
│   MySQL 8    │           │  Uploads (vol)    │
│  base datos  │           │  /app/uploads     │
│  persistente │           │  evidencias, etc  │
└──────────────┘           └──────────────────┘
       │                              │
       ▼                              ▼
┌──────────────┐           ┌──────────────────┐
│ mysql_data   │           │  uploads_data    │
│ (vol Docker) │           │  (vol Docker)    │
└──────────────┘           └──────────────────┘
```

## Estructura del proyecto

```
src/main/
├── java/
│   └── com/grupb2/casarural/
│       ├── controller/     # Controladores MVC
│       ├── model/          # Entidades JPA
│       ├── repository/     # Repositorios Spring Data
│       ├── service/        # Lógica de negocio
│       └── config/         # Configuración (Security, WebMvc, etc.)
├── resources/
│   ├── templates/          # Archivos Thymeleaf (.html)
│   │   ├── cms/            # Panel administrativo
│   │   └── en/             # Versiones en inglés
│   └── static/
│       ├── css/            # Hojas de estilo modulares
│       ├── js/             # JavaScript
│       └── img/            # Imágenes estáticas
uploads/                    # Almacén de archivos subidos (NO se sube a Git)
logs/                       # Archivos de log generados en tiempo de ejecución
docker-compose.yml          # Orquestación de servicios
Dockerfile                  # Definición de la imagen de la aplicación
nginx/                      # Configuración Nginx reverse proxy
scripts/                    # Scripts de backup y restore
docs/                       # Guías de producción y despliegue
backup/                     # Backups de BD y uploads
```

## Docker producción

### Contenedores

| Contenedor          | Imagen                 | Puerto expuesto     | Función                          |
|---------------------|------------------------|---------------------|----------------------------------|
| `monteastur-nginx`  | `nginx:alpine`        | 80 / 443            | Reverse proxy, SSL, compression  |
| `monteastur-app`    | `monteastur-app`      | 8080 (interno)      | Spring Boot + React SPA          |
| `monteastur-mysql`  | `mysql:8.0`           | — (interno)         | Base de datos                    |

### Volúmenes persistentes

| Volumen                   | Mount point          | Contenido                  |
|---------------------------|----------------------|----------------------------|
| `mysql_data`              | `/var/lib/mysql`     | Datos de MySQL             |
| `uploads_data`            | `/app/uploads`       | Imágenes subidas           |
| `logs_data`               | `/app/logs`          | Logs de la aplicación      |
| `certbot_www`             | `/var/www/certbot`   | Desafíos Let's Encrypt     |

### Nginx Reverse Proxy

Nginx actúa como puerta de entrada única, añadiendo:

- **Terminación SSL** (cuando se configura HTTPS)
- **Security headers**: HSTS, CSP, X-Frame-Options, Permissions-Policy
- **Compresión gzip** de assets estáticos
- **Proxy pass** a Spring Boot en `http://app:8080`
- **Límite de tamaño** de subida: 10MB
- **Preparado para WebSocket** (futuro)

Configuración en `nginx/conf.d/monteastur.conf`.

## Requisitos

- **Java 17+** (se recomienda JDK 17 LTS)
- **Maven 3.9+** (para build y gestión de dependencias)
- **Docker Desktop** (para contenedores)
- **MySQL 8** (base de datos, accesible vía localhost:3307 o servicio Docker)
- **Git** (control de versiones)

## Arranque local

### Paso 1: Arrancar MySQL Docker

Si ya tiene el contenedor MySQL creado desde fases anteriores:
```powershell
docker start monteastur-mysql
```

Si necesita crear uno nuevo:
```powershell
docker run -d --name monteastur-mysql -p 3307:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=casarural mysql:8.0
```

### Paso 2: Configurar variables de entorno (PowerShell)

```powershell
$env:PORT="8895"
$env:DB_DDL_AUTO="update"
$env:JPA_SHOW_SQL="true"
$env:UPLOAD_DIR="./uploads"
$env:ADMIN_USERNAME="admin"
$env:ADMIN_PASSWORD="admin123"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3307/casarural?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="root"
```

### Paso 3: Ejecutar la aplicación

```powershell
mvn spring-boot:run
```

La aplicación estará disponible en: http://localhost:8895

## Arranque con Docker Compose

### Paso 1: Preparar variables de entorno

```powershell
cp .env.example .env
```

Luego edite el archivo `.env` con sus credenciales de producción (especialmente cambie las contraseñas por defecto).

### Paso 2: Construir y levantar los contenedores

```powershell
docker compose up -d --build
```

La aplicación estará disponible en el puerto definido en la variable `PORT` (por defecto 8080 en producción).

## Variables importantes

| Variable | Descripción | Valor por defecto (dev) | Comentario |
|----------|-------------|-------------------------|------------|
| PORT | Puerto del servidor HTTP | 8081 | En producción suele ser 8080 |
| DB_DDL_AUTO | Estrategia de actualización de schema | update | En producción usar `validate` |
| JPA_SHOW_SQL | Mostrar SQL en consola | true | En producción usar `false` |
| UPLOAD_DIR | Directorio para archivos subidos | ./uploads | En producción: `/app/uploads` (volumen Docker) |
| ADMIN_USERNAME | Usuario de acceso al panel admin | admin | Cambiar en producción |
| ADMIN_PASSWORD | Contraseña de acceso al panel admin | admin123 | **Obligatorio cambiar en producción** |
| DB_USERNAME | Usuario de MySQL | root |  |
| DB_PASSWORD | Contraseña de MySQL | (vacía) |  |
| SPRING_PROFILES_ACTIVE | Perfil de Spring activo | (vacío) | En producción: `prod` |
| LOG_DIR | Directorio para archivos de log | ./logs |  |
| NGINX_PORT | Puerto del proxy Nginx | 80 | Requiere sudo en Linux si < 1024 |

## URLs importantes

### Frontend (público)
| Ruta | Descripción |
|------|-------------|
| `/` | Página de inicio |
| `/seguimiento` | Seguimiento de envíos por código |
| `/reservas` | Formulario y gestión de envíos |
| `/contacto` | Formulario de contacto |

### Panel de Admin
| Ruta | Descripción |
|------|-------------|
| `/login` | Inicio de sesión (admin y cliente) |
| `/admin/dashboard` | Panel principal de administración |
| `/admin/tracking` | Gestión de envíos y tracking |
| `/admin/imagenes` | Galería y gestión de imágenes del CMS |

### Panel de Cliente
| Ruta | Descripción |
|------|-------------|
| `/cliente/login` | Acceso específico para clientes |
| `/cliente/panel` | Panel de cliente tras login exitoso |

### React Dashboard (SPA)
| Ruta | Descripción |
|------|-------------|
| `/react-dashboard` | Dashboard React SPA de administración |
| `/react-dashboard/dashboard/envio/:codigo` | Detalle de envío con timeline y evidencias |

### Actuator (monitoreo)
| Ruta | Descripción |
|------|-------------|
| `/actuator/health` | Estado de salud de la aplicación |
| `/actuator/info` | Información de la aplicación (nombre, versión, etc.) |

## Seguridad

### Pilares generales

- **BCrypt para clientes**: Las contraseñas de clientes se almacenan hash con BCrypt (nunca en texto plano)
- **Admin externalizado**: Las credenciales del admin se externalizan a variables de entorno (no hay hardcoded)
- **Variables de entorno**: Toda configuración sensible pasa por entorno (puertos, passwords, URLs)
- **.env ignorado**: El archivo `.env` está en `.gitignore` para evitar subir credenciales a Git
- **Uploads fuera del jar**: Los archivos subidos se almacenan en el sistema de archivos, no dentro del JAR
- **Logs separados**: Los logs se escriben en archivos externos, rotados diariamente
- **Actuator seguro**: Solo se exponen los endpoints `health` e `info`, y los detalles de salud requieren autenticación

### Seguridad SPA + API REST

La aplicación implementa una **arquitectura híbrida** donde el frontend React SPA convive con el frontend Thymeleaf tradicional, compartiendo el mismo backend Spring Security.

#### Mecanismo de autenticación

El SPA React **no utiliza JWT**. En su lugar, reutiliza la sesión de Spring Security mediante cookie **JSESSIONID** (HttpOnly, Secure en producción). Esto evita la complejidad de gestión de tokens manteniendo la seguridad.

#### Flujo de autenticación

```
React SPA (cliente navegador)
       │
       │ (1) GET /login → recibe formulario con CSRF token
       ▼
Spring Security (servidor)
       │
       │ (2) POST /login + _csrf + credentials
       │     → valida usuario/contraseña
       ▼
JSESSIONID cookie (HttpOnly, no accesible desde JavaScript)
       │
       │ (3) SPA navega al dashboard
       │     → GET /api/v1/admin/envios?page=0&size=1
       │     → el navegador envía JSESSIONID automáticamente
       ▼
API REST autenticada (200 JSON con datos)
```

1. El usuario accede a `/login-react` en el SPA
2. El SPA hace `GET /login` (a Spring Boot vía proxy de Vite) para obtener el token CSRF del formulario HTML
3. Extrae el token CSRF del HTML parseado
4. Envía `POST /login` con `username`, `password` y `_csrf`
5. Spring Security valida, crea la sesión y devuelve la cookie `JSESSIONID` (HttpOnly)
6. El SPA redirige al dashboard; todas las peticiones a la API REST llevan la cookie automáticamente

#### CSRF: deshabilitado para APIs, activo para Thymeleaf

| Contexto | CSRF | Motivo |
|----------|------|--------|
| Formularios Thymeleaf | ✅ Activo | Protección estándar contra CSRF en formularios HTML |
| APIs `/api/**` | ❌ Deshabilitado | La sesión ya está protegida por cookie HttpOnly; el SPA no puede leer JSESSIONID desde JavaScript. Sin CSRF token se evitan errores 403 en operaciones PUT/POST desde el SPA sin sacrificar seguridad real, ya que un atacante no puede leer la cookie JSESSIONID ni fabricar una sesión válida. |

**Decisión técnica documentada en**: `SecurityConfig.java` (javadoc de clase y comentarios en `filterChain`).

#### Cookie de sesión

- **HttpOnly**: `true` — no accesible desde JavaScript (`document.cookie`)
- **Secure**: `true` en producción (`application-prod.properties`)
- **SameSite**: Lax (default Spring Security) — evita envío en peticiones cross-site
- **CSRF token**: no necesario en APIs porque la cookie HttpOnly ya autentica cada petición

#### Protección de rutas

| Ruta | Protección |
|------|------------|
| `/` (home) | Pública |
| `/seguimiento` | Pública |
| `/api/v1/tracking/**` | Pública |
| `/api/v1/cliente/**` | Sesión requerida (403 si no autenticado) |
| `/admin/**` | Spring Security (redirect a `/login` si no autenticado) |
| `/api/v1/admin/**` | Spring Security (redirect a `/login` si no autenticado) |
| `/react-dashboard/**` | Pública (el SPA protege internamente con `ProtectedRoute`) |

#### Seguridad del SPA React

- **No almacena credenciales**: la sesión vive en el servidor, no en localStorage ni sessionStorage
- **No expone tokens**: no hay JWT que puedan ser interceptados por XSS
- **Logout**: `POST /logout` con CSRF invalida la sesión del lado del servidor
- **ProtectedRoute**: componente React que redirige a `/login-react` si no hay sesión activa
- **AuthContext**: verifica la sesión al montar la aplicación (`GET /api/v1/admin/envios?page=0&size=1`)
- **Interceptores Axios**: detectan respuestas HTML (login page) y muestran error "Necesitas iniciar sesión como admin"

## Uploads

- **Carpeta local**: En desarrollo, los archivos se guardan en `./uploads` relativo al directorio de ejecución
- **Persistencia**: Las imágenes de tracking, galería del CMS y evidencias se guardan permanentemente
- **Backups recomendados**: Realizar copias de seguridad periódicas de la carpeta `uploads/` ya que contiene:
  - Imágenes de seguimiento de envíos
  - Galería de operaciones del CMS
  - Evidencias y documentos adjuntos

## Logging

El sistema de logging utiliza **Logback** con la siguiente configuración:

- `logs/monteastur.log`: Log general de la aplicación (nivel INFO y superior)
- `logs/monteastur-error.log`: Solo advertencias y errores (nivel WARN y superior)
- **Rotación**: Diaria (un nuevo archivo cada día a medianoche)
- **Retención**: 30 días (los archivos más antiguos se eliminan automáticamente)
- El directorio de logs se puede configurar con la variable de entorno `LOG_DIR` (por defecto `./logs`)

## PWA (Progressive Web App)

El dashboard React SPA es una **PWA instalable** con las siguientes capacidades:

- **Instalable**: El usuario puede instalar la app en el dispositivo (manifest.webmanifest con iconos SVG 192/512)
- **Service Worker**: Registrado con Workbox, precache de 12 entradas (~980KB)
- **Offline fallback**: El SW sirve la app incluso sin conexión (navigateFallback)
- **Push Notifications**: Suscripción y recepción de notificaciones push en el navegador

### Cómo instalar

1. Abrir el dashboard en Chrome/Edge (https://dominio/react-dashboard)
2. Click en el icono de instalación en la barra del navegador
3. O usar el botón "Instalar App" en el navbar del dashboard

## Push Notifications

El sistema incluye notificaciones push nativas del navegador:

- **Backend**: `POST /api/v1/push/subscribe` (guarda suscripción)
- **Frontend**: Hook `usePushNotifications.js` (solicita permiso, suscribe/desuscribe)
- **Service Worker**: Manejador de eventos `push` y `notificationclick`
- **Demo**: Endpoint de prueba `POST /api/v1/push/test`

### Estados del botón

- 🔔 Activo — notificaciones habilitadas
- 🔕 Inactivo — no suscrito (click para activar)
- 🚫 Bloqueado — permiso denegado en el navegador

## Offline Mode

La aplicación funciona parcialmente sin conexión:

- **OfflineBanner**: Banner sticky que indica "Estás sin conexión"
- **Cache de datos**: Dashboard y detalle de envíos cacheados en localStorage
- **Cola offline**: Cambios de estado encolados cuando no hay conexión
  - Se procesan automáticamente al recuperar conexión
  - Deduplicación por código + estado
  - Toast de confirmación al sincronizar
- **Indicador visual**: "Mostrando datos offline" cuando se usa cache

## Healthchecks

El endpoint de healthcheck está disponible en:

**GET /actuator/health**

Debe devolver:
```json
{"status":"UP"}
```

Este endpoint es utilizado por Docker Compose y orquestadores para verificar que la aplicación está funcionando correctamente.

## API REST v1

La aplicación expone una API REST pública bajo `/api/v1/` para integración con sistemas externos, apps móviles y futuros frontends SPA.

La API convive con el frontend Thymeleaf actual. No requiere autenticación para los endpoints públicos de tracking; los endpoints de cliente y admin reutilizan la sesión existente (Spring Security para admin, sesión HTTP para cliente).

### API Pública — Tracking

No requiere autenticación.

**GET /api/v1/tracking/{codigo}**

```bash
curl http://localhost:8895/api/v1/tracking/MT-2026-0001
```

Respuesta 200:
```json
{
  "codigoUnico": "MT-2026-0001",
  "estado": "EN_TRANSITO",
  "destinatario": "María González",
  "origen": "Asturias, España",
  "destino": "Asunción, Paraguay",
  "peso": "15 kg",
  "contenido": "Ropa y alimentos",
  "ultimaActualizacion": "2026-05-20T14:30:00"
}
```

Respuesta 404:
```json
{
  "timestamp": "2026-05-21T02:00:00Z",
  "status": 404,
  "error": "Tracking no encontrado"
}
```

### API Cliente — Envíos propios

Requiere sesión de cliente activa (login en `/cliente/login`). Reutiliza la misma cookie de sesión.

**GET /api/v1/cliente/envios**

```bash
curl http://localhost:8895/api/v1/cliente/envios
```

Respuesta 200:
```json
[
  {
    "codigo": "MT-2026-0001",
    "estado": "EN_TRANSITO",
    "origen": "Asturias, España",
    "destino": "Asunción, Paraguay",
    "ultimaActualizacion": "2026-05-20T14:30:00"
  }
]
```

**GET /api/v1/cliente/envios/{codigo}**

```bash
curl http://localhost:8895/api/v1/cliente/envios/MT-2026-0001
```

Respuesta 200: Ídem TrackingDto completo con eventos y evidencias visibles.

Respuesta 403 (envío ajeno o sin sesión):
```json
{
  "timestamp": "2026-05-21T02:00:00Z",
  "status": 403,
  "error": "Acceso denegado"
}
```

Respuesta 404:
```json
{
  "timestamp": "2026-05-21T02:00:00Z",
  "status": 404,
  "error": "Tracking no encontrado"
}
```

### API Admin — Gestión de envíos

Requiere sesión de administrador (Spring Security, login en `/login`). Reutiliza la misma cookie de sesión.

**GET /api/v1/admin/envios**

Lista paginada de todos los envíos con filtros y ordenación.

```bash
# Paginación básica
curl "http://localhost:8895/api/v1/admin/envios?page=0&size=5"

# Filtro por estado
curl "http://localhost:8895/api/v1/admin/envios?estado=EN_TRANSITO"

# Búsqueda por código (coincidencia parcial)
curl "http://localhost:8895/api/v1/admin/envios?codigo=MT-2026"

# Ordenación por fecha descendente
curl "http://localhost:8895/api/v1/admin/envios?sort=ultimaActualizacion,desc"

# Combinación de filtros
curl "http://localhost:8895/api/v1/admin/envios?page=0&size=10&estado=EN_TRANSITO&sort=ultimaActualizacion,desc"
```

| Parámetro | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `page` | int | 0 | Número de página (zero-based) |
| `size` | int | 20 | Elementos por página |
| `estado` | string | — | Filtro exacto por estado (RECIBIDO, EN_TRANSITO, ENTREGADO, etc.) |
| `codigo` | string | — | Búsqueda parcial por código único |
| `sort` | string | `ultimaActualizacion,desc` | Campo y dirección de ordenación |

Respuesta 200:
```json
{
  "content": [
    {
      "codigoUnico": "MT-2026-0001",
      "estado": "EN_TRANSITO",
      "destinatario": "María González",
      "origen": "Asturias, España",
      "destino": "Asunción, Paraguay",
      "ultimaActualizacion": "2026-05-20T14:30:00"
    }
  ],
  "totalElements": 25,
  "totalPages": 3,
  "size": 10,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "first": true,
  "last": false,
  "empty": false
}
```

**Ventajas de la paginación:**
- Escalabilidad: consultas optimizadas con LIMIT/OFFSET en base de datos
- Dashboards grandes: carga progresiva sin bloquear la interfaz
- Apps móviles: respuestas ligeras con tamaños de página reducidos
- Tablas dinámicas: integración directa con tablas DataTables, AG Grid, etc.
- Optimización backend: evita cargar miles de registros en memoria

**GET /api/v1/admin/envios/{codigo}**

```bash
curl http://localhost:8895/api/v1/admin/envios/MT-2026-0001
```

Respuesta 200: TrackingDto completo con datos del cliente, eventos del timeline y todas las evidencias.

**PUT /api/v1/admin/envios/{codigo}/estado**

Actualiza el estado del envío y crea automáticamente un evento de tracking en el timeline.

```bash
curl -X PUT http://localhost:8895/api/v1/admin/envios/MT-2026-0001/estado \
  -H "Content-Type: application/json" \
  -d '{"estado":"EN_TRANSITO"}'
```

Respuesta 200: TrackingDto completo actualizado.

Respuesta 404:
```json
{
  "timestamp": "2026-05-21T02:00:00Z",
  "status": 404,
  "error": "Tracking no encontrado"
}
```

### Códigos de estado HTTP

| Código | Descripción |
|--------|-------------|
| 200 OK | Petición exitosa |
| 403 Forbidden | Acceso denegado (sesión no válida o envío ajeno) |
| 404 Not Found | Recurso no encontrado |
| 500 Internal Server Error | Error interno del servidor |

### Nota de arquitectura

El backend implementa un modelo híbrido **MVC + REST API + SPA**. El frontend Thymeleaf sigue activo y es completamente funcional. El dashboard React SPA se sirve desde `/react-dashboard` y se comunica con la API REST `/api/v1/admin/` usando la misma sesión Spring Security. La API REST `/api/v1/` también está diseñada para ser consumida por aplicaciones móviles e integraciones con sistemas externos. Todas las capas comparten los mismos servicios, repositorios y entidades JPA, garantizando consistencia en la lógica de negocio.

## Backup & Restore

Ver guía completa en [`docs/BACKUP_RECOVERY.md`](docs/BACKUP_RECOVERY.md).

### Scripts disponibles

```bash
# Backup base de datos (Linux)
./scripts/backup-db.sh              # → backup/db/YYYY-MM-DD_HH-mm.sql.gz

# Restore base de datos (Linux)
./scripts/restore-db.sh backup/db/2026-05-23_14-00.sql.gz

# Backup uploads (Linux)
./scripts/backup-uploads.sh         # → backup/uploads/YYYY-MM-DD_HH-mm.tar.gz

# Restore uploads (Linux) — crea backup previo automático
./scripts/restore-uploads.sh backup/uploads/2026-05-23_14-00.tar.gz
```

Versiones PowerShell para Windows disponibles en `scripts/*.ps1`.

### Automatización (cron)

```cron
0 3 * * * /opt/monteastur/scripts/backup-db.sh
0 4 * * * /opt/monteastur/scripts/backup-uploads.sh
0 5 * * * find /opt/monteastur/backup -name "*.sql.gz" -mtime +30 -delete
```

## Deploy rápido (VPS)

```bash
# En el VPS Ubuntu 22.04:
apt update && apt install -y docker.io docker-compose-v2 git curl
cd /opt && git clone <repo-url> monteastur && cd monteastur
cp .env.example .env && nano .env     # Configurar credenciales
docker compose build && docker compose up -d
curl http://localhost/actuator/health  # Debe responder {"status":"UP"}
```

Ver guía completa en [`docs/VPS_DEPLOY_GUIDE.md`](docs/VPS_DEPLOY_GUIDE.md).

## HTTPS

Para producción con SSL, seguir [`docs/HTTPS_SETUP.md`](docs/HTTPS_SETUP.md):

```bash
docker compose --profile certbot run --rm certbot certonly \
  --webroot -w /var/www/certbot -d monteastur.com -d www.monteastur.com
# Descomentar bloque HTTPS en nginx/conf.d/monteastur.conf
docker compose restart nginx
```

## Checklist de producción

Antes de desplegar en un entorno de producción, verificar:

- [ ] **Build OK**: `mvn clean package -DskipTests` finaliza sin errores
- [ ] **Docker OK**: `docker compose up -d` levanta todos los servicios correctamente
- [ ] **Health UP**: `curl http://localhost/actuator/health` → `{"status":"UP"}`
- [ ] **Uploads OK**: Las imágenes se suben, se almacenan y se muestran correctamente
- [ ] **Login admin OK**: Acceso al panel admin con credenciales de producción
- [ ] **Login cliente OK**: Los clientes pueden autenticarse y acceder a su panel
- [ ] **Logs OK**: Se generan archivos en logs/ sin errores de permisos
- [ ] **Backups probados**: Se puede restaurar BD y uploads desde backup
- [ ] **Variables entorno**: Todas las variables en `.env` configuradas correctamente
- [ ] **Security headers**: `curl -I http://localhost` muestra HSTS, CSP, XFO
- [ ] **Nginx proxy**: Nginx sirve en puerto 80/443, proxy a app:8080
- [ ] **PWA instalable**: Manifest y Service Worker funcionando
- [ ] **Offline mode**: Dashboard funciona con datos cacheados sin conexión

## Troubleshooting

| Problema | Causa probable | Solución |
|----------|---------------|----------|
| App no arranca (container restart loop) | Schema BD no existe (`DDL_AUTO=validate`) | Temporalmente `DB_DDL_AUTO=update`, luego revertir |
| Error 502 Bad Gateway | Nginx no alcanza app | `docker ps` para verificar app está running |
| Error 403 en API | Sesión no válida o CSRF | Login en `/login` primero; CSRF deshabilitado para `/api/**` |
| Uploads no se ven | Ruta incorrecta o permisos | `docker exec monteastur-app ls -la /app/uploads` |
| Puerto 80 ocupado | Otro servicio (IIS, Apache) | Cambiar `NGINX_PORT` en `.env` |
| PWA no instala | Sin HTTPS o manifest incorrecto | Usar HTTPS; verificar console para errores |
| Push notifications no funcionan | Permiso bloqueado o sin HTTPS | HTTPS requerido; resetear permiso en navegador |
| Offline no funciona | Service Worker no registrado | Hard refresh (Ctrl+Shift+R) y recargar |

## CI/CD

El proyecto usa [GitHub Actions](https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS/actions) para integración continua. El pipeline se ejecuta automáticamente en cada push a `develop` o `feature/*`, y en cada PR hacia `develop`.

### Jobs

| Job | Comando | Artefacto |
|-----|---------|-----------|
| `backend-build` | `mvn clean package -DskipTests` | `backend-jar` (target/*.jar) |
| `frontend-build` | `npm install` → `npm run build` | `frontend-dist` (frontend-react/dist) |
| `docker-build` | `docker compose build` | — |

Los artefactos generados (`backend-jar`, `frontend-dist`) están disponibles para descarga en la página de cada ejecución en GitHub Actions.

## Roadmap futuro

### Funcionalidades

- **Emails automáticos**: Notificaciones por email en cambios de estado de envíos
- **WhatsApp API**: Envío de notificaciones y actualizaciones vía WhatsApp
- **WebSockets tracking**: Actualizaciones en tiempo real del tracking sin recargar la página
- **App móvil**: Aplicación nativa para clientes y operadores
- **Roles avanzados**: Sistema de permisos más granular (operador, supervisor, auditor)
- **Exportar datos**: CSV, Excel y PDF desde el dashboard React
- **Notificaciones push**: Alertas en el navegador para cambios de estado

### Roadmap Seguridad

- **JWT para APIs**: Autenticación stateless basada en tokens para apps móviles e integraciones externas
- **Refresh tokens**: Rotación segura de tokens sin reautenticar al usuario
- **Roles granulares**: Permisos por acción (lectura, escritura, borrado) en lugar de roles planos
- **Rate limiting**: Protección contra abuso de API y fuerza bruta con bucket4j o Spring Cloud Gateway
- **Audit logs**: Trazabilidad completa de todas las operaciones sobre envíos (quién, cuándo, qué cambió)
- **HTTPS reverse proxy**: Terminación TLS en Nginx/Caddy con HSTS y cabeceras de seguridad
- **Content Security Policy**: Cabeceras CSP para prevenir XSS en el SPA
- **OWASP Top 10**: Auditoría periódica contra las vulnerabilidades más críticas

---

> **Nota profesional**: Este documento está pensado como guía de referencia para desarrolladores, DevOps y equipos de operaciones. Para consultas técnicas específicas, referirse al código fuente y los comentarios en el mismo.