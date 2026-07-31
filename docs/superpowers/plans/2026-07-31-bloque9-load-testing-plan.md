# Bloque 9: Pruebas de Carga y Estrés con k6 — Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar y ejecutar una batería de pruebas de carga (Smoke, Load, Stress) con k6 contra la aplicación Spring Boot dockerizada, con umbrales cuantitativos y un reporte ejecutable con hallazgos.

**Architecture:** Scripts k6 en `src/test/k6/` ejecutados vía el contenedor `grafana/k6` (sin binario en el host) contra la app en `http://host.docker.internal:8080`. Un único `load-test.js` con escenarios seleccionables por env `SCENARIO`, helpers de autenticación (form login + extracción de CSRF + cookies de sesión por VU) y de datos (fechas futuras aleatorias). Los resultados (JSON + summary logs + REPORT.md) se versionan en `src/test/k6/results/`.

**Tech Stack:** k6 (imagen `grafana/k6` latest), Docker Compose (db MySQL 8, redis 7, app Spring Boot 3.3.5), PowerShell 7 (host Windows).

## Global Constraints

- Ejecución de k6 SOLO vía Docker container `grafana/k6` — nunca instalar binario k6 en el host.
- `BASE_URL` apunta a la app directa `http://host.docker.internal:8080` (NO a nginx `:80` — se excluye para aislar app+DB+Redis).
- Umbrales: Smoke/Load `p(95) < 500ms` y `http_req_failed < 1%`; Stress `p(95) < 1000ms` y `http_req_failed < 5%`.
- Mix de Load (50 VUs): 60% tracking (DB), 25% disponibilidad (Redis), 10% POST reservas, 5% flujo admin.
- Prohibido modificar código de la aplicación; solo se añaden ficheros bajo `src/test/k6/` y los resultados versionados.
- `responseCallback` de k6 configurado para fallar solo con `status >= 500` (los 4xx esperados —p.ej. 409 de reservas— no inflan la tasa de error).
- Fechas de reservas generadas en runtime en rango futuro `+30..+180` días (garantizando salida > entrada) para evitar 400/409.
- Seed mínimo: 5 envíos con códigos únicos `MT-2026-0001`..`MT-2026-0005`.
- Credenciales admin se leen de `.env` (`ADMIN_USERNAME`/`ADMIN_PASSWORD`) y se pasan a k6 por env — nunca hardcodear el password.
- Resultados versionados en `src/test/k6/results/`: `<scenario>-<timestamp>.json` + `<scenario>-<timestamp>.log` + `REPORT.md`.
- Commits convencionales (estilo repo): `feat(k6): ...`, `docs(k6): ...`, `test(k6): ...`.
- Host Windows: `host.docker.internal` es el alias del host dentro de contenedores Docker Desktop.

---

### Task 1: Infraestructura de ejecución y seed de datos

**Files:**
- Create: `src/test/k6/seed-envios.sql`

**Interfaces:**
- Consumes: `docker-compose.yml` (servicios `db`, `redis`, `app`), `.env` (credenciales DB y admin), esquema `envios_tracking` (entidad `EnvioTracking.java`)
- Produces: Stack arriba y saludable en `http://localhost:8080`; 5 envíos con códigos conocidos en `envios_tracking`; tablas `envios_tracking` con los códigos seed que el script k6 de Task 2 usará.

- [ ] **Step 1: Verificar que el daemon de Docker está corriendo**

Run: `docker info`
Expected: sale con éxito (no error de conexión al daemon). Si falla, arrancar Docker Desktop y esperar a que esté `running`.

- [ ] **Step 2: Arrancar la stack de la aplicación**

Run: `docker compose up -d db redis app`
Expected: contenedores `monteastur-mysql`, `monteastur-redis`, `monteastur-app` creados y `Started` (comprobar con `docker compose ps`; `app` espera a que `db` y `redis` estén healthy vía `depends_on`).

- [ ] **Step 3: Verificar salud de la app y presencia de CSRF en /login**

