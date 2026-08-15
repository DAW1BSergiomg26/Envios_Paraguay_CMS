package com.monteastur.envios.service;

import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.exception.WebhookDispatchException;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.WebhookConfig;
import com.monteastur.envios.model.WebhookLog;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.WebhookConfigRepository;
import com.monteastur.envios.repository.WebhookLogRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
     * sobre la llamada HTTP: el Retry envuelve al CircuitBreaker, de modo que cada
     * intento individual pasa por el breaker (cuando está abierto se corta el intento
     * con {@link CallNotPermittedException}) y el bucle de reintentos decide cuántos
     * intentos se permiten. Solo los fallos transitorios (5xx / timeout / error de red,
     * transportados como {@link WebhookDispatchException}) se registran en el breaker y
     * se reintentan con backoff exponencial. Las respuestas 4xx no se reintentan ni
     * cuentan para el breaker: se auditan como fallo por intento y el método retorna
     * sin propagar excepción.
     * Nota: la invocación es interna (auto-llamada), por lo que la resiliencia se
     * resuelve aquí con decoradores programáticos en lugar de anotaciones AOP.
     */
    public void despacharIndividual(WebhookConfig config, EstadoEnvioActualizadoEvent event, String payload) {
        String firma = WebhookSignature.hmacSha256(config.getSecretToken(), payload);
        Integer status = null;
        boolean exitoso = false;
        String error = null;
        AtomicInteger intentos = new AtomicInteger();
        try {
            Supplier<Integer> llamadaResiliente = Retry.decorateSupplier(webhookRetry,
                    CircuitBreaker.decorateSupplier(webhookCircuitBreaker, () -> {
                        intentos.incrementAndGet();
                        return enviar(config, payload, firma);
                    }));
            status = llamadaResiliente.get();
            exitoso = status >= 200 && status < 300;
            if (!exitoso) {
                error = "HTTP " + status;
            }
            log.info("Webhook {} -> HTTP {} ({})", config.getId(), status, exitoso ? "OK" : "fallo");
        } catch (WebhookDispatchException e) {
            // Fallo transitorio agotado: se transporta el número de intentos realizados
            // y se propaga para que despachar audite el resultado final.
            throw new WebhookDispatchException(
                    "Fallo transitorio al enviar webhook " + config.getId() + ": " + e.getMessage()
                            + " tras " + intentos.get() + " intento(s)",
                    null, intentos.get(), e);
        } catch (CallNotPermittedException e) {
            throw new WebhookDispatchException(
                    "CircuitBreaker abierto: despacho de webhook " + config.getId() + " bloqueado en contingencia",
                    null, null, e);
        } catch (RestClientResponseException e) {
            status = e.getStatusCode().value();
            error = "HTTP " + status;
            log.warn("Webhook {} respondió HTTP {}: {}", config.getId(), status, config.getUrl());
        } catch (Exception e) {
            error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("Fallo de red en webhook {} -> {}: {}", config.getId(), config.getUrl(), error);
        }
        webhookLogRepository.save(new WebhookLog(config.getId(), event.envioId(), payload, status, exitoso, error));
    }

    private int enviar(WebhookConfig config, String payload, String firma) {
        try {
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
        } catch (RestClientResponseException e) {
            // Los 5xx se traducen a WebhookDispatchException para que Retry y el
            // CircuitBreaker los consideren reintentables; los 4xx se propagan sin
            // reintento y sin registrarse en el breaker (se auditan en el intento).
            if (e.getStatusCode().value() >= 500) {
                throw new WebhookDispatchException("HTTP " + e.getStatusCode().value(),
                        e.getStatusCode().value(), null, e);
            }
            throw e;
        } catch (ResourceAccessException e) {
            throw new WebhookDispatchException("connect timed out", null, null, e);
        }
    }
}
