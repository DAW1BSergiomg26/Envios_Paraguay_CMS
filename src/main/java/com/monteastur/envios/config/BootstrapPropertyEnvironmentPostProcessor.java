package com.monteastur.envios.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class BootstrapPropertyEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "bootstrapPropertyNormalizer";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> overrides = new LinkedHashMap<>();

        String datasourceUrl = environment.getProperty("spring.datasource.url");
        if (StringUtils.hasText(datasourceUrl)) {
            String normalized = BootstrapPropertyNormalizer.normalizeJdbcUrl(datasourceUrl);
            if (!datasourceUrl.equals(normalized)) {
                overrides.put("spring.datasource.url", normalized);
            }
        }

        String redisHost = environment.getProperty("spring.data.redis.host");
        if (!StringUtils.hasText(redisHost)) {
            String aliasHost = environment.getProperty("SPRING_DATA_REDIS_HOST");
            if (StringUtils.hasText(aliasHost)) {
                overrides.put("spring.data.redis.host", aliasHost);
            }
        }

        String redisPort = environment.getProperty("spring.data.redis.port");
        if (!StringUtils.hasText(redisPort)) {
            String aliasPort = environment.getProperty("REDIS_PORT");
            if (StringUtils.hasText(aliasPort)) {
                overrides.put("spring.data.redis.port", aliasPort);
            }
        }

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
