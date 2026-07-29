package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.CrearReservaPublicRequest;
import com.monteastur.envios.dto.api.ReservaAdminDto;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Map;

@Tag(name = "Reservas Público", description = "Creación y consulta de reservas desde la web pública (no requiere autenticación)")
@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaPublicApiController {

    private final ReservaService reservaService;

    public ReservaPublicApiController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Operation(summary = "Crear reserva", description = "Registra una nueva reserva en el sistema")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reserva creada",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ReservaAdminDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos de reserva inválidos o fechas no disponibles",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class))),
        @ApiResponse(responseCode = "409", description = "Conflicto — las fechas ya están ocupadas",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @PostMapping
    public ResponseEntity<ReservaAdminDto> crear(@Valid @RequestBody CrearReservaPublicRequest request) {
        Reserva reserva = reservaService.crearPublico(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(reserva));
    }

    @Operation(summary = "Verificar disponibilidad", description = "Comprueba si un rango de fechas está disponible para reservar")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resultado de disponibilidad (disponible: true/false)"),
        @ApiResponse(responseCode = "400", description = "Formato de fecha inválido",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @GetMapping("/disponibilidad")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidad(
            @RequestParam String fechaEntrada,
            @RequestParam String fechaSalida) {
        LocalDate inicio = LocalDate.parse(fechaEntrada);
        LocalDate fin = LocalDate.parse(fechaSalida);
        boolean disponible = reservaService.verificarDisponibilidad(inicio, fin);
        return ResponseEntity.ok(Map.of(
            "disponible", disponible,
            "fechaEntrada", fechaEntrada,
            "fechaSalida", fechaSalida
        ));
    }

    private ReservaAdminDto toDto(Reserva r) {
        ReservaAdminDto dto = new ReservaAdminDto();
        dto.setId(r.getId());
        dto.setNombreCliente(r.getNombreCliente());
        dto.setEmail(r.getEmail());
        dto.setTelefono(r.getTelefono());
        dto.setFechaEntrada(r.getFechaEntrada());
        dto.setFechaSalida(r.getFechaSalida());
        dto.setNumeroHuespedes(r.getNumeroHuespedes());
        dto.setComentarios(r.getComentarios());
        dto.setEstado(r.getEstado());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}
