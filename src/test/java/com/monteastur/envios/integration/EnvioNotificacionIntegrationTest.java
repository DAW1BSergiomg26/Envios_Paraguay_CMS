package com.monteastur.envios.integration;

import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.Notificacion;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.NotificacionRepository;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.EnvioTrackingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class EnvioNotificacionIntegrationTest {

    @Autowired
    private EnvioTrackingService envioTrackingService;

    @Autowired
    private EnvioTrackingRepository envioTrackingRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private EmailService emailService;

    private Long envioId;
    private Long clienteId;

    @AfterEach
    void limpiar() {
        if (envioId != null) {
            envioTrackingRepository.deleteById(envioId);
        }
        if (clienteId != null) {
            clienteRepository.deleteById(clienteId);
        }
    }

    @Test
    void transicionConCliente_registraNotificacionEnviada() {
        String emailCliente = "cliente-integracion-" + System.nanoTime() + "@example.com";
        String codigo = "PY-TEST-" + System.nanoTime();

        EnvioTracking envioGuardado = transactionTemplate.execute(status -> {
            Cliente cliente = new Cliente(emailCliente, "password123", "Cliente Test", "+595 000 000");
            Cliente clientePersistido = clienteRepository.save(cliente);
            EnvioTracking envio = new EnvioTracking(codigo, "RECIBIDO", "Destinatario Test",
                    "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos");
            envio.setCliente(clientePersistido);
            EnvioTracking guardado = envioTrackingService.guardar(envio);
            clienteId = guardado.getCliente().getId();
            return guardado;
        });
        envioId = envioGuardado.getId();

        transactionTemplate.executeWithoutResult(status ->
                envioTrackingService.actualizarEstado(codigo, "EN_TRANSITO"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Notificacion> notificaciones =
                    notificacionRepository.findByEnvioIdOrderByFechaCreacionDesc(envioId);
            assertThat(notificaciones).hasSize(1);
            Notificacion notificacion = notificaciones.get(0);
            assertThat(notificacion.getEstado()).isEqualTo(Notificacion.EstadoNotificacion.ENVIADO);
            assertThat(notificacion.getDestinatario()).isEqualTo(emailCliente);
            assertThat(notificacion.getMensaje()).contains(codigo);
        });

        verify(emailService, timeout(10000))
                .enviarCorreoSimple(eq(emailCliente), anyString(), contains(codigo));
    }

    @Test
    void transicionSinCliente_registraOmitido() {
        String codigo = "PY-TEST-" + System.nanoTime();

        EnvioTracking envioGuardado = transactionTemplate.execute(status ->
                envioTrackingService.guardar(new EnvioTracking(codigo, "RECIBIDO", "Destinatario Test",
                        "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos")));
        envioId = envioGuardado.getId();

        transactionTemplate.executeWithoutResult(status ->
                envioTrackingService.actualizarEstado(codigo, "EN_TRANSITO"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Notificacion> notificaciones =
                    notificacionRepository.findByEnvioIdOrderByFechaCreacionDesc(envioId);
            assertThat(notificaciones).hasSize(1);
            assertThat(notificaciones.get(0).getEstado())
                    .isEqualTo(Notificacion.EstadoNotificacion.OMITIDO_SIN_DESTINATARIO);
        });

        verify(emailService, never()).enviarCorreoSimple(anyString(), anyString(), anyString());
    }
}
