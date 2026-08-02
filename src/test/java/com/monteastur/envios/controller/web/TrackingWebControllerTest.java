package com.monteastur.envios.controller.web;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.dto.web.EventoView;
import com.monteastur.envios.dto.web.EvidenciaView;
import com.monteastur.envios.dto.web.EntregaView;
import com.monteastur.envios.dto.web.PublicTrackingView;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.web.PublicTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TrackingWebController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class TrackingWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicTrackingService publicTrackingService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private PublicTrackingView viewValida() {
        return new PublicTrackingView("MT-1", "RECIBIDO", "Destinatario", "Asturias, España",
                "Asunción, Paraguay", "10 kg", "Documentos", null, "Asturias",
                LocalDateTime.of(2026, 5, 10, 9, 0), LocalDateTime.of(2026, 5, 15, 14, 30),
                null, null, 0, new ArrayList<>(PublicTrackingView.PASOS_CANONICOS),
                List.of(new EventoView()), List.of(new EvidenciaView()), null);
    }

    @Test
    void formulario_retornaBuscador() throws Exception {
        mockMvc.perform(get("/tracking"))
                .andExpect(status().isOk())
                .andExpect(view().name("tracking-search"))
                .andExpect(model().attribute("buscado", false));
    }

    @Test
    void formulario_en_retornaBuscador() throws Exception {
        mockMvc.perform(get("/en/tracking"))
                .andExpect(status().isOk())
                .andExpect(view().name("tracking-search"));
    }

    @Test
    void buscar_encontrado_redirigePrg() throws Exception {
        when(publicTrackingService.cargarPagina("MT-1")).thenReturn(viewValida());

        mockMvc.perform(post("/tracking").param("codigo", " mt-1 ").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tracking/MT-1"));
    }

    @Test
    void buscar_noEncontrado_rerenderConError() throws Exception {
        when(publicTrackingService.cargarPagina("NOPE")).thenReturn(null);

        mockMvc.perform(post("/tracking").param("codigo", "nope").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("tracking-search"))
                .andExpect(model().attribute("error", true))
                .andExpect(model().attribute("codigo", "NOPE"));
    }

    @Test
    void resultado_retornaTimeline() throws Exception {
        when(publicTrackingService.cargarPagina("MT-1")).thenReturn(viewValida());

        mockMvc.perform(get("/tracking/MT-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("tracking-result"))
                .andExpect(model().attributeExists("view"));
    }

    @Test
    void resultado_en_retornaTimeline() throws Exception {
        when(publicTrackingService.cargarPagina("MT-1")).thenReturn(viewValida());

        mockMvc.perform(get("/en/tracking/MT-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("tracking-result"));
    }

    @Test
    void resultado_noEncontrado_retorna404() throws Exception {
        when(publicTrackingService.cargarPagina("NOPE")).thenReturn(null);

        mockMvc.perform(get("/tracking/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("tracking-404"))
                .andExpect(model().attribute("codigo", "NOPE"));
    }
}
