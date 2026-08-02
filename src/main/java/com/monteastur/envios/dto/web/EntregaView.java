package com.monteastur.envios.dto.web;

import com.monteastur.envios.model.EntregaEvidencia;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EntregaView {

    private String receptorNombre;
    private String receptorDocumento;
    private String firmaBase64;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String notas;
    private LocalDateTime fechaEntrega;

    public EntregaView() {}

    public EntregaView(String receptorNombre, String receptorDocumento, String firmaBase64,
                       BigDecimal latitud, BigDecimal longitud, String notas, LocalDateTime fechaEntrega) {
        this.receptorNombre = receptorNombre;
        this.receptorDocumento = receptorDocumento;
        this.firmaBase64 = firmaBase64;
        this.latitud = latitud;
        this.longitud = longitud;
        this.notas = notas;
        this.fechaEntrega = fechaEntrega;
    }

    public static EntregaView from(EntregaEvidencia evidencia) {
        return new EntregaView(evidencia.getReceptorNombre(), evidencia.getReceptorDocumento(),
                evidencia.getFirmaBase64(), evidencia.getLatitud(), evidencia.getLongitud(),
                evidencia.getNotas(), evidencia.getFechaEntrega());
    }

    public String getReceptorNombre() { return receptorNombre; }
    public void setReceptorNombre(String receptorNombre) { this.receptorNombre = receptorNombre; }
    public String getReceptorDocumento() { return receptorDocumento; }
    public void setReceptorDocumento(String receptorDocumento) { this.receptorDocumento = receptorDocumento; }
    public String getFirmaBase64() { return firmaBase64; }
    public void setFirmaBase64(String firmaBase64) { this.firmaBase64 = firmaBase64; }
    public BigDecimal getLatitud() { return latitud; }
    public void setLatitud(BigDecimal latitud) { this.latitud = latitud; }
    public BigDecimal getLongitud() { return longitud; }
    public void setLongitud(BigDecimal longitud) { this.longitud = longitud; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }
}
