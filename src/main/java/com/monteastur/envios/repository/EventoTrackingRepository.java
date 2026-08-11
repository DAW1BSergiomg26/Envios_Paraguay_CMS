package com.monteastur.envios.repository;

import com.monteastur.envios.model.EventoTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventoTrackingRepository extends JpaRepository<EventoTracking, Long> {
    List<EventoTracking> findByEnvioTrackingIdOrderByFechaEventoDesc(Long envioId);
    List<EventoTracking> findTop20ByEnvioTrackingIdOrderByFechaEventoDesc(Long envioId);
    void deleteByEnvioTrackingId(Long envioId);
}
