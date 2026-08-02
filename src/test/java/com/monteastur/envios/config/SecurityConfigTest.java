package com.monteastur.envios.config;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.controller.api.PushSubscriptionController;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PushSubscriptionController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

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
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void apiAdminSinAuth_redirigeALogin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/envios")
                .accept(org.springframework.http.MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void deliveriesSinAuth_devuelve401Json() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/MT-1/pod")
                .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
