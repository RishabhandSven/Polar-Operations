package com.polarops.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarops.config.DataSeeder;
import com.polarops.inventory.InventoryDtos.CreateInventoryItemRequest;
import com.polarops.inventory.InventoryDtos.CreateTransactionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllItems() throws Exception {
        mockMvc.perform(get("/api/inventory/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].itemCode").exists());
    }

    @Test
    void testCreateItemSuccess() throws Exception {
        String uniqueCode = "RADIO-HF-" + UUID.randomUUID().toString().substring(0, 6);
        CreateInventoryItemRequest req = new CreateInventoryItemRequest(
                uniqueCode, "Polar HF Transceiver", "SCIENTIFIC", "UNITS", "Long-range radio"
        );

        mockMvc.perform(post("/api/inventory/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemCode").value(uniqueCode.toUpperCase()))
                .andExpect(jsonPath("$.name").value("Polar HF Transceiver"));
    }

    @Test
    void testCreateItemValidationError() throws Exception {
        CreateInventoryItemRequest req = new CreateInventoryItemRequest(
                "", "", "", "", ""
        );

        mockMvc.perform(post("/api/inventory/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.itemCode").exists())
                .andExpect(jsonPath("$.details.name").exists());
    }

    @Test
    void testGetStationStockLevels() throws Exception {
        mockMvc.perform(get("/api/inventory")
                        .param("stationId", DataSeeder.BHARATI_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].stationName").value("Bharati Station"))
                .andExpect(jsonPath("$[0].currentStock").exists());
    }

    @Test
    void testGetStationStockLevelsNotFound() throws Exception {
        mockMvc.perform(get("/api/inventory")
                        .param("stationId", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testRecordTransactionSuccess() throws Exception {
        CreateTransactionRequest req = new CreateTransactionRequest(
                DataSeeder.BHARATI_ID,
                DataSeeder.ITEM_FUEL_ID,
                "CONSUMPTION",
                new BigDecimal("50.00"),
                "DAILY_OPS",
                "REF-001",
                "OPERATOR_1"
        );

        mockMvc.perform(post("/api/inventory/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").exists());
    }

    @Test
    void testRecordTransactionInsufficientStockFails422() throws Exception {
        CreateTransactionRequest req = new CreateTransactionRequest(
                DataSeeder.BHARATI_ID,
                DataSeeder.ITEM_FUEL_ID,
                "CONSUMPTION",
                new BigDecimal("9999999.00"),
                "DAILY_OPS",
                "REF-002",
                "OPERATOR_1"
        );

        mockMvc.perform(post("/api/inventory/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient stock")));
    }

    @Test
    void testGetStationIntelligenceReportSuccess() throws Exception {
        mockMvc.perform(get("/api/inventory/intelligence")
                        .param("stationId", DataSeeder.BHARATI_ID.toString())
                        .param("delayDays", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value(DataSeeder.BHARATI_ID.toString()))
                .andExpect(jsonPath("$.stationName").value("Bharati Station"))
                .andExpect(jsonPath("$.simulatedDelayDays").value(10))
                .andExpect(jsonPath("$.assessments").isArray())
                .andExpect(jsonPath("$.assessments[0].itemCode").exists())
                .andExpect(jsonPath("$.assessments[0].riskLevel").exists())
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void testGetStationIntelligenceNegativeDelayFails() throws Exception {
        mockMvc.perform(get("/api/inventory/intelligence")
                        .param("stationId", DataSeeder.BHARATI_ID.toString())
                        .param("delayDays", "-3"))
                .andExpect(status().isBadRequest());
    }
}
