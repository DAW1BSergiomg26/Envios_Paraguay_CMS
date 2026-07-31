# Bloque 9: Pruebas de Carga y Estrés con k6 — Diseño Técnico

> **Para agentes autónomos:** REQUERIDO SUB-HABILIDAD: Usa subagentes de desarrollo impulsado por habilidades para implementar este bloque tarea por tarea.

**Objetivo:** Caracterizar el comportamiento de la aplicación bajo carga (Smoke, Load y Stress) usando k6, sobre los endpoints críticos de la stack Dockerizada (Spring Boot + MySQL + Redis), con umbrales cuantitativos (p95 < 500ms, error rate < 1%) y un reporte ejecutable con resultados y recomendaciones.

**Rol:** Arquitecto de Software / SRE. Diseño e implementación de la estrategia de pruebas de carga, no cambios de código de la aplicación salvo los mínimos necesarios para testear (seed de datos).

**Alcance:**
- Escenarios Smoke, Load y Stress definidos con `options` de k6 (VUs, duración, ramp-up, mix de endpoints)
- Umbrales automáticos: `p(95) < 500ms` y `http_req_failed < 1%` (Smoke/Load); `p(95) < 1000ms` y `http_req_failed < 5%` (Stress)
- Flujo autenticado de admin completo (form login + extracción de CSRF + reutilización de cookies de sesión) para golpear endpoints RBAC
- Seed mínimo de datos de prueba (3-5 envíos con código conocido) y generación de fechas futuras aleatorias para reservas
- Salida: resumen JSON, exportación `--out json` a `src/test/k6/results/`, y reporte markdown con hallazgos

**Fuera de alcance:**
- Cambios de código de la aplicación para optimizar rendimiento (solo se reportan hallazgos, no se implementan fix)
- Enrutar el tracking API por el servicio cacheado (`EnvioTrackingService.buscarPorCodigo`): se documenta el hallazgo, no se cambia
- Pruebas soak/longevidad (posible extensión futura)
- Monitoreo profundo de JVM/Redis/MySQL (solo observación a nivel HTTP con métricas de k6)

**Decisiones de arquitectura (confirmadas con el stakeholder):**
1. **Ejecución:** k6 vía Docker container `grafana/k6` (`docker run --rm -i grafana/k6 run ...`), sin instalar binario en el host. Versionado junto a la stack.
2. **Endpoint cacheado:** se prueban ambos — `/api/v1/tracking/{codigo}` (DB directa, sin caché) y `/api/v1/reservas/disponibilidad` (Redis, `@Cacheable("envios.disponibilidad")`) — y el reporte destaca la diferencia. **Hallazgo clave:** `/api/v1/tracking/{codigo}` usa `EnvioTrackingRepository` directamente y NO aprovecha el cache Redis existente en `EnvioTrackingService.buscarPorCodigo` (método actualmente sin invocar desde ningún controlador).
3. **Autenticación:** los escenarios incluyen el flujo admin completo. k6 hace `GET /login` (extrae token `_csrf` del HTML), `POST /login` con credenciales (credenciales de admin desde env), reutiliza cookies `JSESSIONID`/sesión y golpea `/api/v1/admin/envios`.
4. **Datos de prueba:** fechas futuras aleatorias generadas en runtime para `POST /api/v1/reservas` (evita 409 Conflict), y seed mínimo de 3-5 envíos con código único conocido para tracking. Sin limpieza destructiva tras el test.

**Endpoints bajo prueba:**

| Endpoint | Método | Auth | Cache | Esperado |
|---|---|---|---|---|
| `/api/v1/tracking/{codigo}` | GET | Pública | No (DB directa) | 200 |
| `/api/v1/reservas/disponibilidad?fechaEntrada=&fechaSalida=` | GET | Pública | Sí (Redis `envios.disponibilidad`) | 200 |
| `/api/v1/reservas` | POST | Pública | Evict en Redis | 201 |
| `/login` | GET | Pública | — | 200 (form, contiene `_csrf`) |
| `/login` | POST | Form | — | 302 → success admin |
| `/api/v1/admin/envios` | GET | `ROLE_ADMIN` | — | 200 |

**Mix de carga (Load, 50 VUs):** 60% tracking (DB), 25% disponibilidad (Redis), 10% POST reservas, 5% flujo admin (login + `/api/v1/admin/envios`). Login de admin: 1 sesión por VU, reutilizada en iteraciones posteriores.

