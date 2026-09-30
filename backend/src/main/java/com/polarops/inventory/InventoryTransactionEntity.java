package com.polarops.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransactionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @NotNull
    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @NotBlank
    @Column(name = "type", nullable = false, length = 32)
    private String type; // RECEIPT, CONSUMPTION, TRANSFER_OUT, TRANSFER_IN, ADJUSTMENT, WASTE

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @NotNull
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @NotBlank
    @Column(name = "source", nullable = false, length = 64)
    private String source; // MANUAL, CARGO_RECEIPT, INTER_STATION_TRANSFER, OFFLINE_SYNC

    @Column(name = "reference_id", length = 128)
    private String referenceId;

    @Column(name = "operator_id", length = 64)
    private String operatorId;

    public InventoryTransactionEntity() {
    }

    public InventoryTransactionEntity(UUID id, UUID stationId, UUID inventoryItemId, String type,
                                      BigDecimal quantity, Instant timestamp, String source,
                                      String referenceId, String operatorId) {
        this.id = id != null ? id : UUID.randomUUID();
        this.stationId = stationId;
        this.inventoryItemId = inventoryItemId;
        this.type = type;
        this.quantity = quantity;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.source = source;
        this.referenceId = referenceId;
        this.operatorId = operatorId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStationId() {
        return stationId;
    }

    public void setStationId(UUID stationId) {
        this.stationId = stationId;
    }

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(UUID inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryTransactionEntity that = (InventoryTransactionEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
