package com.grupb2.casarural.service;

import com.grupb2.casarural.model.EnvioTracking;
import com.grupb2.casarural.model.EventoTracking;
import com.grupb2.casarural.repository.EventoTrackingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EventoTrackingService {

    private final EventoTrackingRepository repo;

    public EventoTrackingService(EventoTrackingRepository repo) {
        this.repo = repo;
    }

    public EventoTracking crearEvento(EnvioTracking envio, String estadoAnterior) {
        if (estadoAnterior != null && estadoAnterior.equals(envio.getEstado())) {
            return null;
        }
        String icono = getIcono(envio.getEstado());
        String color = getColor(envio.getEstado());
        String titulo = getTitulo(envio.getEstado());
        String ubicacion = envio.getUbicacionActual();
        String descripcion = envio.getObservaciones();

        EventoTracking evento = new EventoTracking();
        evento.setEnvioTracking(envio);
        evento.setEstado(envio.getEstado());
        evento.setTitulo(titulo);
        evento.setDescripcion(descripcion);
        evento.setUbicacion(ubicacion);
        evento.setIcono(icono);
        evento.setColor(color);
        evento.setFechaEvento(LocalDateTime.now());
        evento.setCreadoPor("admin");
        evento.setVisibleCliente(true);

        return repo.save(evento);
    }

    public EventoTracking crearEventoInicial(EnvioTracking envio) {
        EventoTracking evento = new EventoTracking();
        evento.setEnvioTracking(envio);
        evento.setEstado(envio.getEstado());
        evento.setTitulo("Envío registrado en MONTEASTUR");
        evento.setDescripcion(envio.getObservaciones());
        evento.setUbicacion(envio.getOrigen());
        evento.setIcono("📋");
        evento.setColor("#3f6338");
        evento.setFechaEvento(LocalDateTime.now());
        evento.setCreadoPor("admin");
        evento.setVisibleCliente(true);
        return repo.save(evento);
    }

    public List<EventoTracking> listarPorEnvio(Long envioId) {
        return repo.findByEnvioTrackingIdOrderByFechaEventoDesc(envioId);
    }

    public List<EventoTracking> listarRecientesPorEnvio(Long envioId) {
        return repo.findTop20ByEnvioTrackingIdOrderByFechaEventoDesc(envioId);
    }

    public Optional<EventoTracking> buscar(Long id) {
        return repo.findById(id);
    }

    private String getIcono(String estado) {
        if (estado == null) return "📦";
        return switch (estado) {
            case "RECIBIDO" -> "📦";
            case "EN_ADUANA_ORIGEN", "EN_ADUANA_DESTINO" -> "🛃";
            case "EN_TRANSITO" -> "🚢";
            case "EN_REPARTO" -> "🇵🇾";
            case "ENTREGADO" -> "✅";
            default -> "📦";
        };
    }

    private String getColor(String estado) {
        if (estado == null) return "#555";
        return switch (estado) {
            case "RECIBIDO" -> "#27ae60";
            case "EN_ADUANA_ORIGEN", "EN_ADUANA_DESTINO" -> "#d4762a";
            case "EN_TRANSITO" -> "#1d4ed8";
            case "EN_REPARTO" -> "#d62828";
            case "ENTREGADO" -> "#1e7e34";
            default -> "#555";
        };
    }

    private String getTitulo(String estado) {
        if (estado == null) return "Actualización del envío";
        return switch (estado) {
            case "RECIBIDO" -> "Envío recibido en origen";
            case "EN_ADUANA_ORIGEN" -> "Gestión aduanera en origen";
            case "EN_TRANSITO" -> "Envío en tránsito internacional";
            case "EN_ADUANA_DESTINO" -> "Control aduanero en Paraguay";
            case "EN_REPARTO" -> "En reparto";
            case "ENTREGADO" -> "Entrega completada";
            default -> "Actualización del envío";
        };
    }
}
