package com.monteastur.envios.dto.api;

import com.monteastur.envios.model.EntregaEvidencia;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evidencia digital de entrega (POD)")
public class EntregaEvidenciaDto {

    private final Long id;
    private final String codigoRastreo;
    private final String receptorNombre;
    private final String receptorDocumento;
    private final String firmaBase64;
    private final Double latitud;
    private final Double longitud;
    private final String notas;
    private final String fechaEntrega;

    private EntregaEvidenciaDto(Long id, String codigoRastreo, String receptorNombre,
                                String receptorDocumento, String firmaBase64, Double latitud,
                                Double longitud, String notas, String fechaEntrega) {
        this.id = id;
        this.codigoRastreo = codigoRastreo;
        this.receptorNombre = receptorNombre;
        this.receptorDocumento = receptorDocumento;
        this.firmaBase64 = firmaBase64;
        this.latitud = latitud;
        this.longitud = longitud;
        this.notas = notas;
        this.fechaEntrega = fechaEntrega;
    }

    public static EntregaEvidenciaDto from(EntregaEvidencia evidencia) {
        return new EntregaEvidenciaDto(evidencia.getId(),
                evidencia.getEnvio().getCodigoUnico(),
                evidencia.getReceptorNombre(),
                evidencia.getReceptorDocumento(),
                evidencia.getFirmaBase64(),
                evidencia.getLatitud(),
                evidencia.getLongitud(),
                evidencia.getNotas(),
                evidencia.getFechaEntrega() != null ? evidencia.getFechaEntrega().toString() : null);
    }

    public Long getId() { return id; }
    public String getCodigoRastreo() { return codigoRastreo; }
    public String getReceptorNombre() { return receptorNombre; }
    public String getReceptorDocumento() { return receptorDocumento; }
    public String getFirmaBase64() { return firmaBase64; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
    public String getNotas() { return notas; }
    public String getFechaEntrega() { return fechaEntrega; }
}
