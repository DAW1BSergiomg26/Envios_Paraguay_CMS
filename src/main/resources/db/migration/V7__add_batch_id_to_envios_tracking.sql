ALTER TABLE envios_tracking ADD COLUMN batch_id BIGINT NULL;
ALTER TABLE envios_tracking ADD CONSTRAINT fk_envios_batch
    FOREIGN KEY (batch_id) REFERENCES batch_imports(id) ON DELETE SET NULL;
ALTER TABLE envios_tracking ADD INDEX idx_envios_batch_id (batch_id);
