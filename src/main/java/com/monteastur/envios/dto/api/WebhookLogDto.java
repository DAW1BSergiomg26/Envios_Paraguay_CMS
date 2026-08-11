package com.monteastur.envios.dto.api;

import com.monteastur.envios.model.WebhookLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Registro de despacho de un webhook (sin payload por seguridad)")
public class WebhookLogDto {

    @Schema(description = "Identificador del registro", example = "50")
    private Long id;

    @Schema(description = "Identificador del webhook configurado", example = "10")
    private Long webhookId;

    @Schema(description = "Identificador del envío que disparó el webhook", example = "1")
    private Long envioId;

    @Schema(description = "Estado HTTP devuelto por el receptor", example = "200")
    private Integer responseStatus;

    @Schema(description = "Indica si el despacho fue exitoso", example = "true")
    private boolean exitoso;

    @Schema(description = "Mensaje de error en despachos fallidos")
    private String errorMensaje;

    @Schema(description = "Fecha del despacho", example = "2026-07-29T12:00:00")
    private LocalDateTime fechaCreacion;

    public WebhookLogDto() {}

    public static WebhookLogDto from(WebhookLog log) {
        WebhookLogDto dto = new WebhookLogDto();
        dto.setId(log.getId());
        dto.setWebhookId(log.getWebhookId());
        dto.setEnvioId(log.getEnvioId());
        dto.setResponseStatus(log.getResponseStatus());
        dto.setExitoso(log.isExitoso());
        dto.setErrorMensaje(log.getErrorMensaje());
        dto.setFechaCreacion(log.getFechaCreacion());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWebhookId() { return webhookId; }
    public void setWebhookId(Long webhookId) { this.webhookId = webhookId; }
    public Long getEnvioId() { return envioId; }
    public void setEnvioId(Long envioId) { this.envioId = envioId; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public boolean isExitoso() { return exitoso; }
    public void setExitoso(boolean exitoso) { this.exitoso = exitoso; }
    public String getErrorMensaje() { return errorMensaje; }
    public void setErrorMensaje(String errorMensaje) { this.errorMensaje = errorMensaje; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
