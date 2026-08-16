package com.monteastur.envios.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ExtendWith(MockitoExtension.class)
class InfraestructuraHealthIndicatorTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    private InfraestructuraHealthIndicator indicador;

    @BeforeEach
    void setUp() {
        indicador = new InfraestructuraHealthIndicator(dataSource, redisConnectionFactory);
    }

    @Test
    void mysqlYRedisOk_devuelveUpConLatencias() throws Exception {
        Connection conexion = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.createStatement()).thenReturn(stmt);
        when(stmt.execute("SELECT 1")).thenReturn(true);
        RedisConnection redis = mock(RedisConnection.class);
        when(redisConnectionFactory.getConnection()).thenReturn(redis);
        when(redis.ping()).thenReturn("PONG");

        Health salud = indicador.health();

        assertThat(salud.getStatus()).isEqualTo(Status.UP);
        assertThat(salud.getDetails()).containsKey("database");
        assertThat(salud.getDetails()).containsKey("database_latency_ms");
        assertThat(salud.getDetails()).containsKey("redis");
        assertThat((Double) salud.getDetails().get("database_latency_ms")).isGreaterThanOrEqualTo(0);
        assertThat((Double) salud.getDetails().get("redis_latency_ms")).isGreaterThanOrEqualTo(0);
    }

    @Test
    void select1Falla_devuelveDownConDetalleDeError() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));

        Health salud = indicador.health();

        assertThat(salud.getStatus()).isEqualTo(Status.DOWN);
        assertThat((String) salud.getDetails().get("database")).contains("connection refused");
    }

    @Test
    void pingRedisFalla_devuelveDownConDetalleDeError() throws Exception {
        Connection conexion = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.createStatement()).thenReturn(stmt);
        when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("redis down"));

        Health salud = indicador.health();

        assertThat(salud.getStatus()).isEqualTo(Status.DOWN);
        assertThat((String) salud.getDetails().get("redis")).contains("redis down");
    }

    @Test
    void mysqlTimeout_devuelveDownSinBloquearIndefinidamente() throws Exception {
        Connection conexion = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.createStatement()).thenReturn(stmt);
        // Simular que execute() bloquea indefinidamente - en test real usaríamos un Future con timeout
        when(stmt.execute("SELECT 1")).thenAnswer(invocation -> {
            Thread.sleep(5000); // Simula query lenta (5s)
            return true;
        });
        RedisConnection redis = mock(RedisConnection.class);
        when(redisConnectionFactory.getConnection()).thenReturn(redis);
        when(redis.ping()).thenReturn("PONG");

        Health salud = indicador.health();

        // Con timeout de 2s, debería devolver DOWN (no UP) y no bloquear 5s
        assertThat(salud.getStatus()).isEqualTo(Status.DOWN);
        assertThat((String) salud.getDetails().get("database")).containsIgnoringCase("timeout");
    }

    @Test
    void redisTimeout_devuelveDownSinBloquearIndefinidamente() throws Exception {
        Connection conexion = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.createStatement()).thenReturn(stmt);
        when(stmt.execute("SELECT 1")).thenReturn(true);
        RedisConnection redis = mock(RedisConnection.class);
        when(redisConnectionFactory.getConnection()).thenReturn(redis);
        when(redis.ping()).thenAnswer(invocation -> {
            Thread.sleep(5000); // Simula ping lento (5s)
            return "PONG";
        });

        Health salud = indicador.health();

        assertThat(salud.getStatus()).isEqualTo(Status.DOWN);
        assertThat((String) salud.getDetails().get("redis")).containsIgnoringCase("timeout");
    }

    @Test
    void mensajeError_sanitizado_noFiltraDetallesInternos() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("jdbc:mysql://localhost:3306/db?user=root&password=secret"));

        Health salud = indicador.health();

        assertThat(salud.getStatus()).isEqualTo(Status.DOWN);
        String detalle = (String) salud.getDetails().get("database");
        assertThat(detalle).doesNotContain("password");
        assertThat(detalle).doesNotContain("jdbc:mysql");
        assertThat(detalle).doesNotContain("localhost");
    }
}
