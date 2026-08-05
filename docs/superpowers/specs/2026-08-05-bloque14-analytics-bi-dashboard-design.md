# Spec — Bloque 14: Módulo de Estadísticas y Analítica Avanzada (BI Dashboard)

- **Fecha:** 2026-08-05
- **Proyecto:** Envios_Paraguay_CMS (MONTEASTUR ENVIOS)
- **Estado:** Aprobado por el usuario (100%, sin reservas)
- **Contexto del repositorio:** `docs/handoff.md` (estado `0267304`), suite de 243 tests en verde.

## 1. Objetivo

Transformar el panel `/admin/dashboard` en un centro de control de Business Intelligence (BI) con:

- Métricas en tiempo real (KPIs).
- Agregación optimizada mediante índices MySQL (migración Flyway `V10`).
- Almacenamiento en caché Redis (`@Cacheable("envios.analytics")`, TTL 2 min) con respuestas sub-10ms.
- Gráficos interactivos con Chart.js estilizados bajo el sistema visual *Asturias-Paraguay*.

## 2. Reglas y restricciones arquitectónicas

1. **PROHIBIDO LOMBOK:** DTOs y servicios de analítica en Java puro (getters/setters/constructores manuales).
2. **Inyección por constructor:** campos `private final` inicializados en el constructor.
3. **Migración Flyway V10:** `src/main/resources/db/migration/V10__create_analytics_indexes.sql`.
4. **Caché Redis:** agregaciones cacheadas bajo el nombre `"envios.analytics"`.
5. **Tokens de color Chart.js:** Verde Bosque Asturiano (`#1B4D3B`, `#153C2D`), Naranja Paraguay (`#E67E22`), Esmeralda Confirmado (`#4ADE80`).
6. **Sin CDN en runtime:** Chart.js se vendorea localmente (`static/js/vendor/chart.umd.min.js`) y se commitea.

## 3. Decisiones de diseño aprobadas

| Decisión | Opción elegida |
|---|---|
| Alcance | Set completo: KPIs + 4 gráficos + actividad reciente existente |
| Agregación | JdbcTemplate dedicado con SQL nativo (`AnalyticsQueryService`) |
| Caché | `envios.analytics` TTL 2 min + `@CacheEvict(allEntries=true)` en escrituras |
| Frontend | REST API + `fetch` en cliente (CSRF desactivado en `/api/**`) |

## 4. Arquitectura

```
dashboard.html (Chart.js)
   │  fetch GET /api/v1/admin/analytics/resumen   (sesión admin)
   ▼
AnalyticsRestController  (@PreAuthorize("hasRole('ROLE_ADMIN')"))
   ▼
AnalyticsDashboardService.resumen()  ── @Cacheable("envios.analytics") → Redis (TTL 2 min)
   ▼
AnalyticsQueryService  (JdbcTemplate + SQL nativo)
   ▼
MySQL  (usa índices de la V10)

Escrituras (guardar/actualizarEstado/eliminar envío, procesarChunk/finalizar lote,
persistir WebhookLog) → @CacheEvict("envios.analytics", allEntries=true)
```

## 5. Componentes

### 5.1 Migración `V10__create_analytics_indexes.sql`

La columna real de `envios_tracking` es `estado` (NO `estado_actual`). Índices:

```sql
CREATE INDEX idx_envios_fecha_estado ON envios_tracking(fecha_creacion, estado);
CREATE INDEX idx_envios_origen_destino ON envios_tracking(origen, destino);
CREATE INDEX idx_webhook_logs_exitoso ON webhook_logs(exitoso, fecha_creacion);
```

Cabecera de archivo estándar del proyecto (InnoDB, `utf8mb4`), comentarios de propósito.
`V1` ya crea índices simples (`idx_envios_estado`, `idx_envios_ultima_actualizacion`,
`idx_envios_cliente_id`) y `V7` el `idx_envios_batch_id`; los compuestos de V10 son complementarios.

### 5.2 DTOs (Java puro, paquete `com.monteastur.envios.dto.analytics`)

- `AnalyticsSummaryDto` — agregado: `kpis` (List<KpiDto>), `enviosPorEstado` (List<EstadoCountDto>), `tendencia` (List<TendenciaDto>), `topRutas` (List<RutaDto>), `webhookPorDia` (List<WebhookPuntoDto>), `generadoEn` (LocalDateTime).
- `KpiDto` — `label`, `value` (double, admite tasas como 83.33; el frontend formatea enteros sin decimales), `color` (token hex).
- `EstadoCountDto` — `estado`, `cantidad`.
- `TendenciaDto` — `fecha` (LocalDate), `total`.
- `RutaDto` — `origen`, `destino`, `cantidad`.
- `WebhookPuntoDto` — `fecha` (LocalDate), `exitosos`, `total`, `tasaExito` (double, 0..100).

