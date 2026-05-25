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

Configuración en `nginx/conf.d/`:
- **`local.conf`**: HTTP sin SSL, `server_name localhost`. Usada en desarrollo local.
- **`monteastur.conf`**: HTTP con security headers, `server_name _` (catch-all). Usada en producción antes de SSL.
- **`examples/production-example.conf`**: Plantilla completa HTTPS para producción (no se carga automáticamente, copiar a `conf.d/` cuando se tengan certificados).

## Requisitos

- **Java 17+** (se recomienda JDK 17 LTS)
- **Maven 3.9+** (para build y gestión de dependencias)
- **Docker Desktop** (para contenedores)
- **MySQL 8** (base de datos, accesible vía localhost:3307 o servicio Docker)
- **Git** (control de versiones)

## Arranque local

## Credenciales desarrollo local

| Rol | URL login | Usuario | Contraseña |
|-----|-----------|---------|------------|
| Admin (Spring Security) | `http://localhost:8090/login` | `admin` | `admin123` |
| Cliente (custom session) | `http://localhost:8090/cliente/login` | email: `cliente@monteastur.com` | `demo2026` |
| React SPA | `http://localhost:8090/login-react` | `admin` | `admin123` |
| Grafana | `http://localhost:3001` | `admin` | `admin123` |

> **IMPORTANTE:** Estas credenciales son SOLO para desarrollo local. En producción, generar contraseñas seguras con `openssl rand -base64 32` y configurarlas vía variables de entorno.

### Demo Data

Cuando `APP_DEMO_DATA=true` (valor por defecto en `.env` local), al iniciar la aplicación se cargan automáticamente:

- **Cliente demo**: `cliente@monteastur.com` / `demo2026` (María González)
- **4 envíos demo**: MT-2026-0001 a MT-2026-0004, con historial de eventos, estados variados (en tránsito, aduana, reparto, entregado)
- **4 mensajes de contacto**: para que `/admin/mensajesrecibidos` tenga contenido
- **4 reservas/solicitudes**: con estados pendiente, confirmada y cancelada
- **4 imágenes demo**: SVG estáticos en `/img/demo-gallery/` (oficinas, flota, almacén, puerto). Las subidas reales siguen usando `/uploads/`
- **Textos legales**: aviso legal y política de cookies

> **Persistencia de datos:**
> - `docker compose down` — borra contenedores **sin** borrar datos (volúmenes intactos)
> - `docker compose down -v` — borra contenedores **y** volúmenes (incluyendo MySQL). Al arrancar de nuevo, `APP_DEMO_DATA=true` repuebla automáticamente todos los datos demo
> - Si se añaden datos reales durante el desarrollo, evitar `docker compose down -v` para no perderlos

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

## Preproducción local

Documentación generada tras la auditoría técnica previa al primer despliegue real:

| Documento | Descripción |
|-----------|-------------|
| [`docs/PREPRODUCTION_AUDIT_REPORT.md`](docs/PREPRODUCTION_AUDIT_REPORT.md) | Auditoría completa: servicios, rutas, tests, seguridad, riesgos y decisión final |
| [`docs/KNOWN_ISSUES_PREPROD.md`](docs/KNOWN_ISSUES_PREPROD.md) | Issues conocidos con impacto, prioridad y solución propuesta |
| [`docs/LOCAL_DEV_COMMANDS.md`](docs/LOCAL_DEV_COMMANDS.md) | Comandos rápidos para desarrollo local (docker, logs, troubleshooting) |

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

## Producción VPS

Guía completa en [`docs/PRODUCTION_VPS_RUNBOOK.md`](docs/PRODUCTION_VPS_RUNBOOK.md).

### Comandos rápidos

```bash
# Bootstrap del VPS (como root)
sudo ./scripts/vps-bootstrap.sh

# Deploy (como usuario deploy en /opt/monteastur)
./scripts/deploy-prod.sh

# Rollback a tag específica
./scripts/rollback-prod.sh v14.0-e2e-ready

# Backup manual
./scripts/backup-db.sh
./scripts/backup-uploads.sh

# Restore
./scripts/restore-db.sh backup/db/<archivo>.sql.gz
./scripts/restore-uploads.sh backup/uploads/<archivo>.tar.gz

# Verificar estado
curl -f http://localhost/actuator/health
docker ps
```

### Estructura en VPS

