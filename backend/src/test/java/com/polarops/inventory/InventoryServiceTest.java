package com.polarops.inventory;

import com.polarops.common.DomainException;
import com.polarops.config.DataSeeder;
import com.polarops.inventory.InventoryDtos.CreateInventoryItemRequest;
import com.polarops.inventory.InventoryDtos.CreateStockLevelRequest;
import com.polarops.inventory.InventoryDtos.CreateTransactionRequest;
import com.polarops.inventory.InventoryDtos.InventoryItemDto;
import com.polarops.inventory.InventoryDtos.StockLevelDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryItemRepository itemRepository;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    private UUID testStationId;
    private UUID testItemId;

    @BeforeEach
    void setUp() {
        testStationId = DataSeeder.BHARATI_ID;
        testItemId = DataSeeder.ITEM_FUEL_ID;
    }

    @Test
    void testCreateInventoryItem() {
        String uniqueCode = "SOLAR-PANEL-300W-" + UUID.randomUUID().toString().substring(0, 8);
        CreateInventoryItemRequest req = new CreateInventoryItemRequest(
                uniqueCode,
                "Photovoltaic 300W Panel",
                "SCIENTIFIC",
                "UNITS",
                "High-efficiency polar PV panel"
        );

        InventoryItemDto created = inventoryService.createItem(req);

        assertThat(created.id()).isNotNull();
        assertThat(created.itemCode()).isEqualTo(uniqueCode.toUpperCase());
        assertThat(created.name()).isEqualTo("Photovoltaic 300W Panel");
    }

    @Test
    void testRejectDuplicateItemCode() {
        CreateInventoryItemRequest req = new CreateInventoryItemRequest(
                "FUEL-DIESEL-A",
                "Duplicate Fuel Item",
                "FUEL",
                "LITERS",
                "Duplicate test"
        );

        assertThatThrownBy(() -> inventoryService.createItem(req))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void testCreateStockLevel() {
        // Create new item first
        String uniqueCode = "TEST-ITEM-" + UUID.randomUUID().toString().substring(0, 8);
        InventoryItemDto item = inventoryService.createItem(new CreateInventoryItemRequest(
                uniqueCode, "Test Sensor", "SCIENTIFIC", "UNITS", "Test"
        ));

        CreateStockLevelRequest req = new CreateStockLevelRequest(
                DataSeeder.HIMADRI_ID,
                item.id(),
                new BigDecimal("50.00"),
                new BigDecimal("2.00"),
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                14,
                LocalDate.now().plusDays(30)
        );

        StockLevelDto stockLevel = inventoryService.createStockLevel(req);

        assertThat(stockLevel.id()).isNotNull();
        assertThat(stockLevel.currentStock()).isEqualByComparingTo("50.00");
        assertThat(stockLevel.dailyConsumption()).isEqualByComparingTo("2.00");
    }

    @Test
    void testRejectDuplicateStockLevel() {
        // Bharati already has ITEM_FUEL_ID from DataSeeder
        CreateStockLevelRequest req = new CreateStockLevelRequest(
                DataSeeder.BHARATI_ID,
                DataSeeder.ITEM_FUEL_ID,
                new BigDecimal("100.00"),
                new BigDecimal("5.00"),
                new BigDecimal("20.00"),
                new BigDecimal("40.00"),
                14,
                LocalDate.now().plusDays(30)
        );

        assertThatThrownBy(() -> inventoryService.createStockLevel(req))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Stock level already exists");
    }

    @Test
    void testReceiptIncreasesStockAndCreatesTransaction() {
        StockLevelEntity initial = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        BigDecimal initialStock = initial.getCurrentStock();
        BigDecimal receiptQty = new BigDecimal("500.00");

        long initialTxCount = transactionRepository.count();

        CreateTransactionRequest req = new CreateTransactionRequest(
                testStationId,
                testItemId,
                "RECEIPT",
                receiptQty,
                "CARGO_SHIPMENT",
                "VOY-2026-TEST",
                "OP-01"
        );

        StockLevelDto result = inventoryService.recordTransaction(req);

        assertThat(result.currentStock()).isEqualByComparingTo(initialStock.add(receiptQty));
        assertThat(transactionRepository.count()).isEqualTo(initialTxCount + 1);

        List<InventoryTransactionEntity> txs = transactionRepository.findByStationIdAndInventoryItemIdOrderByTimestampDesc(testStationId, testItemId);
        assertThat(txs).isNotEmpty();
        assertThat(txs.get(0).getType()).isEqualTo("RECEIPT");
        assertThat(txs.get(0).getQuantity()).isEqualByComparingTo(receiptQty);
    }

    @Test
    void testConsumptionDecreasesStockAndCreatesTransaction() {
        StockLevelEntity initial = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        BigDecimal initialStock = initial.getCurrentStock();
        BigDecimal consumeQty = new BigDecimal("420.00");

        CreateTransactionRequest req = new CreateTransactionRequest(
                testStationId,
                testItemId,
                "CONSUMPTION",
                consumeQty,
                "DAILY_LOG",
                "GEN-DAILY-01",
                "OP-01"
        );

        StockLevelDto result = inventoryService.recordTransaction(req);

        assertThat(result.currentStock()).isEqualByComparingTo(initialStock.subtract(consumeQty));
    }

    @Test
    void testConsumptionGreaterThanAvailableStockFails() {
        StockLevelEntity initial = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        BigDecimal initialStock = initial.getCurrentStock();
        BigDecimal excessiveQty = initialStock.add(new BigDecimal("1000.00"));

        long initialTxCount = transactionRepository.count();

        CreateTransactionRequest req = new CreateTransactionRequest(
                testStationId,
                testItemId,
                "CONSUMPTION",
                excessiveQty,
                "DAILY_LOG",
                "GEN-DAILY-OVER",
                "OP-01"
        );

        assertThatThrownBy(() -> inventoryService.recordTransaction(req))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Insufficient stock");

        // Verify stock is untouched
        StockLevelEntity reloaded = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        assertThat(reloaded.getCurrentStock()).isEqualByComparingTo(initialStock);

        // Verify no transaction record was persisted
        assertThat(transactionRepository.count()).isEqualTo(initialTxCount);
    }

    @Test
    void testTransferOutDecreasesStock() {
        StockLevelEntity initial = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        BigDecimal initialStock = initial.getCurrentStock();
        BigDecimal transferQty = new BigDecimal("100.00");

        CreateTransactionRequest req = new CreateTransactionRequest(
                testStationId,
                testItemId,
                "TRANSFER_OUT",
                transferQty,
                "INTER_STATION",
                "TRF-MAI-01",
                "OP-01"
        );

        StockLevelDto result = inventoryService.recordTransaction(req);
        assertThat(result.currentStock()).isEqualByComparingTo(initialStock.subtract(transferQty));
    }

    @Test
    void testTransferInIncreasesStock() {
        StockLevelEntity initial = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        BigDecimal initialStock = initial.getCurrentStock();
        BigDecimal transferInQty = new BigDecimal("250.00");

        CreateTransactionRequest req = new CreateTransactionRequest(
                testStationId,
                testItemId,
                "TRANSFER_IN",
                transferInQty,
                "INTER_STATION",
                "TRF-MAI-01",
                "OP-01"
        );

        StockLevelDto result = inventoryService.recordTransaction(req);
        assertThat(result.currentStock()).isEqualByComparingTo(initialStock.add(transferInQty));
    }

    @Test
    void testWasteDecreasesStock() {
        StockLevelEntity initial = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        BigDecimal initialStock = initial.getCurrentStock();
        BigDecimal wasteQty = new BigDecimal("15.00");

        CreateTransactionRequest req = new CreateTransactionRequest(
                testStationId,
                testItemId,
                "WASTE",
                wasteQty,
                "SPOILAGE_REPORT",
                "WST-001",
                "OP-01"
        );

        StockLevelDto result = inventoryService.recordTransaction(req);
        assertThat(result.currentStock()).isEqualByComparingTo(initialStock.subtract(wasteQty));
    }

    @Test
    void testAdjustmentIncreasesStock() {
        StockLevelEntity initial = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        BigDecimal initialStock = initial.getCurrentStock();
        BigDecimal adjustQty = new BigDecimal("35.00");

        CreateTransactionRequest req = new CreateTransactionRequest(
                testStationId,
                testItemId,
                "ADJUSTMENT",
                adjustQty,
                "AUDIT",
                "AUD-2026-Q1",
                "OP-01"
        );

        StockLevelDto result = inventoryService.recordTransaction(req);
        assertThat(result.currentStock()).isEqualByComparingTo(initialStock.add(adjustQty));
    }
}
