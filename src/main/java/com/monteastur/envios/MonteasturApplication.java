package com.monteastur.envios;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class MonteasturApplication {

    private static final Logger log = LoggerFactory.getLogger(MonteasturApplication.class);

    private final Environment env;

    public MonteasturApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(MonteasturApplication.class, args);
    }

    @PostConstruct
    public void validateEnvironment() {
        String[] required = {"DB_USERNAME", "DB_PASSWORD"};
        boolean isProd = "prod".equals(env.getProperty("spring.profiles.active"));
        if (isProd) {
            for (String key : required) {
                String val = env.getProperty(key);
                if (val == null || val.isBlank()) {
                    log.error("Variable de entorno obligatoria '{}' no está definida. " +
                            "La aplicación se detendrá.", key);
                    throw new IllegalStateException(
                            "Variable de entorno obligatoria '" + key + "' no definida");
                }
            }
            log.info("Validación de entorno superada — todas las variables requeridas están presentes");
        } else {
            log.info("Perfil activo: {}. Saltando validación de entorno en desarrollo.",
                    env.getProperty("spring.profiles.active", "default"));
        }
    }
}
