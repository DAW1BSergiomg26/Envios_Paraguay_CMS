# Sprint de Optimización y Resiliencia — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aplicar el Sprint de Optimización y Resiliencia (Enfoque A): pool de conexiones Lettuce real (`commons-pool2`), tuning de HikariCP y Spring Session en los perfiles base y prod, blindaje del ciclo de vida de la caché `envios.tracking` con un test de integración real, y corrección documental del `REPORT.md` de k6.

**Architecture:** Cambios de configuración y dependencias (sin tocar código de producción del dominio). El tracking público ya consume `EnvioTrackingService.buscarPorCodigo` con `@Cacheable("envios.tracking")` (commit `4407c07`); el sprint verifica y blinda ese comportamiento con un test `@SpringBootTest` contra MySQL+Redis reales, y corrige el reporte que afirmaba lo contrario.

**Tech Stack:** Spring Boot 3.3.5, Java 17, Maven 3.9.9, MySQL 8, Redis 7, `org.apache.commons:commons-pool2` (versionado por el parent), JUnit 5 + AssertJ.

## Global Constraints

- Perfil prod es el que validan las pruebas k6 (docker-compose activa `SPRING_PROFILES_ACTIVE: prod`). Todo tuning que afecte al rendimiento bajo carga debe aplicar en `application.properties` (base) **y** en `application-prod.properties` (reconciliado).
- Sin `commons-pool2` en el classpath, las propiedades `spring.data.redis.lettuce.pool.*` se ignoran silenciosamente. La adición al pom es obligatoria y el test debe verificar el bean `GenericObjectPoolConfig`.
- `spring.session.redis.save-mode=on-set-attribute` y `spring.session.redis.flush-mode=on-save` son los defaults de Spring Session (no-op funcional; explícitos por autodocumentación). El namespace `monteastur:session` **NO cambia** (cambiarlo invalidaría sesiones existentes).
- Valores exactos: Hikari `maximum-pool-size=25`, `minimum-idle=5`, `connection-timeout=20000`, `idle-timeout=300000`, `max-lifetime=1200000`. Lettuce `max-active=30`, `max-idle=15`, `min-idle=5`, `max-wait=2000ms`. Redis `timeout=3000ms`.
- `application-prod.properties` conserva su hardening: `validation-timeout=5000`, `leak-detection-threshold=30000`, `connection-test-query=SELECT 1`, `auto-commit=true`, `pool-name=EnviosProdPool`.
- No se modifica `TrackingApiController` ni `EnvioTrackingService`. No se cambia namespace de sesión. No se añaden flags JVM al Dockerfile. No se extiende la caché a otros endpoints.
- Tests de integración (`@SpringBootTest @ActiveProfiles("test")`) requieren MySQL en `localhost:3307` (DB `envios_paraguay_cms_test`) y Redis en `localhost:6379`; `docker compose ps` debe mostrar `db` y `redis` en `healthy`.
- Commits pequeños y atómicos, uno por task. No push ni merge sin confirmación del usuario.

---

### Task 1: Añadir dependencia `commons-pool2`

**Files:**
- Modify: `pom.xml:82-85` (bloque de dependencias redis/session)

**Interfaces:**
- Consumes: nada.
- Produces: `org.apache.commons.pool2` en classpath → habilita el bean `GenericObjectPoolConfig` que bindea las propiedades `spring.data.redis.lettuce.pool.*` (consumido por Task 3).

- [ ] **Step 1: Añadir la dependencia**

Insertar el bloque tras la dependencia `spring-session-data-redis` (líneas 82-85):

```xml
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>
```

El resultado del bloque redis/session queda:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.session</groupId>
            <artifactId>spring-session-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>
```

- [ ] **Step 2: Verificar compilación**

Run: `mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS` sin errores (salida silenciosa con `-q`).

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore(deps): add commons-pool2 for Lettuce connection pooling (sprint optimizacion)"
```

---

### Task 2: Tuning de pools (HikariCP + Lettuce) y Spring Session

**Files:**
- Modify: `src/main/resources/application.properties:17-23` (Hikari) y `:113-120` (Redis/Session)
- Modify: `src/main/resources/application-prod.properties:37-46` (Hikari prod)

**Interfaces:**
- Consumes: Task 1 (`commons-pool2` presente → las props de Lettuce dejan de ser ignoradas).
- Produces: configuración final sobre la que Task 3 valida el comportamiento (pool maxTotal=30, caché TTL 5min, etc.).

