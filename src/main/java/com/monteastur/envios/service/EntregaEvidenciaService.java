package com.monteastur.envios.service;

import com.monteastur.envios.dto.api.EntregaEvidenciaDto;
import com.monteastur.envios.dto.api.RegistrarEntregaRequest;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntregaEvidenciaService {

    private final EntregaEvidenciaRepository entregaRepository;
    private final EnvioTrackingRepository envioTrackingRepository;
    private final EnvioTrackingService envioTrackingService;
    private final EventoTrackingService eventoTrackingService;

    public EntregaEvidenciaService(EntregaEvidenciaRepository entregaRepository,
                                   EnvioTrackingRepository envioTrackingRepository,
                                   EnvioTrackingService envioTrackingService,
                                   EventoTrackingService eventoTrackingService) {
        this.entregaRepository = entregaRepository;
        this.envioTrackingRepository = envioTrackingRepository;
        this.envioTrackingService = envioTrackingService;
        this.eventoTrackingService = eventoTrackingService;
    }

    @Transactional
    @CacheEvict(value = "envios.dashboard", allEntries = true)
    public EntregaEvidencia registrarEntrega(String codigo, RegistrarEntregaRequest request) {
        EntregaValidator.validar(request);
        EnvioTracking envio = envioTrackingRepository.findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Tracking no encontrado: " + codigo));
        if (entregaRepository.existsByEnvioId(envio.getId())) {
            throw new ConflictException("El envío " + codigo + " ya tiene evidencia de entrega registrada");
        }
        EntregaEvidencia evidencia = new EntregaEvidencia(envio, request.getReceptorNombre(),
                request.getReceptorDocumento(), request.getFirmaBase64(),
                request.getLatitud(), request.getLongitud(), request.getNotas());
        EntregaEvidencia guardada = entregaRepository.save(evidencia);
        String estadoAnterior = envio.getEstado();
        EnvioTracking actualizado = envioTrackingService.actualizarEstado(codigo, "ENTREGADO");
        eventoTrackingService.crearEvento(actualizado, estadoAnterior);
        return guardada;
    }

    @Transactional(readOnly = true)
    public EntregaEvidenciaDto obtenerEntrega(String codigo) {
        EnvioTracking envio = envioTrackingRepository.findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Tracking no encontrado: " + codigo));
        EntregaEvidencia evidencia = entregaRepository.findByEnvioId(envio.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe evidencia de entrega para el envío: " + codigo));
        return EntregaEvidenciaDto.from(evidencia);
    }
}
