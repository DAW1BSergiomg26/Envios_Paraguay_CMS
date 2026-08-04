package com.monteastur.envios.config;

import org.springframework.util.StringUtils;

public final class BootstrapPropertyNormalizer {

    private BootstrapPropertyNormalizer() {
    }

    public static String normalizeJdbcUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            return rawUrl;
        }

        return rawUrl.replace(";", "&");
    }
}
