package com.monteastur.envios.dto.analytics;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsSummaryDto {

    private List<KpiDto> kpis = new ArrayList<>();
    private List<EstadoCountDto> enviosPorEstado = new ArrayList<>();
    private List<TendenciaDto> tendencia = new ArrayList<>();
    private List<RutaDto> topRutas = new ArrayList<>();
    private List<WebhookPuntoDto> webhookPorDia = new ArrayList<>();
    private LocalDateTime generadoEn;

    public AnalyticsSummaryDto() {}

    public List<KpiDto> getKpis() { return kpis; }
    public void setKpis(List<KpiDto> kpis) { this.kpis = kpis; }
    public List<EstadoCountDto> getEnviosPorEstado() { return enviosPorEstado; }
    public void setEnviosPorEstado(List<EstadoCountDto> enviosPorEstado) { this.enviosPorEstado = enviosPorEstado; }
    public List<TendenciaDto> getTendencia() { return tendencia; }
    public void setTendencia(List<TendenciaDto> tendencia) { this.tendencia = tendencia; }
    public List<RutaDto> getTopRutas() { return topRutas; }
    public void setTopRutas(List<RutaDto> topRutas) { this.topRutas = topRutas; }
    public List<WebhookPuntoDto> getWebhookPorDia() { return webhookPorDia; }
    public void setWebhookPorDia(List<WebhookPuntoDto> webhookPorDia) { this.webhookPorDia = webhookPorDia; }
    public LocalDateTime getGeneradoEn() { return generadoEn; }
    public void setGeneradoEn(LocalDateTime generadoEn) { this.generadoEn = generadoEn; }
}
