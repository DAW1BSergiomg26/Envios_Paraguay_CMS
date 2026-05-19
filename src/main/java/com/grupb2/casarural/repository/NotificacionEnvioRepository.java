package com.grupb2.casarural.repository;

import com.grupb2.casarural.model.NotificacionEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionEnvioRepository extends JpaRepository<NotificacionEnvio, Long> {
    List<NotificacionEnvio> findByEnvioTrackingIdOrderByFechaCreacionDesc(Long envioId);
    List<NotificacionEnvio> findByClienteIdOrderByFechaCreacionDesc(Long clienteId);
}
