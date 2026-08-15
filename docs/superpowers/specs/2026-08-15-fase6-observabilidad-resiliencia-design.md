# FASE 6 — Observabilidad (Actuator + Micrometer) y Resiliencia (Resilience4j)

**Fecha:** 2026-08-15
**Estado:** Aprobado en brainstorming — pendiente de revisión final y plan de implementación
**Alcance:** Seguridad de Actuator, health check personalizado (MySQL + Redis), métricas Micrometer en servicios críticos y CircuitBreaker/Retry con Resilience4j sobre el despacho de webhooks.

---

## 1. Contexto y objetivos

El proyecto (Spring Boot 3.5.16, Thymeleaf, JPA/Hibernate, MySQL 8, Redis, Bootstrap 5) ya expone métricas de Actuator y Prometheus, pero:

- `/actuator/prometheus` y `/actuator/metrics` están **públicos** en el puerto 8080, que está publicado al host (`docker-compose.yml:52`). Esto expone información interna del sistema.
- No existe health check personalizado que verifique conectividad real de MySQL y Redis con latencias, más allá de los indicadores `db`/`redis` genéricos de Boot.
- No hay métricas de negocio registradas en Micrometer para los servicios críticos (tracking de envíos y difusión WebSocket).
- `WebhookDispatchService.despacharIndividual` (`WebhookDispatchService.java:70-98`) hace llamadas HTTP externas **sin reintentos ni circuit breaker**, con una sola oportunidad y auditoría en `webhook_logs`.

Esta fase implementa observabilidad y resiliencia siguiendo los estándares del repositorio (Java puro, inyección por constructor, cero excepciones silenciadas, TDD).

## 2. Decisiones de diseño aprobadas

| Decisión | Opción elegida |
|---|---|
| Seguridad de Actuator | IP-allowlist (`hasIpAddress`) para `/actuator/prometheus` y `/actuator/metrics/**`; `health`/`info` abiertos; resto de `/actuator/**` con rol `ADMIN` |
| Objetivo de Resilience4j | Solo `WebhookDispatchService` (CircuitBreaker + Retry sobre el POST HTTP) |

## 3. Dependencias (`pom.xml`)

**Verificar existentes** (ya presentes): `spring-boot-starter-actuator` y `micrometer-registry-prometheus` (`pom.xml:76-82`).

**Añadir:**
- `io.github.resilience4j:resilience4j-spring-boot3:2.3.0`
- `io.github.resilience4j:resilience4j-micrometer:2.3.0` (métricas de CircuitBreaker en Prometheus)

Versión fija explícita — Spring Boot no gestiona la versión de Resilience4j. Ajustar a la última compatible si el contexto lo exige.

## 4. Seguridad de Actuator (IP-allowlist)

En `SecurityConfig`, agregar reglas **antes** de `anyRequest().permitAll()`:

1. `/actuator/health`, `/actuator/health/**`, `/actuator/info` → `permitAll`
   (requeridos por probes de Kubernetes/Docker, uptime-kuma y el healthCheck de Render).
2. `/actuator/prometheus` y `/actuator/metrics/**` → acceso restringido por IP:
   `access("hasIpAddress('172.16.0.0/12') or hasIpAddress('10.0.0.0/8') or hasIpAddress('127.0.0.1') or hasIpAddress('::1')")`
   (subred de Docker + loopback).
3. `/actuator/**` restante → `hasRole("ADMIN")` vía HTTP Basic (ya configurado).

Notas:
- `hasIpAddress` usa `request.getRemoteAddr()`. Con nginx en la misma red Docker, la IP de origen es la del contenedor de nginx (subred `172.16.0.0/12`) → permitida. No se cambia `server.forward-headers-strategy`.
- En Render no hay scraper de Prometheus: solo se usa `/actuator/health` (abierto) y credenciales ADMIN para acceso manual. La política de IP no interfiere.
- El índice `/actuator` (raíz) queda cubierto por la regla `/actuator/**` → ADMIN. No se expone ningún endpoint adicional.

### Propiedades (`application.properties` base y `application-prod.properties`)

Estado actual verificado: `show-details=when_authorized`, exposición `health,info,metrics,prometheus` y `prometheus.enabled=true` ya están en base y prod. `probes.enabled=true` solo está en prod (`application-prod.properties:63`).

**Base** — añadir:
- `management.endpoint.health.probes.enabled=true` (activa `/actuator/health/liveness` y `/readiness` también en dev).
- `management.endpoint.health.show-components=when_authorized`.

**Prod** — añadir:
- `management.endpoint.health.show-components=when_authorized`.

## 5. HealthIndicator personalizado (MySQL + Redis en tiempo real)

