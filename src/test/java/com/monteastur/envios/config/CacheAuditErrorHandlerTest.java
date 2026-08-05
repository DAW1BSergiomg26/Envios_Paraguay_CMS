package com.monteastur.envios.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import static org.assertj.core.api.Assertions.assertThatCode;

class CacheAuditErrorHandlerTest {

    private final CacheAuditErrorHandler handler = new CacheAuditErrorHandler();
    private final Cache cache = new ConcurrentMapCache("envios.analytics");

    @Test
    void getError_noPropagaYDelegaAlMetodo() {
        assertThatCode(() -> handler.handleCacheGetError(new RuntimeException("redis caído"), cache, "k"))
                .doesNotThrowAnyException();
    }

    @Test
    void putEvictClear_noPropaganExcepciones() {
        assertThatCode(() -> handler.handleCachePutError(new RuntimeException("x"), cache, "k", new Object()))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheEvictError(new RuntimeException("x"), cache, "k"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheClearError(new RuntimeException("x"), cache))
                .doesNotThrowAnyException();
    }
}
