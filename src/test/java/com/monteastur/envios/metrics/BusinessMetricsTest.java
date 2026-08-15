package com.monteastur.envios.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessMetricsTest {

    private MeterRegistry registry;
    private BusinessMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new BusinessMetrics(registry);
    }

    @Test
    void buscarEncontrado_registraTimerYCounter() {
        Timer.Sample sample = metrics.iniciarBusqueda();
        metrics.registrarBusqueda(sample, true);

        assertThat(registry.get("envios.tracking.resultado").counter().count()).isEqualTo(1);
        Timer timer = registry.get("envios.tracking.pagina").tag("encontrado", "true").timer();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isGreaterThanOrEqualTo(0);
    }

    @Test
    void buscarNoEncontrado_registraTagFalse() {
        Timer.Sample sample = metrics.iniciarBusqueda();
        metrics.registrarBusqueda(sample, false);

        assertThat(registry.get("envios.tracking.resultado").counter().count()).isEqualTo(1);
        assertThat(registry.get("envios.tracking.pagina").tag("encontrado", "false").timer().count())
                .isEqualTo(1);
    }

    @Test
    void difundirOk_registraTimerYCounter() {
        Timer.Sample sample = metrics.iniciarDifusion();
        metrics.registrarDifusion(sample, true);

        assertThat(registry.get("envios.websocket.resultado").counter().count()).isEqualTo(1);
        assertThat(registry.get("envios.websocket.difusion").tag("resultado", "ok").timer().count())
                .isEqualTo(1);
    }

    @Test
    void difundirConError_registraResultadoError() {
        Timer.Sample sample = metrics.iniciarDifusion();
        metrics.registrarDifusion(sample, false);

        assertThat(registry.get("envios.websocket.resultado").counter().count()).isEqualTo(1);
        assertThat(registry.get("envios.websocket.difusion").tag("resultado", "error").timer().count())
                .isEqualTo(1);
    }
}
