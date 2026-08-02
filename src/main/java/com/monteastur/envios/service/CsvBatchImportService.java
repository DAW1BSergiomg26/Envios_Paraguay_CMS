package com.monteastur.envios.service;

import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
import com.monteastur.envios.service.batch.CsvEnvioParser;
import com.monteastur.envios.service.batch.CsvImportLineError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CsvBatchImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvBatchImportService.class);

    private final CsvEnvioParser parser;
    private final BatchImportPersistenceService persistence;
    private final EnvioTrackingRepository envioTrackingRepository;
    private final ClienteRepository clienteRepository;
    private final int chunkSize;
    private final int maxRows;

    public CsvBatchImportService(CsvEnvioParser parser,
                                 BatchImportPersistenceService persistence,
                                 EnvioTrackingRepository envioTrackingRepository,
                                 ClienteRepository clienteRepository,
                                 @Value("${app.batch.chunk-size:100}") int chunkSize,
                                 @Value("${app.batch.max-rows:200000}") int maxRows) {
        this.parser = parser;
        this.persistence = persistence;
        this.envioTrackingRepository = envioTrackingRepository;
        this.clienteRepository = clienteRepository;
        this.chunkSize = chunkSize;
        this.maxRows = maxRows;
    }

    @Async("batchTaskExecutor")
    public void procesarLote(Long batchId, String rutaTemporal, Long clienteId) {
        Path fichero = Path.of(rutaTemporal);
        boolean excedeMaxRows = false;
        try {
            persistence.registrarInicio(batchId);
            Cliente cliente = clienteId != null
                    ? clienteRepository.findById(clienteId).orElse(null) : null;
            Set<String> codigosVistos = new HashSet<>();
            List<EnvioTracking> validos = new ArrayList<>(chunkSize);
            List<CsvImportLineError> errores = new ArrayList<>(chunkSize);
            int[] filasLeidas = {0};

            try (Reader reader = Files.newBufferedReader(fichero, StandardCharsets.UTF_8)) {
                try {
                    parser.procesar(reader, fila -> {
                        filasLeidas[0]++;
                        if (fila.esError()) {
                            errores.add(fila.getError());
                        } else {
                            EnvioTracking envio = fila.getEnvio();
                            String codigo = envio.getCodigoUnico().trim().toUpperCase();
                            if (codigosVistos.contains(codigo)
                                    || envioTrackingRepository.existsByCodigoUnico(codigo)) {
                                errores.add(new CsvImportLineError(fila.getLineaNumero(), codigo,
                                        "codigo duplicado"));
                            } else {
                                codigosVistos.add(codigo);
                                envio.setCodigoUnico(codigo);
                                if (cliente != null) {
                                    envio.setCliente(cliente);
                                }
                                validos.add(envio);
                            }
                        }
                        if (validos.size() + errores.size() >= chunkSize) {
                            vaciarChunk(batchId, validos, errores);
                        }
                        if (filasLeidas[0] >= maxRows) {
                            throw new MaxRowsExcedidoException();
                        }
                    });
                } catch (MaxRowsExcedidoException ex) {
                    excedeMaxRows = true;
                }
            }

            vaciarChunk(batchId, validos, errores);
            persistence.finalizar(batchId, filasLeidas[0],
                    excedeMaxRows ? "Se superó el límite máximo de " + maxRows + " filas" : null);
        } catch (Exception ex) {
            log.error("Lote {} falló: {}", batchId, ex.getMessage(), ex);
            persistence.marcarFallido(batchId, "Error procesando el lote: "
                    + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
        } finally {
            borrarTemporal(fichero);
        }
    }

    private void vaciarChunk(Long batchId, List<EnvioTracking> validos, List<CsvImportLineError> errores) {
        if (validos.isEmpty() && errores.isEmpty()) {
            return;
        }
        persistence.procesarChunk(batchId, List.copyOf(validos), List.copyOf(errores));
        validos.clear();
        errores.clear();
    }

    private void borrarTemporal(Path fichero) {
        try {
            Files.deleteIfExists(fichero);
        } catch (IOException ex) {
            log.warn("No se pudo borrar el fichero temporal {}: {}", fichero, ex.getMessage());
        }
    }

    private static final class MaxRowsExcedidoException extends RuntimeException {
        private MaxRowsExcedidoException() {
            super("Se superó el límite máximo de filas");
        }
    }
}
