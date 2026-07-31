package com.monteastur.envios.dto.api;

import com.monteastur.envios.model.EnvioTracking;

import java.time.LocalDateTime;

public class PublicTrackingDto {

    private String codigoUnico;
    private String estado;
    private String origen;
    private String destino;
    private LocalDateTime ultimaActualizacion;

    public static PublicTrackingDto from(EnvioTracking envio) {
        PublicTrackingDto dto = new PublicTrackingDto();
        dto.setCodigoUnico(envio.getCodigoUnico());
        dto.setEstado(envio.getEstado());
        dto.setOrigen(envio.getOrigen());
        dto.setDestino(envio.getDestino());
        dto.setUltimaActualizacion(envio.getUltimaActualizacion());
        return dto;
    }

    public String getCodigoUnico() {
        return codigoUnico;
    }

    public void setCodigoUnico(String codigoUnico) {
        this.codigoUnico = codigoUnico;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public LocalDateTime getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }
}
