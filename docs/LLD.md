# Low-Level Design (LLD) — Revised
## PolarOps — Integrated Polar Expedition Logistics & Asset Management System
**SIH 2026 — Problem Statement SIH26062 Prototype**

---

### 1. Package & Class Layout

```
backend/src/main/java/com/polarops/
├── PolarOpsApplication.java
├── config/
│   ├── CorsConfig.java
│   └── DataSeeder.java
├── common/
│   ├── GlobalExceptionHandler.java
│   ├── DomainException.java
│   └── ResourceNotFoundException.java
├── expedition/
│   ├── StationEntity.java
│   ├── StationRepository.java
│   ├── StationService.java
│   ├── StationDtos.java
│   └── StationController.java
├── inventory/
│   ├── InventoryItemEntity.java
│   ├── StockLevelEntity.java
│   ├── InventoryTransactionEntity.java
│   ├── InventoryItemRepository.java
│   ├── StockLevelRepository.java
│   ├── InventoryTransactionRepository.java
│   ├── InventoryService.java
│   ├── InventoryIntelligenceService.java
│   ├── InventoryDtos.java
│   └── InventoryController.java
├── cargo/
│   ├── CargoConsignmentEntity.java
│   ├── CargoItemEntity.java
│   ├── CargoConsignmentRepository.java
│   ├── CargoService.java
│   ├── CargoDtos.java
│   └── CargoController.java
├── personnel/
│   ├── PersonnelEntity.java
│   ├── PersonnelMovementEntity.java
│   ├── PersonnelRepository.java
│   ├── PersonnelService.java
│   ├── PersonnelDtos.java
│   └── PersonnelController.java
├── emergency/
│   ├── AlertEntity.java
│   ├── AlertRepository.java
│   ├── EmergencyService.java
│   ├── EmergencyDtos.java
│   └── EmergencyController.java
└── sync/
    ├── ChangeEventEntity.java
    ├── ChangeEventRepository.java
    ├── SyncService.java
    ├── SyncDtos.java
    └── SyncController.java
```

---

### 2. Detailed Class Specifications

#### 2.1 Common Domain Handling

##### `GlobalExceptionHandler`
- Centralized exception mapping:
  - `ResourceNotFoundException` $\rightarrow$ 404 Not Found
  - `DomainException` $\rightarrow$ 422 Unprocessable Entity (with specific domain violation code)
  - `MethodArgumentNotValidException` $\rightarrow$ 400 Bad Request (field validation map)
  - `Exception` $\rightarrow$ 500 Internal Server Error

---

#### 2.2 Inventory Domain

##### `InventoryItemEntity` (Item Master)
- **Table**: `inventory_items`
- **Fields**:
  - `UUID id` (PK)
  - `String itemCode` (Unique, e.g., `FUEL-DIESEL-A`, `FOOD-RATION-STD`, `MED-TRAUMA-KIT`)
  - `String name` (e.g., Polar Diesel Arctic Grade)
  - `String category` (`FUEL`, `FOOD`, `MEDICAL`, `BATTERIES`, `SPARE_PARTS`, `SCIENTIFIC`)
  - `String unit` (`LITERS`, `KG`, `UNITS`, `BOXES`)
  - `String description`

##### `StockLevelEntity` (Station Specific Stock)
- **Table**: `stock_levels`
- **Fields**:
  - `UUID id` (PK)
  - `UUID stationId` (FK $\rightarrow$ `stations.id`)
  - `UUID inventoryItemId` (FK $\rightarrow$ `inventory_items.id`)
  - `BigDecimal currentStock`
  - `BigDecimal dailyConsumption`
  - `BigDecimal safetyStock`
  - `BigDecimal reorderPoint`
  - `int leadTimeDays`
  - `LocalDate nextResupplyDate`
  - `long version` (Optimistic Locking `@Version`)
- **Invariants**: Unique index on `(station_id, inventory_item_id)`. `currentStock >= 0`, `dailyConsumption >= 0`.

