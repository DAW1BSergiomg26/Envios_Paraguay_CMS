package com.monteastur.envios.service.batch;

public final class CsvImportLineError {

    private final int lineaNumero;
    private final String codigoRastreo;
    private final String errorMensaje;

    public CsvImportLineError(int lineaNumero, String codigoRastreo, String errorMensaje) {
        this.lineaNumero = lineaNumero;
        this.codigoRastreo = codigoRastreo;
        this.errorMensaje = errorMensaje;
    }

    public int getLineaNumero() {
        return lineaNumero;
    }

    public String getCodigoRastreo() {
        return codigoRastreo;
    }

    public String getErrorMensaje() {
        return errorMensaje;
    }
}
