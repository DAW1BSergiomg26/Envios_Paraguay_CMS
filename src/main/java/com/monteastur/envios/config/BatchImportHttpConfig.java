package com.monteastur.envios.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class BatchImportHttpConfig {

    @Bean(name = "batchTaskExecutor")
    public Executor batchTaskExecutor(
            @Value("${app.batch.executor.core-size:2}") int coreSize,
            @Value("${app.batch.executor.max-size:4}") int maxSize,
            @Value("${app.batch.executor.queue-capacity:500}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("batch-");
        // En saturacion la carga se procesa en el hilo del llamador: nunca se pierde ni se lanza
        // TaskRejectedException.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
