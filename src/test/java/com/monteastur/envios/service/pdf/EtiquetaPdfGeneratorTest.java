package com.monteastur.envios.service.pdf;

import com.monteastur.envios.model.EnvioTracking;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EtiquetaPdfGeneratorTest {

    private final EtiquetaPdfGenerator generator = new EtiquetaPdfGenerator(new BarcodeService());

    @Test
    void generar_devuelvePdfValido() {
        EnvioTracking envio = new EnvioTracking("MT-2026-0099", "EN_TRANSITO", "María López",
                "Asturias, España", "Asunción, Paraguay", "1.5 kg", "Documentos");
        byte[] pdf = generator.generar(envio, "http://localhost:8080/tracking/MT-2026-0099");

        assertThat(pdf).isNotEmpty();
        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        assertThat(new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1))
                .contains("MT-2026-0099")
                .contains("María López");
    }

    @Test
    void tamanoPagina_es100x150mm() {
        assertThat(EtiquetaPdfGenerator.ANCHO_PT).isCloseTo(283.46f, org.assertj.core.data.Offset.offset(0.5f));
        assertThat(EtiquetaPdfGenerator.ALTO_PT).isCloseTo(425.2f, org.assertj.core.data.Offset.offset(0.5f));
    }
}
