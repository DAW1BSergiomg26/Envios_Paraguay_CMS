package com.monteastur.envios.listener;

import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.service.WebhookDispatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WebhookEventListenerTest {

    @Mock
    private WebhookDispatchService dispatchService;

    private WebhookEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new WebhookEventListener(dispatchService);
        ReflectionTestUtils.setField(listener, "webhooksHabilitados", true);
    }

    private EstadoEnvioActualizadoEvent evento() {
        return new EstadoEnvioActualizadoEvent(1L, "MT-TEST-1", "RECIBIDO", "EN_TRANSITO",
                LocalDateTime.now());
    }

    @Test
    void delegaEnElServicioDeDespacho() {
        EstadoEnvioActualizadoEvent event = evento();
        listener.manejar(event);
        verify(dispatchService).despachar(event);
    }

    @Test
    void tragaLasExcepcionesDelDespacho() {
        doThrow(new RuntimeException("boom")).when(dispatchService).despachar(org.mockito.ArgumentMatchers.any());
        assertThatCode(() -> listener.manejar(evento())).doesNotThrowAnyException();
    }

    @Test
    void noDespachaCuandoLosWebhooksEstanDeshabilitados() {
        ReflectionTestUtils.setField(listener, "webhooksHabilitados", false);
        listener.manejar(evento());
        verifyNoInteractions(dispatchService);
    }
}
