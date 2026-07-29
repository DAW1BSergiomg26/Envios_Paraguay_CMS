package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.*;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.ClienteService;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Cliente", description = "Portal del cliente autenticado por sesión (requiere cookie JSESSIONID)")
@RestController
@RequestMapping("/api/v1/cliente")
public class ClienteApiController {

    private final EnvioTrackingRepository trackingRepo;
    private final ClienteService clienteService;
    private final EvidenciaEnvioService evidenciaService;
    private final EventoTrackingService eventoTrackingService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public ClienteApiController(EnvioTrackingRepository trackingRepo,
                                 ClienteService clienteService,
                                 EvidenciaEnvioService evidenciaService,
                                 EventoTrackingService eventoTrackingService) {
        this.trackingRepo = trackingRepo;
        this.clienteService = clienteService;
        this.evidenciaService = evidenciaService;
        this.eventoTrackingService = eventoTrackingService;
    }

    @Operation(summary = "Listar envíos del cliente", description = "Devuelve los envíos asociados al cliente autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de envíos del cliente",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ClienteEnvioResumenDto.class))),
        @ApiResponse(responseCode = "403", description = "No autenticado",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @GetMapping("/envios")
    public ResponseEntity<?> listarEnvios(HttpSession session) {
        Long clienteId = (Long) session.getAttribute("clienteId");
        if (clienteId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDto(Instant.now().toString(), 403, "Acceso denegado"));
        }
        List<EnvioTracking> envios = trackingRepo.findByClienteIdOrderByUltimaActualizacionDesc(clienteId);
        List<ClienteEnvioResumenDto> dtos = envios.stream().map(e -> {
            ClienteEnvioResumenDto dto = new ClienteEnvioResumenDto();
            dto.setCodigo(e.getCodigoUnico());
            dto.setEstado(e.getEstado());
            dto.setOrigen(e.getOrigen());
            dto.setDestino(e.getDestino());
            dto.setUltimaActualizacion(e.getUltimaActualizacion());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Detalle de envío del cliente", description = "Obtiene el detalle completo de un envío con eventos y evidencias visibles")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle del envío",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.TrackingDto.class))),
        @ApiResponse(responseCode = "403", description = "No autenticado o no autorizado",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Envío no encontrado",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @GetMapping("/envios/{codigo}")
    public ResponseEntity<?> detalleEnvio(@PathVariable String codigo, HttpSession session) {
        Long clienteId = (Long) session.getAttribute("clienteId");
        if (clienteId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDto(Instant.now().toString(), 403, "Acceso denegado"));
        }
        EnvioTracking envio = trackingRepo.findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
            .orElseThrow(() -> new com.monteastur.envios.exception.ResourceNotFoundException("Tracking no encontrado: " + codigo));
        if (envio.getCliente() == null || !envio.getCliente().getId().equals(clienteId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDto(Instant.now().toString(), 403, "Acceso denegado"));
        }
        TrackingDto dto = new TrackingDto();
        dto.setCodigoUnico(envio.getCodigoUnico());
        dto.setEstado(envio.getEstado());
        dto.setDestinatario(envio.getDestinatario());
        dto.setOrigen(envio.getOrigen());
        dto.setDestino(envio.getDestino());
        dto.setPeso(envio.getPeso());
        dto.setContenido(envio.getContenido());
        dto.setUltimaActualizacion(envio.getUltimaActualizacion());
        dto.setEventos(eventoTrackingService.listarPorEnvio(envio.getId()).stream().map(ev -> {
            EventoDto evDto = new EventoDto();
            evDto.setFecha(ev.getFechaEvento());
            evDto.setDescripcion(ev.getDescripcion());
            evDto.setTipo(ev.getEstado());
            return evDto;
        }).collect(Collectors.toList()));
        dto.setEvidencias(evidenciaService.listarPorEnvioParaCliente(envio.getId()).stream().map(ev -> {
            EvidenciaDto evDto = new EvidenciaDto();
            evDto.setTitulo(ev.getTitulo());
            evDto.setDescripcion(ev.getDescripcion());
            evDto.setTipo(ev.getTipo());
            evDto.setUrlArchivo("/api/v1/cliente/evidencias/" + ev.getId() + "/archivo");
            evDto.setVisibleCliente(ev.getVisibleCliente());
            return evDto;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Descargar evidencia", description = "Descarga un archivo de evidencia asociado a un envío del cliente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Archivo descargado"),
        @ApiResponse(responseCode = "403", description = "No autenticado, archivo no visible o nombre no permitido",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Evidencia o archivo no encontrado",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @GetMapping("/evidencias/{id}/archivo")
    public ResponseEntity<?> descargarEvidencia(@PathVariable Long id, HttpSession session) {
        Long clienteId = (Long) session.getAttribute("clienteId");
        if (clienteId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDto(Instant.now().toString(), 403, "Acceso denegado"));
        }

        var evidencia = evidenciaService.buscar(id)
            .orElseThrow(() -> new com.monteastur.envios.exception.ResourceNotFoundException("Evidencia no encontrada: " + id));
        if (!Boolean.TRUE.equals(evidencia.getVisibleCliente())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDto(Instant.now().toString(), 403, "Acceso denegado"));
        }

        var envio = evidencia.getEnvioTracking();
        if (envio == null || envio.getCliente() == null || !envio.getCliente().getId().equals(clienteId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDto(Instant.now().toString(), 403, "Acceso denegado"));
        }

        String urlArchivo = evidencia.getUrlArchivo();
        if (urlArchivo == null || !urlArchivo.startsWith("/uploads/evidencias/")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDto(Instant.now().toString(), 404, "Archivo no encontrado"));
        }

        String fileName = urlArchivo.substring("/uploads/evidencias/".length());
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDto(Instant.now().toString(), 403, "Nombre de archivo no permitido"));
        }

        String baseDir = uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
        Path filePath = Paths.get(baseDir, "evidencias", fileName).normalize();

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorDto(Instant.now().toString(), 404, "Archivo no encontrado"));
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDto(Instant.now().toString(), 404, "Archivo no encontrado"));
        }
    }
}
