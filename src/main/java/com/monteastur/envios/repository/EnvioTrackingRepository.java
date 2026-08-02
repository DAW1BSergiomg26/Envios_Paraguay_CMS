package com.monteastur.envios.repository;

import com.monteastur.envios.model.EnvioTracking;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvioTrackingRepository extends JpaRepository<EnvioTracking, Long>, JpaSpecificationExecutor<EnvioTracking> {
    Optional<EnvioTracking> findByCodigoUnico(String codigoUnico);
    boolean existsByCodigoUnico(String codigoUnico);
    List<EnvioTracking> findAllByOrderByUltimaActualizacionDesc();
    List<EnvioTracking> findByClienteIdOrderByUltimaActualizacionDesc(Long clienteId);

    List<EnvioTracking> findByBatchIdOrderByCodigoUnicoAsc(Long batchId);
    long countByBatchId(Long batchId);

    @EntityGraph(attributePaths = "cliente")
    Optional<EnvioTracking> findWithClienteById(Long id);

    @EntityGraph(attributePaths = "cliente")
    Optional<EnvioTracking> findWithClienteByCodigoUnico(String codigoUnico);
}