Nuevo componente `com.monteastur.envios.health.InfraestructuraHealthIndicator` que implementa `HealthIndicator`.

- **Dependencias por constructor:** `DataSource` y `RedisConnectionFactory`.
- **Método `health()`:**
  - Mide latencia con `System.nanoTime()`.
  - **Base de datos:** `SELECT 1` a través de `DataSource.getConnection()` con `try-with-resources`.
  - **Redis:** `PING` a través de una conexión obtenida con `RedisConnectionUtils.getConnection(factory)` dentro de `try-with-resources`.
  - Si ambos responden → `Health.up()` con detalles `database`, `database_latency_ms`, `redis`, `redis_latency_ms`.
  - Si alguno falla → `Health.status(Status.DOWN)` con detalle del error por dependencia y latencias parciales (si se obtuvieron).
- **Registro:** automático por Spring Boot como `/actuator/health/infraestructura` (derivado del nombre de bean `infraestructuraHealthIndicator`).
- **Relación con los indicadores de Boot:** los complementa (no los reemplaza). `db`, `redis`, `ping`, `diskSpace`, `livenessState` y `readinessState` siguen activos.
- `show-details=when_authorized` implica que las probes solo leen `status` (UP/DOWN); los detalles requieren sesión ADMIN.

## 6. Métricas Micrometer en servicios críticos

Nuevo componente `com.monteastur.envios.metrics.BusinessMetrics` que envuelve `MeterRegistry` (inyectado por constructor). API explícita de Micrometer; **sin** `@Timed`/AOP.

### Métricas registradas

| Nombre | Tipo | Tags | Ubicación |
|---|---|---|---|
| `envios.tracking.pagina` | `Timer` | `encontrado=true/false` | `PublicTrackingService.cargarPagina` |
| `envios.tracking.resultado` | `Counter` | — | `PublicTrackingService.cargarPagina` |
| `envios.websocket.difusion` | `Timer` | `resultado=ok/error` | `WebSocketEventListener.manejar` |
| `envios.websocket.resultado` | `Counter` | — | `WebSocketEventListener.manejar` |

- La métrica de tracking mide el trabajo del servicio (incluye la ruta con y sin cache miss dentro de `cargarPagina`); el tag `encontrado` distingue resultados.
- Las métricas de websocket miden el ciclo de difusión (broadcast) y distinguen éxito/error.
- Los timers se registran con el ciclo completo del método instrumentado; se usa `Timer.Sample` para medir y luego detener con `stop(context)`.

## 7. Resilience4j sobre WebhookDispatchService

### 7.1 Comportamiento de `despacharIndividual`

- Deja de tragar el fallo para errores **transitorios**: lanza `com.monteastur.envios.exception.WebhookDispatchException` en:
  - errores de red / timeout (`ResourceAccessException`), y
  - respuestas HTTP **5xx** del endpoint del cliente.
- Las respuestas **4xx no lanzan excepción**: el intento se audita como fallo en `webhook_logs` (auditoría por intento ya existente) y el método retorna sin propagar. No se reintentan (el contrato del lado del cliente no mejora con reintentos) y no cuentan como fallo del CircuitBreaker.
- La excepción transporta `statusCode`, `attempts` y `message` para la auditoría.
- De este modo `Retry` y `CircuitBreaker` solo ven fallos transitorios: su excepción registrada es únicamente `WebhookDispatchException`.

### 7.2 Anotaciones y configuración

- `@Retry(name = "webhook")` + `@CircuitBreaker(name = "webhook")` sobre `despacharIndividual`.

**Propiedades** (`application.properties` base y prod):

```
resilience4j.retry.instances.webhook.max-attempts=3
resilience4j.retry.instances.webhook.wait-duration=1s
resilience4j.retry.instances.webhook.enable-exponential-backoff=true
resilience4j.retry.instances.webhook.exponential-backoff-multiplier=2
resilience4j.retry.instances.webhook.retry-exceptions=com.monteastur.envios.exception.WebhookDispatchException
resilience4j.circuitbreaker.instances.webhook.sliding-window-size=10
resilience4j.circuitbreaker.instances.webhook.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.webhook.wait-duration-in-open-state=30s
resilience4j.circuitbreaker.instances.webhook.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.webhook.record-exceptions=com.monteastur.envios.exception.WebhookDispatchException
resilience4j.circuitbreaker.instances.webhook.register-health-indicator=true
```

- El CircuitBreaker se expone como contribuidor de `/actuator/health` y sus métricas `resilience4j_circuitbreaker_*` se publican en Prometheus vía `resilience4j-micrometer`.

### 7.3 Flujo de `despachar`

