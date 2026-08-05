package com.monteastur.envios.service.analytics;

import com.monteastur.envios.dto.analytics.EstadoCountDto;
import com.monteastur.envios.dto.analytics.KpiDto;
import com.monteastur.envios.dto.analytics.RutaDto;
import com.monteastur.envios.dto.analytics.TendenciaDto;
import com.monteastur.envios.dto.analytics.WebhookPuntoDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsQueryService {

    public static final int DIAS_TENDENCIA = 14;
    public static final int LIMITE_RUTAS = 5;

    private static final List<String> ESTADOS_EN_TRANSITO =
            List.of("EN_ADUANA_ORIGEN", "EN_TRANSITO", "EN_ADUANA_DESTINO", "EN_REPARTO");

    private static final String SQL_TOTAL = "SELECT COUNT(*) FROM envios_tracking";
    private static final String SQL_EN_TRANSITO =
            "SELECT COUNT(*) FROM envios_tracking WHERE estado IN (?, ?, ?, ?)";
    private static final String SQL_ENTREGADOS =
            "SELECT COUNT(*) FROM envios_tracking WHERE estado = 'ENTREGADO'";
    private static final String SQL_WEBHOOK_TOTAL = "SELECT COUNT(*) FROM webhook_logs";
    private static final String SQL_WEBHOOK_EXITOSOS =
            "SELECT COUNT(*) FROM webhook_logs WHERE exitoso = TRUE";
    private static final String SQL_RESERVAS_PENDIENTES =
            "SELECT COUNT(*) FROM reservas WHERE estado = 'pendiente'";
    private static final String SQL_POR_ESTADO =
            "SELECT estado, COUNT(*) AS cantidad FROM envios_tracking GROUP BY estado ORDER BY cantidad DESC, estado ASC";
    private static final String SQL_TENDENCIA =
            "SELECT DATE(fecha_creacion) AS d, COUNT(*) AS total FROM envios_tracking WHERE fecha_creacion >= ? GROUP BY DATE(fecha_creacion) ORDER BY d ASC";
    private static final String SQL_TOP_RUTAS =
            "SELECT COALESCE(origen, 'N/D') AS origen, COALESCE(destino, 'N/D') AS destino, COUNT(*) AS cantidad FROM envios_tracking GROUP BY origen, destino ORDER BY cantidad DESC, origen ASC LIMIT ?";
    private static final String SQL_WEBHOOK_POR_DIA =
            "SELECT DATE(fecha_creacion) AS d, SUM(exitoso) AS ok, COUNT(*) AS total FROM webhook_logs WHERE fecha_creacion >= ? GROUP BY DATE(fecha_creacion) ORDER BY d ASC";

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<KpiDto> kpis() {
        long total = count(SQL_TOTAL);
        long enTransito = count(SQL_EN_TRANSITO, ESTADOS_EN_TRANSITO.toArray());
        long entregados = count(SQL_ENTREGADOS);
        long totalWebhooks = count(SQL_WEBHOOK_TOTAL);
        long exitosos = count(SQL_WEBHOOK_EXITOSOS);
        long reservasPendientes = count(SQL_RESERVAS_PENDIENTES);
        double tasa = calcularTasa(exitosos, totalWebhooks);
        return List.of(
                new KpiDto("Total envíos", total, "#1B4D3B"),
                new KpiDto("En tránsito", enTransito, "#E67E22"),
                new KpiDto("Entregados", entregados, "#4ADE80"),
                new KpiDto("Éxito webhooks", tasa, "#153C2D"),
                new KpiDto("Reservas pendientes", reservasPendientes, "#E67E22")
        );
    }

    static double calcularTasa(long exitosos, long total) {
        if (total <= 0) {
            return 100.0;
        }
        return Math.round(exitosos * 1000.0 / total) / 10.0;
    }

    public List<EstadoCountDto> enviosPorEstado() {
        return jdbcTemplate.query(SQL_POR_ESTADO, (rs, rowNum) -> mapEstadoCount(rs));
    }

    static EstadoCountDto mapEstadoCount(ResultSet rs) throws SQLException {
        return new EstadoCountDto(rs.getString("estado"), rs.getLong("cantidad"));
    }

    public List<TendenciaDto> tendenciaUltimosDias(int dias) {
        LocalDate desde = LocalDate.now().minusDays(dias - 1L);
        Map<LocalDate, Long> porDia = new LinkedHashMap<>();
        jdbcTemplate.query(SQL_TENDENCIA, rs -> {
            porDia.put(rs.getDate("d").toLocalDate(), rs.getLong("total"));
        }, Timestamp.valueOf(desde.atStartOfDay()));
        return completarTendencia(porDia, desde, dias);
    }

    static List<TendenciaDto> completarTendencia(Map<LocalDate, Long> porDia, LocalDate desde, int dias) {
        List<TendenciaDto> resultado = new ArrayList<>(dias);
        for (int i = 0; i < dias; i++) {
            LocalDate d = desde.plusDays(i);
            resultado.add(new TendenciaDto(d, porDia.getOrDefault(d, 0L)));
        }
        return resultado;
    }

    public List<RutaDto> topRutas(int limite) {
        return jdbcTemplate.query(SQL_TOP_RUTAS, (rs, rowNum) -> mapRuta(rs), limite);
    }

    static RutaDto mapRuta(ResultSet rs) throws SQLException {
        return new RutaDto(rs.getString("origen"), rs.getString("destino"), rs.getLong("cantidad"));
    }

    public List<WebhookPuntoDto> webhookPorDia(int dias) {
        LocalDate desde = LocalDate.now().minusDays(dias - 1L);
        Map<LocalDate, WebhookPuntoDto> porDia = new LinkedHashMap<>();
        jdbcTemplate.query(SQL_WEBHOOK_POR_DIA, rs -> {
            LocalDate d = rs.getDate("d").toLocalDate();
            long ok = rs.getLong("ok");
            long total = rs.getLong("total");
            porDia.put(d, new WebhookPuntoDto(d, ok, total, calcularTasa(ok, total)));
        }, Timestamp.valueOf(desde.atStartOfDay()));
        return completarWebhook(porDia, desde, dias);
    }

    static List<WebhookPuntoDto> completarWebhook(Map<LocalDate, WebhookPuntoDto> porDia, LocalDate desde, int dias) {
        List<WebhookPuntoDto> resultado = new ArrayList<>(dias);
        for (int i = 0; i < dias; i++) {
            LocalDate d = desde.plusDays(i);
            resultado.add(porDia.getOrDefault(d, new WebhookPuntoDto(d, 0, 0, 0.0)));
        }
        return resultado;
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }
}
