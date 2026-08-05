package com.monteastur.envios.dto.analytics;

import java.time.LocalDate;

public class WebhookPuntoDto {

    private LocalDate fecha;
    private long exitosos;
    private long total;
    private double tasaExito;

    public WebhookPuntoDto() {}

    public WebhookPuntoDto(LocalDate fecha, long exitosos, long total, double tasaExito) {
        this.fecha = fecha;
        this.exitosos = exitosos;
        this.total = total;
        this.tasaExito = tasaExito;
    }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public long getExitosos() { return exitosos; }
    public void setExitosos(long exitosos) { this.exitosos = exitosos; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public double getTasaExito() { return tasaExito; }
    public void setTasaExito(double tasaExito) { this.tasaExito = tasaExito; }
}
