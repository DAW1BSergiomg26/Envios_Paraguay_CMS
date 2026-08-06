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

    @Test
    void normalizeJdbcUrlPrependsJdbcPrefixWhenMissing() {
        String normalized = BootstrapPropertyNormalizer.normalizeJdbcUrl(
            "mysql://host:3306/envios_paraguay_cms?useSSL=false"
        );

        assertThat(normalized)
            .isEqualTo("jdbc:mysql://host:3306/envios_paraguay_cms?useSSL=false");
    }

    @Test
    void normalizeJdbcUrlPrependsPrefixAndReplacesSemicolons() {
        String normalized = BootstrapPropertyNormalizer.normalizeJdbcUrl(
            "mysql://host:3306/envios_paraguay_cms?useSSL=false;serverTimezone=UTC"
        );

        assertThat(normalized)
            .isEqualTo("jdbc:mysql://host:3306/envios_paraguay_cms?useSSL=false&serverTimezone=UTC");
    }

    @Test
    void normalizeJdbcUrlDoesNotDoublePrefixOnUppercaseJdbc() {
        String original = "JDBC:mysql://host:3306/envios_paraguay_cms";

        assertThat(BootstrapPropertyNormalizer.normalizeJdbcUrl(original)).isEqualTo(original);
    }

    @Test
    void normalizeJdbcUrlLeavesBlankAndNullUntouched() {
        assertThat(BootstrapPropertyNormalizer.normalizeJdbcUrl(null)).isNull();
        assertThat(BootstrapPropertyNormalizer.normalizeJdbcUrl("")).isEmpty();
        assertThat(BootstrapPropertyNormalizer.normalizeJdbcUrl("   ")).isEqualTo("   ");
    }
}
