# Bloque 14 — Pipeline CI/CD Enterprise Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sustituir el `ci.yml` básico por un pipeline enterprise en `.github/workflows/ci.yml` con job `test` (MySQL 8.0 + Redis 7 efímeros, `./mvnw clean test`) encadenado a un job `docker-build` (solo push) que construye la imagen con Buildx y ejecuta un sanity check de arranque en frío contra `/actuator/health`, añadiendo el Maven Wrapper 3.9.9 para reproducibilidad.

**Architecture:** Un único workflow con `permissions: contents: read` y `concurrency` por rama. Job `test`: servicios MySQL/Redis con healthchecks, `setup-java@v4` con caché Maven, `./mvnw clean test -B` con las env vars del perfil test, y subida de reportes Surefire con `if: always()`. Job `docker-build`: `needs: test`, corre solo en `push`; buildx + `docker/build-push-action@v5` con `load: true`; sanity check con `docker run --network host` (los servicios publican puertos en localhost del runner), env vars de prod que el prompt omitía (`ADMIN_USERNAME/PASSWORD`) y retry de `curl /actuator/health` hasta `UP`.

**Tech Stack:** GitHub Actions, Docker Buildx, Maven Wrapper 3.9.9, MySQL 8.0, Redis 7-alpine, Java 17 Temurin, Spring Boot 3.3.5 (Actuator `/actuator/health`).

## Global Constraints

- **Maven Wrapper 3.9.9 exacto:** `distributionUrl` apunta a `apache-maven-3.9.9-bin.zip`. Se usa `./mvnw clean test -B` en CI (nunca `mvn` global).
- **Job `docker-build` SOLO en push a `main`/`develop`** (`if: github.event_name == 'push'`). Los PRs ejecutan únicamente la suite de tests.
- **`permissions: contents: read`** (mínimos). **`concurrency`** agrupada por `github.ref` con `cancel-in-progress: true`.
- **Job `test` sin `MYSQL_USER`:** credenciales `root/root` (coinciden con el comando Docker verificado del `AGENTS.md`); DB `envios_paraguay_cms_test`.
- **Sanity check completo:** el perfil `prod` exige `DB_USERNAME`, `DB_PASSWORD` (lo valida `MonteasturApplication.validateEnvironment()`), `ADMIN_USERNAME` y `ADMIN_PASSWORD` (los consume `DefaultUsersInitializer` al arrancar). El sanity check provee las cuatro + `REDIS_HOST` + `APP_NOTIFICATION_MAIL_ENABLED=false`.
- **`--network host`** en el sanity check: los contenedores de servicio publican puertos en el localhost del runner; el probe de salud se hace **desde el runner** (`curl http://localhost:8080/actuator/health`), no desde dentro del contenedor.
- **Prohibido exponer secrets:** todas las env vars del pipeline son de test/smoke.
- **`deploy.yml`, `deploy-prod.yml`, `deploy-koyeb.yml` quedan intactos** (fuera de alcance).
- **`.gitattributes` nuevo:** `mvnw text eol=lf` para que el script bash conserve LF en cualquier checkout (el repo tiene `core.autocrlf=true`).
- **No hacer push a GitHub:** la ejecución real del workflow queda documentada como pendiente en `docs/handoff.md`.
- **Comandos de verificación local:**
  - Maven local: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd`
  - Wrapper Windows: `.\mvnw.cmd --version`
  - Suite completa (replica CI, red `envios_paraguay_cms_backend` con `db`/`redis`):
    `docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 sh -c "./mvnw clean test -B"`
  - YAML: `python -c "import yaml,sys; d=yaml.safe_load(open('.github/workflows/ci.yml', encoding='utf-8')); assert set(d['jobs'].keys())=={'test','docker-build'}, d['jobs'].keys(); print('YAML OK; jobs:', list(d['jobs'].keys()))"`

---

### Task 1: Maven Wrapper 3.9.9 + `.gitattributes`

**Files:**
- Create: `mvnw`
- Create: `mvnw.cmd`
- Create: `.mvn/wrapper/maven-wrapper.properties` (y `maven-wrapper.jar` si el plugin lo genera)
- Create: `.gitattributes`

**Interfaces:**
- Produces: `./mvnw clean test -B` (bash, Linux/macOS) y `.\mvnw.cmd` (Windows) usando Maven 3.9.9. Lo consumen el job `test` del `ci.yml` (Task 2) y la verificación local (Task 3).

- [ ] **Step 1: Generar el wrapper con el Maven local**

```powershell
& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" wrapper:wrapper "-Dmaven=3.9.9"
```

Expected: `BUILD SUCCESS` y aparición de `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`.

- [ ] **Step 2: Verificar que `maven-wrapper.properties` fija 3.9.9**

```powershell
Get-Content ".mvn/wrapper/maven-wrapper.properties"
```

Expected: `distributionUrl=...apache-maven-3.9.9-bin.zip` (o `maven-wrapper.properties` con `distributionType=only-script` + `distributionUrl` 3.9.9).

- [ ] **Step 3: Verificar versión con el wrapper Windows**

```powershell
.\mvnw.cmd --version
```

Expected: primera línea `Apache Maven 3.9.9 (...)`. (El wrapper descargará el distribution en `~/.m2/wrapper` la primera vez.)

- [ ] **Step 4: Crear `.gitattributes` para pin de LF en `mvnw`**

`.gitattributes`:
```
mvnw text eol=lf
mvnw.cmd text eol=crlf
```

- [ ] **Step 5: Commit**

```bash
git add mvnw mvnw.cmd .mvn .gitattributes
git commit -m "build: add Maven Wrapper 3.9.9 for reproducible CI builds"
```

---

### Task 2: Workflow enterprise `.github/workflows/ci.yml`

**Files:**
- Modify: `.github/workflows/ci.yml` (reemplazo total; el actual dispara en `feature/seguimiento-premium`, usa `mvn test` sin wrapper y no tiene job Docker)

**Interfaces:**
- Consumes: `./mvnw` (Task 1). Produce: pipeline con jobs `test` y `docker-build` (referenciado en la verificación YAML y documentado en `docs/handoff.md`).

- [ ] **Step 1: Escribir el workflow**

`.github/workflows/ci.yml` (reemplazo completo):

```yaml
name: CI/CD Enterprise Pipeline - Envios Paraguay CMS

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

