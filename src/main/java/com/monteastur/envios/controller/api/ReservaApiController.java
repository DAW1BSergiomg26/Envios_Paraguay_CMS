package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.*;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.service.ReservaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Reservas", description = "Gestión de reservas del panel de administración (requiere Basic Auth)")
@RestController
@RequestMapping("/api/v1/admin/reservas")
public class ReservaApiController {

    private final ReservaService reservaService;

    public ReservaApiController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Operation(summary = "Listar reservas", description = "Devuelve todas las reservas, opcionalmente filtradas por estado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de reservas",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ReservaAdminDto.class)))
    })
    @GetMapping
    public ResponseEntity<List<ReservaAdminDto>> listar(
            @RequestParam(required = false) String estado) {

        List<Reserva> reservas = reservaService.listarTodas();

        if (estado != null && !estado.isBlank()) {
            String estadoNormalizado = estado.trim().toLowerCase();
            reservas = reservas.stream()
                .filter(r -> r.getEstado().equals(estadoNormalizado))
                .collect(Collectors.toList());
        }

        List<ReservaAdminDto> dtos = reservas.stream()
            .map(this::toDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Detalle de reserva", description = "Obtiene una reserva por su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva encontrada",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ReservaAdminDto.class))),
        @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReservaAdminDto> detalle(@PathVariable Long id) {
        Reserva reserva = reservaService.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + id));
        return ResponseEntity.ok(toDto(reserva));
    }

    @Operation(summary = "Actualizar reserva", description = "Actualiza los datos de una reserva existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva actualizada",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ReservaAdminDto.class))),
        @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ReservaAdminDto> actualizar(@PathVariable Long id,
                                                       @RequestBody ActualizarReservaRequest request) {
        Reserva reserva = reservaService.actualizar(id, request)
            .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + id));
        return ResponseEntity.ok(toDto(reserva));
    }

    @Operation(summary = "Cambiar estado de reserva", description = "Cambia el estado de una reserva (pendiente/aprobada/cancelada)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ReservaAdminDto.class))),
        @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReservaAdminDto> cambiarEstado(@PathVariable Long id,
                                                          @RequestBody ActualizarEstadoRequest request) {
        Reserva reserva = reservaService.cambiarEstado(id, request.getEstado())
            .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + id));
        return ResponseEntity.ok(toDto(reserva));
    }

    @Operation(summary = "Eliminar reserva", description = "Elimina una reserva por su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reserva eliminada (sin contenido)"),
        @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        Reserva reserva = reservaService.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + id));
        reservaService.eliminar(id);
        return ResponseEntity.noContent().build();
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