```
/opt/monteastur/
├── docker-compose.yml
├── .env                  # Credenciales (NO en Git)
├── scripts/              # Deploy, rollback, backup, restore
├── docs/                 # Runbook y guías
├── backup/
│   ├── db/               # Backups MySQL (.sql.gz)
│   └── uploads/          # Backups uploads (.tar.gz)
├── logs/                 # Logs de la aplicación
├── nginx/conf.d/         # Configuración proxy y SSL
└── monitoring/           # Prometheus, Grafana
```

### Scripts disponibles

| Script | Función |
|--------|---------|
| `scripts/vps-bootstrap.sh` | Instala Docker, Docker Compose, crea directorios, configura UFW |
| `scripts/deploy-prod.sh` | Git pull, build, up -d, image prune, healthcheck |
| `scripts/rollback-prod.sh <tag>` | Git checkout a tag, rebuild, healthcheck |
| `scripts/backup-db.sh` | Backup MySQL → `backup/db/` |
| `scripts/backup-uploads.sh` | Backup uploads → `backup/uploads/` |
| `scripts/restore-db.sh` | Restore MySQL desde backup |
| `scripts/restore-uploads.sh` | Restore uploads desde backup |

### Healthchecks

```bash
curl http://localhost/actuator/health        # {"status":"UP"}
curl http://localhost/actuator/info          # Info app
curl http://localhost:9090/targets           # Prometheus targets
```

### Rollback rápido

```bash
./scripts/rollback-prod.sh v14.0-e2e-ready
```

### GitHub Actions — Deploy manual

El workflow [`deploy-prod.yml`](.github/workflows/deploy-prod.yml) permite desplegar a producción manualmente desde GitHub Actions.

**Cómo ejecutar:**

1. Ir a **GitHub → Actions → Deploy Production**
2. Click **Run workflow**
3. Seleccionar branch `develop`
4. Escribir `deploy` en el campo de confirmación
5. Click **Run workflow**

**Jobs:**

| Job | Descripción |
|-----|-------------|
| `pre-deploy-check` | Valida `docker compose config`, ejecuta `mvn test`, `npm test`, `npm run build` |
| `deploy-production` | SSH al VPS, git pull, `./scripts/deploy-prod.sh` |
| `notify-failure` | Muestra instrucciones de rollback si falla |

**Protecciones:**
- Solo ejecutable desde branch `develop`
- Requiere confirmación explícita escribiendo "deploy"
- `fail-fast` configurado
- Timeout de 15min (validación) + 20min (deploy)

**Secrets requeridos en GitHub:**

| Secret | Descripción |
|--------|-------------|
| `VPS_HOST` | IP o dominio del VPS |
| `VPS_USER` | Usuario SSH (ej: `deploy`) |
| `VPS_SSH_KEY` | Clave privada SSH (formato PEM/OpenSSH) |
| `VPS_PORT` | Puerto SSH (opcional, default 22) |

### Monitoring

| Servicio | Puerto | Acceso |
|----------|--------|--------|
| Prometheus | 9090 | `http://<vps>:9090` |
| Grafana | 3000 | `http://<vps>:3000` (admin / pass desde .env) |
| Uptime Kuma | 3001 | `http://<vps>:3001` |

