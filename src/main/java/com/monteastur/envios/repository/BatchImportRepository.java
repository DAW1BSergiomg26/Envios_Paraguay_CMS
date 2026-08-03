package com.monteastur.envios.repository;

import com.monteastur.envios.model.BatchImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchImportRepository extends JpaRepository<BatchImport, Long> {
    List<BatchImport> findByClienteIdOrderByFechaCreacionDesc(Long clienteId);

    List<BatchImport> findAllByOrderByIdDesc();
}
