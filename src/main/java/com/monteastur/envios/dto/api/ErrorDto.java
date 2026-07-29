package com.monteastur.envios.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Respuesta de error estándar de la API")
public class ErrorDto {
    @Schema(description = "Marca temporal del error (ISO 8601)", example = "2026-07-29T12:00:00Z")
    private String timestamp;

    @Schema(description = "Código de estado HTTP", example = "404")
    private int status;

    @Schema(description = "Mensaje descriptivo del error", example = "Recurso no encontrado")
    private String error;

    public ErrorDto(String timestamp, int status, String error) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
    }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
