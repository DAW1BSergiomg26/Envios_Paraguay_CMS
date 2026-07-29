package com.monteastur.envios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI enviosOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Monteastur Envíos API")
                .description("API REST de la plataforma logística premium España ⇢ Paraguay")
                .version("3.2")
                .contact(new Contact()
                    .name("Monteastur Envíos")
                    .email("admin@casarrural.com")))
            .components(new Components()
                .addSecuritySchemes("basicAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("basic")
                    .description("Credenciales de administrador (usuario:contraseña)"))
                .addSecuritySchemes("cookieAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .name("JSESSIONID")
                    .description("Sesión de cliente autenticado vía portal /cliente/login")))
            .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
            .addSecurityItem(new SecurityRequirement().addList("cookieAuth"));
    }
}
