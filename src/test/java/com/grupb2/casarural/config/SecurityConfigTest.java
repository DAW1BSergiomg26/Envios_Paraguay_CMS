package com.grupb2.casarural.config;

import com.grupb2.casarural.controller.api.PushSubscriptionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PushSubscriptionController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiPublico_accesibleSinAuth() throws Exception {
        mockMvc.perform(post("/api/v1/push/test"))
                .andExpect(status().isOk());
    }

    @Test
    void adminSinAuth_redirigeALogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void apiAdminSinAuth_redirigeALogin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/envios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
