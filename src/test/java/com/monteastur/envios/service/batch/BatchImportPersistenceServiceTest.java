package com.monteastur.envios.service.batch;

import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.BatchImportEstado;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.BatchImportErrorRepository;
import com.monteastur.envios.repository.BatchImportRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchImportPersistenceServiceTest {

    @Mock private BatchImportRepository batchImportRepository;
    @Mock private BatchImportErrorRepository batchImportErrorRepository;
    @Mock private EnvioTrackingRepository envioTrackingRepository;

    private BatchImportPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new BatchImportPersistenceService(batchImportRepository,
                batchImportErrorRepository, envioTrackingRepository);
    }

    @Test
    void procesarChunk_asignaBatchIdAEnvios() {
        BatchImport lote = new BatchImport(null, "lote.csv", BatchImportEstado.EN_PROCESO);
        lote.setId(7L);
        when(batchImportRepository.findById(7L)).thenReturn(Optional.of(lote));

        EnvioTracking e1 = new EnvioTracking("MT-P1", "RECIBIDO", "Ana", "O", "D", "1 kg", "Docs");
        EnvioTracking e2 = new EnvioTracking("MT-P2", "RECIBIDO", "Luis", "O", "D", "2 kg", "Docs");

        service.procesarChunk(7L, List.of(e1, e2), List.of());

        assertThat(e1.getBatchId()).isEqualTo(7L);
        assertThat(e2.getBatchId()).isEqualTo(7L);
        verify(envioTrackingRepository).saveAll(List.of(e1, e2));
    }

    @Test
    void listarLotes_delegaEnRepositorioOrdenado() {
        BatchImport lote = new BatchImport(1L, "envios.csv", BatchImportEstado.COMPLETADO);
        when(batchImportRepository.findAllByOrderByIdDesc()).thenReturn(List.of(lote));

        assertThat(service.listarLotes()).containsExactly(lote);
        verify(batchImportRepository).findAllByOrderByIdDesc();
    }
}
