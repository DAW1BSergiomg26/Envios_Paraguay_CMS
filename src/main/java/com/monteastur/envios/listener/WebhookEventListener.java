package com.monteastur.envios.listener;

import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.service.WebhookDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WebhookEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventListener.class);

    private final WebhookDispatchService dispatchService;

    @Value("${app.webhook.enabled:true}")
    private boolean webhooksHabilitados;

    public WebhookEventListener(WebhookDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Async("webhookTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void manejar(EstadoEnvioActualizadoEvent event) {
        if (!webhooksHabilitados) {
            log.info("Webhooks deshabilitados. Se omite el evento de {}", event.codigoRastreo());
            return;
        }
        try {
            dispatchService.despachar(event);
        } catch (Exception e) {
            log.error("Fallo al despachar webhooks para {}", event.codigoRastreo(), e);
        }
    }
}
