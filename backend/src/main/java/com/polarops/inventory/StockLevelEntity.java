package com.polarops.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "stock_levels", uniqueConstraints = {
        @UniqueConstraint(name = "uq_station_item", columnNames = {"station_id", "inventory_item_id"})
})
public class StockLevelEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @NotNull
    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "current_stock", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentStock;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "daily_consumption", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyConsumption;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "safety_stock", nullable = false, precision = 12, scale = 2)
    private BigDecimal safetyStock;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "reorder_point", nullable = false, precision = 12, scale = 2)
    private BigDecimal reorderPoint;

    @Min(0)
    @Column(name = "lead_time_days", nullable = false)
    private int leadTimeDays = 14;

    @NotNull
    @Column(name = "next_resupply_date", nullable = false)
    private LocalDate nextResupplyDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public StockLevelEntity() {
    }

    public StockLevelEntity(UUID id, UUID stationId, UUID inventoryItemId, BigDecimal currentStock,
                            BigDecimal dailyConsumption, BigDecimal safetyStock, BigDecimal reorderPoint,
                            int leadTimeDays, LocalDate nextResupplyDate) {
        this.id = id != null ? id : UUID.randomUUID();
        this.stationId = stationId;
        this.inventoryItemId = inventoryItemId;
        this.currentStock = currentStock;
        this.dailyConsumption = dailyConsumption;
        this.safetyStock = safetyStock;
        this.reorderPoint = reorderPoint;
        this.leadTimeDays = leadTimeDays;
        this.nextResupplyDate = nextResupplyDate;
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

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }

    public BigDecimal getDailyConsumption() {
        return dailyConsumption;
    }

    public void setDailyConsumption(BigDecimal dailyConsumption) {
        this.dailyConsumption = dailyConsumption;
    }

    public BigDecimal getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(BigDecimal safetyStock) {
        this.safetyStock = safetyStock;
    }

    public BigDecimal getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(BigDecimal reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(int leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public LocalDate getNextResupplyDate() {
        return nextResupplyDate;
    }

    public void setNextResupplyDate(LocalDate nextResupplyDate) {
        this.nextResupplyDate = nextResupplyDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockLevelEntity that = (StockLevelEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
