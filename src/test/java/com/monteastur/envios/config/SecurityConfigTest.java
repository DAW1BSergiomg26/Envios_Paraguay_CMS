package com.monteastur.envios.config;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.controller.api.ClienteApiController;
import com.monteastur.envios.controller.api.PushSubscriptionController;
import com.monteastur.envios.controller.web.ClientDashboardController;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.ClienteService;
import com.monteastur.envios.service.DocumentoPdfService;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import com.monteastur.envios.service.web.ClientDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PushSubscriptionController.class,
        ClienteApiController.class, ClientDashboardController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test",
    "app.upload.dir=src/test/resources/uploads"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private JdbcUserDetailsManager userDetailsManager;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @MockBean
    private EnvioTrackingRepository envioTrackingRepository;

    @MockBean
    private ClienteService clienteService;

    @MockBean
    private EvidenciaEnvioService evidenciaEnvioService;

    @MockBean
    private EventoTrackingService eventoTrackingService;

    @MockBean
    private ClientDashboardService dashboardService;

    @MockBean
    private DocumentoPdfService documentoPdfService;

    @Test
    void apiPublico_accesibleSinAuth() throws Exception {
        mockMvc.perform(post("/api/v1/push/test"))
                .andExpect(status().isOk());
    }

    @Test
    void adminSinAuth_redirigeALogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                .accept(org.springframework.http.MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void apiAdminSinAuth_redirigeALogin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/envios")
                .accept(org.springframework.http.MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void deliveriesSinAuth_devuelve401Json() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/MT-1/pod")
                .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiClienteSinAuth_devuelve401Json() throws Exception {
        mockMvc.perform(get("/api/v1/cliente/envios")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void panelClienteSinAuth_redirigeLogin() throws Exception {
        mockMvc.perform(get("/cliente/panel")
                .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/login"));
    }

    @Test
    void apiClienteConSesion_accesible() throws Exception {
        mockMvc.perform(get("/api/v1/cliente/envios")
                .sessionAttr("clienteId", 7L)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void loginCorrecto_redirigeAlPanelReact() throws Exception {
        String encodedPassword = new BCryptPasswordEncoder().encode("test");
        org.springframework.security.core.userdetails.UserDetails adminUser =
                org.springframework.security.core.userdetails.User
                        .withUsername("admin")
                        .password(encodedPassword)
                        .roles("ADMIN")
                        .build();
        when(userDetailsManager.loadUserByUsername("admin")).thenReturn(adminUser);

        mockMvc.perform(formLogin("/login").user("admin").password("test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/react-dashboard/"));
    }

    @Test
    void respuestaContieneCabecerasDeSeguridadBasicas() throws Exception {
        mockMvc.perform(post("/api/v1/push/test"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Content-Security-Policy",
                        containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Content-Security-Policy",
                        containsString("script-src 'self';")))
                .andExpect(header().string("Content-Security-Policy",
                        not(containsString("script-src 'self' 'unsafe-inline'"))))
                .andExpect(header().string("Permissions-Policy",
                        containsString("geolocation=()")));
    }

    @Test
    void hstsEmitidoEnHttps() throws Exception {
        mockMvc.perform(post("/api/v1/push/test").secure(true))
                .andExpect(header().string("Strict-Transport-Security",
                        containsString("max-age=31536000")));
    }
}
