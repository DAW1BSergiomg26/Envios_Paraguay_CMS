package com.monteastur.envios.dto.analytics;

public class RutaDto {

    private String origen;
    private String destino;
    private long cantidad;

    public RutaDto() {}

    public RutaDto(String origen, String destino, long cantidad) {
        this.origen = origen;
        this.destino = destino;
        this.cantidad = cantidad;
    }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public long getCantidad() { return cantidad; }
    public void setCantidad(long cantidad) { this.cantidad = cantidad; }
}
