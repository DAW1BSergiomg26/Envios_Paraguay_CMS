package com.monteastur.envios.service.pdf;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BarcodeServiceTest {

    private final BarcodeService service = new BarcodeService();

    @Test
    void generarCode128_devuelveImagenConDimensiones() {
        BufferedImage img = service.generarCode128("MT-2026-0001", 300, 80);
        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(300);
        assertThat(img.getHeight()).isEqualTo(80);
    }

    @Test
    void generarQr_devuelveImagenCuadrada() {
        BufferedImage img = service.generarQr("https://tracking.example/MT-2026-0001", 200);
        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(200);
        assertThat(img.getHeight()).isEqualTo(200);
    }

    @Test
    void generarCode128_contenidoVacio_lanzaIllegalArgument() {
        assertThatThrownBy(() -> service.generarCode128("   ", 100, 50))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generarQr_contenidoVacio_lanzaIllegalArgument() {
        assertThatThrownBy(() -> service.generarQr(null, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toPng_devuelveBytesPng() {
        BufferedImage img = service.generarQr("MT-1", 100);
        byte[] png = service.toPng(img);
        assertThat(png).isNotEmpty();
        assertThat(png[0] & 0xFF).isEqualTo(0x89);
        assertThat(png[1] & 0xFF).isEqualTo(0x50);
    }
}