Todos con constructor vacío, constructor con parámetros, getters/setters manuales (compatibles con
`GenericJackson2JsonRedisSerializer` + `DefaultTyping.NON_FINAL` de `RedisConfig`).

### 5.3 `AnalyticsQueryService` (JdbcTemplate, SQL nativo)

`@Service`, inyección por constructor de `JdbcTemplate`. Consultas:

1. **KPIs:**
   - Total envíos: `SELECT COUNT(*) FROM envios_tracking`
   - En tránsito: `SELECT COUNT(*) FROM envios_tracking WHERE estado IN ('EN_ADUANA_ORIGEN','EN_TRANSITO','EN_ADUANA_DESTINO','EN_REPARTO')`
   - Entregados: `SELECT COUNT(*) FROM envios_tracking WHERE estado = 'ENTREGADO'`
   - Tasa éxito webhooks: `SELECT COUNT(*), SUM(exitoso) FROM webhook_logs` (tasa = exitosos/total * 100; 100 si no hay logs)
   - Reservas pendientes: `SELECT COUNT(*) FROM reservas WHERE estado = 'pendiente'`
2. **Por estado:** `SELECT estado, COUNT(*) AS cantidad FROM envios_tracking GROUP BY estado ORDER BY cantidad DESC, estado ASC`
3. **Tendencia N días (14):** `SELECT DATE(fecha_creacion) AS d, COUNT(*) AS total FROM envios_tracking WHERE fecha_creacion >= ? GROUP BY DATE(fecha_creacion) ORDER BY d ASC` (se completa con ceros los días sin datos, para 14 puntos).
4. **Top 5 rutas:** `SELECT COALESCE(origen,'N/D'), COALESCE(destino,'N/D'), COUNT(*) AS cantidad FROM envios_tracking GROUP BY origen, destino ORDER BY cantidad DESC LIMIT 5`
5. **Éxito webhooks por día (14):** `SELECT DATE(fecha_creacion) AS d, SUM(exitoso) AS ok, COUNT(*) AS total FROM webhook_logs WHERE fecha_creacion >= ? GROUP BY DATE(fecha_creacion) ORDER BY d ASC`

### 5.4 `AnalyticsDashboardService` (orquestación + caché)

`@Service`, inyección por constructor de `AnalyticsQueryService`.

- `@Cacheable(value = "envios.analytics", unless = "#result == null")` sobre `AnalyticsSummaryDto resumen()`.
- Ensambla los 5 bloques de `AnalyticsQueryService` y fija `generadoEn = LocalDateTime.now()`.
- `@CacheEvict(value = "envios.analytics", allEntries = true)` sobre `void refrescar()` (evict simple; la siguiente llamada a `resumen()` recalcula).

### 5.5 `AnalyticsRestController`

`@RestController`, `@RequestMapping("/api/v1/admin/analytics")`, `@PreAuthorize("hasRole('ROLE_ADMIN')")`.

- `GET /resumen` → `ResponseEntity<AnalyticsSummaryDto>` (200).
- `POST /refresh` → `ResponseEntity<AnalyticsSummaryDto>` (evict + recálculo; 200 con datos frescos).
- Swagger (`@Tag`, `@Operation`) coherente con `AdminApiController`.
- Sin filtros de fechas (YAGNI; TTL 2 min cubre frescura).

### 5.6 `RedisConfig` — registro de caché

Añadir a `configs`:
```java
"envios.analytics", defaultConfig.entryTtl(Duration.ofMinutes(2))
```

### 5.7 Invalidación en escrituras

Añadir `"envios.analytics"` a los `@CacheEvict` existentes de:

- `EnvioTrackingService.guardar`, `actualizarEstado`, `eliminar`.
- `BatchImportPersistenceService.procesarChunk`, `finalizar`.
- Punto de persistencia de `WebhookLog` (listener de `WebhookDispatchService`).

### 5.8 Resiliencia ante caída de Redis (`CacheErrorHandler`)