- [ ] **Step 1: Actualizar el bloque Hikari de `application.properties`**

Reemplazar las líneas 17-23 actuales:

```properties
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.pool-name=EnviosHikariPool
spring.datasource.hikari.leak-detection-threshold=60000
```

por:

```properties
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.maximum-pool-size=25
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
spring.datasource.hikari.pool-name=EnviosHikariPool
spring.datasource.hikari.leak-detection-threshold=60000
```

- [ ] **Step 2: Actualizar el bloque REDIS / SESSION de `application.properties`**

Reemplazar las líneas 113-120 actuales:

```properties
# =========================
# REDIS / SESSION
# =========================

spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.session.redis.namespace=monteastur:session
server.servlet.session.timeout=30m
```

por:

```properties
# =========================
# REDIS / SESSION
# =========================

spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.timeout=3000ms

# Pool de conexiones Lettuce (requiere commons-pool2 en el classpath)
spring.data.redis.lettuce.pool.max-active=30
spring.data.redis.lettuce.pool.max-idle=15
spring.data.redis.lettuce.pool.min-idle=5
spring.data.redis.lettuce.pool.max-wait=2000ms

# Save/Flush explícitos (defaults de Spring Session; documentan intención anti-race-condition)
spring.session.redis.save-mode=on-set-attribute
spring.session.redis.flush-mode=on-save
spring.session.redis.namespace=monteastur:session
server.servlet.session.timeout=30m
```

- [ ] **Step 3: Reconciliar Hikari en `application-prod.properties`**

Reemplazar las líneas 37-46 actuales:

```properties
# ---- HikariCP Pool (optimizado para TiDB Cloud free tier) ----
spring.datasource.hikari.connection-timeout=60000
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
spring.datasource.hikari.pool-name=EnviosProdPool
spring.datasource.hikari.leak-detection-threshold=30000
spring.datasource.hikari.auto-commit=true
spring.datasource.hikari.validation-timeout=5000
spring.datasource.hikari.connection-test-query=SELECT 1
```

por:

```properties
# ---- HikariCP Pool (optimizado para TiDB Cloud free tier) ----
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.maximum-pool-size=25
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
spring.datasource.hikari.pool-name=EnviosProdPool
spring.datasource.hikari.leak-detection-threshold=30000
spring.datasource.hikari.auto-commit=true
spring.datasource.hikari.validation-timeout=5000
spring.datasource.hikari.connection-test-query=SELECT 1
```

(Se conserva todo el hardening; cambian `connection-timeout` 60000→20000, `maximum-pool-size` 10→25, `minimum-idle` 2→5.)

- [ ] **Step 4: Verificar que el perfil `test` no contradice los valores**

Run:
```powershell
Select-String -Path src/test/resources/application-test.properties -Pattern "hikari|lettuce|session|timeout"
```
Expected: sin salida (el perfil test no overridea pools ni sesión; hereda los valores base).

- [ ] **Step 5: Compilar para validar sintaxis de los properties**

Run: `mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/application.properties src/main/resources/application-prod.properties
git commit -m "perf(config): tune HikariCP and Lettuce Redis pools, explicit session modes (sprint optimizacion)"
```

---

### Task 3: Test de integración del ciclo de vida de la caché `envios.tracking`

**Files:**
- Create: `src/test/java/com/monteastur/envios/integration/EnvioTrackingCacheIntegrationTest.java`
- Test: `src/test/java/com/monteastur/envios/integration/EnvioTrackingCacheIntegrationTest.java`

**Interfaces:**
- Consumes: `EnvioTrackingService.buscarPorCodigo(String): PublicTrackingDto` (con `@Cacheable("envios.tracking", unless="#result == null")`), `EnvioTrackingService.guardar(EnvioTracking): EnvioTracking` (con `@CacheEvict("envios.tracking", allEntries=true)`), `EnvioTrackingRepository.save/deleteById`, bean `CacheManager` (de `RedisConfig`), bean `StringRedisTemplate`, bean `GenericObjectPoolConfig` (auto-configurado solo si `commons-pool2` está en classpath).
- Produces: verificación de los criterios de aceptación 2 de la spec (populate, evict, TTL, pool activo).

**Precondiciones:** `docker compose ps` → `db` y `redis` en `healthy` (MySQL `localhost:3307`, Redis `localhost:6379`).

- [ ] **Step 1: Escribir el test**

