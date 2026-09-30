package com.polarops.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarops.config.DataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testPushSyncOperations_SuccessAndDuplicateFlow() throws Exception {
        UUID clientTxId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();

        Map<String, Object> payload = Map.of(
                "stationId", DataSeeder.BHARATI_ID.toString(),
                "inventoryItemId", DataSeeder.ITEM_FUEL_ID.toString(),
                "type", "CONSUMPTION",
                "quantity", 10.00,
                "source", "OFFLINE_PWA",
                "referenceId", "SYNC-DEMO-001",
                "operatorId", "OFFLINE-OP"
        );

        Map<String, Object> req = Map.of(
                "operations", List.of(
                        Map.of(
                                "clientTransactionId", clientTxId.toString(),
                                "deviceId", deviceId.toString(),
                                "operation", "INVENTORY_TRANSACTION",
                                "payload", payload
                        )
                )
        );

        // 1st request -> APPLIED
        mockMvc.perform(post("/api/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].clientTransactionId").value(clientTxId.toString()))
                .andExpect(jsonPath("$.results[0].status").value("APPLIED"));

        // 2nd request (Replay) -> DUPLICATE
        mockMvc.perform(post("/api/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].clientTransactionId").value(clientTxId.toString()))
                .andExpect(jsonPath("$.results[0].status").value("DUPLICATE"));
    }

    @Test
    void testPushSyncOperations_MissingRequiredFieldsFails400() throws Exception {
        // Missing operations array or empty batch
        mockMvc.perform(post("/api/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operations\": []}"))
                .andExpect(status().isBadRequest());

        // Missing clientTransactionId
        Map<String, Object> badReq = Map.of(
                "operations", List.of(
                        Map.of(
                                "deviceId", UUID.randomUUID().toString(),
                                "operation", "INVENTORY_TRANSACTION",
                                "payload", Map.of("foo", "bar")
                        )
                )
        );

        mockMvc.perform(post("/api/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());
    }
}
