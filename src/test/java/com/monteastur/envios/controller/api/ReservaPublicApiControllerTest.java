package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservaPublicApiController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class ReservaPublicApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservaService reservaService;

    @MockBean
    private DataSource dataSource;

    private static final String VALID_BODY =
        "{\"nombreCliente\":\"Test\",\"email\":\"test@example.com\"," +
        "\"fechaEntrada\":\"2026-08-01\",\"fechaSalida\":\"2026-08-10\",\"numeroHuespedes\":2}";

    @Test
    void badRequestException_retorna400_conMensaje() throws Exception {
        when(reservaService.crearPublico(any())).thenThrow(new BadRequestException("Fechas no válidas"));

        mockMvc.perform(post("/api/v1/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Fechas no válidas"));
    }

    @Test
    void conflictException_retorna409_conMensaje() throws Exception {
        when(reservaService.crearPublico(any())).thenThrow(new ConflictException("Reserva duplicada"));

        mockMvc.perform(post("/api/v1/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Reserva duplicada"));
    }

    @Test
    void runtimeException_retorna500_conMensajeGenerico() throws Exception {
        when(reservaService.crearPublico(any())).thenThrow(new RuntimeException("Error inesperado"));

        mockMvc.perform(post("/api/v1/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Error interno del servidor"));
    }

    @Test
    void conflictException_verificarDisponibilidad_retorna409() throws Exception {
        when(reservaService.verificarDisponibilidad(any(), any()))
                .thenThrow(new ConflictException("No disponible"));

        mockMvc.perform(get("/api/v1/reservas/disponibilidad")
                .param("fechaEntrada", "2026-08-01")
                .param("fechaSalida", "2026-08-10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("No disponible"));
    }
}
