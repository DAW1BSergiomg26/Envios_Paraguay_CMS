package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.ErrorDto;
import com.monteastur.envios.dto.api.NotificacionDto;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.Notificacion;
import com.monteastur.envios.repository.NotificacionRepository;
import com.monteastur.envios.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Notificaciones Admin", description = "Consultas y reintento de notificaciones de envío (requiere autenticación)")
@RestController
@RequestMapping("/api/v1/admin/notificaciones")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class NotificacionApiController {

    private final NotificacionRepository notificacionRepository;
    private final EmailService emailService;

    public NotificacionApiController(NotificacionRepository notificacionRepository,
                                     EmailService emailService) {
        this.notificacionRepository = notificacionRepository;
        this.emailService = emailService;
    }

    @Operation(summary = "Listar notificaciones", description = "Devuelve todas las notificaciones ordenadas por fecha descendente, opcionalmente filtradas por estado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de notificaciones",
            content = @Content(schema = @Schema(implementation = NotificacionDto.class))),
        @ApiResponse(responseCode = "400", description = "estado inválido",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @GetMapping
    public ResponseEntity<List<NotificacionDto>> listar(@RequestParam(required = false) String estado) {
        List<Notificacion> notificaciones;
        if (estado != null && !estado.isBlank()) {
            notificaciones = notificacionRepository.findByEstadoOrderByFechaCreacionDesc(parseEstado(estado));
        } else {
            notificaciones = notificacionRepository.findAllByOrderByFechaCreacionDesc();
        }
        return ResponseEntity.ok(notificaciones.stream().map(NotificacionDto::from).collect(Collectors.toList()));
    }

    @Operation(summary = "Obtener notificación", description = "Devuelve el detalle de una notificación por su identificador")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notificación encontrada",
            content = @Content(schema = @Schema(implementation = NotificacionDto.class))),
        @ApiResponse(responseCode = "404", description = "Notificación no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<NotificacionDto> detalle(@PathVariable Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada: " + id));
        return ResponseEntity.ok(NotificacionDto.from(notificacion));
    }

    @Operation(summary = "Reintentar notificación fallida", description = "Reenvía el correo de una notificación en estado FALLIDO")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Correo reenviado correctamente",
            content = @Content(schema = @Schema(implementation = NotificacionDto.class))),
        @ApiResponse(responseCode = "400", description = "La notificación no tiene destinatario",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Notificación no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "409", description = "La notificación no está en estado FALLIDO",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "500", description = "Error al reenviar el correo",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PostMapping("/{id}/reintentar")
    public ResponseEntity<?> reintentar(@PathVariable Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada: " + id));
        if (notificacion.getEstado() != Notificacion.EstadoNotificacion.FALLIDO) {
            throw new ConflictException("Solo se pueden reintentar notificaciones en estado FALLIDO");
        }
        if (notificacion.getDestinatario() == null || notificacion.getDestinatario().isBlank()) {
            throw new BadRequestException("La notificación no tiene destinatario");
        }
        try {
            emailService.enviarCorreoSimple(notificacion.getDestinatario(), notificacion.getAsunto(), notificacion.getMensaje());
            notificacion.setEstado(Notificacion.EstadoNotificacion.ENVIADO);
            notificacion.setErrorMensaje(null);
        } catch (Exception ex) {
            notificacion.setEstado(Notificacion.EstadoNotificacion.FALLIDO);
            notificacion.setErrorMensaje(ex.getMessage());
            notificacionRepository.save(notificacion);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorDto(Instant.now().toString(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "No se pudo reenviar la notificación: " + ex.getMessage()));
        }
        return ResponseEntity.ok(NotificacionDto.from(notificacionRepository.save(notificacion)));
    }

    private Notificacion.EstadoNotificacion parseEstado(String estado) {
        try {
            return Notificacion.EstadoNotificacion.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("estado inválido: " + estado);
        }
    }
}
