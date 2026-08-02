# Bloque 14 — Pipeline CI/CD Enterprise (GitHub Actions + Docker & Healthchecks) — Design

**Fecha:** 2026-08-02
**Estado:** Aprobado por el usuario (Enfoque A + correcciones de ingeniería).

## Contexto

El proyecto **Envios_Paraguay_CMS** (Spring Boot 3.3.5, Java 17 Temurin, MySQL 8, Redis 7, Flyway V1–V8, 190+ tests) carece de un pipeline CI/CD de nivel industrial. Existe un `ci.yml` básico (triggers en `feature/seguimiento-premium`, sin job Docker) que se reemplazará.

El objetivo es construir un flujo CI/CD en `.github/workflows/ci.yml` que:
1. Ejecute la suite completa de tests (~190) contra servicios reales y efímeros de MySQL 8.0 y Redis 7 (Alpine) en el runner de GitHub.
2. Valide las migraciones Flyway V1–V8 bajo la misma topología de producción.
3. Optimice tiempos con caché de Maven vía `actions/setup-java`.
4. Construya la imagen OCI con Buildx y ejecute una prueba de sanidad de arranque en frío con verificación de `/actuator/health`.

## Decisiones clave (aprobaron con el usuario)

- **Enfoque A:** un solo `ci.yml` con dos jobs encadenados (`test` → `docker-build` con `needs: test`).
- **Maven Wrapper:** añadir `mvnw`, `mvnw.cmd` y `.mvn/wrapper/maven-wrapper.properties` con Maven 3.9.9 (misma versión local del `AGENTS.md`) para reproducibilidad y alineación con el prompt (`./mvnw`).
- **Docker solo en push:** `docker-build` corre únicamente en `push` a `main`/`develop` (no en PRs) para no gastar ~5–10 min por PR en el build multi-stage. Los PRs ejecutan solo la suite de tests.
- **Credenciales del job `test`:** `root/root` (coincide con el comando Docker verificado del `AGENTS.md`), no `test_user/test_pass`.
- **Corrección del sanity check:** el prompt original corría `docker run` sin variables de entorno; el perfil `prod` exige `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_USERNAME` y `ADMIN_PASSWORD` sin default (y `validateEnvironment()` aborta el arranque sin `DB_USERNAME`/`DB_PASSWORD`). El sanity check debe proveer esas env vars **y** MySQL+Redis alcanzables desde el contenedor.

## Arquitectura del pipeline

```
push: main, develop / PR: main
      │
      ▼
  job: test   ──(BUILD SUCCESS)──►  job: docker-build  (solo push)
  servicios: mysql:8.0 + redis:7-alpine        │
  ./mvnw clean test                            ▼
  upload Surefire (if: always)          buildx + build-push-action (load)
                                         docker run --network host + env vars
                                         retry curl /actuator/health → UP
```

### Job `test`
- `runs-on: ubuntu-latest`.
- Servicios con healthchecks y reintentos (evita race conditions de arranque):
  - `mysql:8.0`: `MYSQL_ROOT_PASSWORD=root`, `MYSQL_DATABASE=envios_paraguay_cms_test`, puerto `3306:3306`, `--health-cmd="mysqladmin ping -h localhost -uroot -proot"` con interval 10s / timeout 5s / retries 5.
  - `redis:7-alpine`: puerto `6379:6379`, `--health-cmd="redis-cli ping"` con el mismo patrón.
- Steps:
  1. `actions/checkout@v4`.
  2. `actions/setup-java@v4` (java 17, temurin, `cache: maven`).
  3. `chmod +x mvnw`.
  4. `./mvnw clean test` con env:
     - `SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`
     - `DB_USERNAME=root`, `DB_PASSWORD=root`
     - `SPRING_DATA_REDIS_HOST=localhost`
     - `SPRING_PROFILES_ACTIVE=test` (beneficioso: fuerza el perfil de test en todos los `@SpringBootTest`, aunque el default de la propiedad apunta a `localhost:3307`).
  5. `actions/upload-artifact@v4` con `if: always()` → `surefire-test-reports` (path `target/surefire-reports/`). Cumple la regla de evidencia ante fallo.

### Job `docker-build`
- `needs: test`, `runs-on: ubuntu-latest`, `if: github.event_name == 'push'` (solo main/develop).
- Steps:
  1. `actions/checkout@v4`.
  2. `docker/setup-buildx-action@v3`.
  3. `docker/build-push-action@v5`: `context: .`, `file: ./Dockerfile`, `push: false`, `tags: envios-paraguay-cms:latest`, `load: true` (carga la imagen en el daemon del runner).
  4. **Sanity check** (arranque en frío con servicios efímeros en el propio job):
     - Servicios `mysql:8.0` (DB `envios_paraguay_cms_smoke`) y `redis:7-alpine` con los mismos healthchecks.
     - `docker run -d --name envios-smoke --network host` con env:
       - `SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/envios_paraguay_cms_smoke?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`
       - `DB_USERNAME=root`, `DB_PASSWORD=root`
       - `ADMIN_USERNAME=smoke`, `ADMIN_PASSWORD=smoke` (arbitrarias; el perfil prod las exige)
       - `REDIS_HOST=localhost`
       - `APP_NOTIFICATION_MAIL_ENABLED=false` (evita intentos SMTP durante el smoke)
       - `SPRING_PROFILES_ACTIVE=prod` explícito (aunque el ENTRYPOINT del Dockerfile ya lo fuerza, se documenta intención).
     - `docker logs envios-smoke` (visibilidad inmediata).
     - Loop de espera: consultar `curl --fail http://localhost:8080/actuator/health` hasta `UP`, con ~30 intentos × 5 s (~150 s; margen sobre el `start-period` de 40 s del HEALTHCHECK del Dockerfile). La consulta se hace **desde el runner** (no desde dentro del contenedor). Si agota o el estado no es UP, imprimir `docker logs envios-smoke` y `exit 1`.
     - Limpieza del contenedor en `finally` (o paso final con `if: always()`).

