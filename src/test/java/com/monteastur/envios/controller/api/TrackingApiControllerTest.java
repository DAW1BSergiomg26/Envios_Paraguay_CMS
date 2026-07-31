package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.dto.api.PublicTrackingDto;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.EnvioTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import java.time.LocalDateTime;

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
    private EnvioTrackingService envioTrackingService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Test
    void getTrackingByCodigo_existente_retorna200() throws Exception {
        PublicTrackingDto dto = new PublicTrackingDto();
        dto.setCodigoUnico("MT-2026-0001");
        dto.setEstado("en_transito");
        dto.setOrigen("Madrid");
        dto.setDestino("Asuncion");
        dto.setUltimaActualizacion(LocalDateTime.now());

        when(envioTrackingService.buscarPorCodigo("MT-2026-0001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/tracking/{codigo}", "MT-2026-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoUnico").value("MT-2026-0001"))
                .andExpect(jsonPath("$.estado").value("en_transito"))
                .andExpect(jsonPath("$.origen").value("Madrid"))
                .andExpect(jsonPath("$.destino").value("Asuncion"));
    }

    @Test
    void getTrackingByCodigo_inexistente_retorna404() throws Exception {
        when(envioTrackingService.buscarPorCodigo(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/v1/tracking/{codigo}", "NO-EXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Tracking no encontrado"));
    }
}
