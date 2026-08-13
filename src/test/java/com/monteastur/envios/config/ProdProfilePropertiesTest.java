package com.monteastur.envios.config;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ProdProfilePropertiesTest {

    private Properties loadProdProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application-prod.properties")) {
            assertThat(in).as("application-prod.properties debe existir").isNotNull();
            props.load(in);
        }
        return props;
    }

    @Test
    void desactivaShowSqlYEvolucionaConValidate() throws Exception {
        Properties p = loadProdProperties();
        assertThat(p.getProperty("spring.jpa.show-sql")).isEqualTo("false");
        assertThat(p.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(p.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }

    @Test
    void ocultaDetallesDeErrorYDetallesDeSaludNoAutorizados() throws Exception {
        Properties p = loadProdProperties();
        assertThat(p.getProperty("server.error.include-message")).isEqualTo("never");
        assertThat(p.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("when_authorized");
    }

    @Test
    void endpointsSensiblesDesactivadosEnProd() throws Exception {
        Properties p = loadProdProperties();
        assertThat(p.getProperty("app.push.test-enabled")).isEqualTo("false");
        assertThat(p.getProperty("app.demo-data")).isEqualTo("false");
    }
}
