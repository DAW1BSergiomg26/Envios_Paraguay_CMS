package com.monteastur.envios.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.dto.api.PushSubscriptionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PushSubscriptionController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.admin.username=admin",
    "app.admin.password=test"
})
class PushSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataSource dataSource;

    @Test
    void subscribe_retorna200() throws Exception {
        PushSubscriptionRequest req = new PushSubscriptionRequest();
        req.setEndpoint("https://fcm.googleapis.com/test-endpoint");
        req.setKeys(null);

        mockMvc.perform(post("/api/v1/push/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void unsubscribe_retorna200() throws Exception {
        PushSubscriptionRequest req = new PushSubscriptionRequest();
        req.setEndpoint("https://fcm.googleapis.com/test-endpoint");

        mockMvc.perform(post("/api/v1/push/unsubscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void testPush_retorna200() throws Exception {
        mockMvc.perform(post("/api/v1/push/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
