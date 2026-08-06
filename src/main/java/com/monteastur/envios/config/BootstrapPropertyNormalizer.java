package com.monteastur.envios.config;

import java.util.Locale;

import org.springframework.util.StringUtils;

public final class BootstrapPropertyNormalizer {

    private static final String JDBC_PREFIX = "jdbc:";

    private BootstrapPropertyNormalizer() {
    }

    public static String normalizeJdbcUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            return rawUrl;
        }

        String normalized = rawUrl;
        if (!normalized.toLowerCase(Locale.ROOT).startsWith(JDBC_PREFIX)) {
            normalized = JDBC_PREFIX + normalized;
        }

        return normalized.replace(";", "&");
    }
}
