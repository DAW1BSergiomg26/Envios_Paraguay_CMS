# FASE 6 — Observabilidad y Resiliencia Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Asegurar los endpoints de Actuator por IP, añadir un health check real de MySQL+Redis, registrar métricas Micrometer en tracking/WebSocket y dotar al despacho de webhooks de Retry + CircuitBreaker con Resilience4j.

**Architecture:** Spring Boot 3.5.16 / Java 25. Se añade Resilience4j (versión fija 2.3.0) en modo **programático** (los registries `RetryRegistry`/`CircuitBreakerRegistry` se exponen como beans y `WebhookDispatchService` decora el POST HTTP con `Retry`/`CircuitBreaker`). El CircuitBreaker se expone como health indicator y sus métricas salen a Prometheus vía `resilience4j-micrometer`. El health check de infraestructura es un `HealthIndicator` propio y las métricas de negocio van en un wrapper `BusinessMetrics` sobre `MeterRegistry`.

**Tech Stack:** Spring Boot 3.5.16, Resilience4j 2.3.0, Micrometer, Spring Security, Flyway, MySQL 8, Redis, JUnit 5 + Mockito + AssertJ + Awaitility.

## Global Constraints

- **Java puro:** prohibido Lombok. Entidades, DTOs y modelos con atributos privados, constructor vacío, constructores con parámetros y getters/setters manuales.
- **Inyección por constructor:** campos de servicios/componentes como `private final`, inicializados en el constructor. Cero `@Autowired` en campos.
- **TDD estricto:** el código de producción se escribe únicamente después de un test que falle (Iron Law). Cada tarea sigue RED → GREEN → REFACTOR.
- **Cero excepciones silenciadas:** los fallos de integraciones se capturan, auditan (en `webhook_logs`) y se registran; nunca se tragan sin dejar rastro.
- **Identidad corporativa:** color `#d4762a`.
- **Verificación final:** `mvn clean test` debe terminar en `BUILD SUCCESS` con la suite completa (421+ tests) usando JDK 25 + MySQL/Redis en Docker.
- **Nota del Arquitecto Principal:** no romper los probes de Docker/Uptime Kuma: `/actuator/health`, `/actuator/health/**` y `/actuator/info` quedan `permitAll()`.

---

### Task 1: Dependencias Resilience4j y arranque de configuración

**Files:**
- Modify: `pom.xml` (bloque de dependencias, tras el bloque `micrometer-registry-prometheus`, línea ~80)
- Create: `src/main/java/com/monteastur/envios/exception/WebhookDispatchException.java`
- Create: `src/main/java/com/monteastur/envios/config/ResilienceConfig.java`

**Interfaces:**
- Consumes: nada (es el primer bloque).
- Produces:
  - `com.monteastur.envios.exception.WebhookDispatchException extends RuntimeException` con getters `getStatusCode()` (`Integer`) y `getAttempts()` (`Integer`).
  - Beans `RetryRegistry` y `CircuitBreakerRegistry` disponibles en el contexto (auto-config de Resilience4j).
  - `com.monteastur.envios.config.ResilienceConfig` que expone los beans `Retry` y `CircuitBreaker` nombrados `"webhook"` a partir de los registries.

- [ ] **Step 1: Añadir las dependencias a `pom.xml`**

Insertar en el bloque de dependencias (tras el `micrometer-registry-prometheus`, línea 82):

```xml
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>2.3.0</version>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-micrometer</artifactId>
            <version>2.3.0</version>
        </dependency>
```

- [ ] **Step 2: Compilar para verificar que las dependencias resuelven**

Run: `.\mvnw.cmd -q -DskipTests compile`
Expected: `BUILD SUCCESS` (o al menos compilación sin errores de dependencias).

- [ ] **Step 3: Escribir `WebhookDispatchException`**

Crear `src/main/java/com/monteastur/envios/exception/WebhookDispatchException.java`:

```java
package com.monteastur.envios.exception;

/**
 * Excepción lanzada cuando el despacho de un webhook falla de forma
 * transitoria (timeout, error de red o respuesta HTTP 5xx) tras agotar
 * los reintentos configurados en Resilience4j. Transporta el status HTTP
 * y el número de intentos para su auditoría en webhook_logs.
 */
public class WebhookDispatchException extends RuntimeException {

    private final Integer statusCode;
    private final Integer attempts;

    public WebhookDispatchException(String message, Integer statusCode, Integer attempts, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.attempts = attempts;
    }

    public WebhookDispatchException(String message, Integer statusCode, Integer attempts) {
        super(message);
        this.statusCode = statusCode;
        this.attempts = attempts;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Integer getAttempts() {
        return attempts;
    }
}
```

- [ ] **Step 4: Escribir `ResilienceConfig`**

Crear `src/main/java/com/monteastur/envios/config/ResilienceConfig.java`:

```java
package com.monteastur.envios.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone las instancias de Retry y CircuitBreaker de nombre "webhook"
 * gestionadas por los registries de Resilience4j (que leen la
 * configuración de application.properties). Las métricas del
 * CircuitBreaker se publican en Prometheus vía resilience4j-micrometer y
 * su estado se expone en /actuator/health vía register-health-indicator.
 */
@Configuration
public class ResilienceConfig {

    public static final String WEBHOOK_INSTANCE = "webhook";

    @Bean
    public Retry webhookRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry(WEBHOOK_INSTANCE);
    }

    @Bean
    public CircuitBreaker webhookCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker(WEBHOOK_INSTANCE);
    }
}
```

- [ ] **Step 5: Compilar**

Run: `.\mvnw.cmd -q -DskipTests compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java/com/monteastur/envios/exception/WebhookDispatchException.java src/main/java/com/monteastur/envios/config/ResilienceConfig.java
git commit -m "feat(resilience): add resilience4j 2.3.0, WebhookDispatchException and webhook resilience beans"
```

---

### Task 2: WebhookDispatchService con Retry + CircuitBreaker

**Files:**
- Modify: `src/main/java/com/monteastur/envios/service/WebhookDispatchService.java` (constructor, `despachar`, `despacharIndividual` → nueva estructura)

**Interfaces:**
- Consumes: `Retry` y `CircuitBreaker` beans (Task 1), `WebhookDispatchException` (Task 1).
- Produces:
  - Constructor público: `WebhookDispatchService(EnvioTrackingRepository, WebhookConfigRepository, WebhookLogRepository, WebhookPayloadBuilder, RestClient, Retry, CircuitBreaker)`.
  - `public void despachar(EstadoEnvioActualizadoEvent event)` — comportamiento sin cambios hacia fuera; un fallo de un config no rompe el batch.
  - `public void despacharIndividual(WebhookConfig config, EstadoEnvioActualizadoEvent event, String payload)` — decorado programáticamente con CircuitBreaker (fuera) + Retry (dentro). Lanza `WebhookDispatchException` en 5xx/timeout agotados; en 4xx retorna y audita fallo por intento sin excepción.

