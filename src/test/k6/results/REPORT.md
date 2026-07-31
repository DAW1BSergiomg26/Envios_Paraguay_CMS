# Reporte de pruebas de carga y estrés (Bloque 9)

- Fecha: 2026-07-31
- Stack: app en `http://host.docker.internal:8080`, MySQL 8 + Redis 7 (docker compose, app con `mem_limit: 512m`)
- Scripts: `src/test/k6/load-test.js` (escenarios smoke/load/stress) + `helpers/auth.js`, `helpers/data.js`
- Ficheros de resultados (mismo directorio): `smoke-20260731-114152.{json,log}`, `load-20260731-203057.log`, `stress-20260731-204212.log`
- Ejecución: contenedor `grafana/k6` (sin binario en host). Mix de Load/Stress: 60% tracking (DB), 25% disponibilidad (Redis), 10% POST reservas, 5% flujo admin.

## Resumen de umbrales

| Escenario | Threshold | Objetivo | Resultado | Estado |
|---|---|---|---|---|
| smoke | http_req_duration p(95) | < 500ms | 15.88ms | ✅ PASS |
| smoke | http_req_failed | < 1% | 0.00% (0/52) | ✅ PASS |
| load | http_req_duration p(95) | < 500ms | 14.26ms | ✅ PASS |
| load | http_req_failed | < 1% | 0.04% (543/1.252.105) | ✅ PASS |
| stress | http_req_duration p(95) | < 1000ms | 41.75ms | ✅ PASS |
| stress | http_req_failed | < 5% | 0.06% (604/908.205) | ✅ PASS |

Los seis umbrales SLA/SLO se cumplieron con amplio margen.

## Métricas agregadas por escenario

| Escenario | VUs (max) | Duración | Requests | Throughput | avg | p(95) | p(99) | Error rate |
|---|---|---|---|---|---|---|---|---|
| smoke | 1 | ~0.4s útil | 52 | ~140.9 rps | 6.86ms | 15.88ms | — | 0.00% |
| load | 50 | 10m16s | 1.252.105 | 2.029.9 rps | 6.47ms | 14.26ms | 20.69ms | 0.04% |
| stress | 200 | 5m25s | 908.205 | 2.787.9 rps | 16.57ms | 41.75ms | 62.79ms | 0.06% |

Checks: smoke 63/63 (100%); load 99.95% (981 fallos de 2.205.656); stress 99.93% (1.091 de 1.599.733). Los fallos de checks son los mismos timeouts que explican el error rate HTTP.

## Métricas por endpoint

Mix observado en Load: tracking 57.1%, disponibilidad 23.8%, POST reservas 9.6%, flujo admin 4.8% (coincide con el mix 60/25/10/5 dentro del ruido aleatorio).

### Load (50 VUs, 616.8s)

| Endpoint | Tipo | % mix | Requests | rps | Errores | Error rate |
|---|---|---|---|---|---|---|
| GET /api/v1/tracking/{codigo} | GET (cache Redis) | 57.1% | 715.060 | 1.159 | 298 | 0.042% |
| GET /api/v1/reservas/disponibilidad | GET (Redis) | 23.8% | 297.936 | 483 | 140 | 0.047% |
| POST /api/v1/reservas | POST (Evict) | 9.6% | 119.991 | 195 | 49 | 0.041% |
| GET /api/v1/admin/envios | GET (RBAC) | 4.8% | 59.523 | 97 | 56 | 0.094% |
| GET+POST /login (setup por VU) | GET/POST | — | ~201 | — | 0 | 0.00% |

### Stress (200 VUs, 325.8s)

| Endpoint | Tipo | % mix | Requests | rps | Errores | Error rate |
|---|---|---|---|---|---|---|
| GET /api/v1/tracking/{codigo} | GET (cache Redis) | 57.2% | 519.209 | 1.594 | 338 | 0.065% |
| GET /api/v1/reservas/disponibilidad | GET (Redis) | 23.7% | 215.546 | 662 | 149 | 0.069% |
| POST /api/v1/reservas | POST (Evict) | 9.5% | 86.177 | 265 | 72 | 0.084% |
| GET /api/v1/admin/envios | GET (RBAC) | 4.8% | 43.445 | 133 | 44 | 0.101% |
| GET+POST /login (setup por VU) | GET/POST | — | 201 | — | 1 | 0.50% |

### Latencias por endpoint (baseline smoke 1 VU, extraído del JSON)

