# Pruebas de carga y estrés con k6

Ejecución vía Docker container `grafana/k6` (sin binario en el host). Target: la app
Spring Boot dockerizada en `http://host.docker.internal:8080` (puerto 8080 directo;
nginx en :80 queda fuera del alcance para aislar app+DB+Redis).

## Precondiciones

1. Daemon Docker corriendo.
2. Stack arriba: `docker compose up -d db redis app`
3. Seed de envíos aplicado: `src/test/k6/seed-envios.sql`
4. Credenciales admin en `.env` (`ADMIN_USERNAME`/`ADMIN_PASSWORD`).

## Ejecución

```pwsh
$env:SCENARIO = 'smoke'   # smoke | load | stress
$env:ADMIN_USERNAME = (Get-Content .env | Where-Object {$_ -match '^ADMIN_USERNAME='}).Split('=',2)[1]
$env:ADMIN_PASSWORD  = (Get-Content .env | Where-Object {$_ -match '^ADMIN_PASSWORD='}).Split('=',2)[1]
$ts = Get-Date -Format 'yyyyMMdd-HHmmss'

docker run --rm `
  --mount type=bind,source="${PWD}/src/test/k6",target=/scripts `
  --mount type=bind,source="${PWD}/src/test/k6/results",target=/results `
  -e SCENARIO=$env:SCENARIO -e BASE_URL=http://host.docker.internal:8080 `
  -e ADMIN_USERNAME=$env:ADMIN_USERNAME -e ADMIN_PASSWORD=$env:ADMIN_PASSWORD `
  --out json=/results/$env:SCENARIO-$ts.json `
  grafana/k6 run /scripts/load-test.js 2>&1 | Tee-Object -FilePath "src/test/k6/results/$env:SCENARIO-$ts.log"
```

Nota: `SCENARIO=load` dura ~10 min y `SCENARIO=stress` ~5 min; si se ejecuta desde una
tool con timeout por defecto, ampliar el timeout a >= 15 min.

## Escenarios

| Escenario | VUs | Duración | Umbrales |
|---|---|---|---|
| smoke | 1 | ~1-2 min (10 iteraciones, todos los endpoints) | p95<500ms, error<1% |
| load | 0→50 | 10 min (ramp 1m, plateau 8m, ramp-down 1m) | p95<500ms, error<1% |
| stress | 10→200 | 5 min (ramp, plateau 200, ramp-down) | p95<1000ms, error<5% |

Mix de Load: 60% tracking (DB), 25% disponibilidad (Redis), 10% POST reservas,
5% flujo admin (login + `/api/v1/admin/envios`).

## Variables de entorno k6

| Variable | Default | Descripción |
|---|---|---|
| `SCENARIO` | `load` | `smoke` \| `load` \| `stress` |
| `BASE_URL` | `http://host.docker.internal:8080` | App bajo prueba |
| `ADMIN_USERNAME` | `admin` | Usuario admin |
| `ADMIN_PASSWORD` | (vacío) | Password admin (desde `.env`) |
| `TRACKING_CODES` | 5 códigos seed | Códigos de envío separados por coma |

## Resultados

- `<scenario>-<timestamp>.json`: serie completa (exportación `--out json`)
- `<scenario>-<timestamp>.log`: summary de k6 (incluye p95/p99 por endpoint)
- `REPORT.md`: análisis y recomendaciones generados por el bloque

## Interpretación

- Comparar `http_req_duration` de tracking (DB directa) vs disponibilidad (Redis)
  en p95: la diferencia evidencia el beneficio de caché. Hallazgo conocido:
  `/api/v1/tracking/{codigo}` NO usa el cache `envios.tracking` de
  `EnvioTrackingService.buscarPorCodigo` (método sin invocar desde controladores).
