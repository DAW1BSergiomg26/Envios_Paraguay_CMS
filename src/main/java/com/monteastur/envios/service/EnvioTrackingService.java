package com.monteastur.envios.service;

import com.monteastur.envios.dto.api.PublicTrackingDto;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(value = "envios.tracking", unless = "#result == null")
    public PublicTrackingDto buscarPorCodigo(String codigo) {
        return repo.findByCodigoUnico(codigo.trim().toUpperCase())
                .map(PublicTrackingDto::from)
                .orElse(null);
    }

    @Cacheable("envios.dashboard")
    public List<EnvioTracking> listarTodos() {
        return repo.findAllByOrderByUltimaActualizacionDesc();
    }

    @CacheEvict(value = "envios.tracking", allEntries = true)
    public EnvioTracking guardar(EnvioTracking envio) {
        envio.setUltimaActualizacion(LocalDateTime.now());
        if (envio.getFechaCreacion() == null) {
            envio.setFechaCreacion(LocalDateTime.now());
        }
        return repo.save(envio);
    }

    @CacheEvict(value = "envios.tracking", allEntries = true)
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
