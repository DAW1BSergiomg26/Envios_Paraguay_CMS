package com.monteastur.envios.service.batch;

@FunctionalInterface
public interface CsvLineConsumer {

    void aceptar(CsvEnvioRow fila);
}
