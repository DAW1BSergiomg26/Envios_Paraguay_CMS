package com.monteastur.envios.service.web;

import com.monteastur.envios.dto.web.PublicTrackingView;
import com.monteastur.envios.metrics.BusinessMetrics;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PublicTrackingServiceTest.TestConfig.class)
class PublicTrackingServiceTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        EnvioTrackingRepository envioTrackingRepository() {
            return Mockito.mock(EnvioTrackingRepository.class);
        }

        @Bean
        EventoTrackingService eventoTrackingService() {
            return Mockito.mock(EventoTrackingService.class);
        }

        @Bean
        EvidenciaEnvioService evidenciaEnvioService() {
            return Mockito.mock(EvidenciaEnvioService.class);
        }

        @Bean
        EntregaEvidenciaRepository entregaEvidenciaRepository() {
            return Mockito.mock(EntregaEvidenciaRepository.class);
        }

        @Bean
        BusinessMetrics businessMetrics() {
            return Mockito.mock(BusinessMetrics.class);
        }

        @Bean
        PublicTrackingService publicTrackingService(EnvioTrackingRepository repo,
                                                    EventoTrackingService eventos,
                                                    EvidenciaEnvioService evidencias,
                                                    EntregaEvidenciaRepository entregas,
                                                    BusinessMetrics businessMetrics) {
            return new PublicTrackingService(repo, eventos, evidencias, entregas, businessMetrics);
        }

        @Bean
        CacheManager cacheManager() {
            ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("envios.tracking.pagina");
            cacheManager.setAllowNullValues(false);
            return cacheManager;
        }
    }

    @Autowired
    private PublicTrackingService service;

    @Autowired
    private EnvioTrackingRepository envioTrackingRepository;

    @Autowired
    private EventoTrackingService eventoTrackingService;

    @Autowired
    private EvidenciaEnvioService evidenciaEnvioService;

    @Autowired
    private EntregaEvidenciaRepository entregaEvidenciaRepository;

    private EnvioTracking envio(String codigo, String estado) {
        EnvioTracking envio = new EnvioTracking(codigo, estado, "Destinatario Test",
                "Asturias, España", "Asunción, Paraguay", "10 kg", "Documentos");
        envio.setId(1L);
        envio.setCliente(new Cliente("cliente@test.com", "x", "Cliente Test", null));
        return envio;
    }

    @Test
    void codigoInexistente_retornaNull() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico(anyString()))
                .thenReturn(Optional.empty());

        assertThat(service.cargarPagina("NOPE")).isNull();
    }

    @Test
    void entregaViewSoloCuandoEntregado() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1"))
                .thenReturn(Optional.of(envio("MT-1", "ENTREGADO")));
        when(eventoTrackingService.listarPorEnvio(1L)).thenReturn(List.of());
        when(evidenciaEnvioService.listarPorEnvioParaCliente(1L)).thenReturn(List.of());
        when(entregaEvidenciaRepository.findByEnvioId(1L))
                .thenReturn(Optional.of(new EntregaEvidencia(envio("MT-1", "ENTREGADO"),
                        "Ana López", "12345678", "firma", null, null, null)));

        PublicTrackingView view = service.cargarPagina("MT-1");

        assertThat(view).isNotNull();
        assertThat(view.getPasoActual()).isEqualTo(5);
        assertThat(view.getEntrega()).isNotNull();
        assertThat(view.getEntrega().getReceptorNombre()).isEqualTo("Ana López");
        assertThat(view.getClienteNombre()).isEqualTo("Cliente Test");
    }

    @Test
    void pasoActualMenosUnoSiEstadoNoCanonico() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-9"))
                .thenReturn(Optional.of(envio("MT-9", "CANCELADO")));
        when(eventoTrackingService.listarPorEnvio(1L)).thenReturn(List.of());
        when(evidenciaEnvioService.listarPorEnvioParaCliente(1L)).thenReturn(List.of());

        PublicTrackingView view = service.cargarPagina("MT-9");

        assertThat(view.getPasoActual()).isEqualTo(-1);
        assertThat(view.getEntrega()).isNull();
    }

    @Test
    void segundaConsultaSeSirveDesdeCache() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-CACHE"))
                .thenReturn(Optional.of(envio("MT-CACHE", "RECIBIDO")));
        when(eventoTrackingService.listarPorEnvio(1L)).thenReturn(List.of());
        when(evidenciaEnvioService.listarPorEnvioParaCliente(1L)).thenReturn(List.of());

        service.cargarPagina("MT-CACHE");
        service.cargarPagina("MT-CACHE");

        verify(envioTrackingRepository, times(1)).findWithClienteByCodigoUnico("MT-CACHE");
    }
}
