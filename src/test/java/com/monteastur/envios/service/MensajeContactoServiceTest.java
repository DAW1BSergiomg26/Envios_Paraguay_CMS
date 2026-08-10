package com.monteastur.envios.service;

import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.repository.MensajeContactoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MensajeContactoServiceTest {

    @Mock
    private MensajeContactoRepository mensajeContactoRepository;

    @InjectMocks
    private MensajeContactoService mensajeContactoService;

    private MensajeContacto mensaje(String nombre, boolean leido) {
        MensajeContacto m = new MensajeContacto(nombre, nombre + "@example.com", "+34 600 000 000", "Mensaje de prueba");
        m.setLeido(leido);
        return m;
    }

    @Test
    void listar_sinFiltro_devuelveTodos() {
        when(mensajeContactoRepository.findAllByOrderByFechaEnvioDesc())
            .thenReturn(List.of(mensaje("Ana", false), mensaje("Luis", true)));

        List<MensajeContacto> resultado = mensajeContactoService.listar(null);

        assertEquals(2, resultado.size());
        verify(mensajeContactoRepository).findAllByOrderByFechaEnvioDesc();
    }

    @Test
    void listar_filtroLeidos_devuelveSoloLeidos() {
        when(mensajeContactoRepository.findAllByOrderByFechaEnvioDesc())
            .thenReturn(List.of(mensaje("Ana", false), mensaje("Luis", true)));

        List<MensajeContacto> resultado = mensajeContactoService.listar(true);

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).isLeido());
    }

    @Test
    void listar_filtroNoLeidos_devuelveSoloNoLeidos() {
        when(mensajeContactoRepository.findAllByOrderByFechaEnvioDesc())
            .thenReturn(List.of(mensaje("Ana", false), mensaje("Luis", true)));

        List<MensajeContacto> resultado = mensajeContactoService.listar(false);

        assertEquals(1, resultado.size());
        assertFalse(resultado.get(0).isLeido());
    }

    @Test
    void marcarLeido_ok_guardaCambio() {
        MensajeContacto m = mensaje("Ana", false);
        when(mensajeContactoRepository.findById(1L)).thenReturn(Optional.of(m));
        when(mensajeContactoRepository.save(any(MensajeContacto.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<MensajeContacto> resultado = mensajeContactoService.marcarLeido(1L, true);

        assertTrue(resultado.isPresent());
        assertTrue(resultado.get().isLeido());
        verify(mensajeContactoRepository).save(m);
    }

    @Test
    void marcarLeido_inexistente_retornaEmpty() {
        when(mensajeContactoRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<MensajeContacto> resultado = mensajeContactoService.marcarLeido(99L, true);

        assertTrue(resultado.isEmpty());
        verify(mensajeContactoRepository, never()).save(any());
    }

    @Test
    void eliminar_ok() {
        mensajeContactoService.eliminar(1L);
        verify(mensajeContactoRepository).deleteById(1L);
    }
}
