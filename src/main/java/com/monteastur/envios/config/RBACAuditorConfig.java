package com.monteastur.envios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class RBACAuditorConfig {

    @Bean
    public RBACAccessLogger rbacAccessLogger(JdbcTemplate jdbcTemplate) {
        return new RBACAccessLogger(jdbcTemplate);
    }
}
