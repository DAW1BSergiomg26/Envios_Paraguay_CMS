package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.ErrorDto;
import com.monteastur.envios.dto.api.TrackingDto;
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
                    TrackingDto dto = new TrackingDto();
                    dto.setCodigoUnico(envio.getCodigoUnico());
                    dto.setEstado(envio.getEstado());
                    dto.setDestinatario(envio.getDestinatario());
                    dto.setOrigen(envio.getOrigen());
                    dto.setDestino(envio.getDestino());
                    dto.setPeso(envio.getPeso());
                    dto.setContenido(envio.getContenido());
                    dto.setUltimaActualizacion(envio.getUltimaActualizacion());
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(Instant.now().toString(), 404, "Tracking no encontrado")));
    }
}
