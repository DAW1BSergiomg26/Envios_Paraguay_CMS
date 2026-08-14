package com.monteastur.envios.controller;

import com.monteastur.envios.dto.websocket.EnvioEstadoWsMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * Endpoint STOMP para la difusión de actualizaciones de estado de envíos.
 * Publica cada mensaje recibido en el topic público {@code /topic/envios}.
 */
@Controller
public class EnvioWebSocketController {

    @MessageMapping("/actualizar-estado")
    @SendTo("/topic/envios")
    public EnvioEstadoWsMessage actualizarEstado(EnvioEstadoWsMessage mensaje) {
        return mensaje;
    }
}