- [ ] **Step 1: Reescribir `WebhookDispatchService`**

Sustituir el contenido completo de `src/main/java/com/monteastur/envios/service/WebhookDispatchService.java` por:

```java
package com.monteastur.envios.service;

import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.exception.WebhookDispatchException;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.WebhookConfig;
import com.monteastur.envios.model.WebhookLog;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.WebhookConfigRepository;
import com.monteastur.envios.repository.WebhookLogRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.function.Supplier;

@Service
public class WebhookDispatchService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatchService.class);

    private static final String CABECERA_FIRMA = "X-Signature-256";

    private final EnvioTrackingRepository envioTrackingRepository;
    private final WebhookConfigRepository webhookConfigRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final WebhookPayloadBuilder payloadBuilder;
    private final RestClient webhookRestClient;
    private final Retry webhookRetry;
    private final CircuitBreaker webhookCircuitBreaker;

    @Value("${app.webhook.tracking.base-url:http://localhost:8080/tracking}")
    private String baseUrl;

    public WebhookDispatchService(EnvioTrackingRepository envioTrackingRepository,
                                  WebhookConfigRepository webhookConfigRepository,
                                  WebhookLogRepository webhookLogRepository,
                                  WebhookPayloadBuilder payloadBuilder,
                                  RestClient webhookRestClient,
                                  Retry webhookRetry,
                                  CircuitBreaker webhookCircuitBreaker) {
        this.envioTrackingRepository = envioTrackingRepository;
        this.webhookConfigRepository = webhookConfigRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.payloadBuilder = payloadBuilder;
        this.webhookRestClient = webhookRestClient;
        this.webhookRetry = webhookRetry;
        this.webhookCircuitBreaker = webhookCircuitBreaker;
    }

    @CacheEvict(value = "envios.analytics", allEntries = true)
    public void despachar(EstadoEnvioActualizadoEvent event) {
        EnvioTracking envio = envioTrackingRepository
                .findWithClienteByCodigoUnico(event.codigoRastreo())
                .orElse(null);
        if (envio == null || envio.getCliente() == null) {
            log.info("Webhook: envío {} sin cliente; se omite el despacho", event.codigoRastreo());
            return;
        }
        List<WebhookConfig> configs =
                webhookConfigRepository.findByClienteIdAndActivoTrue(envio.getCliente().getId());
        if (configs.isEmpty()) {
            log.info("Webhook: cliente {} sin webhooks activos; se omite", envio.getCliente().getId());
            return;
        }
        String payload = payloadBuilder.construir(event, envio, baseUrl);
        for (WebhookConfig config : configs) {
            try {
                despacharIndividual(config, event, payload);
            } catch (WebhookDispatchException e) {
                // Fallo transitorio agotado o circuit breaker abierto: se audita
                // el resultado final y se continúa con el siguiente webhook del batch.
                log.warn("Webhook {} falló definitivamente: {}", config.getId(), e.getMessage());
                webhookLogRepository.save(new WebhookLog(config.getId(), event.envioId(),
                        payload, e.getStatusCode(), false, e.getMessage()));
            }
        }
    }

    /**
     * Despacha un único webhook. La resiliencia se aplica de forma programática
     * sobre la llamada HTTP (el CircuitBreaker envuelve al Retry): solo los fallos
     * transitorios (5xx / timeout / error de red) se registran en el CircuitBreaker
     * y se reintentan con backoff exponencial. Las respuestas 4xx no se reintentan
     * ni cuentan para el CircuitBreaker: se auditan como fallo por intento y el
     * método retorna sin propagar excepción.
     * Nota: la invocación es interna (auto-llamada), por lo que la resiliencia se
     * resuelve aquí con decoradores programáticos en lugar de anotaciones AOP.
     */
    public void despacharIndividual(WebhookConfig config, EstadoEnvioActualizadoEvent event, String payload) {
        String firma = WebhookSignature.hmacSha256(config.getSecretToken(), payload);
        Integer status = null;
        boolean exitoso = false;
        String error = null;
        try {
            Supplier<Integer> llamadaResiliente = CircuitBreaker.decorateSupplier(
                    webhookCircuitBreaker, Retry.decorateSupplier(webhookRetry, () -> enviar(config, payload, firma)));
            status = llamadaResiliente.get();
            exitoso = status >= 200 && status < 300;
            if (!exitoso) {
                error = "HTTP " + status;
            }
            log.info("Webhook {} -> HTTP {} ({})", config.getId(), status, exitoso ? "OK" : "fallo");
        } catch (WebhookDispatchException e) {
            throw e;
        } catch (Exception e) {
            error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("Fallo de red en webhook {} -> {}: {}", config.getId(), config.getUrl(), error);
        }
        webhookLogRepository.save(new WebhookLog(config.getId(), event.envioId(), payload, status, exitoso, error));
    }

    private int enviar(WebhookConfig config, String payload, String firma) {
        var response = webhookRestClient.post()
                .uri(config.getUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header(CABECERA_FIRMA, firma)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        int status = response.getStatusCode().value();
        if (status >= 500) {
            throw new WebhookDispatchException("HTTP " + status, status, null);
        }
        return status;
    }

    private void auditarFalloDeRed(WebhookConfig config, String payload, int intento, Exception causa) {
        String error = causa.getMessage() != null ? causa.getMessage() : causa.getClass().getSimpleName();
        throw new WebhookDispatchException(
                "Fallo transitorio al enviar webhook " + config.getId() + ": " + error
                        + " tras " + intento + " intento(s)",
                null, intento, causa);
    }
}
```

- [ ] **Step 2: Compilar**