Crear `src/test/java/com/monteastur/envios/integration/EnvioTrackingCacheIntegrationTest.java` con el contenido completo:

```java
package com.monteastur.envios.integration;

import com.monteastur.envios.dto.api.PublicTrackingDto;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.EnvioTrackingService;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EnvioTrackingCacheIntegrationTest {

    private static final String CACHE_TRACKING = "envios.tracking";

    @Autowired
    private EnvioTrackingService envioTrackingService;

    @Autowired
    private EnvioTrackingRepository envioTrackingRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectProvider<GenericObjectPoolConfig> lettucePoolConfigProvider;

    private Long envioId;
    private Long envioExtraId;

    @AfterEach
    void limpiar() {
        if (envioExtraId != null) {
            envioTrackingRepository.deleteById(envioExtraId);
        }
        if (envioId != null) {
            envioTrackingRepository.deleteById(envioId);
        }
        var cache = cacheManager.getCache(CACHE_TRACKING);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void segundaConsultaSeSirveDesdeCache() {
        String codigo = "PY-CACHE-" + System.nanoTime();
        EnvioTracking envio = guardarEnvio(codigo);
        envioId = envio.getId();

        PublicTrackingDto primera = envioTrackingService.buscarPorCodigo(codigo);
        assertThat(primera).isNotNull();
        assertThat(primera.getCodigoUnico()).isEqualTo(codigo);

        envioTrackingRepository.deleteById(envioId);
        envioId = null;

        PublicTrackingDto segunda = envioTrackingService.buscarPorCodigo(codigo);
        assertThat(segunda).isNotNull();
        assertThat(segunda.getEstado()).isEqualTo(primera.getEstado());
    }

    @Test
    void guardarEvictaLaEntradaDeTracking() {
        String codigo = "PY-CACHE-" + System.nanoTime();
        EnvioTracking envio = guardarEnvio(codigo);
        envioId = envio.getId();

        assertThat(envioTrackingService.buscarPorCodigo(codigo)).isNotNull();
        envioTrackingRepository.deleteById(envioId);
        envioId = null;

        EnvioTracking otro = new EnvioTracking("PY-CACHE-EVICT-" + System.nanoTime(), "RECIBIDO",
                "Destinatario Test", "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos");
        EnvioTracking guardado = envioTrackingService.guardar(otro);
        envioExtraId = guardado.getId();

        assertThat(envioTrackingService.buscarPorCodigo(codigo)).isNull();
    }

    @Test
    void cacheTieneTtlDeCincoMinutos() {
        String codigo = "PY-CACHE-" + System.nanoTime();
        EnvioTracking envio = guardarEnvio(codigo);
        envioId = envio.getId();

        envioTrackingService.buscarPorCodigo(codigo);

        Long ttl = stringRedisTemplate.getExpire(CACHE_TRACKING + "::" + codigo);
        assertThat(ttl).isNotNull().isBetween(1L, 300L);
    }

    @Test
    void poolLettuceActivo_conMaxTotal30() {
        GenericObjectPoolConfig config = lettucePoolConfigProvider.getIfAvailable();
        assertThat(config).isNotNull();
        assertThat(config.getMaxTotal()).isEqualTo(30);
    }

    private EnvioTracking guardarEnvio(String codigo) {
        return envioTrackingRepository.save(new EnvioTracking(codigo, "RECIBIDO",
                "Destinatario Test", "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos"));
    }
}
```

- [ ] **Step 2: Ejecutar el test**

Run: `mvn test -Dtest=EnvioTrackingCacheIntegrationTest`
Expected: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`. Los 4 tests deben pasar:
- `segundaConsultaSeSirveDesdeCache`: la 2ª llamada no hace query a DB (fila borrada) → servida desde Redis.
- `guardarEvictaLaEntradaDeTracking`: `guardar()` evicta `allEntries` → la llamada posterior da `null`.
- `cacheTieneTtlDeCincoMinutos`: la key `envios.tracking::<codigo>` existe con TTL entre 1 y 300 s.
- `poolLettuceActivo_conMaxTotal30`: el bean `GenericObjectPoolConfig` existe (commons-pool2 presente) y bindea `max-active=30`.

Si `poolLettuceActivo_conMaxTotal30` falla con "isNotNull", el pool no está activo → revisar Task 1 (dependencia) y Task 2 Step 2 (props).

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/monteastur/envios/integration/EnvioTrackingCacheIntegrationTest.java
git commit -m "test: add EnvioTrackingCacheIntegrationTest validating cache lifecycle (sprint optimizacion)"
```

