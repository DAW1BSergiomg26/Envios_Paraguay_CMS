package com.grupb2.casarural.service;

import com.grupb2.casarural.model.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:notificaciones@casarrural.com}")
    private String from;

    @Value("${spring.mail.to:admin@casarrural.com}")
    private String to;

    private void enviar(String asunto, String texto) {
        if (mailSender == null) {
            log.info("Email no configurado. Se saltó el envío de: {}", asunto);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(asunto);
            msg.setText(texto);
            mailSender.send(msg);
            log.info("Email enviado: {}", asunto);
        } catch (Exception e) {
            log.warn("No se pudo enviar email ({}): {}", asunto, e.getMessage());
        }
    }

    public void notificarContacto(String nombre, String email, String mensaje) {
        enviar("Nuevo mensaje de contacto - Casa Rural",
                "Nombre: " + nombre + "\n" +
                "Email: " + email + "\n\n" +
                "Mensaje:\n" + mensaje);
    }

    public void notificarReserva(Reserva r) {
        enviar("Nueva reserva - Casa Rural",
                "Cliente: " + r.getNombreCliente() + "\n" +
                "Email: " + r.getEmail() + "\n" +
                "Teléfono: " + (r.getTelefono() != null ? r.getTelefono() : "—") + "\n" +
                "Entrada: " + r.getFechaEntrada() + "\n" +
                "Salida: " + r.getFechaSalida() + "\n" +
                "Huéspedes: " + r.getNumeroHuespedes() + "\n" +
                "Comentarios: " + (r.getComentarios() != null ? r.getComentarios() : "—") + "\n\n" +
                "Estado: " + r.getEstado());
    }

    public void notificarReservaAprobada(Reserva r) {
        enviar("Reserva confirmada - Casa Rural",
                "La reserva de " + r.getNombreCliente() + " ha sido APROBADA.\n\n" +
                "Email: " + r.getEmail() + "\n" +
                "Entrada: " + r.getFechaEntrada() + "\n" +
                "Salida: " + r.getFechaSalida() + "\n" +
                "Huéspedes: " + r.getNumeroHuespedes());
    }
}
