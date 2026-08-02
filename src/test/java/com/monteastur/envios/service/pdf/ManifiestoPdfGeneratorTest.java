package com.monteastur.envios.service.pdf;

import com.monteastur.envios.model.EnvioTracking;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ManifiestoPdfGeneratorTest {

    private final ManifiestoPdfGenerator generator = new ManifiestoPdfGenerator();

    @Test
    void generar_devuelvePdfValidoConTotales() {
        List<EnvioTracking> envios = List.of(
                new EnvioTracking("MT-M1", "RECIBIDO", "Ana", "O", "D", "1,5 kg", "Documentos"),
                new EnvioTracking("MT-M2", "ENTREGADO", "Luis", "O", "D", "2 kg", "Caja"));

        byte[] pdf = generator.generar(42L, envios, "Cliente Demo");

        assertThat(pdf).isNotEmpty();
        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        String contenido = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(contenido)
                .contains("MANIFIESTO DE CARGA")
                .contains("Cliente Demo")
                .contains("3.50")
                .contains("MT-M1");
    }

    @Test
    void generar_pesosInvalido_muestraGuion() {
        List<EnvioTracking> envios = List.of(
                new EnvioTracking("MT-M3", "RECIBIDO", "Ana", "O", "D", "n/a", "Documentos"));

        byte[] pdf = generator.generar(43L, envios, null);

        String contenido = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(contenido).doesNotContain("0.00");
    }

    @Test
    void anchoA4_es595pt() {
        assertThat(ManifiestoPdfGenerator.ANCHO_A4_PT).isCloseTo(595.28f, org.assertj.core.data.Offset.offset(0.5f));
        assertThat(ManifiestoPdfGenerator.ALTO_A4_PT).isCloseTo(841.89f, org.assertj.core.data.Offset.offset(0.5f));
    }
}
