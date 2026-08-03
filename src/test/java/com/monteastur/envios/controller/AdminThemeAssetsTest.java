package com.monteastur.envios.controller;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.repository.MensajeContactoRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.TextoLegalRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.ClienteService;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.EnvioTrackingService;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminController.class, LoginController.class, ClienteController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test",
    "app.upload.dir=./uploads"
})
class AdminThemeAssetsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ReservaRepository reservaRepo;
    @MockBean private ImagenRepository imagenRepo;
    @MockBean private MensajeContactoRepository mensajeRepo;
    @MockBean private TextoLegalRepository textoRepo;
    @MockBean private EnvioTrackingRepository trackingRepo;
    @MockBean private EmailService emailService;
    @MockBean private ClienteRepository clienteRepo;
    @MockBean private EvidenciaEnvioService evidenciaService;
    @MockBean private EventoTrackingService eventoTrackingService;
    @MockBean private EnvioTrackingService envioTrackingService;
    @MockBean private ClienteService clienteService;
    @MockBean private RBACAccessLogger rbacAccessLogger;
    @MockBean private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockBean private DataSource dataSource;

    @Test
    void loginPages_haveThemeAssetsAndAntiFouc() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-theme")))
                .andExpect(content().string(containsString("/css/design-system.css")))
                .andExpect(content().string(containsString("/css/theme-ui.css")))
                .andExpect(content().string(containsString("/js/theme-toggle.js")))
                .andExpect(content().string(containsString("btn-theme-toggle")));

        mockMvc.perform(get("/cliente/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-theme")))
                .andExpect(content().string(containsString("/css/design-system.css")))
                .andExpect(content().string(containsString("/css/theme-ui.css")))
                .andExpect(content().string(containsString("/js/theme-toggle.js")))
                .andExpect(content().string(containsString("btn-theme-toggle")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/admin/dashboard",
        "/admin/mensajesrecibidos",
        "/admin/reservas",
        "/admin/imagenes",
        "/admin/textos",
        "/admin/tracking",
        "/admin/tracking/nuevo"
    })
    void adminPages_haveThemeAssetsAndAntiFouc(String url) throws Exception {
        mockMvc.perform(get(url).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-theme")))
                .andExpect(content().string(containsString("/css/design-system.css")))
                .andExpect(content().string(containsString("/css/theme-ui.css")))
                .andExpect(content().string(containsString("/js/theme-toggle.js")))
                .andExpect(content().string(containsString("btn-theme-toggle")));
    }
}
