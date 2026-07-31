package com.monteastur.envios.event;

import java.time.LocalDateTime;

public record EstadoEnvioActualizadoEvent(
        Long envioId,
        String codigoRastreo,
        String estadoAnterior,
        String estadoNuevo,
        LocalDateTime timestamp) {

    public EstadoEnvioActualizadoEvent(Long envioId, String codigoRastreo,
                                       String estadoAnterior, String estadoNuevo) {
        this(envioId, codigoRastreo, estadoAnterior, estadoNuevo, LocalDateTime.now());
    }
}