| Endpoint | avg | p(95) |
|---|---|---|
| GET /api/v1/tracking/{codigo} | 6.1ms | ~6.6ms |
| GET /api/v1/reservas/disponibilidad | 5.4ms | ~5.5ms |
| POST /api/v1/reservas | 8.7ms | 8.3ms |
| GET /api/v1/admin/envios | 3.4ms | 3.5ms |
| GET /login | 8.4ms | 3.9ms |

> **Limitación de datos:** el summary estándar de k6 no desglosa percentiles por endpoint; la exportación `--out json` para runs completos genera multi-GB (el run Load parcial previo llegó a 3.5 GB) y se descartó. Por eso los p(95) por endpoint bajo carga no están disponibles; los agregados (p95 load=14.26ms, stress=41.75ms) son reales y los counts/errores por endpoint son reales. Ver recomendación 5.

## Comparativa cache vs DB

- p(95) agregado Load: 14.26ms a 2.030 rps (60% del tráfico en el endpoint **cacheados**). Stress: 41.75ms a 2.788 rps.
- Aclaración importante: `GET /api/v1/tracking/{codigo}` **sí usa la caché** — `TrackingApiController` delega en `EnvioTrackingService.buscarPorCodigo` con `@Cacheable("envios.tracking", unless="#result == null")` desde el commit `4407c07` (anterior a estos runs k6). La versión anterior de este reporte afirmaba lo contrario; queda corregido. `/api/v1/reservas/disponibilidad` usa cache Redis (`envios.disponibilidad`).
- Baseline 1 VU: cache-hit (tracking) ≈ 6.1ms vs cache-hit (disponibilidad) ≈ 5.4ms → ratio 1.13x: a 5 filas seed, servir desde Redis frente a una query trivial de MySQL produce latencias indistinguibles. El beneficio del cache se materializa cuando la consulta es compleja o el volumen de datos crece (ver recomendación 4).

## Punto de quiebre (Stress)

- **No se alcanzó punto de quiebre en el rango probado** (10→200 VUs). p(95)=41.75ms = 4.2% del presupuesto (1000ms); error rate 0.06% = 1.2% del presupuesto (5%). La app sostuvo ~2.800 rps sin degradarse.
- Síntomas incipientes en la meseta de 200 VUs (minutos ~4-5) y en picos de ramp: timeouts `dial: i/o timeout` y `unexpected EOF` (establecimiento TCP), concentrados en ráfagas de pocos segundos (18:46:49-53 y 18:47:26-38). Indican saturación incipiente de la capa de conexiones/accept queue, no de CPU/memoria (contenedor sin OOM, `healthy` tras el run).
- Hallazgo adicional en logs de app durante el primer run Load (18:16): `Session was invalidated` y `RedisSystemException: Connection reset` en `/login` — errores de sesión Spring Session Redis bajo picos de creación de sesiones.

## Hallazgos y recomendaciones

1. **SLA cumplidos con amplio margen**: los tres escenarios pasan sus umbrales (p95 máx 41.75ms vs presupuesto 1000ms en stress; errores 0.06% vs 5%). La app es robusta hasta 200 VUs / ~2.800 rps en esta config (512m RAM).
2. **Primer síntoma de límite**: timeouts de conexión en la meseta de 200 VUs (~0.06%). Para encontrar el punto de quiebre real, ampliar stress a 300-500 VUs y/o incluir la capa nginx (:80) en el target.
3. **Errores de sesión Spring Session Redis bajo carga** (`Session was invalidated`, `Connection reset` en `/login`): revisar el pool de Lettuce y los timeouts de conexión a Redis; es el único ERROR de aplicación observado.
4. **Tracking en caché (verificado)**: `GET /api/v1/tracking/{codigo}` ya se sirve desde `@Cacheable("envios.tracking")` (TTL 5 min, commit `4407c07`). El sprint de optimización añadió un test de integración que blinda populate/evict/TTL. La evicción por escritura (`guardar`/`actualizarEstado`) es `allEntries`; si el volumen de escrituras crece, valorar evicción por clave (`key` del cache) para no purgar toda la caché en cada update.
5. **Operativa de resultados**: el summary estándar no da percentiles por endpoint y `--out json` genera multi-GB con 1M+ muestras. Adoptar un `handleSummary` compacto (por URL, en memoria) o exportar a volumen efímero; el `--summary-trend-stats="avg,p(95),p(99)"` usado aquí se queda con el agregado.
6. **Login bajo carga**: solo ~200 logins totales (1 por VU, correcto); el flujo admin completo (CSRF + session cookie por VU) funcionó al 100% de checks en smoke y >99% en load/stress.
