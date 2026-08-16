package com.monteastur.envios.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * HealthIndicator que verifica conectividad real de MySQL (SELECT 1) y
 * Redis (PING) midiendo la latencia de cada dependencia. Se registra
 * automáticamente como /actuator/health/infraestructura y complementa a
 * los indicadores genéricos de Spring Boot (db, redis, ping, diskSpace).
 *
 * Incluye timeout acotado (2s) para evitar bloqueos indefinidos y
 * sanitización de mensajes de error para no filtrar credenciales.
 */
@Component
public class InfraestructuraHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(InfraestructuraHealthIndicator.class);

    private static final long TIMEOUT_CHEQUEO_MS = 2000;
    private static final int NETWORK_TIMEOUT_MS = 2000;

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public InfraestructuraHealthIndicator(DataSource dataSource,
                                          RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        boolean up = true;

        String database = "up";
        double databaseLatencyMs = 0;
        try {
            long inicio = System.nanoTime();
            CompletableFuture<Boolean> mysqlFuture = CompletableFuture.supplyAsync(() -> {
                try (Connection conexion = dataSource.getConnection()) {
                    conexion.setNetworkTimeout(java.util.concurrent.Executors.newSingleThreadExecutor(), NETWORK_TIMEOUT_MS);
                    try (Statement stmt = conexion.createStatement()) {
                        stmt.execute("SELECT 1");
                    }
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            mysqlFuture.get(TIMEOUT_CHEQUEO_MS, TimeUnit.MILLISECONDS);
            databaseLatencyMs = latenciaMs(inicio);
        } catch (TimeoutException e) {
            up = false;
            database = "timeout";
            log.warn("HealthCheck: MySQL timeout tras {}ms", TIMEOUT_CHEQUEO_MS);
        } catch (ExecutionException e) {
            up = false;
            database = mensajeErrorSanitizado(e.getCause());
            log.error("HealthCheck: MySQL no responde", e.getCause());
        } catch (Exception e) {
            up = false;
            database = mensajeErrorSanitizado(e);
            log.error("HealthCheck: MySQL no responde", e);
        }

        String redis = "up";
        double redisLatencyMs = 0;
        try {
            long inicio = System.nanoTime();
            CompletableFuture<String> redisFuture = CompletableFuture.supplyAsync(() -> {
                RedisConnection conexionRedis = null;
                try {
                    conexionRedis = redisConnectionFactory.getConnection();
                    return conexionRedis.ping();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    if (conexionRedis != null) {
                        try {
                            conexionRedis.close();
                        } catch (Exception ex) {
                            log.warn("HealthCheck: no se pudo cerrar la conexión Redis", ex);
                        }
                    }
                }
            });
            redisFuture.get(TIMEOUT_CHEQUEO_MS, TimeUnit.MILLISECONDS);
            redisLatencyMs = latenciaMs(inicio);
        } catch (TimeoutException e) {
            up = false;
            redis = "timeout";
            log.warn("HealthCheck: Redis timeout tras {}ms", TIMEOUT_CHEQUEO_MS);
        } catch (ExecutionException e) {
            up = false;
            redis = mensajeErrorSanitizado(e.getCause());
            log.error("HealthCheck: Redis no responde", e.getCause());
        } catch (Exception e) {
            up = false;
            redis = mensajeErrorSanitizado(e);
            log.error("HealthCheck: Redis no responde", e);
        }

        builder.withDetail("database", database)
                .withDetail("database_latency_ms", databaseLatencyMs)
                .withDetail("redis", redis)
                .withDetail("redis_latency_ms", redisLatencyMs);
        return up ? builder.up().build() : builder.down().build();
    }

    private double latenciaMs(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000.0;
    }

    private String mensajeErrorSanitizado(Throwable e) {
        if (e == null) {
            return "unknown_error";
        }
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return e.getClass().getSimpleName();
        }
        // Sanitizar: eliminar passwords, URLs JDBC, hosts, IPs, tokens
        String sanitized = msg
                .replaceAll("(?i)password\\s*=\\s*[^\\s&;]+", "password=***")
                .replaceAll("(?i)secret\\s*=\\s*[^\\s&;]+", "secret=***")
                .replaceAll("(?i)token\\s*=\\s*[^\\s&;]+", "token=***")
                .replaceAll("jdbc:[^\\s]+", "jdbc:***")
                .replaceAll("\\b\\d{1,3}(?:\\.\\d{1,3}){3}\\b", "***.***.***.***")
                .replaceAll("\\b[0-9a-fA-F:]{2,}:[0-9a-fA-F:]+\\b", "***:***");
        return sanitized.isBlank() ? e.getClass().getSimpleName() : sanitized;
    }
}