Run: `.\mvnw.cmd -q -DskipTests compile`
Expected: `BUILD SUCCESS`. (La compilación puede fallar si algún test ya construye el servicio con el constructor antiguo; se arregla en Task 3.)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/WebhookDispatchService.java
git commit -m "feat(resilience): wrap webhook HTTP POST with circuit breaker and retry"
```

---

### Task 3: Ampliar `WebhookDispatchServiceTest` (RED/GREEN de resiliencia)

**Files:**
- Modify: `src/test/java/com/monteastur/envios/service/WebhookDispatchServiceTest.java`

**Interfaces:**
- Consumes: `RetryRegistry`, `CircuitBreakerRegistry`, `RetryConfig`, `CircuitBreakerConfig` (Resilience4j), constructor nuevo de `WebhookDispatchService` (Task 2).
- Produces: cobertura de retry (3 intentos), 4xx sin reintento, apertura de CircuitBreaker y auditoría final.

- [ ] **Step 1: Actualizar `setUp()` y helpers**

Reemplazar el `setUp()` (líneas 65-76) por:

```java
    private Retry retryPorDefecto() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(50))
                .enableExponentialBackoff()
                .exponentialBackoffMultiplier(2)
                .retryExceptions(WebhookDispatchException.class)
                .build();
        return Retry.of("webhook", config);
    }

    private CircuitBreaker circuitBreakerCerrado() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .recordExceptions(WebhookDispatchException.class)
                .build();
        return CircuitBreaker.of("webhook", config);
    }

    @BeforeEach
    void setUp() {
        service = new WebhookDispatchService(envioTrackingRepository, webhookConfigRepository,
                webhookLogRepository, new WebhookPayloadBuilder(new ObjectMapper()), webhookRestClient,
                retryPorDefecto(), circuitBreakerCerrado());
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080/tracking");
        org.mockito.Mockito.lenient().when(webhookRestClient.post()).thenReturn(requestBodyUriSpec);
        org.mockito.Mockito.lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        org.mockito.Mockito.lenient().when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        org.mockito.Mockito.lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        org.mockito.Mockito.lenient().when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        org.mockito.Mockito.lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }
```

Añadir imports: `io.github.resilience4j.circuitbreaker.CircuitBreaker`, `io.github.resilience4j.circuitbreaker.CircuitBreakerConfig`, `io.github.resilience4j.retry.Retry`, `io.github.resilience4j.retry.RetryConfig`, `java.time.Duration`.

- [ ] **Step 2: Ajustar los tests existentes al nuevo mensaje de error**

En `respuesta500_registraFalloConStatusYError` (el mock lanza `RestClientResponseException(500)`), la auditoría final cambia: ahora hay 3 intentos y el mensaje es `Fallo transitorio al enviar webhook ...: HTTP 500 tras 3 intento(s)`, y el `statusCode` de la fila final es `null` (el CircuitBreaker abierto o el flujo de red no transporta el status). Reemplazar las aserciones (líneas 179-184) por:

```java
        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository).save(captor.capture());
        WebhookLog log = captor.getValue();
        assertThat(log.isExitoso()).isFalse();
        assertThat(log.getErrorMensaje()).contains("tras 3 intento(s)");
        verify(requestBodyUriSpec, org.mockito.Mockito.times(3)).uri(anyString());
```

En `errorDeRed_registraFalloSinStatus` (mock lanza `ResourceAccessException("connect timed out")`), sustituir la aserción de mensaje (línea 202) por:

```java
        assertThat(log.getErrorMensaje()).contains("connect timed out");
        assertThat(log.getErrorMensaje()).contains("tras 3 intento(s)");
```

(El `verify(requestBodyUriSpec, times(3))` para este test: añadir igualmente.)

- [ ] **Step 3: Añadir el test de 2 fallos + éxito (3 intentos, auditoría final de éxito)**

Añadir tras `errorDeRed_registraFalloSinStatus`:

```java
    @Test
    void dosFallosTransitoriosYSegundoExito_tresIntentosYAuditoriaFinalExitosa() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-7"))
                .thenReturn(Optional.of(envioConCliente("MT-7", 10L)));
        WebhookConfig config = new WebhookConfig(10L, "https://hook.a/endpoint", "secret-a");
        when(webhookConfigRepository.findByClienteIdAndActivoTrue(10L)).thenReturn(List.of(config));
        when(responseSpec.toBodilessEntity())
                .thenThrow(new ResourceAccessException("connect timed out"))
                .thenThrow(new ResourceAccessException("connect timed out"))
                .thenReturn(ResponseEntity.ok().build());

        service.despachar(evento("MT-7", 7L));

        verify(requestBodyUriSpec, org.mockito.Mockito.times(3)).uri(anyString());
        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository).save(captor.capture());
        WebhookLog log = captor.getValue();
        assertThat(log.isExitoso()).isTrue();
        assertThat(log.getResponseStatus()).isEqualTo(200);
        assertThat(log.getErrorMensaje()).isNull();
    }
```

- [ ] **Step 4: Añadir el test de 4xx sin reintento**

```java
    @Test
    void respuesta400_noReintentaYAuditaFalloPorIntento() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-8"))
                .thenReturn(Optional.of(envioConCliente("MT-8", 10L)));
        WebhookConfig config = new WebhookConfig(10L, "https://hook.a/endpoint", "secret-a");
        when(webhookConfigRepository.findByClienteIdAndActivoTrue(10L)).thenReturn(List.of(config));
        when(responseSpec.toBodilessEntity()).thenThrow(
                new RestClientResponseException("400 Bad Request", 400, "Bad Request",
                        new HttpHeaders(), new byte[0], null));

        service.despachar(evento("MT-8", 8L));

        verify(requestBodyUriSpec, org.mockito.Mockito.times(1)).uri(anyString());
        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository).save(captor.capture());
        WebhookLog log = captor.getValue();
        assertThat(log.isExitoso()).isFalse();
        assertThat(log.getResponseStatus()).isEqualTo(400);
    }
```

- [ ] **Step 5: Añadir el test de CircuitBreaker abierto**

```java
    @Test
    void circuitBreakerAbierto_auditaFalloDeContingenciaSinLlamarAlSink() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .recordExceptions(WebhookDispatchException.class)
                .build();
        CircuitBreaker abiertoRapido = CircuitBreaker.of("webhook", config);
        service = new WebhookDispatchService(envioTrackingRepository, webhookConfigRepository,
                webhookLogRepository, new WebhookPayloadBuilder(new ObjectMapper()), webhookRestClient,
                retryPorDefecto(), abiertoRapido);

        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-9"))
                .thenReturn(Optional.of(envioConCliente("MT-9", 10L)));
        WebhookConfig config1 = new WebhookConfig(10L, "https://hook.a/endpoint", "secret-a");
        when(webhookConfigRepository.findByClienteIdAndActivoTrue(10L)).thenReturn(List.of(config1));
        when(responseSpec.toBodilessEntity()).thenThrow(
                new RestClientResponseException("500 Internal Server Error", 500, "Internal Server Error",
                        new HttpHeaders(), new byte[0], null));

        service.despachar(evento("MT-9-1", 9L));
        assertThat(abiertoRapido.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        service.despachar(evento("MT-9-2", 10L));

        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        WebhookLog finalBreaker = captor.getAllValues().get(1);
        assertThat(finalBreaker.isExitoso()).isFalse();
        assertThat(finalBreaker.getErrorMensaje())
                .contains("CircuitBreaker", "abierto", "contingencia");
    }
