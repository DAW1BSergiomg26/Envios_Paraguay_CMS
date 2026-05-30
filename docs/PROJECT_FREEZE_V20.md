# Project Freeze v20.0-pre-deploy — MonteAstur Envios

## Estado congelado

La versión `v20.0-pre-deploy` es la **release estable** del proyecto. Este documento describe su estado y qué no debe modificarse.

---

## Identificador

| Elemento | Valor |
|----------|-------|
| Tag Git | `v20.0-pre-deploy` |
| Commit | Verificar con `git log -1 v20.0-pre-deploy` |
| Rama base | `main` (tras merge) |
| Fecha | Mayo 2026 |
| Estado | ✅ Congelada |

---

## Estado actual

| Área | Estado |
|------|--------|
| **Backend tests** | 11/11 passed (Maven) |
| **Frontend unit tests** | 15/15 passed (Vitest) |
| **E2E tests** | 10/10 passed (Playwright) |
| **Frontend build** | OK (Vite + PWA) |
| **Docker compose** | 6/6 containers UP |
| **Healthcheck** | `{"status":"UP"}` |
| **Urls validadas** | 14/14 200 OK |
| **Seguridad** | Spring Security, CSRF híbrido, BCrypt |
| **Hardening** | Completo (ver HARDENING_FINAL_REPORT.md) |
| **Documentación** | Completa (ver RELEASE_V20_READY.md) |

---

## Qué está incluido

- Login admin con Spring Security
- Login cliente con sesión custom
- Tracking público con timeline
- Panel admin completo (envíos, imágenes, mensajes, reservas)
- Panel cliente seguro
- Dashboard React SPA
- PWA con service worker y offline
- Push notifications
- Galería CMS
- Formulario de contacto
- Demo data (4 envíos, 4 reservas, 4 imágenes SVG, etc.)
- Nginx reverse proxy con security headers
- Docker compose con 6 servicios
- Prometheus + Grafana monitoring
- CI/CD con GitHub Actions
- E2E tests con Playwright
- Documentación completa

---

## Qué NO está incluido (pendiente para futuras releases)

- Rate limiting (protección contra fuerza bruta)
- CORS para APIs externas
- Auditoría de accesos (quién hizo qué)
- Tests de integración con BD real
- HTTPS real (requiere VPS + Let's Encrypt)
- Backup automático (requiere VPS)
- fail2ban (requiere VPS)

---

## Qué no tocar

Para mantener la estabilidad de la release, **no modificar**:

```bash
# NO modificar sin PR y tests
src/main/java/com/monteastur/envios/     # Lógica de negocio
src/main/resources/application-prod.properties  # Config producción
docker-compose.yml                        # Orquestación
Dockerfile                                # Build
nginx/conf.d/                             # Proxy (local y prod)
frontend-react/src/                       # Lógica SPA
frontend-react/e2e/                       # Tests E2E
.github/workflows/                        # CI/CD
```

Se puede modificar:
```bash
docs/                                       # Documentación
scripts/                                    # Scripts auxiliares
frontend-react/tests/                       # Tests unitarios (nuevos)
.env.example / .env.production.example       # Plantillas
README.md                                   # Documentación principal
```

---

## Cómo levantar local

```bash
# 1. Clonar o checkout del tag
git checkout v20.0-pre-deploy

# 2. Crear .env (si no existe)
cp .env.example .env

# 3. Build y levantar
docker compose up -d --build

# 4. Esperar healthcheck
curl http://localhost:8080/actuator/health
# → {"status":"UP"}

# 5. Acceder
http://localhost:8090
```

---

## Cómo volver al estado estable

Si has hecho cambios y quieres volver al estado congelado:

```bash
# Opción 1: Hard reset al tag
git reset --hard v20.0-pre-deploy

# Opción 2: Checkout limpio en otro directorio
git clone <repo-url> monteastur-stable
cd monteastur-stable
git checkout v20.0-pre-deploy
```

---

## Cómo verificar que estás en la versión correcta

```bash
# Verificar tag
git describe --tags
# → v20.0-pre-deploy

# Verificar que no hay cambios sin commit
git status
# → working tree clean

# Verificar tests
mvn test -q                # → BUILD SUCCESS
cd frontend-react
npm run test:unit          # → 15 passed
npm run test:e2e           # → 10 passed
npm run build              # → OK
cd ..

# Verificar Docker
docker compose ps          # → 6/6 UP
curl http://localhost:8080/actuator/health  # → {"status":"UP"}
```

---

## Rama de desarrollo para nuevas features

```bash
# Crear rama desde develop (no desde main)
git checkout develop
git pull origin develop
git checkout -b feature/nueva-funcionalidad
```

La release `v20.0-pre-deploy` está en `main`. Las nuevas features se desarrollan en `develop` y se mergean a `main` cuando estén listas para la siguiente release.

---

## Documentos relacionados

| Documento | Enlace |
|-----------|--------|
| Release Notes | `docs/RELEASE_V20_READY.md` |
| Hardening Report | `docs/HARDENING_FINAL_REPORT.md` |
| Deploy Checklist | `docs/DEPLOY_REAL_READY_CHECKLIST.md` |
| E2E Guide | `docs/E2E_CI_GUIDE.md` |
| Local Commands | `docs/LOCAL_DEV_COMMANDS.md` |