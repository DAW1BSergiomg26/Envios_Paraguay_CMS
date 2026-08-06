package com.monteastur.envios.integration;

import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.DocumentoGeneradoRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminDocumentosPageIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EnvioTrackingRepository envioTrackingRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private DocumentoGeneradoRepository documentoRepository;

    @AfterEach
    void limpiar() {
        documentoRepository.deleteAll();
        envioTrackingRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    @Test
    void adminDocumentos_rendersClienteNombre_conEnvioVinculado() throws Exception {
        Cliente cliente = clienteRepository.save(
                new Cliente("ana.test@example.com", "secreto", "Ana Test Cliente", "600111222"));

        EnvioTracking envio = new EnvioTracking("MT-REND-0001", "RECIBIDO", "María López",
                "Asturias", "Asunción", "1.5 kg", "Documentos");
        envio.setCliente(cliente);
        envioTrackingRepository.save(envio);

        mockMvc.perform(get("/admin/documentos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cms/documentos"))
                .andExpect(content().string(containsString("Ana Test Cliente")));
    }
}
