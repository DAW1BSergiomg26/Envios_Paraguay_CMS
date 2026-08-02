package com.monteastur.envios.dto.web;

import com.monteastur.envios.model.EnvioTracking;
import java.time.LocalDateTime;

public class EnvioResumenView {

    private Long id;
    private String codigoUnico;
    private String estado;
    private String destino;
    private String contenido;
    private String peso;
    private LocalDateTime ultimaActualizacion;
    private Long batchId;

    public EnvioResumenView() {}

    public EnvioResumenView(Long id, String codigoUnico, String estado, String destino,
                            String contenido, String peso, LocalDateTime ultimaActualizacion, Long batchId) {
        this.id = id;
        this.codigoUnico = codigoUnico;
        this.estado = estado;
        this.destino = destino;
        this.contenido = contenido;
        this.peso = peso;
        this.ultimaActualizacion = ultimaActualizacion;
        this.batchId = batchId;
    }

    public static EnvioResumenView from(EnvioTracking envio) {
        return new EnvioResumenView(envio.getId(), envio.getCodigoUnico(), envio.getEstado(),
                envio.getDestino(), envio.getContenido(), envio.getPeso(),
                envio.getUltimaActualizacion(), envio.getBatchId());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigoUnico() { return codigoUnico; }
    public void setCodigoUnico(String codigoUnico) { this.codigoUnico = codigoUnico; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getPeso() { return peso; }
    public void setPeso(String peso) { this.peso = peso; }
    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
}
