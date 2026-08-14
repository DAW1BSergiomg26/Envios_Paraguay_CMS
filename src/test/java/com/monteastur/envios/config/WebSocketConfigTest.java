package com.monteastur.envios.config;

import com.monteastur.envios.controller.EnvioWebSocketController;
import com.monteastur.envios.dto.websocket.EnvioEstadoWsMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageMappingInfo;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {WebSocketConfig.class, EnvioWebSocketController.class})
class WebSocketConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SimpAnnotationMethodMessageHandler messageHandler;

    @Autowired
    private EnvioWebSocketController controller;

    @Test
    void brokerSimpleRegistradoConPrefijosTopicYQueue() {
        SimpleBrokerMessageHandler broker =
                context.getBean("simpleBrokerMessageHandler", SimpleBrokerMessageHandler.class);

        assertThat(broker.getDestinationPrefixes())
                .contains("/topic", "/queue");
    }

    @Test
    void canalesInternosDeMensajeriaPresentesEnElContexto() {
        assertThat(context.containsBean("clientInboundChannel")).isTrue();
        assertThat(context.containsBean("clientOutboundChannel")).isTrue();
        assertThat(context.containsBean("brokerChannel")).isTrue();
    }

    @Test
    void controladorRegistradoParaDestinoActualizarEstado() {
        Set<String> destinosRegistrados = messageHandler.getHandlerMethods().keySet().stream()
                .map(SimpMessageMappingInfo::getDestinationConditions)
                .map(DestinationPatternsMessageCondition::getPatterns)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        assertThat(destinosRegistrados).contains("/actualizar-estado");
    }

    @Test
    void mapeoAnotadoConDestinoYTopicDePublicacion() throws NoSuchMethodException {
        Method metodo = EnvioWebSocketController.class
                .getMethod("actualizarEstado", EnvioEstadoWsMessage.class);

        assertThat(metodo.getAnnotation(MessageMapping.class).value())
                .containsExactly("/actualizar-estado");
        assertThat(metodo.getAnnotation(SendTo.class).value())
                .containsExactly("/topic/envios");
    }

    @Test
    void controladorRetransmiteElMismoMensajeRecibido() {
        EnvioEstadoWsMessage mensaje = new EnvioEstadoWsMessage(
                7L, "MT-1", "EN_TRANSITO", Instant.parse("2026-08-13T10:00:00Z"));

        EnvioEstadoWsMessage resultado = controller.actualizarEstado(mensaje);

        assertThat(resultado).isSameAs(mensaje);
    }
}
