package com.monteastur.envios.service;

import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.WebhookConfig;
import com.monteastur.envios.model.WebhookLog;
import com.monteastur.envios.exception.WebhookDispatchException;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.WebhookConfigRepository;
import com.monteastur.envios.repository.WebhookLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
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

import java.time.Duration;
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

    private Retry retryPorDefecto() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(50), 2))
                .retryExceptions(WebhookDispatchException.class)
                .build();
        return Retry.of("webhook", config);
    }

    private CircuitBreaker circuitBreakerCerrado() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .recordExceptions(WebhookDispatchException.class)
                .build();
        return CircuitBreaker.of("webhook", config);
    }

    @BeforeEach
    void setUp() {
        service = new WebhookDispatchService(envioTrackingRepository, webhookConfigRepository,
                webhookLogRepository, new WebhookPayloadBuilder(new ObjectMapper()), webhookRestClient,
                retryPorDefecto(), circuitBreakerCerrado());
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

        verify(requestBodyUriSpec, org.mockito.Mockito.times(3)).uri(anyString());
        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository).save(captor.capture());
        WebhookLog log = captor.getValue();
        assertThat(log.isExitoso()).isFalse();
        assertThat(log.getErrorMensaje()).contains("tras 3 intento(s)");
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

        verify(requestBodyUriSpec, org.mockito.Mockito.times(3)).uri(anyString());
        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository).save(captor.capture());
        WebhookLog log = captor.getValue();
        assertThat(log.isExitoso()).isFalse();
        assertThat(log.getResponseStatus()).isNull();
        assertThat(log.getErrorMensaje()).contains("connect timed out");
        assertThat(log.getErrorMensaje()).contains("tras 3 intento(s)");
    }

    @Test
    void dosFallosTransitoriosYSegundoExito_tresIntentosYAuditoriaFinalExitosa() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-7"))
                .thenReturn(Optional.of(envioConCliente("MT-7", 10L)));
        WebhookConfig config = new WebhookConfig(10L, "https://hook.a/endpoint", "secret-a");
        when(webhookConfigRepository.findByClienteIdAndActivoTrue(10L)).thenReturn(List.of(config));
        when(responseSpec.toBodilessEntity())
                .thenThrow(new ResourceAccessException("connect timed out"))
                .thenThrow(new ResourceAccessException("connect timed out"))
                .thenReturn(ResponseEntity.ok().build());

        service.despachar(evento("MT-7", 7L));

        verify(requestBodyUriSpec, org.mockito.Mockito.times(3)).uri(anyString());
        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository).save(captor.capture());
        WebhookLog log = captor.getValue();
        assertThat(log.isExitoso()).isTrue();
        assertThat(log.getResponseStatus()).isEqualTo(200);
        assertThat(log.getErrorMensaje()).isNull();
    }

    @Test
    void respuesta400_noReintentaYAuditaFalloPorIntento() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-8"))
                .thenReturn(Optional.of(envioConCliente("MT-8", 10L)));
        WebhookConfig config = new WebhookConfig(10L, "https://hook.a/endpoint", "secret-a");
        when(webhookConfigRepository.findByClienteIdAndActivoTrue(10L)).thenReturn(List.of(config));
        when(responseSpec.toBodilessEntity()).thenThrow(
                new RestClientResponseException("400 Bad Request", 400, "Bad Request",
                        new HttpHeaders(), new byte[0], null));

        service.despachar(evento("MT-8", 8L));

        verify(requestBodyUriSpec, org.mockito.Mockito.times(1)).uri(anyString());
        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository).save(captor.capture());
        WebhookLog log = captor.getValue();
        assertThat(log.isExitoso()).isFalse();
        assertThat(log.getResponseStatus()).isEqualTo(400);
    }

    @Test
    void circuitBreakerAbierto_auditaFalloDeContingenciaSinLlamarAlSink() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .recordExceptions(WebhookDispatchException.class)
                .build();
        CircuitBreaker abiertoRapido = CircuitBreaker.of("webhook", config);
        service = new WebhookDispatchService(envioTrackingRepository, webhookConfigRepository,
                webhookLogRepository, new WebhookPayloadBuilder(new ObjectMapper()), webhookRestClient,
                retryPorDefecto(), abiertoRapido);

        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-9"))
                .thenReturn(Optional.of(envioConCliente("MT-9", 10L)));
        WebhookConfig config1 = new WebhookConfig(10L, "https://hook.a/endpoint", "secret-a");
        when(webhookConfigRepository.findByClienteIdAndActivoTrue(10L)).thenReturn(List.of(config1));
        when(responseSpec.toBodilessEntity()).thenThrow(
                new RestClientResponseException("500 Internal Server Error", 500, "Internal Server Error",
                        new HttpHeaders(), new byte[0], null));

        service.despachar(evento("MT-9", 9L));
        assertThat(abiertoRapido.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        service.despachar(evento("MT-9", 10L));

        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        WebhookLog finalBreaker = captor.getAllValues().get(1);
        assertThat(finalBreaker.isExitoso()).isFalse();
        assertThat(finalBreaker.getErrorMensaje())
                .contains("CircuitBreaker", "abierto", "contingencia");
    }
}