##### `InventoryTransactionEntity` (Ledger)
- **Table**: `inventory_transactions`
- **Fields**:
  - `UUID id` (PK)
  - `UUID stationId` (FK)
  - `UUID inventoryItemId` (FK)
  - `TransactionType type` (`RECEIPT`, `CONSUMPTION`, `TRANSFER_OUT`, `TRANSFER_IN`, `ADJUSTMENT`, `WASTE`)
  - `BigDecimal quantity` (Must be $> 0$)
  - `Instant timestamp`
  - `String source` (`MANUAL`, `CARGO_RECEIPT`, `INTER_STATION_TRANSFER`, `OFFLINE_SYNC`)
  - `String referenceId` (e.g., Cargo tracking number, Consignment ID)
  - `String operatorId`

##### `InventoryDtos.java` (Consolidated Records)
```java
public class InventoryDtos {
    public record InventoryItemDto(UUID id, String itemCode, String name, String category, String unit, String description) {}
    
    public record StockLevelDto(
        UUID id, UUID stationId, String stationName,
        UUID inventoryItemId, String itemCode, String itemName, String category, String unit,
        BigDecimal currentStock, BigDecimal dailyConsumption, BigDecimal safetyStock,
        BigDecimal reorderPoint, int leadTimeDays, LocalDate nextResupplyDate
    ) {}

    public record CreateTransactionRequest(
        @NotNull UUID stationId,
        @NotNull UUID inventoryItemId,
        @NotNull TransactionType type,
        @NotNull @DecimalMin(value = "0.01") BigDecimal quantity,
        String source,
        String referenceId,
        String operatorId
    ) {}

    public record DelaySimulationRequest(
        @NotNull UUID stationId,
        @Min(0) int delayDays
    ) {}

    public record StockRiskAssessment(
        UUID stockLevelId,
        UUID inventoryItemId,
        String itemCode,
        String itemName,
        String category,
        String unit,
        BigDecimal currentStock,
        BigDecimal safetyStock,
        BigDecimal dailyConsumption,
        LocalDate effectiveResupplyDate,
        Double daysToSafetyThreshold,
        BigDecimal projectedStockAtResupply,
        RiskLevel riskLevel, // CRITICAL, WARNING, HEALTHY
        String explanation
    ) {}

    public record TransferRecommendation(
        UUID sourceStationId,
        String sourceStationName,
        UUID targetStationId,
        String targetStationName,
        UUID inventoryItemId,
        String itemCode,
        String itemName,
        BigDecimal recommendedQuantity,
        String unit,
        String rationale
    ) {}

    public record StationIntelligenceReport(
        UUID stationId,
        String stationName,
        int simulatedDelayDays,
        List<StockRiskAssessment> assessments,
        List<TransferRecommendation> recommendations,
        Instant evaluatedAt
    ) {}
}
```

##### `InventoryService`
- **Methods**:
  - `List<StockLevelDto> getStationStock(UUID stationId)`
  - `@Transactional InventoryTransactionEntity recordTransaction(CreateTransactionRequest req)`:
    1. Fetch `StockLevelEntity` by `(stationId, inventoryItemId)`.
    2. Enforce stock mutation constraints:
       - `CONSUMPTION`, `TRANSFER_OUT`, `WASTE`: `currentStock >= quantity` (throw `DomainException("Insufficient stock")` if breached). Deduct `quantity`.
       - `RECEIPT`, `TRANSFER_IN`: Add `quantity`.
       - `ADJUSTMENT`: Direct adjustment (ensure result $\ge 0$).
    3. Save updated `StockLevelEntity`.
    4. Save new `InventoryTransactionEntity`.

