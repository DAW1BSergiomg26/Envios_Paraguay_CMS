package com.monteastur.envios.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.model.EnvioTracking;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookPayloadBuilderTest {

    private final WebhookPayloadBuilder builder = new WebhookPayloadBuilder(new ObjectMapper());

    @Test
    void serializaLosCamposNormalizados() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 2, 10, 30, 0);
        EstadoEnvioActualizadoEvent event =
                new EstadoEnvioActualizadoEvent(42L, "MT-2026-1", "RECIBIDO", "EN_TRANSITO", timestamp);
        EnvioTracking envio = new EnvioTracking("MT-2026-1", "EN_TRANSITO", "Ana García",
                "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos");

        String json = builder.construir(event, envio, "http://localhost:8080/tracking");

        JsonNode node = new ObjectMapper().readTree(json);
        assertThat(node.path("event_id").asText()).isNotBlank();
        assertThat(node.path("envio_id").asLong()).isEqualTo(42L);
        assertThat(node.path("codigo_rastreo").asText()).isEqualTo("MT-2026-1");
        assertThat(node.path("estado_anterior").asText()).isEqualTo("RECIBIDO");
        assertThat(node.path("estado_nuevo").asText()).isEqualTo("EN_TRANSITO");
        assertThat(node.path("timestamp").asText()).isEqualTo("2026-08-02T10:30:00");
        assertThat(node.path("url_seguimiento").asText())
                .isEqualTo("http://localhost:8080/tracking/MT-2026-1");
        assertThat(node.path("destinatario").asText()).isEqualTo("Ana García");
    }
}
