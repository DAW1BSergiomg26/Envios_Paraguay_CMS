package com.monteastur.envios.integration;

import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.DocumentoGenerado;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.TipoDocumento;
import com.monteastur.envios.repository.BatchImportErrorRepository;
import com.monteastur.envios.repository.BatchImportRepository;
import com.monteastur.envios.repository.DocumentoGeneradoRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.DocumentoPdfService;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DocumentosIntegrationTest {

    @Autowired private DocumentoPdfService documentoPdfService;
    @Autowired private EnvioTrackingRepository envioTrackingRepository;
    @Autowired private BatchImportRepository batchImportRepository;
    @Autowired private BatchImportErrorRepository batchImportErrorRepository;
    @Autowired private DocumentoGeneradoRepository documentoRepository;
    @Autowired private BatchImportPersistenceService persistence;

    @MockBean private EmailService emailService;

    private final List<Long> batchIds = new ArrayList<>();
    private final List<Long> envioIds = new ArrayList<>();
    private final List<Long> documentoIds = new ArrayList<>();

    @AfterEach
    void limpiar() {
        documentoIds.forEach(documentoRepository::deleteById);
        documentoIds.clear();
        batchIds.forEach(batchId -> {
            batchImportErrorRepository.deleteAll(batchImportErrorRepository.findByBatchIdOrderByLineaNumeroAsc(batchId));
            batchImportRepository.deleteById(batchId);
        });
        batchIds.clear();
        envioIds.forEach(envioTrackingRepository::deleteById);
        envioIds.clear();
    }

    private EnvioTracking guardarEnvio(String codigo, String peso) {
        EnvioTracking envio = new EnvioTracking(codigo, "RECIBIDO", "Ana Test",
                "Asturias", "Asunción", peso, "Documentos");
        EnvioTracking guardado = envioTrackingRepository.save(envio);
        envioIds.add(guardado.getId());
        return guardado;
    }

    private BatchImport crearLote() {
        BatchImport lote = persistence.crearLote(null, "lote-int.csv");
        batchIds.add(lote.getId());
        return lote;
    }

    @Test
    void generaEtiqueta_retornaPdfYAudita() {
        guardarEnvio("MT-INT-01", "1.5 kg");

        byte[] pdf = documentoPdfService.generarEtiqueta("MT-INT-01", "admin");

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        List<DocumentoGenerado> emisiones = documentoRepository.findByOrderByFechaCreacionDesc();
        DocumentoGenerado audit = emisiones.stream()
                .filter(d -> d.getReferenciaId().equals("MT-INT-01"))
                .findFirst().orElseThrow();
        assertThat(audit.getTipo()).isEqualTo(TipoDocumento.ETIQUETA_TERMICA);
        assertThat(audit.getPesoBytes()).isEqualTo(pdf.length);
        assertThat(audit.getUsuarioGeneracion()).isEqualTo("admin");
        documentoIds.add(audit.getId());
    }

    @Test
    void generaEtiqueta_envioInexistente_lanza404() {
        assertThatThrownBy(() -> documentoPdfService.generarEtiqueta("MT-NO-EXISTE", "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generaEtiquetasDeLote_streamingYAudita() throws Exception {
        BatchImport lote = crearLote();
        EnvioTracking e1 = guardarEnvio("MT-INT-B1", "1 kg");
        EnvioTracking e2 = guardarEnvio("MT-INT-B2", "2 kg");
        e1.setBatchId(lote.getId());
        e2.setBatchId(lote.getId());
        envioTrackingRepository.saveAll(List.of(e1, e2));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        documentoPdfService.generarEtiquetasLote(lote.getId(), "admin", out);

        assertThat(out.toByteArray()).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        DocumentoGenerado audit = documentoRepository.findByOrderByFechaCreacionDesc().stream()
                .filter(d -> d.getTipo() == TipoDocumento.ETIQUETAS_LOTE
                        && d.getReferenciaId().equals(String.valueOf(lote.getId())))
                .findFirst().orElseThrow();
        assertThat(audit.getPesoBytes()).isGreaterThan(0);
        documentoIds.add(audit.getId());
    }

    @Test
    void generaManifiesto_retornaPdfYAudita() {
        BatchImport lote = crearLote();
        EnvioTracking e1 = guardarEnvio("MT-INT-M1", "1,5 kg");
        EnvioTracking e2 = guardarEnvio("MT-INT-M2", "2 kg");
        e1.setBatchId(lote.getId());
        e2.setBatchId(lote.getId());
        envioTrackingRepository.saveAll(List.of(e1, e2));

        byte[] pdf = documentoPdfService.generarManifiesto(lote.getId(), "admin");

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        String contenido = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(contenido).contains("3.50");
        DocumentoGenerado audit = documentoRepository.findByOrderByFechaCreacionDesc().stream()
                .filter(d -> d.getTipo() == TipoDocumento.MANIFIESTO_CARGA
                        && d.getReferenciaId().equals(String.valueOf(lote.getId())))
                .findFirst().orElseThrow();
        assertThat(audit.getPesoBytes()).isEqualTo(pdf.length);
        documentoIds.add(audit.getId());
    }
}
