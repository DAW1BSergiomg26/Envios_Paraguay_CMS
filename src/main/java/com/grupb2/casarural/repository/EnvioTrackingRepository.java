package com.grupb2.casarural.repository;

import com.grupb2.casarural.model.EnvioTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvioTrackingRepository extends JpaRepository<EnvioTracking, Long> {
    Optional<EnvioTracking> findByCodigoUnico(String codigoUnico);
    List<EnvioTracking> findAllByOrderByUltimaActualizacionDesc();
}