Run:
```pwsh
Invoke-WebRequest -UseBasicParsing http://localhost:8080/ | Select-Object StatusCode
$login = Invoke-WebRequest -UseBasicParsing http://localhost:8080/login
$login.StatusCode
$login.Content -match 'name="_csrf"'
```
Expected: `200`, `200`, `True` (el form de Thymeleaf incluye el token `_csrf`).

- [ ] **Step 4: Leer credenciales admin y DB desde .env**

Run:
```pwsh
Get-Content .env | Where-Object { $_ -match '^(ADMIN_USERNAME|ADMIN_PASSWORD|MYSQL_USER|MYSQL_PASSWORD|MYSQL_DATABASE)=' }
```
Expected: valores no vacíos para `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_DATABASE`. Anotar `ADMIN_USERNAME`/`ADMIN_PASSWORD` (se usan en Tasks 2-3). Si `ADMIN_PASSWORD` es el default `change_me_secure_password`, seguir igualmente (entorno local).

- [ ] **Step 5: Crear el seed de envíos**

Create: `src/test/k6/seed-envios.sql`
```sql
-- Seed mínimo de envíos para pruebas de carga k6 (Bloque 9)
INSERT IGNORE INTO envios_tracking
  (codigo_unico, estado, destinatario, origen, destino, peso, contenido, fecha_creacion, ultima_actualizacion)
VALUES
  ('MT-2026-0001', 'EN_TRANSITO',      'Juan Pérez',      'Asturias',   'Asunción',        '12 kg', 'Documentos', NOW(), NOW()),
  ('MT-2026-0002', 'EN_ADUANA_DESTINO','María López',     'Madrid',     'Ciudad del Este', '5 kg',  'Ropa',       NOW(), NOW()),
  ('MT-2026-0003', 'ENTREGADO',        'Carlos Gómez',    'Barcelona',  'Asunción',        '20 kg', 'Mercancía',  NOW(), NOW()),
  ('MT-2026-0004', 'EN_REPARTO',       'Ana Martínez',    'Gijón',      'Encarnación',     '3 kg',  'Regalos',    NOW(), NOW()),
  ('MT-2026-0005', 'RECIBIDO',         'Pedro Fernández', 'Oviedo',     'Asunción',        '8 kg',  'Equipos',    NOW(), NOW());
```

- [ ] **Step 6: Aplicar el seed al contenedor MySQL**

Run:
```pwsh
docker compose cp src/test/k6/seed-envios.sql db:/tmp/seed-envios.sql
docker compose exec -T db sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < /tmp/seed-envios.sql'
```
Expected: sin errores (la contraseña y usuario se leen del `env_file` del contenedor `db`). `INSERT IGNORE` evita duplicados en re-ejecuciones.

- [ ] **Step 7: Verificar el seed vía la API de tracking**

Run:
```pwsh
Invoke-RestMethod http://localhost:8080/api/v1/tracking/MT-2026-0001
```
Expected: JSON con `codigoUnico = MT-2026-0001` y `estado`. También comprobar la disponibilidad (debe devolver 200 con campo `disponible`):
```pwsh
Invoke-RestMethod "http://localhost:8080/api/v1/reservas/disponibilidad?fechaEntrada=2026-09-01&fechaSalida=2026-09-10"
```

- [ ] **Step 8: Commit**

```bash
git add src/test/k6/seed-envios.sql
git commit -m "chore(k6): add envio seed data for load tests (Bloque 9)"
```

---

### Task 2: Scripts k6 (helpers + load-test.js + README)

**Files:**
- Create: `src/test/k6/helpers/auth.js`
- Create: `src/test/k6/helpers/data.js`
- Create: `src/test/k6/load-test.js`
- Create: `src/test/k6/README.md`
- Create: `src/test/k6/results/.gitkeep`

