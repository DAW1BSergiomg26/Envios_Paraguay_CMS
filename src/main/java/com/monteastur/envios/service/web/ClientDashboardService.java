package com.monteastur.envios.service.web;

import com.monteastur.envios.dto.web.ClientDashboardView;
import com.monteastur.envios.dto.web.EnvioResumenView;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.pdf.PesoUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Servicio del dashboard de cliente con caché Redis y métricas de peso.
 * Los pesos no parseables por PesoUtil se ignoran en las sumas.
 */
@Service
public class ClientDashboardService {

    private final EnvioTrackingRepository envioTrackingRepository;
    private final ClienteRepository clienteRepository;

    public ClientDashboardService(EnvioTrackingRepository envioTrackingRepository,
                                  ClienteRepository clienteRepository) {
        this.envioTrackingRepository = envioTrackingRepository;
        this.clienteRepository = clienteRepository;
    }

    @Cacheable(value = "envios.cliente.dashboard", key = "#clienteId", unless = "#result == null")
    public ClientDashboardView cargarDashboard(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (cliente == null) {
            return null;
        }
        List<EnvioTracking> envios = envioTrackingRepository
                .findByClienteIdOrderByUltimaActualizacionDesc(clienteId);
        int entregados = 0;
        double pesoTotal = 0;
        double pesoActivo = 0;
        List<EnvioResumenView> resumenes = new ArrayList<>();
        for (EnvioTracking envio : envios) {
            boolean esEntregado = "ENTREGADO".equals(envio.getEstado());
            if (esEntregado) {
                entregados++;
            }
            OptionalDouble peso = PesoUtil.parsear(envio.getPeso());
            if (peso.isPresent()) {
                pesoTotal += peso.getAsDouble();
                if (!esEntregado) {
                    pesoActivo += peso.getAsDouble();
                }
            }
            resumenes.add(EnvioResumenView.from(envio));
        }
        int total = envios.size();
        return new ClientDashboardView(cliente.getId(), cliente.getNombre(), cliente.getEmail(),
                total, total - entregados, entregados, pesoTotal, pesoActivo, resumenes);
    }
}
