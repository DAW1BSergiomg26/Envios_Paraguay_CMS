package com.grupb2.casarural.dto.api;

import java.time.LocalDateTime;

public class EventoDto {

    private LocalDateTime fecha;
    private String descripcion;
    private String tipo;

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
