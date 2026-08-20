package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.dto.api.EntregaEvidenciaDto;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.EntregaEvidenciaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntregaEvidenciaController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, CustomAccessDeniedHandler.class})
@WithMockUser(username = "operador", roles = "OPERADOR")
class EntregaEvidenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EntregaEvidenciaService entregaEvidenciaService;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private RBACAccessLogger rbacAccessLogger;

    private static final String PNG_1X1 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    private EntregaEvidencia evidenciaValida() {
        EnvioTracking envio = new EnvioTracking("MT-1", "ENTREGADO", "Receptor",
                "Origen", "Destino", "10 kg", "Documentos");
        EntregaEvidencia evidencia = new EntregaEvidencia(envio, "Ana López", "12345678",
                PNG_1X1, new BigDecimal("-25.2637421"), new BigDecimal("-57.575926"), null);
        evidencia.setId(5L);
        return evidencia;
    }

    @Test
    void registrarPod_retorna201ConDto() throws Exception {
        when(entregaEvidenciaService.registrarEntrega(eq("MT-1"), any()))
                .thenReturn(evidenciaValida());

        mockMvc.perform(post("/api/v1/deliveries/MT-1/pod")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorNombre\":\"Ana López\",\"receptorDocumento\":\"12345678\","
                                + "\"firmaBase64\":\"" + PNG_1X1 + "\",\"latitud\":-25.2637421,\"longitud\":-57.575926}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoRastreo").value("MT-1"))
                .andExpect(jsonPath("$.receptorNombre").value("Ana López"))
                .andExpect(jsonPath("$.receptorDocumento").value("12345678"))
                .andExpect(jsonPath("$.firmaBase64").value(PNG_1X1))
                .andExpect(jsonPath("$.latitud").value(new BigDecimal("-25.2637421")));
    }

    @Test
    void registrarPod_validacionFallida_retorna400() throws Exception {
        when(entregaEvidenciaService.registrarEntrega(anyString(), any()))
                .thenThrow(new BadRequestException("La firma debe ser una imagen PNG"));

        mockMvc.perform(post("/api/v1/deliveries/MT-1/pod")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorNombre\":\"Ana\",\"receptorDocumento\":\"1\",\"firmaBase64\":\"AAAA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registrarPod_envioInexistente_retorna404() throws Exception {
        when(entregaEvidenciaService.registrarEntrega(anyString(), any()))
                .thenThrow(new ResourceNotFoundException("Tracking no encontrado: MT-NOPE"));

        mockMvc.perform(post("/api/v1/deliveries/MT-NOPE/pod")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorNombre\":\"Ana\",\"receptorDocumento\":\"1\",\"firmaBase64\":\"AAAA\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void registrarPod_podExistente_retorna409() throws Exception {
        when(entregaEvidenciaService.registrarEntrega(anyString(), any()))
                .thenThrow(new ConflictException("El envío MT-1 ya tiene evidencia de entrega registrada"));

        mockMvc.perform(post("/api/v1/deliveries/MT-1/pod")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorNombre\":\"Ana\",\"receptorDocumento\":\"1\",\"firmaBase64\":\"AAAA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void obtenerPod_retorna200ConDto() throws Exception {
        EntregaEvidenciaDto dto = EntregaEvidenciaDto.from(evidenciaValida());
        when(entregaEvidenciaService.obtenerEntrega("MT-1")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/deliveries/MT-1/pod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoRastreo").value("MT-1"))
                .andExpect(jsonPath("$.receptorNombre").value("Ana López"))
                .andExpect(jsonPath("$.firmaBase64").value(PNG_1X1));
    }

    @Test
    void obtenerPod_sinEvidencia_retorna404() throws Exception {
        when(entregaEvidenciaService.obtenerEntrega(anyString()))
                .thenThrow(new ResourceNotFoundException("No existe evidencia de entrega para el envío: MT-1"));

        mockMvc.perform(get("/api/v1/deliveries/MT-1/pod"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithAnonymousUser
    void sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/MT-1/pod")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "cliente", roles = "CLIENTE")
    void rolCliente_denegado() throws Exception {
        mockMvc.perform(post("/api/v1/deliveries/MT-1/pod")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorNombre\":\"Ana\",\"receptorDocumento\":\"1\",\"firmaBase64\":\"AAAA\"}"))
                .andExpect(status().isBadRequest());
    }
}
