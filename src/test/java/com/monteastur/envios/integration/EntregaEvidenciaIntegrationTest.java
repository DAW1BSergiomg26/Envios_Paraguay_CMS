package com.monteastur.envios.integration;

import com.monteastur.envios.dto.api.EntregaEvidenciaDto;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.Notificacion;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EventoTrackingRepository;
import com.monteastur.envios.repository.NotificacionRepository;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.EnvioTrackingService;
import com.monteastur.envios.service.EntregaEvidenciaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class EntregaEvidenciaIntegrationTest {

    @Autowired private EntregaEvidenciaService entregaEvidenciaService;
    @Autowired private EntregaEvidenciaRepository entregaEvidenciaRepository;
    @Autowired private EnvioTrackingRepository envioTrackingRepository;
    @Autowired private EventoTrackingRepository eventoTrackingRepository;
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private EnvioTrackingService envioTrackingService;
    @Autowired private TransactionTemplate transactionTemplate;

    @MockitoBean private EmailService emailService;

    private Long envioId;

    private static final String PNG_1X1 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @AfterEach
    void limpiar() {
        if (envioId != null) {
            entregaEvidenciaRepository.deleteAll(entregaEvidenciaRepository.findByEnvioId(envioId).stream().toList());
            eventoTrackingRepository.deleteAll(eventoTrackingRepository.findByEnvioTrackingIdOrderByFechaEventoDesc(envioId));
            notificacionRepository.deleteAll(notificacionRepository.findByEnvioIdOrderByFechaCreacionDesc(envioId));
            envioTrackingRepository.deleteById(envioId);
        }
        envioId = null;
    }

    private EnvioTracking crearEnvioSinCliente(String codigo) {
        EnvioTracking guardado = transactionTemplate.execute(status -> {
            EnvioTracking envio = envioTrackingService.guardar(new EnvioTracking(codigo, "RECIBIDO",
                    "Destinatario Test", "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos"));
            return envio;
        });
        envioId = guardado.getId();
        return guardado;
    }

    @Test
    void registrarPod_marcaEntregadoPersisteYPropagaEventos() {
        String codigo = "PY-POD-" + System.nanoTime();
        EnvioTracking envio = crearEnvioSinCliente(codigo);

        com.monteastur.envios.dto.api.RegistrarEntregaRequest request =
                new com.monteastur.envios.dto.api.RegistrarEntregaRequest();
        request.setReceptorNombre("Ana López");
        request.setReceptorDocumento("12345678");
        request.setFirmaBase64(PNG_1X1);
        request.setLatitud(new BigDecimal("-25.2637421"));
        request.setLongitud(new BigDecimal("-57.575926"));

        EntregaEvidencia evidencia = entregaEvidenciaService.registrarEntrega(codigo, request);

        assertThat(evidencia.getId()).isNotNull();
        EnvioTracking recargado = envioTrackingRepository.findById(envio.getId()).orElseThrow();
        assertThat(recargado.getEstado()).isEqualTo("ENTREGADO");
        assertThat(entregaEvidenciaRepository.findByEnvioId(envio.getId())).isPresent();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Notificacion> notificaciones =
                    notificacionRepository.findByEnvioIdOrderByFechaCreacionDesc(envio.getId());
            assertThat(notificaciones).hasSize(1);
            assertThat(notificaciones.get(0).getEstado())
                    .isEqualTo(Notificacion.EstadoNotificacion.OMITIDO_SIN_DESTINATARIO);
        });

        assertThat(eventoTrackingRepository.findByEnvioTrackingIdOrderByFechaEventoDesc(envio.getId()))
                .anyMatch(e -> "ENTREGADO".equals(e.getEstado()));

        EntregaEvidenciaDto dto = entregaEvidenciaService.obtenerEntrega(codigo);
        assertThat(dto.getFirmaBase64()).isEqualTo(PNG_1X1);
        assertThat(dto.getCodigoRastreo()).isEqualTo(codigo);
    }

    @Test
    void registroDuplicado_lanza409() {
        String codigo = "PY-POD-" + System.nanoTime();
        crearEnvioSinCliente(codigo);

        com.monteastur.envios.dto.api.RegistrarEntregaRequest request =
                new com.monteastur.envios.dto.api.RegistrarEntregaRequest();
        request.setReceptorNombre("Ana López");
        request.setReceptorDocumento("12345678");
        request.setFirmaBase64(PNG_1X1);

        entregaEvidenciaService.registrarEntrega(codigo, request);

        assertThatThrownBy(() -> entregaEvidenciaService.registrarEntrega(codigo, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void obtenerSinEvidencia_lanza404() {
        String codigo = "PY-POD-" + System.nanoTime();
        crearEnvioSinCliente(codigo);

        assertThatThrownBy(() -> entregaEvidenciaService.obtenerEntrega(codigo))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
