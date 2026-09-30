package com.polarops.cargo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarops.cargo.CargoDtos.AddCargoItemRequest;
import com.polarops.cargo.CargoDtos.CreateCargoConsignmentRequest;
import com.polarops.config.DataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CargoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateCargoConsignmentSuccess() throws Exception {
        String tracking = "VOY-CTRL-" + UUID.randomUUID().toString().substring(0, 6);
        CreateCargoConsignmentRequest req = new CreateCargoConsignmentRequest(
                tracking,
                DataSeeder.BHARATI_ID,
                "PLANNED",
                LocalDate.now(),
                LocalDate.now().plusDays(15),
                List.of(new AddCargoItemRequest(DataSeeder.ITEM_FUEL_ID, new BigDecimal("500.00")))
        );

        mockMvc.perform(post("/api/cargo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNumber").value(tracking.toUpperCase()))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].quantity").value(500.00));
    }

    @Test
    void testListCargoConsignments() throws Exception {
        mockMvc.perform(get("/api/cargo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].trackingNumber").exists());
    }

    @Test
    void testGetConsignmentById() throws Exception {
        mockMvc.perform(get("/api/cargo/" + DataSeeder.CARGO_BHARATI_ARRIVED_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DataSeeder.CARGO_BHARATI_ARRIVED_ID.toString()))
                .andExpect(jsonPath("$.status").value("ARRIVED"))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void testAddCargoItemSuccess() throws Exception {
        AddCargoItemRequest req = new AddCargoItemRequest(
                DataSeeder.ITEM_BATT_ID,
                new BigDecimal("10.00")
        );

        mockMvc.perform(post("/api/cargo/" + DataSeeder.CARGO_BHARATI_ARRIVED_ID + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(10.00))
                .andExpect(jsonPath("$.itemCode").value("BATTERY-12V"));
    }

    @Test
    void testReceiveArrivedCargoSuccess() throws Exception {
        mockMvc.perform(post("/api/cargo/" + DataSeeder.CARGO_BHARATI_ARRIVED_ID + "/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operatorId\": \"TEST-RECEIVER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousStatus").value("ARRIVED"))
                .andExpect(jsonPath("$.newStatus").value("RECEIVED"))
                .andExpect(jsonPath("$.receivedItemCount").value(2))
                .andExpect(jsonPath("$.totalReceivedQuantity").value(3500.00));
    }

    @Test
    void testReceiveInTransitCargoFails422() throws Exception {
        mockMvc.perform(post("/api/cargo/" + DataSeeder.CARGO_MAITRI_IN_TRANSIT_ID + "/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Must be ARRIVED")));
    }
}
