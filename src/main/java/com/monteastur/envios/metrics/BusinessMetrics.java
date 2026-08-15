package com.monteastur.envios.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Métricas de negocio con Micrometer (Prometheus).
 * Registra búsquedas de rastreo y difusiones WebSocket con latencia y resultado.
 */
@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    private static final String CONTADOR_BUSQUEDA = "envios.tracking.resultado";
    private static final String TIMER_BUSQUEDA = "envios.tracking.pagina";
    private static final String CONTADOR_DIFUSION = "envios.websocket.resultado";
    private static final String TIMER_DIFUSION = "envios.websocket.difusion";
    private static final String TAG_ENCONTRADO = "encontrado";
    private static final String TAG_RESULTADO = "resultado";

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Timer.Sample iniciarBusqueda() {
        return Timer.start(meterRegistry);
    }

    public void registrarBusqueda(Timer.Sample sample, boolean encontrado) {
        Timer.builder(TIMER_BUSQUEDA)
                .description("Tiempo de resolución de una búsqueda pública de rastreo")
                .tag(TAG_ENCONTRADO, String.valueOf(encontrado))
                .register(meterRegistry);
        sample.stop(meterRegistry.timer(TIMER_BUSQUEDA, TAG_ENCONTRADO, String.valueOf(encontrado)));
        counter(CONTADOR_BUSQUEDA, "Búsquedas públicas de rastreo").increment();
    }

    public Timer.Sample iniciarDifusion() {
        return Timer.start(meterRegistry);
    }

    public void registrarDifusion(Timer.Sample sample, boolean exitosa) {
        String resultado = exitosa ? "ok" : "error";
        Timer.builder(TIMER_DIFUSION)
                .description("Tiempo de difusión WebSocket de un evento de tracking")
                .tag(TAG_RESULTADO, resultado)
                .register(meterRegistry);
        sample.stop(meterRegistry.timer(TIMER_DIFUSION, TAG_RESULTADO, resultado));
        counter(CONTADOR_DIFUSION, "Difusiones WebSocket emitidas").increment();
    }

    private Counter counter(String nombre, String descripcion) {
        return Counter.builder(nombre)
                .description(descripcion)
                .register(meterRegistry);
    }
}
