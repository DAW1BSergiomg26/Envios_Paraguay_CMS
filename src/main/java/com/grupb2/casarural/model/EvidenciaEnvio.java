package com.grupb2.casarural.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evidencias_envio")
public class EvidenciaEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envio_id", nullable = false)
    private EnvioTracking envioTracking;

    @Column(nullable = false)
    private String titulo;

    private String descripcion;

    @Column(nullable = false)
    private String tipo;

    @Column(name = "url_archivo", nullable = false)
    private String urlArchivo;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "visible_cliente")
    private boolean visibleCliente = true;

    public EvidenciaEnvio() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EnvioTracking getEnvioTracking() { return envioTracking; }
    public void setEnvioTracking(EnvioTracking envioTracking) { this.envioTracking = envioTracking; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getUrlArchivo() { return urlArchivo; }
    public void setUrlArchivo(String urlArchivo) { this.urlArchivo = urlArchivo; }
    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }
    public boolean isVisibleCliente() { return visibleCliente; }
    public void setVisibleCliente(boolean visibleCliente) { this.visibleCliente = visibleCliente; }
}
