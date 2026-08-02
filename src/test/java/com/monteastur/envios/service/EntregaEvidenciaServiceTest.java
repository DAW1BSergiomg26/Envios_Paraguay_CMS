package com.monteastur.envios.service;

import com.monteastur.envios.dto.api.EntregaEvidenciaDto;
import com.monteastur.envios.dto.api.RegistrarEntregaRequest;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.EventoTracking;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntregaEvidenciaServiceTest {

    @Mock private EntregaEvidenciaRepository entregaRepository;
    @Mock private EnvioTrackingRepository envioTrackingRepository;
    @Mock private EnvioTrackingService envioTrackingService;
    @Mock private EventoTrackingService eventoTrackingService;

    private EntregaEvidenciaService service;

    private static final String PNG_1X1 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @BeforeEach
    void setUp() {
        service = new EntregaEvidenciaService(entregaRepository, envioTrackingRepository,
                envioTrackingService, eventoTrackingService);
    }

    private RegistrarEntregaRequest requestValido() {
        RegistrarEntregaRequest req = new RegistrarEntregaRequest();
        req.setReceptorNombre("Ana López");
        req.setReceptorDocumento("12345678");
        req.setFirmaBase64(PNG_1X1);
        req.setLatitud(-25.2637421);
        req.setLongitud(-57.575926);
        return req;
    }

    private EnvioTracking envioRecibido(Long id) {
        EnvioTracking envio = new EnvioTracking("MT-1", "RECIBIDO", "Receptor",
                "Origen", "Destino", "10 kg", "Documentos");
        envio.setId(id);
        return envio;
    }

    @Test
    void registrarEntrega_actualizaEstadoYGuarda() {
        EnvioTracking envio = envioRecibido(1L);
        EnvioTracking entregado = envioRecibido(1L);
        entregado.setEstado("ENTREGADO");
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1")).thenReturn(Optional.of(envio));
        when(entregaRepository.existsByEnvioId(1L)).thenReturn(false);
        when(entregaRepository.save(any(EntregaEvidencia.class)))
                .thenAnswer(inv -> { EntregaEvidencia e = inv.getArgument(0); e.setId(99L); return e; });
        when(envioTrackingService.actualizarEstado("MT-1", "ENTREGADO")).thenReturn(entregado);
        when(eventoTrackingService.crearEvento(entregado, "RECIBIDO"))
                .thenReturn(Optional.of(new EventoTracking()));

        EntregaEvidencia resultado = service.registrarEntrega("MT-1", requestValido());

        assertThat(resultado.getId()).isEqualTo(99L);
        assertThat(resultado.getReceptorNombre()).isEqualTo("Ana López");
        verify(envioTrackingService).actualizarEstado("MT-1", "ENTREGADO");
        verify(eventoTrackingService).crearEvento(entregado, "RECIBIDO");
        verify(entregaRepository).save(any(EntregaEvidencia.class));
    }

    @Test
    void registrarEntrega_envioInexistente_lanza404() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarEntrega("MT-NOPE", requestValido()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(entregaRepository, never()).save(any(EntregaEvidencia.class));
    }

    @Test
    void registrarEntrega_yaExistePod_lanza409() {
        EnvioTracking envio = envioRecibido(1L);
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1")).thenReturn(Optional.of(envio));
        when(entregaRepository.existsByEnvioId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.registrarEntrega("MT-1", requestValido()))
                .isInstanceOf(ConflictException.class);
        verify(entregaRepository, never()).save(any(EntregaEvidencia.class));
        verify(envioTrackingService, never()).actualizarEstado(anyString(), anyString());
    }

    @Test
    void registrarEntrega_validacionFallida_lanza400() {
        RegistrarEntregaRequest request = requestValido();
        request.setFirmaBase64(null);

        assertThatThrownBy(() -> service.registrarEntrega("MT-1", request))
                .isInstanceOf(BadRequestException.class);
        verify(envioTrackingRepository, never()).findWithClienteByCodigoUnico(anyString());
    }

    @Test
    void obtenerEntrega_retornaDto() {
        EnvioTracking envio = envioRecibido(1L);
        EntregaEvidencia evidencia = new EntregaEvidencia(envio, "Ana López", "12345678",
                PNG_1X1, -25.2637421, -57.575926, "Recibido en mano");
        evidencia.setId(7L);
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1")).thenReturn(Optional.of(envio));
        when(entregaRepository.findByEnvioId(1L)).thenReturn(Optional.of(evidencia));

        EntregaEvidenciaDto dto = service.obtenerEntrega("MT-1");

        assertThat(dto.getCodigoRastreo()).isEqualTo("MT-1");
        assertThat(dto.getReceptorNombre()).isEqualTo("Ana López");
        assertThat(dto.getFirmaBase64()).isEqualTo(PNG_1X1);
        assertThat(dto.getLatitud()).isEqualTo(-25.2637421);
    }

    @Test
    void obtenerEntrega_sinEvidencia_lanza404() {
        EnvioTracking envio = envioRecibido(1L);
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1")).thenReturn(Optional.of(envio));
        when(entregaRepository.findByEnvioId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerEntrega("MT-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
