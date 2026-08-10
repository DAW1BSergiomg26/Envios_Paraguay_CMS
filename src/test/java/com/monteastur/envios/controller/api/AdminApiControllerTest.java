package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import com.monteastur.envios.service.EnvioTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminApiController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "admin", roles = "ADMIN")
class AdminApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnvioTrackingRepository trackingRepo;

    @MockBean
    private EvidenciaEnvioService evidenciaService;

    @MockBean
    private EventoTrackingService eventoTrackingService;

    @MockBean
    private EnvioTrackingService envioTrackingService;

    @MockBean
    private ClienteRepository clienteRepository;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private Cliente cliente(Long id, String nombre, String email) {
        Cliente cliente = new Cliente(email, "secreto-no-expuesto", nombre, "600111222");
        cliente.setId(id);
        return cliente;
    }

    @Test
    void listarClientes_retorna200SinDatosSensibles() throws Exception {
        when(clienteRepository.findAll()).thenReturn(List.of(
                cliente(7L, "Cliente Uno", "cliente@test.com"),
                cliente(8L, "Cliente Dos", "dos@test.com")));

        mockMvc.perform(get("/api/v1/admin/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].nombre").value("Cliente Uno"))
                .andExpect(jsonPath("$[1].id").value(8))
                .andExpect(jsonPath("$[1].nombre").value("Cliente Dos"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].telefono").doesNotExist());
    }

    @Test
    void listarClientes_sinClientes_retorna200ListaVacia() throws Exception {
        when(clienteRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithAnonymousUser
    void listarClientes_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/clientes"))
                .andExpect(status().isUnauthorized());
    }
}