**Interfaces:**
- Consumes: Task 1 (stack en `http://localhost:8080`, códigos seed `MT-2026-0001`..`MT-2026-0005`), credenciales admin de `.env`
- Produces:
  - `helpers/auth.js` → `export function adminLogin(baseURL, username, password)` → `Response` de k6
  - `helpers/data.js` → `export function randomDateRange(minStartDays, maxStartDays, maxSpanDays)` → `{ fechaEntrada, fechaSalida }` (ISO `yyyy-MM-dd`); `export function randomTrackingCode(codes)` → `string`
  - `load-test.js` → `export const options` (con `scenarios`, `thresholds`, `responseCallback`) y `export default function ()` — usado por Tasks 3-4
  - `README.md` → instrucciones de ejecución usadas por Tasks 3-5

- [ ] **Step 1: Crear `helpers/data.js`**

Create: `src/test/k6/helpers/data.js`
```js
function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function toISODate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

// Rango futuro de fechas: inicio +minStartDays..+maxStartDays, salida = inicio + 1..maxSpanDays.
// Garantiza salida > entrada para no provocar 400 en la API.
export function randomDateRange(minStartDays = 30, maxStartDays = 180, maxSpanDays = 20) {
  const now = new Date();
  const start = new Date(now.getTime() + randomInt(minStartDays, maxStartDays) * 86400000);
  const end = new Date(start.getTime() + randomInt(1, maxSpanDays) * 86400000);
  return { fechaEntrada: toISODate(start), fechaSalida: toISODate(end) };
}

export function randomTrackingCode(codes) {
  return codes[randomInt(0, codes.length - 1)];
}
```

- [ ] **Step 2: Crear `helpers/auth.js`**

Create: `src/test/k6/helpers/auth.js`
```js
import http from 'k6/http';
import { check } from 'k6';

// Flujo de login admin: GET /login -> extraer _csrf -> POST /login con credenciales.
// k6 mantiene las cookies JSESSIONID por VU en su cookie jar, por lo que una única
// llamada por VU establece la sesión para iteraciones posteriores.
export function adminLogin(baseURL, username, password) {
  const loginPage = http.get(`${baseURL}/login`);
  check(loginPage, {
    'login page GET 200': (r) => r.status === 200,
  });

  const match = loginPage.body.match(/name="_csrf"\s+type="hidden"\s+value="([^"]+)"/);
  check(null, {
    'csrf token present': () => match !== null,
  });
  const csrfToken = match ? match[1] : '';

  const res = http.post(`${baseURL}/login`, {
    username,
    password,
    _csrf: csrfToken,
  });
  check(res, {
    'login POST redirects to dashboard': (r) => r.status === 200 && r.url.includes('/admin/dashboard'),
  });
  return res;
}
```

- [ ] **Step 3: Crear `load-test.js`**

