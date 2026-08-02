package com.monteastur.envios.service.web;

import com.monteastur.envios.dto.web.ClientDashboardView;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientDashboardServiceTest {

    @Mock
    private EnvioTrackingRepository envioTrackingRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClientDashboardService service;

    private Cliente cliente() {
        Cliente cliente = new Cliente("cliente@test.com", "x", "Cliente Test", null);
        cliente.setId(7L);
        return cliente;
    }

    private EnvioTracking envio(String codigo, String estado, String peso) {
        EnvioTracking envio = new EnvioTracking(codigo, estado, "Destinatario Test",
                "Origen", "Destino", peso, "Contenido");
        envio.setId(1L);
        return envio;
    }

    @Test
    void metricasConPesosValidosInvalidosYMix() {
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente()));
        when(envioTrackingRepository.findByClienteIdOrderByUltimaActualizacionDesc(7L))
                .thenReturn(List.of(
                        envio("MT-A", "ENTREGADO", "10 kg"),
                        envio("MT-B", "EN_TRANSITO", "5,5 kg"),
                        envio("MT-C", "EN_REPARTO", "ab/oo"),
                        envio("MT-D", "RECIBIDO", null)
                ));

        ClientDashboardView view = service.cargarDashboard(7L);

        assertThat(view.getClienteId()).isEqualTo(7L);
        assertThat(view.getClienteNombre()).isEqualTo("Cliente Test");
        assertThat(view.getClienteEmail()).isEqualTo("cliente@test.com");
        assertThat(view.getTotalEnvios()).isEqualTo(4);
        assertThat(view.getEnviosEntregados()).isEqualTo(1);
        assertThat(view.getEnviosActivos()).isEqualTo(3);
        assertThat(view.getPesoTotalKg()).isEqualTo(15.5);
        assertThat(view.getPesoActivoKg()).isEqualTo(5.5);
        assertThat(view.getEnvios()).hasSize(4);
    }

    @Test
    void clienteInexistente_retornaNull() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.cargarDashboard(99L)).isNull();
    }
}
