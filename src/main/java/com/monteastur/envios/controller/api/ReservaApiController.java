package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.*;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.service.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
    public ResponseEntity<Object> detalle(@PathVariable Long id) {
        Optional<Reserva> reserva = reservaService.buscarPorId(id);
        if (reserva.isPresent()) {
            return ResponseEntity.ok(toDto(reserva.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorDto(Instant.now().toString(), 404, "Reserva no encontrada"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable Long id,
                                             @RequestBody ActualizarReservaRequest request) {
        try {
            Optional<Reserva> reserva = reservaService.actualizar(id, request);
            if (reserva.isPresent()) {
                return ResponseEntity.ok(toDto(reserva.get()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorDto(Instant.now().toString(), 404, "Reserva no encontrada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorDto(Instant.now().toString(), 409, e.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(@PathVariable Long id,
                                                 @RequestBody ActualizarEstadoRequest request) {
        try {
            Optional<Reserva> reserva = reservaService.cambiarEstado(id, request.getEstado());
            if (reserva.isPresent()) {
                return ResponseEntity.ok(toDto(reserva.get()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorDto(Instant.now().toString(), 404, "Reserva no encontrada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable Long id) {
        Optional<Reserva> reserva = reservaService.buscarPorId(id);
        if (reserva.isPresent()) {
            reservaService.eliminar(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorDto(Instant.now().toString(), 404, "Reserva no encontrada"));
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