Create: `src/test/k6/load-test.js`
```js
import http from 'k6/http';
import { check } from 'k6';
import { adminLogin } from './helpers/auth.js';
import { randomDateRange, randomTrackingCode } from './helpers/data.js';

const SCENARIO = __ENV.SCENARIO || 'load';
const BASE_URL = (__ENV.BASE_URL || 'http://host.docker.internal:8080').replace(/\/$/, '');
const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || 'admin';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || '';
const TRACKING_CODES = (__ENV.TRACKING_CODES || 'MT-2026-0001,MT-2026-0002,MT-2026-0003,MT-2026-0004,MT-2026-0005').split(',');

const SCENARIO_OPTIONS = {
  smoke: {
    executor: 'per-vu-iterations',
    vus: 1,
    iterations: 10,
  },
  load: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '1m', target: 50 },
      { duration: '8m', target: 50 },
      { duration: '1m', target: 0 },
    ],
  },
  stress: {
    executor: 'ramping-vus',
    startVUs: 10,
    stages: [
      { duration: '2m', target: 100 },
      { duration: '1m', target: 200 },
      { duration: '1m', target: 200 },
      { duration: '1m', target: 0 },
    ],
  },
};

const THRESHOLDS = {
  smoke:  { http_req_duration: ['p(95)<500'],  http_req_failed: ['rate<0.01'] },
  load:   { http_req_duration: ['p(95)<500'],  http_req_failed: ['rate<0.01'] },
  stress: { http_req_duration: ['p(95)<1000'], http_req_failed: ['rate<0.05'] },
};

export const options = {
  scenarios: {
    [SCENARIO]: SCENARIO_OPTIONS[SCENARIO],
  },
  thresholds: THRESHOLDS[SCENARIO],
  // Solo 5xx y errores de red cuentan como request fallido; 4xx esperados (409) no.
  responseCallback: (res) => res.status >= 500,
};

let adminReady = false;

function getTracking() {
  const res = http.get(`${BASE_URL}/api/v1/tracking/${randomTrackingCode(TRACKING_CODES)}`);
  check(res, {
    'tracking GET 200': (r) => r.status === 200,
    'tracking returns codigoUnico': (r) => r.json('codigoUnico') !== undefined,
  });
}

function getDisponibilidad() {
  const { fechaEntrada, fechaSalida } = randomDateRange();
  const res = http.get(`${BASE_URL}/api/v1/reservas/disponibilidad?fechaEntrada=${fechaEntrada}&fechaSalida=${fechaSalida}`);
  check(res, {
    'disponibilidad GET 200': (r) => r.status === 200,
    'disponibilidad returns disponible boolean': (r) => typeof r.json('disponible') === 'boolean',
  });
}

function postReserva() {
  const { fechaEntrada, fechaSalida } = randomDateRange();
  const payload = JSON.stringify({
    nombreCliente: 'Cliente k6',
    email: 'cliente.k6@test.local',
    telefono: '555-0000',
    fechaEntrada,
    fechaSalida,
    numeroHuespedes: 2,
    comentarios: 'Reserva de prueba generada por k6',
  });
  const res = http.post(`${BASE_URL}/api/v1/reservas`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, {
    'reserva POST 201 o 409 (fechas ocupadas)': (r) => r.status === 201 || r.status === 409,
  });
}

function adminFlow() {
  if (!adminReady) {
    const login = adminLogin(BASE_URL, ADMIN_USERNAME, ADMIN_PASSWORD);
    adminReady = login.status === 200;
  }
  if (adminReady) {
    const res = http.get(`${BASE_URL}/api/v1/admin/envios`);
    check(res, {
      'admin envios GET 200': (r) => r.status === 200,
    });
  }
}

export default function () {
  if (SCENARIO === 'smoke') {
    getTracking();
    getDisponibilidad();
    postReserva();
    adminFlow();
    return;
  }
  const roll = Math.random();
  if (roll < 0.60) getTracking();
  else if (roll < 0.85) getDisponibilidad();
  else if (roll < 0.95) postReserva();
  else adminFlow();
}
```

- [ ] **Step 4: Validar sintaxis y opciones con `k6 inspect`**

Run:
```pwsh
docker run --rm --mount type=bind,source="${PWD}/src/test/k6",target=/scripts grafana/k6 inspect /scripts/load-test.js
```
Expected: JSON de opciones con `scenarios` (una sola clave `load` por defecto), `thresholds` y `responseCallback`. Exit code 0. Si hay error de sintaxis/import, corregir y repetir.

- [ ] **Step 5: Verificación funcional end-to-end con Smoke (sin exportación JSON)**

Run (reemplazar `<ADMIN_USERNAME>`/`<ADMIN_PASSWORD>` por los valores reales de `.env` leídos en Task 1, Step 4):
```pwsh
docker run --rm --mount type=bind,source="${PWD}/src/test/k6",target=/scripts `
  -e SCENARIO=smoke -e BASE_URL=http://host.docker.internal:8080 `
  -e ADMIN_USERNAME=<ADMIN_USERNAME> -e ADMIN_PASSWORD=<ADMIN_PASSWORD> `
  grafana/k6 run /scripts/load-test.js
```
Expected: ejecución completa (1 VU, 10 iteraciones, ~1-2 min). En el summary: `checks` al 100%, sin thresholds en rojo (`http_req_duration p(95)<500` y `http_req_failed rate<0.01` en `PASS`). Si el check `login POST redirects to dashboard` falla, revisar credenciales y el regex de CSRF (el atributo debe ser `name="_csrf"` seguido de `type="hidden"` y `value=...`).

