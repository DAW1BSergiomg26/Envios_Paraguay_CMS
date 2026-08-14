package com.monteastur.envios.dto.websocket;

import java.time.Instant;

/**
 * Mensaje de actualización de estado de un envío difundido en tiempo real
 * por WebSocket a los clientes suscritos al topic público de envíos.
 * Java puro: atributos privados, constructores explícitos y getters/setters.
 */
public class EnvioEstadoWsMessage {

    private Long envioId;
    private String tracking;
    private String estado;
    private Instant timestamp;

    public EnvioEstadoWsMessage() {
    }

    public EnvioEstadoWsMessage(Long envioId, String tracking, String estado, Instant timestamp) {
        this.envioId = envioId;
        this.tracking = tracking;
        this.estado = estado;
        this.timestamp = timestamp;
    }

    public Long getEnvioId() {
        return envioId;
    }

    public void setEnvioId(Long envioId) {
        this.envioId = envioId;
    }

    public String getTracking() {
        return tracking;
    }

    public void setTracking(String tracking) {
        this.tracking = tracking;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
