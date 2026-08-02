package com.monteastur.envios.repository;

import com.monteastur.envios.model.DocumentoGenerado;
import com.monteastur.envios.model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoGeneradoRepository extends JpaRepository<DocumentoGenerado, Long> {
    List<DocumentoGenerado> findByOrderByFechaCreacionDesc();
    List<DocumentoGenerado> findAllByTipoOrderByFechaCreacionDesc(TipoDocumento tipo);
    long countByTipo(TipoDocumento tipo);
}
