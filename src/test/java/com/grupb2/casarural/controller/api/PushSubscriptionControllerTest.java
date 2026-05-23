package com.grupb2.casarural.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupb2.casarural.config.SecurityConfig;
import com.grupb2.casarural.dto.api.PushSubscriptionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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
