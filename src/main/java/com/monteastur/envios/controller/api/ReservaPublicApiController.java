package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.CrearReservaPublicRequest;
import com.monteastur.envios.dto.api.ReservaAdminDto;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ReservaAdminDto> crear(@Valid @RequestBody CrearReservaPublicRequest request) {
        Reserva reserva = reservaService.crearPublico(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(reserva));
    }

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
