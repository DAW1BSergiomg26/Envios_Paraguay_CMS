package com.grupb2.casarural.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_tracking")
public class EventoTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envio_id", nullable = false)
    private EnvioTracking envioTracking;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String ubicacion;

    private String icono;

    private String color;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    @Column(name = "creado_por")
    private String creadoPor;

    @Column(name = "visible_cliente", nullable = false)
    private boolean visibleCliente = true;

    public EventoTracking() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EnvioTracking getEnvioTracking() { return envioTracking; }
    public void setEnvioTracking(EnvioTracking envioTracking) { this.envioTracking = envioTracking; }
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
    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }
    public boolean isVisibleCliente() { return visibleCliente; }
    public void setVisibleCliente(boolean visibleCliente) { this.visibleCliente = visibleCliente; }
}
