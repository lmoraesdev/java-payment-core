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
@DisplayName("ChargeControllerIT")
class ChargeControllerIT {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("POST /charges com amount válido → 201 com id, status e createdAt")
    void createChargeReturns201() throws Exception {
        mockMvc.perform(
                        post("/charges")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"amount\": 100.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /charges com amount negativo → 400 Problem Details com errors.amount")
    void createChargeWithNegativeAmountReturns400() throws Exception {
        mockMvc.perform(
                        post("/charges")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"amount\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    @DisplayName("POST /charges sem amount → 400 Problem Details com errors.amount")
    void createChargeWithMissingAmountReturns400() throws Exception {
        mockMvc.perform(post("/charges").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    @DisplayName("GET /charges/{id} de charge existente → 200 com id, status e amount")
    void getChargeReturns200() throws Exception {
        MvcResult created =
                mockMvc.perform(
                                post("/charges")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"amount\": 100.00}"))
                        .andExpect(status().isCreated())
                        .andReturn();

        JsonNode body = new ObjectMapper().readTree(created.getResponse().getContentAsString());
        String id = body.get("id").asText();

        mockMvc.perform(get("/charges/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /charges/{id} com UUID inexistente → 404")
    void getChargeWithUnknownIdReturns404() throws Exception {
        mockMvc.perform(get("/charges/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }
}
