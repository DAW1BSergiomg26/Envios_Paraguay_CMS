package com.monteastur.envios.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud de actualización de estado")
public class ActualizarEstadoRequest {
    @Schema(description = "Nuevo estado", example = "EN_TRANSITO",
        allowableValues = {"RECIBIDO", "EN_ADUANA_ORIGEN", "EN_TRANSITO", "EN_ADUANA_DESTINO", "EN_REPARTO", "ENTREGADO"})
    private String estado;

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
