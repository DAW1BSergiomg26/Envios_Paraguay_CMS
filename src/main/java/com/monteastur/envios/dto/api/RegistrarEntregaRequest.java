package com.monteastur.envios.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Solicitud de registro de evidencia de entrega (POD)")
public class RegistrarEntregaRequest {

    @Schema(description = "Nombre completo del receptor", example = "Ana López")
    private String receptorNombre;

    @Schema(description = "Documento del receptor (DNI/CI)", example = "12345678")
    private String receptorDocumento;

    @Schema(description = "Firma manuscrita codificada en Base64 (PNG)")
    private String firmaBase64;

    @Schema(description = "Latitud de la entrega (opcional)", example = "-25.2637421")
    private BigDecimal latitud;

    @Schema(description = "Longitud de la entrega (opcional)", example = "-57.575926")
    private BigDecimal longitud;

    @Schema(description = "Notas o incidencias de entrega (opcional)")
    private String notas;

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
}
