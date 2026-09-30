package com.polarops.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class InventoryDtos {

    private InventoryDtos() {}

    public record InventoryItemDto(
            UUID id,
            String itemCode,
            String name,
            String category,
            String unit,
            String description
    ) {}

    public record CreateInventoryItemRequest(
            @NotBlank(message = "itemCode is required")
            String itemCode,

            @NotBlank(message = "name is required")
            String name,

            @NotBlank(message = "category is required")
            String category,

            @NotBlank(message = "unit is required")
            String unit,

            String description
    ) {}

    public record StockLevelDto(
            UUID id,
            UUID stationId,
            String stationName,
            UUID inventoryItemId,
            String itemCode,
            String itemName,
            String category,
            String unit,
            BigDecimal currentStock,
            BigDecimal dailyConsumption,
            BigDecimal safetyStock,
            BigDecimal reorderPoint,
            int leadTimeDays,
            LocalDate nextResupplyDate,
            Long version
    ) {}

    public record CreateStockLevelRequest(
            @NotNull(message = "stationId is required")
            UUID stationId,

            @NotNull(message = "inventoryItemId is required")
            UUID inventoryItemId,

            @NotNull(message = "currentStock is required")
            @DecimalMin(value = "0.0", message = "currentStock must be >= 0")
            BigDecimal currentStock,

            @NotNull(message = "dailyConsumption is required")
            @DecimalMin(value = "0.0", message = "dailyConsumption must be >= 0")
            BigDecimal dailyConsumption,

            @NotNull(message = "safetyStock is required")
            @DecimalMin(value = "0.0", message = "safetyStock must be >= 0")
            BigDecimal safetyStock,

            @NotNull(message = "reorderPoint is required")
            @DecimalMin(value = "0.0", message = "reorderPoint must be >= 0")
            BigDecimal reorderPoint,

            @Min(value = 0, message = "leadTimeDays must be >= 0")
            Integer leadTimeDays,

            @NotNull(message = "nextResupplyDate is required")
            LocalDate nextResupplyDate
    ) {}

    public record CreateTransactionRequest(
            @NotNull(message = "stationId is required")
            UUID stationId,

            @NotNull(message = "inventoryItemId is required")
            UUID inventoryItemId,

            @NotBlank(message = "type is required")
            String type, // RECEIPT, CONSUMPTION, TRANSFER_OUT, TRANSFER_IN, ADJUSTMENT, WASTE

            @NotNull(message = "quantity is required")
            @DecimalMin(value = "0.01", message = "quantity must be > 0")
            BigDecimal quantity,

            String source,
            String referenceId,
            String operatorId
    ) {}

    public record InventoryTransactionDto(
            UUID id,
            UUID stationId,
            UUID inventoryItemId,
            String type,
            BigDecimal quantity,
            Instant timestamp,
            String source,
            String referenceId,
            String operatorId
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
            RiskLevel riskLevel,
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
