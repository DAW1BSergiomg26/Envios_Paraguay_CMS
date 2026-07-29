package com.monteastur.envios.repository;

import com.monteastur.envios.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findAllByOrderByCreatedAtDesc();
    List<Reserva> findTop5ByOrderByCreatedAtDesc();
    long countByEstado(String estado);

    @Query("SELECT r FROM Reserva r WHERE r.estado IN ('pendiente', 'aprobada', 'confirmada') AND r.fechaEntrada < :fin AND r.fechaSalida > :inicio")
    List<Reserva> findOcupadasEnRango(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT COUNT(r) > 0 FROM Reserva r WHERE r.estado IN ('pendiente', 'aprobada', 'confirmada') AND r.fechaEntrada < :fechaSalida AND r.fechaSalida > :fechaEntrada")
    boolean existsOverlap(@Param("fechaEntrada") LocalDate fechaEntrada, @Param("fechaSalida") LocalDate fechaSalida);

    @Query("SELECT COUNT(r) > 0 FROM Reserva r WHERE r.estado IN ('pendiente', 'aprobada', 'confirmada') AND r.fechaEntrada < :fechaSalida AND r.fechaSalida > :fechaEntrada AND r.id != :excludeId")
    boolean existsOverlapExcluding(@Param("fechaEntrada") LocalDate fechaEntrada, @Param("fechaSalida") LocalDate fechaSalida, @Param("excludeId") Long excludeId);
}