##### `InventoryIntelligenceService`
- **Deterministic Math & Rules**:
  1. For each `StockLevelEntity` at station $S$:
     - Effective resupply date: $D_{\text{resupply}} = \text{nextResupplyDate} + \text{delayDays}$.
     - Days until resupply: $N = \max(0, \text{ChronoUnit.DAYS.between}(\text{today}, D_{\text{resupply}}))$.
  2. **Zero Consumption Case (`dailyConsumption == 0`)**:
     - `daysToSafetyThreshold = Double.POSITIVE_INFINITY`
     - `projectedStockAtResupply = currentStock`
     - If $\text{currentStock} \le \text{safetyStock}$: Risk is `CRITICAL` ("Current stock is already at or below safety stock. Zero consumption rate recorded.").
     - Else: Risk is `HEALTHY` ("Zero consumption rate; stock remains constant at current levels.").
  3. **Standard Consumption Case (`dailyConsumption > 0`)**:
     - `projectedStockAtResupply = currentStock - (dailyConsumption * N)`
     - **Stock Already Below Safety Case (`currentStock <= safetyStock`)**:
       - `daysToSafetyThreshold = 0.0`
       - Risk is `CRITICAL` ("Stock is already at or below safety threshold (" + currentStock + " <= " + safetyStock + "). Immediate resupply or emergency conservation required.").
     - **Projected Breach Case (`projectedStockAtResupply < safetyStock`)**:
       - `daysToSafetyThreshold = (currentStock - safetyStock) / dailyConsumption`
       - Risk is `CRITICAL` ("Safety threshold breached in " + String.format("%.1f", daysToSafetyThreshold) + " days, before scheduled resupply in " + N + " days.").
     - **Reorder Warning Case (`projectedStockAtResupply < reorderPoint`)**:
       - `daysToSafetyThreshold = (currentStock - safetyStock) / dailyConsumption`
       - Risk is `WARNING` ("Projected stock (" + projectedStockAtResupply + ") will drop below reorder point (" + reorderPoint + ") prior to resupply.").
     - **Healthy Case**:
       - `daysToSafetyThreshold = (currentStock - safetyStock) / dailyConsumption`
       - Risk is `HEALTHY` ("Projected stock (" + projectedStockAtResupply + ") remains above safety buffer (" + safetyStock + ").").
  4. **Inter-Station Transfer Engine**:
     - For any critical item at station $A$, query all other stations $B \ne A$ holding the same `inventoryItemId`.
     - Calculate surplus at station $B$:
       $$\text{surplus}_B = \text{currentStock}_B - (\text{safetyStock}_B + \text{dailyConsumption}_B \times N_B)$$
     - If $\text{surplus}_B > 0$:
       - $\text{shortfall}_A = \text{safetyStock}_A - \text{projectedStockAtResupply}_A$
       - $\text{recommendedQty} = \min(\text{shortfall}_A, \text{surplus}_B)$
       - Build explainable `TransferRecommendation`.

---

#### 2.3 Cargo Domain

##### `CargoConsignmentEntity` & `CargoItemEntity`
- **Tables**: `cargo_consignments`, `cargo_items`
- **Fields (`CargoConsignmentEntity`)**:
  - `UUID id` (PK)
  - `String trackingNumber` (e.g. `VOY-2026-BHA-004`)
  - `UUID destinationStationId` (FK)
  - `CargoStatus status` (`PLANNED`, `IN_TRANSIT`, `ARRIVED`, `RECEIVED`, `CANCELLED`)
  - `LocalDate departureDate`, `LocalDate estimatedArrival`, `LocalDate actualArrival`
  - `List<CargoItemEntity> items` (`@OneToMany(cascade = CascadeType.ALL)`)
- **Fields (`CargoItemEntity`)**:
  - `UUID id` (PK)
  - `UUID consignmentId` (FK)
  - `UUID inventoryItemId` (FK $\rightarrow$ `inventory_items.id`)
  - `BigDecimal quantity`