---

### Task 4: Corregir `REPORT.md` de k6 (afirmación obsoleta sobre tracking)

**Files:**
- Modify: `src/test/k6/results/REPORT.md`

**Interfaces:**
- Consumes: realidad del código (commit `4407c07` enrutó tracking al servicio cacheado; los runs k6 del 20:30 midieron cache-hits).
- Produces: reporte alineado con la spec (criterio de aceptación 4).

- [ ] **Step 1: Corregir la tabla de métricas por endpoint (Load y Stress)**

En la tabla `### Load (50 VUs, 616.8s)` y `### Stress (200 VUs, 325.8s)`, cambiar la columna `Tipo` de la fila de tracking:

```
| GET /api/v1/tracking/{codigo} | GET (DB) | 57.1% | 715.060 | 1.159 | 298 | 0.042% |
```
por:
```
| GET /api/v1/tracking/{codigo} | GET (cache Redis) | 57.1% | 715.060 | 1.159 | 298 | 0.042% |
```

Y en Stress:
```
| GET /api/v1/tracking/{codigo} | GET (DB) | 57.2% | 519.209 | 1.594 | 338 | 0.065% |
```
por:
```
| GET /api/v1/tracking/{codigo} | GET (cache Redis) | 57.2% | 519.209 | 1.594 | 338 | 0.065% |
```

- [ ] **Step 2: Corregir la sección "Comparativa cache vs DB"**

Reemplazar las líneas 70-73 actuales:

```
- p(95) agregado Load: 14.26ms a 2.030 rps (60% del tráfico en el endpoint SIN cache). Stress: 41.75ms a 2.788 rps.
- Baseline 1 VU: tracking (DB directa) ≈ 6.1ms vs disponibilidad (Redis) ≈ 5.4ms → ratio 1.13x, **sin beneficio de caché medible a esta escala** (5 filas seed; query trivial).
- Hallazgo estructural confirmado en código: `/api/v1/tracking/{codigo}` consulta `EnvioTrackingRepository` directo y **NO** usa el cache `envios.tracking` de `EnvioTrackingService.buscarPorCodigo` (método `@Cacheable` que ningún controlador invoca). `/api/v1/reservas/disponibilidad` sí usa cache Redis (`envios.disponibilidad`).
- Lectura: a este volumen de datos la DB no es cuello de botella; el beneficio del cache se materializaría con más filas/consultas complejas. Aun así, enrutar tracking por el servicio cacheado elimina una query por request con coste despreciable.
```

por:

```
- p(95) agregado Load: 14.26ms a 2.030 rps (60% del tráfico en el endpoint **cacheados**). Stress: 41.75ms a 2.788 rps.
- Aclaración importante: `GET /api/v1/tracking/{codigo}` **sí usa la caché** — `TrackingApiController` delega en `EnvioTrackingService.buscarPorCodigo` con `@Cacheable("envios.tracking", unless="#result == null")` desde el commit `4407c07` (anterior a estos runs k6). La versión anterior de este reporte afirmaba lo contrario; queda corregido. `/api/v1/reservas/disponibilidad` usa cache Redis (`envios.disponibilidad`).
- Baseline 1 VU: cache-hit (tracking) ≈ 6.1ms vs cache-hit (disponibilidad) ≈ 5.4ms → ratio 1.13x: a 5 filas seed, servir desde Redis frente a una query trivial de MySQL produce latencias indistinguibles. El beneficio del cache se materializa cuando la consulta es compleja o el volumen de datos crece (ver recomendación 4).
```

- [ ] **Step 3: Corregir la recomendación 4**

Reemplazar la línea 86 actual:

```
4. **Tracking sin cache**: enrutar el tracking API por `EnvioTrackingService.buscarPorCodigo` (`@Cacheable("envios.tracking")`, hoy sin invocar) — mejora de mayor impacto para preparar escala, aunque a 5 filas no se nota (ratio 1.13x).
```

por:

```
4. **Tracking en caché (verificado)**: `GET /api/v1/tracking/{codigo}` ya se sirve desde `@Cacheable("envios.tracking")` (TTL 5 min, commit `4407c07`). El sprint de optimización añadió un test de integración que blinda populate/evict/TTL. La evicción por escritura (`guardar`/`actualizarEstado`) es `allEntries`; si el volumen de escrituras crece, valorar evicción por clave (`key` del cache) para no purgar toda la caché en cada update.
```

