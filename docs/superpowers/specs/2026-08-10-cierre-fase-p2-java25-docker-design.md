# Spec — Cierre fase P2 + Java 25 + validación Docker + limpieza residuales

Fecha: 2026-08-10
Rama: `main`

## Contexto

- Único ítem P2 abierto: **P2.1 (Naming heredado Casa Rural / Monteastur)**. Quedan 2 referencias reales a `casarrural.com`:
  - `DataInitializer.java:223` (Aviso Legal insertado en demo data).
  - `OpenApiConfig.java:24` (email de contacto del bean OpenAPI).
- Discrepancia Java: `pom.xml` (17) y `Dockerfile` (temurin-17) vs CI/docs (25). JDK 25 local disponible en `~/.jdks/openjdk-25.0.2`.
- Docker daemon apagado; Compose v5.1.4 instalado. `.env` existe en raíz (ignorado).
- Residuales trackeados en git: `HANDOFF.md`, `Handoff_2.md`, `render.yaml - Envios_Paraguay_CMS.txt`, `start-all.bat`, `start-all.ps1`, `generate_handoff.py`. Logs ignorados en raíz: `app.log`, `app2.log`, `app-run.log`, `app-run.err.log`.

## Tareas

### T1 — Cierre P2.1: branding Casa Rural → Monteastur (TDD)
- `DataInitializer.java:223`: `info@casarrural.com` → `info@monteastur.com`
- `OpenApiConfig.java:24`: `admin@casarrural.com` → `admin@monteastur.com`
- Tests nuevos:
  - `OpenApiConfigTest`: verifica email de contacto del bean `OpenAPI`.
  - `DataInitializerTest`: verifica que el Aviso Legal generado no contiene `casarrural` y sí `monteastur`.
- Commit: `fix(p2.1): elimina emails heredados de Casa Rural en datos legales y OpenAPI`

### T2 — Alinear Java 25
- `pom.xml`: `java.version`/`source`/`target` → 25
- `Dockerfile`: stage build `maven:3.9-eclipse-temurin-17` → `-25`; runtime `eclipse-temurin:17-jre` → `:25-jre`
- Verificar: `JAVA_HOME=$env:USERPROFILE\.jdks\openjdk-25.0.2` + `mvn.cmd clean test`
- Commit: `chore(build): alinea runtime Java a 25 en pom y Dockerfile`

### T3 — Validación Docker local
- Arrancar Docker Desktop, esperar daemon.
- `docker compose config` (validar compose).
- `docker compose build` (multi-stage).
- `docker compose up -d`, esperar `/actuator/health` UP.
- `docker compose down`.

### T4 — Limpieza residuales + cierre backlog
- `git rm`: `Handoff_2.md`, `HANDOFF.md`, `render.yaml - Envios_Paraguay_CMS.txt`, `start-all.bat`, `start-all.ps1`, `generate_handoff.py`.
- Borrar del disco: `app.log`, `app2.log`, `app-run.log`, `app-run.err.log`.
- `docs/handoff.md`: referencias `start-all.ps1` → `start-app.ps1` (líneas ~12, 161).
- Backlog: marcar P2.1 `Estado: ✅ Cerrado` + Resultado + Criterio de cierre; actualizar "Decision actual".
- Commits: `chore: elimina scripts y documentos heredados obsoletos` + `docs: cierra P2.1 y actualiza decision log del backlog`

## Verificación final
- Suite de confirmación + regresión `mvn clean test` con JDK 25.
- Docker build/up/health real.
- Working tree limpio.