permissions:
  contents: read

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  test:
    name: Test suite (MySQL 8 + Redis 7)
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: envios_paraguay_cms_test
        ports:
          - 3306:3306
        options: >-
          --health-cmd "mysqladmin ping -h localhost -uroot -proot"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 17
          distribution: temurin
          cache: maven
      - name: Make Maven wrapper executable
        run: chmod +x mvnw
      - name: Run full test suite
        run: ./mvnw clean test -B
        env:
          SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
          DB_USERNAME: root
          DB_PASSWORD: root
          SPRING_DATA_REDIS_HOST: localhost
          SPRING_PROFILES_ACTIVE: test
      - name: Upload Surefire reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: surefire-test-reports
          path: target/surefire-reports/

  docker-build:
    name: Docker image build + smoke test
    needs: test
    runs-on: ubuntu-latest
    if: github.event_name == 'push'
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: envios_paraguay_cms_smoke
        ports:
          - 3306:3306
        options: >-
          --health-cmd "mysqladmin ping -h localhost -uroot -proot"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      mailpit:
        image: axllent/mailpit:latest
        ports:
          - 1025:1025
        options: >-
          --health-cmd "wget -q -O /dev/null http://localhost:8025/readyz || exit 1"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      - name: Build and load image
        uses: docker/build-push-action@v5
        with:
          context: .
          file: ./Dockerfile
          push: false
          load: true
          tags: envios-paraguay-cms:latest
      - name: Smoke test - cold start health check
        run: |
          docker run -d --name envios-smoke --network host \
            -e SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/envios_paraguay_cms_smoke?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
            -e DB_USERNAME=root \
            -e DB_PASSWORD=root \
            -e ADMIN_USERNAME=smoke \
            -e ADMIN_PASSWORD=smoke \
            -e REDIS_HOST=localhost \
            -e APP_NOTIFICATION_MAIL_ENABLED=false \
            -e SPRING_PROFILES_ACTIVE=prod \
            envios-paraguay-cms:latest
          docker logs envios-smoke
          for i in $(seq 1 30); do
            status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || true)
            echo "attempt ${i}: HTTP ${status}"
            if [ "${status}" = "200" ]; then
              body=$(curl -s http://localhost:8080/actuator/health)
              echo "${body}"
              if echo "${body}" | grep -q '"UP"'; then
                echo "SMOKE TEST PASSED - container healthy (UP)"
                docker rm -f envios-smoke
                exit 0
              fi
            fi
            sleep 5
          done
          echo "SMOKE TEST FAILED - container did not become healthy within 150s"
          docker logs envios-smoke
          docker rm -f envios-smoke
          exit 1
```

- [ ] **Step 2: Validar sintaxis YAML y estructura de jobs**

```powershell
python -c "import yaml; d=yaml.safe_load(open('.github/workflows/ci.yml', encoding='utf-8')); assert set(d['jobs'].keys())=={'test','docker-build'}, d['jobs'].keys(); assert d['jobs']['docker-build']['needs']=='test'; assert d['jobs']['docker-build']['if']==\"github.event_name == 'push'\"; assert d['permissions']=={'contents':'read'}; print('YAML OK; jobs:', list(d['jobs'].keys()))"
```

Expected: `YAML OK; jobs: ['test', 'docker-build']`.

- [ ] **Step 3: Verificar que los nombres de env vars coinciden con el código**

Comprobación manual con grep (no modifica nada):

```powershell
rg -n "DB_USERNAME|DB_PASSWORD|REDIS_HOST|ADMIN_USERNAME|ADMIN_PASSWORD|APP_NOTIFICATION_MAIL_ENABLED" src/main/resources/application-prod.properties src/main/java/com/monteastur/envios/MonteasturApplication.java src/main/java/com/monteastur/envios/config/DefaultUsersInitializer.java
```

Expected: `spring.datasource.username=${DB_USERNAME}`, `spring.datasource.password=${DB_PASSWORD}`, `app.admin.username=${ADMIN_USERNAME}`, `app.admin.password=${ADMIN_PASSWORD}`, `spring.data.redis.host=${REDIS_HOST:redis}`, `app.notification.mail.enabled=${APP_NOTIFICATION_MAIL_ENABLED:true}`, y `required = {"DB_USERNAME", "DB_PASSWORD"}` en `validateEnvironment()`.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: enterprise pipeline with test and docker-build jobs and smoke health check"
```

---

### Task 3: Verificación local — suite completa con el wrapper (replica del job `test`)

**Files:**
- Ninguno (solo ejecución y evidencia).

**Interfaces:**
- Valida Task 1 (`mvnw` bash funciona) y Task 2 (env vars del job `test` correctas contra MySQL/Redis reales). Requiere red `envios_paraguay_cms_backend` con los contenedores `db` (monteastur-mysql) y `redis` (monteastur-redis) levantados.

- [ ] **Step 1: Garantizar que la DB de test existe en el MySQL de la red**

```powershell
docker exec monteastur-mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS envios_paraguay_cms_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>&1 | Select-String -NotMatch "Using a password"
```

Expected: sin errores.

- [ ] **Step 2: Ejecutar la suite completa dentro del contenedor Maven usando `./mvnw`**

```powershell
docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
  -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis `
  -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 sh -c "./mvnw clean test -B"
```

(Timeout amplio: 600000 ms.)

Expected: `BUILD SUCCESS`, ~190 tests, y en el log del wrapper `Maven home: ...maven-3.9.9...` (demuestra que el wrapper usa 3.9.9 dentro del contenedor, igual que CI).

- [ ] **Step 3: Registrar evidencia**

Anotar en el handoff (Task 5) el número de tests ejecutados/fallos 0 y el `BUILD SUCCESS`.

---

### Task 4: Verificación local — arranque en frío de la imagen + `/actuator/health`

**Files:**
- Ninguno (solo ejecución y evidencia).

**Interfaces:**
- Valida el sanity check de Task 2. Replica local (Docker Desktop Windows: no existe `--network host`, por eso se usan contenedores efímeros en la red `envios_paraguay_cms_backend` y puerto host 18080; el puerto 8080 del host está ocupado por `monteastur-app`).
- **Corrección deliberada al spec (hallada en ejecución):** el endpoint agregado `/actuator/health` incluye el `MailHealthIndicator`; sin servidor SMTP responde `DOWN` y el smoke falla aunque la app esté sana (DB/Redis/Flyway OK). Fix: el smoke incluye `mailpit` (mismo rol que en `docker-compose.yml` de prod). En CI, `axllent/mailpit` publica `1025:1025` y con `--network host` la app usa su default `localhost:1025`. Localmente se apunta a `SPRING_MAIL_HOST=monteastur-mailpit` (mailpit existente en la red).

- [ ] **Step 1: Levantar MySQL y Redis efímeros para el smoke**

```powershell
docker run -d --name smoke-mysql --network envios_paraguay_cms_backend -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=envios_paraguay_cms_smoke mysql:8.0
docker run -d --name smoke-redis --network envios_paraguay_cms_backend redis:7-alpine
docker exec smoke-mysql sh -c "until mysqladmin ping -h localhost -uroot -proot --silent; do sleep 2; done"
```

Expected: los dos contenedores corriendo y el `until` termina sin error (MySQL listo).

- [ ] **Step 2: Construir la imagen**

```powershell
docker build -t envios-paraguay-cms:latest .
```

(Timeout amplio: 600000 ms.)

Expected: `Successfully built` / `Successfully tagged envios-paraguay-cms:latest`.

- [ ] **Step 3: Arrancar la imagen con las env vars de prod (mismas que CI, con servicios efímeros)**

```powershell
docker run -d --name envios-smoke --network envios_paraguay_cms_backend -p 18080:18080 `
  -e PORT=18080 `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://smoke-mysql:3306/envios_paraguay_cms_smoke?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
  -e DB_USERNAME=root -e DB_PASSWORD=root -e ADMIN_USERNAME=smoke -e ADMIN_PASSWORD=smoke `
  -e REDIS_HOST=smoke-redis -e APP_NOTIFICATION_MAIL_ENABLED=false -e SPRING_MAIL_HOST=monteastur-mailpit `
  -e SPRING_PROFILES_ACTIVE=prod `
  envios-paraguay-cms:latest
```

Expected: contenedor `envios-smoke` arrancado (la app aplica Flyway V1–V8 contra `envios_paraguay_cms_smoke`).

- [ ] **Step 4: Esperar salud y validar `/actuator/health`**

```powershell
$ok = $false
for ($i = 1; $i -le 30; $i++) {
  try { $body = Invoke-RestMethod -Uri "http://localhost:18080/actuator/health" -TimeoutSec 5; if ($body.status -eq "UP") { Write-Output "SMOKE PASSED on attempt $i: $($body | ConvertTo-Json -Compress)"; $ok = $true; break } } catch { Start-Sleep -Seconds 5 }
  Start-Sleep -Seconds 5
}
if (-not $ok) { docker logs envios-smoke; throw "SMOKE TEST FAILED" }
```

Expected: `SMOKE PASSED on attempt N: {"status":"UP"}`.

- [ ] **Step 5: Verificar Flyway aplicado (migraciones V1–V8 en el smoke DB)**

```powershell
docker exec smoke-mysql mysql -uroot -proot -e "SELECT version, success FROM envios_paraguay_cms_smoke.flyway_schema_history;" 2>&1 | Select-String -NotMatch "Using a password"
```

Expected: filas V1…V8 todas con `success = 1`.

- [ ] **Step 6: Limpiar contenedores efímeros**

```powershell
docker rm -f envios-smoke smoke-mysql smoke-redis
```

Expected: tres nombres impresos sin error.

- [ ] **Step 7: Registrar evidencia**

Anotar en el handoff (Task 5): imagen construida, salud `UP` en intento N, migraciones V1–V8 aplicadas.

---

### Task 5: Actualizar `docs/handoff.md` (Bloque 14)

**Files:**
- Modify: `docs/handoff.md`

**Interfaces:**
- Consume la evidencia de Tasks 3 y 4 (tests, BUILD SUCCESS, smoke PASSED, migraciones). Deja constancia de que la ejecución real de GitHub Actions queda pendiente de un `push` a GitHub.

- [ ] **Step 1: Leer la sección de estado actual del handoff**

```powershell
rg -n "Bloque 1[34]|Bloque 13|handoff" docs/handoff.md
```

- [ ] **Step 2: Añadir la sección del Bloque 14**

Insertar tras el estado del Bloque 13 un bloque:

```markdown
## Bloque 14 — Pipeline CI/CD Enterprise (GitHub Actions + Docker & Healthchecks)

- **Estado:** implementado y verificado localmente; pendiente de validación real en GitHub Actions.
- **Cambios:** `.github/workflows/ci.yml` enterprise (jobs `test` + `docker-build` encadenados con `needs: test`), Maven Wrapper 3.9.9 (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`), `.gitattributes` (LF para `mvnw`).
- **Job `test`:** servicios MySQL 8.0 + Redis 7-alpine con healthchecks, `./mvnw clean test -B` con `SPRING_DATASOURCE_URL/DB_USERNAME/DB_PASSWORD/SPRING_DATA_REDIS_HOST/SPRING_PROFILES_ACTIVE=test`, upload Surefire `if: always()`.
- **Job `docker-build`:** solo `push` a `main`/`develop`; buildx + `load: true`; sanity check `docker run --network host` con env vars de prod (`DB_USERNAME`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `REDIS_HOST`, `APP_NOTIFICATION_MAIL_ENABLED=false`) y retry `curl /actuator/health` hasta `UP`.
- **Verificación local:** <N> tests en verde (BUILD SUCCESS) con `./mvnw` en contenedor Maven; imagen `envios-paraguay-cms:latest` construida; arranque en frío con salud `UP` en el intento <N>; Flyway V1–V8 aplicadas en el smoke DB.
- **Pendiente:** push a GitHub para validación real del workflow (no se hizo push sin confirmación del usuario).
```

- [ ] **Step 3: Commit**

```bash
git add docs/handoff.md
git commit -m "docs(handoff): Bloque 14 CI/CD enterprise pipeline"
```

---

## Self-Review

- **Cobertura de spec:** job `test` (Task 2) ✓; job `docker-build` con sanity check (Task 2) ✓; Maven Wrapper (Task 1) ✓; permisos/concurrency (Task 2) ✓; corrección de env vars de prod (Task 2 Step 3, Task 4) ✓; verificación local suite+smoke (Tasks 3–4) ✓; handoff (Task 5) ✓; `deploy*.yml` intactos (Task 2 solo toca `ci.yml`) ✓.
- **Sin placeholders:** todos los pasos incluyen código o comandos completos.
- **Consistencia de nombres:** `envios-paraguay-cms:latest`, `envios-smoke`, `smoke-mysql`, `smoke-redis`, `envios_paraguay_cms_smoke` y `envios_paraguay_cms_test` usados consistentemente en Task 2 y Task 4.