- [ ] **Step 6: Crear `README.md`**

Create: `src/test/k6/README.md`
````markdown
# Pruebas de carga y estrés con k6

Ejecución vía Docker container `grafana/k6` (sin binario en el host). Target: la app
Spring Boot dockerizada en `http://host.docker.internal:8080` (puerto 8080 directo;
nginx en :80 queda fuera del alcance para aislar app+DB+Redis).

## Precondiciones

1. Daemon Docker corriendo.
2. Stack arriba: `docker compose up -d db redis app`
3. Seed de envíos aplicado (Task 1): `src/test/k6/seed-envios.sql`
4. Credenciales admin en `.env` (`ADMIN_USERNAME`/`ADMIN_PASSWORD`).

## Ejecución

```pwsh
$env:SCENARIO = 'smoke'   # smoke | load | stress
$env:ADMIN_USERNAME = (Get-Content .env | Where-Object {$_ -match '^ADMIN_USERNAME='}).Split('=',2)[1]
$env:ADMIN_PASSWORD  = (Get-Content .env | Where-Object {$_ -match '^ADMIN_PASSWORD='}).Split('=',2)[1]
$ts = Get-Date -Format 'yyyyMMdd-HHmmss'

docker run --rm --mount type=bind,source="${PWD}/src/test/k6",target=/scripts `
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
````

- [ ] **Step 7: Commit**

```bash
git add src/test/k6/helpers/auth.js src/test/k6/helpers/data.js src/test/k6/load-test.js src/test/k6/README.md src/test/k6/results/.gitkeep
git commit -m "feat(k6): add load testing scripts for Bloque 9"
```

---

### Task 3: Ejecutar escenarios Smoke, Load y Stress

**Files:**
- Create: `src/test/k6/results/smoke-<timestamp>.json`, `src/test/k6/results/smoke-<timestamp>.log`
- Create: `src/test/k6/results/load-<timestamp>.json`, `src/test/k6/results/load-<timestamp>.log`
- Create: `src/test/k6/results/stress-<timestamp>.json`, `src/test/k6/results/stress-<timestamp>.log`

**Interfaces:**
- Consumes: Task 2 (`load-test.js` con escenarios y checks), Task 1 (stack + seed), credenciales admin de `.env`
- Produces: resultados JSON + summary logs por escenario, consumidos por Task 4

- [ ] **Step 1: Definir variables de entorno y timestamp**

Run:
```pwsh
$env:ADMIN_USERNAME = (Get-Content .env | Where-Object {$_ -match '^ADMIN_USERNAME='}).Split('=',2)[1]
$env:ADMIN_PASSWORD  = (Get-Content .env | Where-Object {$_ -match '^ADMIN_PASSWORD='}).Split('=',2)[1]
$ts = Get-Date -Format 'yyyyMMdd-HHmmss'
```
Expected: sin salida; variables pobladas (verificar con `$env:ADMIN_USERNAME`).

- [ ] **Step 2: Ejecutar Smoke y capturar resultados**

Run:
```pwsh
docker run --rm --mount type=bind,source="${PWD}/src/test/k6",target=/scripts `
  -e SCENARIO=smoke -e BASE_URL=http://host.docker.internal:8080 `
  -e ADMIN_USERNAME=$env:ADMIN_USERNAME -e ADMIN_PASSWORD=$env:ADMIN_PASSWORD `
  --out json=/results/smoke-$ts.json `
  grafana/k6 run /scripts/load-test.js 2>&1 | Tee-Object -FilePath "src/test/k6/results/smoke-$ts.log"
