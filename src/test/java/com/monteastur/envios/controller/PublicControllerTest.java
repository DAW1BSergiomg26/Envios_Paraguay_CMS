package com.monteastur.envios.controller;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.model.Imagen;
import com.monteastur.envios.model.TextoLegal;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.repository.MensajeContactoRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.TextoLegalRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class PublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ImagenRepository imagenRepo;
    @MockBean private TextoLegalRepository textoRepo;
    @MockBean private ReservaRepository reservaRepo;
    @MockBean private MensajeContactoRepository mensajeRepo;
    @MockBean private EmailService emailService;
    @MockBean private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @ParameterizedTest
    @CsvSource({
        "/,      home",
        "/en,    en/home",
        "/entorno,       entorno",
        "/en/entorno,    en/entorno",
        "/operaciones,       operaciones",
        "/en/operaciones,    en/operaciones"
    })
    void staticPages_returnCorrectView(String url, String viewName) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName));
    }

    @Test
    void laCasa_returnsViewWithImages() throws Exception {
        when(imagenRepo.findAllByOrderByOrdenAsc()).thenReturn(List.of(new Imagen()));

        mockMvc.perform(get("/casa"))
                .andExpect(status().isOk())
                .andExpect(view().name("lacasa"))
                .andExpect(model().attributeExists("imagenes"));
    }

    @Test
    void laCasa_en_returnsViewWithImages() throws Exception {
        when(imagenRepo.findAllByOrderByOrdenAsc()).thenReturn(List.of(new Imagen()));

        mockMvc.perform(get("/en/casa"))
                .andExpect(status().isOk())
                .andExpect(view().name("en/lacasa"))
                .andExpect(model().attributeExists("imagenes"));
    }

    @Test
    void avisoLegal_returnsViewWithText() throws Exception {
        when(textoRepo.findBySlug("aviso-legal")).thenReturn(Optional.of(new TextoLegal()));

        mockMvc.perform(get("/aviso-legal"))
                .andExpect(status().isOk())
                .andExpect(view().name("aviso-legal"))
                .andExpect(model().attributeExists("texto"));
    }

    @Test
    void avisoLegal_en_returnsViewWithText() throws Exception {
        when(textoRepo.findBySlug("aviso-legal")).thenReturn(Optional.of(new TextoLegal()));

        mockMvc.perform(get("/en/aviso-legal"))
                .andExpect(status().isOk())
                .andExpect(view().name("en/aviso-legal"))
                .andExpect(model().attributeExists("texto"));
    }

    @Test
    void politicaCookies_returnsViewWithText() throws Exception {
        when(textoRepo.findBySlug("politica-cookies")).thenReturn(Optional.of(new TextoLegal()));

        mockMvc.perform(get("/politica-cookies"))
                .andExpect(status().isOk())
                .andExpect(view().name("politica-cookies"))
                .andExpect(model().attributeExists("texto"));
    }

    @Test
    void reservas_returnsView() throws Exception {
        when(reservaRepo.findOcupadasEnRango(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/reservas"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas"))
                .andExpect(model().attribute("reservaEnviada", false))
                .andExpect(model().attributeExists("calendarios"));
    }

    @Test
    void reservas_en_returnsEnView() throws Exception {
        when(reservaRepo.findOcupadasEnRango(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/en/reservas"))
                .andExpect(status().isOk())
                .andExpect(view().name("en/reservas"))
                .andExpect(model().attribute("reservaEnviada", false))
                .andExpect(model().attributeExists("calendarios"));
    }

    @Test
    void contacto_returnsView() throws Exception {
        mockMvc.perform(get("/contacto"))
                .andExpect(status().isOk())
                .andExpect(view().name("contacto"))
                .andExpect(model().attribute("mensajeEnviado", false));
    }

    @Test
    void contacto_en_returnsEnView() throws Exception {
        mockMvc.perform(get("/en/contacto"))
                .andExpect(status().isOk())
                .andExpect(view().name("en/contacto"))
                .andExpect(model().attribute("mensajeEnviado", false));
    }

    @Test
    void themeAssets_marketingPage_hasDesignSystemCss() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/design-system.css")));
    }

    @Test
    void themeAssets_marketingPage_hasVersionedDesignSystemCss() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/design-system.css?v=1")));
    }

    @Test
    void themeAssets_marketingPage_hasToggleAssets() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/js/vendor/lucide.min.js")))
                .andExpect(content().string(containsString("/js/theme-toggle.js")))
                .andExpect(content().string(containsString("brand-monteastur")));
    }

    @Test
    void themeAssets_marketingPage_hasAntiFoucAndToggle() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("setAttribute('data-theme'")))
                .andExpect(content().string(containsString("btn-theme-toggle")));
    }
}
