package com.monteastur.envios.service.batch;

import com.monteastur.envios.model.EnvioTracking;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvEnvioParserTest {

    private final CsvEnvioParser parser = new CsvEnvioParser(10000);

    private List<CsvEnvioRow> procesar(String contenido) throws Exception {
        List<CsvEnvioRow> filas = new ArrayList<>();
        parser.procesar(new StringReader(contenido), filas::add);
        return filas;
    }

    @Test
    void procesar_conBomYCabecera_parseaFilasValidas() throws Exception {
        String csv = "\uFEFFcodigo,estado,destinatario,origen,destino,peso,contenido,observaciones\n"
                + "MT-2026-0001,RECIBIDO,María López,Asturias,Asunción,5 kg,Documentos,Paquete frágil\n"
                + "MT-2026-0002,ENTREGADO,Pedro Ramírez,,,2 kg,,Entrega confirmada\n";

        List<CsvEnvioRow> filas = procesar(csv);

        assertThat(filas).hasSize(2);
        CsvEnvioRow primera = filas.get(0);
        assertThat(primera.esError()).isFalse();
        assertThat(primera.getLineaNumero()).isEqualTo(2);
        EnvioTracking envio = primera.getEnvio();
        assertThat(envio.getCodigoUnico()).isEqualTo("MT-2026-0001");
        assertThat(envio.getEstado()).isEqualTo("RECIBIDO");
        assertThat(envio.getDestinatario()).isEqualTo("María López");
        assertThat(envio.getOrigen()).isEqualTo("Asturias");
        assertThat(envio.getDestino()).isEqualTo("Asunción");
        assertThat(envio.getPeso()).isEqualTo("5 kg");
        assertThat(envio.getContenido()).isEqualTo("Documentos");
        assertThat(envio.getObservaciones()).isEqualTo("Paquete frágil");
    }

    @Test
    void procesar_camposOpcionalesVacios_seMapeanANull() throws Exception {
        String csv = "codigo,estado,destinatario,origen,destino,peso,contenido,observaciones\n"
                + "MT-2026-0003,RECIBIDO,Cliente Sin Datos,,,,\n";

        List<CsvEnvioRow> filas = procesar(csv);

        CsvEnvioRow fila = filas.get(0);
        assertThat(fila.esError()).isFalse();
        assertThat(fila.getEnvio().getOrigen()).isNull();
        assertThat(fila.getEnvio().getDestino()).isNull();
        assertThat(fila.getEnvio().getPeso()).isNull();
        assertThat(fila.getEnvio().getContenido()).isNull();
        assertThat(fila.getEnvio().getObservaciones()).isNull();
    }

    @Test
    void procesar_estadoInvalido_registraErrorDeLinea() throws Exception {
        String csv = "codigo,estado,destinatario,origen,destino,peso,contenido,observaciones\n"
                + "MT-2026-0004,NO_EXISTE,María,,,,\n";

        List<CsvEnvioRow> filas = procesar(csv);

        CsvEnvioRow fila = filas.get(0);
        assertThat(fila.esError()).isTrue();
        assertThat(fila.getError().getLineaNumero()).isEqualTo(2);
        assertThat(fila.getError().getCodigoRastreo()).isEqualTo("MT-2026-0004");
        assertThat(fila.getError().getErrorMensaje()).contains("estado");
    }

    @Test
    void procesar_codigoObligatorioFaltante_registraError() throws Exception {
        String csv = "codigo,estado,destinatario,origen,destino,peso,contenido,observaciones\n"
                + ",RECIBIDO,María,,,,\n";

        List<CsvEnvioRow> filas = procesar(csv);

        CsvEnvioRow fila = filas.get(0);
        assertThat(fila.esError()).isTrue();
        assertThat(fila.getError().getErrorMensaje()).contains("codigo");
        assertThat(fila.getError().getCodigoRastreo()).isNull();
    }

    @Test
    void procesar_destinatarioFaltante_registraError() throws Exception {
        String csv = "codigo,estado,destinatario,origen,destino,peso,contenido,observaciones\n"
                + "MT-2026-0005,RECIBIDO, ,,,\n";

        List<CsvEnvioRow> filas = procesar(csv);

        CsvEnvioRow fila = filas.get(0);
        assertThat(fila.esError()).isTrue();
        assertThat(fila.getError().getErrorMensaje()).contains("destinatario");
    }

    @Test
    void procesar_numeroIncorrectoDeColumnas_registraError() throws Exception {
        String csv = "codigo,estado,destinatario,origen,destino,peso,contenido,observaciones\n"
                + "MT-2026-0006,RECIBIDO\n";

        List<CsvEnvioRow> filas = procesar(csv);

        CsvEnvioRow fila = filas.get(0);
        assertThat(fila.esError()).isTrue();
        assertThat(fila.getError().getErrorMensaje()).contains("columnas");
    }

    @Test
    void procesar_quotingConComaInterna_sePreserva() throws Exception {
        String csv = "codigo,estado,destinatario,origen,destino,peso,contenido,observaciones\n"
                + "\"MT-2026-0007\",RECIBIDO,\"López, María\",Asturias,Asunción,5 kg,\"Documentos, paquete\",\n";

        List<CsvEnvioRow> filas = procesar(csv);

        CsvEnvioRow fila = filas.get(0);
        assertThat(fila.esError()).isFalse();
        assertThat(fila.getEnvio().getDestinatario()).isEqualTo("López, María");
        assertThat(fila.getEnvio().getContenido()).isEqualTo("Documentos, paquete");
    }

    @Test
    void procesar_observacionesLargas_sePermiten() throws Exception {
        String observaciones = "x".repeat(1000);
        String csv = "codigo,estado,destinatario,origen,destino,peso,contenido,observaciones\n"
                + "MT-2026-0008,RECIBIDO,María,,,,," + observaciones + "\n";

        List<CsvEnvioRow> filas = procesar(csv);

        assertThat(filas.get(0).esError()).isFalse();
        assertThat(filas.get(0).getEnvio().getObservaciones()).isEqualTo(observaciones);
    }

    @Test
    void procesar_campoObligatorioSupera255_registraError() throws Exception {
        String codigoLargo = "X".repeat(256);
        String csv = "codigo,estado,destinatario,origen,destino,peso,contenido,observaciones\n"
                + codigoLargo + ",RECIBIDO,María,,,,\n";

        List<CsvEnvioRow> filas = procesar(csv);

        assertThat(filas.get(0).esError()).isTrue();
        assertThat(filas.get(0).getError().getErrorMensaje()).contains("255");
    }

    @Test
    void procesar_lineaMasLargaQueMaxLineLength_registraError() throws Exception {
        CsvEnvioParser parserCorto = new CsvEnvioParser(50);
        String codigoLargo = "C".repeat(60);
        String csv = "codigo,estado,destinatario,origen,destino,peso,contenido,observaciones\n"
                + codigoLargo + ",RECIBIDO,María,,,,\n";

        List<CsvEnvioRow> filas = new ArrayList<>();
        parserCorto.procesar(new StringReader(csv), filas::add);

        assertThat(filas.get(0).esError()).isTrue();
        assertThat(filas.get(0).getError().getErrorMensaje()).contains("larga");
    }
}
