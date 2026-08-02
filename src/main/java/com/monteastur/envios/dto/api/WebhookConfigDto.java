package com.monteastur.envios.dto.api;

import com.monteastur.envios.model.WebhookConfig;

import java.time.LocalDateTime;

public class WebhookConfigDto {

    private Long id;

    private Long clienteId;

    private String url;

    private boolean activo;

    private LocalDateTime fechaCreacion;

    public WebhookConfigDto() {}

    public static WebhookConfigDto from(WebhookConfig config) {
        WebhookConfigDto dto = new WebhookConfigDto();
        dto.setId(config.getId());
        dto.setClienteId(config.getClienteId());
        dto.setUrl(config.getUrl());
        dto.setActivo(config.isActivo());
        dto.setFechaCreacion(config.getFechaCreacion());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
