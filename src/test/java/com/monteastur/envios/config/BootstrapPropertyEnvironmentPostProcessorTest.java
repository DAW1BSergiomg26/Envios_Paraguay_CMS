package com.monteastur.envios.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapPropertyEnvironmentPostProcessorTest {

    private final BootstrapPropertyEnvironmentPostProcessor processor =
        new BootstrapPropertyEnvironmentPostProcessor();

    private String normalizedDatasourceUrl(MockEnvironment environment) {
        processor.postProcessEnvironment(environment, new SpringApplication());
        return environment.getProperty("spring.datasource.url");
    }

    @Test
    void prependsJdbcPrefixToSpringDatasourceUrl() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("SPRING_DATASOURCE_URL", "mysql://host:3306/db?useSSL=false");

        assertThat(normalizedDatasourceUrl(environment))
            .isEqualTo("jdbc:mysql://host:3306/db?useSSL=false");
    }

    @Test
    void fallsBackToDatabaseUrlWhenSpringDatasourceUrlMissing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DATABASE_URL", "mysql://host:3306/db?useSSL=false");

        assertThat(normalizedDatasourceUrl(environment))
            .isEqualTo("jdbc:mysql://host:3306/db?useSSL=false");
    }

    @Test
    void springDatasourceUrlWinsOverDatabaseUrl() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("SPRING_DATASOURCE_URL", "jdbc:mysql://winner:3306/db");
        environment.setProperty("DATABASE_URL", "mysql://loser:3306/db");

        assertThat(normalizedDatasourceUrl(environment))
            .isEqualTo("jdbc:mysql://winner:3306/db");
    }

    @Test
    void doesNotOverrideUrlWhenNeitherVariableIsSet() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url", "jdbc:mysql://default:3306/db");

        assertThat(normalizedDatasourceUrl(environment))
            .isEqualTo("jdbc:mysql://default:3306/db");
    }

    @Test
    void keepsRedisAliasesBehavior() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("SPRING_DATA_REDIS_HOST", "upstash.example.com");
        environment.setProperty("REDIS_PORT", "6380");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.data.redis.host"))
            .isEqualTo("upstash.example.com");
        assertThat(environment.getProperty("spring.data.redis.port"))
            .isEqualTo("6380");
    }
}
