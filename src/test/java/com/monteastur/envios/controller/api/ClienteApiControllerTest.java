package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.EvidenciaEnvio;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.ClienteService;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteApiController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test",
    "app.upload.dir=src/test/resources/uploads"
})
class ClienteApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnvioTrackingRepository trackingRepo;

    @MockBean
    private ClienteService clienteService;

    @MockBean
    private EvidenciaEnvioService evidenciaService;

    @MockBean
    private EventoTrackingService eventoTrackingService;

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

    private EnvioTracking envio(boolean propio) {
        EnvioTracking envio = new EnvioTracking("MT-1", "RECIBIDO", "Destinatario",
                "Asturias, España", "Asunción, Paraguay", "10 kg", "Documentos");
        envio.setId(1L);
        Cliente otro = new Cliente("otro@test.com", "x", "Otro", null);
        otro.setId(99L);
        envio.setCliente(propio ? cliente() : otro);
        return envio;
    }

    private EvidenciaEnvio evidencia(EnvioTracking envio, boolean visible, String url) {
        EvidenciaEnvio evidencia = new EvidenciaEnvio();
        evidencia.setId(1L);
        evidencia.setEnvioTracking(envio);
        evidencia.setVisibleCliente(visible);
        evidencia.setUrlArchivo(url);
        evidencia.setTitulo("Evidencia");
        evidencia.setTipo("FOTO");
        return evidencia;
    }

    @Test
    void listarEnvios_sinSesion_retorna401Json() throws Exception {
        mockMvc.perform(get("/api/v1/cliente/envios")
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void listarEnvios_conSesion_retorna200Lista() throws Exception {
        when(trackingRepo.findByClienteIdOrderByUltimaActualizacionDesc(7L))
                .thenReturn(java.util.List.of(envio(true)));

        mockMvc.perform(get("/api/v1/cliente/envios")
                        .sessionAttr("clienteId", 7L)
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("MT-1"))
                .andExpect(jsonPath("$[0].estado").value("RECIBIDO"));
    }

    @Test
    void detalleEnvio_envioPropio_retorna200() throws Exception {
        when(trackingRepo.findWithClienteByCodigoUnico("MT-1"))
                .thenReturn(java.util.Optional.of(envio(true)));
        when(eventoTrackingService.listarPorEnvio(1L)).thenReturn(java.util.List.of());
        when(evidenciaService.listarPorEnvioParaCliente(1L)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/cliente/envios/MT-1")
                        .sessionAttr("clienteId", 7L)
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoUnico").value("MT-1"))
                .andExpect(jsonPath("$.estado").value("RECIBIDO"));
    }

    @Test
    void detalleEnvio_envioInexistente_retorna404() throws Exception {
        when(trackingRepo.findWithClienteByCodigoUnico("MT-NOPE"))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/v1/cliente/envios/MT-NOPE")
                        .sessionAttr("clienteId", 7L)
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void detalleEnvio_envioAjeno_retorna403() throws Exception {
        when(trackingRepo.findWithClienteByCodigoUnico("MT-1"))
                .thenReturn(java.util.Optional.of(envio(false)));

        mockMvc.perform(get("/api/v1/cliente/envios/MT-1")
                        .sessionAttr("clienteId", 7L)
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void descargarEvidencia_evidenciaNoVisible_retorna403() throws Exception {
        when(evidenciaService.buscar(1L))
                .thenReturn(java.util.Optional.of(evidencia(envio(true), false, "/uploads/evidencias/ok.txt")));

        mockMvc.perform(get("/api/v1/cliente/evidencias/1/archivo")
                        .sessionAttr("clienteId", 7L)
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void descargarEvidencia_envioAjeno_retorna403() throws Exception {
        when(evidenciaService.buscar(1L))
                .thenReturn(java.util.Optional.of(evidencia(envio(false), true, "/uploads/evidencias/ok.txt")));

        mockMvc.perform(get("/api/v1/cliente/evidencias/1/archivo")
                        .sessionAttr("clienteId", 7L)
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void descargarEvidencia_inexistente_retorna404() throws Exception {
        when(evidenciaService.buscar(1L)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/v1/cliente/evidencias/1/archivo")
                        .sessionAttr("clienteId", 7L)
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void descargarEvidencia_pathTraversal_retorna403() throws Exception {
        when(evidenciaService.buscar(1L))
                .thenReturn(java.util.Optional.of(evidencia(envio(true), true, "/uploads/evidencias/../secret")));

        mockMvc.perform(get("/api/v1/cliente/evidencias/1/archivo")
                        .sessionAttr("clienteId", 7L)
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void descargarEvidencia_archivoNoLegible_retorna404() throws Exception {
        when(evidenciaService.buscar(1L))
                .thenReturn(java.util.Optional.of(evidencia(envio(true), true, "/uploads/evidencias/no-existe.txt")));

        mockMvc.perform(get("/api/v1/cliente/evidencias/1/archivo")
                        .sessionAttr("clienteId", 7L)
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void descargarEvidencia_archivoValido_retorna200() throws Exception {
        when(evidenciaService.buscar(1L))
                .thenReturn(java.util.Optional.of(evidencia(envio(true), true, "/uploads/evidencias/evidencia-test.txt")));

        mockMvc.perform(get("/api/v1/cliente/evidencias/1/archivo")
                        .sessionAttr("clienteId", 7L)
                        .accept(org.springframework.http.MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain"));
    }

    @Test
    void descargarEvidencia_sinSesion_retorna401Json() throws Exception {
        mockMvc.perform(get("/api/v1/cliente/evidencias/1/archivo")
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void detalleEnvio_sinSesion_retorna401Json() throws Exception {
        mockMvc.perform(get("/api/v1/cliente/envios/MT-1")
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
