package com.monteastur.envios.service.batch;

import com.monteastur.envios.model.EnvioTracking;

public final class CsvEnvioRow {

    private final int lineaNumero;
    private final EnvioTracking envio;
    private final CsvImportLineError error;

    private CsvEnvioRow(int lineaNumero, EnvioTracking envio, CsvImportLineError error) {
        this.lineaNumero = lineaNumero;
        this.envio = envio;
        this.error = error;
    }

    public static CsvEnvioRow valida(EnvioTracking envio, int lineaNumero) {
        return new CsvEnvioRow(lineaNumero, envio, null);
    }

    public static CsvEnvioRow conError(CsvImportLineError error) {
        return new CsvEnvioRow(error.getLineaNumero(), null, error);
    }

    public boolean esError() {
        return error != null;
    }

    public int getLineaNumero() {
        return lineaNumero;
    }

    public EnvioTracking getEnvio() {
        return envio;
    }

    public CsvImportLineError getError() {
        return error;
    }
}
