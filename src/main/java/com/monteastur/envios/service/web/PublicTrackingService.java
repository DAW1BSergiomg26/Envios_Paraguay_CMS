package com.monteastur.envios.service.web;

import com.monteastur.envios.dto.web.EvidenciaView;
import com.monteastur.envios.dto.web.EventoView;
import com.monteastur.envios.dto.web.EntregaView;
import com.monteastur.envios.dto.web.PublicTrackingView;
import com.monteastur.envios.metrics.BusinessMetrics;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de consulta pública de rastreo con caché Redis.
 * Devuelve un DTO plano (nunca entidades JPA). null si el código no existe.
 */
@Service
public class PublicTrackingService {

    private final EnvioTrackingRepository envioTrackingRepository;
    private final EventoTrackingService eventoTrackingService;
    private final EvidenciaEnvioService evidenciaEnvioService;
    private final EntregaEvidenciaRepository entregaEvidenciaRepository;
    private final BusinessMetrics businessMetrics;

    public PublicTrackingService(EnvioTrackingRepository envioTrackingRepository,
                                 EventoTrackingService eventoTrackingService,
                                 EvidenciaEnvioService evidenciaEnvioService,
                                 EntregaEvidenciaRepository entregaEvidenciaRepository,
                                 BusinessMetrics businessMetrics) {
        this.envioTrackingRepository = envioTrackingRepository;
        this.eventoTrackingService = eventoTrackingService;
        this.evidenciaEnvioService = evidenciaEnvioService;
        this.entregaEvidenciaRepository = entregaEvidenciaRepository;
        this.businessMetrics = businessMetrics;
    }

    @Cacheable(value = "envios.tracking.pagina", key = "#codigo", unless = "#result == null")
    public PublicTrackingView cargarPagina(String codigo) {
        Timer.Sample sample = businessMetrics.iniciarBusqueda();
        try {
            EnvioTracking envio = envioTrackingRepository
                    .findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
                    .orElse(null);
            if (envio == null) {
                businessMetrics.registrarBusqueda(sample, false);
                return null;
            }
            List<EventoView> eventos = eventoTrackingService.listarPorEnvio(envio.getId()).stream()
                    .map(EventoView::from)
                    .toList();
            List<EvidenciaView> evidencias = evidenciaEnvioService.listarPorEnvioParaCliente(envio.getId()).stream()
                    .map(EvidenciaView::from)
                    .toList();
            EntregaView entrega = null;
            if ("ENTREGADO".equals(envio.getEstado())) {
                entrega = entregaEvidenciaRepository.findByEnvioId(envio.getId())
                        .map(EntregaView::from)
                        .orElse(null);
            }
            PublicTrackingView vista = PublicTrackingView.from(envio, eventos, evidencias, entrega);
            businessMetrics.registrarBusqueda(sample, true);
            return vista;
        } catch (RuntimeException e) {
            businessMetrics.registrarBusqueda(sample, false);
            throw e;
        }
    }
}
