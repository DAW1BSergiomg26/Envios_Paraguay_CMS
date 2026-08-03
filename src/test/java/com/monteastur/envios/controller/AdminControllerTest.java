package com.monteastur.envios.controller;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.BatchImportEstado;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.repository.MensajeContactoRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.TextoLegalRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.DocumentoPdfService;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.EnvioTrackingService;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test",
    "app.upload.dir=./uploads"
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ReservaRepository reservaRepo;
    @MockBean private ImagenRepository imagenRepo;
    @MockBean private MensajeContactoRepository mensajeRepo;
    @MockBean private TextoLegalRepository textoRepo;
    @MockBean private EnvioTrackingRepository trackingRepo;
    @MockBean private EmailService emailService;
    @MockBean private ClienteRepository clienteRepo;
    @MockBean private EvidenciaEnvioService evidenciaService;
    @MockBean private EventoTrackingService eventoTrackingService;
    @MockBean private EnvioTrackingService envioTrackingService;
    @MockBean private BatchImportPersistenceService batchImportPersistenceService;
    @MockBean private DocumentoPdfService documentoPdfService;
    @MockBean private RBACAccessLogger rbacAccessLogger;
    @MockBean private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockBean private DataSource dataSource;

    @Test
    void imports_returnsViewWithClientesAndLotes() throws Exception {
        when(clienteRepo.findAll()).thenReturn(List.of(new Cliente()));
        when(batchImportPersistenceService.listarLotes())
                .thenReturn(List.of(new BatchImport(1L, "envios.csv", BatchImportEstado.COMPLETADO)));

        mockMvc.perform(get("/admin/imports").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cms/imports"))
                .andExpect(model().attributeExists("clientes", "lotes"));
    }

    @Test
    void documentos_returnsViewWithModel() throws Exception {
        when(trackingRepo.findAllByOrderByUltimaActualizacionDesc()).thenReturn(List.of(new EnvioTracking()));
        when(batchImportPersistenceService.listarLotes()).thenReturn(List.of());
        when(documentoPdfService.listarEmisiones(null)).thenReturn(List.of());

        mockMvc.perform(get("/admin/documentos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cms/documentos"))
                .andExpect(model().attributeExists("envios", "lotes", "emisiones"));
    }
}