**Estructura de scripts (`src/test/k6/`):**
- `load-test.js` — escenario seleccionable por `-e SCENARIO=smoke|load|stress`; contiene `options` por escenario, threshold definitions, mix de endpoints
- `helpers/auth.js` — flujo login (GET form, regex `_csrf`, POST credentials, almacenamiento de cookies por VU)
- `helpers/data.js` — generación de fechas futuras aleatorias, pool de códigos de envío
- `README.md` — instrucciones de ejecución con Docker, variables de entorno, interpretación de resultados

**Variables de entorno k6:**
- `BASE_URL` — base de la app (p.ej. `http://localhost:8080`)
- `SCENARIO` — `smoke` | `load` | `stress` (default `load`)
- `ADMIN_USERNAME` / `ADMIN_PASSWORD` — credenciales admin (default `admin` / valor del `.env`)
- `TRACKING_CODES` — códigos seed conocidos (default: generados por setup)

**Precondiciones de ejecución:**
1. Daemon de Docker arrancado y stack arriba: `docker compose up -d db redis app`
2. Migraciones Flyway aplicadas (V1 + V2 RBAC) — el arranque las aplica automáticamente
3. `DefaultUsersInitializer` siembra `admin` (ROLE_ADMIN) desde env
4. Seed de 3-5 envíos con código único (Tarea de setup, vía endpoint admin o script SQL/SQL seed en el contenedor MySQL)

**Umbrales y validaciones (checks k6):**
- Smoke/Load: `http_req_duration p(95) < 500ms`, `http_req_failed < 1%`
- Stress: `http_req_duration p(95) < 1000ms`, `http_req_failed < 5%`
- Checks por endpoint: 200/201/302 según tabla, y `JSESSIONID`/`_csrf` presentes en flujo login
- Comparativa reportada: tracking (DB) vs disponibilidad (Redis) en p95 y throughput — debe evidenciarse el beneficio de caché

**Salidas y reporte:**
- `src/test/k6/results/<scenario>-<timestamp>.json` (exportación `--out json`)
- Resumen de k6 (summary trend con `p(95)`, `p(99)`)
- Reporte markdown `src/test/k6/results/REPORT.md` con: umbrales alcanzados (PASS/FAIL), tablas por endpoint (avg, p95, p99, throughput, error rate), comparativa cache vs DB, punto de quiebre en stress, recomendaciones accionables

**Plan de implementación (tareas para subagentes):**
- Tarea 1: Preparar infraestructura de ejecución — arrancar Docker daemon, stack (db, redis, app), verificar salud de endpoints y credenciales admin; seed mínimo de envíos
- Tarea 2: Crear scripts k6 (`helpers/auth.js`, `helpers/data.js`, `load-test.js`) con escenarios y umbrales
- Tarea 3: Ejecutar escenarios contra la stack (Smoke → Load → Stress) y capturar resultados JSON
- Tarea 4: Generar reporte `REPORT.md` con análisis, comparativa cache/DB y recomendaciones
- Tarea 5: Verificación final — scripts documentados en README, resultados versionados, hallazgos consistentes con la spec

**Riesgos y mitigaciones:**
- **CSRF en `/login`:** el token se extrae del HTML del form (`name="_csrf"` valor). k6 lo reutiliza por sesión. Mitigación: helper `auth.js` centralizado.
- **409 Conflict en reservas:** mitigado con fechas futuras aleatorias en un rango amplio (p.ej. +30..+180 días).
- **Volumen de auditoría RBAC:** cada acceso a `/api/v1/admin/envios` registra `auditoria_accesos`; con 5% del tráfico Load es aceptable, se reporta si crece.
- **Daemon Docker apagado:** precondición bloqueante; el plan la maneja como primer paso con verificación explícita.
- **Redes de sesión por VU:** cada VU mantiene su propio contexto de cookies; verificar que el login no se rehaga en cada iteración (flag por VU).

**Ambiente:**
- k6 latest (imagen `grafana/k6`)
- Spring Boot 3.3.5, Java 17 (bajo prueba, sin cambios)
- MySQL 8 (`db` compose), Redis 7 (`redis` compose)
- Windows host, PowerShell; ejecución k6 por Docker Desktop

**Referencias:**
- `src/main/java/com/monteastur/envios/controller/TrackingApiController.java` (tracking API, repo directo)
- `src/main/java/com/monteastur/envios/controller/ReservaPublicApiController.java` (POST reservas + disponibilidad)
- `src/main/java/com/monteastur/envios/service/ReservaService.java` (caches `envios.reservas`/`envios.disponibilidad`)
- `src/main/java/com/monteastur/envios/service/EnvioTrackingService.java` (cache `envios.tracking` no usado por controladores)
- `src/main/java/com/monteastur/envios/config/SecurityConfig.java` (formLogin `/login`, CSRF ignorado solo para `/api/**`)
- `docker-compose.yml` (db, app, nginx, redis)
