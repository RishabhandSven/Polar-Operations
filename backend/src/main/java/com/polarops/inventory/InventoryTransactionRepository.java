package com.polarops.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionEntity, UUID> {
    List<InventoryTransactionEntity> findByStationIdOrderByTimestampDesc(UUID stationId);
    List<InventoryTransactionEntity> findByStationIdAndInventoryItemIdOrderByTimestampDesc(UUID stationId, UUID inventoryItemId);
}