```

> Nota: con `slidingWindowSize=2` y `minimumNumberOfCalls=2`, el primer `despachar` agota el Retry (3 llamadas, 3 fallos registrados) y abre el breaker. El segundo `despachar` falla inmediatamente con `CallNotPermittedException`. Para que el mensaje contenga las palabras esperadas, añadir en `WebhookDispatchService.despacharIndividual` el manejo de `CallNotPermittedException`:

En `despacharIndividual`, dentro del `catch (Exception e)`, antes de construir el error genérico, insertar:

```java
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            throw new WebhookDispatchException(
                    "CircuitBreaker abierto: despacho de webhook " + config.getId() + " bloqueado en contingencia",
                    null, null, e);
```

- [ ] **Step 6: Ejecutar los tests del servicio**

Run: `.\mvnw.cmd -q -Dtest=WebhookDispatchServiceTest test`
Expected: `BUILD SUCCESS` con los 9 tests (5 existentes ajustados + 4 nuevos) en verde.

- [ ] **Step 7: Commit**

```bash
git add src/test/java/com/monteastur/envios/service/WebhookDispatchServiceTest.java src/main/java/com/monteastur/envios/service/WebhookDispatchService.java
git commit -m "test(resilience): cover retry, 4xx-no-retry and open circuit breaker in webhook dispatch"
```

---

### Task 4: Configuración de Resilience4j en propiedades

**Files:**
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/resources/application-prod.properties`
- Modify: `src/test/resources/application-test.properties`

**Interfaces:**
- Consumes: nada nuevo.
- Produces: instancias `webhook` de `Retry` y `CircuitBreaker` configuradas; health indicator del CircuitBreaker activo en prod, desactivado en tests.

- [ ] **Step 1: Añadir bloque Resilience4j a `application.properties`**

Añadir al final del archivo:

```properties
# =========================
# RESILIENCE4J — WEBHOOKS
# =========================
# Reintentos con backoff exponencial para fallos transitorios (5xx/timeout)
resilience4j.retry.instances.webhook.max-attempts=3
resilience4j.retry.instances.webhook.wait-duration=1s
resilience4j.retry.instances.webhook.enable-exponential-backoff=true
resilience4j.retry.instances.webhook.exponential-backoff-multiplier=2
resilience4j.retry.instances.webhook.retry-exceptions=com.monteastur.envios.exception.WebhookDispatchException
# CircuitBreaker: se abre con tasa de fallo >= 50% en ventana de 10 llamadas
resilience4j.circuitbreaker.instances.webhook.sliding-window-size=10
resilience4j.circuitbreaker.instances.webhook.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.webhook.wait-duration-in-open-state=30s
resilience4j.circuitbreaker.instances.webhook.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.webhook.record-exceptions=com.monteastur.envios.exception.WebhookDispatchException
resilience4j.circuitbreaker.instances.webhook.register-health-indicator=true
```

- [ ] **Step 2: Añadir `show-components` a `application.properties`**

Junto al bloque ACTUATOR/HEALTH (líneas 72-77), tras `management.endpoint.health.show-details=when_authorized`, añadir:

```properties
management.endpoint.health.probes.enabled=true
management.endpoint.health.show-components=when_authorized
```

- [ ] **Step 3: Añadir `show-components` a `application-prod.properties`**

En el bloque Actuator (líneas 60-67), tras `management.endpoint.health.probes.enabled=true`, añadir:

```properties
management.endpoint.health.show-components=when_authorized
```

- [ ] **Step 4: Añadir config de test a `application-test.properties`**

Añadir al final:

```properties
# Resilience4j: 3 intentos rápidos y breaker tolerante para los tests de integración
resilience4j.retry.instances.webhook.max-attempts=3
resilience4j.retry.instances.webhook.wait-duration=100ms
resilience4j.retry.instances.webhook.enable-exponential-backoff=true
resilience4j.retry.instances.webhook.exponential-backoff-multiplier=2
resilience4j.retry.instances.webhook.retry-exceptions=com.monteastur.envios.exception.WebhookDispatchException
resilience4j.circuitbreaker.instances.webhook.sliding-window-size=10
resilience4j.circuitbreaker.instances.webhook.minimum-number-of-calls=10
resilience4j.circuitbreaker.instances.webhook.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.webhook.record-exceptions=com.monteastur.envios.exception.WebhookDispatchException
# En tests el estado del breaker no participa en /actuator/health
resilience4j.circuitbreaker.instances.webhook.register-health-indicator=false
# Admin de test para los tests de Actuator (HTTP Basic)
app.admin.username=admin
app.admin.password=admin123
```

- [ ] **Step 5: Verificar que el contexto arranca**

Run: `.\mvnw.cmd -q -Dtest=WebhookDispatchIntegrationTest test`
Expected: `BUILD SUCCESS` (el contexto con los beans de Resilience4j arranca; el test de 200 sigue en verde).

> El test de 500 de la integración sigue verde tal cual (solo verifica la fila final auditada; ahora consumirá 3 intentos). Si se desea validar los 3 intentos, en `WebhookDispatchIntegrationTest` añadir un contador `volatile int sinkCalls` que se incremente en el handler del sink y una aserción `assertThat(sinkCalls).isEqualTo(3)` en el test de 500. Esto es opcional pero recomendado.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/application.properties src/main/resources/application-prod.properties src/test/resources/application-test.properties
git commit -m "config(resilience): tune retry and circuit breaker properties for webhooks (base, prod, test)"
```

---

### Task 5: HealthIndicator de infraestructura (MySQL + Redis)

**Files:**
- Create: `src/main/java/com/monteastur/envios/health/InfraestructuraHealthIndicator.java`
- Create: `src/test/java/com/monteastur/envios/health/InfraestructuraHealthIndicatorTest.java`

**Interfaces:**
- Consumes: `DataSource`, `RedisConnectionFactory`, `Health`, `HealthIndicator`, `HealthIndicatorRegistry` (auto).
- Produces: bean `infraestructuraHealthIndicator` → endpoint `/actuator/health/infraestructura`.

- [ ] **Step 1: Escribir el test (RED)**

Crear `src/test/java/com/monteastur/envios/health/InfraestructuraHealthIndicatorTest.java`:

```java
package com.monteastur.envios.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfraestructuraHealthIndicatorTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    private InfraestructuraHealthIndicator indicador;

    @BeforeEach
    void setUp() {
        indicador = new InfraestructuraHealthIndicator(dataSource, redisConnectionFactory);
    }

    @Test
    void mysqlYRedisOk_devuelveUpConLatencias() throws Exception {
        Connection conexion = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conexion);
        RedisConnection redis = mock(RedisConnection.class);
        when(redisConnectionFactory.getConnection()).thenReturn(redis);
        when(redis.ping()).thenReturn("PONG");

        Health salud = indicador.health();

        assertThat(salud.getStatus()).isEqualTo(Status.UP);
        assertThat(salud.getDetails()).containsKey("database");
        assertThat(salud.getDetails()).containsKey("database_latency_ms");
        assertThat(salud.getDetails()).containsKey("redis");
        assertThat((Double) salud.getDetails().get("database_latency_ms")).isGreaterThanOrEqualTo(0);
        assertThat((Double) salud.getDetails().get("redis_latency_ms")).isGreaterThanOrEqualTo(0);
    }

    @Test
    void select1Falla_devuelveDownConDetalleDeError() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));

        Health salud = indicador.health();

        assertThat(salud.getStatus()).isEqualTo(Status.DOWN);
        assertThat((String) salud.getDetails().get("database")).contains("connection refused");
    }

    @Test
    void pingRedisFalla_devuelveDownConDetalleDeError() throws Exception {
        Connection conexion = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conexion);
        when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("redis down"));

        Health salud = indicador.health();

        assertThat(salud.getStatus()).isEqualTo(Status.DOWN);
        assertThat((String) salud.getDetails().get("redis")).contains("redis down");
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