```
Expected: exit 0, checks al 100%, thresholds PASS. Confirmar que existen los ficheros:
```pwsh
Get-Item src/test/k6/results/smoke-$ts.json, src/test/k6/results/smoke-$ts.log
```

- [ ] **Step 3: Ejecutar Load y capturar resultados**

> La carga dura ~10 min. Si se usa una tool con timeout por defecto, ampliarlo a >= 15 min (900000 ms).

Run:
```pwsh
docker run --rm --mount type=bind,source="${PWD}/src/test/k6",target=/scripts `
  -e SCENARIO=load -e BASE_URL=http://host.docker.internal:8080 `
  -e ADMIN_USERNAME=$env:ADMIN_USERNAME -e ADMIN_PASSWORD=$env:ADMIN_PASSWORD `
  --out json=/results/load-$ts.json `
  grafana/k6 run /scripts/load-test.js 2>&1 | Tee-Object -FilePath "src/test/k6/results/load-$ts.log"
```
Expected: exit 0 (o exit con thresholds en rojo — es un fallo de umbral, no de ejecución), ficheros creados. Anotar si algún threshold quedó en `FAIL`.

- [ ] **Step 4: Ejecutar Stress y capturar resultados**

> Dura ~5 min. Ampliar timeout a >= 8 min (480000 ms) si es necesario.

Run:
```pwsh
docker run --rm --mount type=bind,source="${PWD}/src/test/k6",target=/scripts `
  -e SCENARIO=stress -e BASE_URL=http://host.docker.internal:8080 `
  -e ADMIN_USERNAME=$env:ADMIN_USERNAME -e ADMIN_PASSWORD=$env:ADMIN_PASSWORD `
  --out json=/results/stress-$ts.json `
  grafana/k6 run /scripts/load-test.js 2>&1 | Tee-Object -FilePath "src/test/k6/results/stress-$ts.log"
```
Expected: ficheros creados. Este escenario puede exceder umbrales relajados o degradarse — es el objetivo (observar punto de quiebre). Registrar qué umbrales fallaron y en qué intervalo (los logs por intervalo muestran la progresión).

- [ ] **Step 5: Commit resultados**

```bash
git add src/test/k6/results/
git commit -m "test(k6): run smoke/load/stress scenarios and capture results (Bloque 9)"
```

---

### Task 4: Generar el reporte de resultados

**Files:**
- Create: `src/test/k6/results/REPORT.md`

**Interfaces:**
- Consumes: logs/summaries de Task 3 (`smoke-*.log`, `load-*.log`, `stress-*.log` + sus `.json`)
- Produces: `REPORT.md` con umbrales PASS/FAIL, tablas por endpoint, comparativa cache vs DB, punto de quiebre y recomendaciones — deliverable final del bloque.

- [ ] **Step 1: Extraer métricas por endpoint de los summaries**

Los summaries de k6 (en los `.log`) contienen tablas con `http_req_duration` desglosada por `name` (endpoint) con `avg`, `p(95)`, `p(99)`, throughput y error rate. Extraer para cada escenario:

Run:
```pwsh
# Ver métricas de latencia y errores del Load (ajustar nombre de fichero real)
Select-String -Path "src/test/k6/results/load-*.log" -Pattern 'http_req_duration|http_req_failed|http_reqs|checks|thresholds|level='
```
Expected: líneas con las métricas agregadas y por endpoint. Registrar los valores `p(95)` de cada endpoint: `/api/v1/tracking/{codigo}`, `/api/v1/reservas/disponibilidad`, `/api/v1/reservas` (POST), `/login`, `/api/v1/admin/envios`.

- [ ] **Step 2: Determinar PASS/FAIL de umbrales por escenario**

Del summary de cada escenario, localizar la sección de thresholds (muestra `PASS`/`FAIL` para `http_req_duration: p(95)<...` y `http_req_failed: rate<...`). Anotar el resultado y, si hubo `FAIL`, el valor real observado y la fase (ramp-up, plateau, ramp-down) según los `level=warning`/`level=error` o la progresión en el log.

- [ ] **Step 3: Comparar tracking (DB) vs disponibilidad (Redis)**

Con los p95 extraídos en Step 1, calcular la diferencia entre `/api/v1/tracking/{codigo}` (consulta directa a MySQL) y `/api/v1/reservas/disponibilidad` (cache `envios.disponibilidad` en Redis). Anotar el ratio (p95_tracking / p95_disponibilidad) y si el beneficio de caché es evidente en p95, p99 y throughput.

- [ ] **Step 4: Determinar el punto de quiebre en Stress**

Revisar el log de stress (o el JSON) para localizar el intervalo a partir del cual `http_req_failed` supera el 1% o los p95 se disparan (progresión de VUs 100→200). Registrar el VU/intervalo aproximado del deterioro.

- [ ] **Step 5: Escribir `REPORT.md`**

Create: `src/test/k6/results/REPORT.md`
````markdown
# Reporte de pruebas de carga y estrés (Bloque 9)

- Fecha: <fecha de ejecución>
- Stack: app en `http://host.docker.internal:8080`, MySQL 8 + Redis 7 (docker compose)
- Scripts: `src/test/k6/load-test.js` (escenarios smoke/load/stress)
- Ficheros de resultados: `smoke-*.json|log`, `load-*.json|log`, `stress-*.json|log` (mismo directorio)

