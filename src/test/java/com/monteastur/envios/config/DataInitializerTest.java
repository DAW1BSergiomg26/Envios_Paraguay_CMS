package com.monteastur.envios.config;

import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.TextoLegal;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EventoTrackingRepository;
import com.monteastur.envios.repository.EvidenciaEnvioRepository;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.repository.MensajeContactoRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.TextoLegalRepository;
import com.monteastur.envios.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataInitializerTest {

    private final TextoLegalRepository repo = mock(TextoLegalRepository.class);
    private final EnvioTrackingRepository trackingRepo = mock(EnvioTrackingRepository.class);
    private final ClienteRepository clienteRepo = mock(ClienteRepository.class);
    private final ClienteService clienteService = mock(ClienteService.class);
    private final EventoTrackingRepository eventoRepo = mock(EventoTrackingRepository.class);
    private final EvidenciaEnvioRepository evidenciaRepo = mock(EvidenciaEnvioRepository.class);
    private final ReservaRepository reservaRepo = mock(ReservaRepository.class);
    private final MensajeContactoRepository mensajeRepo = mock(MensajeContactoRepository.class);
    private final ImagenRepository imagenRepo = mock(ImagenRepository.class);

    private final DataInitializer dataInitializer = new DataInitializer(
        repo, trackingRepo, clienteRepo, clienteService,
        eventoRepo, evidenciaRepo, reservaRepo, mensajeRepo, imagenRepo);

    @Test
    void run_generaAvisoLegalConEmailDelNuevoDominio() {
        configurarRepositoriosVacios();

        dataInitializer.run();

        TextoLegal avisoLegal = capturarTextoLegalGuardado("aviso-legal");
        assertThat(avisoLegal).isNotNull();
        assertThat(avisoLegal.getContenido()).contains("info@monteastur.com");
    }

    @Test
    void run_noGeneraAvisoLegalConEmailHeredadoDeCasaRural() {
        configurarRepositoriosVacios();

        dataInitializer.run();

        TextoLegal avisoLegal = capturarTextoLegalGuardado("aviso-legal");
        assertThat(avisoLegal).isNotNull();
        assertThat(avisoLegal.getContenido()).doesNotContain("casarrural");
    }

    private void configurarRepositoriosVacios() {
        when(clienteRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(clienteService.guardar(any(Cliente.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingRepo.findByCodigoUnico(anyString())).thenReturn(Optional.empty());
        when(trackingRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenciaRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repo.findBySlug(anyString())).thenReturn(Optional.empty());
        when(mensajeRepo.count()).thenReturn(0L);
        when(reservaRepo.count()).thenReturn(0L);
        when(imagenRepo.count()).thenReturn(0L);
    }

    private TextoLegal capturarTextoLegalGuardado(String slug) {
        ArgumentCaptor<TextoLegal> captor = ArgumentCaptor.forClass(TextoLegal.class);
        verify(repo, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
            .filter(texto -> slug.equals(texto.getSlug()))
            .findFirst()
            .orElse(null);
    }
}
