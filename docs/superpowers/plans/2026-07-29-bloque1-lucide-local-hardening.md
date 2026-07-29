# Bloque 1: Lucide Local + Caché Nginx + Hardening Credenciales — Plan de Implementación

> **For agentic workers:** Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` to implement task-by-task.

**Goal:** Eliminar dependencia CDN externa para Lucide icons, cachear assets estáticos en Nginx, y blindar credenciales de producción contra defaults inseguros.

**Architecture:** Descargar Lucide UMD a `static/js/vendor/`, referenciar localmente desde los fragments de Thymeleaf. Cacheo agresivo vía `location` blocks en Nginx (30d, `immutable`). Eliminar fallbacks inseguros de `application-prod.properties` y forzar `ddl-auto=validate`.

**Tech Stack:** Spring Boot 3.3.5, Thymeleaf, Nginx Alpine, Lucide 1.27.0 (UMD)

## Global Constraints

- No añadir dependencias Maven nuevas
- No cambiar la estructura del Dockerfile ni docker-compose.yml
- No modificar templates .html excepto header.html y header-en.html
- Estados de Nginx: local.conf (sin CSP) y monteastur.conf (con CSP)
- Las credenciales en application.properties (dev) pueden tener defaults; las de application-prod.properties NO

---

### Task 1: Descargar Lucide UMD y alojarlo localmente

**Files:**
- Create: `src/main/resources/static/js/vendor/lucide.min.js`
- Create: `src/main/resources/static/js/vendor/` (directorio)

**Interfaces:**
- Consumes: nada
- Produces: `lucide.min.js` servido en `/js/vendor/lucide.min.js`

- [ ] **Step 1: Crear directorio vendor**
  ```powershell
  New-Item -ItemType Directory -Path "src/main/resources/static/js/vendor" -Force
  ```

- [ ] **Step 2: Descargar Lucide UMD desde unpkg**
  ```powershell
  Invoke-WebRequest -Uri "https://unpkg.com/lucide@latest/dist/umd/lucide.min.js" -OutFile "src/main/resources/static/js/vendor/lucide.min.js"
  ```

- [ ] **Step 3: Verificar la descarga**
  ```powershell
  $f = Get-Item "src/main/resources/static/js/vendor/lucide.min.js"
  Write-Host "Tamaño: $($f.Length) bytes"
  Get-Content "src/main/resources/static/js/vendor/lucide.min.js" -TotalCount 2
  ```
  Expected: ~414 KB, contenido JS (no HTML), primera línea con `/** @license lucide` o `(function`

- [ ] **Step 4: Commit**
  ```bash
  git add src/main/resources/static/js/vendor/lucide.min.js
  git commit -m "feat: add Lucide UMD bundle locally (vendor/lucide.min.js)"
  ```

---

### Task 2: Referenciar Lucide local desde los fragments de cabecera

**Files:**
- Modify: `src/main/resources/templates/fragments/header.html:17`
- Modify: `src/main/resources/templates/fragments/header-en.html:17`

**Interfaces:**
- Consumes: `static/js/vendor/lucide.min.js` (Task 1)
- Produces: Páginas que cargan Lucide desde `/js/vendor/lucide.min.js` en vez de CDN

- [ ] **Step 1: Cambiar header.html — CDN → local**
  ```html
  <script src="/js/vendor/lucide.min.js" defer></script>
  ```

- [ ] **Step 2: Cambiar header-en.html — CDN → local**
  ```html
  <script src="/js/vendor/lucide.min.js" defer></script>
  ```

- [ ] **Step 3: Verificar que los archivos ya no referencian unpkg**
  ```powershell
  Select-String -Path "src/main/resources/templates/fragments/header*.html" -Pattern "unpkg.com/lucide"
  ```
  Expected: 0 matches (ninguna línea contiene unpkg)

- [ ] **Step 4: Commit**
  ```bash
  git add src/main/resources/templates/fragments/header*.html
  git commit -m "refactor: load Lucide from /js/vendor/lucide.min.js instead of CDN"
  ```

---

### Task 3: Actualizar CSP en monteastur.conf (eliminar unpkg.com)

**Files:**
- Modify: `nginx/conf.d/monteastur.conf:11` (CSP línea activa)
- Modify: `nginx/conf.d/monteastur.conf:59` (CSP en sección HTTPS comentada)

**Interfaces:**
- Consumes: Servicio Nginx montado en Docker
- Produces: CSP header sin referencias externas a unpkg.com

- [ ] **Step 1: Actualizar CSP activo (línea 11)**
  Antes:
  ```
  add_header Content-Security-Policy "default-src 'self'; script-src 'self' https://unpkg.com; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; form-action 'self';" always;
  ```
  Después:
  ```
  add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; form-action 'self';" always;
  ```

- [ ] **Step 2: Actualizar CSP en sección HTTPS comentada (línea 59)**
  Misma lógica: eliminar `https://unpkg.com` de `script-src`

- [ ] **Step 3: Verificar**
  ```powershell
  Select-String -Path "nginx/conf.d/monteastur.conf" -Pattern "unpkg"
  ```
  Expected: 0 matches

- [ ] **Step 4: Commit**
  ```bash
  git add nginx/conf.d/monteastur.conf
  git commit -m "fix: remove unpkg.com from CSP — Lucide now served locally"
  ```

---

### Task 4: Cacheo agresivo de assets estáticos en Nginx

**Files:**
- Modify: `nginx/conf.d/local.conf` (añadir location blocks)
- Modify: `nginx/conf.d/monteastur.conf` (añadir location blocks)

**Interfaces:**
- Consumes: assets servidos por Nginx (CSS, JS, imágenes, fuentes)
- Produces: Respuestas con `Cache-Control: public, immutable, max-age=2592000`

- [ ] **Step 1: Añadir location de assets a local.conf**
  Insertar ANTES del bloque `location /`:
  ```nginx
  # Cache agresivo para assets estáticos con hash implícito
  location ~* \.(css|js)$ {
      expires 30d;
      add_header Cache-Control "public, immutable";
      proxy_pass http://app:8080;
      proxy_set_header Host $http_host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
  }

  location ~* \.(jpg|jpeg|png|gif|ico|svg|webp|woff2?|ttf|eot)$ {
      expires 30d;
      add_header Cache-Control "public, immutable";
      proxy_pass http://app:8080;
      proxy_set_header Host $http_host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
  }
  ```

- [ ] **Step 2: Añadir location de assets a monteastur.conf**
  Insertar ANTES del `location /` en el bloque server (después del location `.well-known`):
  ```nginx
    # Cache agresivo para assets estáticos
    location ~* \.(css|js)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
        proxy_pass http://app:8080;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ~* \.(jpg|jpeg|png|gif|ico|svg|webp|woff2?|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
        proxy_pass http://app:8080;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
  ```

- [ ] **Step 3: Verificar sintaxis Nginx**
  ```powershell
  docker compose run --rm nginx nginx -t
  ```
  Expected: `syntax is ok` / `test is successful`

- [ ] **Step 4: Commit**
  ```bash
  git add nginx/conf.d/local.conf nginx/conf.d/monteastur.conf
  git commit -m "perf: add 30d immutable cache for CSS/JS/images in Nginx"
  ```

---

### Task 5: Hardening de credenciales en application-prod.properties

**Files:**
- Modify: `src/main/resources/application-prod.properties` (líneas 17-18, 24, 72-73)

**Interfaces:**
- Consumes: Variables de entorno `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`
- Produces: Sin fallbacks inseguros — la app falla al arrancar si faltan variables

- [ ] **Step 1: Eliminar fallback DB_USERNAME (línea 17)**
  Antes:
  ```
  spring.datasource.username=${DB_USERNAME:root}
  ```
  Después:
  ```
  spring.datasource.username=${DB_USERNAME}
  ```

- [ ] **Step 2: Eliminar fallback DB_PASSWORD (línea 18)**
  Antes:
  ```
  spring.datasource.password=${DB_PASSWORD:}
  ```
  Después:
  ```
  spring.datasource.password=${DB_PASSWORD}
  ```

- [ ] **Step 3: Forzar ddl-auto=validate (línea 24)**
  Antes:
  ```
  spring.jpa.hibernate.ddl-auto=${DB_DDL_AUTO:update}
  ```
  Después:
  ```
  spring.jpa.hibernate.ddl-auto=${DB_DDL_AUTO:validate}
  ```

- [ ] **Step 4: Eliminar fallbacks de admin credentials (líneas 72-73)**
  Antes:
  ```
  app.admin.username=${ADMIN_USERNAME:admin}
  app.admin.password=${ADMIN_PASSWORD:admin123}
  ```
  Después:
  ```
  app.admin.username=${ADMIN_USERNAME}
  app.admin.password=${ADMIN_PASSWORD}
  ```

- [ ] **Step 5: Verificar que no quedan defaults inseguros**
  ```powershell
  Select-String -Path "src/main/resources/application-prod.properties" -Pattern ":root|:admin|:admin123|DB_PASSWORD:\}|:}$"
  ```
  Expected: 0 matches (ningún fallback con valor por defecto)

- [x] **Step 6: Commit**
  ```bash
  git add src/main/resources/application-prod.properties
  git commit -m "fix: remove insecure credential fallbacks in prod profile, force ddl-auto=validate"
  ```

---

### Task 6: Validación de entorno al arranque (protección contra despliegue sin .env)

**Files:**
- Modify: `src/main/java/com/monteastur/envios/MonteasturApplication.java`

**Interfaces:**
- Consumes: Variables de entorno del sistema
- Produces: Error temprano y claro si faltan variables obligatorias en prod

- [ ] **Step 1: Añadir validación de entorno en MonteasturApplication.java**
  ```java
  package com.monteastur.envios;

  import jakarta.annotation.PostConstruct;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.boot.SpringApplication;
  import org.springframework.boot.autoconfigure.SpringBootApplication;
  import org.springframework.core.env.Environment;

  @SpringBootApplication
  public class MonteasturApplication {

      private static final Logger log = LoggerFactory.getLogger(MonteasturApplication.class);

      private final Environment env;

      public MonteasturApplication(Environment env) {
          this.env = env;
      }

      public static void main(String[] args) {
          SpringApplication.run(MonteasturApplication.class, args);
      }

      @PostConstruct
      public void validateEnvironment() {
          String[] required = {"DB_USERNAME", "DB_PASSWORD"};
          boolean isProd = "prod".equals(env.getProperty("spring.profiles.active"));
          if (isProd) {
              for (String key : required) {
                  String val = env.getProperty(key);
                  if (val == null || val.isBlank()) {
                      log.error("Variable de entorno obligatoria '{}' no está definida. " +
                              "La aplicación se detendrá.", key);
                      SpringApplication.exit(env -> 1);
                      throw new IllegalStateException(
                              "Variable de entorno obligatoria '" + key + "' no definida");
                  }
              }
              log.info("Validación de entorno superada — todas las variables requeridas están presentes");
          } else {
              log.info("Perfil activo: {}. Saltando validación de entorno en desarrollo.",
                      env.getProperty("spring.profiles.active", "default"));
          }
      }
  }
  ```

- [ ] **Step 2: Compilar y verificar**
  ```powershell
  mvn clean compile -q 2>&1 | Select-String -NotMatch "WARNING"
  ```
  Expected: sin errores, sin salida

- [ ] **Step 3: Commit**
  ```bash
  git add src/main/java/com/monteastur/envios/MonteasturApplication.java
  git commit -m "feat: validate required env vars at startup in prod profile"
  ```

---

### Task 7: Verificación final — build Docker + healthcheck

**Files:**
- None (solo validación)

- [ ] **Step 1: Build Docker image**
  ```powershell
  docker compose build app 2>&1
  ```
  Expected: `Image envios_paraguay_cms-app Built`

- [ ] **Step 2: Verificar que lucide.min.js está en la imagen**
  ```powershell
  docker run --rm envios_paraguay_cms-app ls -la /app/BOOT-INFO/classes/static/js/vendor/
  ```
  Expected: `lucide.min.js` presente con tamaño > 400 KB

- [ ] **Step 3: Verificar compilación Maven completa**
  ```powershell
  mvn clean package -DskipTests -q 2>&1 | Select-String -NotMatch "WARNING"
  ```
  Expected: sin errores

- [ ] **Step 4: Verificar cambios pendientes**
  ```powershell
  git status
  ```
  Expected: solo los archivos planeados modificados

- [ ] **Step 5: Push a GitHub**
  ```bash
  git push origin feature/seguimiento-premium
  ```
