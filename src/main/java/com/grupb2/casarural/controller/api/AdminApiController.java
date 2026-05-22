package com.grupb2.casarural.controller.api;

import com.grupb2.casarural.dto.api.*;
import com.grupb2.casarural.model.EnvioTracking;
import com.grupb2.casarural.repository.EnvioTrackingRepository;
import com.grupb2.casarural.service.EvidenciaEnvioService;
import com.grupb2.casarural.service.EventoTrackingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminApiController {

    private final EnvioTrackingRepository trackingRepo;
    private final EvidenciaEnvioService evidenciaService;
    private final EventoTrackingService eventoTrackingService;

    public AdminApiController(EnvioTrackingRepository trackingRepo,
                              EvidenciaEnvioService evidenciaService,
                              EventoTrackingService eventoTrackingService) {
        this.trackingRepo = trackingRepo;
        this.evidenciaService = evidenciaService;
        this.eventoTrackingService = eventoTrackingService;
    }

    @GetMapping("/envios")
    public ResponseEntity<Page<AdminEnvioResumenDto>> listarEnvios(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String codigo,
            Pageable pageable) {
        Specification<EnvioTracking> spec = Specification.where(null);
        if (estado != null && !estado.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado.trim().toUpperCase()));
        }
        if (codigo != null && !codigo.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(root.get("codigoUnico"), "%" + codigo.trim().toUpperCase() + "%"));
        }
        Page<AdminEnvioResumenDto> page = trackingRepo.findAll(spec, pageable)
                .map(e -> {
                    AdminEnvioResumenDto dto = new AdminEnvioResumenDto();
                    dto.setCodigoUnico(e.getCodigoUnico());
                    dto.setEstado(e.getEstado());
                    dto.setDestinatario(e.getDestinatario());
                    dto.setOrigen(e.getOrigen());
                    dto.setDestino(e.getDestino());
                    dto.setUltimaActualizacion(e.getUltimaActualizacion());
                    return dto;
                });
        return ResponseEntity.ok(page);
    }

    @GetMapping("/envios/{codigo}")
    public ResponseEntity<?> detalleEnvio(@PathVariable String codigo) {
        var opt = trackingRepo.findWithClienteByCodigoUnico(codigo.trim().toUpperCase());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDto(Instant.now().toString(), 404, "Tracking no encontrado"));
        }
        EnvioTracking envio = opt.get();
        TrackingDto dto = new TrackingDto();
        dto.setCodigoUnico(envio.getCodigoUnico());
        dto.setEstado(envio.getEstado());
        dto.setDestinatario(envio.getDestinatario());
        dto.setOrigen(envio.getOrigen());
        dto.setDestino(envio.getDestino());
        dto.setPeso(envio.getPeso());
        dto.setContenido(envio.getContenido());
        dto.setUltimaActualizacion(envio.getUltimaActualizacion());
        if (envio.getCliente() != null) {
            dto.setClienteNombre(envio.getCliente().getNombre());
            dto.setClienteEmail(envio.getCliente().getEmail());
        }
        dto.setEventos(eventoTrackingService.listarPorEnvio(envio.getId()).stream().map(ev -> {
            EventoDto evDto = new EventoDto();
            evDto.setFecha(ev.getFechaEvento());
            evDto.setDescripcion(ev.getDescripcion());
            evDto.setTipo(ev.getEstado());
            return evDto;
        }).collect(Collectors.toList()));
        dto.setEvidencias(evidenciaService.listarPorEnvio(envio.getId()).stream().map(ev -> {
            EvidenciaDto evDto = new EvidenciaDto();
            evDto.setTitulo(ev.getTitulo());
            evDto.setDescripcion(ev.getDescripcion());
            evDto.setTipo(ev.getTipo());
            evDto.setUrlArchivo(ev.getUrlArchivo());
            evDto.setVisibleCliente(ev.getVisibleCliente());
            return evDto;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/envios/{codigo}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable String codigo,
                                               @RequestBody ActualizarEstadoRequest request) {
        var opt = trackingRepo.findWithClienteByCodigoUnico(codigo.trim().toUpperCase());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDto(Instant.now().toString(), 404, "Tracking no encontrado"));
        }
        EnvioTracking envio = opt.get();
        String estadoAnterior = envio.getEstado();
        envio.setEstado(request.getEstado());
        envio.setUltimaActualizacion(LocalDateTime.now());
        trackingRepo.save(envio);
        eventoTrackingService.crearEvento(envio, estadoAnterior);

        TrackingDto dto = new TrackingDto();
        dto.setCodigoUnico(envio.getCodigoUnico());
        dto.setEstado(envio.getEstado());
        dto.setDestinatario(envio.getDestinatario());
        dto.setOrigen(envio.getOrigen());
        dto.setDestino(envio.getDestino());
        dto.setPeso(envio.getPeso());
        dto.setContenido(envio.getContenido());
        dto.setUltimaActualizacion(envio.getUltimaActualizacion());
        if (envio.getCliente() != null) {
            dto.setClienteNombre(envio.getCliente().getNombre());
            dto.setClienteEmail(envio.getCliente().getEmail());
        }
        dto.setEventos(eventoTrackingService.listarPorEnvio(envio.getId()).stream().map(ev -> {
            EventoDto evDto = new EventoDto();
            evDto.setFecha(ev.getFechaEvento());
            evDto.setDescripcion(ev.getDescripcion());
            evDto.setTipo(ev.getEstado());
            return evDto;
        }).collect(Collectors.toList()));
        dto.setEvidencias(evidenciaService.listarPorEnvio(envio.getId()).stream().map(ev -> {
            EvidenciaDto evDto = new EvidenciaDto();
            evDto.setTitulo(ev.getTitulo());
            evDto.setDescripcion(ev.getDescripcion());
            evDto.setTipo(ev.getTipo());
            evDto.setUrlArchivo(ev.getUrlArchivo());
            evDto.setVisibleCliente(ev.getVisibleCliente());
            return evDto;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(dto);
    }
}
