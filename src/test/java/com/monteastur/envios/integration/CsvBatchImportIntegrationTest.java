package com.monteastur.envios.integration;

import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.BatchImportEstado;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.BatchImportErrorRepository;
import com.monteastur.envios.repository.BatchImportRepository;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.CsvBatchImportService;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class CsvBatchImportIntegrationTest {

    @Autowired
    private CsvBatchImportService csvBatchImportService;

    @Autowired
    private BatchImportPersistenceService persistence;

    @Autowired
    private BatchImportRepository batchImportRepository;

    @Autowired
    private BatchImportErrorRepository batchImportErrorRepository;

    @Autowired
    private EnvioTrackingRepository envioTrackingRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @MockBean
    private EmailService emailService;

    private final List<Long> batchIds = new ArrayList<>();
    private final List<String> codigosCreados = new ArrayList<>();
    private Long clienteId;

    @AfterEach
    void limpiar() {
        batchIds.forEach(batchId -> {
            batchImportErrorRepository.deleteAll(batchImportErrorRepository.findByBatchIdOrderByLineaNumeroAsc(batchId));
            batchImportRepository.deleteById(batchId);
        });
        batchIds.clear();
        codigosCreados.forEach(codigo -> envioTrackingRepository.findByCodigoUnico(codigo)
                .ifPresent(envio -> envioTrackingRepository.deleteById(envio.getId())));
        codigosCreados.clear();
        if (clienteId != null) {
            clienteRepository.deleteById(clienteId);
            clienteId = null;
        }
    }

    private String cabecera() {
        return "codigo,estado,destinatario,origen,destino,peso,contenido,observaciones\n";
    }

    private Path escribirCsv(String contenido) throws Exception {
        Path fichero = Files.createTempFile("importe-int", ".csv");
        Files.writeString(fichero, contenido, StandardCharsets.UTF_8);
        return fichero;
    }

    private void esperarEstado(Long batchId, BatchImportEstado estado) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(
                batchImportRepository.findById(batchId).orElseThrow().getEstado()).isEqualTo(estado));
    }

    @Test
    void loteValido_completaYPersiste() throws Exception {
        Path fichero = escribirCsv(cabecera()
                + "MT-B1-01,RECIBIDO,María López,Asturias,Asunción,5 kg,Documentos,Paquete frágil\n"
                + "MT-B1-02,ENTREGADO,Pedro Ramírez,,,2 kg,,Entrega confirmada\n");
        codigosCreados.add("MT-B1-01");
        codigosCreados.add("MT-B1-02");

        BatchImport lote = persistence.crearLote(null, "lote.csv");
        batchIds.add(lote.getId());
        csvBatchImportService.procesarLote(lote.getId(), fichero.toString(), null);

        esperarEstado(lote.getId(), BatchImportEstado.COMPLETADO);
        BatchImport guardado = batchImportRepository.findById(lote.getId()).orElseThrow();
        assertThat(guardado.getTotalRegistros()).isEqualTo(2);
        assertThat(guardado.getExitosos()).isEqualTo(2);
        assertThat(guardado.getFallidos()).isEqualTo(0);
        assertThat(guardado.getProcesados()).isEqualTo(2);
        assertThat(envioTrackingRepository.findByCodigoUnico("MT-B1-01")).isPresent();
        assertThat(envioTrackingRepository.findByCodigoUnico("MT-B1-02")).isPresent();
        assertThat(Files.exists(fichero)).isFalse();
    }

    @Test
    void loteConErroresParciales_completaConErrores() throws Exception {
        Path fichero = escribirCsv(cabecera()
                + "MT-B2-01,RECIBIDO,María,,,,\n"
                + "MT-B2-02,ESTADO_INVALIDO,María,,,,\n"
                + "MT-B2-03,RECIBIDO, ,,,\n");
        codigosCreados.add("MT-B2-01");
        codigosCreados.add("MT-B2-03");

        BatchImport lote = persistence.crearLote(null, "lote.csv");
        batchIds.add(lote.getId());
        csvBatchImportService.procesarLote(lote.getId(), fichero.toString(), null);

        esperarEstado(lote.getId(), BatchImportEstado.COMPLETADO_CON_ERRORES);
        BatchImport guardado = batchImportRepository.findById(lote.getId()).orElseThrow();
        assertThat(guardado.getTotalRegistros()).isEqualTo(3);
        assertThat(guardado.getExitosos()).isEqualTo(1);
        assertThat(guardado.getFallidos()).isEqualTo(2);
        var errores = batchImportErrorRepository.findByBatchIdOrderByLineaNumeroAsc(lote.getId());
        assertThat(errores).hasSize(2);
        assertThat(errores.get(0).getLineaNumero()).isEqualTo(3);
        assertThat(errores.get(0).getCodigoRastreo()).isEqualTo("MT-B2-02");
        assertThat(errores.get(1).getLineaNumero()).isEqualTo(4);
    }

    @Test
    void loteConDuplicados_auditaLineas() throws Exception {
        EnvioTracking preexistente = new EnvioTracking("MT-B3-01", "RECIBIDO", "Existente",
                "Origen", "Destino", "1 kg", "Docs");
        envioTrackingRepository.save(preexistente);
        codigosCreados.add("MT-B3-01");

        Path fichero = escribirCsv(cabecera()
                + "MT-B3-01,RECIBIDO,María,,,,\n"
                + "MT-B3-02,RECIBIDO,María,,,,\n"
                + "MT-B3-02,ENTREGADO,Pedro,,,,\n");
        codigosCreados.add("MT-B3-02");

        BatchImport lote = persistence.crearLote(null, "lote.csv");
        batchIds.add(lote.getId());
        csvBatchImportService.procesarLote(lote.getId(), fichero.toString(), null);

        esperarEstado(lote.getId(), BatchImportEstado.COMPLETADO_CON_ERRORES);
        BatchImport guardado = batchImportRepository.findById(lote.getId()).orElseThrow();
        assertThat(guardado.getExitosos()).isEqualTo(1);
        assertThat(guardado.getFallidos()).isEqualTo(2);
        var errores = batchImportErrorRepository.findByBatchIdOrderByLineaNumeroAsc(lote.getId());
        assertThat(errores).hasSize(2);
        assertThat(errores).allSatisfy(e -> assertThat(e.getErrorMensaje()).contains("duplicado"));
    }

    @Test
    void ficheroInexistente_marcaFallido() {
        BatchImport lote = persistence.crearLote(null, "no-existe.csv");
        batchIds.add(lote.getId());

        csvBatchImportService.procesarLote(lote.getId(), "C:/ruta/inexistente.csv", null);

        esperarEstado(lote.getId(), BatchImportEstado.FALLIDO);
        BatchImport guardado = batchImportRepository.findById(lote.getId()).orElseThrow();
        assertThat(guardado.getErrorResumen()).isNotBlank();
    }

    @Test
    void loteConMasFilasQueMaxRows_seCorta() throws Exception {
        Path fichero = escribirCsv(cabecera()
                + "MT-B4-01,RECIBIDO,María,,,,\n"
                + "MT-B4-02,RECIBIDO,María,,,,\n"
                + "MT-B4-03,RECIBIDO,María,,,,\n"
                + "MT-B4-04,RECIBIDO,María,,,,\n");
        codigosCreados.add("MT-B4-01");
        codigosCreados.add("MT-B4-02");

        ReflectionTestUtils.setField(csvBatchImportService, "maxRows", 2);
        BatchImport lote = persistence.crearLote(null, "lote.csv");
        batchIds.add(lote.getId());
        try {
            csvBatchImportService.procesarLote(lote.getId(), fichero.toString(), null);
            esperarEstado(lote.getId(), BatchImportEstado.COMPLETADO_CON_ERRORES);
            BatchImport guardado = batchImportRepository.findById(lote.getId()).orElseThrow();
            assertThat(guardado.getTotalRegistros()).isEqualTo(2);
            assertThat(guardado.getErrorResumen()).contains("máximo");
        } finally {
            ReflectionTestUtils.setField(csvBatchImportService, "maxRows", 200000);
        }
    }

    @Test
    void loteConCliente_asociaEnviosAlCliente() throws Exception {
        Cliente cliente = new Cliente("batch-" + System.nanoTime() + "@example.com",
                "password123", "Cliente Batch", "+595 111 222");
        clienteRepository.save(cliente);
        clienteId = cliente.getId();

        Path fichero = escribirCsv(cabecera() + "MT-B5-01,RECIBIDO,María,,,,\n");
        codigosCreados.add("MT-B5-01");

        BatchImport lote = persistence.crearLote(clienteId, "lote.csv");
        batchIds.add(lote.getId());
        csvBatchImportService.procesarLote(lote.getId(), fichero.toString(), clienteId);

        esperarEstado(lote.getId(), BatchImportEstado.COMPLETADO);
        EnvioTracking envio = envioTrackingRepository.findByCodigoUnico("MT-B5-01").orElseThrow();
        assertThat(envio.getCliente()).isNotNull();
        assertThat(envio.getCliente().getId()).isEqualTo(clienteId);
    }

    @Test
    void lote_vinculaEnviosPersistidos() throws Exception {
        Path fichero = escribirCsv(cabecera()
                + "MT-B6-01,RECIBIDO,María,,,,\n"
                + "MT-B6-02,RECIBIDO,Pedro,,,,\n");
        codigosCreados.add("MT-B6-01");
        codigosCreados.add("MT-B6-02");

        BatchImport lote = persistence.crearLote(null, "lote.csv");
        batchIds.add(lote.getId());
        csvBatchImportService.procesarLote(lote.getId(), fichero.toString(), null);

        esperarEstado(lote.getId(), BatchImportEstado.COMPLETADO);
        EnvioTracking e1 = envioTrackingRepository.findByCodigoUnico("MT-B6-01").orElseThrow();
        EnvioTracking e2 = envioTrackingRepository.findByCodigoUnico("MT-B6-02").orElseThrow();
        assertThat(e1.getBatchId()).isEqualTo(lote.getId());
        assertThat(e2.getBatchId()).isEqualTo(lote.getId());
        assertThat(envioTrackingRepository.findByBatchIdOrderByCodigoUnicoAsc(lote.getId()))
                .extracting(EnvioTracking::getCodigoUnico)
                .containsExactly("MT-B6-01", "MT-B6-02");
    }
}