## Resumen de umbrales

| Escenario | Threshold | Objetivo | Resultado |
|---|---|---|---|
| smoke | http_req_duration p(95) | < 500ms | <PASS/FAIL + valor real> |
| smoke | http_req_failed | < 1% | <PASS/FAIL + valor real> |
| load | http_req_duration p(95) | < 500ms | <PASS/FAIL + valor real> |
| load | http_req_failed | < 1% | <PASS/FAIL + valor real> |
| stress | http_req_duration p(95) | < 1000ms | <PASS/FAIL + valor real> |
| stress | http_req_failed | < 5% | <PASS/FAIL + valor real> |

## Métricas por endpoint (escenario <load>)

| Endpoint | Tipo | Cache | avg | p(95) | p(99) | rps | error rate |
|---|---|---|---|---|---|---|---|
| GET /api/v1/tracking/{codigo} | GET | No (DB) | <valor> | <valor> | <valor> | <valor> | <valor> |
| GET /api/v1/reservas/disponibilidad | GET | Sí (Redis) | <valor> | <valor> | <valor> | <valor> | <valor> |
| POST /api/v1/reservas | POST | Evict | <valor> | <valor> | <valor> | <valor> | <valor> |
| GET /login + POST /login | GET/POST | — | <valor> | <valor> | <valor> | <valor> | <valor> |
| GET /api/v1/admin/envios | GET | — | <valor> | <valor> | <valor> | <valor> | <valor> |

## Comparativa cache vs DB

- p(95) tracking (DB directa): <valor> ms
- p(95) disponibilidad (Redis): <valor> ms
- Ratio y lectura: <análisis; si el cache no mejora, indicar causa probable>

## Punto de quiebre (Stress)

- El deterioro comienza en torno a <N> VUs / <minuto de la prueba>.
- Síntomas: <p95 se dispara / errores 5xx / checks fallidos>.

## Hallazgos y recomendaciones

1. <Hallazgo del Bloque 9: tracking API usa EnvioTrackingRepository directamente y no el
   cache `envios.tracking` de EnvioTrackingService.buscarPorCodigo (método sin invocar).>
2. <Otros hallazgos observados: 409 de reservas, latencia de login, CPU/DB...>
3. Recomendaciones accionables priorizadas (p.ej. enrutar tracking por el servicio cacheado,
   tuning de pools, índices...).
````
> Completar cada `<valor>` con los datos reales de los logs. El `REPORT.md` final NO debe contener placeholders `<>`.

