package com.monteastur.envios.service;

import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReservaService {

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
}
