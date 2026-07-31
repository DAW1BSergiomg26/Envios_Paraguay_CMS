# Reporte de pruebas de carga (Bloque 9)

- Fecha: 2026-07-31
- Stack: app en `http://host.docker.internal:8080`, MySQL 8 + Redis 7 (docker compose)
- Scripts: `src/test/k6/load-test.js` (escenarios smoke/load/stress) + `helpers/data.js`, `helpers/auth.js`
- Ficheros de resultados: `smoke-20260731-114152.json|log` (escenario completado)

## Alcance ejecutado

| Escenario | Estado | Evidencia |
|---|---|---|
| Smoke | ✅ COMPLETADO | `smoke-20260731-114152.log` / `.json` |
| Load | ⏭️ No ejecutado a completitud | Run interrumpido; JSON parcial descartado (3.5 GB) |
| Stress | ⏭️ Omitido por decisión de alcance (2026-07-31) | — |

Los umbrales objetivo de Load (`p(95) < 500ms`, `error < 1%`) y Stress (`p(95) < 1000ms`, `error < 5%`)
**NO han sido verificados bajo carga real**; el gate funcional de esta fase fue el Smoke.

## Resultado del Smoke Test (1 VU, 10 iteraciones, todos los endpoints)

### Umbrales

| Threshold | Objetivo | Resultado | Estado |
|---|---|---|---|
| http_req_duration p(95) | < 500ms | 15.88ms | ✅ PASS |
| http_req_failed | < 1% | 0.00% (0/52) | ✅ PASS |

### Checks

- **63/63 = 100%** correctos (`checks_succeeded`).
- Todos los flujos verificados: `tracking GET 200`, `disponibilidad GET 200`, `reserva POST 201/409`, `login page GET 200`, `csrf token present`, `login POST redirects to dashboard`, `admin envios GET 200`.

### Métricas HTTP (agregadas)

| Métrica | Valor |
|---|---|
| http_req_duration avg | 6.86ms |
| http_req_duration p(90) | 7.99ms |
| http_req_duration p(95) | 15.88ms |
| http_req_duration max | 56.85ms |
| http_reqs totales | 52 (~140.9 rps) |
| iteration_duration p(95) | 86.73ms |

Nota: con 1 VU el desglose por endpoint no es estadísticamente relevante; se omite (se capturaría en un run Load completo con `--out json`).

## Comparativa cache vs DB

No cuantificable con datos de Smoke (1 VU). Hallazgo estructural conocido, confirmado en el código:

- `/api/v1/tracking/{codigo}` usa `EnvioTrackingRepository` directamente (consulta a MySQL por request) — **NO** aprovecha el cache Redis.
- `EnvioTrackingService.buscarPorCodigo` tiene `@Cacheable("envios.tracking")` pero **ningún controlador lo invoca**.
- `/api/v1/reservas/disponibilidad` sí usa el cache Redis (`envios.disponibilidad`).

## Hallazgos y recomendaciones

1. **Fixes necesarios para que los scripts fueran ejecutables** (detectados por el gate Smoke, ya aplicados y commiteados):
   - `options.responseCallback` (función) rompe la inicialización de k6 (`json: unsupported type: func`). Sustituido por `http.setResponseCallback(http.expectedStatuses({ min: 200, max: 499 }))` — 5xx/errores de red cuentan como fallo, 4xx esperados (409) no.
   - Regex de CSRF en `helpers/auth.js` no toleraba el orden de atributos renderizado (`type="hidden" name="_csrf"`); corregido a regex order-agnostic.
2. **Tracking sin caché**: enrutar el tracking API por `EnvioTrackingService.buscarPorCodigo` para aprovechar `envios.tracking` es la mejora de rendimiento de mayor impacto esperado.
3. **Pendiente (decisión de alcance):** ejecutar Load (50 VUs, 10 min) y Stress (10→200 VUs) para validar los umbrales objetivo bajo carga real y medir el punto de quiebre. Hasta entonces los presupuestos p95/error rate del Bloque 9 están verificados solo a nivel funcional (Smoke).
4. **Nota operativa:** el run Load interrumpido generó un JSON de ~3.5 GB; para futuros runs completos considerar `--out json` a un volumen efímero o resumen por endpoints (`--summary-trend-stats="avg,p(95),p(99)"`).
