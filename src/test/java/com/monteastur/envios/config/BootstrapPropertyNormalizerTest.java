package com.monteastur.envios.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapPropertyNormalizerTest {

    @Test
    void normalizeJdbcUrlReplacesSemicolonsWithAmpersands() {
        String normalized = BootstrapPropertyNormalizer.normalizeJdbcUrl(
            "jdbc:mysql://localhost:3306/envios_paraguay_cms_test?useSSL=false;serverTimezone=UTC;allowPublicKeyRetrieval=true"
        );

        assertThat(normalized)
            .isEqualTo("jdbc:mysql://localhost:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    }

    @Test
    void normalizeJdbcUrlLeavesValidUrlUntouched() {
        String original = "jdbc:mysql://localhost:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        assertThat(BootstrapPropertyNormalizer.normalizeJdbcUrl(original)).isEqualTo(original);
    }
}
