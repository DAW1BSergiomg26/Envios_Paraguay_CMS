package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.analytics.AnalyticsSummaryDto;
import com.monteastur.envios.service.analytics.AnalyticsDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics BI", description = "Panel de Business Intelligence del CMS (requiere sesión admin o Basic Auth)")
@RestController
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AnalyticsRestController {

    private final AnalyticsDashboardService dashboardService;

    public AnalyticsRestController(AnalyticsDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Resumen de analítica",
            description = "KPIs y agregaciones del dashboard, cacheados en Redis (envios.analytics, TTL 2 min)")
    @GetMapping("/resumen")
    public ResponseEntity<AnalyticsSummaryDto> resumen() {
        return ResponseEntity.ok(dashboardService.resumen());
    }

    @Operation(summary = "Refrescar analítica",
            description = "Invalida la caché envios.analytics y devuelve datos recién calculados")
    @PostMapping("/refresh")
    public ResponseEntity<AnalyticsSummaryDto> refrescar() {
        dashboardService.refrescar();
        return ResponseEntity.ok(dashboardService.resumen());
    }
}
