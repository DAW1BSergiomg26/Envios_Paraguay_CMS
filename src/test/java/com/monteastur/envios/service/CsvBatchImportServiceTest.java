package com.monteastur.envios.service;

import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.BatchImportEstado;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
import com.monteastur.envios.service.batch.CsvEnvioParser;
import com.monteastur.envios.service.batch.CsvImportLineError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CsvBatchImportServiceTest {

    private final BatchImportPersistenceService persistence = mock(BatchImportPersistenceService.class);
    private final EnvioTrackingRepository envioTrackingRepository = mock(EnvioTrackingRepository.class);
    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);

    private CsvBatchImportService servicio(int chunkSize, int maxRows) {
        return new CsvBatchImportService(new CsvEnvioParser(10000), persistence,
                envioTrackingRepository, clienteRepository, chunkSize, maxRows);
    }

    private Path escribirCsv(String contenido) throws Exception {
        Path fichero = Files.createTempFile("importe", ".csv");
        Files.writeString(fichero, contenido);
        return fichero;
    }

    private String cabecera() {
        return "codigo,estado,destinatario,origen,destino,peso,contenido,observaciones\n";
    }

    @Test
    void loteValido_persisteChunkYFinalizaSinErrores(@TempDir Path tempDir) throws Exception {
        Path fichero = escribirCsv(cabecera()
                + "MT-2026-0101,RECIBIDO,María,,,,\n"
                + "MT-2026-0102,ENTREGADO,Pedro,,,,\n");
        CsvBatchImportService service = servicio(100, 200000);

        service.procesarLote(1L, fichero.toString(), null);

        verify(persistence).registrarInicio(1L);
        ArgumentCaptor<List<EnvioTracking>> captorEnvio = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<CsvImportLineError>> captorError = ArgumentCaptor.forClass(List.class);
        verify(persistence).procesarChunk(eq(1L), captorEnvio.capture(), captorError.capture());
        assertThat(captorEnvio.getValue()).hasSize(2);
        assertThat(captorError.getValue()).isEmpty();
        assertThat(captorEnvio.getValue()).extracting(EnvioTracking::getCodigoUnico)
                .containsExactly("MT-2026-0101", "MT-2026-0102");
        verify(persistence).finalizar(eq(1L), eq(2), isNull());
        assertThat(Files.exists(fichero)).isFalse();
    }

    @Test
    void loteValido_asociaClienteIndicado(@TempDir Path tempDir) throws Exception {
        Cliente cliente = new Cliente("c@example.com", "p", "Cliente", "+595");
        when(clienteRepository.findById(99L)).thenReturn(Optional.of(cliente));
        Path fichero = escribirCsv(cabecera() + "MT-2026-0103,RECIBIDO,María,,,,\n");
        CsvBatchImportService service = servicio(100, 200000);

        service.procesarLote(2L, fichero.toString(), 99L);

        ArgumentCaptor<List<EnvioTracking>> captor = ArgumentCaptor.forClass(List.class);
        verify(persistence).procesarChunk(eq(2L), captor.capture(), eq(List.of()));
        assertThat(captor.getValue().get(0).getCliente()).isSameAs(cliente);
        verify(persistence).finalizar(eq(2L), eq(1), isNull());
    }

    @Test
    void filasInvalidas_seAuditanComoErrores(@TempDir Path tempDir) throws Exception {
        Path fichero = escribirCsv(cabecera()
                + "MT-2026-0104,NO_EXISTE,María,,,,\n"
                + "MT-2026-0105,RECIBIDO,María,,,,\n");
        CsvBatchImportService service = servicio(100, 200000);

        service.procesarLote(3L, fichero.toString(), null);

        ArgumentCaptor<List<EnvioTracking>> captorEnvio = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<CsvImportLineError>> captorError = ArgumentCaptor.forClass(List.class);
        verify(persistence).procesarChunk(eq(3L), captorEnvio.capture(), captorError.capture());
        assertThat(captorEnvio.getValue()).hasSize(1);
        assertThat(captorError.getValue()).hasSize(1);
        assertThat(captorError.getValue().get(0).getCodigoRastreo()).isEqualTo("MT-2026-0104");
        assertThat(captorError.getValue().get(0).getErrorMensaje()).contains("estado");
    }

    @Test
    void codigoDuplicadoDentroDelFichero_seAuditaComoError(@TempDir Path tempDir) throws Exception {
        Path fichero = escribirCsv(cabecera()
                + "MT-2026-0106,RECIBIDO,María,,,,\n"
                + "MT-2026-0106,ENTREGADO,Pedro,,,,\n");
        CsvBatchImportService service = servicio(100, 200000);

        service.procesarLote(4L, fichero.toString(), null);

        ArgumentCaptor<List<EnvioTracking>> captorEnvio = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<CsvImportLineError>> captorError = ArgumentCaptor.forClass(List.class);
        verify(persistence).procesarChunk(eq(4L), captorEnvio.capture(), captorError.capture());
        assertThat(captorEnvio.getValue()).hasSize(1);
        assertThat(captorError.getValue()).hasSize(1);
        assertThat(captorError.getValue().get(0).getErrorMensaje()).contains("duplicado");
        verify(persistence).finalizar(eq(4L), eq(2), isNull());
    }

    @Test
    void codigoYaExistenteEnBD_seAuditaComoError(@TempDir Path tempDir) throws Exception {
        when(envioTrackingRepository.existsByCodigoUnico("MT-2026-0107")).thenReturn(true);
        Path fichero = escribirCsv(cabecera() + "MT-2026-0107,RECIBIDO,María,,,,\n");
        CsvBatchImportService service = servicio(100, 200000);

        service.procesarLote(5L, fichero.toString(), null);

        ArgumentCaptor<List<EnvioTracking>> captorEnvio = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<CsvImportLineError>> captorError = ArgumentCaptor.forClass(List.class);
        verify(persistence).procesarChunk(eq(5L), captorEnvio.capture(), captorError.capture());
        assertThat(captorEnvio.getValue()).isEmpty();
        assertThat(captorError.getValue()).hasSize(1);
        assertThat(captorError.getValue().get(0).getErrorMensaje()).contains("duplicado");
    }

    @Test
    void chunkSizeAcotado_procesaEnVariosChunks(@TempDir Path tempDir) throws Exception {
        Path fichero = escribirCsv(cabecera()
                + "MT-2026-0201,RECIBIDO,María,,,,\n"
                + "MT-2026-0202,RECIBIDO,Pedro,,,,\n"
                + "MT-2026-0203,RECIBIDO,Luis,,,,\n");
        CsvBatchImportService service = servicio(2, 200000);

        service.procesarLote(6L, fichero.toString(), null);

        ArgumentCaptor<List<EnvioTracking>> captor = ArgumentCaptor.forClass(List.class);
        verify(persistence, org.mockito.Mockito.times(2))
                .procesarChunk(eq(6L), captor.capture(), eq(List.of()));
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(0)).hasSize(2);
        assertThat(captor.getAllValues().get(1)).hasSize(1);
    }

    @Test
    void maxRowsSuperado_finalizaConErrorResumen(@TempDir Path tempDir) throws Exception {
        Path fichero = escribirCsv(cabecera()
                + "MT-2026-0301,RECIBIDO,María,,,,\n"
                + "MT-2026-0302,RECIBIDO,Pedro,,,,\n"
                + "MT-2026-0303,RECIBIDO,Luis,,,,\n");
        CsvBatchImportService service = servicio(100, 2);

        service.procesarLote(7L, fichero.toString(), null);

        verify(persistence).finalizar(eq(7L), eq(2), org.mockito.ArgumentMatchers.contains("máximo"));
    }

    @Test
    void ficheroInexistente_marcaFallidoSinRomper(@TempDir Path tempDir) throws Exception {
        Path inexistente = tempDir.resolve("no-existe.csv");
        CsvBatchImportService service = servicio(100, 200000);

        service.procesarLote(8L, inexistente.toString(), null);

        verify(persistence).registrarInicio(8L);
        verify(persistence).marcarFallido(eq(8L), org.mockito.ArgumentMatchers.contains("Error"));
        verify(persistence, never()).finalizar(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
        verify(persistence, never()).procesarChunk(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList());
    }
}
