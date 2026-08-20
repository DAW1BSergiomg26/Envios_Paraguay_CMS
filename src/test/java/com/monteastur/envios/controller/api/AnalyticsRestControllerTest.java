package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.dto.analytics.AnalyticsSummaryDto;
import com.monteastur.envios.dto.analytics.KpiDto;
import com.monteastur.envios.dto.analytics.TendenciaDto;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.analytics.AnalyticsDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsRestController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test",
    "app.upload.dir=./uploads"
})
class AnalyticsRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsDashboardService dashboardService;

    @MockitoBean private RBACAccessLogger rbacAccessLogger;
    @MockitoBean private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockitoBean private DataSource dataSource;

    private AnalyticsSummaryDto resumenEjemplo() {
        AnalyticsSummaryDto dto = new AnalyticsSummaryDto();
        dto.setKpis(List.of(new KpiDto("Total envíos", 42.0, "#1B4D3B")));
        dto.setTendencia(List.of(new TendenciaDto(LocalDate.of(2026, 8, 1), 2L)));
        dto.setGeneradoEn(LocalDateTime.of(2026, 8, 5, 10, 15));
        return dto;
    }

    @Test
    void resumen_sinAutenticar_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/resumen"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resumen_conAdmin_devuelve200YJson() throws Exception {
        when(dashboardService.resumen()).thenReturn(resumenEjemplo());

        mockMvc.perform(get("/api/v1/admin/analytics/resumen").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis[0].label").value("Total envíos"))
                .andExpect(jsonPath("$.kpis[0].value").value(42.0))
                .andExpect(jsonPath("$.tendencia[0].fecha").value("2026-08-01"))
                .andExpect(jsonPath("$.generadoEn").exists());
    }

    @Test
    void refresh_conAdmin_devuelve200YReevalua() throws Exception {
        when(dashboardService.resumen()).thenReturn(resumenEjemplo());

        mockMvc.perform(post("/api/v1/admin/analytics/refresh").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis").isArray());

        verify(dashboardService).refrescar();
        verify(dashboardService).resumen();
    }
}
