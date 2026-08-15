package com.monteastur.envios.integration;

import com.monteastur.envios.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ActuatorSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcUserDetailsManager userDetailsManager;

    @MockBean
    private EmailService emailService;

    private RequestPostProcessor desdeIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private void autenticaAdmin() {
        UserDetails admin = User.withUsername("admin")
                .password(new BCryptPasswordEncoder().encode("admin123"))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
        when(userDetailsManager.loadUserByUsername("admin")).thenReturn(admin);
    }

    @Test
    void healthAbiertoSinAuth_devuelve200ConUp() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
    }

    @Test
    void infoAbiertoSinAuth_devuelve200() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/info"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void livenessYReadiness_devuelvenUp() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health/liveness"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health/readiness"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
    }

    @Test
    void healthInfraestructura_conAdmin_devuelveUpConDetalles() throws Exception {
        autenticaAdmin();

        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health/infraestructura")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.details.database").exists());
    }

    @Test
    void prometheusDesdeIpNoPermitida_devuelve401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus")
                        .with(desdeIp("203.0.113.10")))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void prometheusDesdeIpDocker_devuelve200() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus")
                        .with(desdeIp("172.18.0.5")))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void metricsDesdeIpNoPermitida_devuelve401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/metrics")
                        .with(desdeIp("203.0.113.10")))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void metricsDesdeIpDocker_devuelve200() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/metrics")
                        .with(desdeIp("172.18.0.5")))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void actuatorRestoSinAdmin_devuelve401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void actuatorRestoConAdmin_devuelve200() throws Exception {
        autenticaAdmin();

        mockMvc.perform(MockMvcRequestBuilders.get("/actuator")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
