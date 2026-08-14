package com.monteastur.envios.listener;

import com.monteastur.envios.dto.websocket.EnvioEstadoWsMessage;
import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new WebSocketEventListener(messagingTemplate);
    }

    private EstadoEnvioActualizadoEvent evento() {
        return new EstadoEnvioActualizadoEvent(1L, "MT-TEST-1", "RECIBIDO", "EN_REPARTO",
                LocalDateTime.of(2026, 8, 14, 10, 30));
    }

    @Test
    void publicaElMensajeEnElTopicDeEnvios() {
        listener.manejar(evento());

        ArgumentCaptor<EnvioEstadoWsMessage> captor =
                ArgumentCaptor.forClass(EnvioEstadoWsMessage.class);
        verify(messagingTemplate).convertAndSend(anyString(), captor.capture());

        EnvioEstadoWsMessage mensaje = captor.getValue();
        assertThat(mensaje.getEnvioId()).isEqualTo(1L);
        assertThat(mensaje.getTracking()).isEqualTo("MT-TEST-1");
        assertThat(mensaje.getEstado()).isEqualTo("EN_REPARTO");
    }

    @Test
    void publicaEnElTopicCorrecto() {
        listener.manejar(evento());

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EnvioEstadoWsMessage> mensajeCaptor =
                ArgumentCaptor.forClass(EnvioEstadoWsMessage.class);
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), mensajeCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("/topic/envios");
    }

    @Test
    void mapeaLaFechaALaZonaUtc() {
        listener.manejar(evento());

        ArgumentCaptor<EnvioEstadoWsMessage> captor =
                ArgumentCaptor.forClass(EnvioEstadoWsMessage.class);
        verify(messagingTemplate).convertAndSend(anyString(), captor.capture());

        assertThat(captor.getValue().getTimestamp())
                .isEqualTo(LocalDateTime.of(2026, 8, 14, 10, 30).toInstant(ZoneOffset.UTC));
    }

    @Test
    void tragaLasExcepcionesDelTemplate() {
        doThrow(new RuntimeException("boom"))
                .when(messagingTemplate).convertAndSend(anyString(), any(EnvioEstadoWsMessage.class));
        assertThatCode(() -> listener.manejar(evento())).doesNotThrowAnyException();
    }
}
