package com.monteastur.envios.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud para marcar un mensaje como leído o no leído")
public class MarcarLeidoRequest {
    @Schema(description = "Nuevo estado de lectura del mensaje", example = "true")
    private Boolean leido;

    public Boolean getLeido() { return leido; }
    public void setLeido(Boolean leido) { this.leido = leido; }
}
