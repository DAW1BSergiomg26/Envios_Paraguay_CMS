package com.monteastur.envios.service.analytics;

import com.monteastur.envios.dto.analytics.EstadoCountDto;
import com.monteastur.envios.dto.analytics.KpiDto;
import com.monteastur.envios.dto.analytics.RutaDto;
import com.monteastur.envios.dto.analytics.TendenciaDto;
import com.monteastur.envios.dto.analytics.WebhookPuntoDto;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsQueryServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AnalyticsQueryService service = new AnalyticsQueryService(jdbcTemplate);

    @Test
    void kpis_devuelveCincoKpisConTasaRedondeada() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM envios_tracking"), eq(Long.class), any(Object[].class)))
                .thenReturn(10L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM envios_tracking WHERE estado IN (?, ?, ?, ?)"), eq(Long.class), any(Object[].class)))
                .thenReturn(4L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM envios_tracking WHERE estado = 'ENTREGADO'"), eq(Long.class), any(Object[].class)))
                .thenReturn(3L);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM webhook_logs"), eq(Long.class), any(Object[].class)))
                .thenReturn(8L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM webhook_logs WHERE exitoso = TRUE"), eq(Long.class), any(Object[].class)))
                .thenReturn(6L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM reservas WHERE estado = 'pendiente'"), eq(Long.class), any(Object[].class)))
                .thenReturn(2L);

        List<KpiDto> kpis = service.kpis();

        assertThat(kpis).hasSize(5);
        assertThat(kpis.get(0).getLabel()).isEqualTo("Total envíos");
        assertThat(kpis.get(0).getValue()).isEqualTo(10.0);
        assertThat(kpis.get(1).getValue()).isEqualTo(4.0);
        assertThat(kpis.get(2).getValue()).isEqualTo(3.0);
        assertThat(kpis.get(3).getValue()).isEqualTo(75.0);
        assertThat(kpis.get(4).getValue()).isEqualTo(2.0);
    }

    @Test
    void calcularTasa_redondeaUnDecimalYSinLogsEsCien() {
        assertThat(AnalyticsQueryService.calcularTasa(6, 8)).isEqualTo(75.0);
        assertThat(AnalyticsQueryService.calcularTasa(5, 6)).isEqualTo(83.3);
        assertThat(AnalyticsQueryService.calcularTasa(0, 0)).isEqualTo(100.0);
    }

    @Test
    void mapEstadoCount_mapeaColumnas() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("estado")).thenReturn("ENTREGADO");
        when(rs.getLong("cantidad")).thenReturn(7L);

        EstadoCountDto dto = AnalyticsQueryService.mapEstadoCount(rs);

        assertThat(dto.getEstado()).isEqualTo("ENTREGADO");
        assertThat(dto.getCantidad()).isEqualTo(7L);
    }

    @Test
    void mapRuta_mapeaConFallbackND() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("origen")).thenReturn("N/D");
        when(rs.getString("destino")).thenReturn("Asunción");
        when(rs.getLong("cantidad")).thenReturn(3L);

        RutaDto dto = AnalyticsQueryService.mapRuta(rs);

        assertThat(dto.getOrigen()).isEqualTo("N/D");
        assertThat(dto.getDestino()).isEqualTo("Asunción");
        assertThat(dto.getCantidad()).isEqualTo(3L);
    }

    @Test
    void completarTendencia_rellenaCerosLosDiasSinDatos() {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        Map<LocalDate, Long> datos = new LinkedHashMap<>();
        datos.put(desde.plusDays(2), 5L);

        List<TendenciaDto> tendencia = AnalyticsQueryService.completarTendencia(datos, desde, 5);

        assertThat(tendencia).hasSize(5);
        assertThat(tendencia.get(0).getTotal()).isZero();
        assertThat(tendencia.get(2).getTotal()).isEqualTo(5L);
        assertThat(tendencia.get(4).getFecha()).isEqualTo(desde.plusDays(4));
    }

    @Test
    void completarWebhook_rellenaPuntosSinLogs() {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        Map<LocalDate, WebhookPuntoDto> datos = new LinkedHashMap<>();
        datos.put(desde, new WebhookPuntoDto(desde, 2, 3, 66.7));

        List<WebhookPuntoDto> puntos = AnalyticsQueryService.completarWebhook(datos, desde, 3);

        assertThat(puntos).hasSize(3);
        assertThat(puntos.get(0).getExitosos()).isEqualTo(2L);
        assertThat(puntos.get(1).getTotal()).isZero();
        assertThat(puntos.get(1).getTasaExito()).isZero();
    }
}
