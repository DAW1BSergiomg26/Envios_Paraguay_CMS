package com.monteastur.envios.listener;

import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.Notificacion;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.NotificacionRepository;
import com.monteastur.envios.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionEventListenerTest {

    @Mock
    private EnvioTrackingRepository envioTrackingRepository;

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private EmailService emailService;

    private NotificacionEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificacionEventListener(envioTrackingRepository, notificacionRepository, emailService);
        ReflectionTestUtils.setField(listener, "notificacionesHabilitadas", true);
        ReflectionTestUtils.setField(listener, "baseUrl", "http://localhost:8080/tracking");
    }

    private EstadoEnvioActualizadoEvent evento(String codigo, Long envioId) {
        return new EstadoEnvioActualizadoEvent(envioId, codigo, "RECIBIDO", "EN_TRANSITO", LocalDateTime.now());
    }

    private EnvioTracking envioConCliente(String codigo, String emailCliente) {
        Cliente cliente = new Cliente(emailCliente, "password123", "Cliente Test", "+595 000 000");
        EnvioTracking envio = new EnvioTracking(codigo, "RECIBIDO", "Destinatario",
                "Origen", "Destino", "10 kg", "Docs");
        envio.setCliente(cliente);
        return envio;
    }

    @Test
    void enviaEmailYRegistraEnviado() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-TEST-1"))
                .thenReturn(Optional.of(envioConCliente("MT-TEST-1", "cliente@example.com")));
        doNothing().when(emailService).enviarCorreoSimple(anyString(), anyString(), anyString());

        listener.manejar(evento("MT-TEST-1", 1L));

        verify(emailService).enviarCorreoSimple(
                eq("cliente@example.com"), anyString(), contains("MT-TEST-1"));
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion guardada = captor.getValue();
        assertThat(guardada.getEstado()).isEqualTo(Notificacion.EstadoNotificacion.ENVIADO);
        assertThat(guardada.getDestinatario()).isEqualTo("cliente@example.com");
        assertThat(guardada.getEnvioId()).isEqualTo(1L);
        assertThat(guardada.getMensaje()).contains("http://localhost:8080/tracking/MT-TEST-1");
    }

    @Test
    void registraOmitidoCuandoElClienteNoTieneEmail() {
        EnvioTracking envio = new EnvioTracking("MT-TEST-2", "RECIBIDO", "Destinatario",
                "Origen", "Destino", "10 kg", "Docs");
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-TEST-2"))
                .thenReturn(Optional.of(envio));

        listener.manejar(evento("MT-TEST-2", 2L));

        verify(emailService, never()).enviarCorreoSimple(anyString(), anyString(), anyString());
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado())
                .isEqualTo(Notificacion.EstadoNotificacion.OMITIDO_SIN_DESTINATARIO);
        assertThat(captor.getValue().getDestinatario()).isNull();
    }

    @Test
    void registraOmitidoCuandoElEnvioNoExiste() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-TEST-3"))
                .thenReturn(Optional.empty());

        listener.manejar(evento("MT-TEST-3", 3L));

        verify(emailService, never()).enviarCorreoSimple(anyString(), anyString(), anyString());
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado())
                .isEqualTo(Notificacion.EstadoNotificacion.OMITIDO_SIN_DESTINATARIO);
    }

    @Test
    void registraFallidoCuandoElEnvioDeCorreoLanzaExcepcion() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-TEST-4"))
                .thenReturn(Optional.of(envioConCliente("MT-TEST-4", "cliente@example.com")));
        doThrow(new RuntimeException("SMTP caído"))
                .when(emailService).enviarCorreoSimple(anyString(), anyString(), anyString());

        listener.manejar(evento("MT-TEST-4", 4L));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(Notificacion.EstadoNotificacion.FALLIDO);
        assertThat(captor.getValue().getErrorMensaje()).isEqualTo("SMTP caído");
    }

    @Test
    void noHaceNadaCuandoLasNotificacionesEstanDeshabilitadas() {
        ReflectionTestUtils.setField(listener, "notificacionesHabilitadas", false);

        listener.manejar(evento("MT-TEST-5", 5L));

        verifyNoInteractions(envioTrackingRepository, notificacionRepository, emailService);
    }
}