##### `CargoService`
- **Key Invariant**: Cargo can **ONLY** be received when in `ARRIVED` state. If `RECEIVED`, `PLANNED`, or `IN_TRANSIT`, throw `DomainException`.
- **Atomic Receipt (`@Transactional`)**:
  ```java
  @Transactional
  public ConsignmentDto receiveConsignment(UUID consignmentId, String operatorId) {
      CargoConsignmentEntity consignment = consignmentRepo.findById(consignmentId)
          .orElseThrow(() -> new ResourceNotFoundException("Consignment not found"));
      
      if (consignment.getStatus() == CargoStatus.RECEIVED) {
          throw new DomainException("Consignment has already been received");
      }
      if (consignment.getStatus() != CargoStatus.ARRIVED) {
          throw new DomainException("Consignment cannot be received in status: " + consignment.getStatus() + ". Must be ARRIVED.");
      }

      consignment.setStatus(CargoStatus.RECEIVED);
      consignment.setActualArrival(LocalDate.now());
      consignmentRepo.save(consignment);

      for (CargoItemEntity item : consignment.getItems()) {
          inventoryService.recordTransaction(new CreateTransactionRequest(
              consignment.getDestinationStationId(),
              item.getInventoryItemId(),
              TransactionType.RECEIPT,
              item.getQuantity(),
              "CARGO_RECEIPT",
              consignment.getTrackingNumber(),
              operatorId
          ));
      }
      return mapToDto(consignment);
  }
  ```

---

#### 2.4 Offline Synchronization Domain

##### `ChangeEventEntity`
- **Table**: `change_events`
- **Fields**:
  - `UUID id` (PK)
  - `String clientTransactionId` (Unique Index, UUID string)
  - `String deviceId` (UUID string)
  - `String operation` (`INVENTORY_TRANSACTION`, `RECEIVE_CARGO`)
  - `String payloadJson`
  - `Instant serverTimestamp`
  - `SyncStatus status` (`COMMITTED`, `DUPLICATE_IGNORED`, `REJECTED`)
  - `String errorMessage`

##### `SyncService` (Simple, direct implementation)
- **Key Method**:
  ```java
  @Transactional
  public SyncResultDto processSyncBatch(SyncPushRequest request) {
      List<String> successful = new ArrayList<>();
      List<SyncErrorDto> failed = new ArrayList<>();

      for (ClientTransactionDto tx : request.transactions()) {
          // Idempotency check
          Optional<ChangeEventEntity> existing = changeEventRepo.findByClientTransactionId(tx.clientTransactionId());
          if (existing.isPresent()) {
              successful.add(tx.clientTransactionId());
              continue;
          }

          try {
              switch (tx.operation()) {
                  case "INVENTORY_TRANSACTION" -> {
                      CreateTransactionRequest req = objectMapper.readValue(tx.payloadJson(), CreateTransactionRequest.class);
                      inventoryService.recordTransaction(req);
                  }
                  case "RECEIVE_CARGO" -> {
                      ReceiveCargoPayload req = objectMapper.readValue(tx.payloadJson(), ReceiveCargoPayload.class);
                      cargoService.receiveConsignment(req.consignmentId(), req.operatorId());
                  }
                  default -> throw new DomainException("Unknown sync operation: " + tx.operation());
              }

              ChangeEventEntity event = new ChangeEventEntity(
                  UUID.randomUUID(), tx.clientTransactionId(), tx.deviceId(),
                  tx.operation(), tx.payloadJson(), Instant.now(), SyncStatus.COMMITTED, null
              );
              changeEventRepo.save(event);
              successful.add(tx.clientTransactionId());
          } catch (Exception ex) {
              ChangeEventEntity failedEvent = new ChangeEventEntity(
                  UUID.randomUUID(), tx.clientTransactionId(), tx.deviceId(),
                  tx.operation(), tx.payloadJson(), Instant.now(), SyncStatus.REJECTED, ex.getMessage()
              );
              changeEventRepo.save(failedEvent);
              failed.add(new SyncErrorDto(tx.clientTransactionId(), ex.getMessage()));
          }
      }
      return new SyncResultDto(successful, failed, Instant.now());
  }
  ```

---

#### 2.5 Lightweight Personnel & Emergency Domains

##### `PersonnelService` & `EmergencyService`
- **Personnel**: Station roster with `role`, `status` (`ON_STATION`, `FIELD_SORTIE`, `EVACUATED`), check-in/check-out movements.
- **Emergency**: SOS alerts with `severity` (`CRITICAL`, `WARNING`), coordinates, and a snapshot of current station safety stock status (food/fuel days remaining).
