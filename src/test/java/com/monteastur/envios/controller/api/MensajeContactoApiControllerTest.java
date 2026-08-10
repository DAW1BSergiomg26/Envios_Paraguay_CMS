package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.MensajeContactoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MensajeContactoApiController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
@WithMockUser(username = "admin", roles = "ADMIN")
class MensajeContactoApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MensajeContactoService mensajeContactoService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private MensajeContacto mensaje(Long id, String nombre, boolean leido) {
        MensajeContacto m = new MensajeContacto(nombre, nombre + "@example.com", "+34 600 000 000", "Mensaje de prueba");
        m.setId(id);
        m.setLeido(leido);
        m.setFechaEnvio(LocalDateTime.of(2026, 8, 1, 10, 0));
        return m;
    }

    @Test
    void listar_retorna200_conLista() throws Exception {
        when(mensajeContactoService.listar(null))
            .thenReturn(List.of(mensaje(1L, "Ana", false), mensaje(2L, "Luis", true)));

        mockMvc.perform(get("/api/v1/admin/mensajes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].nombre").value("Ana"))
            .andExpect(jsonPath("$[0].leido").value(false));
    }

    @Test
    void listar_conFiltroLeido_pasaElParametro() throws Exception {
        when(mensajeContactoService.listar(true))
            .thenReturn(List.of(mensaje(2L, "Luis", true)));

        mockMvc.perform(get("/api/v1/admin/mensajes").param("leido", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void marcarLeido_retorna200_conDto() throws Exception {
        MensajeContacto m = mensaje(1L, "Ana", false);
        when(mensajeContactoService.marcarLeido(1L, true)).thenReturn(Optional.of(m));

        mockMvc.perform(patch("/api/v1/admin/mensajes/1/leido")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"leido\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    @Test
    void marcarLeido_inexistente_retorna404() throws Exception {
        when(mensajeContactoService.marcarLeido(99L, true))
            .thenThrow(new ResourceNotFoundException("Mensaje no encontrado: 99"));

        mockMvc.perform(patch("/api/v1/admin/mensajes/99/leido")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"leido\":true}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Mensaje no encontrado: 99"));
    }

    @Test
    void eliminar_retorna204() throws Exception {
        when(mensajeContactoService.buscarPorId(1L))
            .thenReturn(Optional.of(mensaje(1L, "Ana", false)));

        mockMvc.perform(delete("/api/v1/admin/mensajes/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_inexistente_retorna404() throws Exception {
        when(mensajeContactoService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/admin/mensajes/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }
}
