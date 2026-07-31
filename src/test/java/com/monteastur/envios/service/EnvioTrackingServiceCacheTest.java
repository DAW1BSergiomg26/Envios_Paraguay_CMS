package com.monteastur.envios.service;

import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EnvioTrackingServiceCacheTest.TestConfig.class)
class EnvioTrackingServiceCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        EnvioTrackingRepository envioTrackingRepository() {
            return Mockito.mock(EnvioTrackingRepository.class);
        }

        @Bean
        ApplicationEventPublisher applicationEventPublisher() {
            return Mockito.mock(ApplicationEventPublisher.class);
        }

        @Bean
        EnvioTrackingService envioTrackingService(EnvioTrackingRepository repo, ApplicationEventPublisher publisher) {
            return new EnvioTrackingService(repo, publisher);
        }

        @Bean
        CacheManager cacheManager() {
            ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("envios.tracking");
            cacheManager.setAllowNullValues(false);
            return cacheManager;
        }
    }

    @Autowired
    private EnvioTrackingService service;

    @Autowired
    private EnvioTrackingRepository repo;

    @Test
    void codigoInexistente_retornaNull_sinRomperLaCache() {
        when(repo.findByCodigoUnico(anyString())).thenReturn(Optional.empty());

        assertNull(service.buscarPorCodigo("NO-EXISTE"));
    }
}
