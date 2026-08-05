package com.monteastur.envios.migration;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsMigrationTest {

    @Test
    void v10_creaLosTresIndicesDeAnalitica() throws Exception {
        String sql;
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V10__create_analytics_indexes.sql")) {
            assertThat(in).as("la migración V10 debe existir en classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql).contains("idx_envios_fecha_estado ON envios_tracking(fecha_creacion, estado)");
        assertThat(sql).contains("idx_envios_origen_destino ON envios_tracking(origen, destino)");
        assertThat(sql).contains("idx_webhook_logs_exitoso ON webhook_logs(exitoso, fecha_creacion)");
        assertThat(sql).doesNotContain("estado_actual");
    }
}
