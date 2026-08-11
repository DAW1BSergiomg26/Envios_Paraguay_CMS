package com.monteastur.envios.repository;

import com.monteastur.envios.model.EvidenciaEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenciaEnvioRepository extends JpaRepository<EvidenciaEnvio, Long> {
    List<EvidenciaEnvio> findByEnvioTrackingIdOrderByFechaSubidaDesc(Long envioId);
    List<EvidenciaEnvio> findByEnvioTrackingIdAndVisibleClienteTrueOrderByFechaSubidaDesc(Long envioId);
    void deleteByEnvioTrackingId(Long envioId);
}
