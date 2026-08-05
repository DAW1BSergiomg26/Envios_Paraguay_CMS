package com.monteastur.envios.dto.analytics;

public class EstadoCountDto {

    private String estado;
    private long cantidad;

    public EstadoCountDto() {}

    public EstadoCountDto(String estado, long cantidad) {
        this.estado = estado;
        this.cantidad = cantidad;
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public long getCantidad() { return cantidad; }
    public void setCantidad(long cantidad) { this.cantidad = cantidad; }
}