Run: `.\mvnw.cmd -q -Dtest=InfraestructuraHealthIndicatorTest test`
Expected: FAIL (compilación: `InfraestructuraHealthIndicator` no existe).

- [ ] **Step 3: Implementar `InfraestructuraHealthIndicator`**

Crear `src/main/java/com/monteastur/envios/health/InfraestructuraHealthIndicator.java`:

```java
package com.monteastur.envios.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * HealthIndicator que verifica conectividad real de MySQL (SELECT 1) y
 * Redis (PING) midiendo la latencia de cada dependencia. Se registra
 * automáticamente como /actuator/health/infraestructura y complementa a
 * los indicadores genéricos de Spring Boot (db, redis, ping, diskSpace).
 */
@Component
public class InfraestructuraHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(InfraestructuraHealthIndicator.class);

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public InfraestructuraHealthIndicator(DataSource dataSource,
                                          RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        boolean up = true;

        String database = "up";
        double databaseLatencyMs = 0;
        try {
            long inicio = System.nanoTime();
            try (Connection conexion = dataSource.getConnection();
                 Statement stmt = conexion.createStatement()) {
                stmt.execute("SELECT 1");
            }
            databaseLatencyMs = latenciaMs(inicio);
        } catch (Exception e) {
            up = false;
            database = mensajeError(e);
            log.error("HealthCheck: MySQL no responde", e);
        }

        String redis = "up";
        double redisLatencyMs = 0;
        RedisConnection conexionRedis = null;
        try {
            long inicio = System.nanoTime();
            conexionRedis = RedisConnectionUtils.getConnection(redisConnectionFactory);
            conexionRedis.ping();
            redisLatencyMs = latenciaMs(inicio);
        } catch (Exception e) {
            up = false;
            redis = mensajeError(e);
            log.error("HealthCheck: Redis no responde", e);
        } finally {
            RedisConnectionUtils.releaseConnection(conexionRedis, redisConnectionFactory);
        }

        builder.withDetail("database", database)
                .withDetail("database_latency_ms", databaseLatencyMs)
                .withDetail("redis", redis)
                .withDetail("redis_latency_ms", redisLatencyMs);
        return up ? builder.up().build() : builder.down().build();
    }

    private double latenciaMs(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000.0;
    }

    private String mensajeError(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
```

- [ ] **Step 4: Ejecutar el test para verificar que pasa**

Run: `.\mvnw.cmd -q -Dtest=InfraestructuraHealthIndicatorTest test`
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/health/InfraestructuraHealthIndicator.java src/test/java/com/monteastur/envios/health/InfraestructuraHealthIndicatorTest.java
git commit -m "feat(health): add real MySQL and Redis health indicator with latency details"
```

---

### Task 6: BusinessMetrics y métricas en tracking + WebSocket

**Files:**
- Create: `src/main/java/com/monteastur/envios/metrics/BusinessMetrics.java`
- Create: `src/test/java/com/monteastur/envios/metrics/BusinessMetricsTest.java`
- Modify: `src/main/java/com/monteastur/envios/service/web/PublicTrackingService.java`
- Modify: `src/main/java/com/monteastur/envios/listener/WebSocketEventListener.java`
- Modify: `src/test/java/com/monteastur/envios/service/web/PublicTrackingServiceTest.java` (si existe; añadir mock de `BusinessMetrics`)
- Modify: `src/test/java/com/monteastur/envios/listener/WebSocketEventListenerTest.java`

**Interfaces:**
- Consumes: `MeterRegistry` (bean de Boot), `Timer.Sample`.
- Produces:
  - `BusinessMetrics` con:
    - `Timer.Sample iniciarBusqueda()`
    - `void registrarBusqueda(Timer.Sample sample, boolean encontrado)`
    - `Timer.Sample iniciarDifusion()`
    - `void registrarDifusion(Timer.Sample sample, boolean ok)`
  - Métricas: `envios.tracking.pagina` (Timer, tag `encontrado`), `envios.tracking.resultado` (Counter), `envios.websocket.difusion` (Timer, tag `resultado`), `envios.websocket.resultado` (Counter).
  - Constructores: `PublicTrackingService(..., BusinessMetrics)` y `WebSocketEventListener(SimpMessagingTemplate, BusinessMetrics)`.

- [ ] **Step 1: Escribir el test de `BusinessMetrics` (RED)**

Crear `src/test/java/com/monteastur/envios/metrics/BusinessMetricsTest.java`:

```java
package com.monteastur.envios.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.microestmeter.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessMetricsTest {

    private MeterRegistry registry;
    private BusinessMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new BusinessMetrics(registry);
    }

    @Test
    void buscarEncontrado_registraTimerYCounter() {
        Timer.Sample sample = metrics.iniciarBusqueda();
        metrics.registrarBusqueda(sample, true);

        assertThat(registry.get("envios.tracking.resultado").counter().count()).isEqualTo(1);
        Timer timer = registry.get("envios.tracking.pagina").tag("encontrado", "true").timer();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isGreaterThanOrEqualTo(0);
    }

    @Test
    void buscarNoEncontrado_registraTagFalse() {
        Timer.Sample sample = metrics.iniciarBusqueda();
        metrics.registrarBusqueda(sample, false);

        assertThat(registry.get("envios.tracking.resultado").counter().count()).isEqualTo(1);
        assertThat(registry.get("envios.tracking.pagina").tag("encontrado", "false").timer().count())
                .isEqualTo(1);
    }

    @Test
    void difundirOk_registraTimerYCounter() {
        Timer.Sample sample = metrics.iniciarDifusion();
        metrics.registrarDifusion(sample, true);

        assertThat(registry.get("envios.websocket.resultado").counter().count()).isEqualTo(1);
        assertThat(registry.get("envios.websocket.difusion").tag("resultado", "ok").timer().count())
                .isEqualTo(1);
    }

    @Test
    void difundirConError_registraResultadoError() {
        Timer.Sample sample = metrics.iniciarDifusion();
        metrics.registrarDifusion(sample, false);

        assertThat(registry.get("envios.websocket.resultado").counter().count()).isEqualTo(1);
        assertThat(registry.get("envios.websocket.difusion").tag("resultado", "error").timer().count())
                .isEqualTo(1);
    }
}
```

> **Corrección de typo en el import anterior:** el import correcto es `io.micrometer.core.instrument.simple.SimpleMeterRegistry` (con `micrometer`, no `microestmeter`). Copiar el import correcto.

- [ ] **Step 2: Ejecutar el test para verificar que falla**

Run: `.\mvnw.cmd -q -Dtest=BusinessMetricsTest test`
Expected: FAIL (compilación: `BusinessMetrics` no existe).

- [ ] **Step 3: Implementar `BusinessMetrics`**

Crear `src/main/java/com/monteastur/envios/metrics/BusinessMetrics.java`:

```java
package com.monteastur.envios.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Métricas de negocio de Micrometer para los servicios críticos del CMS.
 * API explícita (sin @Timed): quien consume los servicios inicia un
 * Timer.Sample y lo cierra aquí con el resultado.
 */
