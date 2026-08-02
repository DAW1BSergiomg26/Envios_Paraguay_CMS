package com.monteastur.envios.service;

import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.BatchImportEstado;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.DocumentoGenerado;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.TipoDocumento;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.DocumentoGeneradoRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
import com.monteastur.envios.service.pdf.BarcodeService;
import com.monteastur.envios.service.pdf.EtiquetaPdfGenerator;
import com.monteastur.envios.service.pdf.ManifiestoPdfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoPdfServiceTest {

    @Mock private EnvioTrackingRepository envioTrackingRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private DocumentoGeneradoRepository documentoRepository;
    @Mock private BatchImportPersistenceService persistence;
    @Mock private EtiquetaPdfGenerator etiquetaGenerator;
    @Mock private ManifiestoPdfGenerator manifiestoGenerator;

    private DocumentoPdfService service;

    @BeforeEach
    void setUp() {
        service = new DocumentoPdfService(envioTrackingRepository, clienteRepository,
                documentoRepository, persistence, etiquetaGenerator, manifiestoGenerator,
                true, 5000, "http://localhost:8080/tracking");
    }

    private EnvioTracking envio() {
        return new EnvioTracking("MT-D1", "RECIBIDO", "Ana", "O", "D", "1 kg", "Docs");
    }

    @Test
    void generarEtiqueta_generaYAudita() {
        when(envioTrackingRepository.findByCodigoUnico("MT-D1")).thenReturn(Optional.of(envio()));
        when(etiquetaGenerator.generar(any(), any())).thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        byte[] pdf = service.generarEtiqueta("MT-D1", "admin");

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        ArgumentCaptor<DocumentoGenerado> captor = ArgumentCaptor.forClass(DocumentoGenerado.class);
        verify(documentoRepository).save(captor.capture());
        DocumentoGenerado audit = captor.getValue();
        assertThat(audit.getTipo()).isEqualTo(TipoDocumento.ETIQUETA_TERMICA);
        assertThat(audit.getReferenciaId()).isEqualTo("MT-D1");
        assertThat(audit.getPesoBytes()).isEqualTo(4);
        assertThat(audit.getUsuarioGeneracion()).isEqualTo("admin");
    }

    @Test
    void generarEtiqueta_envioInexistente_lanza404() {
        when(envioTrackingRepository.findByCodigoUnico("MT-NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generarEtiqueta("MT-NOPE", "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void generarEtiqueta_deshabilitado_lanza400() {
        DocumentoPdfService apagado = new DocumentoPdfService(envioTrackingRepository,
                clienteRepository, documentoRepository, persistence, etiquetaGenerator,
                manifiestoGenerator, false, 5000, "http://localhost:8080/tracking");

        assertThatThrownBy(() -> apagado.generarEtiqueta("MT-D1", "admin"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void generarEtiquetasLote_streamingYAudita() throws Exception {
        BatchImport lote = new BatchImport(null, "lote.csv", BatchImportEstado.COMPLETADO);
        lote.setId(9L);
        when(persistence.obtenerLote(9L)).thenReturn(lote);
        when(envioTrackingRepository.countByBatchId(9L)).thenReturn(2L);
        when(envioTrackingRepository.findByBatchIdOrderByCodigoUnicoAsc(9L))
                .thenReturn(List.of(envio()));

        DocumentoPdfService streaming = new DocumentoPdfService(envioTrackingRepository,
                clienteRepository, documentoRepository, persistence,
                new EtiquetaPdfGenerator(new BarcodeService()), manifiestoGenerator,
                true, 5000, "http://localhost:8080/tracking");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        streaming.generarEtiquetasLote(9L, "admin", out);

        assertThat(out.toByteArray()).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        ArgumentCaptor<DocumentoGenerado> captor = ArgumentCaptor.forClass(DocumentoGenerado.class);
        verify(documentoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoDocumento.ETIQUETAS_LOTE);
        assertThat(captor.getValue().getReferenciaId()).isEqualTo("9");
    }

    @Test
    void generarEtiquetasLote_superaMaxPages_lanza400() {
        when(persistence.obtenerLote(9L)).thenReturn(new BatchImport(null, "l", BatchImportEstado.COMPLETADO));
        when(envioTrackingRepository.countByBatchId(9L)).thenReturn(6000L);

        assertThatThrownBy(() -> service.generarEtiquetasLote(9L, "admin", new ByteArrayOutputStream()))
                .isInstanceOf(BadRequestException.class);
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void generarManifiesto_generaYAudita() {
        BatchImport lote = new BatchImport(5L, "lote.csv", BatchImportEstado.COMPLETADO);
        lote.setId(9L);
        when(persistence.obtenerLote(9L)).thenReturn(lote);
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(new Cliente("c@x.com", "p", "Cliente X", "+595")));
        when(envioTrackingRepository.findByBatchIdOrderByCodigoUnicoAsc(9L)).thenReturn(List.of(envio()));
        when(manifiestoGenerator.generar(anyLong(), anyList(), anyString()))
                .thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        byte[] pdf = service.generarManifiesto(9L, "admin");

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        verify(documentoRepository).save(any(DocumentoGenerado.class));
    }

    @Test
    void listarEmisiones_sinTipo_devuelveTodas() {
        when(documentoRepository.findByOrderByFechaCreacionDesc()).thenReturn(List.of());

        assertThat(service.listarEmisiones(null)).isEmpty();
        verify(documentoRepository, times(1)).findByOrderByFechaCreacionDesc();
    }
}
