package com.monteastur.envios.repository;

import com.monteastur.envios.model.MensajeContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeContactoRepository extends JpaRepository<MensajeContacto, Long> {
    List<MensajeContacto> findAllByOrderByFechaEnvioDesc();
    List<MensajeContacto> findTop5ByOrderByFechaEnvioDesc();
}
