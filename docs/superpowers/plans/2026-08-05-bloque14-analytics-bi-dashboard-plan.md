# Bloque 14 — BI Dashboard: Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transformar `/admin/dashboard` en un centro de control BI con KPIs, 4 gráficos Chart.js, agregaciones SQL nativas indexadas (V10) y caché Redis `envios.analytics` (TTL 2 min).

**Architecture:** Capa analítica nueva: `AnalyticsQueryService` (JdbcTemplate + SQL nativo) → `AnalyticsDashboardService` (`@Cacheable("envios.analytics")`) → `AnalyticsRestController` (`/api/v1/admin/analytics/resumen` y `/refresh`). El frontend consume la API por `fetch` y renderiza con Chart.js vendoreado localmente. Las escrituras de envíos/webhooks/lotes invalidan la caché vía `@CacheEvict`.

**Tech Stack:** Java 25, Spring Boot 3.3.5, Spring JDBC (`JdbcTemplate`), Spring Cache + Redis, Flyway 10, MySQL 8, Thymeleaf, Chart.js 4.5.1 (vendored), JUnit 5 + AssertJ + Mockito.

## Global Constraints

- **Sin Lombok:** DTOs y servicios en Java puro (constructor vacío + con parámetros + getters/setters manuales).
- **Inyección por constructor:** dependencias `private final`.
- **Migración Flyway** `src/main/resources/db/migration/V10__create_analytics_indexes.sql`; la columna real de `envios_tracking` es **`estado`** (no `estado_actual`).
- **Caché Redis** bajo el nombre exacto `"envios.analytics"`, TTL 2 minutos.
- **Tokens de color oficiales:** Verde Bosque Asturiano `#1B4D3B`/`#153C2D`, Naranja Paraguay `#E67E22`, Esmeralda Confirmado `#4ADE80`.
- **Sin CDN en runtime:** Chart.js vendoreado en `src/main/resources/static/js/vendor/chart.umd.min.js` (descarga única en implementación, se commitea).
- **Validación:** suite completa `mvn clean test` en BUILD SUCCESS, 0 fallos (meta: 255+ tests).
- Maven local: `$env:JAVA_HOME="C:\Users\astur\.jdks\openjdk-25.0.2"` + `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd`.
- Tests: AssertJ; unit con Mockito; caché con `ConcurrentMapCacheManager`; integración `@SpringBootTest @ActiveProfiles("test")`.
- Commits pequeños; push final a `main` con mensaje exacto del usuario (autorizado).

## File Structure

**Crear:**
- `src/main/resources/db/migration/V10__create_analytics_indexes.sql`
- `src/main/java/com/monteastur/envios/dto/analytics/AnalyticsSummaryDto.java`
- `src/main/java/com/monteastur/envios/dto/analytics/KpiDto.java`
- `src/main/java/com/monteastur/envios/dto/analytics/EstadoCountDto.java`
- `src/main/java/com/monteastur/envios/dto/analytics/TendenciaDto.java`
- `src/main/java/com/monteastur/envios/dto/analytics/RutaDto.java`
- `src/main/java/com/monteastur/envios/dto/analytics/WebhookPuntoDto.java`
- `src/main/java/com/monteastur/envios/service/analytics/AnalyticsQueryService.java`
- `src/main/java/com/monteastur/envios/service/analytics/AnalyticsDashboardService.java`
- `src/main/java/com/monteastur/envios/controller/api/AnalyticsRestController.java`
- `src/main/java/com/monteastur/envios/config/CacheAuditErrorHandler.java`
- `src/main/resources/static/js/vendor/chart.umd.min.js`
- `src/main/resources/static/js/analytics.js`
- Tests (ver cada tarea)

**Modificar:**
- `src/main/java/com/monteastur/envios/config/RedisConfig.java` (caché `envios.analytics` + `errorHandler()`)
- `src/main/java/com/monteastur/envios/service/EnvioTrackingService.java` (3 `@CacheEvict`)
- `src/main/java/com/monteastur/envios/service/batch/BatchImportPersistenceService.java` (2 `@CacheEvict`)
- `src/main/java/com/monteastur/envios/service/WebhookDispatchService.java` (1 `@CacheEvict`)
- `src/main/resources/templates/cms/dashboard.html` (sección BI + scripts)
- `src/main/resources/static/css/design-system.css` (clases BI, vía ensamblado)
- `src/test/java/com/monteastur/envios/controller/AdminThemeAssetsTest.java` (assert BI)
- `docs/handoff.md`

---

### Task 1: Migración Flyway V10 (índices de agregación)

**Files:**
- Create: `src/main/resources/db/migration/V10__create_analytics_indexes.sql`
- Test: `src/test/java/com/monteastur/envios/migration/AnalyticsMigrationTest.java`

**Interfaces:**
- Produces: 3 índices compuestos que optimizan los `GROUP BY`/`WHERE` de `AnalyticsQueryService` (Task 3).

- [ ] **Step 1: Write the failing test**

```java
package com.monteastur.envios.migration;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsMigrationTest {

    @Test
    void v10_creaLosTresIndicesDeAnalitica() throws Exception {
        String sql;
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V10__create_analytics_indexes.sql")) {
            assertThat(in).as("la migración V10 debe existir en classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql).contains("idx_envios_fecha_estado ON envios_tracking(fecha_creacion, estado)");
        assertThat(sql).contains("idx_envios_origen_destino ON envios_tracking(origen, destino)");
        assertThat(sql).contains("idx_webhook_logs_exitoso ON webhook_logs(exitoso, fecha_creacion)");
        assertThat(sql).doesNotContain("estado_actual");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
$env:JAVA_HOME="C:\Users\astur\.jdks\openjdk-25.0.2"
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AnalyticsMigrationTest
```
Expected: FAIL (recurso `V10__create_analytics_indexes.sql` no existe).

- [ ] **Step 3: Write the migration**

```sql
-- ============================================================
-- V10: Índices de agregación para el Dashboard BI
-- Optimiza las consultas de AnalyticsQueryService (JdbcTemplate):
--   * envíos por estado y tendencia por fecha (fecha_creacion, estado)
--   * top de rutas origen -> destino
--   * tasa de éxito de webhooks por día (exitoso, fecha_creacion)
-- ============================================================

CREATE INDEX idx_envios_fecha_estado ON envios_tracking(fecha_creacion, estado);
CREATE INDEX idx_envios_origen_destino ON envios_tracking(origen, destino);
CREATE INDEX idx_webhook_logs_exitoso ON webhook_logs(exitoso, fecha_creacion);
```

