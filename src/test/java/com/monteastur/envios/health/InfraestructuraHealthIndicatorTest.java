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
}
