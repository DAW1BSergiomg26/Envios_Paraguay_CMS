package com.monteastur.envios.dto.api;

import java.time.LocalDateTime;

public class EvidenciaDto {

    private Long id;
    private String titulo;
    private String descripcion;
    private String tipo;
    private String urlArchivo;
    private Boolean visibleCliente;
    private LocalDateTime fechaSubida;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getUrlArchivo() { return urlArchivo; }
    public void setUrlArchivo(String urlArchivo) { this.urlArchivo = urlArchivo; }
    public Boolean getVisibleCliente() { return visibleCliente; }
    public void setVisibleCliente(Boolean visibleCliente) { this.visibleCliente = visibleCliente; }
    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }
}
