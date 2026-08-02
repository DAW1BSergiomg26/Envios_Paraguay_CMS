package com.monteastur.envios.service;

import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.WebhookConfig;
import com.monteastur.envios.model.WebhookLog;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.WebhookConfigRepository;
import com.monteastur.envios.repository.WebhookLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
public class WebhookDispatchService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatchService.class);

    private static final String CABECERA_FIRMA = "X-Signature-256";

    private final EnvioTrackingRepository envioTrackingRepository;
    private final WebhookConfigRepository webhookConfigRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final WebhookPayloadBuilder payloadBuilder;
    private final RestClient webhookRestClient;

    @Value("${app.webhook.tracking.base-url:http://localhost:8080/tracking}")
    private String baseUrl;

    public WebhookDispatchService(EnvioTrackingRepository envioTrackingRepository,
                                  WebhookConfigRepository webhookConfigRepository,
                                  WebhookLogRepository webhookLogRepository,
                                  WebhookPayloadBuilder payloadBuilder,
                                  RestClient webhookRestClient) {
        this.envioTrackingRepository = envioTrackingRepository;
        this.webhookConfigRepository = webhookConfigRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.payloadBuilder = payloadBuilder;
        this.webhookRestClient = webhookRestClient;
    }

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
            despacharIndividual(config, event, payload);
        }
    }

    private void despacharIndividual(WebhookConfig config, EstadoEnvioActualizadoEvent event, String payload) {
        String firma = WebhookSignature.hmacSha256(config.getSecretToken(), payload);
        Integer status = null;
        boolean exitoso = false;
        String error = null;
        try {
            var response = webhookRestClient.post()
                    .uri(config.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(CABECERA_FIRMA, firma)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            status = response.getStatusCode().value();
            exitoso = status >= 200 && status < 300;
            if (!exitoso) {
                error = "HTTP " + status;
            }
            log.info("Webhook {} -> HTTP {} ({})", config.getId(), status, exitoso ? "OK" : "fallo");
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
}
