package com.monteastur.envios.service.batch;

import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.BatchImportError;
import com.monteastur.envios.model.BatchImportEstado;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.BatchImportErrorRepository;
import com.monteastur.envios.repository.BatchImportRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BatchImportPersistenceService {

    private final BatchImportRepository batchImportRepository;
    private final BatchImportErrorRepository batchImportErrorRepository;
    private final EnvioTrackingRepository envioTrackingRepository;

    public BatchImportPersistenceService(BatchImportRepository batchImportRepository,
                                         BatchImportErrorRepository batchImportErrorRepository,
                                         EnvioTrackingRepository envioTrackingRepository) {
        this.batchImportRepository = batchImportRepository;
        this.batchImportErrorRepository = batchImportErrorRepository;
        this.envioTrackingRepository = envioTrackingRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchImport crearLote(Long clienteId, String nombreArchivo) {
        return batchImportRepository.save(new BatchImport(clienteId, nombreArchivo, BatchImportEstado.PENDIENTE));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarInicio(Long batchId) {
        BatchImport lote = obtenerLote(batchId);
        lote.setEstado(BatchImportEstado.EN_PROCESO);
        batchImportRepository.save(lote);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = {"envios.dashboard", "envios.tracking.pagina", "envios.cliente.dashboard"}, allEntries = true)
    public void procesarChunk(Long batchId, List<EnvioTracking> envios, List<CsvImportLineError> errores) {
        BatchImport lote = obtenerLote(batchId);
        lote.setProcesados(lote.getProcesados() + envios.size() + errores.size());
        lote.setExitosos(lote.getExitosos() + envios.size());
        lote.setFallidos(lote.getFallidos() + errores.size());
        batchImportRepository.save(lote);
        envios.forEach(envio -> envio.setBatchId(batchId));
        envioTrackingRepository.saveAll(envios);
        if (!errores.isEmpty()) {
            List<BatchImportError> erroresPersistidos = errores.stream()
                    .map(e -> new BatchImportError(batchId, e.getLineaNumero(),
                            e.getCodigoRastreo(), e.getErrorMensaje()))
                    .toList();
            batchImportErrorRepository.saveAll(erroresPersistidos);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "envios.dashboard", allEntries = true)
    public void finalizar(Long batchId, int totalRegistros, String errorResumen) {
        BatchImport lote = obtenerLote(batchId);
        lote.setTotalRegistros(totalRegistros);
        lote.setErrorResumen(errorResumen);
        lote.setEstado(lote.getFallidos() > 0 || errorResumen != null
                ? BatchImportEstado.COMPLETADO_CON_ERRORES : BatchImportEstado.COMPLETADO);
        lote.setFechaFin(LocalDateTime.now());
        batchImportRepository.save(lote);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFallido(Long batchId, String errorResumen) {
        BatchImport lote = obtenerLote(batchId);
        lote.setEstado(BatchImportEstado.FALLIDO);
        lote.setErrorResumen(errorResumen);
        lote.setFechaFin(LocalDateTime.now());
        batchImportRepository.save(lote);
    }

    public BatchImport obtenerLote(Long batchId) {
        return batchImportRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote de importación no encontrado: " + batchId));
    }

    public List<BatchImportError> listarErrores(Long batchId) {
        return batchImportErrorRepository.findByBatchIdOrderByLineaNumeroAsc(batchId);
    }

    public List<BatchImport> listarLotes() {
        return batchImportRepository.findAllByOrderByIdDesc();
    }
}
