package com.polarops.cargo;

import com.polarops.cargo.CargoDtos.AddCargoItemRequest;
import com.polarops.cargo.CargoDtos.CargoConsignmentDto;
import com.polarops.cargo.CargoDtos.CargoItemDto;
import com.polarops.cargo.CargoDtos.CreateCargoConsignmentRequest;
import com.polarops.cargo.CargoDtos.ReceiveCargoResponse;
import com.polarops.common.DomainException;
import com.polarops.common.ResourceNotFoundException;
import com.polarops.config.DataSeeder;
import com.polarops.inventory.InventoryItemRepository;
import com.polarops.inventory.InventoryTransactionEntity;
import com.polarops.inventory.InventoryTransactionRepository;
import com.polarops.inventory.StockLevelEntity;
import com.polarops.inventory.StockLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CargoServiceTest {

    @Autowired
    private CargoService cargoService;

    @Autowired
    private CargoConsignmentRepository consignmentRepository;

    @Autowired
    private CargoItemRepository cargoItemRepository;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private InventoryItemRepository itemRepository;

    private UUID testStationId;
    private UUID testFuelItemId;
    private UUID testFoodItemId;

    @BeforeEach
    void setUp() {
        testStationId = DataSeeder.BHARATI_ID;
        testFuelItemId = DataSeeder.ITEM_FUEL_ID;
        testFoodItemId = DataSeeder.ITEM_FOOD_ID;

        // Reset Bharati fuel stock to 8420.00
        StockLevelEntity fuelSl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        fuelSl.setCurrentStock(new BigDecimal("8420.00"));
        stockLevelRepository.save(fuelSl);

        // Reset Bharati food stock to 2400.00
        StockLevelEntity foodSl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFoodItemId).orElseThrow();
        foodSl.setCurrentStock(new BigDecimal("2400.00"));
        stockLevelRepository.save(foodSl);

        // Reset Bharati Arrived Consignment to ARRIVED
        CargoConsignmentEntity arrived = consignmentRepository.findById(DataSeeder.CARGO_BHARATI_ARRIVED_ID).orElse(null);
        if (arrived != null) {
            arrived.setStatus("ARRIVED");
            arrived.setActualArrival(null);
            consignmentRepository.save(arrived);
        }
    }

    @Test
    void test1_CreateConsignmentSuccessfully() {
        String tracking = "VOY-TEST-" + UUID.randomUUID().toString().substring(0, 6);
        CreateCargoConsignmentRequest req = new CreateCargoConsignmentRequest(
                tracking,
                testStationId,
                "PLANNED",
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                List.of(new AddCargoItemRequest(testFuelItemId, new BigDecimal("1000.00")))
        );

        CargoConsignmentDto created = cargoService.createConsignment(req);

        assertThat(created.id()).isNotNull();
        assertThat(created.trackingNumber()).isEqualTo(tracking.toUpperCase());
        assertThat(created.status()).isEqualTo("PLANNED");
        assertThat(created.items()).hasSize(1);
        assertThat(created.items().get(0).quantity()).isEqualByComparingTo("1000.00");
    }

    @Test
    void test2_RejectDuplicateTrackingNumber() {
        CreateCargoConsignmentRequest req = new CreateCargoConsignmentRequest(
                "VOY-2026-BHA-004", // Already seeded
                testStationId,
                "PLANNED",
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                null
        );

        assertThatThrownBy(() -> cargoService.createConsignment(req))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void test3_AddCargoItemSuccessfully() {
        String tracking = "VOY-ADD-" + UUID.randomUUID().toString().substring(0, 6);
        CargoConsignmentDto consignment = cargoService.createConsignment(new CreateCargoConsignmentRequest(
                tracking, testStationId, "ARRIVED", LocalDate.now().minusDays(10), LocalDate.now(), null
        ));

        CargoItemDto item = cargoService.addCargoItem(consignment.id(), new AddCargoItemRequest(
                testFuelItemId, new BigDecimal("750.00")
        ));

        assertThat(item.id()).isNotNull();
        assertThat(item.consignmentId()).isEqualTo(consignment.id());
        assertThat(item.quantity()).isEqualByComparingTo("750.00");
    }

    @Test
    void test4_RejectZeroQuantity() {
        String tracking = "VOY-ZERO-" + UUID.randomUUID().toString().substring(0, 6);
        CargoConsignmentDto consignment = cargoService.createConsignment(new CreateCargoConsignmentRequest(
                tracking, testStationId, "ARRIVED", LocalDate.now().minusDays(10), LocalDate.now(), null
        ));

        assertThatThrownBy(() -> cargoService.addCargoItem(consignment.id(), new AddCargoItemRequest(
                testFuelItemId, BigDecimal.ZERO
        ))).isInstanceOf(DomainException.class)
                .hasMessageContaining("quantity must be strictly greater than 0");
    }

    @Test
    void test5_RejectNegativeQuantity() {
        String tracking = "VOY-NEG-" + UUID.randomUUID().toString().substring(0, 6);
        CargoConsignmentDto consignment = cargoService.createConsignment(new CreateCargoConsignmentRequest(
                tracking, testStationId, "ARRIVED", LocalDate.now().minusDays(10), LocalDate.now(), null
        ));

        assertThatThrownBy(() -> cargoService.addCargoItem(consignment.id(), new AddCargoItemRequest(
                testFuelItemId, new BigDecimal("-50.00")
        ))).isInstanceOf(DomainException.class)
                .hasMessageContaining("quantity must be strictly greater than 0");
    }

    @Test
    void test6_GetConsignmentWithItems() {
        CargoConsignmentDto dto = cargoService.getConsignment(DataSeeder.CARGO_BHARATI_ARRIVED_ID);

        assertThat(dto).isNotNull();
        assertThat(dto.trackingNumber()).isEqualTo("VOY-2026-BHA-004");
        assertThat(dto.status()).isEqualTo("ARRIVED");
        assertThat(dto.items()).hasSize(2);
    }

    @Test
    void test7_ArrivedToReceivedSucceeds() {
        ReceiveCargoResponse response = cargoService.receiveConsignment(
                DataSeeder.CARGO_BHARATI_ARRIVED_ID,
                "LOG-OFFICER-TEST"
        );

        assertThat(response.consignmentId()).isEqualTo(DataSeeder.CARGO_BHARATI_ARRIVED_ID);
        assertThat(response.previousStatus()).isEqualTo("ARRIVED");
        assertThat(response.newStatus()).isEqualTo("RECEIVED");
        assertThat(response.receivedItemCount()).isEqualTo(2);
        assertThat(response.totalReceivedQuantity()).isEqualByComparingTo("3500.00"); // 3000L Fuel + 500KG Food
    }

    @Test
    void test8_ReceivedToReceivedFails() {
        cargoService.receiveConsignment(DataSeeder.CARGO_BHARATI_ARRIVED_ID, "OP-1");

        assertThatThrownBy(() -> cargoService.receiveConsignment(DataSeeder.CARGO_BHARATI_ARRIVED_ID, "OP-2"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("already been received");
    }

    @Test
    void test9_ReceivingNonExistentConsignmentFails() {
        assertThatThrownBy(() -> cargoService.receiveConsignment(UUID.randomUUID(), "OP-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void test10_ReceivingInTransitConsignmentFails() {
        assertThatThrownBy(() -> cargoService.receiveConsignment(DataSeeder.CARGO_MAITRI_IN_TRANSIT_ID, "OP-1"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Must be ARRIVED");
    }

    @Test
    void test11_ReceivingArrivedCargoCreatesInventoryTransactions() {
        long initialTxCount = transactionRepository.count();

        cargoService.receiveConsignment(DataSeeder.CARGO_BHARATI_ARRIVED_ID, "OP-TEST");

        assertThat(transactionRepository.count()).isEqualTo(initialTxCount + 2);

        List<InventoryTransactionEntity> fuelTxs = transactionRepository.findByStationIdAndInventoryItemIdOrderByTimestampDesc(
                testStationId, testFuelItemId
        );
        assertThat(fuelTxs).isNotEmpty();
        assertThat(fuelTxs.get(0).getType()).isEqualTo("RECEIPT");
        assertThat(fuelTxs.get(0).getSource()).isEqualTo("CARGO_RECEIPT");
        assertThat(fuelTxs.get(0).getReferenceId()).isEqualTo(DataSeeder.CARGO_BHARATI_ARRIVED_ID.toString());
        assertThat(fuelTxs.get(0).getQuantity()).isEqualByComparingTo("3000.00");
    }

    @Test
    void testExplicit_ReferenceIdMatchesConsignmentIdString() {
        String tracking = "VOY-REF-" + UUID.randomUUID().toString().substring(0, 6);
        CargoConsignmentDto consignment = cargoService.createConsignment(new CreateCargoConsignmentRequest(
                tracking,
                testStationId,
                "ARRIVED",
                LocalDate.now().minusDays(5),
                LocalDate.now(),
                List.of(new AddCargoItemRequest(testFuelItemId, new BigDecimal("150.00")))
        ));

        cargoService.receiveConsignment(consignment.id(), "REF-CHECK-OP");

        List<InventoryTransactionEntity> txs = transactionRepository.findByStationIdAndInventoryItemIdOrderByTimestampDesc(
                testStationId, testFuelItemId
        );
        assertThat(txs).isNotEmpty();
        InventoryTransactionEntity latestTx = txs.get(0);

        assertThat(latestTx.getSource()).isEqualTo("CARGO_RECEIPT");
        assertThat(latestTx.getReferenceId()).isEqualTo(consignment.id().toString());
    }

    @Test
    void test12_ReceivingCargoIncreasesCorrespondingStockLevels() {
        StockLevelEntity initialFuel = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        BigDecimal initialFuelStock = initialFuel.getCurrentStock(); // 8420.00

        StockLevelEntity initialFood = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFoodItemId).orElseThrow();
        BigDecimal initialFoodStock = initialFood.getCurrentStock(); // 2400.00

        cargoService.receiveConsignment(DataSeeder.CARGO_BHARATI_ARRIVED_ID, "OP-TEST");

        StockLevelEntity updatedFuel = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        assertThat(updatedFuel.getCurrentStock()).isEqualByComparingTo(initialFuelStock.add(new BigDecimal("3000.00"))); // 11420.00

        StockLevelEntity updatedFood = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFoodItemId).orElseThrow();
        assertThat(updatedFood.getCurrentStock()).isEqualByComparingTo(initialFoodStock.add(new BigDecimal("500.00"))); // 2900.00
    }

    @Test
    void test13_MultipleCargoItemsCreateMultipleInventoryReceipts() {
        String tracking = "VOY-MULTI-" + UUID.randomUUID().toString().substring(0, 6);
        CargoConsignmentDto consignment = cargoService.createConsignment(new CreateCargoConsignmentRequest(
                tracking,
                testStationId,
                "ARRIVED",
                LocalDate.now().minusDays(10),
                LocalDate.now(),
                List.of(
                        new AddCargoItemRequest(testFuelItemId, new BigDecimal("100.00")),
                        new AddCargoItemRequest(testFoodItemId, new BigDecimal("200.00")),
                        new AddCargoItemRequest(DataSeeder.ITEM_MED_ID, new BigDecimal("5.00"))
                )
        ));

        ReceiveCargoResponse res = cargoService.receiveConsignment(consignment.id(), "OP-MULTI");
        assertThat(res.receivedItemCount()).isEqualTo(3);
        assertThat(res.totalReceivedQuantity()).isEqualByComparingTo("305.00");
    }

    @Test
    void test14_TransactionRollbackWhenItemFails() {
        String tracking = "VOY-FAIL-" + UUID.randomUUID().toString().substring(0, 6);
        CargoConsignmentDto consignment = cargoService.createConsignment(new CreateCargoConsignmentRequest(
                tracking,
                testStationId,
                "ARRIVED",
                LocalDate.now().minusDays(5),
                LocalDate.now(),
                null
        ));

        // Add valid item 1 (Bharati Fuel)
        cargoService.addCargoItem(consignment.id(), new AddCargoItemRequest(testFuelItemId, new BigDecimal("500.00")));

        // Create an unallocated item (has master item entity, but NO stock level at Bharati)
        com.polarops.inventory.InventoryItemEntity unallocatedItem = itemRepository.save(new com.polarops.inventory.InventoryItemEntity(
                UUID.randomUUID(),
                "SPECIAL-UNALLOCATED-" + UUID.randomUUID().toString().substring(0, 6),
                "Unallocated Item",
                "SCIENTIFIC",
                "UNITS",
                "No stock level allocated"
        ));

        // Add to consignment
        CargoItemEntity brokenCargoItem = new CargoItemEntity(
                UUID.randomUUID(),
                consignment.id(),
                unallocatedItem.getId(),
                new BigDecimal("10.00")
        );
        cargoItemRepository.save(brokenCargoItem);

        long txCountBefore = transactionRepository.count();
        StockLevelEntity fuelBefore = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        BigDecimal fuelStockBefore = fuelBefore.getCurrentStock();

        // Attempt receipt -> Must fail on item 2 because stock level does not exist
        assertThatThrownBy(() -> cargoService.receiveConsignment(consignment.id(), "OP-ROLLBACK"))
                .isInstanceOf(ResourceNotFoundException.class);

        // Verify Rollback:
        // 1. Cargo status remains ARRIVED
        CargoConsignmentEntity reloadedConsignment = consignmentRepository.findById(consignment.id()).orElseThrow();
        assertThat(reloadedConsignment.getStatus()).isEqualTo("ARRIVED");

        // 2. Fuel stock is completely unchanged (item 1 receipt was rolled back!)
        StockLevelEntity fuelAfter = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        assertThat(fuelAfter.getCurrentStock()).isEqualByComparingTo(fuelStockBefore);

        // 3. No inventory transactions persisted
        assertThat(transactionRepository.count()).isEqualTo(txCountBefore);
    }
}
