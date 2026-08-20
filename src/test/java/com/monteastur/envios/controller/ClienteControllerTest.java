package com.monteastur.envios.controller;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ClienteController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private RBACAccessLogger rbacAccessLogger;

    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private Cliente cliente() {
        Cliente cliente = new Cliente("cliente@test.com", "x", "Cliente Uno", null);
        cliente.setId(7L);
        return cliente;
    }

    @Test
    void indexSinSesion_redirigeAlLoginConRedirect() throws Exception {
        mockMvc.perform(get("/cliente"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/login?redirect=/cliente/panel"));
    }

    @Test
    void indexConSesion_redirigeAlPanel() throws Exception {
        mockMvc.perform(get("/cliente").sessionAttr("clienteId", 7L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/panel"));
    }

    @Test
    void loginSinSesion_muestraFormularioConRedirectPorDefecto() throws Exception {
        mockMvc.perform(get("/cliente/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/login"))
                .andExpect(model().attribute("redirect", "/cliente/panel"));
    }

    @Test
    void loginConRedirectConsulta_loMantieneEnElModelo() throws Exception {
        mockMvc.perform(get("/cliente/login").param("redirect", "/cliente/panel"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("redirect", "/cliente/panel"));
    }

    @Test
    void loginConRedirectExterno_loSanea() throws Exception {
        mockMvc.perform(get("/cliente/login").param("redirect", "https://evil.com"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("redirect", "/cliente/panel"));
    }

    @Test
    void loginConSesion_redirigeAlPanel() throws Exception {
        mockMvc.perform(get("/cliente/login").sessionAttr("clienteId", 7L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/panel"));
    }

    @Test
    void doLoginCorrecto_redirigeAlPanel() throws Exception {
        when(clienteService.autenticar("cliente@test.com", "secreto")).thenReturn(Optional.of(cliente()));
        mockMvc.perform(post("/cliente/login")
                        .param("email", "cliente@test.com")
                        .param("password", "secreto")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/panel"));
    }

    @Test
    void doLoginCorrectoConRedirect_loRespeta() throws Exception {
        when(clienteService.autenticar("cliente@test.com", "secreto")).thenReturn(Optional.of(cliente()));
        mockMvc.perform(post("/cliente/login")
                        .param("email", "cliente@test.com")
                        .param("password", "secreto")
                        .param("redirect", "/cliente/panel")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/panel"));
    }

    @Test
    void doLoginConRedirectExterno_loSanea() throws Exception {
        when(clienteService.autenticar("cliente@test.com", "secreto")).thenReturn(Optional.of(cliente()));
        mockMvc.perform(post("/cliente/login")
                        .param("email", "cliente@test.com")
                        .param("password", "secreto")
                        .param("redirect", "https://evil.com")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/panel"));
    }

    @Test
    void doLoginFallido_redirigeAlLoginConError() throws Exception {
        when(clienteService.autenticar("cliente@test.com", "mal")).thenReturn(Optional.empty());
        mockMvc.perform(post("/cliente/login")
                        .param("email", "cliente@test.com")
                        .param("password", "mal")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/login"))
                .andExpect(flash().attribute("error", "Email o contraseña incorrectos."));
    }
}
