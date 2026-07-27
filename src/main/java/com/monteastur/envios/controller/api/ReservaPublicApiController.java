package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.CrearReservaPublicRequest;
import com.monteastur.envios.dto.api.ErrorDto;
import com.monteastur.envios.dto.api.ReservaAdminDto;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaPublicApiController {

    private final ReservaService reservaService;

    public ReservaPublicApiController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody CrearReservaPublicRequest request) {
        try {
            Reserva reserva = reservaService.crearPublico(request);
            ReservaAdminDto dto = toDto(reserva);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorDto(Instant.now().toString(), 409, e.getMessage()));
        }
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<Object> verificarDisponibilidad(
            @RequestParam String fechaEntrada,
            @RequestParam String fechaSalida) {
        try {
            LocalDate inicio = LocalDate.parse(fechaEntrada);
            LocalDate fin = LocalDate.parse(fechaSalida);
            boolean disponible = reservaService.verificarDisponibilidad(inicio, fin);
            return ResponseEntity.ok(Map.of(
                "disponible", disponible,
                "fechaEntrada", fechaEntrada,
                "fechaSalida", fechaSalida
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Formato de fecha inválido. Use YYYY-MM-DD."));
        }
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
