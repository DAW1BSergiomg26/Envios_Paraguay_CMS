package com.monteastur.envios.service;

import com.monteastur.envios.dto.api.ActualizarReservaRequest;
import com.monteastur.envios.dto.api.CrearReservaPublicRequest;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class ReservaService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("PENDIENTE", "APROBADA", "CONFIRMADA", "CANCELADA");

    private static final Map<String, Set<String>> TRANSICIONES_PERMITIDAS = Map.of(
        "PENDIENTE", Set.of("APROBADA", "CANCELADA"),
        "APROBADA", Set.of("CONFIRMADA", "CANCELADA"),
        "CONFIRMADA", Set.of("CANCELADA")
    );

    private final ReservaRepository repo;

    public ReservaService(ReservaRepository repo) {
        this.repo = repo;
    }

    public Reserva crear(Reserva reserva) {
        return repo.save(reserva);
    }

    public Optional<Reserva> buscarPorId(Long id) {
        return repo.findById(id);
    }

    public List<Reserva> listarTodas() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    @Transactional
    public Reserva crearPublico(CrearReservaPublicRequest request) {
        if (request.getFechaEntrada().isBefore(LocalDate.now())) {
            throw new BadRequestException("La fecha de entrada no puede ser en el pasado");
        }
        if (!request.getFechaSalida().isAfter(request.getFechaEntrada())) {
            throw new BadRequestException("La fecha de salida debe ser posterior a la de entrada");
        }
        if (repo.existsOverlap(request.getFechaEntrada(), request.getFechaSalida())) {
            throw new ConflictException("Las fechas seleccionadas no están disponibles");
        }

        Reserva r = new Reserva(
            request.getNombreCliente(),
            request.getEmail(),
            request.getTelefono(),
            request.getFechaEntrada(),
            request.getFechaSalida(),
            request.getNumeroHuespedes(),
            request.getComentarios()
        );
        return repo.save(r);
    }

    @Transactional
    public Optional<Reserva> actualizar(Long id, ActualizarReservaRequest request) {
        return repo.findById(id).map(r -> {
            if (request.getNombreCliente() != null) r.setNombreCliente(request.getNombreCliente());
            if (request.getEmail() != null) r.setEmail(request.getEmail());
            if (request.getTelefono() != null) r.setTelefono(request.getTelefono());
            if (request.getNumeroHuespedes() != null) r.setNumeroHuespedes(request.getNumeroHuespedes());
            if (request.getComentarios() != null) r.setComentarios(request.getComentarios());

            boolean fechasCambiadas = (request.getFechaEntrada() != null && !request.getFechaEntrada().equals(r.getFechaEntrada()))
                                   || (request.getFechaSalida() != null && !request.getFechaSalida().equals(r.getFechaSalida()));

            if (request.getFechaEntrada() != null) r.setFechaEntrada(request.getFechaEntrada());
            if (request.getFechaSalida() != null) r.setFechaSalida(request.getFechaSalida());

            if (fechasCambiadas) {
                if (r.getFechaEntrada().isBefore(LocalDate.now())) {
                    throw new BadRequestException("La fecha de entrada no puede ser en el pasado");
                }
                if (!r.getFechaSalida().isAfter(r.getFechaEntrada())) {
                    throw new BadRequestException("La fecha de salida debe ser posterior a la de entrada");
                }
                if (repo.existsOverlapExcluding(r.getFechaEntrada(), r.getFechaSalida(), r.getId())) {
                    throw new ConflictException("Las fechas seleccionadas no están disponibles");
                }
            }

            return repo.save(r);
        });
    }

    @Transactional
    public Optional<Reserva> cambiarEstado(Long id, String nuevoEstado) {
        String estadoNormalizado = nuevoEstado.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(estadoNormalizado)) {
            throw new BadRequestException("Estado no válido: " + nuevoEstado);
        }

        return repo.findById(id).map(r -> {
            Set<String> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(r.getEstado(), Set.of());
            if (!permitidos.contains(estadoNormalizado)) {
                throw new ConflictException(
                    "Transición no permitida: " + r.getEstado() + " → " + estadoNormalizado
                );
            }
            r.setEstado(estadoNormalizado);
            return repo.save(r);
        });
    }

    public boolean verificarDisponibilidad(LocalDate fechaEntrada, LocalDate fechaSalida) {
        return !repo.existsOverlap(fechaEntrada, fechaSalida);
    }
}
