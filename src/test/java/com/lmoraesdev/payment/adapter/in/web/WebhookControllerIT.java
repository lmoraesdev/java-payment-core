package com.lmoraesdev.payment.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmoraesdev.payment.support.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("WebhookControllerIT")
class WebhookControllerIT {

    @Autowired MockMvc mockMvc;

    private String createCharge() throws Exception {
        MvcResult created =
                mockMvc.perform(
                                post("/charges")
                                        .header("Idempotency-Key", UUID.randomUUID().toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"amount\": 100.00}"))
                        .andExpect(status().isCreated())
                        .andReturn();

        JsonNode body = new ObjectMapper().readTree(created.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    @Test
    @DisplayName("POST /webhooks/provider com status PAID → 200 e transiciona a charge para PAID")
    void webhookTransitionsChargeToPaid() throws Exception {
        String chargeId = createCharge();
        String eventId = UUID.randomUUID().toString();

        mockMvc.perform(
                        post("/webhooks/provider")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"eventId\": \"%s\", \"chargeId\": \"%s\", \"status\": \"PAID\"}"
                                                .formatted(eventId, chargeId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/charges/{id}", chargeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("POST /webhooks/provider com eventId repetido → 200 e não duplica o processamento")
    void webhookWithRepeatedEventIdIsIdempotent() throws Exception {
        String chargeId = createCharge();
        String eventId = UUID.randomUUID().toString();
        String payload =
                "{\"eventId\": \"%s\", \"chargeId\": \"%s\", \"status\": \"PAID\"}"
                        .formatted(eventId, chargeId);

        mockMvc.perform(
                        post("/webhooks/provider")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/webhooks/provider")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/charges/{id}", chargeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("POST /webhooks/provider com status inválido → 400 Problem Details")
    void webhookWithInvalidStatusReturns400() throws Exception {
        String chargeId = createCharge();
        String eventId = UUID.randomUUID().toString();

        mockMvc.perform(
                        post("/webhooks/provider")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"eventId\": \"%s\", \"chargeId\": \"%s\", \"status\": \"FOO\"}"
                                                .formatted(eventId, chargeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
