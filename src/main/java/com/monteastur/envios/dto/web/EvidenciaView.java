package com.monteastur.envios.dto.web;

import com.monteastur.envios.model.EvidenciaEnvio;
import java.time.LocalDateTime;

public class EvidenciaView {

    private Long id;
    private String titulo;
    private String descripcion;
    private String tipo;
    private String urlArchivo;
    private LocalDateTime fechaSubida;

    public EvidenciaView() {}

    public EvidenciaView(Long id, String titulo, String descripcion, String tipo,
                         String urlArchivo, LocalDateTime fechaSubida) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.urlArchivo = urlArchivo;
        this.fechaSubida = fechaSubida;
    }

    public static EvidenciaView from(EvidenciaEnvio evidencia) {
        return new EvidenciaView(evidencia.getId(), evidencia.getTitulo(), evidencia.getDescripcion(),
                evidencia.getTipo(), evidencia.getUrlArchivo(), evidencia.getFechaSubida());
    }

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
    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }
}