Para más detalles ver [`docs/PRODUCTION_VPS_RUNBOOK.md`](docs/PRODUCTION_VPS_RUNBOOK.md) y [`docs/PRODUCTION_VPS_RUNBOOK.md#14-configurar-github-secrets-para-cd`](docs/PRODUCTION_VPS_RUNBOOK.md#14-configurar-github-secrets-para-cd).

## Hardening VPS

Guía completa en [`docs/VPS_HARDENING_CHECKLIST.md`](docs/VPS_HARDENING_CHECKLIST.md).

### Scripts disponibles

| Script | Función |
|--------|---------|
| `scripts/server-healthcheck.sh` | Reporta uptime, disco, RAM, Docker, healthcheck |
| `scripts/backup-db.sh` | Backup MySQL → `backup/db/` |
| `scripts/backup-uploads.sh` | Backup uploads → `backup/uploads/` |

### Resumen de hardening

| Medida | Estado |
|--------|--------|
| SSH: sin root, sin contraseñas | ✅ Documentado |
| UFW: puertos mínimos (22, 80, 443) | ✅ Documentado + script |
| fail2ban: protección fuerza bruta | ✅ Documentado |
| unattended-upgrades: seguridad auto | ✅ Documentado |
| Docker: restart, healthchecks, límites | ✅ Implementado |
| Backups: BD, uploads, .env | ✅ Scripts listos |
| Monitoring: Prometheus, Grafana, Kuma | ✅ Implementado |
| SSL: Let's Encrypt + renovación auto | ✅ Documentado |
| Security headers: CSP, HSTS, XFO | ✅ Implementado |

### Healthcheck rápido

```bash
./scripts/server-healthcheck.sh
```

### Backups automáticos (cron)

```cron
0 3 * * * /opt/monteastur/scripts/backup-db.sh
0 4 * * * /opt/monteastur/scripts/backup-uploads.sh
```

## Primer Deploy VPS

Guía completa en [`docs/FIRST_VPS_DEPLOY_CHECKLIST.md`](docs/FIRST_VPS_DEPLOY_CHECKLIST.md).

### Proveedor recomendado

| Proveedor | Plan | vCPU | RAM | SSD | Precio/mes |
|-----------|------|------|-----|-----|------------|
| **Hetzner** | CX22 | 2 | 4 GB | 40 GB | **~€4.50** |

Alternativa económica: Contabo Cloud S (~€6.99/mes, 4 vCPU, 8 GB RAM, 200 GB SSD).

### Coste mensual estimado

| Concepto | Coste |
|----------|-------|
| VPS Hetzner CX22 | ~€4.50 |
| Dominio .com | ~€0.83/mes (~€10/año) |
| SSL, Monitoring, Uptime | €0 (auto-hospedado) |
| **Total** | **~€5.33/mes** |

### Orden de ejecución (45 min estimado)

```
 1. Contratar VPS + anotar IP     (10 min)
 2. Bootstrap + clonar repo       ( 5 min)
 3. Crear deploy + configurar SSH  ( 5 min)
 4. Configurar .env                ( 5 min)
 5. Generar SSH key CD             ( 2 min)
 6. docker compose up              (10 min)
 7. DNS + HTTPS                    (10 min + propagación)
 8. GitHub Secrets                 ( 5 min)
 9. Workflow manual                ( 5 min)
10. Validaciones finales           ( 5 min)
```

### Resumen checklist

- [ ] VPS contratado (Hetzner CX22 recomendado)
- [ ] DNS apuntando al VPS (registro A)
- [ ] `.env` configurado con credenciales seguras
- [ ] GitHub Secrets: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`
- [ ] HTTPS con Let's Encrypt funcionando
- [ ] `docker ps` → 6/6 containers UP
- [ ] `curl -f /actuator/health` → `{"status":"UP"}`
- [ ] Workflow manual ejecutado desde GitHub Actions

Ver [`docs/FIRST_VPS_DEPLOY_CHECKLIST.md`](docs/FIRST_VPS_DEPLOY_CHECKLIST.md) para guía completa.

## VPS real online

Guías completas y comandos exactos para el primer despliegue real en VPS.

### Documentos operativos

| Guía | Contenido |
|------|-----------|
| [`docs/VPS_REAL_EXECUTION_GUIDE.md`](docs/VPS_REAL_EXECUTION_GUIDE.md) | Guía completa: compra, SSH, bootstrap, Docker, HTTPS, deploy, rollback |
| [`docs/PRODUCTION_ENV_GUIDE.md`](docs/PRODUCTION_ENV_GUIDE.md) | Cómo configurar `.env`, generar passwords, DDL_AUTO, seguridad |
| [`docs/FIRST_REAL_DEPLOY_COMMANDS.md`](docs/FIRST_REAL_DEPLOY_COMMANDS.md) | Comandos exactos por bloque (A-H): local, VPS, DNS, HTTPS, smoke tests, rollback |
| [`.env.production.example`](.env.production.example) | Plantilla `.env` completa para producción |

### Flujo rápido

```
1. Comprar VPS Hetzner CX22 (Ubuntu 24.04)      coste ~€4.50/mes
2. Seguir VPS_REAL_EXECUTION_GUIDE.md               ~60 min
3. Configurar .env con PRODUCTION_ENV_GUIDE.md       ~15 min
4. Ejecutar comandos FIRST_REAL_DEPLOY_COMMANDS.md   ~45 min
5. Smoke tests y checklist final                     ~30 min
```

### Coste estimado total

| Concepto | Coste |
|----------|-------|
| VPS Hetzner CX22 | ~€4.50/mes |
| Dominio .com | ~€0.83/mes |
| SSL / Monitoring / CI/CD | €0 |
| **Total** | **~€5.33/mes (~€69/año)** |

## Compra VPS y dominio

Guías detalladas para la contratación real de infraestructura.

| Guía | Contenido |
|------|-----------|
| [`docs/HETZNER_VPS_PURCHASE_GUIDE.md`](docs/HETZNER_VPS_PURCHASE_GUIDE.md) | Crear cuenta, verificación, crear servidor CX22, qué NO elegir, primer login, checklist |
| [`docs/DOMAIN_PURCHASE_GUIDE.md`](docs/DOMAIN_PURCHASE_GUIDE.md) | Proveedores, recomendación Cloudflare, qué dominio elegir, DNS, Cloudflare proxy, checklist |
| [`docs/REAL_DEPLOY_TIMELINE.md`](docs/REAL_DEPLOY_TIMELINE.md) | Plan 3 días, tiempos, costes, puntos críticos, riesgos, cuándo abortar |

### Recomendación final

| Recurso | Proveedor | Plan | Coste |
|---------|-----------|------|-------|
| VPS | **Hetzner Cloud** | CX22 (2 vCPU, 4GB, 40GB SSD) | ~€4.50/mes |
| Dominio | **Cloudflare Registrar** | monteastur.com (WHOIS privado incluido) | ~€9.15/año |
| DNS | Cloudflare DNS | Anycast + proxy DDoS | €0 |
| SSL | Let's Encrypt | Automático con renovación | €0 |
| Monitoring | Prometheus + Grafana + Uptime Kuma | En Docker compose | €0 |

### Timeline rápido

```
DÍA 1: Comprar VPS + dominio (~40 min + ~24h verificación Hetzner)
DÍA 2: Bootstrap + Docker + DNS + HTTPS (~1h + espera DNS)
DÍA 3: Monitoring + Backups + GitHub Actions + Smoke tests (~1h)
```

Ver [`docs/REAL_DEPLOY_TIMELINE.md`](docs/REAL_DEPLOY_TIMELINE.md) para detalles.

### Comandos principales

```bash
# Bootstrap VPS
ssh root@<VPS_IP>
sudo ./scripts/vps-bootstrap.sh

# Primer deploy manual en VPS
ssh deploy@<VPS_IP>
cd /opt/monteastur
docker compose up -d --build

# Workflow automático
# GitHub → Actions → Deploy Production → Run workflow

# Healthcheck
./scripts/server-healthcheck.sh

# Rollback
./scripts/rollback-prod.sh v14.0-e2e-ready
```

Ver [`docs/LIVE_DEPLOY_PLAN.md`](docs/LIVE_DEPLOY_PLAN.md) para los 15 pasos detallados.

## GitHub Secrets + SSH

Guía completa en [`docs/GITHUB_SECRETS_SSH_SETUP.md`](docs/GITHUB_SECRETS_SSH_SETUP.md).
Script de verificación: [`scripts/check-ssh-connection.sh`](scripts/check-ssh-connection.sh).

### Secrets necesarios

| Secret | Descripción | Ejemplo |
|--------|-------------|---------|
| `VPS_HOST` | IP o dominio del VPS | `203.0.113.10` |
| `VPS_USER` | Usuario SSH | `deploy` |
| `VPS_SSH_KEY` | Clave privada (multilínea) | `-----BEGIN OPENSSH...` |
| `VPS_PORT` | Puerto SSH (opcional) | `22` |

### Orden recomendado

```bash
# 1. Generar clave SSH dedicada
ssh-keygen -t ed25519 -C "github-actions@monteastur" -f ~/.ssh/github-actions-monteastur

# 2. Copiar clave pública al VPS
ssh-copy-id -i ~/.ssh/github-actions-monteastur.pub deploy@<VPS_IP>

# 3. Mostrar clave privada y copiarla a GitHub Secrets
cat ~/.ssh/github-actions-monteastur

# 4. Verificar conexión
./scripts/check-ssh-connection.sh

# 5. Probar workflow en GitHub Actions
# GitHub → Actions → Deploy Production → Run workflow
```

### Verificación rápida

```bash
# Probar conexión SSH local
VPS_HOST=<VPS_IP> ./scripts/check-ssh-connection.sh

# Verificar desde local
ssh -i ~/.ssh/github-actions-monteastur deploy@<VPS_IP> "echo OK && docker ps"
```

### Troubleshooting básico

| Problema | Solución |
|----------|----------|
| Permission denied | `ssh-copy-id` para añadir clave pública al VPS |
| bad permissions | `chmod 700 ~/.ssh && chmod 600 ~/.ssh/authorized_keys` en VPS |
| Connection refused | `sudo ufw status` verificar puerto 22; `systemctl status sshd` |
| Host key changed | `ssh-keygen -R <VPS_IP>` para limpiar cache |
| fail2ban bloqueó | `sudo fail2ban-client set sshd unbanip <IP>` en VPS |

Ver [`docs/GITHUB_SECRETS_SSH_SETUP.md`](docs/GITHUB_SECRETS_SSH_SETUP.md) para guía completa.

## Dominio + HTTPS

Guía completa en [`docs/DOMAIN_DNS_SSL_SETUP.md`](docs/DOMAIN_DNS_SSL_SETUP.md).
Ejemplo de configuración nginx en [`nginx/examples/production-example.conf`](nginx/examples/production-example.conf).

### Flujo resumido

```
 1. Comprar dominio (Namecheap / Cloudflare Registrar)
 2. Configurar registro A → IP del VPS (TTL 300)
 3. Verificar propagación: dig +short monteastur.com
 4. Obtener certificado SSL con Let's Encrypt
 5. Copiar certificados a nginx/ssl/
 6. Activar HTTPS en nginx (descomentar bloque SSL)
 7. Verificar: curl -I https://monteastur.com
 8. Configurar renovación automática (cron)
```

### DNS

| Registro | Tipo | Valor | TTL |
|----------|------|-------|-----|
| `@` | A | IP del VPS | 300→3600 |
| `www` | A | IP del VPS | 300→3600 |

Subdominios opcionales: `grafana`, `uptime`, `app`.

### HTTPS

```bash
# Obtener certificado
docker compose --profile certbot run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d monteastur.com -d www.monteastur.com \
  --email admin@monteastur.com \
  --agree-tos --no-eff-email

# Copiar a nginx y recargar
cp /etc/letsencrypt/live/monteastur.com/fullchain.pem nginx/ssl/
cp /etc/letsencrypt/live/monteastur.com/privkey.pem nginx/ssl/
docker compose restart nginx
```

### Nginx

Configuración de ejemplo completa en [`nginx/examples/production-example.conf`](nginx/examples/production-example.conf):
- HTTP → HTTPS redirect
- SSL termination
- Security headers (HSTS, CSP, XFO)
- Gzip compression
- Proxy pass a Spring Boot
- WebSocket ready

### Troubleshooting rápido

| Problema | Solución |
|----------|----------|
| DNS no propaga | `dig @8.8.8.8 monteastur.com`, esperar TTL |
| Certbot falla | Verificar puerto 80 abierto y DNS propagado |
| Mixed Content | Todos los assets deben servirse por HTTPS |
| Redirect loop | Cloudflare en modo "Full (strict)" |

Ver [`docs/DOMAIN_DNS_SSL_SETUP.md`](docs/DOMAIN_DNS_SSL_SETUP.md) para guía completa y troubleshooting detallado.

## Deploy checklist final

Checklist operativa completa en [`docs/FINAL_PRODUCTION_DEPLOY_CHECKLIST.md`](docs/FINAL_PRODUCTION_DEPLOY_CHECKLIST.md).
Smoke tests en [`docs/SMOKE_TESTS_PRODUCTION.md`](docs/SMOKE_TESTS_PRODUCTION.md).

### Orden rápido

```
PRE-DEPLOY (30 min)
├── VPS contratado + Ubuntu actualizado
├── usuario deploy + SSH + UFW + fail2ban
├── dominio + DNS propagado
├── GitHub Secrets configurados
└── .env con credenciales seguras

DEPLOY (45 min)
├── docker compose build + up -d
├── verificar 6/6 containers UP
├── healthcheck → {"status":"UP"}
├── HTTPS con Let's Encrypt
├── workflow manual desde GitHub Actions
└── crontab + renovaciones

POST-DEPLOY (20 min)
├── smoke tests (11 tests, 15 min)
├── web + login + API + monitoring
├── backups probados
├── rollback probado
└── checklist 24h
```

### Smoke tests (11 tests, ~15 min)

| # | Test | Prioridad |
|---|------|-----------|
| 1 | Healthcheck endpoint `{"status":"UP"}` | 🔴 Alta |
| 2 | Home page carga con security headers | 🔴 Alta |
| 3 | Tracking público funciona | 🔴 Alta |
| 4 | Login admin correcto | 🔴 Alta |
| 5 | Login cliente correcto | 🔴 Alta |
| 6 | Dashboard React SPA sin page errors | 🟡 Media |
| 7 | Upload/subida de imágenes | 🟡 Media |
| 8 | Monitoring (Prometheus, Grafana, Kuma) | 🟡 Media |
| 9 | PWA instalable | 🟢 Baja |
| 10 | SSL Labs grade A+ | 🟢 Baja |
| 11 | Mobile responsive | 🟢 Baja |

> **Criterio:** Todos los 🔴 deben pasar. Si alguno falla, no considerar deploy exitoso.

### Rollback rápido

```bash
# Si algo falla durante el deploy
cd /opt/monteastur && ./scripts/rollback-prod.sh v14.0-e2e-ready
```

Ver [`docs/FINAL_PRODUCTION_DEPLOY_CHECKLIST.md`](docs/FINAL_PRODUCTION_DEPLOY_CHECKLIST.md) para checklist completa y plan de contingencia.

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

## Primer deploy real

Checklist maestra y scripts para ejecutar el primer deploy real de MonteAstur en un VPS público.

### Documentos

| Documento | Contenido |
|-----------|-----------|
| [`docs/FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md`](docs/FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md) | Checklist maestra 16 fases (A-P): compra VPS, dominio, DNS, SSH, bootstrap, Docker, HTTPS, secrets, deploy, smoke tests, monitoring, backup, rollback |
| [`docs/REAL_DEPLOY_DECISION_LOG.md`](docs/REAL_DEPLOY_DECISION_LOG.md) | Decisiones técnicas, proveedor, costes, riesgos, qué se deja para después |

### Scripts nuevos

| Script | Función |
|--------|---------|
| `scripts/production-smoke-test.sh` | Smoke tests post-deploy: healthcheck, home, login-react, tracking. `BASE_URL=https://dominio ./scripts/production-smoke-test.sh` |
| `scripts/production-post-deploy-check.sh` | Verificación post-deploy: docker ps, healthcheck, disco, RAM, logs, Prometheus/Grafana/Kuma |

### Orden recomendado

```
 1. Seguir docs/FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md (fases A-P)
 2. Ejecutar: ./scripts/production-smoke-test.sh
 3. Ejecutar: ./scripts/production-post-deploy-check.sh
 4. Verificar docs/REAL_DEPLOY_DECISION_LOG.md para contexto
 5. Checklist 24h (fase P de la master checklist)
```

### Comandos rápidos

```bash
# Smoke tests
BASE_URL=https://monteastur.com ./scripts/production-smoke-test.sh

# Post-deploy check
./scripts/production-post-deploy-check.sh

# Healthcheck rápido
curl -f https://monteastur.com/actuator/health
```

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

## CD Automático VPS

### Flujo de deploy

Cada push a `develop` dispara el pipeline de deploy automático (`.github/workflows/deploy.yml`):

```
Git push develop
       ↓
  GitHub Actions
       ↓
  pre-deploy-check (Dockerfile, compose, .env.example)
       ↓
  deploy-vps (SSH → VPS)
       ↓
  git pull → docker compose up -d --build → image prune
```

La conexión SSH se realiza con clave privada — no se usan contraseñas.

### Secrets requeridos

| Secret | Descripción |
|--------|-------------|
| `VPS_HOST` | IP o dominio del VPS |
| `VPS_USER` | Usuario SSH (ej: `root` o `deploy`) |
| `VPS_SSH_KEY` | Clave privada SSH completa (incluyendo `-----BEGIN OPENSSH PRIVATE KEY-----`) |
| `VPS_PORT` | Puerto SSH (opcional, default `22`) |

Configurar en GitHub: **Settings → Secrets and variables → Actions**.

### Seguridad

- La clave SSH se almacena cifrada en GitHub Secrets, nunca en el repo
- El deploy solo se ejecuta en pushes a `develop` (no en PRs ni branches `feature/*`)
- `set -e` en los comandos remotos — si algo falla, el deploy se detiene
- Las credenciales de la base de datos y admin se configuran en el `.env` del VPS, no en GitHub

### Rollback manual

```bash
ssh user@vps
cd /opt/monteastur
git checkout <commit-anterior>
docker compose up -d --build
```

## Monitoring + Observability

El stack de monitorización usa Spring Boot Actuator + Prometheus + Grafana.

### Endpoints

| URL | Descripción |
|-----|-------------|
| `http://localhost:8080/actuator/health` | Healthcheck |
| `http://localhost:8080/actuator/info` | Info app |
| `http://localhost:8080/actuator/prometheus` | Métricas Prometheus (formato texto) |

### Servicios Docker

| Servicio | Puerto por defecto | Descripción |
|----------|--------------------|-------------|
| Prometheus | `9090` | Recolecta métricas cada 15s desde `app:8080/actuator/prometheus` |
| Grafana | `3000` | Dashboards visuales con datasource Prometheus auto-configurado |

### Métricas disponibles (vía Prometheus)

- **JVM**: memoria heap/no-heap, garbage collection, threads, clases cargadas
- **System**: CPU, uptime, file descriptors
- **HTTP**: request count, duration, active requests
- **Tomcat**: sessions, threads activos, errores
- **Logback**: contador por nivel de log
- **Actuator health**: estado de componentes (DB, ping, disk space)

### Acceso

```bash
# Prometheus
http://localhost:9090

# Grafana (login: admin / admin123)
http://localhost:3000
```

Tras iniciar sesión en Grafana, el datasource Prometheus ya está configurado automáticamente.

> Si las credenciales antiguas persisten (volumen con datos previos):
> ```bash
> docker compose down
> docker volume rm envios_paraguay_cms_grafana_data
> docker compose up -d
> ```
> Esto borra la base de datos interna de Grafana (paneles importados, usuarios, config). Los dashboards y datasources provisionados se recargan automáticamente.

### Dashboard Grafana

El dashboard **Monteastur Envios** se importa automáticamente al iniciar Grafana — sin configuración manual.

| Sección del dashboard | Paneles |
|----------------------|---------|
| **Estado** | App Status (UP/DOWN), Uptime, CPU Usage, Threads, Active Sessions, Log Errors |
| **Memoria** | Heap Used, Heap Max, Heap Usage %, Non-Heap Used |
| **JVM** | JVM Memory (time series usado/max/committed), Garbage Collection rate |
| **HTTP** | Requests/min, Latencia media, 4xx/min, 5xx/min |

Los paneles de state (Stat) muestran el valor actual; los de time series (gráficos) mantienen el histórico según el rango temporal seleccionado.

Para validar que Prometheus recibe datos correctamente:
- Abrir `http://localhost:9090/targets` — debe mostrar `app:8080` como `UP`
- Abrir `http://localhost:9090/graph` — ejecutar `up{application="monteastur-envios"}`

### Alertas Grafana

Se provisionan automáticamente 5 alertas básicas:

| Alerta | Condición | Severidad | Tiempo |
|--------|-----------|-----------|--------|
| **App Down** | `up == 0` | 🔴 critical | 1 min |
| **High CPU** | `cpu > 90%` | 🟡 warning | 5 min |
| **High Heap** | `heap > 90%` | 🟡 warning | 5 min |
| **High 5xx Rate** | `5xx > 5/min` | 🔴 critical | 5 min |
| **Prometheus Target Down** | `up == 0` | 🔴 critical | 1 min |

Las alertas se evalúan cada 30s. Cuando se activan, se agrupan por nombre y severidad cada 5 minutos.

**Para notificaciones reales en producción:**

1. Configurar SMTP en el servicio Grafana de `docker-compose.yml`:
   ```yaml
   environment:
     GF_SMTP_ENABLED: "true"
     GF_SMTP_HOST: "smtp.tudominio.com:587"
     GF_SMTP_USER: "tu@email.com"
     GF_SMTP_PASSWORD: "tu_password"
   ```
2. Editar `monitoring/grafana/provisioning/alerting/contactpoints.yml` con el email real
3. _(Opcional)_ Añadir contact point tipo Slack o webhook en el mismo archivo

Las reglas de alerta y contact points se recargan automáticamente al reiniciar Grafana.

### Uptime Monitoring

El stack incluye **Uptime Kuma** como monitor interno de uptime:

| Componente | Rol | Acceso |
|-----------|-----|--------|
| **Uptime Kuma** | Monitor de uptime auto-hospedado | `http://localhost:3001` |
| **Healthchecks.io** | Heartbeat externo (documentado) | `https://healthchecks.io` |

**Uptime Kuma** permite crear monitores de tipo HTTP, SSL, DNS, Docker, y más, con notificaciones multicanal (Telegram, Discord, Slack, email). Para configuración detallada, ver `docs/UPTIME_MONITORING.md`.

## Testing

El backend usa **JUnit 5 + Mockito** para testing automatizado.

### Stack de testing

| Herramienta | Uso |
|------------|-----|
| **JUnit 5 (Jupiter)** | Framework de testing |
| **Mockito** | Mocking de dependencias |
| **Spring Boot Test** | Contexto de aplicación para tests de integración |
| **MockMvc** | Testing de controladores HTTP |

### Tests incluidos

| Clase | Tipo | Dependencias |
|-------|------|-------------|
| `TrackingApiControllerTest` | `@WebMvcTest` | Mock de `EnvioTrackingRepository` |
| `ReservaServiceTest` | `@ExtendWith(MockitoExtension.class)` | Mock de `ReservaRepository` |
| `PushSubscriptionControllerTest` | `@WebMvcTest` | Sin dependencias externas |
| `SecurityConfigTest` | `@WebMvcTest` + `@Import(SecurityConfig.class)` | Verifica rutas públicas y protegidas |

### Ejecutar tests

```bash
# Todos los tests
mvn test

# Test específico
mvn test -Dtest=TrackingApiControllerTest

# Build completo (test + package)
mvn clean package
```

### Cobertura actual

| Capa | Tests | Cobertura aproximada |
|------|-------|---------------------|
| Controladores API | 5 tests | Tracking público, Push subscribe/unsubscribe |
| Servicios | 3 tests | Crear reserva, buscar por ID |
| Seguridad | 3 tests | Rutas públicas accesibles, admin protegido |
| **Total** | **11 tests** | Funcionalidades críticas cubiertas |

### CI

Los tests se ejecutan automáticamente en GitHub Actions antes del empaquetado:
`mvn clean package` (sin `-DskipTests`).

### Frontend Testing

El frontend React usa **Vitest + React Testing Library** para testing de componentes.

| Herramienta | Uso |
|------------|-----|
| **Vitest** | Test runner compatible con Vite |
| **@testing-library/react** | Renderizado e interacción con componentes |
| **@testing-library/jest-dom** | Matchers personalizados para el DOM |
| **@testing-library/user-event** | Simulación realista de eventos de usuario |
| **jsdom** | Entorno DOM simulado para tests |

#### Tests incluidos

| Componente | Tests | Qué prueba |
|-----------|-------|-----------|
| `LoginPage` | 4 | Renderiza formulario, login exitoso navega, login fallido muestra error |
| `StatsCard` | 2 | Renderiza label/valor, soporta icono y color |
| `StatusBadge` | 3 | Formatea estado, renderiza EN_TRANSITO, ENTREGADO, N/A |
| `EmptyState` | 2 | Mensaje por defecto, mensaje personalizado |
| `SearchBar` | 4 | Placeholder, debounce 300ms, botón clear |
| **Total** | **15** | |

#### Ejecutar tests

```bash
cd frontend-react

# Todos los tests (una vez)
npm test

# Modo watch (desarrollo)
npm run test:watch

# Con cobertura
npm run test:coverage
```

En CI, los tests se ejecutan automáticamente antes del build:
`npm test -- --run`

### E2E Testing

El proyecto usa **Playwright** para pruebas E2E (End-to-End) en navegador real.

| Herramienta | Uso |
|------------|-----|
| **Playwright** | Automatización de navegador Chromium |
| **@playwright/test** | Test runner con reporter HTML |

#### Tests E2E incluidos

| Archivo | Tests | Qué prueba |
|---------|-------|-----------|
| `e2e/home.spec.js` | 2 | Página principal carga sin errores, contenido visible |
| `e2e/login.spec.js` | 3 | Formulario login renderizado, login admin exitoso, login fallido muestra error |
| `e2e/dashboard.spec.js` | 2 | Dashboard carga tras login, tabla de envíos visible |
| `e2e/tracking.spec.js` | 2 | Página de búsqueda tracking, código inexistente muestra error |
| **Total** | **9** | |

#### Requisitos

- App corriendo (Docker: `docker compose up -d` o Vite: `npm run dev`)
- URL configurable vía `E2E_BASE_URL` (default: `http://localhost:8090`)

#### Ejecutar tests

```bash
cd frontend-react

# Instalar navegador (solo primera vez)
npx playwright install chromium

# Todos los tests E2E (headless)
npm run e2e

# Con UI interactiva
npm run e2e:ui

# Con navegador visible
npm run e2e:headed
```

#### CI

Los tests E2E tienen un job definido en `ci.yml` pero está deshabilitado por defecto (`if: false`) porque requiere base de datos MySQL y Spring Boot corriendo. Para habilitarlo en CI, cambiar `if: false` a `if: true` en el workflow.

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