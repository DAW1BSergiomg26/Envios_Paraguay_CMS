package com.monteastur.envios.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void enviosOpenAPI_exponeEmailDeContactoDelNuevoDominio() {
        OpenAPI openApi = config.enviosOpenAPI();

        assertThat(openApi.getInfo().getContact().getEmail())
            .isEqualTo("admin@monteastur.com");
    }

    @Test
    void enviosOpenAPI_noExponeEmailHeredadoDeCasaRural() {
        OpenAPI openApi = config.enviosOpenAPI();

        assertThat(openApi.getInfo().getContact().getEmail())
            .doesNotContain("casarrural");
    }
}
