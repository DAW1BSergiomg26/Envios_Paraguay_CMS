# Checklist Rápido — Despliegue Cloud PaaS (Koyeb)

> **Contexto:** Envios_Paraguay_CMS en Java 25 + Spring Boot 3.3.5.
> Imagen: GHCR (multi-plataforma amd64/arm64) via `deploy-koyeb.yml`.
> Contenedor: `eclipse-temurin:25-jre`, non-root (`appuser`), HEALTHCHECK con `curl`.

---

## 1. El contenedor levanta

```bash
docker ps
```

- [ ] `monteastur-app` en estado `Up` (no `restarting` / `exited`).
- [ ] Logs sin excepciones de arranque:

```bash
docker logs monteastur-app --tail 100
```

- [ ] `Started MonteasturApplication in ... seconds` presente.
- [ ] Usuario del proceso `appuser` (no `root`):

```bash
docker exec monteastur-app whoami
```

---

## 2. `/actuator/health` responde UP

```bash
curl -fsS https://<APP_URL>/actuator/health
```

- [ ] Respuesta: `{"status":"UP",...}`.
- [ ] Si devuelve `503`/`DOWN`: el health agrega componentes (mail, redis, db). Verificar cuál falla:

```bash
curl -fsS https://<APP_URL>/actuator/health/liveness
curl -fsS https://<APP_URL>/actuator/health/readiness
```

- [ ] **Mail (causa más común de DOWN):** si no hay SMTP real, `spring.mail.host` apunta a `localhost:1025` y el `MailHealthIndicator` baja el health global. En cloud, o se configura un SMTP real o se deshabilita ese indicador:

```
MANAGEMENT_HEALTH_MAIL_ENABLED=false   # solo si no se usa email en ese entorno
```

- [ ] Healthcheck interno del contenedor en verde:

```bash
docker inspect monteastur-app --format '{{.State.Health.Status}}'
```

---

## 3. Variables de entorno de BD y seguridad inyectadas

Verificar desde dentro del contenedor que las envs existen (sin exponer el valor):

```bash
docker exec monteastur-app sh -c \
  'for v in SPRING_DATASOURCE_URL DB_USERNAME ADMIN_USERNAME REDIS_HOST SPRING_PROFILES_ACTIVE; do
     if [ -n "$(printenv $v)" ]; then echo "$v = OK"; else echo "$v = FALTA"; fi
   done'
```

Checklist por variable:

- [ ] `SPRING_PROFILES_ACTIVE=prod` (o `--spring.profiles.active=prod` en el ENTRYPOINT).
- [ ] `SPRING_DATASOURCE_URL` apunta a la BD **externa** (TiDB/MySQL managed), **no** a `db:3306` (hostname interno del compose).
- [ ] `DB_USERNAME` / `DB_PASSWORD` presentes.
- [ ] `ADMIN_USERNAME` / `ADMIN_PASSWORD` presentes (la app aborta el arranque en prod si faltan).
- [ ] `REDIS_HOST` / `REDIS_PORT` apuntan a un Redis **externo** (el default `redis:6379` del perfil prod es el hostname del compose local).
- [ ] `JAVA_OPTS` acotada a la memoria de la instancia (ej. `-Xms256m -Xmx384m`).

Confirmar que la app lee la BD correcta:

```bash
docker logs monteastur-app 2>&1 | grep -i "Flyway\|Successfully applied\|Validado"
```

- [ ] Migraciones Flyway aplicadas (esquema `validate`, nunca `update`).
- [ ] `DDL` no muta el esquema: verificar `spring.jpa.hibernate.ddl-auto=validate` en prod.

---

## 4. Mapeo de puertos y red en el proveedor

- [ ] El servicio expone el **puerto 8080** (único puerto de la app).
- [ ] Ruta `/` → target del servicio.
- [ ] TLS terminado en el proveedor (sin exponer 443 en el contenedor; Koyeb lo gestiona).
- [ ] Si el proveedor inyecta `PORT`, `server.port=${PORT:8080}` lo respeta.
- [ ] No se expone MySQL/Redis al exterior (BD y caché solo accesibles desde la red interna del proveedor o por IP allowlist).

---

## Estado de ajustes Koyeb (corregidos en `koyeb.yaml`)

| # | Hallazgo | Estado | Acción |
|---|----------|--------|--------|
| 1 | `DB_DDL_AUTO: update` engañoso (el perfil prod fija `ddl-auto=validate`) | ✅ Corregido | Variable eliminada del `koyeb.yaml` |
| 2 | `branch: feature/seguimiento-premium` (deploy no se dispara en `main`) | ⚠️ Pendiente decisión | Alinear la rama con la política de releases si se quiere deploy automático en `main` |
| 3 | Faltaba `REDIS_HOST`/`REDIS_PORT` | ✅ Corregido en el YAML | **Acción operativa:** provisionar Redis externo (Koyeb Redis/Upstash) y crear el secret `redis-host` |
| 4 | `UPLOAD_DIR: /tmp/uploads` | ⚠️ Conocido | Uploads efímeros en cada redeploy — considerar volumen persistente/object storage |
| 5 | SMTP sin configurar → health global `DOWN` | ✅ Corregido | `MANAGEMENT_HEALTH_MAIL_ENABLED=false` en el YAML; configurar SMTP real si se necesita email en prod |
| 6 | Placeholders sin rellenar en el YAML | ✅ Corregido | Mapeados a secrets Koyeb (ver más abajo) |

**Secrets a crear en Koyeb** (Settings → Secrets), referenciados por `koyeb.yaml`:

```text
tidb-username   → usuario de TiDB Serverless
tidb-password   → contraseña de TiDB Serverless
admin-password  → ADMIN_PASSWORD de la app (nunca en texto plano)
redis-host      → hostname del Redis externo
```

---

## Build de la imagen final (referencia)

```bash
docker build -t monteastur-app:final .
```

Verificado localmente:

```text
Runtime: openjdk 25.0.3 (Temurin) — eclipse-temurin:25-jre
JAR: Spring Boot fat-jar (92.6 MB) — incluye el frontend React build en static/
Multi-stage: node:20-alpine (frontend) → maven:3.9-eclipse-temurin-25 (build, con cache mount) → runtime
Healthcheck: curl -fsS http://localhost:${PORT:-8080}/actuator/health (interval 30s, start-period 40s)
Usuario: appuser (non-root)
Arranque verificado: Started MonteasturApplication in ~7s con health UP (infra completa)
```

---

> **Relacionados:** [`FINAL_PRODUCTION_DEPLOY_CHECKLIST.md`](FINAL_PRODUCTION_DEPLOY_CHECKLIST.md) (VPS), [`SMOKE_TESTS_PRODUCTION.md`](SMOKE_TESTS_PRODUCTION.md), [`POST_DEPLOY_CHECKS_REVIEW.md`](POST_DEPLOY_CHECKS_REVIEW.md).
