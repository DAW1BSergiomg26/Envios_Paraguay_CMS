package com.monteastur.envios.integration;

import com.monteastur.envios.dto.analytics.AnalyticsSummaryDto;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.model.WebhookConfig;
import com.monteastur.envios.model.WebhookLog;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.WebhookConfigRepository;
import com.monteastur.envios.repository.WebhookLogRepository;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.analytics.AnalyticsDashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
class AnalyticsDashboardIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private WebhookConfigRepository webhookConfigRepository;
    @Autowired private WebhookLogRepository webhookLogRepository;
    @Autowired private EnvioTrackingRepository envioTrackingRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private AnalyticsDashboardService analyticsService;
    @Autowired private CacheManager cacheManager;

    @MockBean private EmailService emailService;

    private final List<Long> enviosIds = new ArrayList<>();
    private final List<Long> clientesIds = new ArrayList<>();
    private final List<Long> webhookIds = new ArrayList<>();

    @AfterEach
    void limpiar() {
        webhookLogRepository.deleteAll();
        for (Long id : webhookIds) {
            webhookConfigRepository.deleteById(id);
        }
        webhookIds.clear();
        for (Long id : enviosIds) {
            envioTrackingRepository.deleteById(id);
        }
        enviosIds.clear();
        for (Long id : clientesIds) {
            clienteRepository.deleteById(id);
        }
        clientesIds.clear();
        reservaRepository.deleteAll();
        Cache c = cacheManager.getCache("envios.analytics");
        if (c != null) {
            c.clear();
        }
    }

    private Cliente crearCliente(String email) {
        Cliente c = clienteRepository.save(
                new Cliente(email, "hash", "Cliente " + email, "000000000"));
        clientesIds.add(c.getId());
        return c;
    }

    private EnvioTracking guardarEnvio(String codigo, String estado, String origen, String destino) {
        EnvioTracking e = envioTrackingRepository.save(
                new EnvioTracking(codigo, estado, "Destinatario " + codigo, origen, destino, "1 kg", "Docs"));
        enviosIds.add(e.getId());
        return e;
    }

    private void crearWebhookLogs(Cliente cliente, EnvioTracking envio, int exitosos, int fallidos) {
        WebhookConfig config = webhookConfigRepository.save(
                new WebhookConfig(cliente.getId(), "https://hook.test/evt", "secret"));
        webhookIds.add(config.getId());
        for (int i = 0; i < exitosos; i++) {
            webhookLogRepository.save(new WebhookLog(config.getId(), envio.getId(), "{}", 200, true, null));
        }
        for (int i = 0; i < fallidos; i++) {
            webhookLogRepository.save(new WebhookLog(config.getId(), envio.getId(), "{}", 500, false, "HTTP 500"));
        }
    }

    private Reserva crearReserva(String estado) {
        Reserva r = new Reserva("Cliente R", "r@test.local", "123",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 2, "comentario");
        r.setEstado(estado);
        r.setCreatedAt(LocalDateTime.now());
        return reservaRepository.save(r);
    }

    @Test
    void resumen_agregaDatosCorrectos() {
        guardarEnvio("MT-BI-01", "ENTREGADO", "Asturias", "Asunción");
        guardarEnvio("MT-BI-02", "ENTREGADO", "Asturias", "Asunción");
        guardarEnvio("MT-BI-03", "EN_TRANSITO", "Asturias", "Asunción");
        Cliente cliente = crearCliente("bi@test.local");
        guardarEnvio("MT-BI-04", "ENTREGADO", "Oviedo", "Ciudad del Este");
        crearWebhookLogs(cliente, envioTrackingRepository.findByCodigoUnico("MT-BI-04").orElseThrow(), 6, 2);
        crearReserva("pendiente");

        AnalyticsSummaryDto resumen = analyticsService.resumen();

        assertThat(resumen.getKpis()).hasSize(5);
        assertThat(resumen.getKpis().get(0).getValue()).isEqualTo(4.0);
        assertThat(resumen.getKpis().get(1).getValue()).isEqualTo(1.0);
        assertThat(resumen.getKpis().get(2).getValue()).isEqualTo(3.0);
        assertThat(resumen.getKpis().get(3).getValue()).isEqualTo(75.0);
        assertThat(resumen.getKpis().get(4).getValue()).isEqualTo(1.0);

        assertThat(resumen.getEnviosPorEstado())
                .filteredOn(d -> "ENTREGADO".equals(d.getEstado()))
                .singleElement()
                .extracting("cantidad")
                .isEqualTo(3L);

        assertThat(resumen.getTopRutas()).isNotEmpty();
        assertThat(resumen.getTopRutas().get(0).getOrigen()).isEqualTo("Asturias");
        assertThat(resumen.getTopRutas().get(0).getDestino()).isEqualTo("Asunción");
        assertThat(resumen.getTopRutas().get(0).getCantidad()).isEqualTo(3L);

        assertThat(resumen.getTendencia()).hasSize(14);

        assertThat(resumen.getWebhookPorDia()).hasSize(14);
        assertThat(resumen.getWebhookPorDia().get(13).getExitosos()).isEqualTo(6L);
        assertThat(resumen.getWebhookPorDia().get(13).getTotal()).isEqualTo(8L);
        assertThat(resumen.getWebhookPorDia().get(13).getTasaExito()).isEqualTo(75.0);
    }

    @Test
    void resumen_quedaCacheadoYRefreshLoInvalida() {
        AnalyticsSummaryDto primera = analyticsService.resumen();
        long totalInicial = (long) primera.getKpis().get(0).getValue();

        guardarEnvio("MT-CACHE-1", "RECIBIDO", "Asturias", "Asunción");

        AnalyticsSummaryDto segunda = analyticsService.resumen();
        assertThat((long) segunda.getKpis().get(0).getValue()).isEqualTo(totalInicial);

        analyticsService.refrescar();
        AnalyticsSummaryDto tercera = analyticsService.resumen();
        assertThat((long) tercera.getKpis().get(0).getValue()).isEqualTo(totalInicial + 1);
    }

    @Test
    void resumenApi_devuelveJsonConCampos() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.length()").value(5))
                .andExpect(jsonPath("$.enviosPorEstado").isArray())
                .andExpect(jsonPath("$.tendencia.length()").value(14))
                .andExpect(jsonPath("$.generadoEn").exists());
    }

    @Test
    void refreshApi_devuelveDatosFrescos() throws Exception {
        mockMvc.perform(post("/api/v1/admin/analytics/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.length()").value(5));
    }
}