@Component
public class BusinessMetrics {

    private final Timer timerBusqueda;
    private final Counter counterBusqueda;
    private final Timer timerDifusion;
    private final Counter counterDifusion;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.timerBusqueda = Timer.builder("envios.tracking.pagina")
                .description("Tiempo de carga de la página pública de tracking")
                .tag("encontrado", "false")
                .register(meterRegistry);
        this.counterBusqueda = Counter.builder("envios.tracking.resultado")
                .description("Consultas de tracking completadas")
                .register(meterRegistry);
        this.timerDifusion = Timer.builder("envios.websocket.difusion")
                .description("Tiempo de difusión WebSocket de actualización de estado")
                .tag("resultado", "ok")
                .register(meterRegistry);
        this.counterDifusion = Counter.builder("envios.websocket.resultado")
                .description("Difusiones WebSocket completadas")
                .register(meterRegistry);
    }

    public Timer.Sample iniciarBusqueda() {
        return Timer.start();
    }

    public void registrarBusqueda(Timer.Sample sample, boolean encontrado) {
        sample.stop(timerBusqueda.tag("encontrado", Boolean.toString(encontrado)));
        counterBusqueda.increment();
    }

    public Timer.Sample iniciarDifusion() {
        return Timer.start();
    }

    public void registrarDifusion(Timer.Sample sample, boolean ok) {
        sample.stop(timerDifusion.tag("resultado", ok ? "ok" : "error"));
        counterDifusion.increment();
    }
}
```

- [ ] **Step 4: Integrar métricas en `PublicTrackingService`**

Reescribir `src/main/java/com/monteastur/envios/service/web/PublicTrackingService.java`:

```java
package com.monteastur.envios.service.web;

import com.monteastur.envios.dto.web.EvidenciaView;
import com.monteastur.envios.dto.web.EventoView;
import com.monteastur.envios.dto.web.EntregaView;
import com.monteastur.envios.dto.web.PublicTrackingView;
import com.monteastur.envios.metrics.BusinessMetrics;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de consulta pública de rastreo con caché Redis.
 * Devuelve un DTO plano (nunca entidades JPA). null si el código no existe.
 */
@Service
public class PublicTrackingService {

    private final EnvioTrackingRepository envioTrackingRepository;
    private final EventoTrackingService eventoTrackingService;
    private final EvidenciaEnvioService evidenciaEnvioService;
    private final EntregaEvidenciaRepository entregaEvidenciaRepository;
    private final BusinessMetrics businessMetrics;

    public PublicTrackingService(EnvioTrackingRepository envioTrackingRepository,
                                 EventoTrackingService eventoTrackingService,
                                 EvidenciaEnvioService evidenciaEnvioService,
                                 EntregaEvidenciaRepository entregaEvidenciaRepository,
                                 BusinessMetrics businessMetrics) {
        this.envioTrackingRepository = envioTrackingRepository;
        this.eventoTrackingService = eventoTrackingService;
        this.evidenciaEnvioService = evidenciaEnvioService;
        this.entregaEvidenciaRepository = entregaEvidenciaRepository;
        this.businessMetrics = businessMetrics;
    }

    @Cacheable(value = "envios.tracking.pagina", key = "#codigo", unless = "#result == null")
    public PublicTrackingView cargarPagina(String codigo) {
        Timer.Sample sample = businessMetrics.iniciarBusqueda();
        EnvioTracking envio = envioTrackingRepository
                .findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
                .orElse(null);
        if (envio == null) {
            businessMetrics.registrarBusqueda(sample, false);
            return null;
        }
        List<EventoView> eventos = eventoTrackingService.listarPorEnvio(envio.getId()).stream()
                .map(EventoView::from)
                .toList();
        List<EvidenciaView> evidencias = evidenciaEnvioService.listarPorEnvioParaCliente(envio.getId()).stream()
                .map(EvidenciaView::from)
                .toList();
        EntregaView entrega = null;
        if ("ENTREGADO".equals(envio.getEstado())) {
            entrega = entregaEvidenciaRepository.findByEnvioId(envio.getId())
                    .map(EntregaView::from)
                    .orElse(null);
        }
        PublicTrackingView resultado = PublicTrackingView.from(envio, eventos, evidencias, entrega);
        businessMetrics.registrarBusqueda(sample, true);
        return resultado;
    }
}
```

- [ ] **Step 5: Integrar métricas en `WebSocketEventListener`**

Reescribir `src/main/java/com/monteastur/envios/listener/WebSocketEventListener.java`:

```java
package com.monteastur.envios.listener;

import com.monteastur.envios.dto.websocket.EnvioEstadoWsMessage;
import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.metrics.BusinessMetrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneOffset;

