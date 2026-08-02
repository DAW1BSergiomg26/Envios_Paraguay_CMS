package com.monteastur.envios.integration;

import com.monteastur.envios.dto.web.PublicTrackingView;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.EventoTracking;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.DocumentoGeneradoRepository;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EvidenciaEnvioRepository;
import com.monteastur.envios.repository.EventoTrackingRepository;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.web.PublicTrackingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class PortalTrackingDashboardIntegrationTest {

    private static final String PNG_1X1 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @Autowired private MockMvc mockMvc;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private EnvioTrackingRepository envioTrackingRepository;
    @Autowired private EventoTrackingRepository eventoTrackingRepository;
    @Autowired private EvidenciaEnvioRepository evidenciaEnvioRepository;
    @Autowired private EntregaEvidenciaRepository entregaEvidenciaRepository;
    @Autowired private DocumentoGeneradoRepository documentoGeneradoRepository;
    @Autowired private CacheManager cacheManager;
    @Autowired private StringRedisTemplate stringRedisTemplate;
    @Autowired private PublicTrackingService publicTrackingService;

    @MockBean private EmailService emailService;

    private Long clienteId;
    private Long envioId;
    private String codigoActual;

    @AfterEach
    void limpiar() {
        if (codigoActual != null) {
            documentoGeneradoRepository.deleteAll(documentoGeneradoRepository.findAll().stream()
                    .filter(d -> codigoActual.equals(d.getReferenciaId()))
                    .toList());
        }
        if (envioId != null) {
            entregaEvidenciaRepository.findByEnvioId(envioId).ifPresent(entregaEvidenciaRepository::delete);
            eventoTrackingRepository.deleteAll(eventoTrackingRepository.findByEnvioTrackingIdOrderByFechaEventoDesc(envioId));
            evidenciaEnvioRepository.deleteAll(evidenciaEnvioRepository.findByEnvioTrackingIdOrderByFechaSubidaDesc(envioId));
            envioTrackingRepository.deleteById(envioId);
        }
        if (clienteId != null) {
            clienteRepository.deleteById(clienteId);
        }
        for (String cache : List.of("envios.tracking.pagina", "envios.cliente.dashboard")) {
            Cache c = cacheManager.getCache(cache);
            if (c != null) {
                c.clear();
            }
        }
        clienteId = null;
        envioId = null;
        codigoActual = null;
    }

    private void seedClienteYEnvio(String codigo, String estado) {
        Cliente cliente = clienteRepository.save(
                new Cliente("portal-" + System.nanoTime() + "@test.com", "pass", "Cliente Portal", null));
        clienteId = cliente.getId();
        EnvioTracking envio = new EnvioTracking(codigo, estado, "Destinatario Portal",
                "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos");
        envio.setCliente(cliente);
        EnvioTracking guardado = envioTrackingRepository.save(envio);
        envioId = guardado.getId();
        codigoActual = codigo;

        EventoTracking evento = new EventoTracking();
        evento.setEnvioTracking(guardado);
        evento.setEstado(estado);
        evento.setTitulo("Envío registrado en MONTEASTUR");
        evento.setUbicacion("Asturias, España");
        evento.setIcono("📦");
        evento.setColor("#d4762a");
        evento.setFechaEvento(LocalDateTime.now());
        evento.setCreadoPor("admin");
        evento.setVisibleCliente(true);
        eventoTrackingRepository.save(evento);
    }

    @Test
    void paginaTracking_retorna200ConTimeline() throws Exception {
        String codigo = "PY-PORTAL-" + System.nanoTime();
        seedClienteYEnvio(codigo, "RECIBIDO");

        mockMvc.perform(get("/tracking/" + codigo))
                .andExpect(status().isOk())
                .andExpect(view().name("tracking-result"))
                .andExpect(model().attributeExists("view"));
    }

    @Test
    void paginaTracking_entregado_muestraPOD() throws Exception {
        String codigo = "PY-POD-" + System.nanoTime();
        seedClienteYEnvio(codigo, "ENTREGADO");
        EnvioTracking envio = envioTrackingRepository.findById(envioId).orElseThrow();
        entregaEvidenciaRepository.save(new EntregaEvidencia(envio, "Receptor", "1234", PNG_1X1,
                new BigDecimal("-25.2637421"), new BigDecimal("-57.575926"), null));

        mockMvc.perform(get("/tracking/" + codigo))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("view"));

        PublicTrackingView view = publicTrackingService.cargarPagina(codigo);
        assertThat(view.getPasoActual()).isEqualTo(5);
        assertThat(view.getEntrega()).isNotNull();
        assertThat(view.getEntrega().getReceptorNombre()).isEqualTo("Receptor");
    }

    @Test
    void paginaTracking_inexistente_retorna404() throws Exception {
        mockMvc.perform(get("/tracking/PY-NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("tracking-404"));
    }

    @Test
    void panel_cliente_retornaDashboard() throws Exception {
        String codigo = "PY-DASH-" + System.nanoTime();
        seedClienteYEnvio(codigo, "RECIBIDO");

        mockMvc.perform(get("/cliente/panel").sessionAttr("clienteId", clienteId))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/panel"))
                .andExpect(model().attributeExists("panel"));
    }

    @Test
    void etiqueta_propio_retornaPdf() throws Exception {
        String codigo = "PY-PDF-" + System.nanoTime();
        seedClienteYEnvio(codigo, "RECIBIDO");

        mockMvc.perform(get("/cliente/panel/envio/" + codigo + "/etiqueta").sessionAttr("clienteId", clienteId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void etiqueta_ajeno_retorna403() throws Exception {
        String codigo = "PY-AJENO-" + System.nanoTime();
        seedClienteYEnvio(codigo, "RECIBIDO");
        Long otroClienteId = clienteRepository.save(
                new Cliente("ajeno-" + System.nanoTime() + "@test.com", "pass", "Otro", null)).getId();

        mockMvc.perform(get("/cliente/panel/envio/" + codigo + "/etiqueta").sessionAttr("clienteId", otroClienteId))
                .andExpect(status().isForbidden());

        clienteRepository.deleteById(otroClienteId);
    }

    @Test
    void etiqueta_sinSesion_redirigeLogin() throws Exception {
        mockMvc.perform(get("/cliente/panel/envio/PY-NOPE/etiqueta"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void cachePagina_segundaConsultaNoTocaBaseDeDatos() throws Exception {
        String codigo = "PY-CACHE-" + System.nanoTime();
        seedClienteYEnvio(codigo, "RECIBIDO");

        PublicTrackingView primera = publicTrackingService.cargarPagina(codigo);
        assertThat(primera).isNotNull();

        entregaEvidenciaRepository.findByEnvioId(envioId).ifPresent(entregaEvidenciaRepository::delete);
        eventoTrackingRepository.deleteAll(eventoTrackingRepository.findByEnvioTrackingIdOrderByFechaEventoDesc(envioId));
        evidenciaEnvioRepository.deleteAll(evidenciaEnvioRepository.findByEnvioTrackingIdOrderByFechaSubidaDesc(envioId));
        envioTrackingRepository.deleteById(envioId);
        envioId = null;

        PublicTrackingView segunda = publicTrackingService.cargarPagina(codigo);
        assertThat(segunda).isNotNull();
        assertThat(segunda.getCodigoUnico()).isEqualTo(codigo);
    }

    @Test
    void cachePagina_tieneTtlDeCincoMinutos() throws Exception {
        String codigo = "PY-TTL-" + System.nanoTime();
        seedClienteYEnvio(codigo, "RECIBIDO");

        publicTrackingService.cargarPagina(codigo);

        Long ttl = stringRedisTemplate.getExpire("envios.tracking.pagina::" + codigo);
        assertThat(ttl).isNotNull().isBetween(1L, 300L);
    }
}
