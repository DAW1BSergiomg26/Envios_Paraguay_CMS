package com.monteastur.envios.service.analytics;

import com.monteastur.envios.dto.analytics.AnalyticsSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AnalyticsDashboardServiceTest.TestConfig.class)
class AnalyticsDashboardServiceTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("envios.analytics");
            cacheManager.setAllowNullValues(false);
            return cacheManager;
        }

        @Bean
        AnalyticsQueryService queryService() {
            return Mockito.mock(AnalyticsQueryService.class);
        }

        @Bean
        AnalyticsDashboardService dashboardService(AnalyticsQueryService qs) {
            return new AnalyticsDashboardService(qs);
        }
    }

    @Autowired
    private AnalyticsDashboardService dashboardService;

    @Autowired
    private AnalyticsQueryService queryService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetEstado() {
        Mockito.reset(queryService);
        Cache c = cacheManager.getCache("envios.analytics");
        if (c != null) {
            c.clear();
        }
    }

    @Test
    void resumen_quedaCacheadoYNoRepiteLasConsultas() {
        AnalyticsSummaryDto primero = dashboardService.resumen();
        AnalyticsSummaryDto segundo = dashboardService.resumen();

        assertThat(segundo).isSameAs(primero);
        verify(queryService, times(1)).kpis();
    }

    @Test
    void refrescar_invalidaLaCache() {
        dashboardService.resumen();
        dashboardService.refrescar();
        dashboardService.resumen();

        verify(queryService, times(2)).kpis();
    }
}
