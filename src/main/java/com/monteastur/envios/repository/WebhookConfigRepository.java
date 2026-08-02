package com.monteastur.envios.repository;

import com.monteastur.envios.model.WebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {

    List<WebhookConfig> findByClienteId(Long clienteId);

    List<WebhookConfig> findByClienteIdAndActivoTrue(Long clienteId);
}
