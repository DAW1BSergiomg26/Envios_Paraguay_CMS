package com.grupb2.casarural.controller.api;

import com.grupb2.casarural.config.SecurityConfig;
import com.grupb2.casarural.model.EnvioTracking;
import com.grupb2.casarural.repository.EnvioTrackingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrackingApiController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class TrackingApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnvioTrackingRepository trackingRepository;

    @Test
    void getTrackingByCodigo_existente_retorna200() throws Exception {
        EnvioTracking envio = new EnvioTracking();
        envio.setCodigoUnico("MT-2026-0001");
        envio.setEstado("en_transito");
        envio.setDestinatario("Juan Perez");
        envio.setOrigen("Madrid");
        envio.setDestino("Asuncion");
        envio.setPeso("2.5");
        envio.setContenido("Documentos");
        envio.setUltimaActualizacion(LocalDateTime.now());

        when(trackingRepository.findByCodigoUnico("MT-2026-0001")).thenReturn(Optional.of(envio));

        mockMvc.perform(get("/api/v1/tracking/{codigo}", "MT-2026-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoUnico").value("MT-2026-0001"))
                .andExpect(jsonPath("$.estado").value("en_transito"))
                .andExpect(jsonPath("$.destinatario").value("Juan Perez"));
    }

    @Test
    void getTrackingByCodigo_inexistente_retorna404() throws Exception {
        when(trackingRepository.findByCodigoUnico(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tracking/{codigo}", "NO-EXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Tracking no encontrado"));
    }
}
