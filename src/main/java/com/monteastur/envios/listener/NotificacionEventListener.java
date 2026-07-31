package com.monteastur.envios.listener;

import com.monteastur.envios.event.EstadoEnvioActualizadoEvent;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.Notificacion;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.NotificacionRepository;
import com.monteastur.envios.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificacionEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionEventListener.class);

    private final EnvioTrackingRepository envioTrackingRepository;
    private final NotificacionRepository notificacionRepository;
    private final EmailService emailService;

    @Value("${app.notification.mail.enabled:true}")
    private boolean notificacionesHabilitadas;

    @Value("${app.notification.tracking.base-url:http://localhost:8080/tracking}")
    private String baseUrl;

    public NotificacionEventListener(EnvioTrackingRepository envioTrackingRepository,
                                     NotificacionRepository notificacionRepository,
                                     EmailService emailService) {
        this.envioTrackingRepository = envioTrackingRepository;
        this.notificacionRepository = notificacionRepository;
        this.emailService = emailService;
    }

    @Async
    @Transactional(readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void manejar(EstadoEnvioActualizadoEvent event) {
        if (!notificacionesHabilitadas) {
            log.info("Notificaciones deshabilitadas. Se omite el evento de {}", event.codigoRastreo());
            return;
        }
        EnvioTracking envio = envioTrackingRepository
                .findWithClienteByCodigoUnico(event.codigoRastreo())
                .orElse(null);
        String destinatario = envio != null && envio.getCliente() != null
                ? envio.getCliente().getEmail()
                : null;
        if (envio == null || destinatario == null || destinatario.isBlank()) {
            notificacionRepository.save(new Notificacion(
                    event.envioId(),
                    Notificacion.EstadoNotificacion.OMITIDO_SIN_DESTINATARIO,
                    null,
                    "Notificación omitida",
                    "Envío sin cliente o email para notificar el cambio de estado",
                    "Envío sin cliente o email"));
            log.info("Notificación OMITIDO_SIN_DESTINATARIO para {}", event.codigoRastreo());
            return;
        }
        String asunto = "Tu envío " + event.codigoRastreo() + " ahora está en estado: " + event.estadoNuevo();
        String cuerpo = "Hola " + (envio.getCliente().getNombre() != null ? envio.getCliente().getNombre() : "")
                + ",\n\nTu envío con código " + event.codigoRastreo()
                + " ha cambiado de estado:\n"
                + "• Anterior: " + event.estadoAnterior() + "\n"
                + "• Actual: " + event.estadoNuevo() + "\n\n"
                + "Sigue tu paquete aquí: " + baseUrl + "/" + event.codigoRastreo();
        try {
            emailService.enviarCorreoSimple(destinatario, asunto, cuerpo);
            notificacionRepository.save(new Notificacion(
                    event.envioId(),
                    Notificacion.EstadoNotificacion.ENVIADO,
                    destinatario,
                    asunto,
                    cuerpo,
                    null));
            log.info("Notificación ENVIADO para {}", event.codigoRastreo());
        } catch (Exception e) {
            log.error("No se pudo enviar la notificación para {}", event.codigoRastreo(), e);
            notificacionRepository.save(new Notificacion(
                    event.envioId(),
                    Notificacion.EstadoNotificacion.FALLIDO,
                    destinatario,
                    "Notificación fallida",
                    "No se pudo notificar el cambio de estado del envío " + event.codigoRastreo(),
                    e.getMessage()));
        }
    }
}
