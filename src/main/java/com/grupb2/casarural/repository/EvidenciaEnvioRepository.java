package com.grupb2.casarural.repository;

import com.grupb2.casarural.model.EvidenciaEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenciaEnvioRepository extends JpaRepository<EvidenciaEnvio, Long> {
    List<EvidenciaEnvio> findByEnvioTrackingIdOrderByFechaSubidaDesc(Long envioId);
}
