package com.monteastur.envios.service.analytics;

import com.monteastur.envios.dto.analytics.AnalyticsSummaryDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AnalyticsDashboardService {

    private final AnalyticsQueryService queryService;

    public AnalyticsDashboardService(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @Cacheable(value = "envios.analytics", unless = "#result == null")
    public AnalyticsSummaryDto resumen() {
        AnalyticsSummaryDto dto = new AnalyticsSummaryDto();
        dto.setKpis(queryService.kpis());
        dto.setEnviosPorEstado(queryService.enviosPorEstado());
        dto.setTendencia(queryService.tendenciaUltimosDias(AnalyticsQueryService.DIAS_TENDENCIA));
        dto.setTopRutas(queryService.topRutas(AnalyticsQueryService.LIMITE_RUTAS));
        dto.setWebhookPorDia(queryService.webhookPorDia(AnalyticsQueryService.DIAS_TENDENCIA));
        dto.setGeneradoEn(LocalDateTime.now());
        return dto;
    }

    @CacheEvict(value = "envios.analytics", allEntries = true)
    public void refrescar() {
        // Solo invalida la caché; la siguiente llamada a resumen() recalcula.
    }
}
