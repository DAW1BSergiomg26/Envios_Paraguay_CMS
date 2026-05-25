package com.monteastur.envios.repository;

import com.monteastur.envios.model.TextoLegal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TextoLegalRepository extends JpaRepository<TextoLegal, Long> {
    Optional<TextoLegal> findBySlug(String slug);
}
