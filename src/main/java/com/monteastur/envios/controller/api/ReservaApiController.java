package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.*;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.service.ReservaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/reservas")
public class ReservaApiController {

    private final ReservaService reservaService;

    public ReservaApiController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public ResponseEntity<List<ReservaAdminDto>> listar(
            @RequestParam(required = false) String estado) {

        List<Reserva> reservas = reservaService.listarTodas();

        if (estado != null && !estado.isBlank()) {
            String estadoNormalizado = estado.trim().toUpperCase();
            reservas = reservas.stream()
                .filter(r -> r.getEstado().equals(estadoNormalizado))
                .collect(Collectors.toList());
        }

        List<ReservaAdminDto> dtos = reservas.stream()
            .map(this::toDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaAdminDto> detalle(@PathVariable Long id) {
        Reserva reserva = reservaService.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + id));
        return ResponseEntity.ok(toDto(reserva));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservaAdminDto> actualizar(@PathVariable Long id,
                                                       @RequestBody ActualizarReservaRequest request) {
        Reserva reserva = reservaService.actualizar(id, request)
            .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + id));
        return ResponseEntity.ok(toDto(reserva));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReservaAdminDto> cambiarEstado(@PathVariable Long id,
                                                          @RequestBody ActualizarEstadoRequest request) {
        Reserva reserva = reservaService.cambiarEstado(id, request.getEstado())
            .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + id));
        return ResponseEntity.ok(toDto(reserva));
    }

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
