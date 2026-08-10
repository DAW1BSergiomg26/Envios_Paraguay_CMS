package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.*;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.service.MensajeContactoService;
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

@Tag(name = "Admin Mensajes", description = "Gestión de mensajes de contacto del panel de administración (requiere Basic Auth)")
@RestController
@RequestMapping("/api/v1/admin/mensajes")
public class MensajeContactoApiController {

    private final MensajeContactoService mensajeContactoService;

    public MensajeContactoApiController(MensajeContactoService mensajeContactoService) {
        this.mensajeContactoService = mensajeContactoService;
    }

    @Operation(summary = "Listar mensajes de contacto",
        description = "Devuelve todos los mensajes, opcionalmente filtrados por estado de lectura")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de mensajes",
            content = @Content(schema = @Schema(implementation = MensajeContactoAdminDto.class)))
    })
    @GetMapping
    public ResponseEntity<List<MensajeContactoAdminDto>> listar(
            @RequestParam(required = false) Boolean leido) {
        List<MensajeContactoAdminDto> dtos = mensajeContactoService.listar(leido).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Marcar mensaje como leído o no leído",
        description = "Actualiza el estado de lectura de un mensaje")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mensaje actualizado",
            content = @Content(schema = @Schema(implementation = MensajeContactoAdminDto.class))),
        @ApiResponse(responseCode = "404", description = "Mensaje no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PatchMapping("/{id}/leido")
    public ResponseEntity<MensajeContactoAdminDto> marcarLeido(@PathVariable Long id,
                                                                @RequestBody MarcarLeidoRequest request) {
        MensajeContacto mensaje = mensajeContactoService.marcarLeido(id, Boolean.TRUE.equals(request.getLeido()))
            .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado: " + id));
        return ResponseEntity.ok(toDto(mensaje));
    }

    @Operation(summary = "Eliminar mensaje de contacto", description = "Elimina un mensaje por su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Mensaje eliminado (sin contenido)"),
        @ApiResponse(responseCode = "404", description = "Mensaje no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mensajeContactoService.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado: " + id));
        mensajeContactoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private MensajeContactoAdminDto toDto(MensajeContacto m) {
        MensajeContactoAdminDto dto = new MensajeContactoAdminDto();
        dto.setId(m.getId());
        dto.setNombre(m.getNombre());
        dto.setEmail(m.getEmail());
        dto.setTelefono(m.getTelefono());
        dto.setMensaje(m.getMensaje());
        dto.setFechaEnvio(m.getFechaEnvio());
        dto.setLeido(m.isLeido());
        return dto;
    }
}
