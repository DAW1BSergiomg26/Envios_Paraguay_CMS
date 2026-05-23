package com.grupb2.casarural.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Configuraci�n de seguridad para la arquitectura h�brida MVC + SPA.
 *
 * La aplicaci�n convive con dos frontends:
 * - Thymeleaf (MVC tradicional): formularios con protecci�n CSRF activa
 * - React SPA (dashboard moderno): sesi�n mediante cookie JSESSIONID
 *
 * Estrategia CSRF:
 * - Habilitado para Thymeleaf (protege formularios HTML contra ataques CSRF)
 * - Deshabilitado para /api/** (sesi�n autenticada v�a HttpOnly cookie;
 *   el navegador env�a JSESSIONID autom�ticamente en cada petici�n,
 *   incluyendo PUT/POST desde el SPA. No hay formulario HTML que pueda
 *   ser explotado, y el SPA no puede leer la cookie JSESSIONID)
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html">Spring Security CSRF</a>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Rutas protegidas: admin MVC y API REST admin requieren autenticaci�n
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**", "/api/v1/admin/**").authenticated()
                .anyRequest().permitAll()
            )
            // Login basado en formulario Spring Security (Thymeleaf)
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/admin/dashboard")
                .permitAll()
            )
            // Logout con limpieza de sesi�n y cookie
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Protecci�n contra fijaci�n de sesi�n
            .sessionManagement(session -> session
                .sessionFixation().changeSessionId()
            )
            // Headers de seguridad
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )
            // CSRF: deshabilitado solo para API REST (ver JavaDoc de la clase)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        var user = User.builder()
                .username(adminUsername)
                .password(passwordEncoder().encode(adminPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
