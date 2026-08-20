package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.model.WebhookConfig;
import com.monteastur.envios.model.WebhookLog;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.WebhookConfigRepository;
import com.monteastur.envios.repository.WebhookLogRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookConfigController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "admin", roles = "ADMIN")
class WebhookConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookConfigRepository webhookConfigRepository;

    @MockitoBean
    private ClienteRepository clienteRepository;

    @MockitoBean
    private WebhookLogRepository webhookLogRepository;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private RBACAccessLogger rbacAccessLogger;

    @MockitoBean
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
    void sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/webhooks"))
                .andExpect(status().isUnauthorized());
    }

    private WebhookLog log(boolean exitoso, Integer responseStatus) {
        WebhookLog log = new WebhookLog(10L, 1L, "{\"envioId\":1}", responseStatus, exitoso, exitoso ? null : "timeout");
        log.setId(50L);
        return log;
    }

    @Test
    void actualizar_retorna200ActualizaUrlYActivo() throws Exception {
        WebhookConfig config = config("https://hook.example.com/x", "s3cr3t");
        when(webhookConfigRepository.findById(10L)).thenReturn(Optional.of(config));
        when(webhookConfigRepository.save(any(WebhookConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/admin/webhooks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://new.example.com\",\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://new.example.com"))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.secretToken").doesNotExist());

        ArgumentCaptor<WebhookConfig> captor = ArgumentCaptor.forClass(WebhookConfig.class);
        verify(webhookConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getUrl()).isEqualTo("https://new.example.com");
        assertThat(captor.getValue().isActivo()).isFalse();
        assertThat(captor.getValue().getSecretToken()).isEqualTo("s3cr3t");
    }

    @Test
    void actualizar_secretTokenEnBlanco_noBorraSecreto() throws Exception {
        WebhookConfig config = config("https://hook.example.com/x", "s3cr3t");
        when(webhookConfigRepository.findById(10L)).thenReturn(Optional.of(config));
        when(webhookConfigRepository.save(any(WebhookConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/admin/webhooks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secretToken\":\"  \"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<WebhookConfig> captor = ArgumentCaptor.forClass(WebhookConfig.class);
        verify(webhookConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getSecretToken()).isEqualTo("s3cr3t");
    }

    @Test
    void actualizar_secretTokenNuevo_loActualiza() throws Exception {
        WebhookConfig config = config("https://hook.example.com/x", "s3cr3t");
        when(webhookConfigRepository.findById(10L)).thenReturn(Optional.of(config));
        when(webhookConfigRepository.save(any(WebhookConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/admin/webhooks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secretToken\":\"nuevo-secreto\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<WebhookConfig> captor = ArgumentCaptor.forClass(WebhookConfig.class);
        verify(webhookConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getSecretToken()).isEqualTo("nuevo-secreto");
    }

    @Test
    void actualizar_urlInvalida_retorna400() throws Exception {
        when(webhookConfigRepository.findById(10L))
                .thenReturn(Optional.of(config("https://hook.example.com/x", "s3cr3t")));

        mockMvc.perform(put("/api/v1/admin/webhooks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"ftp://x/y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void actualizar_noEncontrado_retorna404() throws Exception {
        when(webhookConfigRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/admin/webhooks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://new.example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void logs_retornaListaSinPayload() throws Exception {
        when(webhookConfigRepository.existsById(10L)).thenReturn(true);
        when(webhookLogRepository.findByWebhookIdOrderByFechaCreacionDesc(10L))
                .thenReturn(List.of(log(true, 200), log(false, 500)));

        mockMvc.perform(get("/api/v1/admin/webhooks/10/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[0].webhookId").value(10))
                .andExpect(jsonPath("$[0].envioId").value(1))
                .andExpect(jsonPath("$[0].exitoso").value(true))
                .andExpect(jsonPath("$[0].responseStatus").value(200))
                .andExpect(jsonPath("$[0].payload").doesNotExist());
    }

    @Test
    void logs_filtraPorExitoso() throws Exception {
        when(webhookConfigRepository.existsById(10L)).thenReturn(true);
        when(webhookLogRepository.findByWebhookIdOrderByFechaCreacionDesc(10L))
                .thenReturn(List.of(log(true, 200), log(false, 500)));

        mockMvc.perform(get("/api/v1/admin/webhooks/10/logs").param("exitoso", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].exitoso").value(true));
    }

    @Test
    void logs_webhookNoEncontrado_retorna404() throws Exception {
        when(webhookConfigRepository.existsById(99L)).thenReturn(false);

        mockMvc.perform(get("/api/v1/admin/webhooks/99/logs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
