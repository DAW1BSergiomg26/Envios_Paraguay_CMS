package com.monteastur.envios.integration;

import com.monteastur.envios.dto.api.PublicTrackingDto;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.EnvioTrackingService;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EnvioTrackingCacheIntegrationTest {

    private static final String CACHE_TRACKING = "envios.tracking";

    @Autowired
    private EnvioTrackingService envioTrackingService;

    @Autowired
    private EnvioTrackingRepository envioTrackingRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private LettuceConnectionFactory lettuceConnectionFactory;

    private Long envioId;
    private Long envioExtraId;

    @AfterEach
    void limpiar() {
        if (envioExtraId != null) {
            envioTrackingRepository.deleteById(envioExtraId);
        }
        if (envioId != null) {
            envioTrackingRepository.deleteById(envioId);
        }
        var cache = cacheManager.getCache(CACHE_TRACKING);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void segundaConsultaSeSirveDesdeCache() {
        String codigo = "PY-CACHE-" + System.nanoTime();
        EnvioTracking envio = guardarEnvio(codigo);
        envioId = envio.getId();

        PublicTrackingDto primera = envioTrackingService.buscarPorCodigo(codigo);
        assertThat(primera).isNotNull();
        assertThat(primera.getCodigoUnico()).isEqualTo(codigo);

        envioTrackingRepository.deleteById(envioId);
        envioId = null;

        PublicTrackingDto segunda = envioTrackingService.buscarPorCodigo(codigo);
        assertThat(segunda).isNotNull();
        assertThat(segunda.getEstado()).isEqualTo(primera.getEstado());
    }

    @Test
    void guardarEvictaLaEntradaDeTracking() {
        String codigo = "PY-CACHE-" + System.nanoTime();
        EnvioTracking envio = guardarEnvio(codigo);
        envioId = envio.getId();

        assertThat(envioTrackingService.buscarPorCodigo(codigo)).isNotNull();
        envioTrackingRepository.deleteById(envioId);
        envioId = null;

        EnvioTracking otro = new EnvioTracking("PY-CACHE-EVICT-" + System.nanoTime(), "RECIBIDO",
                "Destinatario Test", "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos");
        EnvioTracking guardado = envioTrackingService.guardar(otro);
        envioExtraId = guardado.getId();

        assertThat(envioTrackingService.buscarPorCodigo(codigo)).isNull();
    }

    @Test
    void cacheTieneTtlDeCincoMinutos() {
        String codigo = "PY-CACHE-" + System.nanoTime();
        EnvioTracking envio = guardarEnvio(codigo);
        envioId = envio.getId();

        envioTrackingService.buscarPorCodigo(codigo);

        Long ttl = stringRedisTemplate.getExpire(CACHE_TRACKING + "::" + codigo);
        assertThat(ttl).isNotNull().isBetween(1L, 300L);
    }

    @Test
    void poolLettuceActivo_conMaxTotal30() {
        assertThat(lettuceConnectionFactory.getClientConfiguration())
                .isInstanceOf(LettucePoolingClientConfiguration.class);
        GenericObjectPoolConfig config = ((LettucePoolingClientConfiguration) lettuceConnectionFactory
                .getClientConfiguration()).getPoolConfig();
        assertThat(config).isNotNull();
        assertThat(config.getMaxTotal()).isEqualTo(30);
    }

    private EnvioTracking guardarEnvio(String codigo) {
        return envioTrackingRepository.save(new EnvioTracking(codigo, "RECIBIDO",
                "Destinatario Test", "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos"));
    }
}
