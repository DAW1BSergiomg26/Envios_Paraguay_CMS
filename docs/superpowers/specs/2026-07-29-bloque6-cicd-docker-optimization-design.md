# Bloque 6: CI/CD Pipeline & Docker Optimization

**Date:** 2026-07-29
**Project:** Monteastur Envios CMS
**Branch:** feature/seguimiento-premium
**Status:** Design

## Objective

Implement a complete CI/CD pipeline with GitHub Actions and optimize the existing Docker infrastructure for production readiness.

## Motivation

- No automated testing on push — regressions possible without detection
- No automated deployment — manual builds and updates
- Docker healthcheck uses `curl` but the runtime image lacks it
- No backup strategy for MySQL data
- Builds redownload Maven dependencies every time — slow
- Single-platform builds (amd64 only) limits deployment options

## Current State

### CI/CD
- `.github/` directory exists but is **empty** — no workflows
- Koyeb and Render deployment YAMLs exist but require manual trigger

### Docker
- Multi-stage Dockerfile exists (Node → Maven → JRE 17)
- Docker Compose with 7 services: MySQL, App, Nginx, Certbot, Prometheus, Grafana, Uptime Kuma
- `.env` / `.env.example` / `.env.production.example` configured

### Docker Issues Found

| Issue | Severity | Description |
|-------|----------|-------------|
| Missing `curl` | 🔴 High | `docker-compose.yml` healthcheck uses `curl -f http://localhost:8080/actuator/health` but `eclipse-temurin:17-jre` has no `curl` |
| No Maven cache | 🟡 Medium | Every Docker build redownloads all Maven dependencies |
| No backup | 🟡 Medium | MySQL data has no automated backup |
| Single platform | 🟢 Low | Only `linux/amd64` — no ARM64 support |
| No log rotation | 🟢 Low | App logs grow unbounded |

## Requirements

### CI Pipeline (`.github/workflows/ci.yml`)

- Trigger on: `push` to `feature/seguimiento-premium`, `pull_request` to `main`
- Services: MySQL 8.0 container with healthcheck
- Steps:
  1. Checkout repository
  2. Setup Java 17 (Temurin)
  3. Cache Maven dependencies (`~/.m2`)
  4. Run `mvn test` — uses test profile connecting to MySQL container with Flyway `validate`
  5. (Optional) Upload test reports
- Must pass: 47/47 tests with 0 failures

### CD Pipeline (`.github/workflows/deploy-koyeb.yml`)

- Trigger on: `push` to `feature/seguimiento-premium`
- Steps:
  1. Checkout repository
  2. Setup Docker Buildx
  3. Login to GitHub Container Registry (GHCR)
  4. Build multi-platform image and push to `ghcr.io/monteastur-envios/app`
  5. Deploy to Koyeb via Koyeb CLI or API
- Must: only proceed if CI tests passed

### Docker Improvements

1. **BuildKit cache mounts** — cache Maven repository across builds
2. **Healthcheck fix** — replace `curl` with `wget -qO-` (available in `eclipse-temurin:17-jre`) or install `curl`
3. **Multi-platform builds** — `linux/amd64` + `linux/arm64`
4. **MySQL backup script** — `scripts/backup-mysql.sh` with timestamped dumps, 30-day rotation
5. **Log rotation** — configure logrotate for app logs or document host-level rotation

## Architecture Decisions

### Approach A: Single CI/CD Workflow vs Separate

**Decision:** Separate workflows (`ci.yml`, `deploy-koyeb.yml`).
- CI runs on every push/PR — fast feedback
- CD only on push to tracking branch
- CI uses `workflow_run` or reusable workflow to gate CD

### Approach B: Test Database Strategy

**Decision:** MySQL 8.0 service container in CI.
- Matches production exactly
- Flyway `validate` runs as part of `mvn test`
- No H2 fallback needed

### Approach C: Container Registry

**Decision:** GHCR (GitHub Container Registry).
- Native GitHub integration
- No extra credentials needed
- Koyeb can pull from GHCR directly

## Implementation Details

### CI Pipeline

```yaml
name: CI

on:
  push:
    branches: [feature/seguimiento-premium]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: envios_paraguay_cms_test
          MYSQL_USER: test_user
          MYSQL_PASSWORD: test_pass
        ports:
          - 3306:3306
        options: --health-cmd "mysqladmin ping -h localhost" --health-interval 10s
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 17
          distribution: temurin
          cache: maven
      - run: mvn test -B
        env:
          SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/envios_paraguay_cms_test
          DB_USERNAME: test_user
          DB_PASSWORD: test_pass
```

### CD Pipeline

```yaml
name: Deploy to Koyeb

on:
  push:
    branches: [feature/seguimiento-premium]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ghcr.io/${{ github.repository }}:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
      - uses: koyeb/actions-deploy@v1
        with:
          app-name: monteastur-envios
          service-name: app
          image: ghcr.io/${{ github.repository }}:${{ github.sha }}
          api-token: ${{ secrets.KOYEB_API_TOKEN }}
```

### Dockerfile Changes

```dockerfile
# ---- Stage 2: Build backend ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B
COPY src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static/
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -q

# ---- Stage 3: Runtime ----
FROM eclipse-temurin:17-jre
# wget is available in eclipse-temurin:17-jre by default
RUN useradd -m appuser && \
    mkdir -p /app/uploads /app/logs && \
    chown -R appuser:appuser /app
USER appuser
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

### Backup Script

Location: `scripts/backup-mysql.sh`
- Dumps MySQL database with timestamp
- Compresses with gzip
- Rotates backups older than 30 days
- Scheduled via host cron or Docker-based cron container

## Out of Scope

- Staging environment — single environment (dev/prod) with Koyeb
- Slack/email notifications — basic GitHub notifications sufficient
- Security scanning (Docker Scout, Trivy) — future improvement
- Database migration testing in CI beyond Flyway validate

## Testing Strategy

1. Push to `feature/seguimiento-premium` → CI triggers with MySQL container
2. Verify 47/47 tests pass
3. CD deploys to Koyeb automatically
4. Verify Swagger UI accessible at `/api/v1/swagger-ui.html`
5. Verify healthcheck endpoint returns 200
6. Manual trigger of backup script verifies dump creation

## Success Criteria

- [ ] CI pipeline runs tests with MySQL container — all green
- [ ] CD pipeline builds and deploys to Koyeb
- [ ] Docker build uses cached Maven dependencies (< 30s rebuild)
- [ ] Docker healthcheck passes (container stays healthy)
- [ ] MySQL backup script produces valid compressed dump
- [ ] Multi-platform build produces amd64 + arm64 images
