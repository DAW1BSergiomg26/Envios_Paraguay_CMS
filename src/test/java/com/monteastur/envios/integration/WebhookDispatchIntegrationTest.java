package com.monteastur.envios.integration;

import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.WebhookConfig;
import com.monteastur.envios.model.WebhookLog;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.WebhookConfigRepository;
import com.monteastur.envios.repository.WebhookLogRepository;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.EnvioTrackingService;
import com.monteastur.envios.service.WebhookSignature;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class WebhookDispatchIntegrationTest {

    private static final String CABECERA_FIRMA = "X-Signature-256";

    private static HttpServer sink;
    private static volatile String receivedBody;
    private static volatile String receivedSignature;
    private static volatile int respondCode = 200;

    @Autowired
    private EnvioTrackingService envioTrackingService;

    @Autowired
    private EnvioTrackingRepository envioTrackingRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private WebhookConfigRepository webhookConfigRepository;

    @Autowired
    private WebhookLogRepository webhookLogRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private EmailService emailService;

    private Long envioId;
    private Long clienteId;
    private Long webhookConfigId;

    @BeforeAll
    static void arrancarSink() throws IOException {
        sink = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        sink.createContext("/webhook", exchange -> {
            try {
                receivedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                receivedSignature = exchange.getRequestHeaders().getFirst(CABECERA_FIRMA);
                exchange.sendResponseHeaders(respondCode, -1);
            } finally {
                exchange.close();
            }
        });
        sink.start();
    }

    @AfterAll
    static void pararSink() {
        if (sink != null) {
            sink.stop(0);
        }
    }

    @AfterEach
    void limpiar() {
        if (webhookConfigId != null) {
            webhookConfigRepository.deleteById(webhookConfigId);
            webhookConfigId = null;
        }
        if (envioId != null) {
            envioTrackingRepository.deleteById(envioId);
            envioId = null;
        }
        if (clienteId != null) {
            clienteRepository.deleteById(clienteId);
            clienteId = null;
        }
    }

    private static String sinkUrl() {
        return "http://127.0.0.1:" + sink.getAddress().getPort() + "/webhook";
    }

    @Test
    void transicionConWebhookActivo_despachaFirmadoYAudita() {
        String email = "webhook-int-" + System.nanoTime() + "@example.com";
        String codigo = "PY-WH-" + System.nanoTime();
        String secret = "secreto-integracion";
        respondCode = 200;
        receivedBody = null;
        receivedSignature = null;

        EnvioTracking guardado = transactionTemplate.execute(status -> {
            Cliente cliente = new Cliente(email, "password123", "Cliente WH", "+595 000 000");
            Cliente persistido = clienteRepository.save(cliente);
            WebhookConfig config =
                    webhookConfigRepository.save(new WebhookConfig(persistido.getId(), sinkUrl(), secret));
            webhookConfigId = config.getId();
            EnvioTracking envio = new EnvioTracking(codigo, "RECIBIDO", "Destinatario WH",
                    "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos");
            envio.setCliente(persistido);
            EnvioTracking guardar = envioTrackingService.guardar(envio);
            clienteId = guardar.getCliente().getId();
            return guardar;
        });
        envioId = guardado.getId();

        transactionTemplate.executeWithoutResult(status ->
                envioTrackingService.actualizarEstado(codigo, "EN_TRANSITO"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(receivedBody).isNotNull();
            List<WebhookLog> logs = webhookLogRepository.findByEnvioIdOrderByFechaCreacionDesc(envioId);
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).isExitoso()).isTrue();
            assertThat(logs.get(0).getResponseStatus()).isEqualTo(200);
            assertThat(logs.get(0).getErrorMensaje()).isNull();
        });

        List<WebhookLog> logs = webhookLogRepository.findByEnvioIdOrderByFechaCreacionDesc(envioId);
        assertThat(logs.get(0).getPayload()).isEqualTo(receivedBody);
        assertThat(receivedSignature).isEqualTo(WebhookSignature.hmacSha256(secret, receivedBody));
        assertThat(receivedBody).contains(codigo).contains("EN_TRANSITO").contains("RECIBIDO");
    }

    @Test
    void transicionConWebhookQueResponde500_registraFallo() {
        String email = "webhook-int-" + System.nanoTime() + "@example.com";
        String codigo = "PY-WH-" + System.nanoTime();
        respondCode = 500;
        receivedBody = null;
        receivedSignature = null;

        EnvioTracking guardado = transactionTemplate.execute(status -> {
            Cliente cliente = new Cliente(email, "password123", "Cliente WH", "+595 000 000");
            Cliente persistido = clienteRepository.save(cliente);
            WebhookConfig config =
                    webhookConfigRepository.save(new WebhookConfig(persistido.getId(), sinkUrl(), "secreto"));
            webhookConfigId = config.getId();
            EnvioTracking envio = new EnvioTracking(codigo, "RECIBIDO", "Destinatario WH",
                    "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos");
            envio.setCliente(persistido);
            EnvioTracking guardar = envioTrackingService.guardar(envio);
            clienteId = guardar.getCliente().getId();
            return guardar;
        });
        envioId = guardado.getId();

        transactionTemplate.executeWithoutResult(status ->
                envioTrackingService.actualizarEstado(codigo, "EN_TRANSITO"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(receivedBody).isNotNull();
            List<WebhookLog> logs = webhookLogRepository.findByEnvioIdOrderByFechaCreacionDesc(envioId);
            assertThat(logs).hasSize(1);
            WebhookLog log = logs.get(0);
            assertThat(log.isExitoso()).isFalse();
            assertThat(log.getResponseStatus()).isEqualTo(500);
            assertThat(log.getErrorMensaje()).isEqualTo("HTTP 500");
        });
    }
}