- Itera los envíos pendientes y llama a `despacharIndividual`.
- Si `despacharIndividual` termina en fallo (agotados reintentos o `CallNotPermittedException`), `despachar` captura la excepción y **audita el resultado final** en `webhook_logs`, incluyendo `statusCode`, `attempts` y mensaje de error. El flujo continúa con el siguiente envío (un fallo no rompe el batch).
- Se mantiene la regla de auditoría actual: cada intento ya genera su fila en `webhook_logs`; se añade la fila final con el desenlace.

## 8. Tests

### Nuevos

1. **`ActuatorSecurityIntegrationTest`** (`@SpringBootTest` + `@AutoConfigureMockMvc`)
   - `/actuator/health` → 200 y body con `"UP"`.
   - `/actuator/health/liveness` y `/actuator/health/readiness` → 200 y `UP`.
   - `/actuator/health/infraestructura` → 200 y `UP` con detalles `database`/`redis`.
   - `/actuator/prometheus` desde IP no permitida (`remoteAddress("203.0.113.10")`) → 401/403.
   - `/actuator/prometheus` desde IP Docker (`remoteAddress("172.18.0.5")`) → 200.
   - `/actuator/metrics` análogo (bloqueado fuera de allowlist, 200 dentro).
   - `/actuator/configprops` o equivalente sin ADMIN → 401/403; con credenciales ADMIN → 200.

2. **`PrometheusMetricsExposureTest`** (`@SpringBootTest` + `@AutoConfigureMockMvc`)
   - GET `/actuator/prometheus` (admin o IP permitida) → 200, `Content-Type` `text/plain`, body con `jvm_*` y `http_server_requests_seconds_*`.
   - Tras invocar un flujo de tracking (o directamente el componente de métricas), el body contiene `envios_tracking_pagina_seconds_*` o `envios_tracking_resultado_total`.

3. **`InfraestructuraHealthIndicatorTest`** (unit, con mocks)
   - MySQL y Redis OK → `UP` con latencias ≥ 0.
   - `SELECT 1` falla → `DOWN` con detalle de error en `database`.
   - Redis `PING` falla → `DOWN` con detalle de error en `redis`.

4. **`BusinessMetricsTest`** (unit, `SimpleMeterRegistry`)
   - Timer/counter de tracking y websocket se registran e incrementan.

5. **`WebhookDispatchServiceTest`** (ampliación)
   - 2 fallos transitorios (`ResourceAccessException`) seguidos de éxito → se realizan 3 intentos y el estado auditado final es éxito.
   - Respuesta 5xx → reintentos hasta `max-attempts` y fallo auditado final con `statusCode` y `attempts`.
   - Respuesta 4xx → sin reintentos: `despacharIndividual` retorna y la auditoría por intento registra el fallo (sin excepción propagada).
   - Tras superar el umbral de fallos → el CircuitBreaker se abre y `despacharIndividual` lanza `CallNotPermittedException`; `despachar` lo captura y audita.

### Mantener verdes

- **`WebhookDispatchIntegrationTest`** (sink HTTP local): el caso 200 sigue siendo éxito; el caso 500 ahora consume 3 intentos pero el resultado final auditado no cambia. Ajustar aserciones si verifican número de llamadas.
- Suite completa `mvn clean test` → **BUILD SUCCESS** (JDK 25 + MySQL/Redis Docker).

## 9. Archivos afectados

**Modificados**
- `pom.xml`
- `SecurityConfig.java`
- `application.properties`, `application-prod.properties`
- `PublicTrackingService.java`
- `WebSocketEventListener.java`
- `WebhookDispatchService.java`
- `WebhookDispatchIntegrationTest.java` (si las aserciones dependen del nº de llamadas)

**Nuevos**
- `com.monteastur.envios.health.InfraestructuraHealthIndicator.java`
- `com.monteastur.envios.metrics.BusinessMetrics.java`
- `com.monteastur.envios.exception.WebhookDispatchException.java`
- `ActuatorSecurityIntegrationTest.java`
- `PrometheusMetricsExposureTest.java`
- `InfraestructuraHealthIndicatorTest.java`
- `BusinessMetricsTest.java`
- Ampliación de `WebhookDispatchServiceTest.java`

## 10. Riesgos y consideraciones

- `hasIpAddress` depende de la IP de origen real; si en el futuro Prometheus scrapea desde otra subred, hay que añadirla al allowlist.
- La auditoría de `webhook_logs` crece: cada reintento añade filas. Es aceptable (diseño actual ya registra por intento) y aporta trazabilidad.
- No se añade `resilience4j-retry` ni `circuitbreaker` a la configuración de Spring Cloud Gateway ni a otras integraciones: queda acotado al webhook (YAGNI).