- [ ] **Step 4: Run test to verify it passes**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AnalyticsMigrationTest
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V10__create_analytics_indexes.sql src/test/java/com/monteastur/envios/migration/AnalyticsMigrationTest.java
git commit -m "feat(analytics): migración V10 con índices de agregación BI"
```

---

### Task 2: DTOs de analítica en Java puro

**Files:**
- Create: `src/main/java/com/monteastur/envios/dto/analytics/AnalyticsSummaryDto.java`, `KpiDto.java`, `EstadoCountDto.java`, `TendenciaDto.java`, `RutaDto.java`, `WebhookPuntoDto.java`
- Test: `src/test/java/com/monteastur/envios/dto/analytics/AnalyticsDtoSerializationTest.java`

**Interfaces:**
- Produces: `AnalyticsSummaryDto` con `getKpis/setKpis`, `getEnviosPorEstado/setEnviosPorEstado`, `getTendencia/setTendencia`, `getTopRutas/setTopRutas`, `getWebhookPorDia/setWebhookPorDia`, `getGeneradoEn/setGeneradoEn`. Los consumen Tasks 3, 4, 6 y 8.

- [ ] **Step 1: Write the failing test**

```java
package com.monteastur.envios.dto.analytics;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsDtoSerializationTest {

    private static ObjectMapper cacheMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    }

    @Test
    void analyticsSummaryDto_roundTripConSerializerRedis() throws Exception {
        ObjectMapper mapper = cacheMapper();
        AnalyticsSummaryDto dto = new AnalyticsSummaryDto();
        dto.setKpis(List.of(new KpiDto("Total envíos", 42.0, "#1B4D3B")));
        dto.setEnviosPorEstado(List.of(new EstadoCountDto("ENTREGADO", 30L)));
        dto.setTendencia(List.of(new TendenciaDto(LocalDate.of(2026, 8, 1), 2L)));
        dto.setTopRutas(List.of(new RutaDto("Asturias", "Asunción", 18L)));
        dto.setWebhookPorDia(List.of(new WebhookPuntoDto(LocalDate.of(2026, 8, 1), 5L, 6L, 83.3)));
        dto.setGeneradoEn(LocalDateTime.of(2026, 8, 5, 10, 15));

        String json = mapper.writeValueAsString(dto);
        AnalyticsSummaryDto copia = mapper.readValue(json, AnalyticsSummaryDto.class);

        assertThat(copia.getKpis()).hasSize(1);
        assertThat(copia.getKpis().get(0).getLabel()).isEqualTo("Total envíos");
        assertThat(copia.getKpis().get(0).getValue()).isEqualTo(42.0);
        assertThat(copia.getKpis().get(0).getColor()).isEqualTo("#1B4D3B");
        assertThat(copia.getEnviosPorEstado().get(0).getCantidad()).isEqualTo(30L);
        assertThat(copia.getTendencia().get(0).getFecha()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(copia.getTopRutas().get(0).getDestino()).isEqualTo("Asunción");
        assertThat(copia.getWebhookPorDia().get(0).getTasaExito()).isEqualTo(83.3);
        assertThat(copia.getGeneradoEn()).isEqualTo(LocalDateTime.of(2026, 8, 5, 10, 15));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AnalyticsDtoSerializationTest
```
Expected: FAIL (clases de DTO no existen).

- [ ] **Step 3: Write the DTOs**

`AnalyticsSummaryDto.java`:
```java
package com.monteastur.envios.dto.analytics;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsSummaryDto {

    private List<KpiDto> kpis = new ArrayList<>();
    private List<EstadoCountDto> enviosPorEstado = new ArrayList<>();
    private List<TendenciaDto> tendencia = new ArrayList<>();
    private List<RutaDto> topRutas = new ArrayList<>();
    private List<WebhookPuntoDto> webhookPorDia = new ArrayList<>();
    private LocalDateTime generadoEn;

    public AnalyticsSummaryDto() {}

    public List<KpiDto> getKpis() { return kpis; }
    public void setKpis(List<KpiDto> kpis) { this.kpis = kpis; }
    public List<EstadoCountDto> getEnviosPorEstado() { return enviosPorEstado; }
    public void setEnviosPorEstado(List<EstadoCountDto> enviosPorEstado) { this.enviosPorEstado = enviosPorEstado; }
    public List<TendenciaDto> getTendencia() { return tendencia; }
    public void setTendencia(List<TendenciaDto> tendencia) { this.tendencia = tendencia; }
    public List<RutaDto> getTopRutas() { return topRutas; }
    public void setTopRutas(List<RutaDto> topRutas) { this.topRutas = topRutas; }
    public List<WebhookPuntoDto> getWebhookPorDia() { return webhookPorDia; }
    public void setWebhookPorDia(List<WebhookPuntoDto> webhookPorDia) { this.webhookPorDia = webhookPorDia; }
    public LocalDateTime getGeneradoEn() { return generadoEn; }
    public void setGeneradoEn(LocalDateTime generadoEn) { this.generadoEn = generadoEn; }
}
```

`KpiDto.java`:
```java
package com.monteastur.envios.dto.analytics;

public class KpiDto {

    private String label;
    private double value;
    private String color;

    public KpiDto() {}

    public KpiDto(String label, double value, String color) {
        this.label = label;
        this.value = value;
        this.color = color;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
```

`EstadoCountDto.java`:
```java
package com.monteastur.envios.dto.analytics;

public class EstadoCountDto {

    private String estado;
    private long cantidad;

    public EstadoCountDto() {}

    public EstadoCountDto(String estado, long cantidad) {
        this.estado = estado;
        this.cantidad = cantidad;
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public long getCantidad() { return cantidad; }
    public void setCantidad(long cantidad) { this.cantidad = cantidad; }
}
```

`TendenciaDto.java`:
```java
package com.monteastur.envios.dto.analytics;

import java.time.LocalDate;

public class TendenciaDto {

    private LocalDate fecha;
    private long total;

    public TendenciaDto() {}

    public TendenciaDto(LocalDate fecha, long total) {
        this.fecha = fecha;
        this.total = total;
    }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
}
```

`RutaDto.java`:
```java
package com.monteastur.envios.dto.analytics;

public class RutaDto {

    private String origen;
    private String destino;
    private long cantidad;

    public RutaDto() {}

    public RutaDto(String origen, String destino, long cantidad) {
        this.origen = origen;
        this.destino = destino;
        this.cantidad = cantidad;
    }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public long getCantidad() { return cantidad; }
    public void setCantidad(long cantidad) { this.cantidad = cantidad; }
}
```

`WebhookPuntoDto.java`:
```java
package com.monteastur.envios.dto.analytics;

import java.time.LocalDate;

public class WebhookPuntoDto {

    private LocalDate fecha;
    private long exitosos;
    private long total;
    private double tasaExito;

    public WebhookPuntoDto() {}

    public WebhookPuntoDto(LocalDate fecha, long exitosos, long total, double tasaExito) {
        this.fecha = fecha;
        this.exitosos = exitosos;
        this.total = total;
        this.tasaExito = tasaExito;
    }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public long getExitosos() { return exitosos; }
    public void setExitosos(long exitosos) { this.exitosos = exitosos; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public double getTasaExito() { return tasaExito; }
    public void setTasaExito(double tasaExito) { this.tasaExito = tasaExito; }
}
```

- [ ] **Step 4: Run test to verify it passes**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AnalyticsDtoSerializationTest
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/dto/analytics/ src/test/java/com/monteastur/envios/dto/analytics/AnalyticsDtoSerializationTest.java
git commit -m "feat(analytics): DTOs de analítica en Java puro (compatibles con serializer Redis)"
```

---

### Task 3: AnalyticsQueryService (JdbcTemplate + SQL nativo)

**Files:**
- Create: `src/main/java/com/monteastur/envios/service/analytics/AnalyticsQueryService.java`
- Test: `src/test/java/com/monteastur/envios/service/analytics/AnalyticsQueryServiceTest.java`

**Interfaces:**
- Consumes: DTOs de Task 2 (`KpiDto`, `EstadoCountDto`, `TendenciaDto`, `RutaDto`, `WebhookPuntoDto`).
- Produces: `AnalyticsQueryService` con `List<KpiDto> kpis()`, `List<EstadoCountDto> enviosPorEstado()`, `List<TendenciaDto> tendenciaUltimosDias(int dias)`, `List<RutaDto> topRutas(int limite)`, `List<WebhookPuntoDto> webhookPorDia(int dias)`; estáticos de prueba: `calcularTasa(long, long)`, `mapEstadoCount(ResultSet)`, `mapRuta(ResultSet)`, `completarTendencia(Map, LocalDate, int)`, `completarWebhook(Map, LocalDate, int)`.

- [ ] **Step 1: Write the failing test**

```java
package com.monteastur.envios.service.analytics;

import com.monteastur.envios.dto.analytics.EstadoCountDto;
import com.monteastur.envios.dto.analytics.KpiDto;
import com.monteastur.envios.dto.analytics.RutaDto;
import com.monteastur.envios.dto.analytics.TendenciaDto;
import com.monteastur.envios.dto.analytics.WebhookPuntoDto;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsQueryServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AnalyticsQueryService service = new AnalyticsQueryService(jdbcTemplate);

    @Test
    void kpis_devuelveCincoKpisConTasaRedondeada() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM envios_tracking"), eq(Long.class), any()))
                .thenReturn(10L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM envios_tracking WHERE estado IN (?, ?, ?, ?)"), eq(Long.class), any()))
                .thenReturn(4L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM envios_tracking WHERE estado = 'ENTREGADO'"), eq(Long.class), any()))
                .thenReturn(3L);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM webhook_logs"), eq(Long.class), any()))
                .thenReturn(8L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM webhook_logs WHERE exitoso = TRUE"), eq(Long.class), any()))
                .thenReturn(6L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM reservas WHERE estado = 'pendiente'"), eq(Long.class), any()))
                .thenReturn(2L);

        List<KpiDto> kpis = service.kpis();

        assertThat(kpis).hasSize(5);
        assertThat(kpis.get(0).getLabel()).isEqualTo("Total envíos");
        assertThat(kpis.get(0).getValue()).isEqualTo(10.0);
        assertThat(kpis.get(1).getValue()).isEqualTo(4.0);
        assertThat(kpis.get(2).getValue()).isEqualTo(3.0);
        assertThat(kpis.get(3).getValue()).isEqualTo(75.0);
        assertThat(kpis.get(4).getValue()).isEqualTo(2.0);
    }

    @Test
    void calcularTasa_redondeaUnDecimalYSinLogsEsCien() {
        assertThat(AnalyticsQueryService.calcularTasa(6, 8)).isEqualTo(75.0);
        assertThat(AnalyticsQueryService.calcularTasa(5, 6)).isEqualTo(83.3);
        assertThat(AnalyticsQueryService.calcularTasa(0, 0)).isEqualTo(100.0);
    }

    @Test
    void mapEstadoCount_mapeaColumnas() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("estado")).thenReturn("ENTREGADO");
        when(rs.getLong("cantidad")).thenReturn(7L);

        EstadoCountDto dto = AnalyticsQueryService.mapEstadoCount(rs);

        assertThat(dto.getEstado()).isEqualTo("ENTREGADO");
        assertThat(dto.getCantidad()).isEqualTo(7L);
    }

    @Test
    void mapRuta_mapeaConFallbackN/D() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("origen")).thenReturn("N/D");
        when(rs.getString("destino")).thenReturn("Asunción");
        when(rs.getLong("cantidad")).thenReturn(3L);

        RutaDto dto = AnalyticsQueryService.mapRuta(rs);

        assertThat(dto.getOrigen()).isEqualTo("N/D");
        assertThat(dto.getDestino()).isEqualTo("Asunción");
        assertThat(dto.getCantidad()).isEqualTo(3L);
    }

    @Test
    void completarTendencia_rellenaCerosLosDiasSinDatos() {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        Map<LocalDate, Long> datos = new LinkedHashMap<>();
        datos.put(desde.plusDays(2), 5L);

        List<TendenciaDto> tendencia = AnalyticsQueryService.completarTendencia(datos, desde, 5);

        assertThat(tendencia).hasSize(5);
        assertThat(tendencia.get(0).getTotal()).isZero();
        assertThat(tendencia.get(2).getTotal()).isEqualTo(5L);
        assertThat(tendencia.get(4).getFecha()).isEqualTo(desde.plusDays(4));
    }

    @Test
    void completarWebhook_rellenaPuntosSinLogs() {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        Map<LocalDate, WebhookPuntoDto> datos = new LinkedHashMap<>();
        datos.put(desde, new WebhookPuntoDto(desde, 2, 3, 66.7));

        List<WebhookPuntoDto> puntos = AnalyticsQueryService.completarWebhook(datos, desde, 3);

        assertThat(puntos).hasSize(3);
        assertThat(puntos.get(0).getExitosos()).isEqualTo(2L);
        assertThat(puntos.get(1).getTotal()).isZero();
        assertThat(puntos.get(1).getTasaExito()).isZero();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AnalyticsQueryServiceTest
```
Expected: FAIL (compila el test: no existe la clase). En la primera ejecución el `mvn -q test -Dtest=...` solo mostrará "No tests were executed" si no compila main — para que "falle", debe fallar la compilación del test. OK para TDD.

- [ ] **Step 3: Write the service**

```java
package com.monteastur.envios.service.analytics;

import com.monteastur.envios.dto.analytics.EstadoCountDto;
import com.monteastur.envios.dto.analytics.KpiDto;
import com.monteastur.envios.dto.analytics.RutaDto;
import com.monteastur.envios.dto.analytics.TendenciaDto;
import com.monteastur.envios.dto.analytics.WebhookPuntoDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsQueryService {

    public static final int DIAS_TENDENCIA = 14;
    public static final int LIMITE_RUTAS = 5;

    private static final List<String> ESTADOS_EN_TRANSITO =
            List.of("EN_ADUANA_ORIGEN", "EN_TRANSITO", "EN_ADUANA_DESTINO", "EN_REPARTO");

    private static final String SQL_TOTAL = "SELECT COUNT(*) FROM envios_tracking";
    private static final String SQL_EN_TRANSITO =
            "SELECT COUNT(*) FROM envios_tracking WHERE estado IN (?, ?, ?, ?)";
    private static final String SQL_ENTREGADOS =
            "SELECT COUNT(*) FROM envios_tracking WHERE estado = 'ENTREGADO'";
    private static final String SQL_WEBHOOK_TOTAL = "SELECT COUNT(*) FROM webhook_logs";
    private static final String SQL_WEBHOOK_EXITOSOS =
            "SELECT COUNT(*) FROM webhook_logs WHERE exitoso = TRUE";
    private static final String SQL_RESERVAS_PENDIENTES =
            "SELECT COUNT(*) FROM reservas WHERE estado = 'pendiente'";
    private static final String SQL_POR_ESTADO =
            "SELECT estado, COUNT(*) AS cantidad FROM envios_tracking GROUP BY estado ORDER BY cantidad DESC, estado ASC";
    private static final String SQL_TENDENCIA =
            "SELECT DATE(fecha_creacion) AS d, COUNT(*) AS total FROM envios_tracking WHERE fecha_creacion >= ? GROUP BY DATE(fecha_creacion) ORDER BY d ASC";
    private static final String SQL_TOP_RUTAS =
            "SELECT COALESCE(origen, 'N/D') AS origen, COALESCE(destino, 'N/D') AS destino, COUNT(*) AS cantidad FROM envios_tracking GROUP BY origen, destino ORDER BY cantidad DESC, origen ASC LIMIT ?";
    private static final String SQL_WEBHOOK_POR_DIA =
            "SELECT DATE(fecha_creacion) AS d, SUM(exitoso) AS ok, COUNT(*) AS total FROM webhook_logs WHERE fecha_creacion >= ? GROUP BY DATE(fecha_creacion) ORDER BY d ASC";

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<KpiDto> kpis() {
        long total = count(SQL_TOTAL);
        long enTransito = count(SQL_EN_TRANSITO, ESTADOS_EN_TRANSITO.toArray());
        long entregados = count(SQL_ENTREGADOS);
        long totalWebhooks = count(SQL_WEBHOOK_TOTAL);
        long exitosos = count(SQL_WEBHOOK_EXITOSOS);
        long reservasPendientes = count(SQL_RESERVAS_PENDIENTES);
        double tasa = calcularTasa(exitosos, totalWebhooks);
        return List.of(
                new KpiDto("Total envíos", total, "#1B4D3B"),
                new KpiDto("En tránsito", enTransito, "#E67E22"),
                new KpiDto("Entregados", entregados, "#4ADE80"),
                new KpiDto("Éxito webhooks", tasa, "#153C2D"),
                new KpiDto("Reservas pendientes", reservasPendientes, "#E67E22")
        );
    }

    static double calcularTasa(long exitosos, long total) {
        if (total <= 0) {
            return 100.0;
        }
        return Math.round(exitosos * 1000.0 / total) / 10.0;
    }

    public List<EstadoCountDto> enviosPorEstado() {
        return jdbcTemplate.query(SQL_POR_ESTADO, (rs, rowNum) -> mapEstadoCount(rs));
    }

    static EstadoCountDto mapEstadoCount(ResultSet rs) throws SQLException {
        return new EstadoCountDto(rs.getString("estado"), rs.getLong("cantidad"));
    }

    public List<TendenciaDto> tendenciaUltimosDias(int dias) {
        LocalDate desde = LocalDate.now().minusDays(dias - 1L);
        Map<LocalDate, Long> porDia = new LinkedHashMap<>();
        jdbcTemplate.query(SQL_TENDENCIA,
                rs -> porDia.put(rs.getDate("d").toLocalDate(), rs.getLong("total")),
                Timestamp.valueOf(desde.atStartOfDay()));
        return completarTendencia(porDia, desde, dias);
    }

    static List<TendenciaDto> completarTendencia(Map<LocalDate, Long> porDia, LocalDate desde, int dias) {
        List<TendenciaDto> resultado = new ArrayList<>(dias);
        for (int i = 0; i < dias; i++) {
            LocalDate d = desde.plusDays(i);
            resultado.add(new TendenciaDto(d, porDia.getOrDefault(d, 0L)));
        }
        return resultado;
    }

    public List<RutaDto> topRutas(int limite) {
        return jdbcTemplate.query(SQL_TOP_RUTAS, (rs, rowNum) -> mapRuta(rs), limite);
    }

    static RutaDto mapRuta(ResultSet rs) throws SQLException {
        return new RutaDto(rs.getString("origen"), rs.getString("destino"), rs.getLong("cantidad"));
    }

    public List<WebhookPuntoDto> webhookPorDia(int dias) {
        LocalDate desde = LocalDate.now().minusDays(dias - 1L);
        Map<LocalDate, WebhookPuntoDto> porDia = new LinkedHashMap<>();
        jdbcTemplate.query(SQL_WEBHOOK_POR_DIA, rs -> {
            LocalDate d = rs.getDate("d").toLocalDate();
            long ok = rs.getLong("ok");
            long total = rs.getLong("total");
            porDia.put(d, new WebhookPuntoDto(d, ok, total, calcularTasa(ok, total)));
        }, Timestamp.valueOf(desde.atStartOfDay()));
        return completarWebhook(porDia, desde, dias);
    }

    static List<WebhookPuntoDto> completarWebhook(Map<LocalDate, WebhookPuntoDto> porDia, LocalDate desde, int dias) {
        List<WebhookPuntoDto> resultado = new ArrayList<>(dias);
        for (int i = 0; i < dias; i++) {
            LocalDate d = desde.plusDays(i);
            resultado.add(porDia.getOrDefault(d, new WebhookPuntoDto(d, 0, 0, 0.0)));
        }
        return resultado;
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AnalyticsQueryServiceTest
```
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/analytics/AnalyticsQueryService.java src/test/java/com/monteastur/envios/service/analytics/AnalyticsQueryServiceTest.java
git commit -m "feat(analytics): AnalyticsQueryService con SQL nativo (JdbcTemplate)"
```

---

### Task 4: AnalyticsDashboardService + registro de caché en RedisConfig

**Files:**
- Create: `src/main/java/com/monteastur/envios/service/analytics/AnalyticsDashboardService.java`
- Modify: `src/main/java/com/monteastur/envios/config/RedisConfig.java` (añadir `"envios.analytics"`)
- Test: `src/test/java/com/monteastur/envios/service/analytics/AnalyticsDashboardServiceTest.java`

**Interfaces:**
- Consumes: `AnalyticsQueryService` (Task 3) y sus constantes `DIAS_TENDENCIA`, `LIMITE_RUTAS`.
- Produces: `AnalyticsDashboardService.resumen()` (`AnalyticsSummaryDto` cacheado) y `refrescar()` (evict). Los consume `AnalyticsRestController` (Task 6).

- [ ] **Step 1: Write the failing test**

```java
package com.monteastur.envios.service.analytics;

import com.monteastur.envios.dto.analytics.AnalyticsSummaryDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AnalyticsDashboardServiceTest.TestConfig.class)
class AnalyticsDashboardServiceTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("envios.analytics");
            cacheManager.setAllowNullValues(false);
            return cacheManager;
        }

        @Bean
        AnalyticsQueryService queryService() {
            return Mockito.mock(AnalyticsQueryService.class);
        }

        @Bean
        AnalyticsDashboardService dashboardService(AnalyticsQueryService qs) {
            return new AnalyticsDashboardService(qs);
        }
    }

    @Autowired
    private AnalyticsDashboardService dashboardService;

    @Autowired
    private AnalyticsQueryService queryService;

    @Test
    void resumen_quedaCacheadoYNoRepiteLasConsultas() {
        AnalyticsSummaryDto primero = dashboardService.resumen();
        AnalyticsSummaryDto segundo = dashboardService.resumen();

        assertThat(segundo).isSameAs(primero);
        verify(queryService, times(1)).kpis();
    }

    @Test
    void refrescar_invalidaLaCache() {
        dashboardService.resumen();
        dashboardService.refrescar();
        dashboardService.resumen();

        verify(queryService, times(2)).kpis();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AnalyticsDashboardServiceTest
```
Expected: FAIL (no existe `AnalyticsDashboardService`).

- [ ] **Step 3: Write the service**

```java
package com.monteastur.envios.service.analytics;

import com.monteastur.envios.dto.analytics.AnalyticsSummaryDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AnalyticsDashboardService {

    private final AnalyticsQueryService queryService;

    public AnalyticsDashboardService(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @Cacheable(value = "envios.analytics", unless = "#result == null")
    public AnalyticsSummaryDto resumen() {
        AnalyticsSummaryDto dto = new AnalyticsSummaryDto();
        dto.setKpis(queryService.kpis());
        dto.setEnviosPorEstado(queryService.enviosPorEstado());
        dto.setTendencia(queryService.tendenciaUltimosDias(AnalyticsQueryService.DIAS_TENDENCIA));
        dto.setTopRutas(queryService.topRutas(AnalyticsQueryService.LIMITE_RUTAS));
        dto.setWebhookPorDia(queryService.webhookPorDia(AnalyticsQueryService.DIAS_TENDENCIA));
        dto.setGeneradoEn(LocalDateTime.now());
        return dto;
    }

    @CacheEvict(value = "envios.analytics", allEntries = true)
    public void refrescar() {
        // Solo invalida la caché; la siguiente llamada a resumen() recalcula.
    }
}
```

- [ ] **Step 4: Register the cache in RedisConfig**

En `src/main/java/com/monteastur/envios/config/RedisConfig.java`, dentro de `var configs = Map.of(...)`, añadir la entrada:

```java
            "envios.analytics", defaultConfig.entryTtl(Duration.ofMinutes(2)),
```

(El bloque `configs` pasa a tener 8 entradas; mantener el resto igual. `Duration` ya está importado.)

- [ ] **Step 5: Run test to verify it passes**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AnalyticsDashboardServiceTest
```
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/analytics/AnalyticsDashboardService.java src/main/java/com/monteastur/envios/config/RedisConfig.java src/test/java/com/monteastur/envios/service/analytics/AnalyticsDashboardServiceTest.java
git commit -m "feat(analytics): AnalyticsDashboardService con caché envios.analytics (TTL 2 min)"
```

---

### Task 5: CacheErrorHandler resiliente + invalidación en escrituras

**Files:**
- Create: `src/main/java/com/monteastur/envios/config/CacheAuditErrorHandler.java`
- Modify: `src/main/java/com/monteastur/envios/config/RedisConfig.java` (implementar `CachingConfigurer`)
- Modify: `src/main/java/com/monteastur/envios/service/EnvioTrackingService.java` (3 anotaciones)
- Modify: `src/main/java/com/monteastur/envios/service/batch/BatchImportPersistenceService.java` (2 anotaciones)
- Modify: `src/main/java/com/monteastur/envios/service/WebhookDispatchService.java` (1 anotación)
- Test: `src/test/java/com/monteastur/envios/config/CacheAuditErrorHandlerTest.java`

**Interfaces:**
- Produces: bean `errorHandler()` de `CachingConfigurer` (fallback a BD si Redis cae) + invalidación automática de `envios.analytics` en toda escritura.

- [ ] **Step 1: Write the failing test**

```java
package com.monteastur.envios.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import static org.assertj.core.api.Assertions.assertThatCode;

class CacheAuditErrorHandlerTest {

    private final CacheAuditErrorHandler handler = new CacheAuditErrorHandler();
    private final Cache cache = new ConcurrentMapCache("envios.analytics");

    @Test
    void getError_noPropagaYDelegaAlMetodo() {
        assertThatCode(() -> handler.handleCacheGetError(new RuntimeException("redis caído"), cache, "k"))
                .doesNotThrowAnyException();
    }

    @Test
    void putEvictClear_noPropaganExcepciones() {
        assertThatCode(() -> handler.handleCachePutError(new RuntimeException("x"), cache, "k", new Object()))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheEvictError(new RuntimeException("x"), cache, "k"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheClearError(new RuntimeException("x"), cache))
                .doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=CacheAuditErrorHandlerTest
```
Expected: FAIL (no existe la clase).

- [ ] **Step 3: Write the error handler**

```java
package com.monteastur.envios.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

public class CacheAuditErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(CacheAuditErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache GET fallido (cache={}, key={}): se degrada a base de datos", cache.getName(), key, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache PUT fallido (cache={}, key={}): se omite el almacenamiento", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache EVICT fallido (cache={}, key={}): se continúa", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache CLEAR fallido (cache={}): se continúa", cache.getName(), exception);
    }
}
```

- [ ] **Step 4: Wire the error handler in RedisConfig**

En `src/main/java/com/monteastur/envios/config/RedisConfig.java`:

1. Cambiar la declaración de clase:
```java
public class RedisConfig implements CachingConfigurer {
```
2. Añadir import `org.springframework.cache.interceptor.CacheErrorHandler;` y `org.springframework.cache.interceptor.CachingConfigurer;`.
3. Añadir método:
```java
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheAuditErrorHandler();
    }
```

- [ ] **Step 5: Add `@CacheEvict` to the write paths**

`EnvioTrackingService.java` — en las 3 anotaciones existentes (guardar, actualizarEstado, eliminar) añadir `"envios.analytics"`:

```java
    @CacheEvict(value = {"envios.tracking", "envios.tracking.pagina", "envios.cliente.dashboard", "envios.analytics"}, allEntries = true)
```

`BatchImportPersistenceService.java` — `procesarChunk`:
```java
    @CacheEvict(value = {"envios.dashboard", "envios.tracking.pagina", "envios.cliente.dashboard", "envios.analytics"}, allEntries = true)
```
`finalizar`:
```java
    @CacheEvict(value = {"envios.dashboard", "envios.analytics"}, allEntries = true)
```

`WebhookDispatchService.java` — añadir anotación a `despachar` (import `org.springframework.cache.annotation.CacheEvict;`):
```java
    @CacheEvict(value = "envios.analytics", allEntries = true)
    public void despachar(EstadoEnvioActualizadoEvent event) {
```

- [ ] **Step 6: Run tests to verify they pass**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=CacheAuditErrorHandlerTest,AnalyticsDashboardServiceTest
```
Expected: PASS (ambos).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/monteastur/envios/config/CacheAuditErrorHandler.java src/main/java/com/monteastur/envios/config/RedisConfig.java src/main/java/com/monteastur/envios/service/EnvioTrackingService.java src/main/java/com/monteastur/envios/service/batch/BatchImportPersistenceService.java src/main/java/com/monteastur/envios/service/WebhookDispatchService.java src/test/java/com/monteastur/envios/config/CacheAuditErrorHandlerTest.java
git commit -m "feat(analytics): CacheErrorHandler resiliente e invalidación de envios.analytics en escrituras"
```

---

### Task 6: AnalyticsRestController

**Files:**
- Create: `src/main/java/com/monteastur/envios/controller/api/AnalyticsRestController.java`
- Test: `src/test/java/com/monteastur/envios/controller/api/AnalyticsRestControllerTest.java`

**Interfaces:**
- Consumes: `AnalyticsDashboardService` (Task 4).
- Produces: `GET /api/v1/admin/analytics/resumen` y `POST /api/v1/admin/analytics/refresh` (JSON de `AnalyticsSummaryDto`), protegidos por `ROLE_ADMIN`. Los consume `analytics.js` (Task 7).

- [ ] **Step 1: Write the failing test**

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.dto.analytics.AnalyticsSummaryDto;
import com.monteastur.envios.dto.analytics.KpiDto;
import com.monteastur.envios.dto.analytics.TendenciaDto;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.analytics.AnalyticsDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsRestController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test",
    "app.upload.dir=./uploads"
})
class AnalyticsRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsDashboardService dashboardService;

    @MockBean private RBACAccessLogger rbacAccessLogger;
    @MockBean private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockBean private DataSource dataSource;

    private AnalyticsSummaryDto resumenEjemplo() {
        AnalyticsSummaryDto dto = new AnalyticsSummaryDto();
        dto.setKpis(List.of(new KpiDto("Total envíos", 42.0, "#1B4D3B")));
        dto.setTendencia(List.of(new TendenciaDto(LocalDate.of(2026, 8, 1), 2L)));
        dto.setGeneradoEn(LocalDateTime.of(2026, 8, 5, 10, 15));
        return dto;
    }

    @Test
    void resumen_sinAutenticar_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/resumen"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resumen_conAdmin_devuelve200YJson() throws Exception {
        when(dashboardService.resumen()).thenReturn(resumenEjemplo());

        mockMvc.perform(get("/api/v1/admin/analytics/resumen").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis[0].label").value("Total envíos"))
                .andExpect(jsonPath("$.kpis[0].value").value(42.0))
                .andExpect(jsonPath("$.tendencia[0].fecha").value("2026-08-01"))
                .andExpect(jsonPath("$.generadoEn").exists());
    }

    @Test
    void refresh_conAdmin_devuelve200YReevalua() throws Exception {
        when(dashboardService.resumen()).thenReturn(resumenEjemplo());

        mockMvc.perform(post("/api/v1/admin/analytics/refresh").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis").isArray());

        verify(dashboardService).refrescar();
        verify(dashboardService).resumen();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AnalyticsRestControllerTest
```
Expected: FAIL (no existe el controlador).

- [ ] **Step 3: Write the controller**

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.analytics.AnalyticsSummaryDto;
import com.monteastur.envios.service.analytics.AnalyticsDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics BI", description = "Panel de Business Intelligence del CMS (requiere sesión admin o Basic Auth)")
@RestController
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AnalyticsRestController {

    private final AnalyticsDashboardService dashboardService;

    public AnalyticsRestController(AnalyticsDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Resumen de analítica",
            description = "KPIs y agregaciones del dashboard, cacheados en Redis (envios.analytics, TTL 2 min)")
    @GetMapping("/resumen")
    public ResponseEntity<AnalyticsSummaryDto> resumen() {
        return ResponseEntity.ok(dashboardService.resumen());
    }

    @Operation(summary = "Refrescar analítica",
            description = "Invalida la caché envios.analytics y devuelve datos recién calculados")
    @PostMapping("/refresh")
    public ResponseEntity<AnalyticsSummaryDto> refrescar() {
        dashboardService.refrescar();
        return ResponseEntity.ok(dashboardService.resumen());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AnalyticsRestControllerTest
```
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/controller/api/AnalyticsRestController.java src/test/java/com/monteastur/envios/controller/api/AnalyticsRestControllerTest.java
git commit -m "feat(analytics): AnalyticsRestController con resumen cacheado y refresh"
```

---

### Task 7: Frontend — Chart.js vendored, sección BI y analytics.js

**Files:**
- Create: `src/main/resources/static/js/vendor/chart.umd.min.js` (descarga puntual, se commitea)
- Create: `src/main/resources/static/js/analytics.js`
- Modify: `src/main/resources/templates/cms/dashboard.html` (sección BI + scripts al final del body)
- Modify: `src/main/resources/static/css/design-system.css` (clases BI vía ensamblado)
- Modify: `src/test/java/com/monteastur/envios/controller/AdminThemeAssetsTest.java` (nuevo método)

**Interfaces:**
- Consumes: API de Task 6 (`/api/v1/admin/analytics/resumen` y `/refresh`).

- [ ] **Step 1: Vendor Chart.js y escribe el test de assets**

Descarga única (implementación): 
```powershell
Invoke-WebRequest -Uri "https://cdn.jsdelivr.net/npm/chart.js@4.5.1/dist/chart.umd.min.js" -OutFile "src\main\resources\static\js\vendor\chart.umd.min.js"
```
Verificación de la descarga (debe salir `OK`):
```powershell
$f = Get-Item "src\main\resources\static\js\vendor\chart.umd.min.js"
if ($f.Length -gt 150000 -and (Select-String -LiteralPath $f.FullName -Pattern 'version:"4.5.1"' -Quiet)) { "OK" } else { "FALLO: revisa la descarga" }
```

Añadir a `src/test/java/com/monteastur/envios/controller/AdminThemeAssetsTest.java` el método:

```java
    @Test
    void dashboard_incluyeSeccionBiYAssetsChartjs() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bi-analytics-section")))
                .andExpect(content().string(containsString("/js/vendor/chart.umd.min.js")))
                .andExpect(content().string(containsString("/js/analytics.js")))
                .andExpect(content().string(containsString("bi-chart-estado")))
                .andExpect(content().string(containsString("Refrescar")));
    }
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AdminThemeAssetsTest#dashboard_incluyeSeccionBiYAssetsChartjs
```
Expected: FAIL (el HTML no contiene la sección BI).

- [ ] **Step 3: Add the BI CSS and rebuild design-system.css**

Crear en `C:\Users\astur\AppData\Local\Temp\opencode\css_bi_section.css`:

```css
/* ===== Sección BI Dashboard ===== */
.bi-analytics { margin-top: 24px; display: flex; flex-direction: column; gap: 20px; }
.bi-analytics-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.bi-refresh-btn { padding: 10px 18px; border: none; border-radius: 12px; background: linear-gradient(135deg, var(--accent-color), #F09A54); color: #fff; font-weight: 700; cursor: pointer; transition: transform .2s ease, box-shadow .2s ease; box-shadow: 0 6px 16px rgba(230,126,34,.3); }
.bi-refresh-btn:hover { transform: translateY(-2px); }
.bi-refresh-btn:disabled { opacity: .6; cursor: progress; }
.bi-error { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 16px; border-radius: 12px; background: rgba(200,16,46,.12); border: 1px solid rgba(200,16,46,.35); color: var(--en-rojo); font-weight: 600; }
.bi-error button { padding: 6px 14px; border: 1px solid var(--en-rojo); background: transparent; color: var(--en-rojo); border-radius: 8px; cursor: pointer; }
.bi-carga { color: var(--text-secondary, #9aa3ad); font-style: italic; }
.bi-kpi-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; background: var(--accent-color); }
.bi-charts-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.bi-chart-card { min-width: 0; }
.bi-chart-wrap { position: relative; height: 280px; }
.bi-vacio { color: var(--text-secondary, #9aa3ad); text-align: center; padding: 24px 8px; font-style: italic; }
@media (max-width: 900px) { .bi-charts-grid { grid-template-columns: 1fr; } }
```

Reconstruir `src/main/resources/static/css/design-system.css` concatenando las 7 fuentes (la última gana la cascada). Orden: `css_ds_base.css`, `css_corelegacy_section.css`, `css_premium_section.css`, `css_tracking_section.css`, `css_admin_section.css`, `css_base_core.css`, `css_bi_section.css`. NO ejecutar el ensamblado dos veces sobre el mismo `design-system.css`.

- [ ] **Step 4: Add the BI section to the dashboard template**

Insertar entre el bloque `card glass-card` de "Resumen General" y `<div class="dashboard-bottom">`:

```html
    <!-- Sección BI Dashboard -->
    <div id="bi-analytics-section" class="bi-analytics">
        <div class="bi-analytics-header">
            <h3 class="luxury-heading">Anal&iacute;tica &mdash; Centro de Control BI</h3>
            <button type="button" id="bi-refresh" class="bi-refresh-btn">Refrescar</button>
        </div>
        <div id="bi-error" class="bi-error" style="display:none;">
            <span id="bi-error-text"></span>
            <button type="button" id="bi-retry">Reintentar</button>
        </div>
        <div id="bi-carga" class="bi-carga">Cargando m&eacute;tricas&hellip;</div>

        <div id="bi-kpis" class="stats-grid">
            <div class="stat-item">
                <span class="bi-kpi-dot" id="bi-kpi-dot-0"></span>
                <span id="bi-kpi-label-0">Total env&iacute;os</span>
                <strong id="bi-kpi-value-0">--</strong>
            </div>
            <div class="stat-item">
                <span class="bi-kpi-dot" id="bi-kpi-dot-1"></span>
                <span id="bi-kpi-label-1">En tr&aacute;nsito</span>
                <strong id="bi-kpi-value-1">--</strong>
            </div>
            <div class="stat-item">
                <span class="bi-kpi-dot" id="bi-kpi-dot-2"></span>
                <span id="bi-kpi-label-2">Entregados</span>
                <strong id="bi-kpi-value-2">--</strong>
            </div>
            <div class="stat-item">
                <span class="bi-kpi-dot" id="bi-kpi-dot-3"></span>
                <span id="bi-kpi-label-3">&Eacute;xito webhooks</span>
                <strong id="bi-kpi-value-3">--</strong>
            </div>
            <div class="stat-item">
                <span class="bi-kpi-dot" id="bi-kpi-dot-4"></span>
                <span id="bi-kpi-label-4">Reservas pendientes</span>
                <strong id="bi-kpi-value-4">--</strong>
            </div>
        </div>

        <div class="bi-charts-grid">
            <div class="card glass-card bi-chart-card">
                <h3 class="luxury-heading">Env&iacute;os por estado</h3>
                <div class="bi-chart-wrap"><canvas id="bi-chart-estado"></canvas></div>
            </div>
            <div class="card glass-card bi-chart-card">
                <h3 class="luxury-heading">Evoluci&oacute;n &uacute;ltimos 14 d&iacute;as</h3>
                <div class="bi-chart-wrap"><canvas id="bi-chart-tendencia"></canvas></div>
            </div>
            <div class="card glass-card bi-chart-card">
                <h3 class="luxury-heading">Top rutas</h3>
                <div class="bi-chart-wrap"><canvas id="bi-chart-rutas"></canvas></div>
            </div>
            <div class="card glass-card bi-chart-card">
                <h3 class="luxury-heading">&Eacute;xito de webhooks</h3>
                <div class="bi-chart-wrap"><canvas id="bi-chart-webhooks"></canvas></div>
            </div>
        </div>
    </div>
```

Antes de `</body>`:
```html
<script src="/js/vendor/chart.umd.min.js"></script>
<script src="/js/analytics.js"></script>
```

- [ ] **Step 5: Write analytics.js**

`src/main/resources/static/js/analytics.js`:

```js
(function () {
    'use strict';

    var URL_RESUMEN = '/api/v1/admin/analytics/resumen';
    var URL_REFRESH = '/api/v1/admin/analytics/refresh';

    var PALETA = {
        ENTREGADO: '#4ADE80',
        EN_TRANSITO: '#E67E22',
        RECIBIDO: '#1B4D3B',
        EN_ADUANA_ORIGEN: '#2D6A4F',
        EN_ADUANA_DESTINO: '#153C2D',
        EN_REPARTO: '#F09A54'
    };
    var FALLBACK_COLOR = '#5B8C7A';

    var graficos = [];
    var cargando = false;

    function $(id) { return document.getElementById(id); }

    function colorDeEstado(estado) { return PALETA[estado] || FALLBACK_COLOR; }

    function coloresTema() {
        var dark = document.documentElement.getAttribute('data-theme') === 'dark';
        return {
            grid: dark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.08)',
            ticks: dark ? 'rgba(255,255,255,0.72)' : 'rgba(0,0,0,0.72)',
            label: dark ? 'rgba(255,255,255,0.9)' : 'rgba(0,0,0,0.9)'
        };
    }

    function formatoNumero(v) {
        var n = Math.round(v * 100) / 100;
        return n.toLocaleString('es');
    }

    function destruirGraficos() {
        graficos.forEach(function (g) { try { g.destroy(); } catch (e) {} });
        graficos = [];
    }

    function limpiarVacios() {
        var nodos = document.querySelectorAll('.bi-vacio');
        for (var i = 0; i < nodos.length; i++) {
            nodos[i].parentNode.removeChild(nodos[i]);
        }
    }

    function renderKpis(kpis) {
        kpis.forEach(function (kpi, i) {
            var v = $('bi-kpi-value-' + i);
            var l = $('bi-kpi-label-' + i);
            var d = $('bi-kpi-dot-' + i);
            if (v) v.textContent = formatoNumero(kpi.value);
            if (l) l.textContent = kpi.label;
            if (d) d.style.background = kpi.color;
        });
    }

    function estadoVacio(id, mensaje) {
        var c = $(id);
        if (!c) return;
        var div = document.createElement('div');
        div.className = 'bi-vacio';
        div.textContent = mensaje;
        c.parentNode.appendChild(div);
    }

    function graficoDona(id, datos) {
        var canvas = $(id);
        if (!canvas) return;
        if (!datos.length) { estadoVacio(id, 'Sin envíos registrados'); return; }
        var tc = coloresTema();
        graficos.push(new Chart(canvas.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: datos.map(function (d) { return d.estado; }),
                datasets: [{
                    data: datos.map(function (d) { return d.cantidad; }),
                    backgroundColor: datos.map(function (d) { return colorDeEstado(d.estado); }),
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom', labels: { color: tc.label } } }
            }
        }));
    }

    function graficoLinea(id, etiquetas, valores, color) {
        var canvas = $(id);
        if (!canvas) return;
        if (!valores.length) { estadoVacio(id, 'Sin datos en el periodo'); return; }
        var tc = coloresTema();
        graficos.push(new Chart(canvas.getContext('2d'), {
            type: 'line',
            data: {
                labels: etiquetas,
                datasets: [{
                    label: 'Envíos',
                    data: valores,
                    borderColor: color,
                    backgroundColor: color + '33',
                    fill: true,
                    tension: 0.35,
                    pointBackgroundColor: color,
                    pointRadius: 3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { ticks: { color: tc.ticks }, grid: { color: tc.grid } },
                    y: { beginAtZero: true, ticks: { color: tc.ticks, precision: 0 }, grid: { color: tc.grid } }
                },
                plugins: { legend: { display: false } }
            }
        }));
    }

    function graficoBarras(id, etiquetas, valores) {
        var canvas = $(id);
        if (!canvas) return;
        if (!valores.length) { estadoVacio(id, 'Sin rutas registradas'); return; }
        var tc = coloresTema();
        graficos.push(new Chart(canvas.getContext('2d'), {
            type: 'bar',
            data: {
                labels: etiquetas,
                datasets: [{
                    label: 'Envíos',
                    data: valores,
                    backgroundColor: ['#1B4D3B', '#2D6A4F', '#153C2D', '#E67E22', '#4ADE80']
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { beginAtZero: true, ticks: { color: tc.ticks, precision: 0 }, grid: { color: tc.grid } },
                    y: { ticks: { color: tc.ticks }, grid: { display: false } }
                },
                plugins: { legend: { display: false } }
            }
        }));
    }

    function renderCharts(data) {
        limpiarVacios();
        destruirGraficos();

        renderKpis(data.kpis || []);

        graficoDona('bi-chart-estado', data.enviosPorEstado || []);

        var tend = data.tendencia || [];
        graficoLinea('bi-chart-tendencia',
            tend.map(function (p) { return p.fecha.slice(5); }),
            tend.map(function (p) { return p.total; }),
            '#E67E22');

        var rutas = data.topRutas || [];
        graficoBarras('bi-chart-rutas',
            rutas.map(function (r) { return (r.origen || 'N/D') + ' → ' + (r.destino || 'N/D'); }),
            rutas.map(function (r) { return r.cantidad; }));

        var wh = data.webhookPorDia || [];
        graficoLinea('bi-chart-webhooks',
            wh.map(function (p) { return p.fecha.slice(5); }),
            wh.map(function (p) { return p.tasaExito; }),
            '#4ADE80');
    }

    function mostrarError(msg) {
        var b = $('bi-error');
        if (!b) return;
        b.style.display = 'flex';
        var t = $('bi-error-text');
        if (t) t.textContent = msg;
        var c = $('bi-carga');
        if (c) c.style.display = 'none';
    }

    function ocultarError() {
        var b = $('bi-error');
        if (b) b.style.display = 'none';
    }

    function setCargando(activo) {
        cargando = activo;
        var btn = $('bi-refresh');
        if (btn) { btn.disabled = activo; btn.textContent = activo ? 'Refrescando…' : 'Refrescar'; }
        var c = $('bi-carga');
        if (c) c.style.display = activo ? 'block' : 'none';
    }

    function cargar() {
        if (cargando) return;
        setCargando(true);
        ocultarError();
        fetch(URL_RESUMEN, { headers: { 'Accept': 'application/json' } })
            .then(function (r) {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(function (data) {
                renderCharts(data);
                setCargando(false);
            })
            .catch(function (err) {
                setCargando(false);
                mostrarError('No se pudieron cargar las métricas (' + err.message + ').');
            });
    }

    function refrescar() {
        if (cargando) return;
        setCargando(true);
        ocultarError();
        fetch(URL_REFRESH, { method: 'POST', headers: { 'Accept': 'application/json' } })
            .then(function (r) {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(function (data) {
                renderCharts(data);
                setCargando(false);
            })
            .catch(function (err) {
                setCargando(false);
                mostrarError('No se pudo refrescar (' + err.message + ').');
            });
    }

    function init() {
        var btn = $('bi-refresh');
        var retry = $('bi-retry');
        if (btn) btn.addEventListener('click', refrescar);
        if (retry) retry.addEventListener('click', cargar);
        cargar();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
```

- [ ] **Step 6: Run test to verify it passes**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AdminThemeAssetsTest
```
Expected: PASS (4 tests: los 3 existentes + el nuevo BI).

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/js/vendor/chart.umd.min.js src/main/resources/static/js/analytics.js src/main/resources/templates/cms/dashboard.html src/main/resources/static/css/design-system.css src/test/java/com/monteastur/envios/controller/AdminThemeAssetsTest.java
git commit -m "feat(analytics): Chart.js vendored, sección BI en dashboard y analytics.js"
```

---

### Task 8: Test de integración del BI Dashboard

**Files:**
- Create: `src/test/java/com/monteastur/envios/integration/AnalyticsDashboardIntegrationTest.java`

**Interfaces:**
- Consumes: `AnalyticsDashboardService` (Task 4) y la API de Task 6. Verifica la migración V10 (se aplica vía Flyway), las agregaciones reales y el comportamiento de caché.
- Requiere MySQL + Redis locales (contenedores `monteastur-mysql`/`monteastur-redis` con puertos expuestos) o los del entorno Docker.

- [ ] **Step 1: Write the failing test**

```java
package com.monteastur.envios.integration;

import com.monteastur.envios.dto.analytics.AnalyticsSummaryDto;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.model.WebhookConfig;
import com.monteastur.envios.model.WebhookLog;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.WebhookConfigRepository;
import com.monteastur.envios.repository.WebhookLogRepository;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.analytics.AnalyticsDashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsDashboardIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private WebhookConfigRepository webhookConfigRepository;
    @Autowired private WebhookLogRepository webhookLogRepository;
    @Autowired private EnvioTrackingRepository envioTrackingRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private AnalyticsDashboardService analyticsService;
    @Autowired private CacheManager cacheManager;

    @MockBean private EmailService emailService;

    private final List<Long> enviosIds = new ArrayList<>();
    private final List<Long> clientesIds = new ArrayList<>();
    private final List<Long> webhookIds = new ArrayList<>();

    @AfterEach
    void limpiar() {
        webhookLogRepository.deleteAll();
        for (Long id : webhookIds) {
            webhookConfigRepository.deleteById(id);
        }
        webhookIds.clear();
        for (Long id : enviosIds) {
            envioTrackingRepository.deleteById(id);
        }
        enviosIds.clear();
        for (Long id : clientesIds) {
            clienteRepository.deleteById(id);
        }
        clientesIds.clear();
        reservaRepository.deleteAll();
        Cache c = cacheManager.getCache("envios.analytics");
        if (c != null) {
            c.clear();
        }
    }

    private Cliente crearCliente(String email) {
        Cliente c = clienteRepository.save(
                new Cliente(email, "hash", "Cliente " + email, "000000000"));
        clientesIds.add(c.getId());
        return c;
    }

    private EnvioTracking guardarEnvio(String codigo, String estado, String origen, String destino) {
        EnvioTracking e = envioTrackingRepository.save(
                new EnvioTracking(codigo, estado, "Destinatario " + codigo, origen, destino, "1 kg", "Docs"));
        enviosIds.add(e.getId());
        return e;
    }

    private void crearWebhookLogs(Cliente cliente, EnvioTracking envio, int exitosos, int fallidos) {
        WebhookConfig config = webhookConfigRepository.save(
                new WebhookConfig(cliente.getId(), "https://hook.test/evt", "secret"));
        webhookIds.add(config.getId());
        for (int i = 0; i < exitosos; i++) {
            webhookLogRepository.save(new WebhookLog(config.getId(), envio.getId(), "{}", 200, true, null));
        }
        for (int i = 0; i < fallidos; i++) {
            webhookLogRepository.save(new WebhookLog(config.getId(), envio.getId(), "{}", 500, false, "HTTP 500"));
        }
    }

    private Reserva crearReserva(String estado) {
        Reserva r = new Reserva("Cliente R", "r@test.local", "123",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 2, "comentario");
        r.setEstado(estado);
        r.setCreatedAt(LocalDateTime.now());
        return reservaRepository.save(r);
    }

    @Test
    void resumen_agregaDatosCorrectos() {
        guardarEnvio("MT-BI-01", "ENTREGADO", "Asturias", "Asunción");
        guardarEnvio("MT-BI-02", "ENTREGADO", "Asturias", "Asunción");
        guardarEnvio("MT-BI-03", "EN_TRANSITO", "Asturias", "Asunción");
        Cliente cliente = crearCliente("bi@test.local");
        guardarEnvio("MT-BI-04", "ENTREGADO", "Oviedo", "Ciudad del Este");
        crearWebhookLogs(cliente, envioTrackingRepository.findByCodigoUnico("MT-BI-04").orElseThrow(), 6, 2);
        crearReserva("pendiente");

        AnalyticsSummaryDto resumen = analyticsService.resumen();

        assertThat(resumen.getKpis()).hasSize(5);
        assertThat(resumen.getKpis().get(0).getValue()).isEqualTo(4.0);
        assertThat(resumen.getKpis().get(1).getValue()).isEqualTo(1.0);
        assertThat(resumen.getKpis().get(2).getValue()).isEqualTo(3.0);
        assertThat(resumen.getKpis().get(3).getValue()).isEqualTo(75.0);
        assertThat(resumen.getKpis().get(4).getValue()).isEqualTo(1.0);

        assertThat(resumen.getEnviosPorEstado())
                .filteredOn(d -> "ENTREGADO".equals(d.getEstado()))
                .singleElement()
                .extracting("cantidad")
                .isEqualTo(3L);

        assertThat(resumen.getTopRutas()).isNotEmpty();
        assertThat(resumen.getTopRutas().get(0).getOrigen()).isEqualTo("Asturias");
        assertThat(resumen.getTopRutas().get(0).getDestino()).isEqualTo("Asunción");
        assertThat(resumen.getTopRutas().get(0).getCantidad()).isEqualTo(3L);

        assertThat(resumen.getTendencia()).hasSize(14);

        assertThat(resumen.getWebhookPorDia()).hasSize(14);
        assertThat(resumen.getWebhookPorDia().get(13).getExitosos()).isEqualTo(6L);
        assertThat(resumen.getWebhookPorDia().get(13).getTotal()).isEqualTo(8L);
        assertThat(resumen.getWebhookPorDia().get(13).getTasaExito()).isEqualTo(75.0);
    }

    @Test
    void resumen_quedaCacheadoYRefreshLoInvalida() {
        AnalyticsSummaryDto primera = analyticsService.resumen();
        long totalInicial = (long) primera.getKpis().get(0).getValue();

        guardarEnvio("MT-CACHE-1", "RECIBIDO", "Asturias", "Asunción");

        AnalyticsSummaryDto segunda = analyticsService.resumen();
        assertThat((long) segunda.getKpis().get(0).getValue()).isEqualTo(totalInicial);

        analyticsService.refrescar();
        AnalyticsSummaryDto tercera = analyticsService.resumen();
        assertThat((long) tercera.getKpis().get(0).getValue()).isEqualTo(totalInicial + 1);
    }

    @Test
    void resumenApi_devuelveJsonConCampos() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.length()").value(5))
                .andExpect(jsonPath("$.enviosPorEstado").isArray())
                .andExpect(jsonPath("$.tendencia.length()").value(14))
                .andExpect(jsonPath("$.generadoEn").exists());
    }

    @Test
    void refreshApi_devuelveDatosFrescos() throws Exception {
        mockMvc.perform(post("/api/v1/admin/analytics/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.length()").value(5));
    }
}
```

Notas del seed:
- `EnvioTracking` constructor: `(codigoUnico, estado, destinatario, origen, destino, peso, contenido)`.
- `WebhookConfig` constructor: `(clienteId, url, secretToken)`.
- `WebhookLog` constructor: `(webhookId, envioId, payload, responseStatus, exitoso, errorMensaje)`.
- `Reserva` constructor: `(nombreCliente, email, telefono, fechaEntrada, fechaSalida, numeroHuespedes, comentarios)` + setter `setEstado`.
- El total del KPI es relativo (`totalInicial`) para ser robusto ante datos preexistentes de la BD test.

- [ ] **Step 2: Run test to verify it fails (o directamente se valida el comportamiento)**

Primero ejecutar el test de caché para ver el ciclo rojo/verde:
```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AnalyticsDashboardIntegrationTest#resumen_quedaCacheadoYRefreshLoInvalida
```
Los 4 tests deben acabar en PASS cuando la implementación de Tasks 1–7 está completa.

- [ ] **Step 3: Run the full integration class**

```powershell
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q test -Dtest=AnalyticsDashboardIntegrationTest
```
Expected: PASS (4 tests).

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/monteastur/envios/integration/AnalyticsDashboardIntegrationTest.java
git commit -m "feat(analytics): test de integración del BI Dashboard (agregaciones y caché)"
```

---

### Task 9: Suite completa, Docker, commit final y handoff

**Files:**
- Modify: `docs/handoff.md`

- [ ] **Step 1: Run the full suite locally**

```powershell
$env:JAVA_HOME="C:\Users\astur\.jdks\openjdk-25.0.2"
C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd clean test
```
Expected: BUILD SUCCESS, 255+ tests, 0 failures.

- [ ] **Step 2: Run the full suite in Docker**

```powershell
docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://monteastur-mysql:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
  -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=monteastur-redis `
  -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-25 mvn clean test
```
Expected: BUILD SUCCESS, 255+ tests, 0 failures.

- [ ] **Step 3: Update `docs/handoff.md`**

Añadir entrada del Bloque 14 con el total de tests (255+) y el commit final.

- [ ] **Step 4: Final commit with the user-specified message**

```bash
git add docs/handoff.md
git commit -m "feat(analytics): add real-time BI dashboard with native SQL aggregations, Redis cache and Chart.js"
git push origin main
```

- [ ] **Step 5: Report**

Reportar: total de tests en verde (local y Docker), commits del Bloque 14, estado de `origin/main`.

---

## Self-Review

1. **Cobertura de spec:** Todos los puntos de la spec tienen tarea: V10 (T1), DTOs (T2), QueryService (T3), DashboardService+RedisConfig (T4), CacheErrorHandler+evictions (T5), REST (T6), frontend completo (T7), integración (T8), suite/verificación/handoff (T9). 
2. **Placeholders:** Ningún TBD/TODO; todo el código está presente.
3. **Consistencia de tipos:** `resumen()` devuelve `AnalyticsSummaryDto` en T4/T6/T8; `AnalyticsQueryService.calcularTasa(long,long)` y los mappers estáticos se usan igual en T3 y sus tests; `refrescar()` void en T4/T6. Nombres de caches (`envios.analytics`) y URLs (`/api/v1/admin/analytics/resumen|refresh`) consistentes entre T4–T8.
