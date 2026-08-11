package com.monteastur.envios.service;

import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EventoTrackingRepository;
import com.monteastur.envios.repository.EvidenciaEnvioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvioTrackingServiceActualizarEstadoTest {

    @Mock
    private EnvioTrackingRepository repo;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private EventoTrackingService eventoTrackingService;

    @Mock
    private EventoTrackingRepository eventoRepo;

    @Mock
    private EvidenciaEnvioRepository evidenciaRepo;

    private EnvioTrackingService newService() {
        return new EnvioTrackingService(repo, publisher, eventoTrackingService, eventoRepo, evidenciaRepo);
    }

    @Test
    void cambiaEstadoGuardaYPublicaElEvento() {
        Cliente cliente = new Cliente("cliente@example.com", "x", "Cliente", "+595 000 000");
        EnvioTracking envio = new EnvioTracking("MT-UPD-1", "RECIBIDO", "Destinatario",
                "Origen", "Destino", "10 kg", "Docs");
        envio.setCliente(cliente);
        envio.setId(99L);
        when(repo.findWithClienteByCodigoUnico("MT-UPD-1")).thenReturn(Optional.of(envio));
        when(repo.save(envio)).thenReturn(envio);

        EnvioTrackingService service = newService();
        EnvioTracking actualizado = service.actualizarEstado("MT-UPD-1", "EN_TRANSITO");

        assertThat(actualizado.getEstado()).isEqualTo("EN_TRANSITO");
        assertThat(actualizado.getUltimaActualizacion()).isNotNull();
        verify(repo).save(envio);
        ArgumentCaptor<EstadoEnvioActualizadoEvent> captor = ArgumentCaptor.forClass(EstadoEnvioActualizadoEvent.class);
        verify(publisher).publishEvent(captor.capture());
        EstadoEnvioActualizadoEvent event = captor.getValue();
        assertThat(event.codigoRastreo()).isEqualTo("MT-UPD-1");
        assertThat(event.envioId()).isEqualTo(99L);
        assertThat(event.estadoAnterior()).isEqualTo("RECIBIDO");
        assertThat(event.estadoNuevo()).isEqualTo("EN_TRANSITO");
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void mismoEstadoNoPublicaEvento() {
        EnvioTracking envio = new EnvioTracking("MT-UPD-2", "EN_TRANSITO", "Destinatario",
                "Origen", "Destino", "10 kg", "Docs");
        when(repo.findWithClienteByCodigoUnico("MT-UPD-2")).thenReturn(Optional.of(envio));

        EnvioTrackingService service = newService();
        EnvioTracking resultado = service.actualizarEstado("MT-UPD-2", "EN_TRANSITO");

        assertThat(resultado.getEstado()).isEqualTo("EN_TRANSITO");
        verify(publisher, never()).publishEvent(any(EstadoEnvioActualizadoEvent.class));
        verify(repo, never()).save(any(EnvioTracking.class));
    }

    @Test
    void enviaoInexistenteLanzaResourceNotFound() {
        when(repo.findWithClienteByCodigoUnico(anyString())).thenReturn(Optional.empty());
        EnvioTrackingService service = newService();

        assertThatThrownBy(() -> service.actualizarEstado("MT-NO-EXISTE", "EN_TRANSITO"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(publisher, never()).publishEvent(any(EstadoEnvioActualizadoEvent.class));
    }
}