Bean `CacheErrorHandler` en la configuración que, ante cualquier error de caché, audita el fallo
(log con contexto del cache/método) y devuelve `null` para que Spring ejecute el método real
(fallback a base de datos). Coherente con "cero excepciones silenciadas".

### 5.9 Frontend

- **`static/js/vendor/chart.umd.min.js`:** descarga puntual en implementación (una sola vez) desde
  el CDN oficial y se commitea como asset local. Sin referencia CDN en runtime.
- **`static/js/analytics.js`:** IIFE ES5 (patrón `app.js`). Al `DOMContentLoaded` hace
  `fetch('/api/v1/admin/analytics/resumen')` y renderiza KPIs y los 4 gráficos con Chart.js.
  Estados vacíos → mensaje amigable. Fallo del fetch → banner + botón reintentar.
  Botón "Refrescar" → `POST /api/v1/admin/analytics/refresh` y re-render.
  Tema: respeta `prefers-color-scheme`/`data-theme` para fondos de ejes (usa `getComputedStyle`).
- **`cms/dashboard.html`:** nueva sección BI entre el resumen general y la actividad reciente:
  grid de KPI cards + grid de canvases (dona, línea tendencia, barras rutas, línea webhooks).
  Solo esta página carga `chart.umd.min.js` + `analytics.js` (al final del body).
  Se conserva la actividad reciente existente (decisión aprobada).
- **Paleta (constantes JS):**
  - `ENTREGADO` → `#4ADE80` (Esmeralda Confirmado)
  - `EN_TRANSITO` → `#E67E22` (Naranja Paraguay)
  - resto de estados → verdes bosque `#1B4D3B`, `#153C2D` y variantes
  - KPIs: total → `#1B4D3B`, tránsito → `#E67E22`, entregados → `#4ADE80`, webhooks → `#153C2D`, reservas → `#E67E22`.

## 6. API contract (GET /api/v1/admin/analytics/resumen)

```json
{
  "kpis": [
    { "label": "Total envíos", "value": 42, "color": "#1B4D3B" },
    { "label": "En tránsito", "value": 7, "color": "#E67E22" },
    { "label": "Entregados", "value": 30, "color": "#4ADE80" },
    { "label": "Éxito webhooks", "value": 95, "color": "#153C2D" },
    { "label": "Reservas pendientes", "value": 3, "color": "#E67E22" }
  ],
  "enviosPorEstado": [ { "estado": "ENTREGADO", "cantidad": 30 } ],
  "tendencia": [ { "fecha": "2026-07-23", "total": 2 } ],
  "topRutas": [ { "origen": "Asturias", "destino": "Asunción", "cantidad": 18 } ],
  "webhookPorDia": [ { "fecha": "2026-07-23", "exitosos": 5, "total": 6, "tasaExito": 83.33 } ],
  "generadoEn": "2026-08-05T10:15:30.123"
}
```

## 7. Manejo de errores

- Redis caído → `CacheErrorHandler` audita y cae a BD (fallback).
- Datos vacíos → gráficos con estado vacío ("Sin envíos registrados").
- Fallo del fetch → banner + botón reintentar en `analytics.js`.
- `@PreAuthorize` protege ambos endpoints (401/403 para no autenticados).

## 8. Testing (TDD)

| Test | Tipo | Qué verifica |
|---|---|---|
| `AnalyticsQueryServiceTest` | Unit (Mockito) | SQL correcto, mapeo `RowMapper`→DTO, tasa webhooks sin logs = 100 |
| `AnalyticsDashboardServiceTest` | Unit (cache `ConcurrentMapCacheManager`, patrón existente) | Ensamblado, `@Cacheable` (2ª llamada sin re-query), `@CacheEvict` en refresh |
| `AnalyticsRestControllerTest` | `@WebMvcTest` | 200 con sesión admin, 401 sin auth, contrato JSON (campos `kpis`, `tendencia`...) |
| `AnalyticsDashboardIntegrationTest` | `@SpringBootTest` (MySQL test) | Seed envíos + webhook_logs → KPIs/agregaciones correctas; 2ª llamada cacheada (chequea `CacheManager`) |

Meta de suite: **255+ tests en verde, 0 fallos** (243 existentes + los nuevos de analítica).

## 9. Handoff y versionado

- Commits por bloque con mensajes descriptivos.
- `git push origin main` con confirmación explícita (AGENTS.md) — autorizada para esta tarea.
- Actualizar `docs/handoff.md` con el cierre del Bloque 14 (nuevo total de tests).
