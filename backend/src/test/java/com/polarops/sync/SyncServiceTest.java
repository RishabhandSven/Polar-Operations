package com.polarops.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarops.config.DataSeeder;
import com.polarops.inventory.InventoryTransactionEntity;
import com.polarops.inventory.InventoryTransactionRepository;
import com.polarops.inventory.StockLevelEntity;
import com.polarops.inventory.StockLevelRepository;
import com.polarops.sync.SyncDtos.SyncBatchRequest;
import com.polarops.sync.SyncDtos.SyncBatchResponse;
import com.polarops.sync.SyncDtos.SyncOperationRequest;
import com.polarops.sync.SyncDtos.SyncOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SyncServiceTest {

    @Autowired
    private SyncService syncService;

    @Autowired
    private ChangeEventRepository changeEventRepository;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testStationId;
    private UUID testFuelItemId;
    private UUID testDeviceId;

    @BeforeEach
    void setUp() {
        testStationId = DataSeeder.BHARATI_ID;
        testFuelItemId = DataSeeder.ITEM_FUEL_ID;
        testDeviceId = UUID.randomUUID();

        // Reset Bharati fuel stock to 8420.00
        StockLevelEntity fuelSl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        fuelSl.setCurrentStock(new BigDecimal("8420.00"));
        stockLevelRepository.save(fuelSl);
    }

    private JsonNode createInventoryPayload(UUID stationId, UUID itemId, String type, BigDecimal qty, String source, String ref, String op) {
        Map<String, Object> map = Map.of(
                "stationId", stationId.toString(),
                "inventoryItemId", itemId.toString(),
                "type", type,
                "quantity", qty,
                "source", source,
                "referenceId", ref,
                "operatorId", op
        );
        return objectMapper.valueToTree(map);
    }

    @Test
    void test1_SingleValidInventoryOperationIsApplied() {
        UUID clientTxId = UUID.randomUUID();
        JsonNode payload = createInventoryPayload(testStationId, testFuelItemId, "CONSUMPTION", new BigDecimal("100.00"), "OFFLINE_DEVICE", "OFFLINE-001", "OP-1");

        SyncBatchRequest req = new SyncBatchRequest(List.of(
                new SyncOperationRequest(clientTxId, testDeviceId, "INVENTORY_TRANSACTION", payload)
        ));

        SyncBatchResponse response = syncService.processBatch(req);

        assertThat(response.results()).hasSize(1);
        SyncOperationResult res = response.results().get(0);
        assertThat(res.clientTransactionId()).isEqualTo(clientTxId);
        assertThat(res.status()).isEqualTo("APPLIED");
    }

    @Test
    void test2_InventoryStockChangesCorrectly() {
        UUID clientTxId = UUID.randomUUID();
        JsonNode payload = createInventoryPayload(testStationId, testFuelItemId, "CONSUMPTION", new BigDecimal("120.00"), "OFFLINE_DEVICE", "OFFLINE-002", "OP-1");

        syncService.processBatch(new SyncBatchRequest(List.of(
                new SyncOperationRequest(clientTxId, testDeviceId, "INVENTORY_TRANSACTION", payload)
        )));

        StockLevelEntity fuelSl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        assertThat(fuelSl.getCurrentStock()).isEqualByComparingTo("8300.00"); // 8420 - 120
    }

    @Test
    void test3_InventoryTransactionIsCreated() {
        UUID clientTxId = UUID.randomUUID();
        JsonNode payload = createInventoryPayload(testStationId, testFuelItemId, "RECEIPT", new BigDecimal("500.00"), "OFFLINE_DEVICE", "OFF-REF-03", "OP-1");

        syncService.processBatch(new SyncBatchRequest(List.of(
                new SyncOperationRequest(clientTxId, testDeviceId, "INVENTORY_TRANSACTION", payload)
        )));

        List<InventoryTransactionEntity> txs = transactionRepository.findByStationIdAndInventoryItemIdOrderByTimestampDesc(testStationId, testFuelItemId);
        assertThat(txs).isNotEmpty();
        InventoryTransactionEntity latest = txs.get(0);
        assertThat(latest.getType()).isEqualTo("RECEIPT");
        assertThat(latest.getQuantity()).isEqualByComparingTo("500.00");
        assertThat(latest.getReferenceId()).isEqualTo("OFF-REF-03");
        assertThat(latest.getSource()).isEqualTo("OFFLINE_DEVICE");
    }

    @Test
    void test4_5_ChangeEventIsCreatedWithFullAuditFields() {
        UUID clientTxId = UUID.randomUUID();
        JsonNode payload = createInventoryPayload(testStationId, testFuelItemId, "CONSUMPTION", new BigDecimal("50.00"), "OFFLINE_DEVICE", "OFFLINE-004", "OP-AUDIT");

        syncService.processBatch(new SyncBatchRequest(List.of(
                new SyncOperationRequest(clientTxId, testDeviceId, "INVENTORY_TRANSACTION", payload)
        )));

        Optional<ChangeEventEntity> eventOpt = changeEventRepository.findByClientTransactionId(clientTxId);
        assertThat(eventOpt).isPresent();
        ChangeEventEntity event = eventOpt.get();
        assertThat(event.getId()).isNotNull();
        assertThat(event.getClientTransactionId()).isEqualTo(clientTxId);
        assertThat(event.getDeviceId()).isEqualTo(testDeviceId);
        assertThat(event.getOperation()).isEqualTo("INVENTORY_TRANSACTION");
        assertThat(event.getPayloadJson()).contains("OFFLINE-004");
        assertThat(event.getServerTimestamp()).isNotNull();
        assertThat(event.getStatus()).isEqualTo("APPLIED");
    }

    @Test
    void test6_7_8_SameClientTransactionIdSubmittedTwiceIsIdempotent() {
        UUID clientTxId = UUID.randomUUID();
        JsonNode payload = createInventoryPayload(testStationId, testFuelItemId, "CONSUMPTION", new BigDecimal("50.00"), "OFFLINE_DEVICE", "OFFLINE-005", "OP-1");

        SyncBatchRequest req = new SyncBatchRequest(List.of(
                new SyncOperationRequest(clientTxId, testDeviceId, "INVENTORY_TRANSACTION", payload)
        ));

        // 1st submission -> APPLIED
        SyncBatchResponse resp1 = syncService.processBatch(req);
        assertThat(resp1.results().get(0).status()).isEqualTo("APPLIED");

        StockLevelEntity fuelAfter1 = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        assertThat(fuelAfter1.getCurrentStock()).isEqualByComparingTo("8370.00"); // 8420 - 50

        long txCountAfter1 = transactionRepository.count();

        // 2nd submission (Replay) -> DUPLICATE
        SyncBatchResponse resp2 = syncService.processBatch(req);
        assertThat(resp2.results().get(0).status()).isEqualTo("DUPLICATE");

        // Stock MUST NOT change again
        StockLevelEntity fuelAfter2 = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        assertThat(fuelAfter2.getCurrentStock()).isEqualByComparingTo("8370.00");

        // Transaction ledger count MUST NOT increase
        long txCountAfter2 = transactionRepository.count();
        assertThat(txCountAfter2).isEqualTo(txCountAfter1);
    }

    @Test
    void test9_10_InvalidInventoryOperationReturnsRejectedAndDoesNotCreatePartialEvent() {
        UUID clientTxId = UUID.randomUUID();
        // Excess consumption beyond available stock (8420.00)
        JsonNode payload = createInventoryPayload(testStationId, testFuelItemId, "CONSUMPTION", new BigDecimal("999999.00"), "OFFLINE_DEVICE", "OFFLINE-FAIL", "OP-1");

        SyncBatchRequest req = new SyncBatchRequest(List.of(
                new SyncOperationRequest(clientTxId, testDeviceId, "INVENTORY_TRANSACTION", payload)
        ));

        SyncBatchResponse response = syncService.processBatch(req);
        assertThat(response.results()).hasSize(1);
        SyncOperationResult res = response.results().get(0);
        assertThat(res.status()).isEqualTo("REJECTED");
        assertThat(res.message()).contains("Insufficient stock");

        // No change event persisted for failed operation
        assertThat(changeEventRepository.findByClientTransactionId(clientTxId)).isEmpty();

        // Stock unmodified
        StockLevelEntity fuelSl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        assertThat(fuelSl.getCurrentStock()).isEqualByComparingTo("8420.00");
    }

    @Test
    void test11_12_BatchMultipleValidOperationsProcessesAllAndPreservesOrder() {
        UUID tx1 = UUID.randomUUID();
        UUID tx2 = UUID.randomUUID();
        UUID tx3 = UUID.randomUUID();

        JsonNode p1 = createInventoryPayload(testStationId, testFuelItemId, "CONSUMPTION", new BigDecimal("10.00"), "OFFLINE_DEVICE", "B-1", "OP");
        JsonNode p2 = createInventoryPayload(testStationId, testFuelItemId, "CONSUMPTION", new BigDecimal("20.00"), "OFFLINE_DEVICE", "B-2", "OP");
        JsonNode p3 = createInventoryPayload(testStationId, testFuelItemId, "RECEIPT", new BigDecimal("30.00"), "OFFLINE_DEVICE", "B-3", "OP");

        SyncBatchRequest req = new SyncBatchRequest(List.of(
                new SyncOperationRequest(tx1, testDeviceId, "INVENTORY_TRANSACTION", p1),
                new SyncOperationRequest(tx2, testDeviceId, "INVENTORY_TRANSACTION", p2),
                new SyncOperationRequest(tx3, testDeviceId, "INVENTORY_TRANSACTION", p3)
        ));

        SyncBatchResponse resp = syncService.processBatch(req);

        assertThat(resp.results()).hasSize(3);
        assertThat(resp.results().get(0).clientTransactionId()).isEqualTo(tx1);
        assertThat(resp.results().get(0).status()).isEqualTo("APPLIED");

        assertThat(resp.results().get(1).clientTransactionId()).isEqualTo(tx2);
        assertThat(resp.results().get(1).status()).isEqualTo("APPLIED");

        assertThat(resp.results().get(2).clientTransactionId()).isEqualTo(tx3);
        assertThat(resp.results().get(2).status()).isEqualTo("APPLIED");

        // Total stock: 8420 - 10 - 20 + 30 = 8420.00
        StockLevelEntity fuelSl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        assertThat(fuelSl.getCurrentStock()).isEqualByComparingTo("8420.00");
    }

    @Test
    void test13_14_PartialBatchSuccess_InvalidBOperationDoesNotRollbackAOrC() {
        UUID txA = UUID.randomUUID();
        UUID txB = UUID.randomUUID();
        UUID txC = UUID.randomUUID();

        JsonNode pA = createInventoryPayload(testStationId, testFuelItemId, "CONSUMPTION", new BigDecimal("100.00"), "OFFLINE", "TX-A", "OP");
        JsonNode pB = createInventoryPayload(testStationId, testFuelItemId, "CONSUMPTION", new BigDecimal("9999999.00"), "OFFLINE", "TX-B", "OP"); // Exceeds stock
        JsonNode pC = createInventoryPayload(testStationId, testFuelItemId, "CONSUMPTION", new BigDecimal("200.00"), "OFFLINE", "TX-C", "OP");

        SyncBatchRequest req = new SyncBatchRequest(List.of(
                new SyncOperationRequest(txA, testDeviceId, "INVENTORY_TRANSACTION", pA),
                new SyncOperationRequest(txB, testDeviceId, "INVENTORY_TRANSACTION", pB),
                new SyncOperationRequest(txC, testDeviceId, "INVENTORY_TRANSACTION", pC)
        ));

        SyncBatchResponse resp = syncService.processBatch(req);

        assertThat(resp.results()).hasSize(3);
        assertThat(resp.results().get(0).clientTransactionId()).isEqualTo(txA);
        assertThat(resp.results().get(0).status()).isEqualTo("APPLIED");

        assertThat(resp.results().get(1).clientTransactionId()).isEqualTo(txB);
        assertThat(resp.results().get(1).status()).isEqualTo("REJECTED");

        assertThat(resp.results().get(2).clientTransactionId()).isEqualTo(txC);
        assertThat(resp.results().get(2).status()).isEqualTo("APPLIED");

        // Total stock: 8420 - 100 - 200 = 8120.00
        StockLevelEntity fuelSl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        assertThat(fuelSl.getCurrentStock()).isEqualByComparingTo("8120.00");

        assertThat(changeEventRepository.findByClientTransactionId(txA)).isPresent();
        assertThat(changeEventRepository.findByClientTransactionId(txB)).isEmpty();
        assertThat(changeEventRepository.findByClientTransactionId(txC)).isPresent();
    }

    @Test
    void test15_UnsupportedOperationRejected() {
        UUID clientTxId = UUID.randomUUID();
        JsonNode payload = createInventoryPayload(testStationId, testFuelItemId, "CONSUMPTION", new BigDecimal("10.00"), "OFFLINE", "REF", "OP");

        SyncBatchRequest req = new SyncBatchRequest(List.of(
                new SyncOperationRequest(clientTxId, testDeviceId, "UNKNOWN_ARBITRARY_OP", payload)
        ));

        SyncBatchResponse resp = syncService.processBatch(req);
        assertThat(resp.results().get(0).status()).isEqualTo("REJECTED");
        assertThat(resp.results().get(0).message()).contains("Unsupported operation");
    }

    @Test
    void test16_ConcurrentSubmissionHandledSafely() throws InterruptedException {
        UUID concurrentTxId = UUID.randomUUID();
        JsonNode payload = createInventoryPayload(testStationId, testFuelItemId, "CONSUMPTION", new BigDecimal("10.00"), "OFFLINE", "REF-CONCURRENT", "OP");

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger appliedCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    SyncBatchResponse resp = syncService.processBatch(new SyncBatchRequest(List.of(
                            new SyncOperationRequest(concurrentTxId, testDeviceId, "INVENTORY_TRANSACTION", payload)
                    )));
                    String status = resp.results().get(0).status();
                    if ("APPLIED".equals(status)) {
                        appliedCount.incrementAndGet();
                    } else if ("DUPLICATE".equals(status)) {
                        duplicateCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(appliedCount.get()).isEqualTo(1);
        assertThat(duplicateCount.get()).isEqualTo(threadCount - 1);

        // Stock decreased exactly once by 10.00
        StockLevelEntity fuelSl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testFuelItemId).orElseThrow();
        assertThat(fuelSl.getCurrentStock()).isEqualByComparingTo("8410.00");
    }
}
