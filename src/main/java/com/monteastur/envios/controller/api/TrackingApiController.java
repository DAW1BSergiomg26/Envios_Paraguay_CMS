package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.ErrorDto;
import com.monteastur.envios.dto.api.PublicTrackingDto;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/tracking")
public class TrackingApiController {

    private final EnvioTrackingRepository trackingRepository;

    public TrackingApiController(EnvioTrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<?> getTrackingByCodigo(@PathVariable String codigo) {
        return trackingRepository.findByCodigoUnico(codigo.trim().toUpperCase())
                .<ResponseEntity<?>>map(envio -> {
                    PublicTrackingDto dto = new PublicTrackingDto();
                    dto.setCodigoUnico(envio.getCodigoUnico());
                    dto.setEstado(envio.getEstado());
                    dto.setOrigen(envio.getOrigen());
                    dto.setDestino(envio.getDestino());
                    dto.setUltimaActualizacion(envio.getUltimaActualizacion());
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(Instant.now().toString(), 404, "Tracking no encontrado")));
    }
}
