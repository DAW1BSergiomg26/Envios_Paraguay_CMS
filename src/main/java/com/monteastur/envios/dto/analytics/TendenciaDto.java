package com.monteastur.envios.dto.analytics;

import java.time.LocalDate;

public class TendenciaDto {

    private LocalDate fecha;
    private long total;

    public TendenciaDto() {}

    public TendenciaDto(LocalDate fecha, long total) {
        this.fecha = fecha;
        this.total = total;
    }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
}
