package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.*;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.ClienteService;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/cliente")
public class ClienteApiController {

    private final EnvioTrackingRepository trackingRepo;
    private final ClienteService clienteService;
    private final EvidenciaEnvioService evidenciaService;
    private final EventoTrackingService eventoTrackingService;

    public ClienteApiController(EnvioTrackingRepository trackingRepo,
                                 ClienteService clienteService,
                                 EvidenciaEnvioService evidenciaService,
                                 EventoTrackingService eventoTrackingService) {
        this.trackingRepo = trackingRepo;
        this.clienteService = clienteService;
        this.evidenciaService = evidenciaService;
        this.eventoTrackingService = eventoTrackingService;
    }

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

    @GetMapping("/envios/{codigo}")
    public ResponseEntity<?> detalleEnvio(@PathVariable String codigo, HttpSession session) {
        Long clienteId = (Long) session.getAttribute("clienteId");
        if (clienteId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDto(Instant.now().toString(), 403, "Acceso denegado"));
        }
        var opt = trackingRepo.findWithClienteByCodigoUnico(codigo.trim().toUpperCase());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDto(Instant.now().toString(), 404, "Tracking no encontrado"));
        }
        EnvioTracking envio = opt.get();
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
            evDto.setUrlArchivo(ev.getUrlArchivo());
            evDto.setVisibleCliente(ev.getVisibleCliente());
            return evDto;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(dto);
    }
}
