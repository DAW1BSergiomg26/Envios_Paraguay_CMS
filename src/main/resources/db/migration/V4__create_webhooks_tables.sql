CREATE TABLE webhooks_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    url VARCHAR(255) NOT NULL,
    secret_token VARCHAR(100) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhooks_config_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE webhook_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    webhook_id BIGINT NOT NULL,
    envio_id BIGINT NOT NULL,
    payload TEXT NOT NULL,
    response_status INT NULL,
    exitoso BOOLEAN NOT NULL,
    error_mensaje TEXT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook_logs_config FOREIGN KEY (webhook_id) REFERENCES webhooks_config(id) ON DELETE CASCADE,
    CONSTRAINT fk_webhook_logs_envio FOREIGN KEY (envio_id) REFERENCES envios_tracking(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
