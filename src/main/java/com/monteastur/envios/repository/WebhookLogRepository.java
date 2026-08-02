package com.monteastur.envios.repository;

import com.monteastur.envios.model.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {

    List<WebhookLog> findByEnvioIdOrderByFechaCreacionDesc(Long envioId);

    List<WebhookLog> findByWebhookIdOrderByFechaCreacionDesc(Long webhookId);
}
