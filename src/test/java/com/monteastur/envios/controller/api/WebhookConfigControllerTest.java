package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.model.WebhookConfig;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.WebhookConfigRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookConfigController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "admin", roles = "ADMIN")
class WebhookConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebhookConfigRepository webhookConfigRepository;

    @MockBean
    private ClienteRepository clienteRepository;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private WebhookConfig config(String url, String secret) {
        WebhookConfig config = new WebhookConfig(1L, url, secret);
        config.setId(10L);
        return config;
    }

    @Test
    void listar_retornaWebhooksSinExponerSecretToken() throws Exception {
        when(webhookConfigRepository.findAll())
                .thenReturn(List.of(config("https://hook.example.com/x", "s3cr3t")));

        mockMvc.perform(get("/api/v1/admin/webhooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].clienteId").value(1))
                .andExpect(jsonPath("$[0].url").value("https://hook.example.com/x"))
                .andExpect(jsonPath("$[0].activo").value(true))
                .andExpect(jsonPath("$[0].secretToken").doesNotExist());
    }

    @Test
    void listar_conClienteId_filtraPorCliente() throws Exception {
        when(webhookConfigRepository.findByClienteId(1L))
                .thenReturn(List.of(config("https://hook.example.com/x", "s3cr3t")));

        mockMvc.perform(get("/api/v1/admin/webhooks").param("clienteId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clienteId").value(1));
    }

    @Test
    void crear_retorna201SinExponerSecretToken() throws Exception {
        when(clienteRepository.existsById(1L)).thenReturn(true);
        when(webhookConfigRepository.save(any(WebhookConfig.class)))
                .thenAnswer(invocation -> {
                    WebhookConfig c = invocation.getArgument(0);
                    c.setId(10L);
                    return c;
                });

        mockMvc.perform(post("/api/v1/admin/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"url\":\"https://hook.example.com/x\",\"secretToken\":\"s3cr3t\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.url").value("https://hook.example.com/x"))
                .andExpect(jsonPath("$.secretToken").doesNotExist());
    }

    @Test
    void crear_clienteInexistente_retorna404() throws Exception {
        when(clienteRepository.existsById(99L)).thenReturn(false);

        mockMvc.perform(post("/api/v1/admin/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":99,\"url\":\"https://hook.example.com/x\",\"secretToken\":\"s3cr3t\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void crear_urlVacia_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"url\":\"\",\"secretToken\":\"s3cr3t\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void crear_urlConEsquemaInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"url\":\"ftp://hook.example.com/x\",\"secretToken\":\"s3cr3t\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void eliminar_retorna204() throws Exception {
        when(webhookConfigRepository.existsById(10L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/admin/webhooks/10"))
                .andExpect(status().isNoContent());

        verify(webhookConfigRepository).deleteById(10L);
    }

    @Test
    void eliminar_noEncontrado_retorna404() throws Exception {
        when(webhookConfigRepository.existsById(10L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/admin/webhooks/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithAnonymousUser
    void sinAutenticacion_redirigeAlLogin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/webhooks"))
                .andExpect(status().is3xxRedirection());
    }
}
