package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.dto.api.ActualizarOrdenImagenRequest;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.model.Imagen;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.UploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImagenApiController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "admin", roles = {"ADMIN"})
class ImagenApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImagenRepository imagenRepo;

    @MockitoBean
    private UploadService uploadService;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private RBACAccessLogger rbacAccessLogger;

    @MockitoBean
    private CustomAccessDeniedHandler accessDenied;

    private Imagen imagen(Long id, String titulo, String url, Integer orden) {
        Imagen img = new Imagen(titulo, "desc " + titulo, url, "cat", orden);
        img.setId(id);
        img.setCreatedAt(LocalDateTime.now());
        return img;
    }

    @Test
    void listar_retornaIordenado() throws Exception {
        Imagen img1 = imagen(1L, "A", "/uploads/a.jpg", 0);
        Imagen img2 = imagen(2L, "B", "/uploads/b.jpg", 1);
        when(imagenRepo.findAllByOrderByOrdenAsc()).thenReturn(List.of(img1, img2));

        mockMvc.perform(get("/api/v1/admin/imagenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("A"))
                .andExpect(jsonPath("$[0].url").value("/uploads/a.jpg"))
                .andExpect(jsonPath("$[0].orden").value(0));
    }

    @Test
    void subir_retornaCreado() throws Exception {
        byte[] bytes = "fake-jpg-bytes".getBytes();
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.jpg",
                MediaType.IMAGE_JPEG_VALUE, bytes);
        when(uploadService.subirArchivo(any(MultipartFile.class), anyString()))
                .thenReturn("abc.jpg");
        Imagen saved = imagen(1L, "Foto", "/uploads/abc.jpg", 1);
        when(imagenRepo.save(any(Imagen.class))).thenReturn(saved);

        mockMvc.perform(multipart("/api/v1/admin/imagenes")
                        .file(archivo)
                        .param("titulo", "Foto")
                        .param("orden", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/uploads/abc.jpg"));

        verify(uploadService).subirArchivo(any(MultipartFile.class), anyString());
        verify(imagenRepo).save(any(Imagen.class));
    }

    @Test
    void subir_archivoVacio_400() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.jpg",
                MediaType.IMAGE_JPEG_VALUE, new byte[0]);
        when(uploadService.subirArchivo(any(MultipartFile.class), anyString()))
                .thenThrow(new BadRequestException("Debes seleccionar un archivo."));

        mockMvc.perform(multipart("/api/v1/admin/imagenes")
                        .file(archivo)
                        .param("titulo", "Foto")
                        .param("orden", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void subir_extensionInvalida_400() throws Exception {
        byte[] bytes = "fake-exe-bytes".getBytes();
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, bytes);
        when(uploadService.subirArchivo(any(MultipartFile.class), anyString()))
                .thenThrow(new BadRequestException("Extensión de archivo no permitida."));

        mockMvc.perform(multipart("/api/v1/admin/imagenes")
                        .file(archivo)
                        .param("titulo", "Foto")
                        .param("orden", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarOrden_ok() throws Exception {
        Imagen img = imagen(1L, "A", "/uploads/a.jpg", 1);
        when(imagenRepo.findById(1L)).thenReturn(Optional.of(img));
        Imagen updated = imagen(1L, "A", "/uploads/a.jpg", 2);
        when(imagenRepo.save(any(Imagen.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/admin/imagenes/1/orden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orden\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orden").value(2));
    }

    @Test
    void cambiarOrden_noExiste_404() throws Exception {
        when(imagenRepo.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/v1/admin/imagenes/1/orden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orden\":2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_204() throws Exception {
        Imagen img = imagen(1L, "A", "/uploads/x.jpg", 0);
        when(imagenRepo.findById(1L)).thenReturn(Optional.of(img));

        mockMvc.perform(delete("/api/v1/admin/imagenes/1"))
                .andExpect(status().isNoContent());

        verify(uploadService).eliminarArchivo("x.jpg");
        verify(imagenRepo).delete(any(Imagen.class));
    }

    @Test
    @WithAnonymousUser
    void sinAutenticacion_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/imagenes"))
                .andExpect(status().isUnauthorized());
    }
}
