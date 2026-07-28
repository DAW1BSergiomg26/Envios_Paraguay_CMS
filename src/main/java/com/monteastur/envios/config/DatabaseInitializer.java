package com.monteastur.envios.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@ConditionalOnProperty(name = "spring.datasource.url")
public class DatabaseInitializer implements BeanFactoryPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final int MAX_RETRIES = 5;
    private static final long BASE_DELAY_MS = 5000;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || url.isBlank()) {
            log.info("[DB-Init] No SPRING_DATASOURCE_URL configured - skipping auto-creation");
            return;
        }

        String dbName = extractDatabaseName(url);
        String baseUrl = buildBaseUrl(url);

        if (dbName == null || dbName.isBlank()) {
            log.warn("[DB-Init] Could not extract database name from URL - skipping");
            return;
        }

        log.info("[DB-Init] Target database: '{}' - ensuring it exists...", dbName);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS " + dbName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                );
                log.info("[DB-Init] Database '{}' confirmed (attempt {}/{})", dbName, attempt, MAX_RETRIES);
                return;
            } catch (SQLException e) {
                log.warn("[DB-Init] Attempt {}/{} failed: [{}] {}", attempt, MAX_RETRIES, e.getErrorCode(), e.getMessage());
                if (attempt < MAX_RETRIES) {
                    long delay = BASE_DELAY_MS * attempt;
                    log.info("[DB-Init] Retrying in {} ms...", delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
        log.error("[DB-Init] Could not create '{}' after {} attempts", dbName, MAX_RETRIES);
    }

    private String extractDatabaseName(String jdbcUrl) {
        try {
            String without = jdbcUrl.substring("jdbc:mysql://".length());
            int slash = without.indexOf('/');
            int question = without.indexOf('?');
            if (slash < 0) return null;
            int end = question > slash ? question : without.length();
            return without.substring(slash + 1, end);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildBaseUrl(String jdbcUrl) {
        try {
            String without = jdbcUrl.substring("jdbc:mysql://".length());
            int slash = without.indexOf('/');
            String host = without.substring(0, slash);
            String rest = without.substring(slash + 1);
            int question = rest.indexOf('?');
            String params = question >= 0 ? rest.substring(question) : "";
            return "jdbc:mysql://" + host + "/?" + params;
        } catch (Exception e) {
            return jdbcUrl;
        }
    }
}
