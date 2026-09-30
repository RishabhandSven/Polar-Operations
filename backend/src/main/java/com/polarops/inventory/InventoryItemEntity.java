package com.polarops.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItemEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "item_code", nullable = false, unique = true, length = 64)
    private String itemCode;

    @NotBlank
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @NotBlank
    @Column(name = "category", nullable = false, length = 64)
    private String category;

    @NotBlank
    @Column(name = "unit", nullable = false, length = 32)
    private String unit;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public InventoryItemEntity() {
    }

    public InventoryItemEntity(UUID id, String itemCode, String name, String category, String unit, String description) {
        this.id = id != null ? id : UUID.randomUUID();
        this.itemCode = itemCode;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryItemEntity that = (InventoryItemEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
