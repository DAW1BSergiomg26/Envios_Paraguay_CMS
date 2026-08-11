package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.model.TextoLegal;
import com.monteastur.envios.repository.TextoLegalRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TextoLegalApiController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "admin", roles = {"ADMIN"})
class TextoLegalApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TextoLegalRepository textoRepo;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler accessDenied;

    private TextoLegal textoLegal(Long id, String slug, String titulo, String contenido) {
        TextoLegal t = new TextoLegal();
        t.setId(id);
        t.setSlug(slug);
        t.setTitulo(titulo);
        t.setContenido(contenido);
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    @Test
    void listar_sinContenido() throws Exception {
        TextoLegal t1 = textoLegal(1L, "aviso-legal", "Aviso Legal", "Contenido 1");
        TextoLegal t2 = textoLegal(2L, "politica-cookies", "Política de Cookies", "Contenido 2");
        when(textoRepo.findAll()).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/api/v1/admin/textos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("aviso-legal"))
                .andExpect(jsonPath("$[0].titulo").value("Aviso Legal"))
                .andExpect(jsonPath("$[0].contenido").value((String) null))
                .andExpect(jsonPath("$[1].slug").value("politica-cookies"))
                .andExpect(jsonPath("$[1].contenido").value((String) null));
    }

    @Test
    void porSlug_conContenido() throws Exception {
        TextoLegal t = textoLegal(1L, "aviso-legal", "Aviso Legal", "Texto largo legal...");
        when(textoRepo.findBySlug("aviso-legal")).thenReturn(Optional.of(t));

        mockMvc.perform(get("/api/v1/admin/textos/aviso-legal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("aviso-legal"))
                .andExpect(jsonPath("$.titulo").value("Aviso Legal"))
                .andExpect(jsonPath("$.contenido").value("Texto largo legal..."));
    }

    @Test
    void porSlug_noExiste_404() throws Exception {
        when(textoRepo.findBySlug("no-existe")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/admin/textos/no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_ok() throws Exception {
        TextoLegal t = textoLegal(1L, "aviso-legal", "Aviso Legal Original", "Contenido viejo");
        when(textoRepo.findBySlug("aviso-legal")).thenReturn(Optional.of(t));

        TextoLegal saved = textoLegal(1L, "aviso-legal", "Nuevo Título", "Nuevo Contenido");
        when(textoRepo.save(any(TextoLegal.class))).thenReturn(saved);

        mockMvc.perform(put("/api/v1/admin/textos/aviso-legal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Nuevo Título\",\"contenido\":\"Nuevo Contenido\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Nuevo Título"))
                .andExpect(jsonPath("$.contenido").value("Nuevo Contenido"));

        verify(textoRepo).save(any(TextoLegal.class));
    }

    @Test
    void actualizar_noExiste_404() throws Exception {
        when(textoRepo.findBySlug("no-existe")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/admin/textos/no-existe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Nuevo Título\",\"contenido\":\"Nuevo Contenido\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_contenidoVacio_400() throws Exception {
        mockMvc.perform(put("/api/v1/admin/textos/aviso-legal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Nuevo Título\",\"contenido\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void sinAutenticacion_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/textos"))
                .andExpect(status().isUnauthorized());
    }
}
