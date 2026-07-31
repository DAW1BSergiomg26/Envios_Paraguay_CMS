package com.monteastur.envios.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultUsersInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUsersInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DefaultUsersInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUserIfMissing(adminUsername, adminPassword, "ROLE_ADMIN");
        createUserIfMissing("operador", adminPassword, "ROLE_OPERADOR");
    }

    private void createUserIfMissing(String username, String rawPassword, String role) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO users (username, password, email, enabled) VALUES (?, ?, ?, ?)",
                username, passwordEncoder.encode(rawPassword), username, true);
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) " +
                "SELECT u.id, r.id FROM users u CROSS JOIN roles r " +
                "WHERE u.username = ? AND r.nombre = ?",
                username, role);
        log.info("Usuario por defecto creado: {} con rol {}", username, role);
    }
}
