package com.monteastur.envios.dto.web;

import com.monteastur.envios.model.EventoTracking;
import java.time.LocalDateTime;

public class EventoView {

    private String estado;
    private String titulo;
    private String descripcion;
    private String ubicacion;
    private String icono;
    private String color;
    private LocalDateTime fechaEvento;
    private boolean visibleCliente;

    public EventoView() {}

    public EventoView(String estado, String titulo, String descripcion, String ubicacion,
                      String icono, String color, LocalDateTime fechaEvento, boolean visibleCliente) {
        this.estado = estado;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.icono = icono;
        this.color = color;
        this.fechaEvento = fechaEvento;
        this.visibleCliente = visibleCliente;
    }

    public static EventoView from(EventoTracking evento) {
        return new EventoView(evento.getEstado(), evento.getTitulo(), evento.getDescripcion(),
                evento.getUbicacion(), evento.getIcono(), evento.getColor(),
                evento.getFechaEvento(), evento.isVisibleCliente());
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDateTime fechaEvento) { this.fechaEvento = fechaEvento; }
    public boolean isVisibleCliente() { return visibleCliente; }
    public void setVisibleCliente(boolean visibleCliente) { this.visibleCliente = visibleCliente; }
}
