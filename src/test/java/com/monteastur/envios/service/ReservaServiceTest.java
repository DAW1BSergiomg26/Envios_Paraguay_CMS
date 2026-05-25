package com.monteastur.envios.service;

import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private ReservaService reservaService;

    @Test
    void crear_reserva_ok() {
        Reserva reserva = new Reserva();
        reserva.setNombreCliente("Maria Lopez");
        reserva.setEmail("maria@example.com");
        reserva.setTelefono("+595981234567");
        reserva.setFechaEntrada(LocalDate.of(2026, 6, 15));
        reserva.setFechaSalida(LocalDate.of(2026, 6, 20));
        reserva.setNumeroHuespedes(2);
        reserva.setEstado("pendiente");

        when(reservaRepository.save(any(Reserva.class))).thenReturn(reserva);

        Reserva resultado = reservaService.crear(reserva);

        assertNotNull(resultado);
        assertEquals("Maria Lopez", resultado.getNombreCliente());
        assertEquals("maria@example.com", resultado.getEmail());
        assertEquals("pendiente", resultado.getEstado());
        verify(reservaRepository, times(1)).save(reserva);
    }

    @Test
    void buscarPorId_existente_retornaReserva() {
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setNombreCliente("Carlos Gomez");

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        Optional<Reserva> resultado = reservaService.buscarPorId(1L);

        assertTrue(resultado.isPresent());
        assertEquals("Carlos Gomez", resultado.get().getNombreCliente());
    }

    @Test
    void buscarPorId_inexistente_retornaEmpty() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Reserva> resultado = reservaService.buscarPorId(99L);

        assertFalse(resultado.isPresent());
    }
}