- [ ] **Step 4: Verificar que no quedan referencias obsoletas**

Run:
```powershell
Select-String -Path src/test/k6/results/REPORT.md -Pattern "SIN cache|sin cache|hoy sin invocar|sin invocar|GET \(DB\)"
```
Expected: sin resultados.

- [ ] **Step 5: Commit**

```bash
git add src/test/k6/results/REPORT.md
git commit -m "docs(k6): correct stale claim about tracking cache in load report (sprint optimizacion)"
```

---

### Task 5: Suite completa, handoff y verificación final

**Files:**
- Modify: `docs/handoff.md` (sección "Mejoras de Hardening y Seguridad Recientes (Sprint Actual)" + "Estado Git Actual")

**Interfaces:**
- Consumes: Tasks 1-4.
- Produces: confirmación de completitud de la spec (criterios de aceptación 1-5).

- [ ] **Step 1: Ejecutar la suite completa**

Run: `mvn clean test -q`
Expected: `BUILD SUCCESS` con todos los tests en verde (los previos + los 4 nuevos de `EnvioTrackingCacheIntegrationTest`). En el resumen de surefire: `Tests run: N, Failures: 0, Errors: 0, Skipped: 0` (con `-q` el detalle se ve en el log; si el BUILD no es SUCCESS, revisar el reporte de surefire en `target/surefire-reports/`).

- [ ] **Step 2: Actualizar `docs/handoff.md`**

Añadir una entrada numerada al final de la sección "🔒 Mejoras de Hardening y Seguridad Recientes (Sprint Actual)" (tras la entrada 4 del módulo de notificaciones, línea ~43):

```
5. **Sprint de Optimización y Resiliencia** (post k6, 2026-07-31):
   - `commons-pool2` añadido al pom para activar el pool de conexiones Lettuce (sin él, las props de pool se ignoraban): commit de Task 1.
   - Tuning de pools: HikariCP max=25/min=5/connection-timeout=20000 (base y prod reconciliado, conservando hardening); pool Lettuce max-active=30/max-idle=15/min-idle=5/max-wait=2000ms; `spring.data.redis.timeout=3000ms`; save/flush mode explícitos manteniendo namespace `monteastur:session`: commit de Task 2.
   - Nuevo test de integración `EnvioTrackingCacheIntegrationTest` (populate/evict/TTL de `envios.tracking` + verificación del bean `GenericObjectPoolConfig`): commit de Task 3.
   - Corregido el `REPORT.md` de k6 (afirmación obsoleta: tracking ya usaba caché desde `4407c07`): commit de Task 4.
   - Verificado: `mvn clean test` en verde.
```

Y actualizar el bloque "Estado Git Actual" (líneas 91-96): `HEAD:` debe reflejar el commit actual de `main` tras los commits de este sprint (ejecutar `git log --oneline -1` para obtenerlo).

- [ ] **Step 3: Verificar árbol y commits del sprint**

Run:
```powershell
git status --short
git log --oneline -6
```
Expected: working tree limpio y 6 commits (spec + 5 de tareas del sprint).

- [ ] **Step 4: Commit final**

```bash
git add docs/handoff.md
git commit -m "docs: update handoff with Sprint de Optimizacion y Resiliencia"
```

---

## Self-Review

- **Spec coverage:** Las 5 tareas cubren los 5 criterios de aceptación de la spec: pom con commons-pool2 (Task 1, CA5), values de properties base/prod (Task 2, CA3), test de integración con los 4 asserts (Task 3, CA2), REPORT.md corregido (Task 4, CA4), `mvn clean test` en verde (Task 5, CA1).
- **Placeholder scan:** Todos los pasos contienen contenido exacto (bloques de properties completos, test completo, texto exacto de reemplazo en REPORT.md). No hay TBD/TODO ni pasos descriptivos sin código.
- **Type consistency:** `buscarPorCodigo(String): PublicTrackingDto`, `guardar(EnvioTracking): EnvioTracking` y el constructor `EnvioTracking(codigo, estado, destinatario, origen, destino, peso, contenido)` coinciden con `EnvioTrackingService.java:29-48` y `EnvioTracking.java:53-64` verificados. La key de caché `envios.tracking::<codigo>` coincide con el serializador `StringRedisSerializer` + separador `::` de `RedisConfig.java:35`. El bean `GenericObjectPoolConfig` (erasure) se resuelve con `ObjectProvider<GenericObjectPoolConfig>`.
