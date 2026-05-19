package com.grupb2.casarural.service;

import com.grupb2.casarural.model.Cliente;
import com.grupb2.casarural.model.EnvioTracking;
import com.grupb2.casarural.model.NotificacionEnvio;
import com.grupb2.casarural.repository.NotificacionEnvioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacionEnvioService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionEnvioService.class);

    private final NotificacionEnvioRepository repo;
    private final EmailService emailService;

    public NotificacionEnvioService(NotificacionEnvioRepository repo, EmailService emailService) {
        this.repo = repo;
        this.emailService = emailService;
    }

    public NotificacionEnvio crearNotificacion(EnvioTracking envio, String estadoAnterior) {
        String nuevoEstado = envio.getEstado();
        if (estadoAnterior != null && estadoAnterior.equals(nuevoEstado)) {
            return null;
        }
        Cliente cliente = envio.getCliente();
        if (cliente == null) {
            return null;
        }
        String mensaje = getMensajePorEstado(nuevoEstado);
        String tipo = getTipoNotificacion(nuevoEstado);
        String asunto = "Actualización de tu envío MONTEASTUR: " + envio.getCodigoUnico();
        String cuerpo = buildCuerpoEmail(cliente, envio, mensaje);

        NotificacionEnvio notif = new NotificacionEnvio();
        notif.setEnvioTracking(envio);
        notif.setCliente(cliente);
        notif.setEmailDestino(cliente.getEmail());
        notif.setAsunto(asunto);
        notif.setMensaje(mensaje);
        notif.setTipo(tipo);
        notif.setEstadoEnvio(nuevoEstado);
        notif.setFechaCreacion(LocalDateTime.now());
        notif.setEnviada(false);

        try {
            emailService.enviarACliente(asunto, cuerpo, cliente.getEmail());
            notif.setEnviada(true);
            notif.setFechaEnvio(LocalDateTime.now());
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido";
            notif.setErrorEnvio(errorMsg);
            log.warn("No se pudo notificar a {} para envío {}: {}",
                    cliente.getEmail(), envio.getCodigoUnico(), errorMsg);
        }

        return repo.save(notif);
    }

    public List<NotificacionEnvio> listarPorEnvio(Long envioId) {
        return repo.findByEnvioTrackingIdOrderByFechaCreacionDesc(envioId);
    }

    public List<NotificacionEnvio> listarPorCliente(Long clienteId) {
        return repo.findByClienteIdOrderByFechaCreacionDesc(clienteId);
    }

    private String getMensajePorEstado(String estado) {
        if (estado == null) return "Tu envío ha sido actualizado.";
        return switch (estado) {
            case "RECIBIDO" -> "Tu envío fue registrado correctamente.";
            case "EN_ADUANA_ORIGEN", "EN_ADUANA_DESTINO" -> "Tu envío está en gestión aduanera.";
            case "EN_TRANSITO" -> "Tu envío está en tránsito internacional.";
            case "EN_REPARTO" -> "Tu envío llegó a Paraguay o está en proceso de recepción.";
            case "ENTREGADO" -> "Tu envío fue entregado correctamente.";
            default -> "Tu envío ha sido actualizado.";
        };
    }

    private String getTipoNotificacion(String estado) {
        if (estado == null) return "OTRO";
        return switch (estado) {
            case "RECIBIDO" -> "RECIBIDO";
            case "EN_ADUANA_ORIGEN", "EN_ADUANA_DESTINO" -> "ADUANAS";
            case "EN_TRANSITO" -> "TRANSITO";
            case "EN_REPARTO" -> "PARAGUAY";
            case "ENTREGADO" -> "ENTREGADO";
            default -> "OTRO";
        };
    }

    private String buildCuerpoEmail(Cliente cliente, EnvioTracking envio, String mensajeEstado) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hola ").append(cliente.getNombre()).append(",\n\n");
        sb.append("Tu envío ").append(envio.getCodigoUnico()).append(" ha sido actualizado.\n\n");
        sb.append("Nuevo estado: ").append(mensajeEstado).append("\n");
        sb.append("Ubicación actual: ").append(envio.getUbicacionActual()).append("\n");
        if (envio.getObservaciones() != null && !envio.getObservaciones().isEmpty()) {
            sb.append("Observaciones: ").append(envio.getObservaciones()).append("\n");
        }
        sb.append("\nPodés consultar el detalle completo en tu panel de cliente:\n");
        sb.append("https://monteastur.com/cliente/login\n\n");
        sb.append("— MONTEASTUR ENVIOS\n");
        sb.append("Transporte Asturias ↔ Paraguay\n");
        sb.append("Tel: +34 642 687 292");
        return sb.toString();
    }
}
