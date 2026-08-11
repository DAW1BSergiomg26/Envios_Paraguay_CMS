package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.EvidenciaEnvio;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EventoTrackingRepository;
import com.monteastur.envios.repository.EvidenciaEnvioRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import com.monteastur.envios.service.EnvioTrackingService;
import com.monteastur.envios.service.UploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnvioApiController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "admin", roles = {"ADMIN"})
class EnvioApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnvioTrackingRepository trackingRepo;

    @MockBean
    private EventoTrackingRepository eventoTrackingRepo;

    @MockBean
    private EvidenciaEnvioRepository evidenciaEnvioRepo;

    @MockBean
    private ClienteRepository clienteRepo;

    @MockBean
    private EventoTrackingService eventoTrackingService;

    @MockBean
    private EnvioTrackingService envioTrackingService;

    @MockBean
    private EvidenciaEnvioService evidenciaService;

    @MockBean
    private UploadService uploadService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler accessDenied;

    private EnvioTracking envio(String codigo, String estado, String destinatario) {
        EnvioTracking envio = new EnvioTracking(codigo, estado, destinatario,
                "Asturias, España", "Asunción, Paraguay", "1kg", "Documentos");
        envio.setId(1L);
        return envio;
    }

    private EvidenciaEnvio evidencia(Long id, String titulo, String tipo, String url) {
        EvidenciaEnvio ev = new EvidenciaEnvio();
        ev.setId(id);
        ev.setTitulo(titulo);
        ev.setDescripcion("desc " + titulo);
        ev.setTipo(tipo);
        ev.setUrlArchivo(url);
        ev.setVisibleCliente(true);
        ev.setFechaSubida(LocalDateTime.now());
        return ev;
    }

    private void stubDetalle() {
        when(eventoTrackingService.listarPorEnvio(anyLong())).thenReturn(List.of());
        when(evidenciaService.listarPorEnvio(anyLong())).thenReturn(List.of());
    }

    @Test
    void crear_201_retornaTrackingDto() throws Exception {
        EnvioTracking envio = envio("MT-2026-0001", "RECIBIDO", "María");
        stubDetalle();
        when(envioTrackingService.crear(any(EnvioTracking.class))).thenReturn(envio);

        mockMvc.perform(post("/api/v1/admin/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RECIBIDO\",\"destinatario\":\"María\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoUnico").value("MT-2026-0001"))
                .andExpect(jsonPath("$.destinatario").value("María"));
    }

    @Test
    void crear_400_destinatarioVacio() throws Exception {
        mockMvc.perform(post("/api/v1/admin/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RECIBIDO\",\"destinatario\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_400_codigoDuplicado() throws Exception {
        when(trackingRepo.existsByCodigoUnico("MT-2026-0001")).thenReturn(true);

        mockMvc.perform(post("/api/v1/admin/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoUnico\":\"MT-2026-0001\",\"estado\":\"RECIBIDO\",\"destinatario\":\"María\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_generaCodigoCuandoVieneVacio() throws Exception {
        EnvioTracking envio = envio("MT-2026-0001", "RECIBIDO", "María");
        stubDetalle();
        when(envioTrackingService.generarCodigo()).thenReturn("MT-2026-0001");
        when(envioTrackingService.crear(any(EnvioTracking.class))).thenReturn(envio);

        mockMvc.perform(post("/api/v1/admin/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RECIBIDO\",\"destinatario\":\"María\"}"))
                .andExpect(status().isCreated());

        verify(envioTrackingService).generarCodigo();
    }

    @Test
    void actualizar_200() throws Exception {
        EnvioTracking envio = envio("MT-2026-0001", "RECIBIDO", "María");
        stubDetalle();
        when(trackingRepo.findWithClienteByCodigoUnico("MT-2026-0001")).thenReturn(Optional.of(envio));
        when(envioTrackingService.guardar(any(EnvioTracking.class))).thenReturn(envio);

        mockMvc.perform(put("/api/v1/admin/envios/MT-2026-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatario\":\"Nuevo nombre\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinatario").value("Nuevo nombre"));
    }

    @Test
    void actualizar_404() throws Exception {
        when(trackingRepo.findWithClienteByCodigoUnico("MT-2026-0001")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/admin/envios/MT-2026-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatario\":\"Nuevo nombre\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_204() throws Exception {
        EnvioTracking envio = envio("MT-2026-0001", "RECIBIDO", "María");
        when(trackingRepo.findWithClienteByCodigoUnico("MT-2026-0001")).thenReturn(Optional.of(envio));

        mockMvc.perform(delete("/api/v1/admin/envios/MT-2026-0001"))
                .andExpect(status().isNoContent());

        verify(envioTrackingService).eliminar(anyLong());
    }

    @Test
    void eliminar_404() throws Exception {
        when(trackingRepo.findWithClienteByCodigoUnico("MT-2026-0001")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/admin/envios/MT-2026-0001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void subirEvidencia_201() throws Exception {
        EnvioTracking envio = envio("MT-2026-0001", "RECIBIDO", "María");
        EvidenciaEnvio ev = evidencia(1L, "Factura", "DOCUMENTO", "/uploads/evidencias/uuid.pdf");
        when(trackingRepo.findWithClienteByCodigoUnico("MT-2026-0001")).thenReturn(Optional.of(envio));
        when(uploadService.subirArchivo(any(MultipartFile.class), anyString(), any(String[].class)))
                .thenReturn("evidencias/uuid.pdf");
        when(evidenciaService.guardar(any(EvidenciaEnvio.class))).thenReturn(ev);

        MockMultipartFile archivo = new MockMultipartFile("archivo", "evidencia.pdf",
                MediaType.APPLICATION_PDF_VALUE, "pdf-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/envios/MT-2026-0001/evidencias")
                        .file(archivo)
                        .param("titulo", "Factura")
                        .param("tipo", "DOCUMENTO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("DOCUMENTO"))
                .andExpect(jsonPath("$.urlArchivo").value("/uploads/evidencias/uuid.pdf"));
    }

    @Test
    @WithAnonymousUser
    void sinAutenticacion_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/envios"))
                .andExpect(status().isUnauthorized());
    }
}
