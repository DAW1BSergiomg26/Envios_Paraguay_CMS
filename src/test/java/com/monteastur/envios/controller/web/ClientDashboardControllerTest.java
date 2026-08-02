package com.monteastur.envios.controller.web;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.dto.web.ClientDashboardView;
import com.monteastur.envios.dto.web.EnvioResumenView;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.ClienteService;
import com.monteastur.envios.service.DocumentoPdfService;
import com.monteastur.envios.service.web.ClientDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ClientDashboardController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class ClientDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientDashboardService dashboardService;

    @MockBean
    private DocumentoPdfService documentoPdfService;

    @MockBean
    private EnvioTrackingRepository envioTrackingRepository;

    @MockBean
    private ClienteService clienteService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private Cliente cliente() {
        Cliente cliente = new Cliente("cliente@test.com", "x", "Cliente Uno", null);
        cliente.setId(7L);
        return cliente;
    }

    private ClientDashboardView viewValida() {
        EnvioResumenView resumen = new EnvioResumenView(1L, "MT-1", "RECIBIDO",
                "Asunción, Paraguay", "Documentos", "10 kg", LocalDateTime.now(), null);
        return new ClientDashboardView(7L, "Cliente Uno", "cliente@test.com",
                1, 1, 0, 10.0, 10.0, new ArrayList<>(List.of(resumen)));
    }

    private EnvioTracking envio(boolean propio) {
        EnvioTracking envio = new EnvioTracking("MT-1", "RECIBIDO", "Destinatario",
                "Asturias, España", "Asunción, Paraguay", "10 kg", "Documentos");
        envio.setId(1L);
        envio.setCliente(propio ? cliente() : new Cliente("otro@test.com", "x", "Otro", null));
        return envio;
    }

    @Test
    void panel_sinSesion_redirigeLogin() throws Exception {
        mockMvc.perform(get("/cliente/panel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/login"));
    }

    @Test
    void panel_conSesion_retornaDashboard() throws Exception {
        when(clienteService.buscarPorId(7L)).thenReturn(Optional.of(cliente()));
        when(dashboardService.cargarDashboard(7L)).thenReturn(viewValida());

        mockMvc.perform(get("/cliente/panel").sessionAttr("clienteId", 7L))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/panel"))
                .andExpect(model().attributeExists("panel"));
    }

    @Test
    void panel_clienteInexistente_redirigeLogin() throws Exception {
        when(clienteService.buscarPorId(7L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/cliente/panel").sessionAttr("clienteId", 7L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/login"));
    }

    @Test
    void etiqueta_sinSesion_redirigeLogin() throws Exception {
        mockMvc.perform(get("/cliente/panel/envio/MT-1/etiqueta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cliente/login"));
    }

    @Test
    void etiqueta_envioPropio_retornaPdf() throws Exception {
        when(clienteService.buscarPorId(7L)).thenReturn(Optional.of(cliente()));
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1"))
                .thenReturn(Optional.of(envio(true)));
        when(documentoPdfService.generarEtiqueta("MT-1", "cliente:cliente@test.com"))
                .thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(get("/cliente/panel/envio/MT-1/etiqueta").sessionAttr("clienteId", 7L))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"etiqueta-MT-1.pdf\""))
                .andExpect(content().bytes(new byte[]{'%', 'P', 'D', 'F'}));
    }

    @Test
    void etiqueta_envioAjeno_retorna403() throws Exception {
        when(clienteService.buscarPorId(7L)).thenReturn(Optional.of(cliente()));
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1"))
                .thenReturn(Optional.of(envio(false)));

        mockMvc.perform(get("/cliente/panel/envio/MT-1/etiqueta").sessionAttr("clienteId", 7L))
                .andExpect(status().isForbidden());
    }

    @Test
    void etiqueta_envioInexistente_retorna404() throws Exception {
        when(clienteService.buscarPorId(7L)).thenReturn(Optional.of(cliente()));
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-NOPE"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/cliente/panel/envio/MT-NOPE/etiqueta").sessionAttr("clienteId", 7L))
                .andExpect(status().isNotFound());
    }
}
