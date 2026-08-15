package com.monteastur.envios.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone las instancias de Retry y CircuitBreaker de nombre "webhook"
 * gestionadas por los registries de Resilience4j (que leen la
 * configuración de application.properties). Las métricas del
 * CircuitBreaker se publican en Prometheus vía resilience4j-micrometer y
 * su estado se expone en /actuator/health vía register-health-indicator.
 */
@Configuration
public class ResilienceConfig {

    public static final String WEBHOOK_INSTANCE = "webhook";

    @Bean
    public Retry webhookRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry(WEBHOOK_INSTANCE);
    }

    @Bean
    public CircuitBreaker webhookCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker(WEBHOOK_INSTANCE);
    }
}