- [ ] **Step 6: Commit**

```bash
git add src/test/k6/results/REPORT.md
git commit -m "docs(k6): add load test report for Bloque 9"
```

---

### Task 5: Verificación final y consistencia con la spec

**Files:**
- Review: `docs/superpowers/specs/2026-07-31-bloque9-load-testing-design.md`
- Review: `src/test/k6/README.md`, `src/test/k6/load-test.js`, `src/test/k6/results/REPORT.md`

**Interfaces:**
- Consumes: Tasks 1-4 (todo implementado y commiteado)
- Produces: confirmación de completitud y, si procede, commits de corrección de docs.

- [ ] **Step 1: Verificar árbol de trabajo y commits**

Run:
```bash
git status --short
git log --oneline -8
```
Expected: working tree limpio y 6 commits de este bloque (seed, scripts, resultados, report) tras el commit de la spec.

- [ ] **Step 2: Cross-check spec ↔ plan**

Leer `docs/superpowers/specs/2026-07-31-bloque9-load-testing-design.md` y confirmar que cada sección tiene implementación:
- [ ] Escenarios Smoke/Load/Stress → Task 2 `SCENARIO_OPTIONS` + Task 3
- [ ] Umbrales (500ms/1% y 1000ms/5%) → Task 2 `THRESHOLDS`
- [ ] Mix 60/25/10/5 → Task 2 `default()`
- [ ] Flujo admin completo (login + CSRF + RBAC) → Task 2 `helpers/auth.js`
- [ ] Seed mínimo 5 envíos → Task 1 `seed-envios.sql`
- [ ] Resultados en `src/test/k6/results/` + REPORT.md → Tasks 3-4
- [ ] Documentación de ejecución → Task 2 `README.md`

- [ ] **Step 3: Re-verificación rápida de la stack y un run smoke**

Run (mismo comando que Task 2 Step 5, sin exportación JSON):
```pwsh
docker run --rm --mount type=bind,source="${PWD}/src/test/k6",target=/scripts `
  -e SCENARIO=smoke -e BASE_URL=http://host.docker.internal:8080 `
  -e ADMIN_USERNAME=(Get-Content .env | Where-Object {$_ -match '^ADMIN_USERNAME='}).Split('=',2)[1] `
  -e ADMIN_PASSWORD=(Get-Content .env | Where-Object {$_ -match '^ADMIN_PASSWORD='}).Split('=',2)[1] `
  grafana/k6 run /scripts/load-test.js
```
Expected: checks al 100%, thresholds PASS.

- [ ] **Step 4: Commit final si hubo correcciones**

```bash
git add -A
git commit -m "docs(k6): final verification for Bloque 9"
```
Run: `git status --short` → Expected: limpio (si no hubo cambios, omitir el commit).

---

## Self-Review

- **Spec coverage:** Todas las secciones de la spec tienen tarea asignada (escenarios, umbrales, mix, auth, seed, resultados, reporte, README). El hallazgo de cache (tracking sin Redis) se ejecuta comparando endpoints en Task 4 y se documenta en REPORT.md. La decisión de ejecución por Docker container está en Global Constraints y en cada comando.
- **Placeholder scan:** Ningún paso usa "TBD/TODO"; los únicos `<>` son valores de métricas que Task 4 debe completar con datos reales y el propio Task 4 Step 5 lo prohíbe explícitamente en el reporte final. Nombres de fichero `*-<timestamp>.json` son artefactos de ejecución, no placeholders.
- **Type consistency:** `adminLogin(baseURL, username, password)` se define en Task 2 Step 2 y se consume en Task 2 Step 3. `randomDateRange(minStartDays, maxStartDays, maxSpanDays)` y `randomTrackingCode(codes)` se definen en Step 1 y consumen en Step 3. `options`/`default` de `load-test.js` se consumen en Tasks 3-5. Códigos seed `MT-2026-0001..0005` consistentes entre Task 1, Task 2 (default `TRACKING_CODES`) y README.
