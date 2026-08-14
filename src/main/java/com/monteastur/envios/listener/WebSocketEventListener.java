package com.monteastur.envios.listener;

import com.monteastur.envios.dto.websocket.EnvioEstadoWsMessage;
import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
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

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void manejar(EstadoEnvioActualizadoEvent event) {
        try {
            EnvioEstadoWsMessage mensaje = new EnvioEstadoWsMessage(
                    event.envioId(),
                    event.codigoRastreo(),
                    event.estadoNuevo(),
                    event.timestamp().toInstant(ZoneOffset.UTC));
            messagingTemplate.convertAndSend("/topic/envios", mensaje);
        } catch (Exception e) {
            log.error("Fallo al difundir actualización de estado del envío {}", event.codigoRastreo(), e);
        }
    }
}
