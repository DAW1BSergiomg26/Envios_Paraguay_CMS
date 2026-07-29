package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.ErrorDto;
import com.monteastur.envios.dto.api.PublicTrackingDto;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;

@Tag(name = "Tracking Público", description = "Consulta pública de tracking por código (no requiere autenticación)")
@RestController
@RequestMapping("/api/v1/tracking")
public class TrackingApiController {

    private final EnvioTrackingRepository trackingRepository;

    public TrackingApiController(EnvioTrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
    }

    @Operation(summary = "Consultar tracking", description = "Obtiene el estado actual de un envío mediante su código único")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado del envío",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.PublicTrackingDto.class))),
        @ApiResponse(responseCode = "404", description = "Código de tracking no encontrado",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
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
