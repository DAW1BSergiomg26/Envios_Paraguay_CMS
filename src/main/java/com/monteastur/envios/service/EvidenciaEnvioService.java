package com.monteastur.envios.service;

import com.monteastur.envios.model.EvidenciaEnvio;
import com.monteastur.envios.repository.EvidenciaEnvioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EvidenciaEnvioService {

    private final EvidenciaEnvioRepository repo;

    public EvidenciaEnvioService(EvidenciaEnvioRepository repo) {
        this.repo = repo;
    }

    public List<EvidenciaEnvio> listarPorEnvio(Long envioId) {
        return repo.findByEnvioTrackingIdOrderByFechaSubidaDesc(envioId);
    }

    public List<EvidenciaEnvio> listarPorEnvioParaCliente(Long envioId) {
        return repo.findByEnvioTrackingIdAndVisibleClienteTrueOrderByFechaSubidaDesc(envioId);
    }

    public void toggleVisibilidad(Long id) {
        repo.findById(id).ifPresent(ev -> {
            Boolean actual = ev.getVisibleCliente();
            ev.setVisibleCliente(actual == null ? true : !actual);
            repo.save(ev);
        });
    }

    public EvidenciaEnvio guardar(EvidenciaEnvio evidencia) {
        evidencia.setFechaSubida(LocalDateTime.now());
        return repo.save(evidencia);
    }

    public Optional<EvidenciaEnvio> buscar(Long id) {
        return repo.findById(id);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}
