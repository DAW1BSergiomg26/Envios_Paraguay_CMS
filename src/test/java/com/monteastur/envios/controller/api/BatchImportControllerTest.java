package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.BatchImportEstado;
import com.monteastur.envios.model.BatchImportError;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.CsvBatchImportService;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
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

import javax.sql.DataSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchImportController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "admin", roles = "ADMIN")
class BatchImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BatchImportPersistenceService persistence;

    @MockBean
    private CsvBatchImportService csvBatchImportService;

    @MockBean
    private ClienteRepository clienteRepository;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private MockMultipartFile ficheroCsv(String contenido) {
        return new MockMultipartFile("file", "envios.csv", "text/csv",
                contenido.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importar_retorna202ConBatchId() throws Exception {
        BatchImport lote = new BatchImport(null, "envios.csv", BatchImportEstado.PENDIENTE);
        lote.setId(42L);
        when(persistence.crearLote(isNull(), any())).thenReturn(lote);

        mockMvc.perform(multipart("/api/v1/admin/imports/csv")
                        .file(ficheroCsv("codigo,estado,destinatario\nMT-1,RECIBIDO,María")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        verify(csvBatchImportService).procesarLote(anyLong(), any(), any());
    }

    @Test
    void importar_conClienteId_validaExistencia() throws Exception {
        when(clienteRepository.existsById(7L)).thenReturn(true);
        BatchImport lote = new BatchImport(7L, "envios.csv", BatchImportEstado.PENDIENTE);
        lote.setId(43L);
        when(persistence.crearLote(7L, "envios.csv")).thenReturn(lote);

        mockMvc.perform(multipart("/api/v1/admin/imports/csv")
                        .file(ficheroCsv("codigo,estado,destinatario\nMT-1,RECIBIDO,María"))
                        .param("clienteId", "7"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(43));
    }

    @Test
    void importar_sinFichero_retorna400() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/imports/csv"))
                .andExpect(status().isBadRequest());
        verify(persistence, never()).crearLote(any(), any());
    }

    @Test
    void importar_ficheroVacio_retorna400() throws Exception {
        MockMultipartFile vacio = new MockMultipartFile("file", "envios.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/v1/admin/imports/csv").file(vacio))
                .andExpect(status().isBadRequest());
        verify(persistence, never()).crearLote(any(), any());
    }

    @Test
    void importar_extensionInvalida_retorna400() throws Exception {
        MockMultipartFile txt = new MockMultipartFile("file", "envios.txt", "text/plain",
                "hola".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/admin/imports/csv").file(txt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verify(persistence, never()).crearLote(any(), any());
    }

    @Test
    void importar_clienteInexistente_retorna404() throws Exception {
        when(clienteRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(multipart("/api/v1/admin/imports/csv")
                        .file(ficheroCsv("codigo,estado,destinatario\nMT-1,RECIBIDO,María"))
                        .param("clienteId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        verify(csvBatchImportService, never()).procesarLote(anyLong(), any(), any());
    }

    @Test
    void estado_retorna200ConContadores() throws Exception {
        BatchImport lote = new BatchImport(null, "envios.csv", BatchImportEstado.EN_PROCESO);
        lote.setId(10L);
        lote.setProcesados(50);
        lote.setExitosos(48);
        lote.setFallidos(2);
        when(persistence.obtenerLote(10L)).thenReturn(lote);

        mockMvc.perform(get("/api/v1/admin/imports/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.procesados").value(50))
                .andExpect(jsonPath("$.exitosos").value(48))
                .andExpect(jsonPath("$.fallidos").value(2))
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));
    }

    @Test
    void estado_loteInexistente_retorna404() throws Exception {
        when(persistence.obtenerLote(999L))
                .thenThrow(new com.monteastur.envios.exception.ResourceNotFoundException("Lote de importación no encontrado: 999"));

        mockMvc.perform(get("/api/v1/admin/imports/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void errores_retorna200ConLineas() throws Exception {
        when(persistence.obtenerLote(10L)).thenReturn(new BatchImport(null, "envios.csv", BatchImportEstado.COMPLETADO_CON_ERRORES));
        BatchImportError error = new BatchImportError(10L, 5, "MT-1", "estado no válido");
        when(persistence.listarErrores(10L)).thenReturn(List.of(error));

        mockMvc.perform(get("/api/v1/admin/imports/10/errors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lineaNumero").value(5))
                .andExpect(jsonPath("$[0].codigoRastreo").value("MT-1"))
                .andExpect(jsonPath("$[0].errorMensaje").value("estado no válido"));
    }

    @Test
    void listarLotes_retorna200ConLotes() throws Exception {
        BatchImport lote = new BatchImport(7L, "envios.csv", BatchImportEstado.COMPLETADO);
        lote.setId(10L);
        lote.setProcesados(50);
        lote.setExitosos(48);
        lote.setFallidos(2);
        when(persistence.listarLotes()).thenReturn(List.of(lote));

        mockMvc.perform(get("/api/v1/admin/imports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].nombreArchivo").value("envios.csv"))
                .andExpect(jsonPath("$[0].estado").value("COMPLETADO"))
                .andExpect(jsonPath("$[0].exitosos").value(48));
    }

    @Test
    void listarLotes_sinLotes_retorna200ListaVacia() throws Exception {
        when(persistence.listarLotes()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/imports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithAnonymousUser
    void sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/imports/csv")
                        .file(ficheroCsv("codigo,estado,destinatario\nMT-1,RECIBIDO,María"))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void listarLotes_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/imports"))
                .andExpect(status().isUnauthorized());
    }
}
