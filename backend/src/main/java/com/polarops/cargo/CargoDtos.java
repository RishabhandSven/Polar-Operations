package com.polarops.cargo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CargoDtos {

    private CargoDtos() {}

    public record CreateCargoConsignmentRequest(
            @NotBlank(message = "trackingNumber is required")
            String trackingNumber,

            @NotNull(message = "destinationStationId is required")
            UUID destinationStationId,

            String status, // Defaults to PLANNED or ARRIVED

            LocalDate departureDate,

            @NotNull(message = "estimatedArrival is required")
            LocalDate estimatedArrival,

            List<AddCargoItemRequest> initialItems
    ) {}

    public record AddCargoItemRequest(
            @NotNull(message = "inventoryItemId is required")
            UUID inventoryItemId,

            @NotNull(message = "quantity is required")
            @DecimalMin(value = "0.01", message = "quantity must be > 0")
            BigDecimal quantity
    ) {}

    public record CargoItemDto(
            UUID id,
            UUID consignmentId,
            UUID inventoryItemId,
            String itemCode,
            String itemName,
            String category,
            String unit,
            BigDecimal quantity
    ) {}

    public record CargoConsignmentDto(
            UUID id,
            String trackingNumber,
            UUID destinationStationId,
            String destinationStationName,
            String status,
            LocalDate departureDate,
            LocalDate estimatedArrival,
            LocalDate actualArrival,
            List<CargoItemDto> items
    ) {}

    public record ReceiveCargoRequest(
            String operatorId
    ) {}

    public record ReceiveCargoResponse(
            UUID consignmentId,
            String trackingNumber,
            String previousStatus,
            String newStatus,
            UUID stationId,
            String stationName,
            int receivedItemCount,
            BigDecimal totalReceivedQuantity,
            Instant receivedAt
    ) {}
}
