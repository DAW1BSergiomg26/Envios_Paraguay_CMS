package com.monteastur.envios.integration;

import com.monteastur.envios.metrics.BusinessMetrics;
import com.monteastur.envios.service.EmailService;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PrometheusMetricsExposureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusinessMetrics businessMetrics;

    @MockBean
    private EmailService emailService;

    @Test
    void prometheusExponeMetricasDeJvmYDeNegocio() throws Exception {
        Timer.Sample sample = businessMetrics.iniciarBusqueda();
        businessMetrics.registrarBusqueda(sample, true);

        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith("text/plain"))
                .andExpect(MockMvcResultMatchers.content().string(containsString("jvm_")));
        // Las métricas de negocio se registran al registrar la búsqueda:
        // envios_tracking_resultado_total y envios_tracking_busqueda_seconds_*
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(MockMvcResultMatchers.content().string(containsString("envios_tracking_resultado_total")))
                .andExpect(MockMvcResultMatchers.content().string(containsString("envios_tracking_busqueda_seconds")));
    }
}
