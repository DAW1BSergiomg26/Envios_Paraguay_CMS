package com.monteastur.envios.service;

import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EventoTrackingRepository;
import com.monteastur.envios.repository.EvidenciaEnvioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvioTrackingServiceTest {

    @Mock
    private EnvioTrackingRepository repo;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EventoTrackingService eventoTrackingService;

    @Mock
    private EventoTrackingRepository eventoRepo;

    @Mock
    private EvidenciaEnvioRepository evidenciaRepo;

    @Test
    void crear_guardaYRegistraEventoInicial() {
        EnvioTracking envio = new EnvioTracking("MT-CREAR-1", "RECIBIDO", "Destinatario",
                "Origen", "Destino", "10 kg", "Docs");
        when(repo.save(envio)).thenReturn(envio);

        EnvioTrackingService service = new EnvioTrackingService(
                repo, eventPublisher, eventoTrackingService, eventoRepo, evidenciaRepo);
        EnvioTracking resultado = service.crear(envio);

        assertThat(resultado).isSameAs(envio);
        verify(repo).save(envio);
        verify(eventoTrackingService).crearEventoInicial(any(EnvioTracking.class));
    }

    @Test
    void crear_registraEventoInicialTrasGuardar() {
        EnvioTracking envio = new EnvioTracking("MT-CREAR-2", "RECIBIDO", "Destinatario",
                "Origen", "Destino", "10 kg", "Docs");
        when(repo.save(envio)).thenReturn(envio);

        EnvioTrackingService service = new EnvioTrackingService(
                repo, eventPublisher, eventoTrackingService, eventoRepo, evidenciaRepo);
        service.crear(envio);

        InOrder inOrder = inOrder(repo, eventoTrackingService);
        inOrder.verify(repo).save(envio);
        inOrder.verify(eventoTrackingService).crearEventoInicial(any(EnvioTracking.class));
    }

    @Test
    void eliminar_borraEvidenciasEventosYEnvio() {
        EnvioTrackingService service = new EnvioTrackingService(
                repo, eventPublisher, eventoTrackingService, eventoRepo, evidenciaRepo);
        service.eliminar(7L);

        verify(evidenciaRepo).deleteByEnvioTrackingId(7L);
        verify(eventoRepo).deleteByEnvioTrackingId(7L);
        verify(repo).deleteById(7L);
    }

    @Test
    void eliminar_ordenaHijosAntesQueEnvio() {
        EnvioTrackingService service = new EnvioTrackingService(
                repo, eventPublisher, eventoTrackingService, eventoRepo, evidenciaRepo);
        service.eliminar(7L);

        InOrder inOrder = inOrder(evidenciaRepo, eventoRepo, repo);
        inOrder.verify(evidenciaRepo).deleteByEnvioTrackingId(7L);
        inOrder.verify(eventoRepo).deleteByEnvioTrackingId(7L);
        inOrder.verify(repo).deleteById(7L);
    }

    @Test
    void generarCodigo_devuelveFormatoMT() {
        when(repo.count()).thenReturn(41L);
        EnvioTrackingService service = new EnvioTrackingService(
                repo, eventPublisher, eventoTrackingService, eventoRepo, evidenciaRepo);

        String codigo = service.generarCodigo();

        assertThat(codigo).matches(Pattern.compile("MT-\\d{4}-0042"));
    }

    @Test
    void guardar_fijaFechaCreacionSiNula() {
        EnvioTracking envio = new EnvioTracking("MT-CREAR-3", "RECIBIDO", "Destinatario",
                "Origen", "Destino", "10 kg", "Docs");
        envio.setFechaCreacion(null);
        when(repo.save(envio)).thenReturn(envio);

        EnvioTrackingService service = new EnvioTrackingService(
                repo, eventPublisher, eventoTrackingService, eventoRepo, evidenciaRepo);
        EnvioTracking guardado = service.guardar(envio);

        assertThat(guardado.getFechaCreacion()).isNotNull();
        assertThat(guardado.getUltimaActualizacion()).isNotNull();
    }
}
