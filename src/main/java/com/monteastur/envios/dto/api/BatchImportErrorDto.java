package com.monteastur.envios.dto.api;

import com.monteastur.envios.model.BatchImportError;

public class BatchImportErrorDto {

    private int lineaNumero;
    private String codigoRastreo;
    private String errorMensaje;

    public BatchImportErrorDto() {}

    public static BatchImportErrorDto from(BatchImportError error) {
        BatchImportErrorDto dto = new BatchImportErrorDto();
        dto.setLineaNumero(error.getLineaNumero());
        dto.setCodigoRastreo(error.getCodigoRastreo());
        dto.setErrorMensaje(error.getErrorMensaje());
        return dto;
    }

    public int getLineaNumero() { return lineaNumero; }
    public void setLineaNumero(int lineaNumero) { this.lineaNumero = lineaNumero; }
    public String getCodigoRastreo() { return codigoRastreo; }
    public void setCodigoRastreo(String codigoRastreo) { this.codigoRastreo = codigoRastreo; }
    public String getErrorMensaje() { return errorMensaje; }
    public void setErrorMensaje(String errorMensaje) { this.errorMensaje = errorMensaje; }
}
