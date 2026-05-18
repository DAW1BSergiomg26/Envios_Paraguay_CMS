package com.grupb2.casarural.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "envios_tracking")
public class EnvioTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_unico", nullable = false, unique = true)
    private String codigoUnico;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private String destinatario;

    private String origen;

    private String destino;

    private String peso;

    private String contenido;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "ultima_actualizacion", nullable = false)
    private LocalDateTime ultimaActualizacion;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    public EnvioTracking() {}

    public EnvioTracking(String codigoUnico, String estado, String destinatario,
                         String origen, String destino, String peso, String contenido) {
        this.codigoUnico = codigoUnico;
        this.estado = estado;
        this.destinatario = destinatario;
        this.origen = origen;
        this.destino = destino;
        this.peso = peso;
        this.contenido = contenido;
        this.fechaCreacion = LocalDateTime.now();
        this.ultimaActualizacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigoUnico() { return codigoUnico; }
    public void setCodigoUnico(String codigoUnico) { this.codigoUnico = codigoUnico; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getPeso() { return peso; }
    public void setPeso(String peso) { this.peso = peso; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getUbicacionActual() {
        if (estado == null) return "";
        return switch (estado) {
            case "RECIBIDO" -> "Asturias, España";
            case "EN_ADUANA_ORIGEN" -> "Puerto de Gijón";
            case "EN_TRANSITO" -> "Océano Atlántico";
            case "EN_ADUANA_DESTINO" -> "Aduana de Paraguay";
            case "EN_REPARTO" -> "Paraguay";
            case "ENTREGADO" -> "Destino final";
            default -> "";
        };
    }
}
