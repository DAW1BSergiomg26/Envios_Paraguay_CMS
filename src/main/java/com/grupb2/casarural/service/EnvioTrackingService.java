package com.grupb2.casarural.service;

import com.grupb2.casarural.model.EnvioTracking;
import com.grupb2.casarural.repository.EnvioTrackingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EnvioTrackingService {

    private final EnvioTrackingRepository repo;

    public EnvioTrackingService(EnvioTrackingRepository repo) {
        this.repo = repo;
    }

    public Optional<EnvioTracking> buscarPorCodigo(String codigo) {
        return repo.findByCodigoUnico(codigo.trim().toUpperCase());
    }

    public List<EnvioTracking> listarTodos() {
        return repo.findAllByOrderByUltimaActualizacionDesc();
    }

    public EnvioTracking guardar(EnvioTracking envio) {
        envio.setUltimaActualizacion(LocalDateTime.now());
        if (envio.getFechaCreacion() == null) {
            envio.setFechaCreacion(LocalDateTime.now());
        }
        return repo.save(envio);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    public Optional<EnvioTracking> buscarPorId(Long id) {
        return repo.findById(id);
    }

    public String generarCodigo() {
        long count = repo.count() + 1;
        return String.format("MT-%d-%04d", LocalDateTime.now().getYear(), count);
    }

    public long count() {
        return repo.count();
    }
}
