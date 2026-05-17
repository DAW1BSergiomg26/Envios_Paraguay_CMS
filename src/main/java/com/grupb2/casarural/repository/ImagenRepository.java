package com.grupb2.casarural.repository;

import com.grupb2.casarural.model.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImagenRepository extends JpaRepository<Imagen, Long> {
    List<Imagen> findAllByOrderByOrdenAsc();
}
