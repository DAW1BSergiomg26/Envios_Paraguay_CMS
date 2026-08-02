package com.monteastur.envios.service;

import com.monteastur.envios.dto.api.RegistrarEntregaRequest;
import com.monteastur.envios.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntregaValidatorTest {

    private static final String PNG_1X1 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    private RegistrarEntregaRequest requestValido() {
        RegistrarEntregaRequest req = new RegistrarEntregaRequest();
        req.setReceptorNombre("Ana López");
        req.setReceptorDocumento("12345678");
        req.setFirmaBase64(PNG_1X1);
        req.setLatitud(new BigDecimal("-25.2637421"));
        req.setLongitud(new BigDecimal("-57.575926"));
        return req;
    }

    @Test
    void requestValido_noLanzaExcepcion() {
        assertThatCode(() -> EntregaValidator.validar(requestValido()))
                .doesNotThrowAnyException();
    }

    @Test
    void receptorNombreVacio_lanza400() {
        RegistrarEntregaRequest req = requestValido();
        req.setReceptorNombre("  ");
        assertThatThrownBy(() -> EntregaValidator.validar(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("receptor");
    }

    @Test
    void receptorDocumentoNulo_lanza400() {
        RegistrarEntregaRequest req = requestValido();
        req.setReceptorDocumento(null);
        assertThatThrownBy(() -> EntregaValidator.validar(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("documento");
    }

    @Test
    void firmaNula_lanza400() {
        RegistrarEntregaRequest req = requestValido();
        req.setFirmaBase64(null);
        assertThatThrownBy(() -> EntregaValidator.validar(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("firma");
    }

    @Test
    void firmaNoBase64_lanza400() {
        assertThatThrownBy(() -> EntregaValidator.validarFirmaBase64("###no-es-base64###"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    void firmaBase64NoPng_lanza400() {
        String base64 = java.util.Base64.getEncoder().encodeToString("hola".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThatThrownBy(() -> EntregaValidator.validarFirmaBase64(base64))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PNG");
    }

    @Test
    void firmaPngValida_noLanza() {
        assertThatCode(() -> EntregaValidator.validarFirmaBase64(PNG_1X1))
                .doesNotThrowAnyException();
    }

    @Test
    void latitudFueraDeRango_lanza400() {
        assertThatThrownBy(() -> EntregaValidator.validarCoordenadas(new BigDecimal("90.5"), BigDecimal.ZERO))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Latitud");
    }

    @Test
    void longitudFueraDeRango_lanza400() {
        assertThatThrownBy(() -> EntregaValidator.validarCoordenadas(BigDecimal.ZERO, new BigDecimal("181")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Longitud");
    }

    @Test
    void bordesValidos_noLanzan() {
        assertThatCode(() -> EntregaValidator.validarCoordenadas(new BigDecimal("-90"), new BigDecimal("180")))
                .doesNotThrowAnyException();
        assertThatCode(() -> EntregaValidator.validarCoordenadas(new BigDecimal("90"), new BigDecimal("-180")))
                .doesNotThrowAnyException();
    }

    @Test
    void coordenadasNulas_noLanzan() {
        assertThatCode(() -> EntregaValidator.validarCoordenadas(null, null)).doesNotThrowAnyException();
    }
}
