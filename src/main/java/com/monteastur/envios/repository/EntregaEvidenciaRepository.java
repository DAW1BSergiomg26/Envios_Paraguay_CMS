package com.monteastur.envios.repository;

import com.monteastur.envios.model.EntregaEvidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntregaEvidenciaRepository extends JpaRepository<EntregaEvidencia, Long> {
    Optional<EntregaEvidencia> findByEnvioId(Long envioId);
    boolean existsByEnvioId(Long envioId);
}
