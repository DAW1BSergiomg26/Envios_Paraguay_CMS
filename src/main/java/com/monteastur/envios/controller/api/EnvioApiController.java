package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.ActualizarEnvioRequest;
import com.monteastur.envios.dto.api.ActualizarVisibilidadRequest;
import com.monteastur.envios.dto.api.CrearEnvioRequest;
import com.monteastur.envios.dto.api.EvidenciaDto;
import com.monteastur.envios.dto.api.EventoDto;
import com.monteastur.envios.dto.api.TrackingDto;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.EvidenciaEnvio;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import com.monteastur.envios.service.EnvioTrackingService;
import com.monteastur.envios.service.UploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Admin Envíos")
@RestController
@RequestMapping("/api/v1/admin/envios")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class EnvioApiController {

    private final EnvioTrackingRepository trackingRepo;
    private final ClienteRepository clienteRepo;
    private final EnvioTrackingService envioTrackingService;
    private final EventoTrackingService eventoTrackingService;
    private final EvidenciaEnvioService evidenciaService;
    private final UploadService uploadService;

    public EnvioApiController(EnvioTrackingRepository trackingRepo,
                              ClienteRepository clienteRepo,
                              EnvioTrackingService envioTrackingService,
                              EventoTrackingService eventoTrackingService,
                              EvidenciaEnvioService evidenciaService,
                              UploadService uploadService) {
        this.trackingRepo = trackingRepo;
        this.clienteRepo = clienteRepo;
        this.envioTrackingService = envioTrackingService;
        this.eventoTrackingService = eventoTrackingService;
        this.evidenciaService = evidenciaService;
        this.uploadService = uploadService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrackingDto crear(@RequestBody CrearEnvioRequest request) {
        if (request.getEstado() == null || request.getEstado().isBlank()) {
            throw new BadRequestException("El estado es obligatorio.");
        }
        if (request.getDestinatario() == null || request.getDestinatario().isBlank()) {
            throw new BadRequestException("El destinatario es obligatorio.");
        }
        String codigo = request.getCodigoUnico();
        if (codigo == null || codigo.isBlank()) {
            codigo = envioTrackingService.generarCodigo();
        } else {
            codigo = codigo.trim().toUpperCase();
        }
        if (trackingRepo.existsByCodigoUnico(codigo)) {
            throw new BadRequestException("Ya existe un envío con el código " + codigo);
        }

        EnvioTracking envio = new EnvioTracking();
        envio.setCodigoUnico(codigo);
        envio.setEstado(request.getEstado().trim().toUpperCase());
        envio.setDestinatario(request.getDestinatario().trim());
        envio.setOrigen(request.getOrigen());
        envio.setDestino(request.getDestino());
        envio.setPeso(request.getPeso());
        envio.setContenido(request.getContenido());
        envio.setObservaciones(request.getObservaciones());
        if (request.getClienteId() != null) {
            envio.setCliente(clienteRepo.findById(request.getClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + request.getClienteId())));
        }
        return toTrackingDto(envioTrackingService.crear(envio));
    }

    @PutMapping("/{codigo}")
    public TrackingDto actualizar(@PathVariable String codigo,
                                  @RequestBody ActualizarEnvioRequest request) {
        EnvioTracking envio = trackingRepo.findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Tracking no encontrado: " + codigo));
        String estadoAnterior = envio.getEstado();
        if (request.getEstado() != null && !request.getEstado().isBlank()) {
            envio.setEstado(request.getEstado().trim().toUpperCase());
        }
        if (request.getDestinatario() != null && !request.getDestinatario().isBlank()) {
            envio.setDestinatario(request.getDestinatario().trim());
        }
        if (request.getOrigen() != null) envio.setOrigen(request.getOrigen());
        if (request.getDestino() != null) envio.setDestino(request.getDestino());
        if (request.getPeso() != null) envio.setPeso(request.getPeso());
        if (request.getContenido() != null) envio.setContenido(request.getContenido());
        if (request.getObservaciones() != null) envio.setObservaciones(request.getObservaciones());
        if (request.getClienteId() != null) {
            envio.setCliente(clienteRepo.findById(request.getClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + request.getClienteId())));
        }
        if (!envio.getEstado().equals(estadoAnterior)) {
            eventoTrackingService.crearEvento(envio, estadoAnterior);
        }
        return toTrackingDto(envioTrackingService.guardar(envio));
    }

    @DeleteMapping("/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String codigo) {
        EnvioTracking envio = trackingRepo.findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Tracking no encontrado: " + codigo));
        envioTrackingService.eliminar(envio.getId());
    }

    @PostMapping(value = "/{codigo}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenciaDto subirEvidencia(@PathVariable String codigo,
                                       @RequestParam String titulo,
                                       @RequestParam(required = false) String descripcion,
                                       @RequestParam String tipo,
                                       @RequestPart("archivo") MultipartFile archivo) throws IOException {
        EnvioTracking envio = trackingRepo.findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Tracking no encontrado: " + codigo));
        if (titulo == null || titulo.isBlank()) {
            throw new BadRequestException("El título es obligatorio.");
        }
        String tipoNormalizado = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!"FOTO".equals(tipoNormalizado) && !"DOCUMENTO".equals(tipoNormalizado)) {
            throw new BadRequestException("El tipo debe ser FOTO o DOCUMENTO.");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("Debes seleccionar un archivo.");
        }

        String relPath = uploadService.subirArchivo(archivo, "evidencias", "jpg", "jpeg", "png", "webp", "pdf");
        EvidenciaEnvio evidencia = new EvidenciaEnvio();
        evidencia.setEnvioTracking(envio);
        evidencia.setTitulo(titulo.trim());
        evidencia.setDescripcion(descripcion);
        evidencia.setTipo(tipoNormalizado);
        evidencia.setUrlArchivo("/uploads/" + relPath);
        evidencia.setVisibleCliente(true);
        return EvidenciaDto.from(evidenciaService.guardar(evidencia));
    }

    @PatchMapping("/evidencias/{id}/visibilidad")
    public EvidenciaDto cambiarVisibilidad(@PathVariable Long id,
                                           @RequestBody ActualizarVisibilidadRequest request) {
        evidenciaService.toggleVisibilidad(id);
        EvidenciaEnvio evidencia = evidenciaService.buscar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidencia no encontrada: " + id));
        return EvidenciaDto.from(evidencia);
    }

    @DeleteMapping("/evidencias/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEvidencia(@PathVariable Long id) {
        EvidenciaEnvio evidencia = evidenciaService.buscar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidencia no encontrada: " + id));
        uploadService.eliminarArchivo(evidencia.getUrlArchivo().replaceFirst("^/uploads/", ""));
        evidenciaService.eliminar(id);
    }

    private TrackingDto toTrackingDto(EnvioTracking envio) {
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
        }).toList());
        dto.setEvidencias(evidenciaService.listarPorEnvio(envio.getId()).stream()
                .map(EvidenciaDto::from)
                .toList());
        return dto;
    }
}
