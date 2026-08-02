package com.monteastur.envios.repository;

import com.monteastur.envios.model.BatchImportError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchImportErrorRepository extends JpaRepository<BatchImportError, Long> {
    List<BatchImportError> findByBatchIdOrderByLineaNumeroAsc(Long batchId);
    long countByBatchId(Long batchId);
}
