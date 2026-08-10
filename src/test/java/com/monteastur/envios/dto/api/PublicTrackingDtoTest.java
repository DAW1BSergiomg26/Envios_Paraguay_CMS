package com.monteastur.envios.dto.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PublicTrackingDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void serializaSoloLasCincoClavesPermitidas() throws Exception {
        PublicTrackingDto dto = new PublicTrackingDto();
        dto.setCodigoUnico("MT-2026-0001");
        dto.setEstado("en_transito");
        dto.setOrigen("Madrid");
        dto.setDestino("Asuncion");
        dto.setUltimaActualizacion(LocalDateTime.of(2026, 8, 7, 12, 30));

        String json = objectMapper.writeValueAsString(dto);
        Map<String, Object> claves = objectMapper.readValue(json, Map.class);

        assertThat(claves.keySet()).containsExactlyInAnyOrder(
                "codigoUnico", "estado", "origen", "destino", "ultimaActualizacion");
    }

    @Test
    void noSerializaCamposSensiblesDelEnvio() throws Exception {
        PublicTrackingDto dto = new PublicTrackingDto();
        dto.setCodigoUnico("MT-2026-0001");
        dto.setEstado("en_transito");
        dto.setOrigen("Madrid");
        dto.setDestino("Asuncion");
        dto.setUltimaActualizacion(LocalDateTime.now());

        String json = objectMapper.writeValueAsString(dto);
        Map<String, Object> claves = objectMapper.readValue(json, Map.class);

        Set<String> sensibles = Set.of("destinatario", "peso", "contenido", "cliente", "id");
        assertThat(claves.keySet()).doesNotContainAnyElementsOf(sensibles);
    }

    @Test
    void fromMapeaSoloLosCamposSegurosDelEnvio() throws Exception {
        PublicTrackingDto dto = new PublicTrackingDto();
        dto.setCodigoUnico("MT-2026-0001");
        dto.setEstado("en_transito");
        dto.setOrigen("Madrid");
        dto.setDestino("Asuncion");
        dto.setUltimaActualizacion(LocalDateTime.now());

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json)
                .contains("codigoUnico", "estado", "origen", "destino", "ultimaActualizacion")
                .doesNotContain("destinatario", "peso", "contenido");
    }
}
