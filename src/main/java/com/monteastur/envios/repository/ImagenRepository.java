package com.monteastur.envios.repository;

import com.monteastur.envios.model.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImagenRepository extends JpaRepository<Imagen, Long> {
    List<Imagen> findAllByOrderByOrdenAsc();
}
