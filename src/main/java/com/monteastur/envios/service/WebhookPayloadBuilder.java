package com.monteastur.envios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.model.EnvioTracking;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class WebhookPayloadBuilder {

    private final ObjectMapper objectMapper;

    public WebhookPayloadBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String construir(EstadoEnvioActualizadoEvent event, EnvioTracking envio, String baseUrl) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event_id", UUID.randomUUID().toString());
        payload.put("envio_id", event.envioId());
        payload.put("codigo_rastreo", event.codigoRastreo());
        payload.put("estado_anterior", event.estadoAnterior());
        payload.put("estado_nuevo", event.estadoNuevo());
        payload.put("timestamp", event.timestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        payload.put("url_seguimiento", baseUrl + "/" + event.codigoRastreo());
        payload.put("destinatario", envio.getDestinatario());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el payload del webhook", e);
        }
    }
}
