package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.model.DocumentoGenerado;
import com.monteastur.envios.model.TipoDocumento;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.DocumentoPdfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import java.io.OutputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentosController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "admin", roles = "ADMIN")
class DocumentosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentoPdfService documentoPdfService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Test
    void etiqueta_retorna200PdfInline() throws Exception {
        when(documentoPdfService.generarEtiqueta("MT-1", "admin"))
                .thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(get("/api/v1/admin/documentos/envios/MT-1/etiqueta"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"etiqueta-MT-1.pdf\""))
                .andExpect(content().bytes(new byte[]{'%', 'P', 'D', 'F'}));
    }

    @Test
    void etiqueta_envioInexistente_retorna404() throws Exception {
        when(documentoPdfService.generarEtiqueta("MT-NOPE", "admin"))
                .thenThrow(new com.monteastur.envios.exception.ResourceNotFoundException("Envío no encontrado: MT-NOPE"));

        mockMvc.perform(get("/api/v1/admin/documentos/envios/MT-NOPE/etiqueta"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void etiquetasLote_retorna200PdfAttachment() throws Exception {
        doAnswer(inv -> {
            OutputStream out = inv.getArgument(2);
            out.write(new byte[]{'%', 'P', 'D', 'F'});
            return null;
        }).when(documentoPdfService).generarEtiquetasLote(eq(9L), eq("admin"), any());

        mockMvc.perform(get("/api/v1/admin/documentos/lotes/9/etiquetas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"etiquetas-lote-9.pdf\""))
                .andExpect(content().bytes(new byte[]{'%', 'P', 'D', 'F'}));
    }

    @Test
    void etiquetasLote_superaTope_retorna400() throws Exception {
        doThrow(new com.monteastur.envios.exception.BadRequestException("El lote tiene 6000 envíos"))
                .when(documentoPdfService).generarEtiquetasLote(anyLong(), anyString(), any());

        mockMvc.perform(get("/api/v1/admin/documentos/lotes/9/etiquetas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void manifiesto_retorna200PdfAttachment() throws Exception {
        when(documentoPdfService.generarManifiesto(9L, "admin"))
                .thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(get("/api/v1/admin/documentos/lotes/9/manifiesto"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"manifiesto-lote-9.pdf\""))
                .andExpect(content().bytes(new byte[]{'%', 'P', 'D', 'F'}));
    }

    @Test
    void listar_retornaJsonDeEmisiones() throws Exception {
        DocumentoGenerado doc = new DocumentoGenerado(TipoDocumento.ETIQUETA_TERMICA,
                "MT-1", "etiqueta-MT-1.pdf", 1234, "admin");
        when(documentoPdfService.listarEmisiones(null)).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/v1/admin/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("ETIQUETA_TERMICA"))
                .andExpect(jsonPath("$[0].referenciaId").value("MT-1"))
                .andExpect(jsonPath("$[0].pesoBytes").value(1234))
                .andExpect(jsonPath("$[0].usuarioGeneracion").value("admin"));
    }

    @Test
    void listar_conTipo_filtra() throws Exception {
        when(documentoPdfService.listarEmisiones(TipoDocumento.MANIFIESTO_CARGA)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/documentos").param("tipo", "MANIFIESTO_CARGA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithAnonymousUser
    void sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/documentos"))
                .andExpect(status().isUnauthorized());
    }
}
