package com.monteastur.envios.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

public class CacheAuditErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(CacheAuditErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache GET fallido (cache={}, key={}): se degrada a base de datos", cache.getName(), key, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache PUT fallido (cache={}, key={}): se omite el almacenamiento", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache EVICT fallido (cache={}, key={}): se continúa", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache CLEAR fallido (cache={}): se continúa", cache.getName(), exception);
    }
}
