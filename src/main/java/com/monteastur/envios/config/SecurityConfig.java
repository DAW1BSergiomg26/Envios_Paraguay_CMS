package com.monteastur.envios.config;

import com.monteastur.envios.security.ClienteSessionAuthenticationFilter;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.security.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CustomAccessDeniedHandler customAccessDeniedHandler,
                                           RBACAccessLogger rbacAccessLogger,
                                           ObjectMapper objectMapper) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/cliente/login", "/cliente/assets/**").permitAll()
                .requestMatchers("/admin/**", "/api/v1/admin/**", "/api/v1/deliveries/**").authenticated()
                .requestMatchers("/api/v1/docs", "/api/v1/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/cliente/**", "/api/v1/cliente/**").hasRole("CLIENTE")
                .anyRequest().permitAll()
            )
            .exceptionHandling(handling -> handling
                .accessDeniedHandler(customAccessDeniedHandler)
                .authenticationEntryPoint(new RestAuthenticationEntryPoint(rbacAccessLogger, objectMapper))
            )
            .addFilterBefore(new ClienteSessionAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/react-dashboard/", true)
                .permitAll()
            )
            .httpBasic(Customizer.withDefaults())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation().changeSessionId()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                    "geolocation=(), microphone=(), camera=(), payment=()"))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                    .preload(true))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data: blob:; font-src 'self'; connect-src 'self'; " +
                        "frame-ancestors 'none'; form-action 'self'"))
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            );
        return http.build();
    }

    @Bean
    public JdbcUserDetailsManager userDetailsService(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
        manager.setUsersByUsernameQuery(
            "SELECT username, password, enabled FROM users WHERE username = ?");
        manager.setAuthoritiesByUsernameQuery(
            "SELECT u.username, r.nombre AS authority " +
            "FROM users u " +
            "JOIN user_roles ur ON u.id = ur.user_id " +
            "JOIN roles r ON ur.role_id = r.id " +
            "WHERE u.username = ?");
        return manager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}