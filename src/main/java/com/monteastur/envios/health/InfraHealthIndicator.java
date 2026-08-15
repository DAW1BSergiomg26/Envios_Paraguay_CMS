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
import java.sql.Statement;

/**
 * HealthIndicator que verifica conectividad real de MySQL (SELECT 1) y
 * Redis (PING) midiendo la latencia de cada dependencia. Se registra
 * automáticamente como /actuator/health/infra y complementa a los
 * indicadores genéricos de Spring Boot (db, redis, ping, diskSpace).
 */
@Component
public class InfraHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(InfraHealthIndicator.class);

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public InfraHealthIndicator(DataSource dataSource,
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
            try (Connection conexion = dataSource.getConnection();
                 Statement stmt = conexion.createStatement()) {
                stmt.execute("SELECT 1");
            }
            databaseLatencyMs = latenciaMs(inicio);
        } catch (Exception e) {
            up = false;
            database = mensajeError(e);
            log.error("HealthCheck: MySQL no responde", e);
        }

        String redis = "up";
        double redisLatencyMs = 0;
        RedisConnection conexionRedis = null;
        try {
            long inicio = System.nanoTime();
            conexionRedis = redisConnectionFactory.getConnection();
            conexionRedis.ping();
            redisLatencyMs = latenciaMs(inicio);
        } catch (Exception e) {
            up = false;
            redis = mensajeError(e);
            log.error("HealthCheck: Redis no responde", e);
        } finally {
            if (conexionRedis != null) {
                try {
                    conexionRedis.close();
                } catch (Exception e) {
                    log.warn("HealthCheck: no se pudo cerrar la conexión Redis", e);
                }
            }
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

    private String mensajeError(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
