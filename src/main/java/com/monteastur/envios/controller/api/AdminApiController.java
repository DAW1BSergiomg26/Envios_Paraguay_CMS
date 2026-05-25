package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.*;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;

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
            @RequestParam(required = false) List<String> estados,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        Specification<EnvioTracking> spec = Specification.where(null);

        // Multi-state filter (takes priority over single estado)
        if (estados != null && !estados.isEmpty()) {
            List<String> estadosTrimmed = estados.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toList());
            if (!estadosTrimmed.isEmpty()) {
                spec = spec.and((root, query, cb) -> root.get("estado").in(estadosTrimmed));
            }
        } else if (estado != null && !estado.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado.trim().toUpperCase()));
        }

        // Date range filter (ultimaActualizacion)
        if (fechaDesde != null) {
            spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("ultimaActualizacion"), fechaDesde.atStartOfDay()));
        }
        if (fechaHasta != null) {
            spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("ultimaActualizacion"), fechaHasta.atTime(23, 59, 59)));
        }

        // General search (codigo, destinatario, origen, destino) — takes priority over codigo-only
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("codigoUnico")), pattern),
                    cb.like(cb.lower(root.get("destinatario")), pattern),
                    cb.like(cb.lower(root.get("origen")), pattern),
                    cb.like(cb.lower(root.get("destino")), pattern)
                ));
        } else if (codigo != null && !codigo.isBlank()) {
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
