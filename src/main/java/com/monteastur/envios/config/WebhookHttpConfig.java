package com.monteastur.envios.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Executor;

@Configuration
public class WebhookHttpConfig {

    @Bean
    public RestClient webhookRestClient(
            @Value("${app.webhook.connect-timeout:2000}") int connectTimeoutMs,
            @Value("${app.webhook.read-timeout:5000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Bean(name = "webhookTaskExecutor")
    public Executor webhookTaskExecutor(
            @Value("${app.webhook.executor.core-size:4}") int coreSize,
            @Value("${app.webhook.executor.max-size:8}") int maxSize,
            @Value("${app.webhook.executor.queue-capacity:100}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("webhook-");
        executor.initialize();
        return executor;
    }
}