/**
 * Escucha los eventos de actualización de estado de envíos y los difunde en
 * tiempo real a los clientes suscritos al topic WebSocket {@code /topic/envios}.
 * El evento transaccional nunca debe romper el flujo principal del cliente.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final BusinessMetrics businessMetrics;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate,
                                  BusinessMetrics businessMetrics) {
        this.messagingTemplate = messagingTemplate;
        this.businessMetrics = businessMetrics;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void manejar(EstadoEnvioActualizadoEvent event) {
        Timer.Sample sample = businessMetrics.iniciarDifusion();
        try {
            EnvioEstadoWsMessage mensaje = new EnvioEstadoWsMessage(
                    event.envioId(),
                    event.codigoRastreo(),
                    event.estadoNuevo(),
                    event.timestamp().toInstant(ZoneOffset.UTC));
            messagingTemplate.convertAndSend("/topic/envios", mensaje);
            businessMetrics.registrarDifusion(sample, true);
        } catch (Exception e) {
            businessMetrics.registrarDifusion(sample, false);
            log.error("Fallo al difundir actualización de estado del envío {}", event.codigoRastreo(), e);
        }
    }
}
```

- [ ] **Step 6: Actualizar tests existentes**

En `src/test/java/com/monteastur/envios/listener/WebSocketEventListenerTest.java`:
- Import: `com.monteastur.envios.metrics.BusinessMetrics`.
- `@Mock private BusinessMetrics businessMetrics;`
- `setUp()`: `listener = new WebSocketEventListener(messagingTemplate, businessMetrics);`

En `src/test/java/com/monteastur/envios/service/web/PublicTrackingServiceTest.java` (si existe):
- Import `com.monteastur.envios.metrics.BusinessMetrics`.
- `@Mock private BusinessMetrics businessMetrics;`
- El constructor de `PublicTrackingService` recibe `businessMetrics` como último argumento.
- (Si el test usa `@ExtendWith(MockitoExtension.class)`, el mock se inyecta solo; los métodos de `BusinessMetrics` son llamados pero no verificados, así que los stubs con `when` de los repos siguen igual.)

- [ ] **Step 7: Ejecutar los tests afectados**

Run: `.\mvnw.cmd -q -Dtest=BusinessMetricsTest,WebSocketEventListenerTest,PublicTrackingServiceTest test`
Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/monteastur/envios/metrics/BusinessMetrics.java src/test/java/com/monteastur/envios/metrics/BusinessMetricsTest.java src/main/java/com/monteastur/envios/service/web/PublicTrackingService.java src/main/java/com/monteastur/envios/listener/WebSocketEventListener.java src/test/java/com/monteastur/envios/service/web/PublicTrackingServiceTest.java src/test/java/com/monteastur/envios/listener/WebSocketEventListenerTest.java
git commit -m "feat(metrics): add BusinessMetrics and instrument public tracking and WebSocket broadcast"
```

---

### Task 7: Seguridad de Actuator (IP-allowlist)

**Files:**
- Modify: `src/main/java/com/monteastur/envios/config/SecurityConfig.java`
- Create: `src/test/java/com/monteastur/envios/integration/ActuatorSecurityIntegrationTest.java`

**Interfaces:**
- Consumes: `SecurityFilterChain` actual (Task 0 / FASE 5), credenciales `app.admin.username`/`app.admin.password`.
- Produces: reglas de autorización para `/actuator/**`.

- [x] **Step 1: Escribir el test de integración (RED)**

Crear `src/test/java/com/monteastur/envios/integration/ActuatorSecurityIntegrationTest.java`:

```java
package com.monteastur.envios.integration;

import com.monteastur.envios.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ActuatorSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcUserDetailsManager userDetailsManager;

    @MockBean
    private EmailService emailService;

    @Test
    void healthAbiertoSinAuth_devuelve200ConUp() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
    }

    @Test
    void infoAbiertoSinAuth_devuelve200() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/info"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void livenessYReadiness_devuelvenUp() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health/liveness"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health/readiness"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
    }

    @Test
    void healthInfraestructura_conAdmin_devuelveUpConDetalles() throws Exception {
        UserDetails admin = User.withUsername("admin")
                .password("admin123")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
        when(userDetailsManager.loadUserByUsername("admin")).thenReturn(admin);

        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health/infraestructura")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.components.infraestructura.details.database").exists());
    }

    @Test
    void prometheusDesdeIpNoPermitida_devuelve401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus")
                        .with(SecurityMockMvcRequestPostProcessors.remoteAddress("203.0.113.10")))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void prometheusDesdeIpDocker_devuelve200() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus")
                        .with(SecurityMockMvcRequestPostProcessors.remoteAddress("172.18.0.5")))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void metricsDesdeIpNoPermitida_devuelve401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/metrics")
                        .with(SecurityMockMvcRequestPostProcessors.remoteAddress("203.0.113.10")))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void metricsDesdeIpDocker_devuelve200() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/metrics")
                        .with(SecurityMockMvcRequestPostProcessors.remoteAddress("172.18.0.5")))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void actuatorRestoSinAdmin_devuelve401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void actuatorRestoConAdmin_devuelve200() throws Exception {
        UserDetails admin = User.withUsername("admin")
                .password("admin123")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
        when(userDetailsManager.loadUserByUsername("admin")).thenReturn(admin);

        mockMvc.perform(MockMvcRequestBuilders.get("/actuator")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
```

> Nota: `with(SecurityMockMvcRequestPostProcessors.remoteAddress(...))` NO existe en spring-security-test. El patrón correcto es un `RequestPostProcessor` propio que setea `request.setRemoteAddr(ip)`. Sustituir los usos de `remoteAddress` por:

```java
import org.springframework.test.web.servlet.request.RequestPostProcessor;

private RequestPostProcessor desdeIp(String ip) {
    return request -> {
        request.setRemoteAddr(ip);
        return request;
    };
}
```

Y usar `.with(desdeIp("203.0.113.10"))` / `.with(desdeIp("172.18.0.5"))` en los 4 tests de IP. (Esta es la implementación canónica: `MockHttpServletRequest.setRemoteAddr`.)

- [ ] **Step 2: Ejecutar el test para verificar que falla**

Run: `.\mvnw.cmd -q -Dtest=ActuatorSecurityIntegrationTest test`
Expected: FAIL — los endpoints protegidos aún no existen (los tests de `/actuator/prometheus` e IP devuelven 200 porque hoy todo queda permitido).

- [x] **Step 3: Implementar las reglas en `SecurityConfig`**

En `src/main/java/com/monteastur/envios/config/SecurityConfig.java`, dentro de `authorizeHttpRequests`, insertar **antes** de `anyRequest().permitAll()` (línea 39):

```java
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus", "/actuator/metrics/**")
                        .access("hasIpAddress('172.16.0.0/12') or hasIpAddress('10.0.0.0/8') or hasIpAddress('127.0.0.1') or hasIpAddress('::1')")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
```

Resultando:

```java
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/cliente", "/cliente/login", "/cliente/assets/**").permitAll()
                .requestMatchers("/admin/**", "/api/v1/admin/**", "/api/v1/deliveries/**").authenticated()
                .requestMatchers("/api/v1/docs", "/api/v1/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/cliente/**", "/api/v1/cliente/**").hasRole("CLIENTE")
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus", "/actuator/metrics/**")
                        .access("hasIpAddress('172.16.0.0/12') or hasIpAddress('10.0.0.0/8') or hasIpAddress('127.0.0.1') or hasIpAddress('::1')")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
```

> Nota de implementación: en la versión resuelta (Spring Security 6.5.11, Java 25) `AuthorizedUrl.access(String)` no compila. Se usó `new WebExpressionAuthorizationManager("hasIpAddress(...) or ...")` (org.springframework.security.web.access.expression), y `RestAuthenticationEntryPoint` devolvía 302 en vez de 401 para `/actuator` — se amplió su rama JSON a `uri.startsWith("/actuator")`. La aserción de health se ajustó a `$.details.database` (un componente aislado se sirve como `{status, details}`, no con envoltorio `components`). Adicionalmente `application-test.properties` añade `management.prometheus.metrics.export.enabled=true` porque `@SpringBootTest` desactiva la observabilidad vía `DisableObservabilityContextCustomizer`.

- [x] **Step 4: Ejecutar el test de integración**

Run: `.\mvnw.cmd -q -Dtest=ActuatorSecurityIntegrationTest test`
Expected: `BUILD SUCCESS` (los 9 tests en verde).

- [x] **Step 5: Ejecutar `SecurityConfigTest` para verificar que no se rompió**

Run: `.\mvnw.cmd -q -Dtest=SecurityConfigTest test`
Expected: `BUILD SUCCESS`.

- [x] **Step 6: Commit**

```bash
git add src/main/java/com/monteastur/envios/config/SecurityConfig.java src/test/java/com/monteastur/envios/integration/ActuatorSecurityIntegrationTest.java src/main/java/com/monteastur/envios/security/RestAuthenticationEntryPoint.java src/test/resources/application-test.properties
git commit -m "feat(security): restrict actuator prometheus and metrics to Docker and loopback IPs; health open; rest admin"
```

---

### Task 8: Exposición de métricas en Prometheus

**Files:**
- Create: `src/test/java/com/monteastur/envios/integration/PrometheusMetricsExposureTest.java`

**Interfaces:**
- Consumes: `BusinessMetrics` (Task 6), endpoint `/actuator/prometheus` (Task 7 + base properties).
- Produces: verificación de que las métricas de negocio y de JVM salen por Prometheus.

- [x] **Step 1: Escribir el test (RED)**

Crear `src/test/java/com/monteastur/envios/integration/PrometheusMetricsExposureTest.java`:

```java
package com.monteastur.envios.integration;

import com.monteastur.envios.metrics.BusinessMetrics;
import com.monteastur.envios.service.EmailService;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PrometheusMetricsExposureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusinessMetrics businessMetrics;

    @MockBean
    private EmailService emailService;

    @Test
    void prometheusExponeMetricasDeJvmYDeNegocio() throws Exception {
        Timer.Sample sample = businessMetrics.iniciarBusqueda();
        businessMetrics.registrarBusqueda(sample, true);

        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith("text/plain"))
                .andExpect(MockMvcResultMatchers.content().string(containsString("jvm_")));
        // Las métricas de negocio se registran al registrar la búsqueda:
        // envios_tracking_resultado_total y envios_tracking_pagina_seconds_*
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(MockMvcResultMatchers.content().string(containsString("envios_tracking_resultado_total")))
                .andExpect(MockMvcResultMatchers.content().string(containsString("envios_tracking_pagina_seconds")));
    }
}
```

- [x] **Step 2: Ejecutar el test**

Run: `.\mvnw.cmd -q -Dtest=PrometheusMetricsExposureTest test`
Expected: `BUILD SUCCESS` (el endpoint ya está expuesto y las métricas de negocio se registran al invocar `BusinessMetrics`).

> Nota: el `.with(...)` inline es un `RequestPostProcessor` válido (equivalente a `desdeIp("127.0.0.1")` del Task 7). La métrica `envios.tracking.pagina` solo se expone tras su primera muestra (micrometer no muestra timers vacíos); por eso el test invoca `registrarBusqueda` antes de consultar.

- [x] **Step 3: Commit**

```bash
git add src/test/java/com/monteastur/envios/integration/PrometheusMetricsExposureTest.java
git commit -m "test(metrics): verify business and JVM metrics exposed by prometheus endpoint"
```

---

### Task 9: Suite completa, handoff y cierre

**Files:**
- Modify: `docs/handoff.md`
- (Test) `src/test/java/com/monteastur/envios/integration/WebhookDispatchIntegrationTest.java` — opcional: añadir contador de llamadas al sink

**Interfaces:**
- Consumes: todo lo anterior.
- Produces: `BUILD SUCCESS` con la suite completa y estado documentado.

- [x] **Step 1: (Opcional pero recomendado) Añadir contador de intentos al test de integración de webhooks**

> Ya implementado en un commit anterior: `sinkCalls` (campo volatile, líneas 45/80/86/184) y `assertThat(sinkCalls).isEqualTo(3)` en `transicionConWebhookQueResponde500_registraFallo`.

En `WebhookDispatchIntegrationTest`:
- Añadir campo `private static volatile int sinkCalls;`
- En el handler del sink, tras `exchange.sendResponseHeaders(respondCode, -1);`, añadir `sinkCalls++;`
- En `@BeforeAll` tras `sink.start();`, `sinkCalls = 0;`
- En `transicionConWebhookQueResponde500_registraFallo`, antes del `await`, `sinkCalls = 0;` y tras el `await` añadir:
  `assertThat(sinkCalls).isEqualTo(3);`
- En los demás tests con 200, `sinkCalls` no se consulta (no rompe nada).

- [x] **Step 2: Ejecutar la suite completa**

Run: `.\mvnw.cmd clean test` (JDK 25 local, o el comando Docker de AGENTS.md con MySQL/Redis levantados)
Expected: `BUILD SUCCESS`, suite completa (421+ tests, con los nuevos: 9 de ActuatorSecurity, 1 de PrometheusExposure, 3 de InfraestructuraHealth, 4 de BusinessMetrics, 4 nuevos en WebhookDispatchServiceTest). — Verificado: **442 tests, 0 fallos, BUILD SUCCESS**.

- [x] **Step 3: Actualizar `docs/handoff.md`**

Añadir al estado de avance una entrada para la FASE 6 (fecha, commits principales, endpoints nuevos y verificación `mvn clean test` BUILD SUCCESS). — Hecho: entrada "FASE 6 — Observabilidad y Resiliencia" añadida (commits `96ef015`…`2ed968f`, endpoints `/actuator/health/infraestructura` y `/actuator/prometheus`, verificación 442 tests BUILD SUCCESS) y sección "Estado Git Actual" actualizada.

- [x] **Step 4: Commit final**

```bash
git add docs/handoff.md src/test/java/com/monteastur/envios/integration/WebhookDispatchIntegrationTest.java
git commit -m "docs: update handoff with FASE 6 observability and resilience state"
```

> Ejecutado: commit `e702e31` con `docs/handoff.md` (el test de webhooks ya estaba en commits anteriores).

- [ ] **Step 5: Pedir revisión de código**

Tras el commit final, solicitar `requesting-code-review` del trabajo completo antes de ofrecer push a `origin/main`.
