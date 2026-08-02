package com.monteastur.envios.service;

import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.WebhookConfig;
import com.monteastur.envios.model.WebhookLog;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.WebhookConfigRepository;
import com.monteastur.envios.repository.WebhookLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookDispatchServiceTest {

    @Mock
    private EnvioTrackingRepository envioTrackingRepository;

    @Mock
    private WebhookConfigRepository webhookConfigRepository;

    @Mock
    private WebhookLogRepository webhookLogRepository;

    @Mock
    private RestClient webhookRestClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private WebhookDispatchService service;

    @BeforeEach
    void setUp() {
        service = new WebhookDispatchService(envioTrackingRepository, webhookConfigRepository,
                webhookLogRepository, new WebhookPayloadBuilder(new ObjectMapper()), webhookRestClient);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080/tracking");
        org.mockito.Mockito.lenient().when(webhookRestClient.post()).thenReturn(requestBodyUriSpec);
        org.mockito.Mockito.lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        org.mockito.Mockito.lenient().when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        org.mockito.Mockito.lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        org.mockito.Mockito.lenient().when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        org.mockito.Mockito.lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    private EstadoEnvioActualizadoEvent evento(String codigo, Long envioId) {
        return new EstadoEnvioActualizadoEvent(envioId, codigo, "RECIBIDO", "EN_TRANSITO",
                LocalDateTime.of(2026, 8, 2, 10, 30, 0));
    }

    private EnvioTracking envioConCliente(String codigo, Long clienteId) {
        Cliente cliente = new Cliente("cliente@example.com", "password123", "Cliente", "+595");
        ReflectionTestUtils.setField(cliente, "id", clienteId);
        EnvioTracking envio = new EnvioTracking(codigo, "RECIBIDO", "Destinatario",
                "Origen", "Destino", "10 kg", "Docs");
        envio.setCliente(cliente);
        return envio;
    }

    @Test
    void envioSinCliente_noDespachaNiAudita() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1"))
                .thenReturn(Optional.of(new EnvioTracking("MT-1", "RECIBIDO", "Dest",
                        "Origen", "Destino", "10 kg", "Docs")));

        service.despachar(evento("MT-1", 1L));

        verifyNoInteractions(webhookConfigRepository);
        verify(webhookLogRepository, never()).save(any());
    }

    @Test
    void envioInexistente_noDespachaNiAudita() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-2"))
                .thenReturn(Optional.empty());

        service.despachar(evento("MT-2", 2L));

        verifyNoInteractions(webhookConfigRepository);
        verify(webhookLogRepository, never()).save(any());
    }

    @Test
    void clienteSinWebhooksActivos_noAudita() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-3"))
                .thenReturn(Optional.of(envioConCliente("MT-3", 10L)));
        when(webhookConfigRepository.findByClienteIdAndActivoTrue(10L)).thenReturn(List.of());

        service.despachar(evento("MT-3", 3L));

        verify(webhookLogRepository, never()).save(any());
        verify(webhookRestClient, never()).post();
    }

    @Test
    void despachaCadaWebhookActivoConFirmaCorrecta() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-4"))
                .thenReturn(Optional.of(envioConCliente("MT-4", 10L)));
        WebhookConfig configA = new WebhookConfig(10L, "https://hook.a/endpoint", "secret-a");
        WebhookConfig configB = new WebhookConfig(10L, "https://hook.b/endpoint", "secret-b");
        when(webhookConfigRepository.findByClienteIdAndActivoTrue(10L))
                .thenReturn(List.of(configA, configB));
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        service.despachar(evento("MT-4", 4L));

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodyUriSpec, org.mockito.Mockito.times(2)).uri(uriCaptor.capture());
        assertThat(uriCaptor.getAllValues())
                .containsExactlyInAnyOrder("https://hook.a/endpoint", "https://hook.b/endpoint");

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodySpec, org.mockito.Mockito.times(2)).header(eq("X-Signature-256"), headerCaptor.capture());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodySpec, org.mockito.Mockito.times(2)).body(bodyCaptor.capture());
        String payload = bodyCaptor.getAllValues().get(0);
        assertThat(headerCaptor.getAllValues())
                .containsExactlyInAnyOrder(
                        WebhookSignature.hmacSha256("secret-a", payload),
                        WebhookSignature.hmacSha256("secret-b", payload));

        ArgumentCaptor<WebhookLog> logCaptor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository, org.mockito.Mockito.times(2)).save(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .allSatisfy(log -> {
                    assertThat(log.isExitoso()).isTrue();
                    assertThat(log.getResponseStatus()).isEqualTo(200);
                    assertThat(log.getErrorMensaje()).isNull();
                    assertThat(log.getEnvioId()).isEqualTo(4L);
                });
    }

    @Test
    void respuesta500_registraFalloConStatusYError() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-5"))
                .thenReturn(Optional.of(envioConCliente("MT-5", 10L)));
        WebhookConfig config = new WebhookConfig(10L, "https://hook.a/endpoint", "secret-a");
        when(webhookConfigRepository.findByClienteIdAndActivoTrue(10L)).thenReturn(List.of(config));
        when(responseSpec.toBodilessEntity()).thenThrow(
                new RestClientResponseException("500 Internal Server Error", 500, "Internal Server Error",
                        new HttpHeaders(), new byte[0], null));

        service.despachar(evento("MT-5", 5L));

        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository).save(captor.capture());
        WebhookLog log = captor.getValue();
        assertThat(log.isExitoso()).isFalse();
        assertThat(log.getResponseStatus()).isEqualTo(500);
        assertThat(log.getErrorMensaje()).isEqualTo("HTTP 500");
    }

    @Test
    void errorDeRed_registraFalloSinStatus() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-6"))
                .thenReturn(Optional.of(envioConCliente("MT-6", 10L)));
        WebhookConfig config = new WebhookConfig(10L, "https://hook.a/endpoint", "secret-a");
        when(webhookConfigRepository.findByClienteIdAndActivoTrue(10L)).thenReturn(List.of(config));
        when(responseSpec.toBodilessEntity()).thenThrow(
                new ResourceAccessException("connect timed out"));

        service.despachar(evento("MT-6", 6L));

        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository).save(captor.capture());
        WebhookLog log = captor.getValue();
        assertThat(log.isExitoso()).isFalse();
        assertThat(log.getResponseStatus()).isNull();
        assertThat(log.getErrorMensaje()).contains("connect timed out");
    }
}
