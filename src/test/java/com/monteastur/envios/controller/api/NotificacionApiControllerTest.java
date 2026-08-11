package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.model.Notificacion;
import com.monteastur.envios.repository.NotificacionRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificacionApiController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "admin", roles = "ADMIN")
class NotificacionApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificacionRepository notificacionRepository;

    @MockBean
    private EmailService emailService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private Notificacion notificacion(Long id, Notificacion.EstadoNotificacion estado, String destinatario) {
        Notificacion notificacion = new Notificacion(10L, estado, destinatario,
                "Cambio de estado", "Tu envío cambió de estado", null);
        notificacion.setId(id);
        return notificacion;
    }

    @Test
    void listar_retornaNotificaciones() throws Exception {
        when(notificacionRepository.findAllByOrderByFechaCreacionDesc())
                .thenReturn(List.of(notificacion(1L, Notificacion.EstadoNotificacion.ENVIADO, "maria@correo.com")));

        mockMvc.perform(get("/api/v1/admin/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].estado").value("ENVIADO"))
                .andExpect(jsonPath("$[0].destinatario").value("maria@correo.com"))
                .andExpect(jsonPath("$[0].envioId").value(10));
    }

    @Test
    void listar_filtraPorEstadoValido() throws Exception {
        when(notificacionRepository.findByEstadoOrderByFechaCreacionDesc(Notificacion.EstadoNotificacion.FALLIDO))
                .thenReturn(List.of(notificacion(1L, Notificacion.EstadoNotificacion.FALLIDO, "maria@correo.com")));

        mockMvc.perform(get("/api/v1/admin/notificaciones").param("estado", "FALLIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("FALLIDO"));
    }

    @Test
    void listar_estadoInvalido_retorna400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notificaciones").param("estado", "INVALIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void detalle_retornaNotificacion() throws Exception {
        when(notificacionRepository.findById(1L))
                .thenReturn(Optional.of(notificacion(1L, Notificacion.EstadoNotificacion.ENVIADO, "maria@correo.com")));

        mockMvc.perform(get("/api/v1/admin/notificaciones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.asunto").value("Cambio de estado"));
    }

    @Test
    void detalle_noEncontrado_retorna404() throws Exception {
        when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/admin/notificaciones/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void reintentar_fallidaEmailOk_retorna200YEstadoEnviado() throws Exception {
        Notificacion notificacion = notificacion(1L, Notificacion.EstadoNotificacion.FALLIDO, "maria@correo.com");
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(notificacion));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/admin/notificaciones/1/reintentar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADO"));

        verify(emailService).enviarCorreoSimple("maria@correo.com", "Cambio de estado", "Tu envío cambió de estado");
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(Notificacion.EstadoNotificacion.ENVIADO);
        assertThat(captor.getValue().getErrorMensaje()).isNull();
    }

    @Test
    void reintentar_enviada_retorna409() throws Exception {
        when(notificacionRepository.findById(1L))
                .thenReturn(Optional.of(notificacion(1L, Notificacion.EstadoNotificacion.ENVIADO, "maria@correo.com")));

        mockMvc.perform(post("/api/v1/admin/notificaciones/1/reintentar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void reintentar_noEncontrada_retorna404() throws Exception {
        when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/admin/notificaciones/99/reintentar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void reintentar_sinDestinatario_retorna400() throws Exception {
        when(notificacionRepository.findById(1L))
                .thenReturn(Optional.of(notificacion(1L, Notificacion.EstadoNotificacion.FALLIDO, null)));

        mockMvc.perform(post("/api/v1/admin/notificaciones/1/reintentar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void reintentar_fallaEmail_retorna500YEstadoFallido() throws Exception {
        Notificacion notificacion = notificacion(1L, Notificacion.EstadoNotificacion.FALLIDO, "maria@correo.com");
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(notificacion));
        doThrow(new IllegalStateException("JavaMailSender no configurado"))
                .when(emailService).enviarCorreoSimple(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/admin/notificaciones/1/reintentar"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(Notificacion.EstadoNotificacion.FALLIDO);
        assertThat(captor.getValue().getErrorMensaje()).isNotNull();
    }

    @Test
    @WithAnonymousUser
    void sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notificaciones"))
                .andExpect(status().isUnauthorized());
    }
}
