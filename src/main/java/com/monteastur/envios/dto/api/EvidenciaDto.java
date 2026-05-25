package com.monteastur.envios.dto.api;

public class EvidenciaDto {

    private String titulo;
    private String descripcion;
    private String tipo;
    private String urlArchivo;
    private Boolean visibleCliente;

    // Getters y Setters
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
}
