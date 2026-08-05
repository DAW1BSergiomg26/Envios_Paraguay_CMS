package com.monteastur.envios.dto.analytics;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsDtoSerializationTest {

    private static ObjectMapper cacheMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    }

    @Test
    void analyticsSummaryDto_roundTripConSerializerRedis() throws Exception {
        ObjectMapper mapper = cacheMapper();
        AnalyticsSummaryDto dto = new AnalyticsSummaryDto();
        dto.setKpis(List.of(new KpiDto("Total envíos", 42.0, "#1B4D3B")));
        dto.setEnviosPorEstado(List.of(new EstadoCountDto("ENTREGADO", 30L)));
        dto.setTendencia(List.of(new TendenciaDto(LocalDate.of(2026, 8, 1), 2L)));
        dto.setTopRutas(List.of(new RutaDto("Asturias", "Asunción", 18L)));
        dto.setWebhookPorDia(List.of(new WebhookPuntoDto(LocalDate.of(2026, 8, 1), 5L, 6L, 83.3)));
        dto.setGeneradoEn(LocalDateTime.of(2026, 8, 5, 10, 15));

        String json = mapper.writeValueAsString(dto);
        AnalyticsSummaryDto copia = mapper.readValue(json, AnalyticsSummaryDto.class);

        assertThat(copia.getKpis()).hasSize(1);
        assertThat(copia.getKpis().get(0).getLabel()).isEqualTo("Total envíos");
        assertThat(copia.getKpis().get(0).getValue()).isEqualTo(42.0);
        assertThat(copia.getKpis().get(0).getColor()).isEqualTo("#1B4D3B");
        assertThat(copia.getEnviosPorEstado().get(0).getCantidad()).isEqualTo(30L);
        assertThat(copia.getTendencia().get(0).getFecha()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(copia.getTopRutas().get(0).getDestino()).isEqualTo("Asunción");
        assertThat(copia.getWebhookPorDia().get(0).getTasaExito()).isEqualTo(83.3);
        assertThat(copia.getGeneradoEn()).isEqualTo(LocalDateTime.of(2026, 8, 5, 10, 15));
    }
}
