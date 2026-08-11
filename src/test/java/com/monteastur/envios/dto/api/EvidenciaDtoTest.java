package com.monteastur.envios.dto.api;

import com.monteastur.envios.model.EvidenciaEnvio;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EvidenciaDtoTest {

    @Test
    void from_mapeaTodosLosCampos() {
        EvidenciaEnvio evidencia = new EvidenciaEnvio();
        evidencia.setId(42L);
        evidencia.setTitulo("Comprobante de entrega");
        evidencia.setDescripcion("Firma del destinatario");
        evidencia.setTipo("DOCUMENTO");
        evidencia.setUrlArchivo("/uploads/evidencias/uuid.pdf");
        evidencia.setVisibleCliente(false);
        evidencia.setFechaSubida(LocalDateTime.of(2026, 8, 11, 10, 30));

        EvidenciaDto dto = EvidenciaDto.from(evidencia);

        assertEquals(42L, dto.getId());
        assertEquals("Comprobante de entrega", dto.getTitulo());
        assertEquals("Firma del destinatario", dto.getDescripcion());
        assertEquals("DOCUMENTO", dto.getTipo());
        assertEquals("/uploads/evidencias/uuid.pdf", dto.getUrlArchivo());
        assertEquals(false, dto.getVisibleCliente());
        assertEquals(LocalDateTime.of(2026, 8, 11, 10, 30), dto.getFechaSubida());
    }

    @Test
    void from_nullDevuelveNull() {
        assertNull(EvidenciaDto.from(null));
    }
}
