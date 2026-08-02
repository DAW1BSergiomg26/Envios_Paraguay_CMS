package com.monteastur.envios.service.pdf;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;

class PesoUtilTest {

    @Test
    void parsear_pesoDecimalEspacioUnidad() {
        assertThat(PesoUtil.parsear("1.5 kg")).hasValue(1.5);
    }

    @Test
    void parsear_pesoEntero() {
        assertThat(PesoUtil.parsear("2")).hasValue(2.0);
    }

    @Test
    void parsear_commaDecimal() {
        assertThat(PesoUtil.parsear("1,5 kg")).hasValue(1.5);
    }

    @Test
    void parsear_soloNumeroYEspacios() {
        assertThat(PesoUtil.parsear("  12.5  ")).hasValue(12.5);
    }

    @Test
    void parsear_null_o_vacio_devuelveVacio() {
        assertThat(PesoUtil.parsear(null)).isEmpty();
        assertThat(PesoUtil.parsear("")).isEmpty();
        assertThat(PesoUtil.parsear("   ")).isEmpty();
    }

    @Test
    void parsear_invalido_devuelveVacio() {
        assertThat(PesoUtil.parsear("n/a")).isEmpty();
        assertThat(PesoUtil.parsear("peso no declarado")).isEmpty();
        assertThat(PesoUtil.parsear(".")).isEmpty();
    }
}
