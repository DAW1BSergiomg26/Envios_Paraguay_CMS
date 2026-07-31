package com.monteastur.envios.repository;

import com.monteastur.envios.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByEnvioIdOrderByFechaCreacionDesc(Long envioId);
}
