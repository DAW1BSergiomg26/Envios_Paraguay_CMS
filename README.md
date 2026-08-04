# 🌐 MONTEASTUR ENVIOS

> Plataforma logística profesional para envíos internacionales **España ↔ Paraguay**.

[![CI](https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS/actions/workflows/ci.yml/badge.svg)](https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-25-%23ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-%236DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-%2361DAFB?logo=react&logoColor=white)](https://react.dev/)
[![Docker](https://img.shields.io/badge/Docker-%232496ED?logo=docker&logoColor=white)](https://docker.com/)

---

## 📌 Índice

- [✨ Sistema de diseño Asturias-Paraguay](#-sistema-de-diseño-asturias-paraguay)
- [🚀 Características](#-características)
- [🧱 Stack tecnológico](#-stack-tecnológico)
- [🏗️ Arquitectura](#️-arquitectura)
- [📁 Estructura del proyecto](#-estructura-del-proyecto)
- [▶️ Arranque rápido](#️-arranque-rápido)
- [⚙️ Variables de entorno](#️-variables-de-entorno)
- [🔗 URLs importantes](#-urls-importantes)
- [🔐 Seguridad](#-seguridad)
- [📡 API REST v1](#-api-rest-v1)
- [📤 Uploads](#-uploads)
- [📜 Logging](#-logging)
- [📱 PWA, Push y Offline](#-pwa-push-y-offline)
- [💚 Healthchecks](#-healthchecks)
- [🧪 Testing](#-testing)
- [🔄 CI/CD](#-cicd)
- [📊 Monitoring y Observabilidad](#-monitoring-y-observabilidad)
- [🐳 Docker producción](#-docker-producción)
- [💾 Backup y Restore](#-backup-y-restore)
- [🖥️ Producción VPS](#️-producción-vps)
- [🛡️ Hardening VPS](#️-hardening-vps)
- [🚀 Primer deploy VPS](#-primer-deploy-vps)
- [🔑 GitHub Secrets + SSH](#-github-secrets--ssh)
- [🌐 Dominio + HTTPS](#-dominio--https)
- [✅ Deploy checklists](#-deploy-checklists)
- [📚 Guías y documentación](#-guías-y-documentación)
- [🆘 Troubleshooting](#-troubleshooting)
- [🗺️ Roadmap](#️-roadmap)

---

## ✨ Sistema de diseño Asturias-Paraguay

El proyecto cuenta con un **sistema de diseño propio** llamado **Asturias-Paraguay**, que fusiona el **verde bosque profundo** de Asturias con los **acentos cálidos** de Paraguay. Es el corazón visual de la plataforma y se aplica en todas las páginas públicas (premium) y paneles.

| Concepto                                            | Valor                             |
| --------------------------------------------------- | --------------------------------- |
| **Verde bosque profundo** (fondos oscuros)          | `#0D2319` · `#153C2D` · `#1B4D3B` |
| **Acento cálido** (acciones, CTA, destacados)       | `#E67E22`                         |
| **Ámbar secundario**                                | `#F59E0B`                         |
| **Hover / active** del acento                       | `#D97706` / `#B45309`             |
| **Textos oscuros** (principal / secundario / muted) | `#F4F7F5` · `#A3C9B8` · `#7BA897` |
| **Tema claro** (fondos / superficies)               | `#f8fafc` / `#ffffff`             |

**Implementación:**

- Tokens de diseño como variables CSS en `src/main/resources/static/css/design-system.css`
- Tema oscuro y claro con **theme switcher** (persistencia de preferencia del usuario)
- Hojas de estilo "premium": `tracking-premium.css`, `casa-premium.css`, `hero-premium.css`, `reservas-premium-v2.css`, `operaciones-premium.css`, `contacto-premium.css`, `luxury-core.css`
- Semántica de estados: éxito `#4ade80`, warning `#f0a830`, danger `#f87171`, info `#a5b4fc`

> 🎨 Identidad corporativa: el acento corporativo del proyecto es `#d4762a`.

---

## 🚀 Características

| ✅ Característica              | 📝 Descripción                                           |
| ------------------------------ | -------------------------------------------------------- |
| 📦 **Tracking en tiempo real** | Seguimiento de envíos por código único                   |
| 🛡️ **Panel de administración** | Gestión completa de envíos, estados y evidencias         |
| 👤 **Panel de cliente seguro** | Acceso a los envíos propios con sesión propia            |
| 🖼️ **Evidencias y documentos** | Adjuntos por envío (imágenes de tracking, galería, etc.) |
| 🗂️ **Galería CMS**             | Gestión de imágenes de operaciones                       |
| ⏱️ **Seguimiento premium**     | Timeline visual de eventos                               |
| 📊 **Dashboard React SPA**     | Analytics con gráficos (Recharts)                        |
| 📱 **PWA instalable**          | Offline mode, push notifications, service worker         |

---

## 🧱 Stack tecnológico

### 🔙 Backend

| Tecnología                      | Uso                                          |
| ------------------------------- | -------------------------------------------- |
| **Java 17**                     | Lenguaje de programación                     |
| **Spring Boot 3.3.5**           | Framework backend                            |
| **Spring Security**             | Autenticación y autorización (sesión + CSRF) |
| **Thymeleaf**                   | Motor de plantillas MVC                      |
| **Spring Data JPA + Hibernate** | Persistencia                                 |
| **Actuator**                    | Monitoreo y healthchecks                     |
| **Logback**                     | Logging profesional con rotación             |
| **Maven 3.9+**                  | Build y dependencias                         |

### 🔜 Frontend

| Tecnología      | Uso                                     |
| --------------- | --------------------------------------- |
| **React 19**    | Dashboard SPA de administración         |
| **Vite 8**      | Bundler y dev server del frontend React |
| **Axios**       | Cliente HTTP del SPA                    |
| **Recharts**    | Gráficos del analytics dashboard        |
| **CSS modular** | Estilos organizados por funcionalidad   |
| **Bootstrap 5** | Base UI de las vistas Thymeleaf         |

### 🗄️ Base de datos y servicios

| Tecnología  | Uso                                              |
| ----------- | ------------------------------------------------ |
| **MySQL 8** | Base de datos relacional                         |
| **Flyway**  | Migraciones de esquema (`V{N}__descripcion.sql`) |
| **Redis 7** | Caché y soporte (entorno de tests/CI)            |
| **Mailpit** | Captura de correos en desarrollo                 |

### 🐳 DevOps y PWA

| Tecnología                  | Uso                                                        |
| --------------------------- | ---------------------------------------------------------- |
| **Docker + Docker Compose** | Contenerización y orquestación                             |
| **Nginx**                   | Reverse proxy, SSL termination, compression                |
| **Let's Encrypt / Certbot** | Certificados SSL automáticos                               |
| **Prometheus + Grafana**    | Métricas y dashboards                                      |
| **Uptime Kuma**             | Monitor de uptime auto-hospedado                           |
| **PWA**                     | Service Worker, manifest, push notifications, offline mode |

---

## 🏗️ Arquitectura

La aplicación sigue una arquitectura **MVC + REST API + SPA** con capas claramente separadas:

| Capa                      | Responsabilidad                                        |
| ------------------------- | ------------------------------------------------------ |
| **Controllers**           | Manejan peticiones HTTP y retornan vistas o datos      |
| **Services**              | Lógica de negocio y transacciones                      |
| **Repositories**          | Interfaz con la capa de persistencia (Spring Data JPA) |
| **Templates**             | Thymeleaf en `src/main/resources/templates`            |
| **Static resources**      | CSS, JS e imágenes en `src/main/resources/static`      |
| **Uploads**               | Almacén de archivos subidos (tracking, galería, etc.)  |
| **Logs**                  | Registros de la aplicación                             |
| **Configuración externa** | Variables de entorno y archivos `.properties`          |

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

El backend implementa un modelo híbrido: el frontend Thymeleaf sigue activo y es completamente funcional, mientras que el dashboard React SPA se sirve desde `/react-dashboard` y se comunica con la API REST `/api/v1/admin/` usando la **misma sesión Spring Security**. La API REST `/api/v1/` también está diseñada para apps móviles e integraciones externas. Todas las capas comparten los mismos servicios, repositorios y entidades JPA.

---

## 📁 Estructura del proyecto

```
src/main/
├── java/
│   └── com/monteastur/envios/
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
│       ├── css/            # Hojas de estilo modulares (design-system, premium…)
│       ├── js/             # JavaScript
│       └── img/            # Imágenes estáticas
uploads/                    # Almacén de archivos subidos (NO se sube a Git)
logs/                       # Archivos de log generados en tiempo de ejecución
docker-compose.yml          # Orquestación de servicios
Dockerfile                  # Definición de la imagen de la aplicación
nginx/                      # Configuración Nginx reverse proxy
scripts/                    # Scripts de deploy, backup y restore
docs/                       # Guías de producción y despliegue
backup/                     # Backups de BD y uploads
```

---

## ▶️ Arranque rápido

### ⚡ Arranque Docker 1-Click

El proyecto incluye un **script de arranque en un solo clic** para levantar toda la stack (DB + app + nginx + mailpit + redis + monitoring):

```powershell
.\start-app.ps1        # Windows (PowerShell)
```

```bat
start-app.bat          # Windows (doble clic)
```

> El script usa `-ExecutionPolicy Bypass`, lee las credenciales desde `.env` y valida que todo el stack quede UP.

### 🐳 Arranque con Docker Compose

```powershell
# 1. Preparar variables de entorno
cp .env.example .env

# 2. Editar .env con credenciales de producción (cambiar contraseñas por defecto)

# 3. Construir y levantar
docker compose up -d --build
```

La aplicación estará disponible en el puerto definido en `PORT` (por defecto 8080 en producción).

### 💻 Arranque local (Maven)

```powershell
# Paso 1: Arrancar MySQL Docker (si no existe crear con:)
docker run -d --name monteastur-mysql -p 3307:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=casarural mysql:8.0
# (si ya existe) docker start monteastur-mysql

# Paso 2: Configurar variables de entorno (PowerShell)
$env:PORT="8895"
$env:DB_DDL_AUTO="update"
$env:JPA_SHOW_SQL="true"
$env:UPLOAD_DIR="./uploads"
$env:ADMIN_USERNAME="admin"
$env:ADMIN_PASSWORD="admin123"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3307/casarural?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="root"

# Paso 3: Ejecutar
mvn spring-boot:run
```

La aplicación estará disponible en: `http://localhost:8895`.

### 🧾 Requisitos

| Requisito          | Detalle                                              |
| ------------------ | ---------------------------------------------------- |
| **Java**           | 17+ (se recomienda JDK 17 LTS)                       |
| **Maven**          | 3.9+                                                 |
| **Docker Desktop** | Para contenedores                                    |
| **MySQL**          | 8 (accesible vía `localhost:3307` o servicio Docker) |
| **Git**            | Control de versiones                                 |

### 🔑 Credenciales desarrollo local

| Rol                      | URL login                             | Usuario                         | Contraseña |
| ------------------------ | ------------------------------------- | ------------------------------- | ---------- |
| Admin (Spring Security)  | `http://localhost:8090/login`         | `admin`                         | `admin123` |
| Cliente (custom session) | `http://localhost:8090/cliente/login` | email: `cliente@monteastur.com` | `demo2026` |
| React SPA                | `http://localhost:8090/login-react`   | `admin`                         | `admin123` |
| Grafana                  | `http://localhost:3001`               | `admin`                         | `admin123` |

> ⚠️ **IMPORTANTE:** Estas credenciales son **SOLO para desarrollo local**. En producción, generar contraseñas seguras con `openssl rand -base64 32` y configurarlas vía variables de entorno.

### 🧪 Demo Data

Cuando `APP_DEMO_DATA=true` (valor por defecto en `.env` local), al iniciar la aplicación se cargan automáticamente:

- **Cliente demo**: `cliente@monteastur.com` / `demo2026` (María González)
- **4 envíos demo**: MT-2026-0001 a MT-2026-0004, con historial de eventos y estados variados (en tránsito, aduana, reparto, entregado)
- **4 mensajes de contacto**: para que `/admin/mensajesrecibidos` tenga contenido
- **4 reservas/solicitudes**: con estados pendiente, confirmada y cancelada
- **4 imágenes demo**: SVG estáticos en `/img/demo-gallery/` (oficinas, flota, almacén, puerto). Las subidas reales siguen usando `/uploads/`
- **Textos legales**: aviso legal y política de cookies

> **Persistencia de datos:**
>
> - `docker compose down` — borra contenedores **sin** borrar datos (volúmenes intactos)
> - `docker compose down -v` — borra contenedores **y** volúmenes (incluyendo MySQL). Al arrancar de nuevo, `APP_DEMO_DATA=true` repuebla automáticamente todos los datos demo
> - Si se añaden datos reales durante el desarrollo, evitar `docker compose down -v` para no perderlos

---

## ⚙️ Variables de entorno

| Variable                 | Descripción                           | Valor por defecto (dev) | Comentario                                     |
| ------------------------ | ------------------------------------- | ----------------------- | ---------------------------------------------- |
| `PORT`                   | Puerto del servidor HTTP              | 8081                    | En producción suele ser 8080                   |
| `NGINX_PORT`             | Puerto del proxy Nginx                | 80                      | Requiere sudo en Linux si < 1024               |
| `DB_DDL_AUTO`            | Estrategia de actualización de schema | update                  | En producción usar `validate`                  |
| `JPA_SHOW_SQL`           | Mostrar SQL en consola                | true                    | En producción usar `false`                     |
| `UPLOAD_DIR`             | Directorio para archivos subidos      | ./uploads               | En producción: `/app/uploads` (volumen Docker) |
| `LOG_DIR`                | Directorio para archivos de log       | ./logs                  |                                                |
| `ADMIN_USERNAME`         | Usuario de acceso al panel admin      | admin                   | Cambiar en producción                          |
| `ADMIN_PASSWORD`         | Contraseña del panel admin            | admin123                | **Obligatorio cambiar en producción**          |
| `DB_USERNAME`            | Usuario de MySQL                      | root                    |                                                |
| `DB_PASSWORD`            | Contraseña de MySQL                   | (vacía)                 |                                                |
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring activo               | (vacío)                 | En producción: `prod`                          |
| `APP_DEMO_DATA`          | Cargar datos demo al arrancar         | true                    | Desactivar en producción                       |

Plantilla de producción completa: [`.env.production.example`](.env.production.example).

---

## 🔗 URLs importantes

### 🌍 Frontend (público)

| Ruta           | Descripción                      |
| -------------- | -------------------------------- |
| `/`            | Página de inicio                 |
| `/seguimiento` | Seguimiento de envíos por código |
| `/reservas`    | Formulario y gestión de envíos   |
| `/contacto`    | Formulario de contacto           |

### 🛡️ Panel de Admin

| Ruta               | Descripción                           |
| ------------------ | ------------------------------------- |
| `/login`           | Inicio de sesión (admin y cliente)    |
| `/admin/dashboard` | Panel principal de administración     |
| `/admin/tracking`  | Gestión de envíos y tracking          |
| `/admin/imagenes`  | Galería y gestión de imágenes del CMS |

### 👤 Panel de Cliente

| Ruta             | Descripción                         |
| ---------------- | ----------------------------------- |
| `/cliente/login` | Acceso específico para clientes     |
| `/cliente/panel` | Panel de cliente tras login exitoso |

### ⚛️ React Dashboard (SPA)

| Ruta                                       | Descripción                                |
| ------------------------------------------ | ------------------------------------------ |
| `/react-dashboard`                         | Dashboard React SPA de administración      |
| `/react-dashboard/dashboard/envio/:codigo` | Detalle de envío con timeline y evidencias |

### 📊 Actuator (monitoreo)

| Ruta                   | Descripción                                          |
| ---------------------- | ---------------------------------------------------- |
| `/actuator/health`     | Estado de salud de la aplicación                     |
| `/actuator/info`       | Información de la aplicación (nombre, versión, etc.) |
| `/actuator/prometheus` | Métricas Prometheus (formato texto)                  |

---

## 🔐 Seguridad

### Pilares generales

- **BCrypt para clientes**: contraseñas hasheadas, nunca en texto plano
- **Admin externalizado**: credenciales por variables de entorno (nada hardcoded)
- **Variables de entorno**: toda configuración sensible pasa por entorno
- **`.env` ignorado**: está en `.gitignore` para evitar subir credenciales a Git
- **Uploads fuera del jar**: los archivos subidos viven en el sistema de archivos
- **Logs separados**: archivos externos, rotados diariamente
- **Actuator seguro**: solo `health` e `info`, detalles de salud requieren autenticación

### Seguridad SPA + API REST

El SPA React **no utiliza JWT**. Reutiliza la sesión de Spring Security mediante la cookie **JSESSIONID** (HttpOnly, Secure en producción), evitando la complejidad de gestión de tokens sin sacrificar seguridad.

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

| Contexto              | CSRF             | Motivo                                                                                                                                                                                                                                                                          |
| --------------------- | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Formularios Thymeleaf | ✅ Activo        | Protección estándar contra CSRF en formularios HTML                                                                                                                                                                                                                             |
| APIs `/api/**`        | ❌ Deshabilitado | La sesión ya está protegida por cookie HttpOnly; el SPA no puede leer JSESSIONID desde JavaScript. Sin CSRF token se evitan errores 403 en PUT/POST del SPA sin sacrificar seguridad real, ya que un atacante no puede leer la cookie JSESSIONID ni fabricar una sesión válida. |

**Decisión técnica documentada en**: `SecurityConfig.java` (javadoc de clase y comentarios en `filterChain`).

#### Cookie de sesión

- **HttpOnly**: `true` — no accesible desde JavaScript (`document.cookie`)
- **Secure**: `true` en producción (`application-prod.properties`)
- **SameSite**: Lax (default Spring Security) — evita envío en peticiones cross-site

#### Protección de rutas

| Ruta                  | Protección                                                 |
| --------------------- | ---------------------------------------------------------- |
| `/` (home)            | Pública                                                    |
| `/seguimiento`        | Pública                                                    |
| `/api/v1/tracking/**` | Pública                                                    |
| `/api/v1/cliente/**`  | Sesión requerida (403 si no autenticado)                   |
| `/admin/**`           | Spring Security (redirect a `/login` si no autenticado)    |
| `/api/v1/admin/**`    | Spring Security (redirect a `/login` si no autenticado)    |
| `/react-dashboard/**` | Pública (el SPA protege internamente con `ProtectedRoute`) |

#### Seguridad del SPA React

- **No almacena credenciales**: la sesión vive en el servidor, no en localStorage ni sessionStorage
- **No expone tokens**: no hay JWT que puedan ser interceptados por XSS
- **Logout**: `POST /logout` con CSRF invalida la sesión del lado del servidor
- **ProtectedRoute**: componente React que redirige a `/login-react` si no hay sesión activa
- **AuthContext**: verifica la sesión al montar la aplicación (`GET /api/v1/admin/envios?page=0&size=1`)
- **Interceptores Axios**: detectan respuestas HTML (login page) y muestran "Necesitas iniciar sesión como admin"

---

## 📡 API REST v1

La aplicación expone una API REST bajo `/api/v1/` para integración con sistemas externos, apps móviles y futuros frontends SPA. La API convive con el frontend Thymeleaf actual. Los endpoints de tracking son públicos; los de cliente y admin reutilizan la sesión existente.

### 🔓 API Pública — Tracking

No requiere autenticación.

**GET `/api/v1/tracking/{codigo}`**

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

### 👤 API Cliente — Envíos propios

Requiere sesión de cliente activa (login en `/cliente/login`). Reutiliza la misma cookie de sesión.

**GET `/api/v1/cliente/envios`**

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

**GET `/api/v1/cliente/envios/{codigo}`**

```bash
curl http://localhost:8895/api/v1/cliente/envios/MT-2026-0001
```

- Respuesta 200: ídem TrackingDto completo con eventos y evidencias visibles
- Respuesta 403 (envío ajeno o sin sesión): `{"timestamp": "...", "status": 403, "error": "Acceso denegado"}`
- Respuesta 404: `{"timestamp": "...", "status": 404, "error": "Tracking no encontrado"}`

### 🛡️ API Admin — Gestión de envíos

Requiere sesión de administrador (Spring Security, login en `/login`). Reutiliza la misma cookie de sesión.

**GET `/api/v1/admin/envios`** — lista paginada con filtros y ordenación.

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

| Parámetro | Tipo   | Default                    | Descripción                                                       |
| --------- | ------ | -------------------------- | ----------------------------------------------------------------- |
| `page`    | int    | 0                          | Número de página (zero-based)                                     |
| `size`    | int    | 20                         | Elementos por página                                              |
| `estado`  | string | —                          | Filtro exacto por estado (RECIBIDO, EN_TRANSITO, ENTREGADO, etc.) |
| `codigo`  | string | —                          | Búsqueda parcial por código único                                 |
| `sort`    | string | `ultimaActualizacion,desc` | Campo y dirección de ordenación                                   |

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
  "sort": { "sorted": true, "unsorted": false, "empty": false },
  "first": true,
  "last": false,
  "empty": false
}
```

**Ventajas de la paginación:**

- Escalabilidad: consultas optimizadas con LIMIT/OFFSET en base de datos
- Dashboards grandes: carga progresiva sin bloquear la interfaz
- Apps móviles: respuestas ligeras con tamaños de página reducidos
- Tablas dinámicas: integración con DataTables, AG Grid, etc.
- Optimización backend: evita cargar miles de registros en memoria

**GET `/api/v1/admin/envios/{codigo}`**

```bash
curl http://localhost:8895/api/v1/admin/envios/MT-2026-0001
```

Respuesta 200: TrackingDto completo con datos del cliente, eventos del timeline y todas las evidencias.

**PUT `/api/v1/admin/envios/{codigo}/estado`** — actualiza el estado y crea automáticamente un evento de tracking en el timeline.

```bash
curl -X PUT http://localhost:8895/api/v1/admin/envios/MT-2026-0001/estado \
  -H "Content-Type: application/json" \
  -d '{"estado":"EN_TRANSITO"}'
```

Respuesta 200: TrackingDto completo actualizado.
Respuesta 404: `{"timestamp": "...", "status": 404, "error": "Tracking no encontrado"}`

### Códigos de estado HTTP

| Código                    | Descripción                                      |
| ------------------------- | ------------------------------------------------ |
| 200 OK                    | Petición exitosa                                 |
| 403 Forbidden             | Acceso denegado (sesión no válida o envío ajeno) |
| 404 Not Found             | Recurso no encontrado                            |
| 500 Internal Server Error | Error interno del servidor                       |

---

## 📤 Uploads

- **Carpeta local**: en desarrollo, los archivos se guardan en `./uploads` relativo al directorio de ejecución
- **Persistencia**: las imágenes de tracking, galería del CMS y evidencias se guardan permanentemente
- **Backups recomendados**: realizar copias de seguridad periódicas de `uploads/`, ya que contiene:
  - Imágenes de seguimiento de envíos
  - Galería de operaciones del CMS
  - Evidencias y documentos adjuntos

---

## 📜 Logging

El sistema de logging usa **Logback**:

| Archivo                     | Contenido                                     |
| --------------------------- | --------------------------------------------- |
| `logs/monteastur.log`       | Log general (nivel INFO y superior)           |
| `logs/monteastur-error.log` | Solo advertencias y errores (WARN y superior) |

- **Rotación**: diaria (nuevo archivo a medianoche)
- **Retención**: 30 días (eliminación automática de los más antiguos)
- **Directorio configurable**: variable de entorno `LOG_DIR` (por defecto `./logs`)

---

## 📱 PWA, Push y Offline

### 📱 PWA (Progressive Web App)

El dashboard React SPA es una **PWA instalable**:

- **Instalable**: manifest.webmanifest con iconos SVG 192/512
- **Service Worker**: registrado con Workbox, precache de 12 entradas (~980KB)
- **Offline fallback**: el SW sirve la app incluso sin conexión (navigateFallback)
- **Push Notifications**: suscripción y recepción en el navegador

**Cómo instalar:**

1. Abrir el dashboard en Chrome/Edge (`https://dominio/react-dashboard`)
2. Click en el icono de instalación de la barra del navegador
3. O usar el botón "Instalar App" en el navbar del dashboard

### 🔔 Push Notifications

- **Backend**: `POST /api/v1/push/subscribe` (guarda suscripción)
- **Frontend**: hook `usePushNotifications.js` (solicita permiso, suscribe/desuscribe)
- **Service Worker**: manejador de eventos `push` y `notificationclick`
- **Demo**: endpoint de prueba `POST /api/v1/push/test`

| Estado       | Significado                      |
| ------------ | -------------------------------- |
| 🔔 Activo    | Notificaciones habilitadas       |
| 🔕 Inactivo  | No suscrito (click para activar) |
| 🚫 Bloqueado | Permiso denegado en el navegador |

### 📴 Offline Mode

La aplicación funciona parcialmente sin conexión:

- **OfflineBanner**: banner sticky "Estás sin conexión"
- **Cache de datos**: dashboard y detalle de envíos cacheados en localStorage
- **Cola offline**: cambios de estado encolados sin conexión
  - Se procesan automáticamente al recuperar conexión
  - Deduplicación por código + estado
  - Toast de confirmación al sincronizar
- **Indicador visual**: "Mostrando datos offline" cuando se usa cache

---

## 💚 Healthchecks

**GET `/actuator/health`** debe devolver:

```json
{ "status": "UP" }
```

Este endpoint es utilizado por Docker Compose y orquestadores para verificar que la aplicación funciona correctamente.

```bash
curl -f http://localhost/actuator/health        # {"status":"UP"}
curl http://localhost/actuator/info             # Info app
curl http://localhost:9090/targets              # Prometheus targets
```

---

## 🧪 Testing

### 🔙 Backend — JUnit 5 + Mockito

| Herramienta           | Uso                                              |
| --------------------- | ------------------------------------------------ |
| **JUnit 5 (Jupiter)** | Framework de testing                             |
| **Mockito**           | Mocking de dependencias                          |
| **Spring Boot Test**  | Contexto de aplicación para tests de integración |
| **MockMvc**           | Testing de controladores HTTP                    |

**Tests incluidos:**

| Clase                            | Tipo                                            | Dependencias                         |
| -------------------------------- | ----------------------------------------------- | ------------------------------------ |
| `TrackingApiControllerTest`      | `@WebMvcTest`                                   | Mock de `EnvioTrackingRepository`    |
| `ReservaServiceTest`             | `@ExtendWith(MockitoExtension.class)`           | Mock de `ReservaRepository`          |
| `PushSubscriptionControllerTest` | `@WebMvcTest`                                   | Sin dependencias externas            |
| `SecurityConfigTest`             | `@WebMvcTest` + `@Import(SecurityConfig.class)` | Verifica rutas públicas y protegidas |

> La suite completa del proyecto cuenta con **233 tests** (BUILD SUCCESS verificada en Docker).

**Ejecutar:**

```bash
mvn test                                # Todos los tests
mvn test -Dtest=TrackingApiControllerTest  # Test específico
mvn clean package                       # Build completo (test + package)
```

### 🔜 Frontend React — Vitest + React Testing Library

| Herramienta                     | Uso                                       |
| ------------------------------- | ----------------------------------------- |
| **Vitest**                      | Test runner compatible con Vite           |
| **@testing-library/react**      | Renderizado e interacción con componentes |
| **@testing-library/jest-dom**   | Matchers personalizados para el DOM       |
| **@testing-library/user-event** | Simulación realista de eventos de usuario |
| **jsdom**                       | Entorno DOM simulado                      |

| Componente    | Tests  | Qué prueba                                                              |
| ------------- | ------ | ----------------------------------------------------------------------- |
| `LoginPage`   | 4      | Renderiza formulario, login exitoso navega, login fallido muestra error |
| `StatsCard`   | 2      | Renderiza label/valor, soporta icono y color                            |
| `StatusBadge` | 3      | Formatea estado: EN_TRANSITO, ENTREGADO, N/A                            |
| `EmptyState`  | 2      | Mensaje por defecto y personalizado                                     |
| `SearchBar`   | 4      | Placeholder, debounce 300ms, botón clear                                |
| **Total**     | **15** |                                                                         |

```bash
cd frontend-react
npm test                 # Todos los tests (una vez)
npm run test:watch       # Modo watch (desarrollo)
npm run test:coverage    # Con cobertura
```

### 🌐 E2E — Playwright

| Archivo                 | Tests | Qué prueba                                            |
| ----------------------- | ----- | ----------------------------------------------------- |
| `e2e/home.spec.js`      | 2     | Página principal carga sin errores, contenido visible |
| `e2e/login.spec.js`     | 3     | Formulario login, login admin exitoso, login fallido  |
| `e2e/dashboard.spec.js` | 2     | Dashboard tras login, tabla de envíos visible         |
| `e2e/tracking.spec.js`  | 2     | Búsqueda tracking, código inexistente muestra error   |
| **Total**               | **9** |                                                       |

**Requisitos:** app corriendo (Docker o Vite) y URL vía `E2E_BASE_URL` (default `http://localhost:8090`).

```bash
cd frontend-react
npx playwright install chromium
npm run e2e                # Headless
npm run e2e:ui             # Con UI interactiva
npm run e2e:headed         # Con navegador visible
```

**E2E local en un comando:**

```powershell
.\scripts\run-e2e-local.ps1     # Windows
```

```bash
./scripts/run-e2e-local.sh      # Linux/macOS
```

---

## 🔄 CI/CD

El proyecto usa [GitHub Actions](https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS/actions). El pipeline se ejecuta automáticamente en cada push a `develop` o `feature/*`, y en cada PR hacia `develop`.

### Jobs

| Job              | Comando                         | Artefacto                             |
| ---------------- | ------------------------------- | ------------------------------------- |
| `backend-build`  | `mvn clean package -DskipTests` | `backend-jar` (target/\*.jar)         |
| `frontend-build` | `npm install` → `npm run build` | `frontend-dist` (frontend-react/dist) |
| `docker-build`   | `docker compose build`          | —                                     |

Los artefactos (`backend-jar`, `frontend-dist`) están disponibles para descarga en cada ejecución.

### CD Automático VPS

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

La conexión SSH se realiza con **clave privada** — no se usan contraseñas.

**Secrets requeridos:** `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, `VPS_PORT` (opcional, default 22). Configurar en **Settings → Secrets and variables → Actions**.

**Seguridad:**

- La clave SSH se almacena cifrada en GitHub Secrets, nunca en el repo
- El deploy solo se ejecuta en pushes a `develop` (no en PRs ni `feature/*`)
- `set -e` en los comandos remotos — si algo falla, el deploy se detiene
- Las credenciales de BD y admin se configuran en el `.env` del VPS, no en GitHub

**Rollback manual:**

```bash
ssh user@vps
cd /opt/monteastur
git checkout <commit-anterior>
docker compose up -d --build
```

### Deploy manual desde GitHub Actions

El workflow [`deploy-prod.yml`](.github/workflows/deploy-prod.yml) permite desplegar a producción manualmente: **GitHub → Actions → Deploy Production → Run workflow** (branch `develop`, escribir `deploy`).

| Job                 | Descripción                                                             |
| ------------------- | ----------------------------------------------------------------------- |
| `pre-deploy-check`  | Valida `docker compose config`, `mvn test`, `npm test`, `npm run build` |
| `deploy-production` | SSH al VPS, git pull, `./scripts/deploy-prod.sh`                        |
| `notify-failure`    | Muestra instrucciones de rollback si falla                              |

**Protecciones:** solo branch `develop`, confirmación escribiendo "deploy", `fail-fast`, timeout 15min (validación) + 20min (deploy).

---

## 📊 Monitoring y Observabilidad

Stack: **Spring Boot Actuator + Prometheus + Grafana + Uptime Kuma**.

### Servicios Docker

| Servicio    | Puerto por defecto | Descripción                                                      |
| ----------- | ------------------ | ---------------------------------------------------------------- |
| Prometheus  | `9090`             | Recolecta métricas cada 15s desde `app:8080/actuator/prometheus` |
| Grafana     | `3000`             | Dashboards con datasource Prometheus auto-configurado            |
| Uptime Kuma | `3001`             | Monitor de uptime auto-hospedado                                 |

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

> Si las credenciales antiguas persisten (volumen con datos previos):
>
> ```bash
> docker compose down
> docker volume rm envios_paraguay_cms_grafana_data
> docker compose up -d
> ```
>
> Esto borra la base interna de Grafana; los dashboards y datasources provisionados se recargan automáticamente.

### Dashboard Grafana

El dashboard **Monteastur Envios** se importa automáticamente al iniciar Grafana.

| Sección     | Paneles                                                                       |
| ----------- | ----------------------------------------------------------------------------- |
| **Estado**  | App Status (UP/DOWN), Uptime, CPU Usage, Threads, Active Sessions, Log Errors |
| **Memoria** | Heap Used, Heap Max, Heap Usage %, Non-Heap Used                              |
| **JVM**     | JVM Memory (time series), Garbage Collection rate                             |
| **HTTP**    | Requests/min, Latencia media, 4xx/min, 5xx/min                                |

Para validar que Prometheus recibe datos:

- `http://localhost:9090/targets` → `app:8080` debe aparecer como `UP`
- `http://localhost:9090/graph` → ejecutar `up{application="monteastur-envios"}`

### Alertas Grafana (auto-provisionadas)

| Alerta                     | Condición     | Severidad   | Tiempo |
| -------------------------- | ------------- | ----------- | ------ |
| **App Down**               | `up == 0`     | 🔴 critical | 1 min  |
| **High CPU**               | `cpu > 90%`   | 🟡 warning  | 5 min  |
| **High Heap**              | `heap > 90%`  | 🟡 warning  | 5 min  |
| **High 5xx Rate**          | `5xx > 5/min` | 🔴 critical | 5 min  |
| **Prometheus Target Down** | `up == 0`     | 🔴 critical | 1 min  |

Se evalúan cada 30s y se agrupan por nombre y severidad cada 5 min. **Para notificaciones reales:** configurar SMTP en el servicio Grafana de `docker-compose.yml` y editar `monitoring/grafana/provisioning/alerting/contactpoints.yml`.

### Uptime Monitoring

| Componente          | Rol                              | Acceso                    |
| ------------------- | -------------------------------- | ------------------------- |
| **Uptime Kuma**     | Monitor de uptime auto-hospedado | `http://localhost:3001`   |
| **Healthchecks.io** | Heartbeat externo (documentado)  | `https://healthchecks.io` |

Configuración detallada en `docs/UPTIME_MONITORING.md`.

---

## 🐳 Docker producción

### Contenedores

| Contenedor         | Imagen           | Puerto expuesto | Función                         |
| ------------------ | ---------------- | --------------- | ------------------------------- |
| `monteastur-nginx` | `nginx:alpine`   | 80 / 443        | Reverse proxy, SSL, compression |
| `monteastur-app`   | `monteastur-app` | 8080 (interno)  | Spring Boot + React SPA         |
| `monteastur-mysql` | `mysql:8.0`      | — (interno)     | Base de datos                   |

### Volúmenes persistentes

| Volumen        | Mount point        | Contenido              |
| -------------- | ------------------ | ---------------------- |
| `mysql_data`   | `/var/lib/mysql`   | Datos de MySQL         |
| `uploads_data` | `/app/uploads`     | Imágenes subidas       |
| `logs_data`    | `/app/logs`        | Logs de la aplicación  |
| `certbot_www`  | `/var/www/certbot` | Desafíos Let's Encrypt |

### Nginx Reverse Proxy

Nginx actúa como puerta de entrada única añadiendo:

- **Terminación SSL** (cuando se configura HTTPS)
- **Security headers**: HSTS, CSP, X-Frame-Options, Permissions-Policy
- **Compresión gzip** de assets estáticos
- **Proxy pass** a Spring Boot en `http://app:8080`
- **Límite de tamaño** de subida: 10MB
- **Preparado para WebSocket** (futuro)

Configuración en `nginx/conf.d/`:

- **`local.conf`**: HTTP sin SSL, `server_name localhost`. Uso en desarrollo local.
- **`monteastur.conf`**: HTTP con security headers, `server_name _` (catch-all). Producción antes de SSL.
- **`examples/production-example.conf`**: plantilla completa HTTPS para producción (copiar a `conf.d/` cuando haya certificados).

---

## 💾 Backup y Restore

Guía completa: [`docs/BACKUP_RECOVERY.md`](docs/BACKUP_RECOVERY.md).

### Scripts disponibles

```bash
./scripts/backup-db.sh                 # → backup/db/YYYY-MM-DD_HH-mm.sql.gz
./scripts/restore-db.sh backup/db/2026-05-23_14-00.sql.gz
./scripts/backup-uploads.sh            # → backup/uploads/YYYY-MM-DD_HH-mm.tar.gz
./scripts/restore-uploads.sh backup/uploads/2026-05-23_14-00.tar.gz
```

Versiones PowerShell para Windows disponibles en `scripts/*.ps1`.

### Automatización (cron)

```cron
0 3 * * * /opt/monteastur/scripts/backup-db.sh
0 4 * * * /opt/monteastur/scripts/backup-uploads.sh
0 5 * * * find /opt/monteastur/backup -name "*.sql.gz" -mtime +30 -delete
```

---

## 🖥️ Producción VPS

Guía completa: [`docs/PRODUCTION_VPS_RUNBOOK.md`](docs/PRODUCTION_VPS_RUNBOOK.md).

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

| Script                            | Función                                                         |
| --------------------------------- | --------------------------------------------------------------- |
| `scripts/vps-bootstrap.sh`        | Instala Docker, Docker Compose, crea directorios, configura UFW |
| `scripts/deploy-prod.sh`          | Git pull, build, up -d, image prune, healthcheck                |
| `scripts/rollback-prod.sh <tag>`  | Git checkout a tag, rebuild, healthcheck                        |
| `scripts/backup-db.sh`            | Backup MySQL → `backup/db/`                                     |
| `scripts/backup-uploads.sh`       | Backup uploads → `backup/uploads/`                              |
| `scripts/restore-db.sh`           | Restore MySQL desde backup                                      |
| `scripts/restore-uploads.sh`      | Restore uploads desde backup                                    |
| `scripts/server-healthcheck.sh`   | Reporta uptime, disco, RAM, Docker, healthcheck                 |
| `scripts/check-ssh-connection.sh` | Verifica conexión SSH desde local                               |

### Monitoring

| Servicio    | Puerto | Acceso                                        |
| ----------- | ------ | --------------------------------------------- |
| Prometheus  | 9090   | `http://<vps>:9090`                           |
| Grafana     | 3000   | `http://<vps>:3000` (admin / pass desde .env) |
| Uptime Kuma | 3001   | `http://<vps>:3001`                           |

---

## 🛡️ Hardening VPS

Guía completa: [`docs/VPS_HARDENING_CHECKLIST.md`](docs/VPS_HARDENING_CHECKLIST.md).

| Medida                                 | Estado                  |
| -------------------------------------- | ----------------------- |
| SSH: sin root, sin contraseñas         | ✅ Documentado          |
| UFW: puertos mínimos (22, 80, 443)     | ✅ Documentado + script |
| fail2ban: protección fuerza bruta      | ✅ Documentado          |
| unattended-upgrades: seguridad auto    | ✅ Documentado          |
| Docker: restart, healthchecks, límites | ✅ Implementado         |
| Backups: BD, uploads, .env             | ✅ Scripts listos       |
| Monitoring: Prometheus, Grafana, Kuma  | ✅ Implementado         |
| SSL: Let's Encrypt + renovación auto   | ✅ Documentado          |
| Security headers: CSP, HSTS, XFO       | ✅ Implementado         |

```bash
./scripts/server-healthcheck.sh    # Healthcheck rápido (uptime, disco, RAM, Docker)
```

---

## 🚀 Primer deploy VPS

Guía completa: [`docs/FIRST_VPS_DEPLOY_CHECKLIST.md`](docs/FIRST_VPS_DEPLOY_CHECKLIST.md).

### Proveedor recomendado

| Proveedor   | Plan | vCPU | RAM  | SSD   | Precio/mes |
| ----------- | ---- | ---- | ---- | ----- | ---------- |
| **Hetzner** | CX22 | 2    | 4 GB | 40 GB | **~€4.50** |

Alternativa económica: Contabo Cloud S (~€6.99/mes, 4 vCPU, 8 GB RAM, 200 GB SSD).

### Coste mensual estimado

| Concepto                | Coste                     |
| ----------------------- | ------------------------- |
| VPS Hetzner CX22        | ~€4.50                    |
| Dominio .com            | ~€0.83/mes (~€10/año)     |
| SSL, Monitoring, Uptime | €0 (auto-hospedado)       |
| **Total**               | **~€5.33/mes (~€69/año)** |

### Resumen checklist

- [ ] VPS contratado (Hetzner CX22 recomendado)
- [ ] DNS apuntando al VPS (registro A)
- [ ] `.env` configurado con credenciales seguras
- [ ] GitHub Secrets: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`
- [ ] HTTPS con Let's Encrypt funcionando
- [ ] `docker ps` → 6/6 containers UP
- [ ] `curl -f /actuator/health` → `{"status":"UP"}`
- [ ] Workflow manual ejecutado desde GitHub Actions

---

## 🔑 GitHub Secrets + SSH

Guía completa: [`docs/GITHUB_SECRETS_SSH_SETUP.md`](docs/GITHUB_SECRETS_SSH_SETUP.md).

### Secrets necesarios

| Secret        | Descripción                | Ejemplo                 |
| ------------- | -------------------------- | ----------------------- |
| `VPS_HOST`    | IP o dominio del VPS       | `203.0.113.10`          |
| `VPS_USER`    | Usuario SSH                | `deploy`                |
| `VPS_SSH_KEY` | Clave privada (multilínea) | `-----BEGIN OPENSSH...` |
| `VPS_PORT`    | Puerto SSH (opcional)      | `22`                    |

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

### Troubleshooting básico

| Problema           | Solución                                                      |
| ------------------ | ------------------------------------------------------------- |
| Permission denied  | `ssh-copy-id` para añadir clave pública al VPS                |
| bad permissions    | `chmod 700 ~/.ssh && chmod 600 ~/.ssh/authorized_keys` en VPS |
| Connection refused | `sudo ufw status` (puerto 22); `systemctl status sshd`        |
| Host key changed   | `ssh-keygen -R <VPS_IP>` para limpiar cache                   |
| fail2ban bloqueó   | `sudo fail2ban-client set sshd unbanip <IP>` en VPS           |

---

## 🌐 Dominio + HTTPS

Guía completa: [`docs/DOMAIN_DNS_SSL_SETUP.md`](docs/DOMAIN_DNS_SSL_SETUP.md). Plantilla nginx en [`nginx/examples/production-example.conf`](nginx/examples/production-example.conf).

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

| Registro | Tipo | Valor      | TTL      |
| -------- | ---- | ---------- | -------- |
| `@`      | A    | IP del VPS | 300→3600 |
| `www`    | A    | IP del VPS | 300→3600 |

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

### Troubleshooting rápido

| Problema       | Solución                                    |
| -------------- | ------------------------------------------- |
| DNS no propaga | `dig @8.8.8.8 monteastur.com`, esperar TTL  |
| Certbot falla  | Verificar puerto 80 abierto y DNS propagado |
| Mixed Content  | Todos los assets deben servirse por HTTPS   |
| Redirect loop  | Cloudflare en modo "Full (strict)"          |

---

## ✅ Deploy checklists

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

| #   | Test                                   | Prioridad |
| --- | -------------------------------------- | --------- |
| 1   | Healthcheck endpoint `{"status":"UP"}` | 🔴 Alta   |
| 2   | Home page carga con security headers   | 🔴 Alta   |
| 3   | Tracking público funciona              | 🔴 Alta   |
| 4   | Login admin correcto                   | 🔴 Alta   |
| 5   | Login cliente correcto                 | 🔴 Alta   |
| 6   | Dashboard React SPA sin page errors    | 🟡 Media  |
| 7   | Upload/subida de imágenes              | 🟡 Media  |
| 8   | Monitoring (Prometheus, Grafana, Kuma) | 🟡 Media  |
| 9   | PWA instalable                         | 🟢 Baja   |
| 10  | SSL Labs grade A+                      | 🟢 Baja   |
| 11  | Mobile responsive                      | 🟢 Baja   |

> **Criterio:** Todos los 🔴 deben pasar. Si alguno falla, no considerar deploy exitoso.

```bash
# Scripts post-deploy
BASE_URL=https://monteastur.com ./scripts/production-smoke-test.sh
./scripts/production-post-deploy-check.sh
curl -f https://monteastur.com/actuator/health
```

### Rollback rápido

```bash
cd /opt/monteastur && ./scripts/rollback-prod.sh v14.0-e2e-ready
```

### Checklist de producción

- [ ] **Build OK**: `mvn clean package -DskipTests` finaliza sin errores
- [ ] **Docker OK**: `docker compose up -d` levanta todos los servicios
- [ ] **Health UP**: `curl http://localhost/actuator/health` → `{"status":"UP"}`
- [ ] **Uploads OK**: las imágenes se suben, almacenan y muestran correctamente
- [ ] **Login admin OK**: acceso al panel con credenciales de producción
- [ ] **Login cliente OK**: los clientes pueden autenticarse y acceder a su panel
- [ ] **Logs OK**: archivos en `logs/` sin errores de permisos
- [ ] **Backups probados**: restauración de BD y uploads desde backup
- [ ] **Variables entorno**: todas las variables en `.env` configuradas
- [ ] **Security headers**: `curl -I http://localhost` muestra HSTS, CSP, XFO
- [ ] **Nginx proxy**: sirve en puerto 80/443, proxy a `app:8080`
- [ ] **PWA instalable**: manifest y service worker funcionando
- [ ] **Offline mode**: dashboard funciona con datos cacheados sin conexión

---

## 📚 Guías y documentación

### 🔍 Auditoría y preproducción

| Documento                                                                  | Descripción                                                                      |
| -------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| [`docs/PREPRODUCTION_AUDIT_REPORT.md`](docs/PREPRODUCTION_AUDIT_REPORT.md) | Auditoría completa: servicios, rutas, tests, seguridad, riesgos y decisión final |
| [`docs/KNOWN_ISSUES_PREPROD.md`](docs/KNOWN_ISSUES_PREPROD.md)             | Issues conocidos con impacto, prioridad y solución                               |
| [`docs/LOCAL_DEV_COMMANDS.md`](docs/LOCAL_DEV_COMMANDS.md)                 | Comandos rápidos de desarrollo local (docker, logs, troubleshooting)             |
| [`docs/TESTING_STRATEGY.md`](docs/TESTING_STRATEGY.md)                     | Estrategia global de testing                                                     |
| [`docs/QA_REAL_EXECUTION_LOG.md`](docs/QA_REAL_EXECUTION_LOG.md)           | Registro de ejecución QA real                                                    |

### 🛡️ Hardening y seguridad

| Documento                                                                      | Descripción                                                          |
| ------------------------------------------------------------------------------ | -------------------------------------------------------------------- |
| [`docs/HARDENING_FINAL_REPORT.md`](docs/HARDENING_FINAL_REPORT.md)             | Informe: Spring Security, configuración prod, Docker, Nginx, riesgos |
| [`docs/HARDENING_PHASE_1_CLOSURE.md`](docs/HARDENING_PHASE_1_CLOSURE.md)       | Cierre fase 1 de hardening                                           |
| [`docs/HARDENING_BACKLOG_ENVIOS_CMS.md`](docs/HARDENING_BACKLOG_ENVIOS_CMS.md) | Backlog de hardening pendiente                                       |
| [`docs/VPS_HARDENING_CHECKLIST.md`](docs/VPS_HARDENING_CHECKLIST.md)           | Hardening VPS (SSH, UFW, fail2ban, upgrades)                         |
| [`docs/MONITORING_ACCESS_REVIEW.md`](docs/MONITORING_ACCESS_REVIEW.md)         | Revisión de accesos de monitoring                                    |

### 🧪 E2E, CI y testing

| Documento                                                          | Descripción                                 |
| ------------------------------------------------------------------ | ------------------------------------------- |
| [`docs/E2E_CI_GUIDE.md`](docs/E2E_CI_GUIDE.md)                     | Ejecutar, interpretar fallos, activar en CI |
| [`docs/QA_E2E_NIVEL_DIOS.md`](docs/QA_E2E_NIVEL_DIOS.md)           | QA E2E nivel dios                           |
| [`docs/SMOKE_TESTS_PRODUCTION.md`](docs/SMOKE_TESTS_PRODUCTION.md) | Smoke tests de producción                   |

### 🚀 Deploy y VPS

| Documento                                                                                  | Descripción                                     |
| ------------------------------------------------------------------------------------------ | ----------------------------------------------- |
| [`docs/DEPLOY_REAL_READY_CHECKLIST.md`](docs/DEPLOY_REAL_READY_CHECKLIST.md)               | Checklist final pre-deploy                      |
| [`docs/FINAL_PRODUCTION_DEPLOY_CHECKLIST.md`](docs/FINAL_PRODUCTION_DEPLOY_CHECKLIST.md)   | Checklist completa de producción                |
| [`docs/FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md`](docs/FIRST_REAL_DEPLOY_MASTER_CHECKLIST.md) | Checklist maestra 16 fases (A-P)                |
| [`docs/FIRST_VPS_DEPLOY_CHECKLIST.md`](docs/FIRST_VPS_DEPLOY_CHECKLIST.md)                 | Primer deploy VPS                               |
| [`docs/VPS_DEPLOY_GUIDE.md`](docs/VPS_DEPLOY_GUIDE.md)                                     | Guía de deploy VPS                              |
| [`docs/VPS_REAL_EXECUTION_GUIDE.md`](docs/VPS_REAL_EXECUTION_GUIDE.md)                     | Ejecución real: compra, SSH, Docker, HTTPS      |
| [`docs/PRODUCTION_VPS_RUNBOOK.md`](docs/PRODUCTION_VPS_RUNBOOK.md)                         | Runbook de producción                           |
| [`docs/VPS_DEPLOY_DAY_RUNBOOK.md`](docs/VPS_DEPLOY_DAY_RUNBOOK.md)                         | Runbook del día de deploy                       |
| [`docs/REAL_DEPLOY_DECISION_LOG.md`](docs/REAL_DEPLOY_DECISION_LOG.md)                     | Decisiones técnicas, proveedor, costes, riesgos |
| [`docs/FIRST_DEPLOY_RISK_REGISTER.md`](docs/FIRST_DEPLOY_RISK_REGISTER.md)                 | Registro de 18 riesgos                          |
| [`docs/REAL_DEPLOY_TIMELINE.md`](docs/REAL_DEPLOY_TIMELINE.md)                             | Plan 3 días con tiempos y costes                |
| [`docs/LIVE_DEPLOY_PLAN.md`](docs/LIVE_DEPLOY_PLAN.md)                                     | 15 pasos detallados                             |

### 💾 Backup, monitoreo y operaciones

| Documento                                                            | Descripción                   |
| -------------------------------------------------------------------- | ----------------------------- |
| [`docs/BACKUP_RECOVERY.md`](docs/BACKUP_RECOVERY.md)                 | Guía completa backup/restore  |
| [`docs/BACKUP_RESTORE_REVIEW.md`](docs/BACKUP_RESTORE_REVIEW.md)     | Revisión de restore           |
| [`docs/BACKUP_RETENTION_POLICY.md`](docs/BACKUP_RETENTION_POLICY.md) | Política de retención         |
| [`docs/UPTIME_MONITORING.md`](docs/UPTIME_MONITORING.md)             | Uptime Kuma y Healthchecks.io |
| [`docs/HTTPS_SETUP.md`](docs/HTTPS_SETUP.md)                         | Configuración HTTPS           |

### 🌐 Dominio, DNS y proveedores

| Documento                                                                  | Descripción                    |
| -------------------------------------------------------------------------- | ------------------------------ |
| [`docs/DOMAIN_DNS_SSL_SETUP.md`](docs/DOMAIN_DNS_SSL_SETUP.md)             | Dominio + DNS + SSL            |
| [`docs/HETZNER_VPS_PURCHASE_GUIDE.md`](docs/HETZNER_VPS_PURCHASE_GUIDE.md) | Compra VPS Hetzner CX22        |
| [`docs/DOMAIN_PURCHASE_GUIDE.md`](docs/DOMAIN_PURCHASE_GUIDE.md)           | Compra de dominio (Cloudflare) |
| [`docs/GITHUB_SECRETS_SSH_SETUP.md`](docs/GITHUB_SECRETS_SSH_SETUP.md)     | Secrets SSH para CD            |

### 🔑 Entorno y secretos

| Documento                                                                    | Descripción                               |
| ---------------------------------------------------------------------------- | ----------------------------------------- |
| [`docs/PRODUCTION_ENV_GUIDE.md`](docs/PRODUCTION_ENV_GUIDE.md)               | Configuración `.env` de producción        |
| [`docs/PRODUCTION_SECRETS_TEMPLATE.md`](docs/PRODUCTION_SECRETS_TEMPLATE.md) | Plantilla de secretos SIN valores reales  |
| [`.env.production.example`](.env.production.example)                         | Plantilla `.env` completa para producción |

### 🎯 Demo y preventa

| Documento                                                                          | Descripción                                |
| ---------------------------------------------------------------------------------- | ------------------------------------------ |
| [`docs/FREE_DEMO_DEPLOY_OPTIONS.md`](docs/FREE_DEMO_DEPLOY_OPTIONS.md)             | 8 opciones gratis de demo                  |
| [`docs/RECOMMENDED_FREE_DEMO_PLAN.md`](docs/RECOMMENDED_FREE_DEMO_PLAN.md)         | Cloudflare Tunnel recomendado              |
| [`docs/CLOUDFLARE_TUNNEL_DEMO_GUIDE.md`](docs/CLOUDFLARE_TUNNEL_DEMO_GUIDE.md)     | Exponer Docker local con Cloudflare Tunnel |
| [`docs/DEMO_SALES_PRESENTATION_SCRIPT.md`](docs/DEMO_SALES_PRESENTATION_SCRIPT.md) | Guión de presentación de ventas            |
| [`docs/PROJECT_FREEZE_V20.md`](docs/PROJECT_FREEZE_V20.md)                         | Estado congelado de la release v20         |

**Demo en vivo (sin coste, <5 min setup):**

```bash
cloudflared tunnel --url http://localhost:8090
# Compartir la URL https://<aleatorio>.trycloudflare.com con el cliente
```

### 📦 Releases y cierre

| Documento                                                                  | Descripción                          |
| -------------------------------------------------------------------------- | ------------------------------------ |
| [`docs/RELEASE_V20_READY.md`](docs/RELEASE_V20_READY.md)                   | Resumen técnico release v20          |
| [`docs/VPS_REAL_NEXT_ACTIONS.md`](docs/VPS_REAL_NEXT_ACTIONS.md)           | Pasos concretos para VPS             |
| [`docs/FIRST_REAL_DEPLOY_COMMANDS.md`](docs/FIRST_REAL_DEPLOY_COMMANDS.md) | Comandos exactos por bloque (A-H)    |
| [`docs/handoff.md`](docs/handoff.md)                                       | Estado actual de avance del proyecto |

---

## 🆘 Troubleshooting

| Problema                                | Causa probable                            | Solución                                                     |
| --------------------------------------- | ----------------------------------------- | ------------------------------------------------------------ |
| App no arranca (container restart loop) | Schema BD no existe (`DDL_AUTO=validate`) | Temporalmente `DB_DDL_AUTO=update`, luego revertir           |
| Error 502 Bad Gateway                   | Nginx no alcanza app                      | `docker ps` para verificar app está running                  |
| Error 403 en API                        | Sesión no válida o CSRF                   | Login en `/login` primero; CSRF deshabilitado para `/api/**` |
| Uploads no se ven                       | Ruta incorrecta o permisos                | `docker exec monteastur-app ls -la /app/uploads`             |
| Puerto 80 ocupado                       | Otro servicio (IIS, Apache)               | Cambiar `NGINX_PORT` en `.env`                               |
| PWA no instala                          | Sin HTTPS o manifest incorrecto           | Usar HTTPS; verificar console                                |
| Push notifications no funcionan         | Permiso bloqueado o sin HTTPS             | HTTPS requerido; resetear permiso                            |
| Offline no funciona                     | Service Worker no registrado              | Hard refresh (Ctrl+Shift+R)                                  |

---

## 🗺️ Roadmap

### Funcionalidades

- 📧 **Emails automáticos**: notificaciones por email en cambios de estado
- 💬 **WhatsApp API**: envío de notificaciones y actualizaciones
- 🔌 **WebSockets tracking**: actualizaciones en tiempo real sin recargar
- 📱 **App móvil**: aplicación nativa para clientes y operadores
- 👥 **Roles avanzados**: permisos granulares (operador, supervisor, auditor)
- 📄 **Exportar datos**: CSV, Excel y PDF desde el dashboard React
- 🔔 **Notificaciones push**: alertas en el navegador para cambios de estado

### Roadmap Seguridad

- 🔑 **JWT para APIs**: autenticación stateless para apps móviles e integraciones
- 🔄 **Refresh tokens**: rotación segura de tokens
- 🎯 **Roles granulares**: permisos por acción en lugar de roles planos
- 🚦 **Rate limiting**: bucket4j o Spring Cloud Gateway
- 🧾 **Audit logs**: trazabilidad completa (quién, cuándo, qué cambió)
- 🔒 **HTTPS reverse proxy**: TLS en Nginx/Caddy con HSTS
- 🛡️ **Content Security Policy**: cabeceras CSP para prevenir XSS en el SPA
- 📋 **OWASP Top 10**: auditoría periódica

---

> **Nota profesional**: Este documento está pensado como guía de referencia para desarrolladores, DevOps y equipos de operaciones. Para consultas técnicas específicas, referirse al código fuente y sus comentarios.