### Por qué `--network host`
En runners nativos de GitHub (Linux) los contenedores de servicio publican sus puertos en el localhost del runner; un contenedor con `--network host` comparte la red del runner, por lo que `localhost:3306` (MySQL) y `localhost:6379` (Redis) son alcanzables sin depender del gateway de bridge (`172.17.0.1`). Con host networking, `-p 8080:8080` se ignora y `curl http://localhost:8080/...` desde el runner alcanza a la app. Es determinista y evita suposiciones de red.

## Cambios de infraestructura

### Maven Wrapper (Maven 3.9.9)
- Generar con el Maven local (`C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd wrapper:wrapper -Dmaven=3.9.9`).
- Ficheros esperados: `mvnw` (bash), `mvnw.cmd` (Windows), `.mvn/wrapper/maven-wrapper.properties` (y `maven-wrapper.jar` si la versión del plugin lo genera).
- Se versiona en el repo. En el workflow se usa `chmod +x mvnw` + `./mvnw clean test`.

### Reemplazo de `.github/workflows/ci.yml`
- Nombre: `CI/CD Enterprise Pipeline - Envios Paraguay CMS`.
- Triggers: `push` a `main` y `develop`; `pull_request` a `main`.
- **Permisos mínimos:** `permissions: contents: read` (nada más).
- **Concurrencia:** `concurrency` agrupada por rama para cancelar runs redundantes en push rápidos.
- El `ci.yml` actual (trigger `feature/seguimiento-premium`, sin job Docker) se sustituye por completo.
- **Fuera de alcance:** `deploy.yml`, `deploy-prod.yml`, `deploy-koyeb.yml` (CD existente) no se modifican.

## Seguridad

- El pipeline no usa secrets: todas las env vars del sanity check son de smoke (`smoke/smoke`, `root/root` sobre la DB efímera `envios_paraguay_cms_smoke`).
- No se exponen credenciales reales. Los reportes Surefire no contienen campos sensibles (los DTOs admin ya los filtran).

## Manejo de errores / evidencias

- **Suite rota:** `upload-artifact` con `if: always()` sube los reportes Surefire descargables para auditoría inmediata.
- **Sanity check roto:** `docker logs` se imprime antes del `exit 1` para diagnóstico.
- **Build de imagen roto:** el job falla y, por `needs: test` + `if: push`, jamás se empaqueta código con tests rotos.

## Verificación del bloque

El workflow no es ejecutable fuera de GitHub Actions. Verificación local equivalente:
1. **Wrapper:** `mvnw.cmd --version` reporta 3.9.9.
2. **Suite completa:** `mvnw.cmd clean test` (o con el Maven local) contra MySQL+Redis reales en Docker, con las mismas env vars del job `test` → BUILD SUCCESS (~190 tests).
3. **Arranque en frío de la imagen:** `docker build -t envios-paraguay-cms:latest .`, `docker run` contra la red `envios_paraguay_cms_backend` (con `monteastur-mysql`/`monteastur-redis` levantados) con las env vars de prod, y `curl http://localhost:8080/actuator/health` → UP.
4. **Sintaxis YAML:** validación del `ci.yml` (parser YAML / actionlint si disponible).
5. El paso final del bloque documenta en `docs/handoff.md` que la ejecución real queda pendiente de un `push` a GitHub.

## Riesgos y mitigaciones

- **Tiempos de arranque MySQL/Redis:** healthchecks con reintentos en ambos jobs.
- **Arranque lento de Spring Boot en el smoke:** loop de retry con margen sobre `start-period`; el fallo imprime logs.
- **Versión de Maven en el runner:** resuelta con Maven Wrapper 3.9.9 (pinning).
- **Red Docker entre runner y contenedor de smoke:** `--network host` evita dependencia del gateway.
- **Costo de CI en PRs:** el job Docker solo corre en push; los PRs solo ejecutan `test`.

## Definición de "Done"

- `.github/workflows/ci.yml` reemplazado con los dos jobs encadenados y el sanity check corregido.
- Maven Wrapper versionado (Maven 3.9.9).
- Verificación local: suite completa en verde + arranque en frío de la imagen con `/actuator/health` UP + YAML válido.
- `docs/handoff.md` actualizado (Bloque 14).
